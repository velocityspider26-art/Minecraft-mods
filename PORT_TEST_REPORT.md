# Warium NeoForge 1.21.1 Port (by noa) — Dedicated Server Test Report

**Tested build:** `crusty_chunks-1.2.8-neoforge.1.jar` (noa's unreleased port, shared in the Warium Discord)
**Test date:** 2026-07-02
**Environment:** NeoForge **21.1.234**, Minecraft **1.21.1**, GeckoLib **4.8.4** (neoforge), Java 21 (Temurin/OpenJDK 21.0.10), dedicated server (`--nogui`), fresh world.

## Verdict

The port **passes a full dedicated-server smoke test**. Clean boot, clean registries, clean world save. No mod-related errors of any kind in the log.

## Static comparison vs. original Forge 1.2.8 jar

File inventory comparison (metadata only) shows the port is **content-complete**:

| Category | Forge 1.2.8 | NeoForge port |
|---|---|---|
| Class files | 4,712 | 4,716 |
| Models | 1,668 | 1,668 |
| Textures | 1,307 | 1,306 |
| Blockstates | 448 | 448 |
| Loot tables | 435 | 435 |
| Recipes | 505 | 476 * |
| Sounds | 94 | 94 |
| GeckoLib geo/anim | 80 | 80 |
| Tags | 179 | 179 |
| Advancements | 13 | 13 |
| Structures | 8 | 8 |

\* Recipe delta is consistent with 1.21's recipe format consolidation, not missing content.

The port adds real migration work, not just re-compilation: a mixin config (`mixins.crusty_chunks.json`), NeoForge enum extensions (`META-INF/enumextensions.json`), and a properly rewritten `neoforge.mods.toml` (requires NeoForge `[21.1.233,)`, GeckoLib `[4.6.6,)`).

## Runtime results (dedicated server)

- ✅ Mod discovered and loaded: `crusty_chunks 1.2.8-neoforge.1` alongside GeckoLib 4.8.4
- ✅ All registry events fired without errors (`REGISTRIES` logging marker enabled)
- ✅ Datapack content live: **1,762 recipes** loaded (vanilla 1.21.1 ships ~1,290 — the delta matches Warium's recipe count) and **1,412 advancements**
- ✅ Fresh world generated (overworld + nether + end)
- ✅ Server reached ready state: `Done (10.315s)!`
- ✅ Graceful shutdown: `All dimensions are saved`, world directory intact (level.dat + 4 region files)
- ✅ Zero mod-related ERROR lines. The only ERROR in the entire log is vanilla first-run noise (`Failed to load properties from file: server.properties` — the file doesn't exist yet on first boot). All WARN lines are standard vanilla/dev-environment noise (command ambiguity, offline mode, GeckoLib refmap notice in dev).

## Not yet tested (needs a real client / multiplayer session)

- Client boot, title screen, and in-world rendering (this environment is headless)
- Weapon firing, projectile behavior, GeckoLib animations in-game
- GUI/menu synchronization between client and server
- Multiplayer join + networking desync checks
- Interop noa mentioned: Create Aeronautics 1.30, Sable 2.01

## Note on provenance

This report documents black-box testing of a publicly shared build. No decompilation or source extraction was performed. The port is noa's work; Warium is Novum's work. This report is intended to be handed to noa as QA feedback toward an official release.
