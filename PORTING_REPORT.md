# VS Genesis → NeoForge 1.21.1 + Create Aeronautics — Porting Report

This document explains what was rewritten to move **VS Genesis** from **Forge 1.20.1 /
Valkyrien Skies** to **NeoForge 1.21.1 / Create Aeronautics**, and why.

## 1. Platform change: Valkyrien Skies → Create Aeronautics

Genesis was built around Valkyrien Skies (VS) ships. VS is not available for this target, and the
brief requires the moving-vehicle/physics layer to be **Create Aeronautics** instead.

Create Aeronautics is built on two libraries:

| Layer | Mod | Role |
|-------|-----|------|
| Physics / moving sub-worlds | **Sable** (`dev.ryanhcode.sable`) | The engine that assembles blocks into moving "sub-levels" and simulates them |
| Content / gameplay | **Create Aeronautics** = `simulated` + `aeronautics` (`dev.eriksonn`, `dev.simulated_team`) | Propellers, bearings, assembly UX, etc. built on Sable + Create |

Genesis now depends on `sable`, `simulated`, `aeronautics` and `create`, and **no longer requires
Valkyrien Skies**. A repository-wide search confirms there are no remaining `org.valkyrienskies`
imports or `valkyrienskies` mod dependencies.

### The compatibility layer

All VS interaction was replaced by a single, self-contained package —
`shipwrights.genesis.compat.aeronautics` — so that nothing else in the mod references a Sable or
Create type directly. Swapping the physics platform again would only require re-implementing this
one package.

| Class | Replaces (VS) | Responsibility |
|-------|---------------|----------------|
| `AeronauticsCompat` | — | Feature detection (`isLoaded`, `isAeronauticsLoaded`) |
| `AeronauticsConstruct` | `Ship` / `ServerShip` / `LoadedServerShip` | Handle around a Sable `SubLevel`: id (UUID), pose, velocities, bounds, mass |
| `AeronauticsContraptionLookup` | `VSGameUtilsKt.getShipManagingPos`, `getLoadedShips`, `getShipObjectWorld` | Find the construct at a position / all constructs in a level / sorted by size |
| `AeronauticsTransformHelper` | `ship.getShipToWorld()`, `VectorConversionsMCKt` | Local ↔ world coordinate/direction conversion |
| `AeronauticsPhysicsAdapter` | `ship.getVelocity/getAngularVelocity/getInertiaData` | Velocity, orientation, mass, rigid-body handle |
| `AeronauticsForceHandler` | VS force inducers | Apply impulse/force/torque at a world point |
| `AeronauticsMovementHelper` | VS entity dragging | Velocity inheritance, velocity-at-point, world velocity of a point |
| `AeronauticsCollisionHandler` | VS ship-AABB overlap | Constructs overlapping a box / touching an entity |
| `AeronauticsBlockAttachmentHandler` | `ship.saveAttachment/getAttachment` | Is a block/BE part of a construct, and which |
| `AeronauticsTeleportHelper` | `shipWorld.teleportShip(ship, ShipTeleportData)` | Same-dimension pose/velocity teleport; **cross-dimension disassemble→transfer→reassemble** |
| `AeronauticsNetworkingHelper` | VS ship-id networking | Save-/network-safe construct id resolution |

### VS → Sable API mapping (key equivalences)

| Valkyrien Skies | Create Aeronautics / Sable |
|-----------------|-----------------------------|
| `getShipManagingPos(level, pos)` | `Sable.HELPER.getContaining(level, pos)` → `SubLevel` |
| `ship.getTransform().getPositionInWorld()` | `subLevel.logicalPose().position()` |
| `ship.getShipToWorld()` transform | `subLevel.logicalPose().transformPosition/Normal` |
| `ship.getWorldAABB()` | `subLevel.boundingBox()` (`BoundingBox3dc`) |
| `ship.getShipAABB()` | `subLevel.getPlot().getBoundingBox()` (`BoundingBox3ic`) |
| `ship.getVelocity()/getAngularVelocity()` | `ServerSubLevel.latestLinearVelocity/latestAngularVelocity` |
| `ship.getId()` (`long`) | `subLevel.getUniqueId()` (`UUID`) — **save-safe** |
| `ship.getSlug()` | `subLevel.getName()` |
| `shipWorld.getLoadedShips()` | `SubLevelContainer.getContainer(level).getAllSubLevels()` |
| `shipWorld.teleportShip` (same dim) | `RigidBodyHandle.teleport(pos, quat)` + velocity |
| `shipWorld.teleportShip` (cross dim) | *No Sable primitive.* Snapshot plot blocks + BE data → stamp into target level → `SubLevelAssemblyHelper.assembleBlocks` → remove source |
| VS joint graph (`collectConnected`) | Proximity-based clustering via `queryIntersecting` (CA has no ship↔ship revolute joints) |
| `updateDimension(gravity, waterLevel)` | Sable `dimension_physics` datapack (gravity/drag/pressure JSON) |
| explosion force scaling mixin (MixinSquared → VS) | dropped; Sable owns explosion↔sub-level interaction |

