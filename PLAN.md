# Seamless space travel — staged plan

Written 2026-07-30. The goal: crossing between space and a planet with no
loading screen and no stutter.

The rule for this plan: **no stage starts until the one before it is proven by
evidence, not by argument.** Three freeze diagnoses have already been argued
from correlation and all three were wrong. That is what this ordering is for.

---

## Established facts

Things that are settled, with the source. Do not re-litigate these.

**A single dimension cannot hold this design.**
`DimensionType`: `Y_SIZE = (1 << BITS_FOR_Y) - 32` = **4064**, `MAX_Y` 2031,
`MIN_Y` −2032, enforced by the codec *and* an explicit `IllegalStateException`.
`BITS_FOR_Y` is `BlockPos`'s packed-long Y field, so it is a serialisation
constraint, not a setting. Space at y=20000 cannot contain blocks in the same
dimension as a planet surface at y=64.

**Entities above the build height are fine.**
`SkyLightSectionStorage.getLightValue` returns 15 above the top section, so
they are fully lit. An earlier claim in this repo's history that they render
black was wrong. Only *blocks* are limited to the Y range.

**The loading screen is structural, not cosmetic.**
`ServerPlayer.changeDimension` → `ClientboundRespawnPacket` → the client
discards its `ClientLevel` and builds a new one → `startWaitingForNewLevel`
opens `ReceivingLevelScreen` → it closes when
`isOutsideBuildHeight(y) || levelRenderer.isSectionCompiled(playerPos)`.
The screen exists because the vanilla client holds exactly **one**
`ClientLevel`. Pre-generating chunks server-side cannot remove it; the client
still has to receive and mesh them.

**Planet arrivals land at y≈19000, outside the build range.**
So `isOutsideBuildHeight` is already true and the entry screen closes almost
immediately. The screen that is actually felt is the **launch to space** one,
where the arrival can land inside the range and waits on a real chunk.

**Immersive Portals conflicts with Sable, but the conflict is solved.**
Both redirect `Entity.collide`; Sable wins on priority (1100 vs 1000) and IP's
`defaultRequire: 1` turned every loss into a fatal error. Patched jar in
`patched-mods/` sets it to 0 and drops `MixinPlayer_Collision`. Verified to get
past class transform. **Remaining blocker: IP needs Cloth Config API for
NeoForge 1.21.1, which its `-all.jar` does not bundle.**

---

## RESULT: the crossing freeze is fixed

`LevelRendererViewAreaMixin` works, confirmed 2026-07-30:

```
20:19:14.816 [VIEWAREA] reused 15000 sections crossing into minecraft:overworld
20:19:51.221 [VIEWAREA] reused 15000 sections crossing into genesis:great_unknown
```

The launch to space at `20:19:50.957` produced **no watchdog hit at all**. The
1.5–2.5 s stall on every crossing is gone.

### What is still felt, and it is not ours

The remaining stalls in that session were all *before* the first crossing, and
the largest was 21.7 seconds:

```
Render thread [RUNNABLE]
    zps/block/cableNetwork/CableBlock.getShape
    zps/block/cableNetwork/TransformerBlock.getShape
    Shapes.or → Shapes.join → BitSetDiscreteVoxelShape.join
```

The full stack names the caller, and it is **world load**, not chunk meshing:

```
WorldLoader.lambda$load$1
ReloadableServerResources.updateRegistryTags
Blocks.rebuildCache            ← every blockstate in the game
BlockStateBase.initCache
CableBlock.getOcclusionShape   ← ZPS, expensive shape unions
```

So it is a one-time cost each time a world loads, it does not depend on any
block being placed (a scan of the save found zero ZPS blocks), and **it has
nothing to do with travelling to space**. An earlier note in this file claimed
taking off re-meshed those chunks; that was wrong.

ZPS is removed, which is worth 21.7 s off every world load. It does not explain
a crossing freeze.

The last watchdog hit that session was `Minecraft.disconnect` — quitting the
world. Not a bug.

### Surveyed and rejected: Cosmic Horizons

MCreator-generated (`net/lointain/cosmos/procedures/*Procedure`). Ordinary
separate dimensions, planet-selector GUI, plain vanilla teleports, and the same
loading screen as everything else. There is no seamless technique in it to
take. Its planet-selection UI is worth looking at; its travel is not.

---

## Stage 0 — What the freeze actually is: ANSWERED

**The freeze is vanilla `Minecraft.setLevel` rebuilding the render section
grid.** Caught by `FreezeWatchdog` on 2026-07-30:

```
Render thread [RUNNABLE]
    GL45C.nglCreateBuffers (native)
    VertexBuffer.<init>
    SectionRenderDispatcher$RenderSection.<init>
    ViewArea.createSections
    ViewArea.<init>
    LevelRenderer.allChanged
    LevelRenderer.setLevel
    Minecraft.updateLevelInEngines
    Minecraft.setLevel
```

