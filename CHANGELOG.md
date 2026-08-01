# Changelog

## 1.0.3 — blade z-fighting fix, flame-like plume

### Blades (the in-game screenshot showed shards, not blades)
- Root cause was not thin geometry, it was **overlap**. At `r_hub = 1.9` with 16 blades the
  angular gap is 22.5 deg — about 0.75 units of arc — while each blade is 1.7 units wide, so
  neighbouring blades interpenetrated near the hub and z-fought into a mess of slivers.
- `blade_ring()` now **staggers blades into two axial layers**. That halves the count per layer,
  doubling the available gap, and any pair that still overlaps radially sits at a different depth
  so no faces are coplanar. Real fans are staggered for the same packing reason.
- Hub radii pushed out and blades widened so the packing actually closes.
- `disc()` was the other half: four full-length crossed bars share caps and side planes at the
  centre. Each bar is now nudged a hair in z and radius, so nothing is ever coplanar.

### Plume — flame instead of liquid
- **Mach diamonds.** Radius is modulated periodically along the core, and — more importantly —
  so is brightness. Beading the outline alone was too subtle; diamonds are bright *nodes*, and
  modulating brightness on the same period is what actually makes them read.
- **Turbulence.** Each ring vertex is perturbed by a per-parcel hash, with the amount growing as
  the square of age: the jet leaves the nozzle clean and breaks up as it entrains air. A perfectly
  circular cross-section is most of why the old version looked like water.
- **Flicker** per parcel rather than per plume, so brightness ripples along the length instead of
  the whole thing pulsing at once.
- Alpha falls off more steeply, so the *bright* core stays short and the rest trails off as haze.
  A long uniformly-bright tube was the other half of the liquid look.
- Ring resolution raised from 8 to 12 so the broken-up surface has something to break up.

### Fixes
- The off-centre torque gametest sampled angular velocity at a single instant. The craft yaws and
  swings back, so the sample could land near a zero crossing — observed 1.7e-3 to 5.5 across runs
  against a 1e-3 threshold, which made the suite intermittently red. Now peak-sampled like the
  others; typical peak is ~30.

## 1.0.2 — blades and a dynamic plume mesh

### Blades
- `blade_ring()` can now build 4/8/12/16 blades. Minecraft only allows one rotation axis at one of
  five fixed angles, so the four axis-aligned blades are rotated by `0/±22.5/45` to fill in the rest.
- Fan rotor is now 16 thin blades instead of 8 fat ones, with a four-step spinner cone. The old
  version read as a handful of loose plates rather than a bladed disc.
- Compressor rotors are 12 blades per stage; stators and afterburner spray bars thinned to match.

### Dynamic plume mesh
- New `PlumeTrail`: the plume is generated from the nozzle's **emission history**, not bolted to the
  block. Once per tick the nozzle emits a gas parcel recording where in the world it left and which
  way it was pointing; each parcel then drifts downstream along *its own* emission direction,
  spreads, cools and fades. The plume is the surface through all live parcels.
  - Fly forward and older parcels are left behind, so the plume stretches and lags.
  - Turn, roll or pitch and the plume **bends**, because each parcel keeps the direction the nozzle
    had when it was emitted.
  - Each parcel stores a raycast clearance measured ahead of the nozzle, so the plume **splashes
    flat against terrain** instead of passing through it.
- Rendered from `RenderLevelStageEvent` in **world space**, not from the block entity renderer.
  A block entity inside a Sable sublevel is drawn with the sublevel transform already applied, so
  anything drawn there moves rigidly with the aircraft — exhaust gas must not.
- Ring frames are parallel-transported between parcels, so the tube does not spin about its own axis
  when the aircraft rolls.
- Custom additive `RenderType` (no depth write, no cull) drawn as two concentric shells, so the
  overlap accumulates into a hot core with a soft edge.
- Reheat radius follows a spindle — bulging just past the nozzle, necking to a point — rather than
  widening monotonically, which read as a fat cone.
- Colour ramp corrected: blue collapses early and hard, otherwise the mid-plume sits at roughly
  equal red and blue and reads as mauve rather than flame.
- The particle system is unchanged and layers on top; the mesh supplies the volume, the particles
  the turbulence and shock diamonds.

### Tooling
- Added `tools/preview_plume.py`, which reimplements the parcel model with the same constants and
  renders four scenarios (stationary, flying straight, banking turn, impinging on terrain) so the
  plume's shape can be checked without launching the game.

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
