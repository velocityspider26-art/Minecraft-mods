# VS Genesis — master brief

Paste this whole file into a fresh session before asking for any code.
Written 2026-07-30. Supersedes `MASTER-PROMPT.md` (older, thinner).
Companion files: `PLAN.md` (staged roadmap), `HANDOVER-SRE.md` (older history),
`patched-mods/README.md` (Immersive Portals patch).

---

## 1. What this is

**VS Genesis** — a space mod for Minecraft **1.21.1 / NeoForge**, at
`C:\Users\veloc\IdeaProjects\Project-Starbound`, branch `neoforge-1.21.1-port`.

You fly a **Sable** physics ship (a real moving sub-level, not an entity) from
a planet up through the atmosphere into a space dimension, where planets are
rendered as textured cubes on real orbits, and down into another planet's
dimension. Earth is `minecraft:overworld`. Space is `genesis:great_unknown`,
which runs at a **1:16 scale** to the planet dimensions.

**The goal, in one sentence: crossing between space and a planet should feel
like flying, not like loading.**

### Build

```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'   # Java 25 on PATH fails the build
./gradlew.bat compileJava test
```

**Copy `run/logs` aside before launching the client.** Minecraft rotates logs on
every launch, and a batch of verification launches has already destroyed crash
logs before anyone read them.

### Key dependencies

| Mod | Role | Notes |
|---|---|---|
| **Sable** | Physics sub-levels — the ships | Load-bearing. Nothing may break it. |
| **Dimensional Sable** | Cross-dimension craft warp | Reached reflectively via `DimensionalSableBridge` |
| **Zero Point Systems (zps)** | Materials, moon ore | **`type="required"`** in `neoforge.mods.toml` — see §5 |
| Create + addons, Veil, Lodestone, Pehkui | Various | |

---

## 2. The Sub-Dimensions idea — the core architecture

**This is the user's concept and it is the organising idea of the whole
project. Understand it before proposing anything.**

> Vanilla dimensions are like separate houses. The nether and the end are
> different buildings — going there means leaving one and entering another.
> Genesis dimensions should be **rooms in one house**. You are still in the
> same building; you are just walking into another room.

That is not a metaphor for flavour. It is a technical claim with a precise
payoff, and it is the reason the biggest performance win in this project
exists.

### Why it matters

Changing dimension in vanilla is expensive because **the destination could be
any shape**. The client throws away its entire render section grid and rebuilds
it — `Minecraft.setLevel` → `LevelRenderer.allChanged` → `ViewArea.createSections`,
allocating one OpenGL buffer per section. At render distance 12 in a 320-tall
world that is **12,500 `glCreateBuffers` calls on the render thread**, and it
measured as a **1.5–2.5 second freeze on every crossing**.

Rooms in a house **share a floorplan**. If the destination is guaranteed to have
the same shape, none of that teardown is necessary — the grid that is already
standing still fits.

### How it is implemented

- **`SubDimensions`** (`shipwrights.genesis.dimension`) declares which worlds are
  rooms: `great_unknown`, `subspace`, `mercury`, `moon`, `malachite`, and
  `minecraft:overworld` (Earth). Membership is *intent*.
- **`sameFloorplan(from, to)`** checks the geometry actually matches
  (`getMinBuildHeight`, `getSectionsCount`). Intent is never trusted alone — a
  datapack body with a different height falls back to vanilla's rebuild, which
  is slow but correct.
- **`LevelRendererViewAreaMixin`** redirects the two expensive calls in
  `allChanged`: skips `releaseAllBuffers()` and returns the standing `ViewArea`
  re-pointed at the new level instead of building a new one. Everything else
  vanilla does — occlusion reset, camera reposition — is untouched and is what
  makes the sections recompile.
- **`genesis:great_unknown` was changed from `height 320` to `384`** so space
  matches the planets and the overworld. Without that the section counts differ
  and the reuse can never engage.

**Status: working, confirmed in a real session log.**

```
[VIEWAREA] reused 15000 sections crossing into minecraft:overworld
[VIEWAREA] reused 15000 sections crossing into genesis:great_unknown
```

…and no watchdog stall on the crossing that followed.

### The rule for extending this

