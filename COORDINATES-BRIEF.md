# Coordinates, continuity and orientation — design brief

Companion to `SUPER-PROMPT.md`. Written 2026-07-31.

Three things that look separate and are the same problem:

1. **Where you are** — space position ↔ planet position must round-trip.
2. **Which way is up** — space has no up; a planet does. The change of frame
   must be gradual, not a snap at the boundary.
3. **What you are looking at** — the planet in orbit should be the world you
   land on, at the same place and the same scale.

All three are one question: **is there a single, invertible transform between
the space frame and the planet frame?** Right now there is not, and every
continuity bug traces back to that.

---

## 1. The numbers, exactly

| Quantity | Value | Where |
|---|---|---|
| Space→planet scale | **1 : 16** | `SPACE_BLOCKS_PER_PLANET_BLOCK` |
| Earth cube size | 512 space blocks (half-extent 256) | `overworld.json` |
| Entry padding (body ≥ 400) | 1000 | `PLANET_ENTRY_PADDING` |
| Entry padding (body < 400) | 200 | `SMALL_BODY_ENTRY_PADDING` |
| Exit standoff | half + 400 | `ATMOSPHERE_EXIT_CLEARANCE` |
| Arrival altitude | ~19000 | `atmosphereEntryHeight` |
| Exit altitude | ~20000 | `atmosphereExitHeight` |

The current surface mapping, in `SpaceToPlanetTeleporter.surfaceColumn`:

```
scale = halfExtent / (halfExtent + entryPadding) * 16
surfaceX = local.x * scale
surfaceZ = local.z * scale        // local.y is DISCARDED
```

For Earth: `256 / 1256 * 16 = 3.2611`. A point anywhere on the padded cube
(|component| ≤ 1256) maps into **±4096** blocks of planet surface. So Earth's
world is an 8192-block square, and `8192 / 512 = 16` — the scale is consistent.
That part is right.

---

## 2. Where it breaks: the cube collapses to a plane

`surfaceColumn` uses `local.x` and `local.z` and **throws away `local.y`**. That
means the whole cube — all six faces — is projected onto one flat square.

The consequences are all the continuity bugs seen so far:

- **The top and bottom faces map to the same place.** Entering from above
  Earth and from below Earth land you at the same coordinates.
- **Side faces are degenerate.** On the +X face, `local.x` is pinned at 1256, so
  the entire face collapses to a single *line* at surfaceX = 4096.
- **Entry through the top face lands near the origin**, from which no direction
  can be recovered — which is precisely why `SpaceTravelManager` has to
  *remember* the entry direction in `ENTRY_DIRECTIONS` to make leaving work.
  That map is a workaround for a lossy transform, not a feature.
- **It is not invertible**, so exit reconstructs an approximation and the round
  trip drifts.

**The fix is to stop discarding a dimension.** A cube has six faces; the planet
world should have six regions.

---

## 3. Proposal: the cube-net surface

Lay the planet world out as a **cube net** — the same 3×2 arrangement the planet
texture atlas already uses (`north west south / east down up`, confirmed by the
768×512 and 384×256 painted textures).

```
        ┌────────┬────────┬────────┐
        │ north  │  west  │ south  │   row 0
        ├────────┼────────┼────────┤
        │  east  │  down  │   up   │   row 1
        └────────┴────────┴────────┘
```

Each region is one face's worth of surface — for Earth, 8192 × 8192 blocks.

**Keep the `up` face centred on the origin.** That preserves every existing
world and build: today's mapping is effectively the up-face projection, so
anchoring `up` at (0, 0) means nothing already placed moves. The other five
regions go to previously unused coordinates.

### The transform

Given a point `p` in the celestial's local rotating frame:

1. **Pick the face** — the axis with the largest absolute component
   (the Chebyshev norm). `|p.y|` largest and positive → `up`, and so on.
2. **Project to that face** — scale `p` so the dominant component equals the
   cube half-extent plus padding. This is where the ray from the body's centre
   leaves the entry cube, i.e. the face you are actually sinking through.
3. **Take the two in-face axes** as the face's local (u, v), scale by
   `halfExtent / (halfExtent + padding) * 16`.
4. **Offset by the face's region origin** in the net.

Inverting is the same steps backwards: world position → which region → face and
(u, v) → local direction → space position. **It is exactly invertible**, which
means `ENTRY_DIRECTIONS` can be deleted rather than maintained.

### Why this is worth doing

- Entry and exit round-trip exactly. No remembered state.
- The world's layout matches the planet's texture atlas, so the surface you see
  in orbit is the surface you land on — face for face.
- Six distinct landing regions instead of one, so a planet has actual geography
  rather than one square repeated.

### What it costs

- The seams between regions are discontinuous — walking off the edge of `up`
  should wrap to a neighbouring face, and that needs either a teleport at the
  seam or an accepted wall. **This is the main unsolved piece.**
- Existing saves keep their `up` region; the other five generate fresh.

---

## 4. Proposal: the gravity frame (the SFS camera correction)

In space there is no up. Near a body there is: **down is toward its centre.**
The jarring part of a crossing is that orientation changes discontinuously —
you can be flying "upright" in space with the planet off to your left, and
arrive on a surface whose up is +Y.

