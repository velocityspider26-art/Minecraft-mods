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

## Round 3 — physics hardening, radar, laser, missile guidance, cinematic nukes (RCON-driven dedicated server)

- [x] Server boots clean with the radar/laser/guidance/nuke systems and the 587 crash-isolated procedures
- [x] Radar-guided missiles (radar_spear, large_radar) fired at zombie targets fly with 0 recovered errors
- [x] Heat-seeking missiles (seeker_spear, ir_missile) fly with 0 recovered errors
- [x] `save-all flush` with 4 guided missiles in flight succeeds
- [x] Nuclear bomb: full staged effect sequence runs, craters a stone platform, saves clean
- [x] No per-nuke tick lag (single one-time chunk-gen spike at world load only)
- [x] Restart on the saved world (missiles + nuke aftermath): loads in ~2.9s, 0 errors, 0 recovered errors
- [x] Every gameplay procedure is crash-isolated: an exception logs once via WariumSafety and no-ops instead of crashing the world
- [x] Laser designation wired to the Aimer item (server-side raycast); missile LASER guidance consumes it
- [x] Client boots with the client-only ScreenShake / nuke-effect code (dedicated server never loads it)
- [ ] Visual quality of cinematic nuke stages, screen-shake feel, radar/laser HUD — need an interactive client
- [ ] Firing mounted guns from a real moving Create: Aeronautics craft (velocity inheritance, recoil pushback) — needs the full Sable+Create+Aeronautics stack in an interactive client

## Remaining known issues / limits

- The Aeronautics integration (velocity inheritance, construct forces, aircraft radar targeting) is implemented and null-safe but was verified only without Sable installed here; behavior with live constructs needs interactive testing.
- Cinematic nuke visuals and screen shake cannot be visually graded in a headless container.
- Assembly/Create recipe "parse error" log lines remain (unchanged original behavior; harmless).
