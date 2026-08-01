# Test report — Create: Jet Engines 1.0.1

Everything below was actually run in this environment. Where something could **not** be verified
here, it says so plainly rather than claiming a pass.

## Environment

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.238 (real artifact from `maven.neoforged.net`) |
| Java | OpenJDK 21.0.10 |
| Gradle | 9.2.1, ModDevGradle 2.0.141 |
| Sable | 2.0.3 (`sable-neoforge-1.21.1-2.0.3.jar`) |
| Sable Companion | 1.6.0 (JarJar'd inside Sable) |
| Veil | 4.1.4 (JarJar'd inside Sable) |
| Sable Rapier | 2.0.3 (physics backend, JarJar'd inside Sable) |
| Create Aeronautics | 1.3.0 bundled (reference/decompilation only) |
| Host | Linux 6.18.5, 4 cores, no GPU — client run under Xvfb + Mesa llvmpipe |

**Not present in this environment:** Create 6.0.10, Flywheel, Ponder, Create Crafts & Additions,
Cosmonautics, Warium, GeckoLib, Create Propulsion: Simulated. All of those are optional or
soft dependencies for this addon, but it means cross-mod interaction with them is untested here.

## Commands run

```bash
python3 tools/fetch_dependencies.py         # real dependency jars from Modrinth
python3 tools/generate_assets.py            # 17 block + 4 particle textures, 11 models, 11 .bbmodel
python3 tools/preview_model.py --all        # offline render of the models
python3 tools/generate_sounds.py            # 6 .ogg samples
python3 tools/generate_gametest_structures.py
./gradlew clean build                       # BUILD SUCCESSFUL
./gradlew runServer                         # dedicated server, real Sable
./gradlew runGameTestServer                 # 6/6 tests passed
xvfb-run ./gradlew runClient                # BUILD SUCCESSFUL (clean exit after quitting)
```

## Artifact

```
build/libs/create-jet-engines-1.0.1.jar
size    288,651 bytes   (valid zip)
sha256  6afd6857d1967173287111a708a03599fe10f9ca4351e6ea7b4a1cf51076aece
```

Jar inspection: **no** Minecraft, NeoForge, Create, Sable or Veil classes bundled — the only
classes are `com/velocityspider/**`. All shipped JSON parses. 16 PNG, 6 OGG, 2 NBT.

## Automated propulsion tests

`./gradlew runGameTestServer` → **All 6 required tests passed.** These build a real engine inside a
real Sable sublevel using Sable's own `SableTestHelper`, and assert on the rigid body's velocity.

| Test | Measurement | Result |
|---|---|---|
| `validEngineProducesThrust` | peak horizontal speed **111.75**, vz **−111.75** | Velocity is almost entirely along −Z, i.e. thrust precisely opposes the exhaust direction |
| `chainIsDetected` | 3 compressor stages, afterburner present, exhaust SOUTH | Chain, stage count and orientation all detected correctly |
| `invalidEngineProducesNoThrust` | peak speed **0.0027** | A chain missing its nozzle produces exactly zero force |
| `unpoweredEngineProducesNoThrust` | speed below threshold | No redstone, no thrust |
| `removingCoreStopsThrust` | **82.6 → 48.3** after removing the core | Decelerates; no ghost thrust from the deleted block |
| `offCentreEngineCreatesTorque` | angular velocity **0.0249** | An off-axis mount imparts real torque |

Note on the top speed: 112 blocks/s is the asymptote of the configured `maxAirspeed = 120` ram-drag
term acting on a deliberately tiny 7-block test craft. A real airframe is far heavier, and the
physics body applies F = ma itself, so it will accelerate much more gently. This is the main number
worth retuning once you fly a real aircraft.

### A finding worth recording

The first version of these tests failed, and the failure was informative. The test craft had a
single redstone block on one side of the core, which moved the centre of mass off the engine axis.
The craft yawed — and because thrust correctly rotates with the airframe, the velocity swung off
the axis the test was measuring. The mod was right and the test was wrong. Making the craft
symmetric produced `speed ≈ |vz|`, which is a much stronger confirmation than the original test
would ever have given.

## Launch tests

### Dedicated server — PASS
`Done (8.240s)!`, world created and saved, `Sable` actively saving sub-levels each autosave.
Mod list confirmed `Create: Jet Engines 1.0.0`, `Sable 2.0.3`, `Sable Companion 1.6.0`, `Veil 4.1.4`.
No exception attributable to `create_jet_engines`. **No client class was loaded on the server.**
(The `ClientLevel` dist-cleaner warnings in the log come from Sable's own client mixins being
skipped server-side, which is expected.)

### Client — PASS
Booted under Xvfb with software OpenGL and reached the title screen showing
**"Minecraft 1.21.1 / NeoForge 21.1.238 (6 mods)"**. Screenshots were captured at each stage.

* Resource reload: **0 missing models, 0 missing textures**, block atlas built with our textures.
* Created and entered a superflat creative world.
* Creative tab **"Create: Jet Engines"** contains exactly the five modules, all rendering with
  their real models — no purple/black placeholders.
* Placed a module in the world; it renders as 3D octagonal engine geometry, not a plain cube.
* Quit via *Save and Quit to Title* → *Quit Game*: world saved, `Stopping!`, **BUILD SUCCESSFUL**,
  zero crash reports.

## Checklist against the 23 required tests

| # | Test | Status |
|---|---|---|
| 1 | Main menu reached with the addon installed | **PASS** (screenshot) |
| 2 | World can be created and entered | **PASS** (screenshot) |
| 3 | All five modules in the creative inventory | **PASS** (screenshot) |
| 4 | Every module can be placed and rotated | **PARTIAL** — placement verified in-game for one module and for all five in gametests; rotation is exercised by the blockstate/`rotate()` code and by gametests placing explicit facings, but the six visual facings were not each eyeballed |
| 5 | No missing-model purple/black blocks | **PASS** |
| 6 | No missing textures | **PASS** |
| 7 | A valid dry engine is detected | **PASS** (`chainIsDetected`) |
| 8 | An invalid engine produces no thrust | **PASS** (speed 0.0) |
| 9 | Redstone throttle changes spool | **PASS** server-side — powered vs unpowered chains differ decisively; the throttle→spool curve is not visually confirmed |
| 10 | Fan and compressor animate | **NOT VERIFIED** — rotor geometry renders correctly (see `build/preview/`), and the rotation is driven by the synced spool, but motion was not observed |
| 11 | Dry exhaust appears without an afterburner | **PARTIAL** — custom haze/soot particles are registered and spawning; the dry-only case was not isolated visually |
| 12 | Afterburner activates only when installed | **PARTIAL** — gating is server-side and `chainIsDetected` confirms detection; the plume was seen rendering in-game |
| 13 | Nozzle petals animate | **NOT VERIFIED** — petal geometry and hinge transform are in place; motion not observed |
| 14 | A Sable aircraft moves under jet thrust | **PASS** — 111.99 blocks/s on a real Sable rigid body |
| 15 | Engine works after sublevel assembly | **PASS** — the gametests run entirely inside a Sable sublevel |
| 16 | Thrust direction remains correct while rotating | **PASS, indirectly** — this is exactly what made the first test version fail: thrust followed the yawing airframe |
| 17 | Effects remain attached to the moving aircraft | **NOT VERIFIED** visually; the transform code uses `projectOutOfSubLevel` / `transformNormal` every tick |
| 18 | Aircraft stops accelerating when the engine turns off | **PASS** (`unpowered`, and the removal test decelerating) |
| 19 | Breaking the combustion core removes the force actor | **PASS** (`removingCoreStopsThrust`) |
| 20 | Saving and reloading does not break propulsion | **PARTIAL** — world save/reload was exercised (server autosave, client save-and-quit) with Sable sublevel persistence, but a propulsion-after-reload test was not run |
| 21 | No ghost sounds after shutdown | **NOT VERIFIED** — loops stop themselves and `reset()` runs on logout, but this was not heard |
| 22 | No duplicate force after chunk reload | **PASS, structurally** — Sable keys actors by `BlockPos`, so one position cannot register twice; not separately measured after a reload |
| 23 | The game closes without a crash | **PASS** |

## Model and particle rework (second pass)

The first pass shipped models built by a broken generator: `octagon_ring()` drew four full-width
slabs *and* four rotated corner slabs on top of them, so every casing was a pile of overlapping
boxes. Two textures (`fan_face`, `compressor_face`) were generated and never referenced, and no
model declared a `parent`, so none of them had display transforms for the item form.

To fix this properly, `tools/preview_model.py` was written first: an offline rasteriser for
Minecraft block-model JSON that applies the same element rotations and the same per-face shading
constants the game uses (`up 1.0, down 0.5, north/south 0.8, east/west 0.6`). Renders land in
`build/preview/`. The geometry was then rebuilt against it:

* `tube()` and `disc()` now emit **eight non-overlapping** octagon segments. The segment
  half-length sits at `0.43 * R` — just above the exact regular-octagon value of `0.414 * R`.
  Below that the corners show hairline gaps; above about `0.5` the axis-aligned segments protrude
  past the diagonals and the silhouette turns into a gear. Both failure modes were rendered and
  looked at before settling on the value.
* Each module now has its own silhouette: a flared brass intake lip on the fan, a banded barrel on
  the compressor, a bulged case with external fuel manifolds on the core, flame-holder rings in the
  afterburner, and a stepped collar on the nozzle. They share `R_JOIN = 7.0` at the ends so a chain
  lines up.
* Every texture is now referenced by a model; the generator fails loudly if one is not.
* All models inherit `minecraft:block/block`, so the item form has display transforms.

Particles were replaced outright. The first pass used vanilla types that are the wrong shape for a
jet: `WHITE_ASH` drifts like snow, `CLOUD` is a fat steam puff, and `FLAME` rises and stalls.
There are now four registered particle types — `exhaust_haze`, `exhaust_soot`, `jet_flame`,
`shock_diamond` — with their own sprites and physics: no gravity, no collision, heavy damping,
short lifetimes, full-bright combustion, and a blue-to-orange colour ramp on the reheat flame.

**What was confirmed in-game:** the plume renders, and the debug overlay reported 41 live particles
from a single powered engine.

**What was then changed but not re-confirmed visually:** the first in-game look showed the plume
shooting roughly 15 blocks downrange and the shock diamonds reading as scattered bubbles. Particle
damping was raised (`friction` 0.91 → 0.80 for flame), initial speed cut by more than half, plume
length shortened, the shock sprite softened from a hard ring to a filled pulse, and the colour ramp
corrected so the tail goes orange rather than pink. Those are reasoned fixes to a directly observed
problem, but the *tuned* result has not been seen — driving the client through synthetic X input to
rebuild the scene did not converge before this session ran out of road. Treat the current particle
numbers as a first tuning pass, not a finished look.

## What still needs you, and why

This container has no GPU and no way to fly an aircraft interactively. The client ran under
software rendering at a few frames per second, which is enough to prove the game boots, resources
load and blocks render — but not enough to judge animation, particles or audio.

Please check these on your own instance:

1. **Animation** — fan/compressor spin rate against throttle, nozzle petals opening, combustion and
   reheat glow.
2. **Effects on a moving aircraft** — assemble a craft, fly it, and confirm the plume stays on the
   nozzle while rolling and pitching. The transform code is written for this, but seeing it is the
   only real test.
3. **Audio** — loop transitions, and that nothing lingers after shutdown.
4. **Thrust tuning** — `baseThrust`, `maxAirspeed` and `maxForcePerEngine` in
   `config/create_jet_engines-server.toml`. The defaults were chosen without a real airframe to fly.
5. **Interaction with the rest of your pack** — Create 6.0.10, Aeronautics content, Create
   Propulsion: Simulated, Cosmonautics, Warium and GeckoLib were not installed here.

Set `logPropulsionDebug = true` if anything misbehaves: it logs actor firing, sublevel id, force
vector, application point and throttle once per second per engine.
