# Warium — NeoForge 1.21.1 Port

Port of the **Warium** mod (mod id `crusty_chunks`, version 1.2.8) by **Novum** from
Minecraft **Forge 1.20.1** to **NeoForge 1.21.1**.

Warium is a semi-vanilla warfare technology mod adding guns, cannons, artillery,
bombs, rockets, nukes, industrial machines, fluids, and hostile war machines.

- Original mod: https://www.curseforge.com/minecraft/mc-mods/warium (license: Academic Free License v3.0 per the mod's own metadata)
- This port preserves the original mod id, registry names, resource paths, saved-data
  names, and gameplay behavior so existing content (recipes, tags, commands,
  advancements) keeps working.

## Requirements

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.x (built against 21.1.234)
- [GeckoLib](https://modrinth.com/mod/geckolib) 4.7+ for NeoForge 1.21.1 (required)

## Building

```bash
./gradlew build          # produces build/libs/crusty_chunks-1.2.8.jar
./gradlew runClient      # launch a development client
./gradlew runServer      # launch a development dedicated server
```

The first build downloads NeoForge and Minecraft artifacts; later builds are fast.

## Installing

1. Install NeoForge 21.1.x for Minecraft 1.21.1.
2. Drop `crusty_chunks-1.2.8.jar` and the GeckoLib NeoForge 1.21.1 jar into `mods/`.

## Dependencies

| Dependency | Version | Side | Status |
|---|---|---|---|
| NeoForge | 21.1.234+ | both | required |
| GeckoLib | 4.7.7 (`geckolib-neoforge-1.21.1`) | both | required |
| Create (integration recipes) | — | both | optional; 4 pressing recipes load only when Create is present |
| Sable (physics engine used by Create: Aeronautics) | 2.0.3+ | both | optional; enables construct-aware ballistics, recoil and explosion forces |
| Create: Aeronautics | 1.x for 1.21.1 | both | optional; detected through Sable |

## Weapon stability & physics integration

- Projectile save data was hardened: bullets alive during a world save no longer
  crash chunk saving (the 1.21.1 `AbstractArrow` empty-pickup-stack crash).
- Fast bullets, tracers and explosion-effect entities are transient: they are never
  written to disk, so chunk reload cannot resurrect stale projectiles or replay
  explosion animations. Bombs, shells, missiles and torpedoes still persist.
- A global safety layer clamps projectile speed, enforces a configurable lifetime,
  discards NaN/out-of-range projectiles, and caps live projectiles per level.
- Every Warium explosion is routed through one wrapper that applies the configured
  radius cap and block-damage policy.
- Config file: `config/crusty_chunks-common.toml` (projectile lifetime/cap/speed,
  explosion radius cap, block damage toggle, physics-construct coupling).
- With Sable / Create: Aeronautics installed: projectiles inherit shooter construct
  velocity, explosions and recoil push constructs (`AeronauticsCompat` layer);
  without them everything falls back to vanilla behavior. Valkyrien Skies
  integration data was removed; VS is not referenced or required.

## Known issues / notes

- The original mod's custom `crusty_chunks:assembly` recipes are parsed by the mod
  itself (not the vanilla recipe manager), so the vanilla recipe loader logs
  "Parsing error loading recipe crusty_chunks:assembly/..." at startup. This is
  harmless and identical to the behavior of the original 1.20.1 build.
- The 4 `create:pressing` recipes log parse errors when Create is not installed —
  also identical to the original behavior.
- Common tags were migrated from the Forge `forge:` namespace to the NeoForge/1.21
  `c:` namespace (both the shipped tag files and every code/data reference).
- Player variables were migrated from Forge capabilities to NeoForge data
  attachments; world/map variables keep the same saved-data names
  (`crusty_chunks_mapvars`, `crusty_chunks_worldvars`), so existing world data carries over.
- Gun arm poses use NeoForge enum extensions (`META-INF/enumextensions.json`).

See `PORTING_REPORT.md` for a full description of what was migrated and how, and
`TESTING.md` for the verification checklist.
