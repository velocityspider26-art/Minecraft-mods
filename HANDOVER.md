# Genesis — Outstanding Issues & Context

State as of commit `2ad3e2b`. Written so you (or another dev/AI) can pick this
up cold. Every claim here came from reading logs or Sable's source, not guessing
— where something is unverified, it says so.

---

## 1. Sable sub-levels vanish when crossing dimensions  ← THE BIG ONE

### Symptom
Fly/transfer a craft from one dimension to another. The player arrives, the
craft does not. Log shows the transfer succeeding, then the craft disappearing
2–4 seconds later.

### What is actually happening (confirmed, not theory)
The transfer itself **works**. `SubLevelSerializer.fullyLoad` creates the craft
in the destination — you can see it in the log:

```
[FOLLOW] arrived sub-levels: [c4e9beca-...]
[FOLLOW] ship c4e9beca-... + crew -> genesis:great_unknown : OK
[RETRACK] re-synced ship c4e9beca-... to 1 crew in genesis:great_unknown   <- visibility OK
[REMOVED] sub-level c4e9beca-... in genesis:great_unknown reason=UNLOADED   <- then deleted
    at SubLevelHoldingChunkMap.moveToUnloaded
    at PhysicsChunkTicketManager.update
    at SubLevelPhysicsSystem.tick
```

**Sable deletes it.** `PhysicsChunkTicketManager.update` runs every physics tick
and does this (Sable 2.0.3 source):

```java
b.set(subLevel.boundingBox());                  // world-space footprint
final BoundingBox3i chunkBounds = b.chunkBoundsFrom();
for (each chunk x,z in chunkBounds) {
    if (!isChunkLoadedEnough(level, x, z)) {
        holdingChunkMap.moveToUnloaded(subLevel, new ChunkPos(x, z));  // craft gone
    }
}
```

And the check itself:

```java
public static boolean isChunkLoadedEnough(ServerLevel level, int x, int z) {
    if (container != null && container.inBounds(x, z)) return true;  // inside plot grid
    return distanceManager.inBlockTickingRange(ChunkPos.asLong(x, z));
}
```

**Key insight:** it does not ask whether the chunk is *loaded*. It requires
**block-ticking range**. Every chunk the craft's hull covers must be
block-ticking, every tick, or the craft is serialised into a holding store and
removed from the container.

### What has been tried and DID NOT work (don't repeat these)
| Attempt | Result |
|---|---|
| Delayed re-announce / re-sync to client | Fixed *visibility*, but craft was already deleted — wrong layer entirely |
| Move crew first, ship follows 25 ticks later | **Made it worse.** Sable unloads the sub-level the moment the last player leaves the origin, so the craft was destroyed before it could follow |
| `addRegionTicket` at arrival point | Craft survived ~2s |
| Synchronous `level.getChunk()` at arrival | Craft survived ~4.2s, re-sync succeeded — closer, still evicted |
| Chunk claim following the craft + leading its velocity | Still evicted |
| `setChunkForced` on the footprint | Still evicted |

All of these tried to *satisfy* the check from outside. That's a losing race:
the check runs every tick over the whole footprint and only has to fail once.

### Current fix (committed `2ad3e2b`, NOT yet tested by the user)
Stop racing it — **refuse the unload**:

- `src/main/java/shipwrights/genesis/mixin/SubLevelHoldingChunkMapMixin.java`
  cancels `moveToUnloaded` at HEAD for protected craft.
- `src/main/java/shipwrights/genesis/teleportation/CraftKeepAlive.java`
  holds the protected set; protection expires after 60s so nothing is pinned.
- `SpaceTravelManager.transferChainToTargetLevel` calls
  `CraftKeepAlive.protect(...)` as each sub-level loads.

Verified the mixin applies and injects (`Mixing SubLevelHoldingChunkMapMixin
... @Inject::genesis$keepProtectedCraftLoaded`). **Whether it actually keeps the
craft alive is untested.**

### If it still fails
`[REMOVED]` will now show a *different* stack (the holding path is blocked).
That's a new cause, not the old one. Also watch for:
- `[HOLD] craft ... chunk NOT ticking` — claim not taking effect
- Whether refusing the unload causes physics instability (the craft may sit in
  an unticking chunk with an unhappy rigid body)

### ROOT CAUSE FOUND (from decompiling Alcubierre 1.2.6)

