# Changelog

## 1.0.1 — model and particle rework

### Models
- Rewrote the octagon builder. The previous `octagon_ring()` drew four full-width slabs *and* four
  rotated corner slabs over them, so every casing was a pile of interpenetrating boxes. `tube()` and
  `disc()` now emit eight non-overlapping segments at `0.43 * R` half-length (exact regular octagon
  is `0.414 * R`).
- Gave each module its own silhouette: flared brass intake lip, banded compressor barrel, bulged
  core with external fuel manifolds, afterburner flame-holder rings, stepped nozzle collar. All
  share `R_JOIN = 7.0` at the ends so a chain lines up.
- Fixed two textures that were generated but never referenced; the generator now fails loudly on
  unused textures.
- All models inherit `minecraft:block/block`, so the item form finally has display transforms.
- Textures redrawn at 16x16 with flat banding instead of per-pixel noise.

### Particles
- Replaced the vanilla stand-ins (`WHITE_ASH`, `CLOUD`, `FLAME`) with four custom types:
  `exhaust_haze`, `exhaust_soot`, `jet_flame`, `shock_diamond`, each with its own sprite.
- Jet-appropriate physics: no gravity, no collision, heavy damping, short lifetimes, full-bright
  combustion, blue-to-orange cooling ramp on the reheat flame.
- Emission is a proper cone about the nozzle axis, with flames spawned *along* the plume rather than
  fired downrange.

### Tooling
- Added `tools/preview_model.py`, an offline rasteriser for block-model JSON that reproduces
  Minecraft's per-face shading, so models can be iterated on without launching the game.

### Fixes
- Gametests no longer read a destroyed physics body. A light test craft under full thrust leaves the
  loaded area within seconds and Sable frees its rigid body; every read is now `isValid()`-guarded
  and peak speed is sampled per tick rather than once at the end.
- `sable$physicsTick` also checks `handle.isValid()` before touching the body.

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