### Genesis systems converted onto the compat layer

* **Ship detection / transforms / local↔world** — `RadarDisplayBlockEntity`, `RadarDisplay`,
  `NavProjectorBlockEntityRenderer`, `VoidCoreBlockEntity`, `VoidEngineInterfaceBlockEntity`,
  `ServerLevelMixin`.
* **Dimension travel** — the whole `teleportation` package (`DimensionTravelTeleporter`,
  `ShipCollector`, `ShipTeleporter`, `EntityCollector`, `PlanetToSpaceTeleporter`,
  `SpaceToPlanetTeleporter`, `Util`, `TeleportDisallowedEvent`) now operates on
  `AeronauticsConstruct` and routes teleports through `AeronauticsTeleportHelper`.
* **Construct mass/rotation queries, force/torque** — exposed through `AeronauticsPhysicsAdapter`
  / `AeronauticsForceHandler` (ready for machine behaviours that push a construct).
* **Save/load safety** — Genesis never stores a live construct, level or player. Cross-network and
  cross-save references use the construct **UUID**, block positions, and resource locations only.

## 2. Forge 1.20.1 → NeoForge 1.21.1 migration

| System | Change |
|--------|--------|
| Mod init | `@Mod` ctor now takes `(IEventBus, ModContainer)`; config via `ModContainer.registerConfig` |
| Registries | `DeferredRegister.create(ForgeRegistries.X)` → `Registries.X`; typed `DeferredRegister.Blocks/Items`; `RegistryObject` → `DeferredHolder`/`DeferredBlock`/`DeferredItem` |
| Events | `net.minecraftforge.*` → `net.neoforged.*`; `TickEvent.*` → `LevelTickEvent.Post` / `ClientTickEvent.Post`; `@Mod.EventBusSubscriber` → `@EventBusSubscriber` |
| Networking | `SimpleChannel` → NeoForge payload system (`CustomPacketPayload` + `RegisterPayloadHandlersEvent` + `StreamCodec`); clientbound handlers isolated in `networking.client` (server-safe) |
| Block entities | `load/saveAdditional` now take `HolderLookup.Provider`; capabilities via `RegisterCapabilitiesEvent` (`Capabilities.EnergyStorage/ItemHandler`), no more `LazyOptional`/`getCapability` override |
| Blocks | `Properties.copy` → `ofFullCopy`; `FallingBlock`/`BushBlock` abstract → concrete `GenesisFallingBlock`/`GenesisBushBlock`; custom blocks override `codec()`; `use` → `useWithoutItem`; `NetworkHooks.openScreen` → `player.openMenu`; `StairBlock(BlockState,…)`; `DropExperienceBlock(IntProvider,…)`; `isPathfindable`/`playerWillDestroy` signatures |
| Items / armour | `ArmorMaterial` is now a record (`SpaceArmourMaterial.HOLDER`); item NBT → `DataComponents.CUSTOM_DATA` |
| Paintings | `PaintingVariant(w,h,assetId)`; creative-tab paintings via `CustomData` + `DataComponents.ENTITY_DATA` |
| Menus / screens | `MenuScreens.register` → `RegisterMenuScreensEvent`; `IForgeMenuType` → `IMenuTypeExtension` |
| SavedData | `save`/`load` take `HolderLookup.Provider`; `computeIfAbsent(SavedData.Factory, name)` |
| Fluids | `ForgeFlowingFluid` → `BaseFlowingFluid`; `LiquidBlock(FlowingFluid, …)` |
| Worldgen | density functions register `MapCodec` (not `Codec`) via `RegisterEvent`; surface rules likewise |
| Codecs | `Codec.dispatch/dispatchStable` sub-codecs are now `MapCodec` |
| Rendering | vertex builder `vertex/color/uv/normal/endVertex` → `addVertex/setColor/setUv/setNormal` (+ no `endVertex`); `Tesselator.getBuilder()`+`begin` → `Tesselator.begin`; `BufferBuilder.RenderedBuffer` → `MeshData` |
| Entity data | `entityData.define` → `SynchedEntityData.Builder.define` in `defineSynchedData(Builder)` |
| Data folders | `recipes`→`recipe`, `loot_tables`→`loot_table`, `structures`→`structure`, `tags/blocks`→`tags/block`, `forge`→`neoforge` |

