# Feature Status

Legend: ✅ ported & working · 🟡 ported with reduced fidelity (documented) · ❌ removed (documented)

## Content

| Feature | Status | Notes |
|---------|--------|-------|
| Blocks (alien stones, sands, salts, ores, plants, shrooms, verdite, machines…) | ✅ | ~130 blocks; `DeferredRegister.Blocks`, `codec()` added, falling/bush concretized |
| Items (block items, tools, ingredients, space armour) | ✅ | Armour on 1.21 `ArmorMaterial` record + `CUSTOM_DATA` modules |
| Fluids (Miasma) | ✅ | Registrate `BaseFlowingFluid`; bucket + liquid block |
| Sounds / particles (zap bubble, verdite) | ✅ | DeferredRegister on 1.21 registries |
| Paintings (space_0) | ✅ | Datapack `painting_variant/space_0.json` (dynamic registry in 1.21.1); creative tab via `CustomData` |
| Creative tabs (Genesis, Genesis Natural) | ✅ | `Registries.CREATIVE_MODE_TAB`; dynamic slab/stair items included |
| Void Engine machine (warp drive) | ✅ | NeoForge energy capability; charge/warp/return logic on compat layer |
| Tulcite Catalyzer (menu + generator) | ✅ | `useWithoutItem`/`openMenu`; item+energy capabilities; container menu |
| Radar display | ✅ | Scans constructs & planets via compat lookup |
| Nav projector (BE + renderer) | ✅ | Projects celestials; ship transforms via compat |

## Worlds & mechanics

