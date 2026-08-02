# Master prompt — paste this into a fresh GPT session

---

You are helping me build **VS Genesis**, a space mod for Minecraft. Read this
whole brief before writing any code. There is a detailed handover file in the
repo at `HANDOVER-SRE.md` — read it first, it contains the reasoning behind
everything below.

## Stack, exactly

- Minecraft **1.21.1**, NeoForge, **Java 21** (Java 25 on PATH breaks the build —
  set `JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'`)
- Repo: `C:\Users\veloc\IdeaProjects\Project-Starbound`, branch
  `neoforge-1.21.1-port`
- Physics/ships: **Sable** + **Create Aeronautics**. NOT Valkyrien Skies — do not
  suggest VS APIs, they do not exist here.
- Cross-dimension ship transfer depends on the **`dimensional_sable`** mod, which
  exposes `dev.egg.SubLevelWarper.WarpSubLevel(ServerSubLevel, Level, Vector3d)`.
  Base Sable has **no** cross-dimension sub-level API. This is reached through a
  reflective bridge (`DimensionalSableBridge`) so it stays an optional dep.
- Build: `./gradlew.bat compileJava test`

## What the mod does

Real Keplerian orbits, landable planets as separate dimensions (Earth = overworld,
plus Moon, Mercury, Malachite), a space dimension you fly up into, an orbit map on
`M`, and ships that carry their crew between dimensions as one unit.

## My goal, in priority order

1. **Kill the loading screen on dimension crossings.** Flying up to space or down
   to a planet should feel continuous. This is the main thing.
2. **Render planets from the actual Minecraft world** rather than painted
   textures, so what I see from orbit is the terrain I land on.
3. Fix the open bugs listed at the end of `HANDOVER-SRE.md`.

## Hard-won constraints — violating these wastes both our time

**On the loading screen.** Two approaches have already failed:

- Cancelling `ScreenEvent.Opening` for `ReceivingLevelScreen` **strands the
  player on a frozen screen forever.** Cancelling `Opening` does not close the
  current screen, it only blocks the new one. Do not do this.
- Pre-generating destination chunks **cannot remove the screen by itself.** The
  client still has to receive chunks and build meshes, and vanilla holds the
  screen until `isSectionCompiled(playerPos)`. That work is client-side and
  unavoidable.

The current approach (already implemented, unverified) is to register a custom
screen via NeoForge's **`RegisterDimensionTransitionScreenEvent`** that draws
nothing, letting the world show through while it streams. It keeps vanilla's
dismissal logic, so it cannot strand anyone.

**On Immersive Portals.** I own the jar and asked about porting it. Findings:
licence is Apache 2.0 so it is legally fine, but the jar ships **compiled Fabric
1.20.1 intermediary bytecode** — there is no source to take, and every symbol,
mixin and signature would need remapping. More importantly the seamless part is
not a liftable file: it is `ClientWorldLoader` keeping several `ClientLevel`s
alive at once, plus a packet-redirection layer that tags play packets by
dimension. Sable does its own per-level client sync that would likely collide
with it. **Implement techniques, do not try to port the code.**

**On the reference video** (a Create & Sable space addon with a spherical planet):
that author uses real LOD chunk geometry stretched onto a sphere, and has **no
dimension switch to hide** because his planet is one continuous world. That is a
different foundation, not a feature to bolt on. Do not propose matching it as if
it were a small change.

## How I need you to work

- **Verify before claiming.** Compiling is not working. Booting to the title
  screen never exercises a dimension crossing, so it proves nothing about
  travel. If you have not seen it work, say so plainly.
- **Never run the game without copying `run/logs` aside first.** Minecraft
  rotates logs on every launch; a previous session destroyed my crash logs with
  repeated verification runs and the crash is still undiagnosed.
- Tell me when something you shipped is the likely cause of a new problem. Two
  of my recent breakages were introduced by "fixes".
- If a request is a much bigger job than it sounds, say so up front rather than
  half-building it.
- Don't reformat or refactor code you were not asked to touch.

## Start here

Read `HANDOVER-SRE.md`. Everything in its two most recent commits (`1347ced`,
`6c08c4e`) **compiles and passes tests but has never been run in game.** Before
building anything new, help me verify whether those landed:

- Loading screen: is the crossing visibly shorter, and does the world show
  through instead of a themed screen? The handover notes the win should show on
  the **launch to space** side specifically, and explains why entry may already
  have been fast.
- Surface sampling: grep the log for `[SURFACE]`. Expect `sampling <planet>
  across <n> blocks`, progress lines, `sampled <planet> from its own terrain`,
  and client-side `is now drawn from its real terrain`. **Zero `[SURFACE]` lines
  means it still is not running.**

Then work the open bugs in `HANDOVER-SRE.md`, starting with the undiagnosed
crash when returning to Earth.
