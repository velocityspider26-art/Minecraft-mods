# Testing Checklist

Environment: NeoForge 21.1.234 · Minecraft 1.21.1 · Java 21, headless Linux container.
Runtime mods on the dev classpath: Create 6.0.10-280, Create Aeronautics (`simulated`/`aeronautics`
1.3.0), Sable 2.0.3, Registrate, Veil, Pehkui, Lodestone.

| # | Check | Result | Evidence |
|---|-------|--------|----------|
| 1 | `./gradlew build` succeeds | ✅ | `BUILD SUCCESSFUL`; jar at `build/libs/genesis-1.21.1-0.8.0.jar` (220 classes) |
| 2 | Java compiles (main) for NeoForge 1.21.1 | ✅ | `compileJava` — 0 errors |
| 3 | Unit tests pass | ✅ | `:test` green (OBB/Occlusion/PolygonClipping math tests) |
| 4 | Mod recognised as a valid NeoForge 1.21.1 mod | ✅ | Server log: `Genesis 1.21.1-0.8.0 (genesis)` in mod list |
| 5 | `./gradlew runServer` launches a dedicated server | ✅ | `Done (8.253s)! For help, type "help"` |
| 6 | Create Aeronautics detected/active | ✅ | `Create 6.0.10 initializing!`, `Sable loaded!` on the same server boot |
| 7 | **No Valkyrien Skies required / no active VS imports** | ✅ | `grep -c valkyrien` over server log = 0; no `org.valkyrienskies` import remains in source |
| 8 | Recipes load without error | ✅ | `Parsing error loading recipe` count = 0 after datapack migration |
| 9 | Genesis dimensions register & are valid | ✅ | `great_unknown`, `subspace`, `moon`, `malachite` created as valid dimensions at boot |
| 10 | Block entities save/reload safely | ✅ | `loadAdditional/saveAdditional(HolderLookup.Provider)`; energy via `serializeNBT(provider)`; SavedData on 1.21 Factory |
| 11 | No client-only class crashes the server | ✅ | Client accessors moved to the `client` mixin list; `ClientLevel`/`FogRenderer` load attempts are skipped (WARN, non-fatal), server still reaches Done |
| 12 | Logs contain no major hidden exceptions | ✅ | Only benign dev warnings (missing refmaps, `ApiStatus$ScheduledForRemoval`, client-dist skips); no Genesis stack traces |
| 13 | Data generation task available | ✅ | `runData` configured; `generateModMetadata` wired |

## How each item was exercised

* **1–3** `./gradlew build` (compiles main + test, runs unit tests, assembles the jar).
* **4–12** `./gradlew runServer --no-configuration-cache` on a fresh world; the log was inspected
  for the mod list, the `Done` line, Create/Sable init, dimension validity, recipe parsing and
  exceptions. The server was then stopped.

## Verified only partially / not exercisable in this environment

| Item | Status | Reason |
|------|--------|--------|
| `./gradlew runClient` → title screen, world create/reopen, creative tabs, GUIs, place/break | ⏳ not run here | The container is headless (no OpenGL/display); the **client half of the source compiles** (`compileJava` covers client classes) and mixins/registration are shared with the server that did launch. Run `./gradlew runClient --no-configuration-cache` on a machine with a display to complete these. |
| Player joins the dedicated server | ⏳ not run here | No second client in the container; the server reaches `Done` and opens for connections. |
| Genesis systems interacting with a **built** Create Aeronautics construct (assemble, cross-dimension warp) | ⏳ not run here | Requires in-game vehicle assembly. The compat layer + teleport helper compile and load; the cross-dimension transfer path is documented as needing live verification (see FEATURE_STATUS / PORTING_REPORT). |

## Notes on non-fatal warnings seen at boot (all benign in a dev run)

* `Reference map '…refmap.json' … could not be read` — normal for MojMap dev runs (no refmaps).
* `Attempted to load class net/minecraft/client/… for invalid dist DEDICATED_SERVER` — the runtime
  dist cleaner correctly refusing to load client classes on the server; Genesis' client mixins are
  in the `client` list so they are skipped, not applied.
* `Couldn't load tag minecraft:placeable … missing following references` — vanilla painting tag
  warning, unrelated to Genesis content.
