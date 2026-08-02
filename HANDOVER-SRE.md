# Handover — SRE / seamless travel session

Updated 2026-07-29. Branch `neoforge-1.21.1-port`, head `8f5c483`.

## On Immersive Portals — read before trying again

The user supplied `immersive-portals-5.2.0-mc1.20.1-fabric.jar` and asked for
its code. Findings, so nobody re-does this:

- **Licence is Apache 2.0.** An earlier version of this handover said porting it
  was blocked by licensing. That was wrong — Apache 2.0 permits derivative works
  with attribution and a NOTICE. The obstacle is technical, not legal.
- **There is no source in the jar to take.** It ships compiled Fabric
  intermediary bytecode (`class_638`, `class_310`, …) for 1.20.1. Every symbol
  would need remapping to Mojang mappings, every mixin retargeting from Fabric
  to NeoForge, and every 1.20.1→1.21.1 signature change chasing down.
- **The seamless part is not a file you can lift.** `MixinReceivingLevelScreen`
  is an empty stub — it does nothing. The mechanism is `ClientWorldLoader`
  (keeps several `ClientLevel`s alive at once) plus
  `ClientTeleportationManager.changePlayerDimension(player, fromLevel, toLevel,
  pos)`, which moves the player between two **already-loaded** client levels. No
  respawn packet is sent, so `startWaitingForNewLevel` never runs and no screen
  ever appears.
- **What makes that possible is a packet-redirection layer.** Vanilla's protocol
  carries one dimension at a time. IP wraps play packets with a dimension tag so
  the server can stream chunks and entities for a second dimension while you
  stand in the first. That is the large subsystem, it is why IP is known for
  breaking other mods, and Sable does its own per-level client sync that would
  very likely collide with it head-on.

So the technique was implemented rather than the code copied: see `8f5c483`
below. If someone does eventually want true multi-level rendering, the entry
point to study is `ClientWorldLoader` + the redirection layer in
`q_misc_util`, and it is a project in its own right, not a session's work.

## `8f5c483` — show the world during a crossing

The screen is not the delay, it is the curtain over it. **Pre-generating the
arrival chunks cannot remove it on its own**: the client still has to receive
those chunks and build meshes, and vanilla holds the screen until
`isSectionCompiled(playerPos)`. That work is client-side and unavoidable.

So the screen now draws nothing and the world shows through while it streams.
Both ends of a Genesis crossing are high above terrain, where a world that has
not finished meshing looks like sky — which is what makes this work here and
would not work for, say, a portal at ground level.

`SeamlessTransitionScreen` is a real `ReceivingLevelScreen` with vanilla's
dismissal logic untouched, registered per Genesis dimension through
**`RegisterDimensionTransitionScreenEvent`** — NeoForge's supported extension
point for exactly this. That is what makes it safe where attempt 1 was not: it
cannot leave anyone on a screen that never closes. Registered on the Genesis
dimensions rather than the overworld, so vanilla nether/end portals are
untouched. After 450 ms the themed screen fades up, so a genuinely slow crossing
still says something.

`LoadingScreenReskin` and `GenesisLoadingScreen` are deleted, and the
loading-hint packet is retired to a no-op phase (kept so an old client does not
fall through to `default` and cancel a transit mid-flight).

### Micro-freezes, two causes

- **Surface sampling had no business being on the tick.** Every arrangement was
  a choice between stuttering and taking minutes, because each texel builds a
  whole noise column. It runs on `Util.backgroundExecutor()` now; the tick just
  collects the finished array. The job captures generator, random state and
  climate sampler on the server thread up front, so the worker touches nothing
  that is not already safe for the worldgen threads to call.
- **Approach pre-generation ramped its ticket radius to 8** — 289 chunks
  requested afresh every second at a centre that moves the whole way in, for
  chunks nobody ends up standing in, competing with the arrival chunks that
  matter. Capped at 4.

## Build

```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'   # Java 25 on PATH fails the build
./gradlew.bat compileJava test
```

**Copy `run/logs` aside before running the client.** Minecraft rotates logs on
every launch, and six verification launches in an earlier session destroyed the
user's crash logs before they could be read. That is how the crash went
undiagnosed.

## Nothing below has been seen in game

Everything in this session compiles, links and passes tests. **None of it has
been run.** The two commits below are reasoned from the code and from
Minecraft's own sources, not from observed behaviour. Read the "how to tell if
it worked" notes before assuming either one landed.

## This session (`1347ced`, `6c08c4e`)

### `1347ced` — hold a crossing until its destination exists