Base Sable 2.0.3 has **no public API for moving a sub-level between
dimensions**. That is why none was ever found — it does not exist. Everything
Genesis does here (serialise -> find empty plot -> `fullyLoad` in the target ->
remove the original) is a reimplementation of a missing subsystem from the
outside, which is why Sable's physics tick keeps destroying the result: nothing
tells it the craft is legitimately mid-transfer.

Alcubierre does not hand-roll any of it. It makes one call:

```java
dev.egg.SubLevelWarper.WarpSubLevel(ServerSubLevel subLevel, Level target, Vector3d pos) -> int
```

That class ships in a **separate mod, `dimensional_sable`**, declared as a hard
dependency in Alcubierre's `neoforge.mods.toml`:

```toml
[[dependencies.alcubierre]]
    modId="dimensional_sable"
    versionRange="[1.0,)"
```

It is **not installed** here — not in `run/mods`, not bundled inside Alcubierre,
not in Sable, not in the Aeronautics nested jars.

**The fix:** install `dimensional_sable`, add it as a compile dependency, and
replace `SpaceTravelManager.transferChainToTargetLevel` (~150 lines) with that
single call. Most of the scaffolding built to fight the symptoms then comes out:
`SubLevelHoldingChunkMapMixin`, `CraftKeepAlive`, the chunk-following claims in
`SubLevelVisibilitySweep`, and probably the visibility sweep itself. Keep the
crew/seat handling — that part works.

For reference, Alcubierre's own cross-dimension path is
`AlcubierreControllerBlockEntity.executeCrossDimWarp`, which does:
`SubLevelHelper.getConnectedChain` -> `RiderCarry.playersAboard` ->
`RidingPreservation.snapshot` -> `SubLevelWarper.WarpSubLevel` ->
`RidingPreservation.restore`. Note it uses `getConnectedChain`, where Genesis
uses `getLoadingDependencyChain`.

### Best remaining lead
**Get the Alcubierre or Cosmonautics jar into `run/mods/`** and decompile it.
Both reportedly move Sable craft across dimensions successfully. Neither is
installed, so it could not be inspected. Reading a working implementation would
settle this in minutes versus days of inference.

---

## 2. Mercury entry — probably NOT a bug

Log from the last test:
```
[APPROACH] Dev nearest genesis:mercury dist=157 entryZone=148 (inside=false)
```
Entry fires at 148 (radius 98 + 50-block band, as requested). The closest
approach recorded was 157 — **9 blocks short**. Nothing in the code is wrong;
it just needs those last blocks.

If you want it more forgiving, change `SMALL_BODY_ENTRY_PADDING` in
`SpaceTravelManager.java` (currently `50.0`). Bodies ≥ 400 blocks use
`PLANET_ENTRY_PADDING` (1000).

Entry is tested along the path travelled since last tick
(`SpaceToPlanetTeleporter.findEnteredSwept`), so tunnelling through the band at
speed is handled.

**Previously fixed here:** `entrySuppressed` used to block entry to *every* body
after any arrival in space, making small bodies mathematically unreachable
(148-block zone vs 300-block suppression radius). Removed.

---

## 3. Orbit map — improvement list

File: `src/main/java/shipwrights/genesis/client/OrbitMapScreen.java`
Opens with **M** while holding/wearing Orbit Goggles.

### Working now
- Orbits sampled from real `getPosition` (shows eccentricity, inclination, precession)
- Craft's predicted path as a true conic (from `r × v` and the eccentricity vector)
- Readout: trajectory class, speed, circular speed, escape speed, Δv to escape, ap/pe/e
- Drag to rotate, scroll to zoom, left-click target, right-click centre
- Bodies as textured 3D cubes (real planet textures, 3×2 cube-face atlas)

### Needs work
1. **Lighting is fake.** Faces use fixed per-face shading (`shades[]` array), not
   the real star direction. Should compute `dot(faceNormal, toStar)` per body.
2. **No smooth camera.** Right-click snaps focus instantly. Wants easing on
   focus change and zoom (lerp `focus`/`zoom` toward targets each frame).
3. **No zoom-to-inspect.** Asked for: selecting a body smoothly zooms until it
   fills the view like a model viewer. Needs a per-body "inspect" zoom target
   derived from `getActualSize()`.