| Feature | Status | Notes |
|---------|--------|-------|
| Space dimension (`great_unknown`, 1/16 scale) | ✅ | Loads valid; dimension + dimension_type datapacks migrated |
| Wormhole dimension (`subspace`) | ✅ | Loads valid; lightning ticker on `LevelTickEvent.Post` |
| Planet dimensions (moon, malachite, …) | ✅ | Registered & valid at server start |
| Asteroid worldgen (density fns, surface rule, belt) | ✅ | `MapCodec` registration; int-providers migrated to 1.21 format |
| Biome modifiers (ore/miasma placement) | ✅ | `neoforge:add_features` |
| Celestial datapack registry + transform providers | ✅ | `MapCodec` dispatch; static/orbiting providers |
| Frozen-time system (time offset) | ✅ | `SavedData` (1.21 Factory) + payload sync |
| Seamless Earth→space ascent | ✅ | Leaving Earth is **no longer a dimension teleport** — the player and craft stay in the overworld the whole climb. Sky/fog fade to black and the celestial renderer lifts the planet away below (altitude-aware `VantagePoint.getObserverPosition()`), so surface → atmosphere → orbit → past the Moon is continuous, with no portal/loading screen. The Sun and Moon render in the overworld sky; Earth materialises below you as you climb clear of the terrain. |
| Deep-space transition | ✅ | The **only** travel dimension change: once you climb clear past the Moon (deep-space height, ~20,000 blocks beyond the Moon's ~10,000) you cross into `genesis:subspace`, carrying momentum/rotation/passengers. Height in `genesis-common.toml` (`deepSpaceRadius`, default 30,000). It cannot fire near Earth — it is purely a past-the-Moon altitude gate. Server-side dimension load verified (`Done`, no errors); the frame-captured transition is wrapped so a GL failure degrades to the vanilla screen instead of crashing. |
| `/genesis space` + `great_unknown`/`moon` dimensions | ✅ | The scaled space dimension and its space↔planet loop remain for the `/genesis space` / `/genesis land` debug commands, but are no longer entered automatically by flying up. |
| Recipes (crafting, smelting, blasting) | ✅ | Migrated to 1.21 format (`result.id`, string ingredients); removed-mod items substituted |
| Loot tables / tags / advancements | ✅ | Folders migrated (`loot_table`, `recipe`, `tags/block`…) |
| Networking (warp/sound/time packets) | ✅ | NeoForge `CustomPacketPayload` system |
| Configs (client + common) | ✅ | `ModConfigSpec` |
| Save/load safety | ✅ | Only UUIDs / block positions / resource locations persisted; never live constructs |

## Cubed Earth landing

| Feature | Status | Notes |
|---------|--------|-------|
| Cube-face → overworld region mapping | ✅ | `CubePlanetMapping`: each of the 6 faces maps to a distinct overworld region (cross-net layout), and position on the face shifts landing within the region; launches reverse it (leave a region → exit the matching face). Unit tested (`CubePlanetMappingTest`, 6 cases) + verified live (top→(0,0), west→(19331,0)). |

## Create Aeronautics integration (was Valkyrien Skies)

| Capability | Status | Notes |
|-----------|--------|-------|
| Construct detection at position | ✅ | `AeronauticsContraptionLookup.getConstructManaging` |
| Construct transform / local↔world | ✅ | `AeronauticsTransformHelper` via Sable `logicalPose` |
| Construct velocity / angular velocity / mass | ✅ | `AeronauticsPhysicsAdapter` |
| Apply force / torque / impulse | ✅ | `AeronauticsForceHandler` via `RigidBodyHandle` |
| Same-dimension construct teleport | ✅ | `RigidBodyHandle.teleport` + velocity |
| Cross-dimension construct teleport | 🟡 | Real disassemble→transfer→reassemble via `SubLevelAssemblyHelper`; needs live CA verification with a built vehicle |
| Moving-construct collision queries | ✅ | `AeronauticsCollisionHandler` (Sable owns the actual collision) |
| Construct-cluster teleport | 🟡 | Proximity-based (CA has no ship↔ship revolute joints like VS) |
| Velocity inheritance for riders | ✅ | `AeronauticsMovementHelper` |

## Atmosphere flight effects

| Feature | Status | Notes |
|---------|--------|-------|
| Re-entry / atmosphere-exit heating | ✅ (code-verified) | `AtmosphereBurnEffects`: a **coloured-plasma bow-shock cap** wraps the leading face of **Sable physics objects only** (constructs from `AeronauticsContraptionLookup`) — a bright dome of `DUST_COLOR_TRANSITION` particles, white-gold at the stagnation point bleeding through orange to deep-red at the shoulders, curving back around the hull with a white-hot core and a plasma wake streaming behind (the KSP-style look in the reference image, not scattered vanilla flames). Heat ramps smoothly with speed (15–65 blocks/s) and air density and fades out smoothly; players/mobs/items are never touched. Needs a built CA vehicle for live colour/scale tuning. |

## Client rendering

| Feature | Status | Notes |
|---------|--------|-------|
| Block-entity renderers (nav, radar, void core, void engine) | ✅ | 1.21 vertex API (`addVertex`/`setColor`/`setUv`) |
| Dimension sky/fog effects (space, planet, wormhole) | 🟡 | Fog colour + sky type preserved; custom `renderSky`/`renderClouds` hooks removed in 1.21.1 → vanilla sky fallback |
| Celestial rendering (stars/planets/black holes) | ✅ | Restored VS Genesis' original raymarched/textured **cube** celestials via `StarRenderer` + `PlanetRenderer`: the Sun ray-marches a rounded cube in the `sun` fragment shader tinted between its two colours; Earth/Moon draw as six-faced cubes sampling the original 3×2 surface net through `planet_textured`, directionally lit from the nearest star with an additive atmosphere rim. Automatic vanilla-pipeline billboard fallback if a shader can't compile. |
| Custom GLSL shaders (sun/planet/atmosphere/blackhole/wormhole) | ✅ | VS Genesis' original `.vsh`/`.fsh`/`.json` shaders ported onto **Lodestone 1.8.2's** `ShaderHolder`/`ExtendedShaderInstance` API (`client/ShaderRegistry`). All five Genesis shaders (`sun`, `planet_textured`, `planet_atmosphere`, `blackhole`, `star_glow`) compile and link — verified in a headless llvmpipe client boot. Toggle with `UseProceduralCelestialShaders` in the client config. |
| Space colour-invert post-process | ❌ | Depended on the removed full-screen post pipeline |
| Planet shadow projector (`client/shading/*`) | ❌ | Part of the removed GLSL subsystem |
| Screens (Tulcite catalyzer, transition, warp loading) | ✅ | Menu screen via `RegisterMenuScreensEvent`; overlays rebased on `Screen` |
| Sound handlers / ambiance | ✅ | `ClientTickEvent.Post` |

## Tests

| Feature | Status | Notes |
|---------|--------|-------|
| Math unit tests (OBB, Occlusion, PolygonClipping) | ✅ | Pure JOML; pass in `./gradlew test` |
| Teleport GameTests | ✅ (compiled) | Rebuilt on Sable assembly + compat; run under `gameTestServer` |
| Shadow/shading unit tests | ❌ | Tested the removed GLSL subsystem; deleted |
