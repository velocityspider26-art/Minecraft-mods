# Changelog

## 1.0.0 — full rebuild

Rebuilt from an empty NeoForge MDK. Nothing from the previous stub-compiled builds was carried over.

### Build system
- Real NeoForge **21.1.238** / Minecraft **1.21.1** / Java **21** workspace (ModDevGradle 2.0.141).
- Compiles against the **real** Sable 2.0.3, Sable Companion 1.6.0 and Veil 4.1.4 jars.
  No hand-written API stubs anywhere in the project.
- `runClient` / `runServer` / `runGameTestServer` all boot with real Sable present.
- Production jar contains only `com/velocityspider/**` — no bundled dependency classes.

### Propulsion
- `CombustionCoreBlockEntity` implements Sable's `BlockEntitySubLevelActor` and applies thrust in
  `sable$physicsTick` via `QueuedForceGroup#applyAndRecordPointForce` under `ForceGroups.PROPULSION`.
- Force is built in sublevel-local space and applied at the nozzle, so off-centre engines produce
  real torque and symmetric pairs cancel.
- Exactly one actor per chain (the core), so a chain can never double-count force.
- Thrust computed on the game thread and handed to the physics step through a volatile, so the
  physics callback never touches world state.

### Engine structure
- Five modules: fan, compressor, combustion core, afterburner (optional), nozzle.
- 1–4 compressor stages with diminishing returns.
- Bounded, cached chain scan; rescans only on neighbour/place/remove/load changes.
- Detects intake and exhaust obstruction, misalignment, and every incomplete-chain case.

### Engine behaviour
- Redstone throttle (0 = off, 1–15 proportional) with slew limiting.
- Gradual spool up/down; thrust scales with `spool²` so a cold engine cannot jump to full thrust.
- Afterburner only lights above a configurable dry-spool threshold and only when installed.
- Atmospheric efficiency from Sable's per-dimension air pressure, sampled at the engine's real
  world position.
- Ram-drag term gives a natural top speed instead of unbounded acceleration.
- Full server config with a hard per-engine force clamp.

### Visuals and audio
- Five custom models built from octagonal casings, bladed rotors, flame-holder rings and hinged
  nozzle petals — no plain cubes.
- Animated fan and compressor rotors driven by the synchronised spool fraction; hinged nozzle
  petals; combustion and reheat glow.
- Exhaust plume built from layered, alternately-rolled shells with a shock-diamond ripple — 3D from
  every angle, not a camera-facing quad.
- Dry exhaust effects (shimmer, plume, start-up smoke, rich-throttle smoke, condensation) plus a
  separate afterburner effect set.
- All effects and sounds are projected out of the sublevel every tick, so they stay attached to a
  moving, rotating aircraft.
- Six procedurally synthesised, seamlessly looping engine sounds.

### Assets
- `tools/generate_assets.py` — textures, block models, blockstates, item models, `.bbmodel` sources.
- `tools/generate_sounds.py` — all six `.ogg` samples.
- `tools/generate_gametest_structures.py` — gametest templates.
- Editable Blockbench projects in `assets_src/`.

### Tests
- Six automated gametests covering chain detection, thrust, invalid chains, unpowered chains,
  actor removal and off-centre torque. All pass against real Sable physics.
