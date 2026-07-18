# Warium 1.2.8 — Forge 1.20.1 → NeoForge 1.21.1 Porting Report

## Source recovery

Warium is distributed as a compiled jar only (no public source). The port was made
from the official **Warium 1.2.8** Forge 1.20.1 jar:

1. All 4,712 class files were decompiled with Vineflower 1.10.1.
2. Minecraft member references (SRG names, `m_12345_`/`f_12345_`) were remapped to
   official Mojang names using MCPConfig 1.20.1 + Mojang's official 1.20.1
   mappings (64,225 members mapped, 942 unique tokens in the mod, 0 misses).
3. The result is 1,882 readable top-level Java classes matching what MCreator
   2023/2024 generated for the original author, plus the author's custom code
   (assembly recipe system, custom render pipelines, apocalypse AI, chunkloading).

The mod is MCreator-generated, so the port systematically follows the diffs
between MCreator's `forge-1.20.1` and `neoforge-1.21.1` code generators (and the
Nerdy's GeckoLib plugin templates for both versions) as ground truth.

## Migrated directly (mechanical rewrites)

- **Packages/events**: `net.minecraftforge.*` → `net.neoforged.*` families, mod/game
  bus annotations, `TickEvent.*` → `ServerTickEvent.Post`/`PlayerTickEvent.Post`/
  `ClientTickEvent.Post`/`LevelTickEvent`, `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`.
- **Registries**: `RegistryObject` → `DeferredHolder`/`DeferredBlock`/`DeferredItem`,
  `ForgeRegistries.*` → `BuiltInRegistries.*`/`Registries.*`/`NeoForgeRegistries.*`.
- **ResourceLocation** constructors → `parse`/`fromNamespaceAndPath` (2,594 sites).
- **Item NBT → data components**: every `getOrCreateTag()` read/write (1,446 sites,
  ammo counters, gun state, fire-select modes...) rewritten to
  `CustomData`/`DataComponents.CUSTOM_DATA` with effectively-final-safe lambdas.
- **Capabilities**: block/entity/item handler, energy and fluid capability lookups
  (1,200+ sites) rewritten to the NeoForge `Capabilities.*` block/entity/item
  capability system via a small `WariumCaps` bridge; block entities now expose
  `getItemHandler/getEnergyStorage/getFluidTank` and are registered through
  `RegisterCapabilitiesEvent` (97 block entities).
- **Menus/screens**: `IForgeMenuType` → `IMenuTypeExtension`, screen registration via
  `RegisterMenuScreensEvent`, container capability binding per the 1.21.1 templates.
- **Blocks**: `use` → `useWithoutItem`, `appendHoverText` TooltipContext, BlockSetType/
  WoodType constructor orders, StairBlock direct BlockState, `codec()` overrides for
  FallingBlock/BaseEntityBlock subclasses.
- **Entities**: `SynchedEntityData.Builder`, `finalizeSpawn` signature, removal of
  `PlayMessages.SpawnEntity` client factories and `getAddEntityPacket`, spawn
  placements via `RegisterSpawnPlacementsEvent`, `MobType` removal, scale via
  attributes, `EntityDimensions` finality.
- **Projectiles**: `AbstractArrow` constructor changes, `getDefaultPickupItem`,
  knockback/pierce moved to `doKnockback`/`getPierceLevel` overrides (all ~400
  gun-fire call sites).
- **GeckoLib 4.4 → 4.7.7**: package flattening (`core.*` removal), renderer color
  int signatures, `GeoRenderProvider`/`getGeoItemRenderer` item renderers,
  `EntityTickEvent` animation sync, `RenderUtil`.
- **Models/rendering**: `renderToBuffer`/`ModelPart.render` int-colour signatures,
  Tesselator/BufferBuilder API (`begin`/`addVertex`/`setUv`/`setColor`/`MeshData`)
  for the mod's custom missile/nuke/night-vision render pipelines.

## Rewritten systems

- **Networking**: the `SimpleChannel` stack was rewritten to 1.21 payloads —
  `CustomPacketPayload` records + `StreamCodec` + `RegisterPayloadHandlersEvent`
  (7 keybind messages, GUI button message, player-variable sync, saved-data sync,
  and 2 custom messages embedded in procedures).