## 3. Dependencies

Removed: `valkyrienskies`, `vs-core`, `vlib`, `zps`, `zpl`, `kotlinforforge`, `architectury`,
`curios`, `radios` (VS-ecosystem or unused-in-code).

Added / retained (all versions verified to exist on their mavens):
`neoforge 21.1.234`, `create 6.0.10-280`, `sable 2.0.3`, `simulated 1.3.0`, `aeronautics 1.3.0`,
`registrate MC1.21-1.3.0+67`, `veil 4.1.4`, `pehkui 3.8.3`, `lodestone 1.8.2`,
`mixinsquared-neoforge 0.3.7-beta.3`, `joml-primitives 1.10.0`, `earcut4j 3.0.0`.

## 4. What was intentionally reduced / dropped (honest disclosure)

* **Bespoke GLSL celestial shaders.** Genesis rendered stars/planets/atmospheres/black-holes with
  a suite of custom GLSL shaders driven through Lodestone's 1.20 shader registry
  (`LodestoneRenderTypeRegistry`, `ShaderHolder`, `LodestoneRenderType`). That entire subsystem was
  rewritten from scratch for Minecraft 1.21's core-shader pipeline and Lodestone 1.8.2, with no
  drop-in equivalent. The celestial bodies are now drawn through the **vanilla pipeline** as
  camera-facing billboards (`space.renderer.SimpleBillboardCelestialRenderer`) — they remain
  visible and correctly positioned; the procedural surfaces/atmospheres and the space colour-invert
  post-process are a documented follow-up. Files retired: `PlanetRenderer`, `StarRenderer`,
  `BlackholeRenderer`, `PlanetAtmosphereRenderer`, `ShaderRegistry` (now a stub), the
  `client/shading/*` shadow projector, `SpaceInvertPostProcessor`, `SpaceShaderEventHandler`,
  `PlanetMaskTarget`.
* **Custom sky star-fields in the dimension effects** (`Space/Planet/Wormhole DimensionEffects`):
  the fog colour / sky type are preserved; the custom `renderSky`/`renderClouds` hooks no longer
  exist on the 1.21.1 superclass, so those dimensions fall back to vanilla sky rendering.
* **VS-only mixins** (`VSExplosionForceMixin`, `VSMountedCameraMixin`, `VSBlockItemMixin`,
  `EntityDraggerMixin`, `MinecraftServerMixin`): they targeted VS's own mixin handlers, which do not
  exist in the CA environment. Removed; the equivalent behaviour is now owned by Sable.
* **Mekanism compat mixins**: Mekanism is no longer a dependency; the two optional compat mixins
  were removed.
* **Ship↔ship revolute-joint teleport clustering**: CA has no analogue to VS joints, so the
  "connected ships teleport together" behaviour now clusters by world-bounds proximity, and the
  corresponding VS hinge GameTest was dropped.
* **Obsolete unit tests** for the removed GLSL shadow subsystem (and a kotlin/Minecraft-classpath
  `SpaceLevelTest`) were removed; the pure-geometry math tests (`OBB`, `Occlusion`,
  `PolygonClipping`) remain and pass.

## 5. Result

`./gradlew build` succeeds and produces `build/libs/genesis-1.21.1-0.8.0.jar` — a valid NeoForge
1.21.1 mod with mixins, access transformer and all assets/data. All gameplay content (blocks,
items, machines, fluids, worldgen, dimensions, teleportation, radar, nav projector, networking,
save/load) is ported and compiles cleanly on both distributions.
