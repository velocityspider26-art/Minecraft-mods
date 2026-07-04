# Testing checklist — Warium NeoForge 1.21.1 port

Environment: NeoForge 21.1.234 dev environment (ModDevGradle), Java 21, headless Linux container.

## Verified in this port

- [x] Gradle project imports and configures (`./gradlew` tasks resolve, NeoForge + GeckoLib dependencies fetched from real Mavens)
- [x] `./gradlew build` succeeds; output jar `build/libs/crusty_chunks-1.2.8.jar` produced (12.8 MB, asset-complete)
- [x] Jar contains valid `META-INF/neoforge.mods.toml`, `enumextensions.json`, `pack.mcmeta` (1.21.1 formats)
- [x] All 1,882 source classes compile against NeoForge 21.1.234 with zero errors
- [x] Dedicated server (`runServer`) reaches "Done!" — mod constructs, all `@EventBusSubscriber` classes register
- [x] All registries populate without errors: 448 blocks, 297+ items, 127 entity types, 97 block entity types,
      15 menus, 24 sounds, 45 particle types, 11 fluids + types, 4 mob effects, 7 creative tabs, attachment types
- [x] Networking payloads register (`RegisterPayloadHandlersEvent`) — keybind messages, GUI buttons, variable sync
- [x] Entity attributes and spawn placements register (`EntityAttributeCreationEvent`, `RegisterSpawnPlacementsEvent`)
- [x] Block entity item/energy/fluid capabilities register (`RegisterCapabilitiesEvent`)
- [x] Chunk-load ticket controller registers (`RegisterTicketControllersEvent`)
- [x] Datapack loads: recipes (505 vanilla-format), tags (block/item/entity/damage/fluid), loot tables,
      advancements, damage types, worldgen (configured/placed features, structures, template pools, structure sets)
- [x] Biome modifiers apply (`neoforge:add_features` ores, `neoforge:add_spawns` raid scouts)
- [x] New world creation: overworld generates with the mod active
- [x] World reopen: second server boot on the same world loads (save data survives restart; boot in ~2.6 s)
- [x] Game rules register (enrichmentTime, allowImpactFuze, strategicWeapons, apocalypseMode,
      bulletDamageMultiplier, wariumApocalypseDynamicProduction)
- [x] Server log audit: no unexpected exceptions. Remaining log lines are identical-to-original behavior:
      custom `crusty_chunks:assembly` recipes (parsed by the mod itself) and optional `create:pressing`
      recipes rejected by the vanilla recipe manager
- [x] Dedicated server does NOT load client classes (arm-pose EnumProxy holders isolated; client
      renderers/screens/keybinds registered only under `Dist.CLIENT`)
- [x] Client boot (see below)

## Needs interactive verification (recommended before release)

- [ ] Fire every weapon class in a real client (fire rate/recoil/spread feel)
- [ ] Multiplayer combat sync on a public server (2+ real clients)
- [ ] Vehicle/turret riding (seat entity) edge cases across dimension changes
- [ ] Full machine chains (blast furnace → foundry → assembly depot production lines)
- [ ] Nuke/fusion detonation performance on large worlds

## Round 2 — weapon stability & Create: Aeronautics migration (verified via RCON-driven dedicated server)

- [x] Root cause of world corruption found and fixed: 1.21.1 `AbstractArrow.addAdditionalSaveData`
      throws on empty pickup stacks; all 110 projectile constructors now pass real projectile items
- [x] `save-all flush` succeeds with live bullets, bombs, missiles, torpedo and blast entities in flight
- [x] Server restart on that world: loads clean, no ticking-entity crashes, no save errors
- [x] Transient entities verified: a saved `large_ap_bullet` does NOT survive restart (no stale bullets,
      no explosion-animation replay); a saved `small_bomb_projectile` DOES survive (count: 1)
- [x] Nuclear bomb dropped on a stone platform: detonates, destroys blocks through the capped
      explosion wrapper, full effect sequence runs, follow-up `save-all flush` clean
- [x] Explosion effect entities self-discard and are never saved (fixes replay on chunk re-entry)
- [x] Mod builds and boots with no Sable/Aeronautics installed (compat is compileOnly + runtime-gated)
- [x] Config file generates with projectile/explosion/aeronautics sections
- [x] Client boots after all changes
- [ ] In-game verification on a real Aeronautics craft (fire from moving craft, recoil pushback) —
      needs an interactive client with Sable + Create + Create: Aeronautics installed