- **Player variables**: Forge capability + provider → NeoForge **data attachments**
  (`AttachmentType.serializable`), with identical NBT layout and clone/respawn/
  dimension-change sync semantics. Map/world variables keep `SavedData` names.
- **Armor**: anonymous `ArmorMaterial` implementations → registered
  `ArmorMaterial` records (10 armor sets) with `RegisterEvent`, durability via
  `Item.Properties.durability(Type.getDurability(n))`, custom worn models kept via
  `IClientItemExtensions`, `getArmorTexture` new signature.
- **Arm poses**: `HumanoidModel.ArmPose.create` (Forge) → NeoForge **enum
  extensions**: 25 client-only `EnumProxy` holder classes + `META-INF/enumextensions.json`
  (holders are separate client classes so dedicated servers never load them).
- **Mob effects**: `getCurativeItems` → `fillEffectCures`, boolean `applyEffectTick`,
  `shouldApplyEffectTickThisTick`, client visibility via `RegisterClientExtensionsEvent`,
  effect-expiry procedure via `MobEffectEvent.Remove/Expired`.
- **Game rules**: moved to `FMLCommonSetupEvent` registration (NeoForge rejects
  subscriber classes without handlers).
- **Chunkloading**: `ForgeChunkManager` → NeoForge `TicketController`
  (`WariumChunkLoader`) for torpedo/missile flight chunk loading.
- **Tools**: multitool attribute modifiers → `ItemAttributeModifiers` data component;
  anonymous `Tier` gained `getIncorrectBlocksForDrops`.

## Assets and data

- `assets/` copied unchanged (blockstates, models, GeckoLib geo/animations,
  textures, sounds, particles, GUIs, lang).
- `data/` migrated to 1.21 layout: `recipes→recipe`, `advancements→advancement`,
  `loot_tables→loot_table`, `structures→structure`, tag folders singularized,
  recipe result format (`item`→`id`), advancement icon format.
- Common tags: `data/forge/...` → `data/c/...` and all `forge:` references in
  recipes/tags/code renamed to `c:`.
- Biome modifiers: `data/<ns>/forge/biome_modifier` → `neoforge/biome_modifier`
  with `neoforge:add_features` / `neoforge:add_spawns` / `neoforge:any` types
  (ores, sulfur, bauxite... plus raid-scout spawns).
- The custom `crusty_chunks:assembly` recipe JSONs were kept byte-compatible with
  the mod's own gson parser (only `forge:`→`c:` tag ids updated) and the parser's
  tag detection updated accordingly; loading path updated for the `recipe/` rename.
- `pack.mcmeta` updated to 1.21.1 pack formats; `neoforge.mods.toml` replaces
  `mods.toml`, declaring GeckoLib as required and the enum-extensions file.

## Dependency changes

| Original (Forge 1.20.1) | Port (NeoForge 1.21.1) |
|---|---|
| GeckoLib 4.4.x (forge-1.20.1) | GeckoLib 4.7.7 (`geckolib-neoforge-1.21.1`, Cloudsmith maven) |
| Forge 47.x | NeoForge 21.1.234 |

## Remaining issues

- Startup logs show recipe-manager parse errors for `crusty_chunks:assembly` and
  `create:pressing` recipe types — pre-existing behavior of the original mod
  (custom/optional types unknown to the vanilla loader), harmless.
- The Valkyrien Skies bridge ("Valkyrien Warium"), Ad Astra and Ritchie's
  Projectile Lib integrations are separate mods; their data files are shipped
  unchanged but only take effect if 1.21.1 builds of those mods exist.
- In-game behavior was verified at the level described in `TESTING.md`
  (dedicated server + world lifecycle + registries + datapacks; client boot).
  Deep interactive testing (aiming feel, recoil tuning, multiplayer combat) should
  be done in a real client session.

## Round 2: weapon stability and Create: Aeronautics migration

**World corruption fix.** In 1.21.1, `AbstractArrow.addAdditionalSaveData` calls
`ItemStack.save()` unconditionally, which throws on empty stacks. The port had
passed `ItemStack.EMPTY` as every projectile's pickup stack, so any bullet alive
during a chunk/world save crashed the save. All 110 projectile constructors now
pass their real `PROJECTILE_ITEM`.