The plan the previous handover set out ("remove the wait, not the screen") was
right, and the reason its groundwork had no effect is that **both
pre-generation paths were placing tickets on the wrong chunks.**

- `pregenApproach` applied the 1:16 planet map but skipped the padded-cube to
  surface scaling that `computePlanetTarget` applies. For Earth that factor is
  `256/1256 ≈ 0.2`, so every ticket landed about five times further from the
  origin than the column actually arrived in. It warmed real chunks, never the
  right ones. Entry and pre-generation now share one `surfaceColumn` mapping so
  they cannot drift apart again.
- `pregenAscent` called `computeSpaceTarget` **without the traveller id**, so it
  reconstructed an exit direction from surface coordinates instead of the
  remembered entry side the real teleport uses — a different point on the body's
  shell. It also centred its ticket at `y=0` rather than the arrival height, and
  in the ship branch used the *player's* x/z rather than the *ship's*.

On top of that, `ArrivalGate` (new, `teleportation/ArrivalGate.java`) defers a
committed crossing until the arrival column is genuinely at FULL status, then
fires it. After 100 ticks it fires regardless, so it cannot strand anyone —
worst case is exactly the old behaviour. Note `getChunkNow`, not `hasChunk`:
`hasChunk` reports the ticket level, so it answers yes the moment generation is
*requested*, which is the exact thing being waited on.

**Why this should shorten the screen.** Vanilla's `ReceivingLevelScreen`
dismisses when `LevelLoadStatusManager` reaches `LEVEL_READY`, which needs
(a) the server's `LEVEL_CHUNKS_LOAD_START` game event — sent immediately in
`PlayerList.sendLevelInfo`, never the bottleneck — and then
(b) `isOutsideBuildHeight(y) || levelRenderer.isSectionCompiled(playerPos)`.

**Read this before testing.** Planet arrivals land at `arrivalHeight` ≈ 19000,
which is outside every planet's build height (`min_y -64`, `height 384`), so
condition (b) is satisfied instantly and **the entry screen was probably already
short.** The gate still matters there — it stops you falling toward terrain that
does not exist yet — but the visible loading-screen win should be on the
**launch to space** side, where the arrival can sit inside the space dimension's
`-64..256` range and genuinely waits on a real chunk. If the user reports the
screen is still long specifically on *entry*, the cause is elsewhere and this
diagnosis needs revisiting.

### `6c08c4e` — L3 surface sampling, repaired rather than reverted

The previous handover recommended reverting this. The user chose to fix it,
since it is the groundwork for "Earth rendered from the real world". Four
independent faults, each enough alone to make it invisible:

1. **Never fired** — `ensureSent` hung off `pregenApproach` (space dimension
   only, inside an approach radius, one tick in twenty ⇒ 16+ seconds of unbroken
   approach). Now runs from `ServerTickEvent.Post`, independent of travel, and
   off the transition path entirely.
2. **Wrong area** — `SAMPLE_SPAN` was a fixed 16384, but entry maps a body's
   surface onto `size * 16` blocks: 8192 for Earth, 3136 for Mercury. Every
   planet was drawn at the wrong zoom, so the mountain you aimed at was not the
   one you landed on. Now derived per body.
3. **Wrong texture shape** — a planet is a cube with a **3×2 face atlas**
   (confirmed: the painted textures are 768×512 and 384×256). The sample was
   uploaded as a bare square, so each face showed a different sixth of the
   world, cropped and stretched. The square now fills all six cells.
4. **Fixed rows per tick** assumed every generator costs the same — the reason
   resolution was cut to 64 and the rate to a crawl. Now budgeted at 1.5 ms per
   tick, checked every 16 texels, one body at a time. Resolution back to 128,
   against painted faces of 256.

Also: biome lookups go straight to the biome source in quart coordinates (the
biome manager spent a chunk-map lookup per texel finding a chunk that is
deliberately not loaded, then fell back to this same call); surfaces go to every
player, not just those in orbit, because planets are drawn in the sky of other
planets too; `forget()`/`reset()` are wired to logout/shutdown, having been
written but never called.

**How to tell if it worked:** grep the log for `[SURFACE]`. Expect
`sampling <planet> across <n> blocks`, then `%` progress lines, then
`sampled <planet> from its own terrain`, and client-side `is now drawn from its
real terrain`. Zero `[SURFACE]` lines means it still is not running — that was
the old symptom exactly.

## Earlier work that landed