4. **No info panel.** Asked for a side panel on selection: name, parent star,
   type, gravity, atmosphere, "life chance", orbital elements. All available
   from `Celestial` (`gravity()`, `properties()`, `transformProvider()` casts to
   `OrbitingTransformProvider` for eccentricity/inclination/period).
5. **Cubes, not spheres.** Bodies render as cubes because that's how the world
   renderer draws them. A subdivided sphere in the map would look far better.
6. **No scale indicator / distance readout** between craft and target.
7. **Star is a flat colour** (`drawStarCube`) — no corona or glow.

### Gotchas
- Planet textures live under **genesis** namespace regardless of the body's own
  namespace: `genesis:textures/planets/<body-namespace>/<body-path>.png`.
  (`minecraft:overworld` → `genesis:textures/planets/minecraft/overworld.png`.)
  Getting this wrong renders magenta.
- `graphics.flush()` is required before immediate-mode draws or the batched
  background paints over everything.
- Screen is `isPauseScreen() == false`, so the world keeps ticking behind it.

---

## 4. Mercury content — status

### Done
- Real dimension, landable, 0.38 g
- 3:2 spin–orbit resonance (real 176-day solar day; double sunrise emerges from it)
- Real orbital elements (e=0.2056, i=7.004°, Ω=48.331°, ω=29.124°)
- Four biomes: volcanic plains (basalt), cratered highlands (gravel/andesite),
  barrens (gravel/tuff), polar cold traps (coarse dirt over packed ice)
- Polar-only water ice via custom `genesis:polar` placement modifier (|Z| ≥ 1500)
- Iron/graphite/sulfur ores
- Day burn / night freeze (`MercuryEnvironment`)
- Micro-meteorite impacts on airless worlds (`MicroMeteorites`)

### Unverified / outstanding
- **Creatures.** `cinder_crawler` (Mercury) and `moon_lurker` (Moon) exist with
  model, renderer, texture, spawn placement — all registered correctly. They
  were blocked by a `spawn_costs` entry with `charge: 1.0` against
  `energy_budget: 0.12`, making every spawn unaffordable. **Removed in
  `db91a3c`, never confirmed working in game.**
- New biomes/surfaces only generate in **fresh chunks**. Existing Mercury
  terrain keeps the old flat look — test in a new world or unexplored area.
- Not built: lobate scarps, Caloris basin, no-sound-in-vacuum, spacesuit /
  radiation tiers.

---

## 5. Other known issues

- **Physics-staff crash.** Dragging a craft with the `simulated` mod's physics
  staff across a dimension boundary crashes the server:
  `IllegalArgumentException: pos2 does not fall within the plot of the second
  sub-level`. The drag constraint keeps stale coordinates and re-attaches from
  inside that mod every tick. Not fixable from Genesis — release the drag before
  crossing.
- **Craft orbits are 3D now** (`SpaceLevel.toCelestialSpace` no longer flattens
  Y), but this changed a long-standing assumption. Watch for regressions in
  entry triggers and gravity.
- **Transient startup crash** seen once: `Trying to access unbound value:
  neoforge:swim_speed` during registry bake. Did not reproduce on an identical
  build. Restart clears it.
- **`SubLevelRemovalWatcher`** logs every sub-level removal with reason + stack.
  Useful, but noisy — consider gating behind a config flag before release.

---

## 6. Useful diagnostics already in the code

| Tag | Where | Tells you |
|---|---|---|
| `[REMOVED]` | `SubLevelRemovalWatcher` | Who deleted a sub-level, reason + stack |
| `[HOLD]` | `SubLevelVisibilitySweep` | Craft standing in a non-ticking chunk |
| `[APPROACH]` | `SpaceToPlanetTeleporter` | Distance to nearest body vs its entry zone |
| `[ENTRY]` / `[LAUNCH]` | teleporters | Boundary crossings and their outcome |
| `[FOLLOW]` | `SpaceTravelManager` | Transfer result + arrived sub-level IDs |
| `[RETRACK]` | `SpaceTravelManager` | Visibility re-sync, or "NOT FOUND" |
| `[SWEEP]` | `SubLevelVisibilitySweep` | Re-announced or stranded craft |

Build: `JAVA_HOME=C:\Program Files\Java\jdk-21.0.10` then
`gradlew runClient -PquickWorld="New World"`. Java 25 on PATH fails the build.