The fix is that the frame should already have rotated **before** you cross.

### The model

Define a client-side *gravity frame* — a quaternion describing local up:

```
awayFromBody = normalise(playerPos − bodyCentre)      // in celestial space
targetUp     = the body's local +Y for the face being approached
influence    = 1 − clamp((distance − entryRadius) / (approachRadius − entryRadius), 0, 1)
frame        = slerp(freeFlightFrame, alignTo(targetUp), smoothstep(influence))
```

- **Far from everything:** `influence` is 0. Free 6DoF, no correction, space
  feels like space.
- **On approach:** `influence` ramps toward 1 across the same band
  `pregenApproach` already uses (`APPROACH_RADIUS_FACTOR = 4`), so the horizon
  rolls level over several seconds of flight rather than snapping.
- **At the boundary:** `influence` is 1 and the frame already equals the
  destination's local frame, so the crossing changes nothing visible.

`slerp` with a `smoothstep` on the influence matters — a linear blend has a
visible velocity discontinuity at both ends of the ramp.

### Which "up" to align to

The face being entered, from step 1 of §3. Approaching the `+X` face, your up
should align to the body's local +X — because that face's region has its own up
once you are standing on it. This is the piece that makes the cube-net and the
camera correction the *same* design rather than two.

### Implementation notes

- Sable already rotates the camera with the ship
  (`sable.mixins.json:camera.camera_rotation.EntityMixin`), so a rotated camera
  frame is established machinery here — the gravity frame composes on top of it
  rather than fighting it.
- This is **client-side and purely visual**. Do not rotate the server-side
  player; that breaks movement input and collision.
- A player not aboard a ship should probably get a weaker or zero correction —
  tumbling in a spacesuit is a different feel from flying a craft.

---

## 5. Velocity and scale continuity

Currently `EntityTeleporter.MAX_ARRIVAL_SPEED = 4.0` clamps arrival speed, with
the reasoning that the two dimensions are not at the same scale so carrying
speed literally would multiply it.

That reasoning is right and the fix is wrong. **Speed should be converted, not
clamped:**

```
planetVelocity = spaceVelocity * 16
spaceVelocity  = planetVelocity / 16
```

A craft doing 2 blocks/tick in space is doing 32 blocks/tick of planet-scale
movement — that *is* the same physical speed, and clamping it to 4 turns a
descent into a dead stop. The clamp exists because the multiplication was never
applied; with the conversion, no clamp is needed beyond a sanity ceiling.

**Caveat worth testing:** 32 blocks/tick is fast enough to tunnel through
terrain. Either raise the arrival altitude so there is room to decelerate, or
apply atmospheric drag on entry — which is the physically honest answer and
makes reentry a real manoeuvre rather than a teleport.

---

## 6. Time and rotation continuity

A planet rotates in space (`dayLength` in its celestial JSON) and has its own
day/night cycle on the surface. These must be the same rotation, or the sun is
in one place from orbit and another from the ground.

`LevelTimeAccessMixin` and `OrbitingTransformProvider` already tie these
together. The check to run: stand on the surface at noon, launch to space, and
confirm the sun is on the side of the planet you just left. If it is not, the
surface clock and the orbital rotation have opposite sign or a phase offset.

The same applies to the **cube-net face**: which face is sunlit in orbit must be
the face whose region is in daylight.

---

## 7. "The world is the texture" — how it ties in

Once §3 exists, this stops being a separate feature.

The planet in orbit is a cube with a 3×2 face atlas. The planet's world is a
cube net with 3×2 regions. **They are the same layout**, so:

- Sampling region `up` of the world produces exactly the texture for the `up`
  face.
- A mountain at world (1000, 2000) in the `up` region is at a knowable point on
  the `up` face of the cube in orbit.
- Aiming at a visible feature from orbit and landing on it becomes exact rather
  than approximate.

Current state: `PlanetSurfaceData` already carries real terrain height per
column to the client (see `SUPER-PROMPT.md` §4). The remaining work is to
subdivide each cube face into a grid and displace vertices along the face
normal by real height — at which point the planet has a silhouette instead of a
painted picture of one.

**The sampler currently samples one square centred on the origin**, which is
the `up` region only. Under §3 it should sample all six regions, one per face.

---

## 8. Order of work

Do these in this order; each one makes the next cheaper.

1. **Cube-net transform, in isolation.** A pure function with unit tests —
   `spaceLocal → (face, u, v) → world` and back — asserting the round trip is
   exact. No game code touched. This is testable without launching Minecraft,
   which matters given how much of this project cannot be.
2. **Switch entry and exit to it.** Delete `ENTRY_DIRECTIONS`; it exists only
   to patch the lossy mapping.
3. **Velocity conversion** (§5), with drag if tunnelling shows up.
4. **Gravity frame** (§4). Visual only, so it can be developed against a
   running game without risking world data.
5. **Six-region sampling and face-displaced geometry** (§7).

Seam behaviour at region edges (§3) is the one genuinely open design question.
Decide it before step 1, because it determines whether regions are adjacent in
world space or deliberately separated by unreachable distance.