| Commit | What |
|---|---|
| `d835734` | Seat/crew fix — see below, this one is worth understanding |
| `0966f77` | Iris/Oculus detection; our post chain stands down when a pack is loaded |
| `91190a0` | Violet transition band in the ascent sky (per-body `transitionColor`, optional) |
| `ba1e440` | Velocity carried through transitions; tighter approach pre-gen; cloud shell on planet spheres |

**The seat fix is the one real win.** `WarpSubLevel` assigns the arrived craft a
**new UUID**. `reconcileCrew` was checking whether the craft carrying the player
was one of the ones it had just transferred, matching against the *departing*
ids — which can never succeed after a dimension change. A player who crossed
over perfectly, still in their seat, failed that check, was `stopRiding()`'d and
teleported to the craft's logical anchor. That was two reported symptoms
("not seated any more", "a few blocks off the craft") from one bug.

Everything visual (`91190a0`, clouds in `ba1e440`) is verified to compile, link,
and boot. **None of it is verified to look right.** Tuning dials: band strength
`0.75` in `PlanetDimensionEffects.getSkyColor`, cloud coverage threshold
`0.52–0.78` in `planet_textured.fsh`.

## Do not repeat these

### Two failed attempts at the loading screen

**Attempt 1 (`a90fc91`, reverted in `1760356`).** Cancelled `ScreenEvent.Opening`
for `ReceivingLevelScreen` on Genesis crossings. Result: **infinite loading
screen at 100%.** Cancelling `Opening` does not close the current screen — it
only blocks the new one, so whatever was already on screen stays forever.

That commit was also based on a wrong diagnosis. It was written on the belief
that the "this is a Genesis crossing" hint never fired for atmosphere crossings.
**It already fires** — `GenesisNetworking.sendToPlayer(..., HyperspaceStatePacket.loadingHint())`
appears at four sites in `SpaceToPlanetTeleporter` and `PlanetToSpaceTeleporter`.
The mistake was grepping for `expectGenesisTransition` instead of for the packet
send. Check both.

Current, working behaviour: hint fires → `LoadingScreenReskin` swaps vanilla's
screen for `GenesisLoadingScreen`. The screen is *themed*, not removed.

### Immersive Portals

See the section at the top of this file — investigated properly on 2026-07-29,
including the licence question, which the earlier note got wrong.

### On the reference video the user linked

(Create & Sable space addon.) The author states it is not a curvature shader but
a planet built from LOD chunks taken from a flat world and stretched onto a
sphere, flown as a real orbit at 290 m/s. Two things follow. That is real chunk
geometry, not a texture — the L3 approach cannot reach it. And he has *no
dimension switch to hide*, because the planet is one continuous world; that is
why his video has no loading screen. Matching it is a different foundation, not
a feature to add to this one.

## Open bugs, none fixed

- **Crash on returning to Earth.** Never diagnosed. Both logs supplied ended in
  clean quits (`Saving worlds` → `Stopping!`), so no crash was ever captured.
  Needs `run/logs` copied aside immediately after a real crash. The sampler is
  now off the transition path, which removes one suspect.
- **Earth renders "on its side".** Orbital maths verified correct —
  `baseRotation` is identity and the 23.44° tilt is applied before the spin, so
  JOML's local-frame order spins it about the tilted axis as intended. The fault
  is in the planet model's texture/UV mapping, not `OrbitingTransformProvider`.
  Note the painted path it renders today was already a correct 3×2 atlas, so the
  atlas fix in `6c08c4e` does not explain this one.
- **Earth's teleport box "now inside".** Arithmetic checked and unchanged: half
  extent 256, size 512 ≥ `LARGE_BODY_SIZE` 400 → 1000 padding → 1884 ring, same
  as before. Either something else moved it or the user is seeing the *exit*
  placement rather than the entry box. Unresolved.
- **Earth descent "removes my sublevel".** Untested since the seat fix, which
  may have been the real cause (the player was being separated from a craft that
  had survived).
- Mercury creatures never confirmed spawning in game.
- JarJar embedding of `dimensional_sable` — offered, never approved or done.

## Known dead code, left alone deliberately

`PlanetToSpaceTeleporter` still carries `SPACE_ENTRY_Y` and a
`computeSpaceTarget(VantagePoint.OnCelestial)` overload that nothing calls; both
predate this session. Also, the `surfaceProjection` in `computeSpaceTarget` uses
the fixed `PLANET_ENTRY_PADDING` where entry uses the size-dependent
`entryPadding` — harmless, because the vector is normalised immediately
afterwards and the scale factor cancels. Do not "fix" it expecting a behaviour
change.