**Projectile safety layer** (`compat/WariumProjectileSafety`): per-level live
projectile cap, configurable lifetime, speed clamp, NaN/position sanity discard,
and vehicle/construct velocity inheritance at spawn — implemented once on the
game bus instead of in 110 entity classes.

**Transient effects.** 66 visual/effect/tracer entity classes (muzzle flashes,
blasts, thermal radiation, debris, fast bullets from the `crusty_chunks:bullet`
tag) now override `shouldBeSaved()` to false: chunk reload can no longer replay
explosion animations or resurrect stale projectiles.

**Explosion policy** (`compat/WariumExplosions`): all 60+ `level.explode` sites
route through one wrapper (radius cap, block-damage toggle, construct impulse).

**Networking hardening**: all serverbound weapon/GUI payload handlers validate a
living, non-spectator `ServerPlayer` before acting.

**Valkyrien Skies → Create: Aeronautics.** Warium never had VS code, only VS
integration data (`warium_vs`, `ritchiesprojectilelib`, `vs_mass`) — removed.
New optional integration targets **Sable**, the physics engine under
Create: Aeronautics 1.x for 1.21.1: `compat/AeronauticsCompat` (facade, safe
no-op without Sable) + `compat/SableAdapter` (the only class touching Sable
types: sublevel lookup via `SubLevelContainer.queryIntersecting`, world/local
transforms via `Pose3d`, velocity at point from linear+angular body velocity,
point forces via `QueuedForceGroup`, explosion impulse falloff). Declared as
compileOnly (Modrinth maven + two Jar-in-Jar libs in `libs/`) and optional in
`neoforge.mods.toml`; the mod runs without Sable installed.

**Performance**: per-player-tick ItemStack copies removed from the gun animation
sync; transient entities eliminate saved-entity buildup from sustained fire.

## Round 3: physics-crash hardening, radar, laser designation, missile guidance, cinematic nukes

**World-crash isolation (the core stability fix).** Every one of the 587 `void
execute(...)` gameplay procedures is now wrapped in a fail-safe try/catch that
routes any exception to `WariumSafety.report` (logged once, then suppressed).
A single malformed projectile, an unloaded construct, a NaN raycast, or a
missing owner now degrades to a logged no-op instead of propagating into — and
crashing — the server tick loop or a world save. The projectile safety layer and
the physics adapter are wrapped the same way, so no Warium code path can take
down a world.

**SableAdapter hardened.** Every Sable dereference (`SubLevelContainer`,
`SubLevel.logicalPose()`, `latestLinear/AngularVelocity`, `queryIntersecting`,
`QueuedForceGroup`) is null-guarded and finite-checked; removed sublevels and
null poses yield safe fallbacks. No Sable runtime object is stored beyond a
single call, so a construct unloading/reloading mid-flight cannot leave a stale
reference.

**Load-time projectile safety.** `WariumProjectileSafety.onJoin` (which fires on
world load) discards any projectile that arrives with a non-finite position/
velocity or an absurd altitude, so a corrupt/old save cannot crash the load.
Velocity inheritance is restricted to freshly-fired, saved projectiles (skips
reloads and transient effect entities). A self-collision grace window is added.

**Radar (`AeronauticsRadarHandler`).** Server-authoritative scan/track/resolve:
finds valid targets (the `crusty_chunks:robotarget` tag, players, aircraft in
physics space, optionally projectiles) within `radarRange`, nearest-first,
capped, with optional line-of-sight. State is transient runtime data only, so
locks always clear cleanly on reload. Radar-guided missiles call `bestTarget`/
`resolve`.