Every dimension change throws away the entire `ViewArea` and builds a new one,
allocating one OpenGL buffer per render section. At render distance 12 in a
320-tall world that is 25 × 25 × 20 = **12,500 `glCreateBuffers` calls,
synchronously on the render thread**. Measured stall: 1.5–2.5 s.

What this rules out, and it is most of what was suspected:

- Not the surface sampler. It was off for this run.
- Not chunk generation. `ArrivalGate` completed in **104 ms**
  (`19:17:42.312` launch → `19:17:42.416` ready).
- Not the server. The stall is on the render thread; the server never missed a
  tick.
- Not anything Genesis wrote. This is vanilla, it happens on nether and end
  portals too, and it predates every change in this repo.

It scales with `renderDistance² × worldHeight/16`, so it gets worse the nicer
the player's settings are.

**This is decisive for Stage 3.** Immersive Portals avoids it structurally: by
keeping both `ClientLevel`s alive it never calls `Minecraft.setLevel` on a
crossing, so the `ViewArea` is never rebuilt. The seamless approach is not just
nicer, it is the only one that addresses the actual cost.

### The cheaper alternative worth trying first

If the destination has the **same world height** and the render distance is
unchanged, the section grid is structurally identical and could be reused
rather than rebuilt — a mixin on `LevelRenderer.allChanged` that keeps the
existing `ViewArea` and re-points it instead of allocating 12,500 new buffers.

One obstacle is a data problem, not a code one: `genesis:great_unknown` is
`height 320` while the planets and the overworld are `384`. Align them and
every Genesis crossing becomes same-height, which is the case the reuse
optimisation can serve.

---

## Stage 0 (original) — Find out what the freeze actually is

**Status: DONE, see above.**

`FreezeWatchdog` is in. It dumps every thread's stack, with lock ownership, if
the client stops ticking for 1.5s. `planetSurfaceSampling` is now off by
default and off in `run/config/genesis-common.toml`, so the most-suspected
component is out of the picture.

- **User:** launch, cross a dimension, trigger the freeze, quit, send
  `run/logs/latest.log`.
- **Then:** grep `[WATCHDOG]`.

Nothing else in this plan is worth doing until this is answered. Every stage
below could inherit the freeze.

---

## Stage 1 — Get the tree into one known state

Currently a second agent's work sits uncommitted: 11 modified files, three new
packets (`TransitionReadyPacket`, `ShipVisibilityProbePacket`,
`ShipVisibilityStatusPacket`), `MASTER-PROMPT.md`, and a deleted
`ImmersivePortalsBridge`. It builds and reportedly works.

- Commit it, so it cannot be lost and so `git bisect` means something.
- Decide explicitly whether `ImmersivePortalsBridge` comes back.

Cheap, and everything after depends on a stable baseline.

---

## Stage 2 — Measure the crossing before changing it

Instrument the whole crossing end to end and log the split:

- server: entry test → gate hold → teleport
- client: respawn packet → level rebuilt → first chunk received → player's
  section meshed → screen dismissed

Right now nobody knows which of those dominates. The gate already logs its own
wait (0.4–1.0s observed). If the client-side mesh wait turns out to be 200ms,
the screen is nearly solved and Stage 3 is small. If it is 6 seconds, Stage 3
is the whole job. **This number decides how much Stage 3 is worth.**

---

## Stage 3 — Remove the screen (the "rooms in a house" model)

Only two mechanisms exist, and the choice should be made on Stage 2's number.

**3a. Use Immersive Portals.** `ServerTeleportationManager.forceTeleportPlayer`
moves a player between two already-loaded client levels with no respawn packet.
Mixin conflicts already solved; needs Cloth Config installed, then testing
whether IP's movement/entity sync coexists with Sable's. Days, not weeks — but
with a real risk it is simply incompatible.

**3b. Build the minimum ourselves.** Keep a second `ClientLevel` alive and swap
it in. The hard part is not the swap, it is that vanilla's protocol carries one
dimension at a time, so the server must be taught to stream chunks for a
dimension the player is not in. That is the packet-redirection layer, and it is
the genuinely multi-week part. It is also the part that makes IP famous for
breaking other mods.

Recommendation: try 3a properly first. It is already 80% done.

---

## Stage 4 — Planets from real chunk geometry

Independent of Stages 0–3; can proceed in parallel once Stage 0 is closed.

Already done: the sampler carries real terrain height per column
(`PlanetSurfaceData`), shipped to the client and stored.

Next: subdivide each planet cube face into a grid and displace vertices along
the face normal by real height, reusing the existing planet shader. That is the
first point where terrain has a silhouette instead of being a picture of
terrain.

Note the sampler is currently **off by default** and Mercury and the Moon have
generators too slow to sample within budget — they abandon and keep painted
textures. Earth samples in about a second. Fixing the slow ones means caching
samples to disk rather than making the sampler faster.

---

## Order of work

```
Stage 0 (one test run)  ──►  Stage 1  ──►  Stage 2  ──►  Stage 3a ──► 3b if needed
                                                     └──►  Stage 4 (parallel)
```
