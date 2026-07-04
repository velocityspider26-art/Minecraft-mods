# Feature Status

Legend: ✅ ported & working · 🟡 ported with reduced fidelity (documented) · ❌ removed (documented)

## Content

| Feature | Status | Notes |
|---------|--------|-------|
| Blocks (alien stones, sands, salts, ores, plants, shrooms, verdite, machines…) | ✅ | ~130 blocks; `DeferredRegister.Blocks`, `codec()` added, falling/bush concretized |
| Items (block items, tools, ingredients, space armour) | ✅ | Armour on 1.21 `ArmorMaterial` record + `CUSTOM_DATA` modules |
| Fluids (Miasma) | ✅ | Registrate `BaseFlowingFluid`; bucket + liquid block |
| Sounds / particles (zap bubble, verdite) | ✅ | DeferredRegister on 1.21 registries |
| Paintings (space_0) | ✅ | `PaintingVariant(w,h,assetId)`; creative tab via `CustomData` |
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
| Dimension travel (planet ↔ space, wormhole) | ✅ | Construct + entity teleport on compat layer |
| Recipes (crafting, smelting, blasting) | ✅ | Migrated to 1.21 format (`result.id`, string ingredients); removed-mod items substituted |
| Loot tables / tags / advancements | ✅ | Folders migrated (`loot_table`, `recipe`, `tags/block`…) |
| Networking (warp/sound/time packets) | ✅ | NeoForge `CustomPacketPayload` system |
| Configs (client + common) | ✅ | `ModConfigSpec` |
| Save/load safety | ✅ | Only UUIDs / block positions / resource locations persisted; never live constructs |

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

## Client rendering

| Feature | Status | Notes |
|---------|--------|-------|
| Block-entity renderers (nav, radar, void core, void engine) | ✅ | 1.21 vertex API (`addVertex`/`setColor`/`setUv`) |
| Dimension sky/fog effects (space, planet, wormhole) | 🟡 | Fog colour + sky type preserved; custom `renderSky`/`renderClouds` hooks removed in 1.21.1 → vanilla sky fallback |
| Celestial rendering (stars/planets/black holes) | 🟡 | Drawn as vanilla billboards (`SimpleBillboardCelestialRenderer`); bespoke GLSL surfaces/atmospheres retired |
| Custom GLSL shaders (sun/planet/atmosphere/blackhole/wormhole) | ❌ | Lodestone-1.20 shader API rewritten for 1.21 core shaders; no drop-in — documented in PORTING_REPORT |
| Space colour-invert post-process | ❌ | Depended on the removed shader pipeline |
| Planet shadow projector (`client/shading/*`) | ❌ | Part of the removed GLSL subsystem |
| Screens (Tulcite catalyzer, transition, warp loading) | ✅ | Menu screen via `RegisterMenuScreensEvent`; overlays rebased on `Screen` |
| Sound handlers / ambiance | ✅ | `ClientTickEvent.Post` |

## Tests

| Feature | Status | Notes |
|---------|--------|-------|
| Math unit tests (OBB, Occlusion, PolygonClipping) | ✅ | Pure JOML; pass in `./gradlew test` |
| Teleport GameTests | ✅ (compiled) | Rebuilt on Sable assembly + compat; run under `gameTestServer` |
| Shadow/shading unit tests | ❌ | Tested the removed GLSL subsystem; deleted |