Any new Genesis dimension must use **`min_y: -64, height: 384`** and be added to
`SubDimensions.ROOMS`. A room with different foundations is a different house
and silently loses the optimisation.

---

## 3. Established facts — do not re-litigate these

Each of these was settled from the actual Minecraft source or a real log. Several
were argued wrongly first, at the cost of whole sessions.

**A single dimension cannot hold this design.**
`DimensionType`: `Y_SIZE = (1 << BITS_FOR_Y) - 32` = **4064**, `MAX_Y` 2031,
`MIN_Y` −2032, enforced by the codec *and* an explicit `IllegalStateException`.
`BITS_FOR_Y` is `BlockPos`'s packed-long Y field — a serialisation constraint,
not a setting. Space at y=20000 can never contain blocks in the same dimension
as a planet surface. **Sub-dimensions are the answer, not one big world.**

**Entities above the build height are lit normally.**
`SkyLightSectionStorage.getLightValue` returns `15` above the top section. They
do **not** render black. Only *blocks* are limited to the Y range. (An earlier
claim in this project's history said otherwise; it was wrong.)

**The loading screen is structural.**
`ServerPlayer.changeDimension` → `ClientboundRespawnPacket` → the client
discards its `ClientLevel` and builds a new one → `startWaitingForNewLevel`
opens `ReceivingLevelScreen` → it closes when
`isOutsideBuildHeight(y) || levelRenderer.isSectionCompiled(playerPos)`.
It exists because the vanilla client holds exactly **one** `ClientLevel`.
Pre-generating chunks server-side cannot remove it — the client still has to
receive and mesh them.

**Planet arrivals land at y≈19000, outside the build range**, so
`isOutsideBuildHeight` is already true and the *entry* screen dismisses almost
instantly. The screen that is actually felt is the **launch to space**.

**Chunk loading is not the bottleneck.** `ArrivalGate` holds a crossing until
the arrival chunks are genuinely at FULL status, and it completes in
**~100 ms**. Do not spend effort here.

**Immersive Portals conflicts with Sable, and the conflict is solved.**
Both redirect `Entity.collide`; Sable wins on priority (1100 vs 1000), and IP's
`injectors.defaultRequire: 1` turned every lost injection into a fatal error.
The patched jar in `patched-mods/` sets it to `0` and drops
`common.collision.MixinPlayer_Collision`. Verified to get past class transform.
**Remaining blocker: IP needs Cloth Config API for NeoForge 1.21.1**, which its
`-all.jar` does not bundle. IP is Apache-2.0, so reuse with attribution is fine.
**It may no longer be needed** — its whole value here was avoiding the
`setLevel` rebuild, which §2 already does at a fraction of the risk.

**Cosmic Horizons has nothing to teach us.** MCreator-generated, ordinary
separate dimensions, plain vanilla teleports, same loading screen as everything
else. Its planet-selector UI is worth looking at; its travel is not.

---

## 4. Current state

### Working and confirmed

- **Sub-dimension render grid reuse** (§2) — the 1.5–2.5 s crossing freeze is gone.
- **`ArrivalGate`** — holds a crossing until destination chunks are FULL, ~100 ms,
  times out after 100 ticks so it can never strand anyone.
- **`SeamlessTransitionScreen`** — a real `ReceivingLevelScreen` with vanilla's
  dismissal logic intact, registered per Genesis dimension through NeoForge's
  `RegisterDimensionTransitionScreenEvent`. Draws nothing for the first 450 ms,
  then fades up a themed screen if the crossing runs long.
- **`FreezeWatchdog`** — see §6.
- Seat/crew preservation across craft transfers (hard-won; do not casually refactor).

### Open

- **A residual crossing freeze under 400 ms–1.5 s**, still undiagnosed. The
  watchdog threshold was 1500 ms and saw nothing; it is now **400 ms**. The next
  reading should name it.
- **Planets from real chunk geometry** — `PlanetSurfaceData` already carries real
  terrain height per column to the client. Next step: subdivide each planet cube
  face into a grid and displace vertices along the face normal by real height,
  reusing the existing planet shader. This is where terrain stops being a picture
  of terrain.
- **Planet surface sampling is OFF by default** (`planetSurfaceSampling`). It is
  cosmetic, it succeeds only for the overworld (Mercury and the Moon have
  generators far too slow and abandon), and it has been implicated in three
  freezes. Leave it off until the transition is known good.

---

## 5. Traps — every one of these has already cost a session

**Do not cancel `ScreenEvent.Opening`** to remove a loading screen. Cancelling
blocks the *new* screen without closing the *current* one, leaving a permanent
loading screen at 100%. Use `RegisterDimensionTransitionScreenEvent` instead.

**Do not put background work on `Util.backgroundExecutor()`.** That is
Minecraft's worldgen pool. Three planet samples submitted to it starved chunk
generation so badly the game hung on the respawn button.

**Do not diagnose a freeze from correlation.** Three separate hypotheses were
argued from "this was running nearby" and all three were wrong. The real cause
was found in one reading once the watchdog existed. Use §6.

**Do not remove `zps` — it is a required dependency.** `neoforge.mods.toml`
declares `type="required"`, the moon's lithium ore worldgen places
`zps:deepslate_lithium_ore`, and eight recipes use `zps:aluminum_ingot`,
`zps:space_metal_mesh` and `zps:void_caution_block`. Removing the Gradle line
alone stops the game booting. **Before removing any dependency, check
`neoforge.mods.toml` and `src/main/resources/data/**` — not just `build.gradle`
and the Java sources.**

ZPS *does* cost **21.7 s on every world load** — its cable blocks compute
uncached voxel-shape unions and `Blocks.rebuildCache` walks every blockstate
(`WorldLoader.load` → `updateRegistryTags` → `initCache` →
`CableBlock.getOcclusionShape`). That is a **world-load** cost, **not** a
travel cost, and it was briefly and wrongly blamed for the space freeze. The
correct fix is a mixin caching `getOcclusionShape` per `BlockState`, which keeps
the mod and its content.

**Set `require = 0` on redirects into volatile vanilla methods.** A failed
injection is fatal at class-transform time — the game will not launch. Degrading
to vanilla behaviour is always better than refusing to boot.

**Do not port Immersive Portals wholesale.** Its seamlessness comes from keeping
multiple `ClientLevel`s alive plus a packet-redirection layer so one connection
can carry two dimensions. That layer is the genuinely multi-week part and is why
IP is known for breaking other mods.

---

## 6. Diagnostics — use these instead of guessing

**`FreezeWatchdog`** (client) — a max-priority daemon watching a heartbeat the
client tick updates. If the client stops ticking for **400 ms**, it dumps every
thread's stack **with lock ownership** to the log. This is how the real freeze
was found after three wrong guesses. Never argue about a freeze without a
reading from it.

Log tags to grep:

| Tag | Meaning |
|---|---|
| `[WATCHDOG]` | A client stall, with full thread dump |
| `[VIEWAREA]` | Render grid reused — sub-dimension optimisation engaged |
| `[GATE]` | `ArrivalGate` holding/releasing a crossing |
| `[ENTRY]` / `[LAUNCH]` | A crossing decided server-side |
| `[SURFACE]` | Planet surface sampling |
| `[RETRACK]` / `[WARP]` / `[FOLLOW]` | Sable craft transfer and visibility |

Standard check after a test run:

```bash
grep -E "\[WATCHDOG\]|\[VIEWAREA\]|\[GATE\]" run/logs/latest.log
```

`spark` is installed. `/spark profiler start --timeout 60` is available if a
problem is a slow burn rather than a discrete stall.

---

## 7. How to work on this

The user is new to Java and Minecraft modding — **explain the why, not just the
what.** They are a good judge of architecture (the sub-dimensions idea is
theirs, and it was correct) and they will push back on hand-waving, rightly.

Rules that have been learned the hard way here:

1. **Measure before changing.** Every unmeasured fix in this project's history
   made things worse or missed.
2. **State confidence honestly.** Say "the log proves X" or "I think X and here
   is how to check" — never blur the two.
3. **Verify, then report.** Compiling is not working. Working means a log line
   or a watchdog reading.
4. **Own errors plainly and move on.** Several claims here were wrong and were
   corrected; that is normal and preferable to defending them.