**Laser designation (`AeronauticsLaserDesignationHandler`).** Server-side raycast
from a designator (wired to the existing Aimer item's right-click), entity or
block designation with range + line-of-sight validation, keyed by designator
UUID with automatic expiry — nothing invalid is saved or reloaded. Consumed by
laser-guided munitions via `nearestActive`.

**Missile guidance (`AeronauticsMissileGuidanceHandler`).** Guidance types
UNGUIDED / RADAR / LASER / HEAT, clamped turn rate (`missileMaxTurnRate`),
self-collision grace, lost-target grace (radar/laser configurable), and full
null-safety: if the target or designation disappears, guidance quietly stops.
Only a target UUID and last-known point are persisted (stable data), never
runtime references. Wired into the radar-spear, large-radar, seeker-spear and
IR-missile flight procedures.

**Cinematic nuke effects (`WariumNukeEffects`).** Damage/visual split: damage,
block destruction and fallout stay server-side in the existing explosion
procedures; visuals are a staged, layered sequence (flash → fireball →
shockwave ring → dust wall → mushroom stem → mushroom cap → rolling smoke →
settling fallout) scheduled over ~50 ticks via `queueServerWork` and emitted with
`ServerLevel.sendParticles` (auto distance-culled per client). Five per-type
profiles (SMALL_TACTICAL, STANDARD, HIGH_YIELD, DIRTY, THERMOBARIC) drive
fireball size, colours, dust, mushroom shape and shake. Every stage is particle-
capped and scaled by `nukeEffectsQuality` (LOW/MEDIUM/HIGH/CINEMATIC). A
client-only decaying camera shake (`WariumScreenShake`, `Dist.CLIENT`) is driven
by a broadcast `ScreenShakeMessage`; dedicated servers never load it.

**Config** grew radar, laserDesignator, missiles and nukeEffects sections with
all requested knobs (radarRange/scanRate/lockTime/maxTargets/lineOfSight/track
filters; laser range/grace/LOS/targets/updateRate; missile turnRate/unguided
fallback; nuke quality/shake/mushroom/shockwave/particle+block caps/radius
multiplier/radiation).

**Verified (RCON-driven dedicated server):** server boots clean with all systems;
radar/heat/laser guided missiles (radar-spear, large-radar, seeker-spear, IR)
fly with zero recovered errors and save cleanly; a nuke detonation runs the full
staged sequence, craters blocks, and saves clean; restart on that world loads in
~2.9s with zero errors; no per-nuke tick lag. Still requires an interactive
client with the full Sable+Create+Aeronautics stack to visually confirm
cinematic quality, HUD feel, and firing from a real moving aircraft.

## Round 4: Warium 1.3.0 content update + munition launcher

Upstream released Warium 1.3.0 (2026-07-05) while this port tracked 1.2.8. The update was
merged incrementally instead of re-porting from scratch:

- Decompiled + SRG→Mojang remapped 1.3.0 and computed change sets against the pristine
  1.2.8 baseline: **60 added / 35 removed / 252 changed** classes.
- **Changed classes** went through a git `merge-file` 3-way merge (base = 1.2.8 decomp,
  theirs = 1.3.0 decomp, mine = ported tree), preserving every porting fix. 110 merged
  clean; 372 conflict hunks were resolved by tiered rules (spurious-conflict detection,
  "my port of base" ⇒ "port of theirs", literal-swap, registry-entry set merges for init
  classes, capability→attachment and NBT→data-component conversion of taken hunks), the
  last 47 by hand.
- **Added classes** were ported with the same scripted transform batteries used for the
  base port, plus template-analog generation for the GeckoLib musket (item/model/renderer,
  arm-pose enum extension) and the two new block entities.
- **Removed classes** were deleted with registry reconciliation against the 1.3.0 init
  classes (verified: zero missing / zero stale registrations).
- The Forge `SimpleChannel` packets added in 1.3.0 (`ClientExplosionPacket`,
  `WariumSoundEvent`) were rewritten as NeoForge `CustomPacketPayload` records with
  dedicated-server-safe client dispatch.
- Assets/data: 270 added/changed files migrated through the 1.21.1 pipeline (folder
  renames, recipe/advancement schema, `c:` tags, `neoforge:` model loaders); removed
  content pruned; missing-reference tags made optional (`space_dimensions`) or renamed
  (`#c:sand` → `#c:sands`).
- The port's round-3 systems were re-wired onto the new explosion architecture:
  `WariumNukeEffects` is now an integration facade (screen shake broadcast, Aeronautics
  impulses, blast radius multiplier, client particle quality budgets) feeding
  `ExplosionExampleProcedure` / `NuclearExplosionExampleProcedure`, and the three raw
  `level.explode` calls inside `WariumExplosionServerProcedure` route through the capped
  `WariumExplosions` wrapper.
- New content: **Munition Launcher** placeholder block (this port, not upstream) exposing
  the aircraft-only munitions (large/super-large/nuclear/fusion bombs, torpedoes,
  bunker/block busters, IR + large radar missiles) via right-click selection + redstone
  launch, replacing the VS Warium aircraft dependency.
