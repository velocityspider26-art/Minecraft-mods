# ECOTI Thermal Overlay

A MelonLoader mod for **OPERATOR** (Vector Interactive). Outlines hostile AI in red through walls
while you're looking through quad-tube NVGs with an ECOTI clip-on mounted.

**Solo sessions only.** The overlay does not draw in any session with another player connected.
That is enforced in code, not by convention — see [The solo gate](#the-solo-gate).

Built against the community [OPERATOR Modding Toolkit](https://github.com/ArchdukePierre/operator-modding-toolkit).

---

## The solo gate

OPERATOR runs Lone Wolf, co-op PvE and PvP "Force on Force" out of one client binary over one
Mirror stack. Nothing about a through-wall overlay distinguishes those modes on its own, so this
mod distinguishes them explicitly. `SessionGate` requires two things every frame:

| Condition | Read | Fails when |
|---|---|---|
| We are the server | `NetworkServer.active` | You joined someone else's lobby |
| Nobody else connected | `NetworkServer.connections.Count <= 1` | Any second player is present |

Mirror runs a host as server+client in one process, so a solo session has `active` true and exactly
one connection — the host's own local one. A joiner pushes the count to 2 and the overlay drops on
that frame.

It's evaluated per frame rather than latched at mission start, because a lobby can gain a player
mid-session and a latched answer would go stale in the one direction that matters. Every failure
path resolves to **off**: missing type, null manager, unreadable count, a throw inside the check,
anything ambiguous. A wrong "no" costs you outlines in Lone Wolf. A wrong "yes" is a wallhack in a
PvP match. Those aren't symmetric, so the code isn't either.

There is no config option to disable this gate, and adding one would defeat the point of the mod
having been written this way.

### On the ECOTI itself

The real ECOTI is a clip-on thermal imager that fuses a thermal image into the night vision tube.
It's line-of-sight — thermal doesn't go through walls. This overlay does. It's a game mod, not a
simulation of the device, and the name is describing where it hangs rather than what it can do.

---

## Requirements

- OPERATOR (Steam appid 1913370)
- [MelonLoader](https://github.com/LavaGang/MelonLoader) 0.7.3 or later
- .NET SDK 8 or later, to build

---

## Building

**There is no prebuilt DLL, and that isn't a policy choice — it's that a portable one doesn't
exist.** The project references `MelonLoader/Il2CppAssemblies/*.dll`, which MelonLoader generates
on your machine from your installed game build the first time it runs. Those assemblies carry the
method tokens and type layout of that specific build. A DLL compiled against someone else's
generated interop is not reliably loadable against yours, and it silently rots the next time the
game patches. Building locally takes about thirty seconds:

```powershell
# 1. Install MelonLoader against OPERATOR, then launch the game once and quit.
#    This generates MelonLoader\Il2CppAssemblies\ — the build cannot proceed without it.

# 2. Build and deploy in one step:
.\deploy.ps1

# or if your install isn't on the default path:
.\deploy.ps1 -GameDir "D:\Steam\steamapps\common\OPERATOR"
```

`deploy.ps1` builds, refuses to copy while the game is running (the DLL is locked, and silently
shipping a stale build wastes a whole test cycle), and prints the deployed timestamp so you can
confirm what landed.

Manual build if you'd rather:

```powershell
dotnet build EcotiThermal.csproj -c Release -p:GameDir="C:\Path\To\OPERATOR"
copy bin\Release\net6.0\EcotiThermal.dll "C:\Path\To\OPERATOR\Mods\"
```

---

## Using it

| Key | Does |
|---|---|
| `F7` | Toggle the overlay |
| `F8` | Toggle the status HUD |
| `F9` | Dump equipped kit and NVG state to the log |

The status HUD is on by default and tells you which gate is currently blocking. If nothing is
drawing, read that line first — it's there so you don't have to guess.

Four gates, all required, checked in this order:

1. **Enabled** — config plus the `F7` toggle
2. **Solo session** — the gate above
3. **Quad tubes down** — `NvgLensFollower.LensCount >= 4` with NVGs active
4. **ECOTI mounted** — a matching clip-on in an NVG mount socket

Flip to a dual-tube device and gate 3 drops the count to 2 and the overlay goes away on that frame.

---

## Configuration

`UserData/MelonPreferences.cfg`, category `EcotiThermal`, written on first run.

| Setting | Default | Notes |
|---|---|---|
| `Enabled` | `true` | Master switch |
| `RequireQuadTube` | `true` | Off means any active NVG device |
| `RequiredLenses` | `4` | Lens count that counts as a quad |
| `RequireEcoti` | `true` | See troubleshooting if the overlay never appears |
| `EcotiNameContains` | `ecoti` | Case-insensitive substring match on equipped mod names |
| `MaxRange` | `250` | Metres |
| `OutlineR/G/B` | `1 / 0.12 / 0.12` | Red |
| `OutlineThickness` | `1.5` | Pixels |
| `FillOpacity` | `0.10` | Translucent fill; `0` for outline only |
| `DistanceFade` | `true` | Distant contacts read fainter |
| `ShowDistance` | `false` | Range label under each box |
| `RebuildInterval` | `0.25` | Seconds between registry walks; positions update every frame |
| `ShowStatusHud` | `true` | Gate status line |

---

## Troubleshooting

**Nothing draws and the HUD says "ECOTI state unknown".** The ECOTI item's in-game name is the one
thing the toolkit doesn't document, so it's matched as a configurable substring rather than
hardcoded. Get in a mission with your kit on, press `F9`, and read the socket dump in the
MelonLoader console — it prints every socket and its occupant unfiltered. Put the real name in
`EcotiNameContains`, or set `RequireEcoti = false` to skip gate 4 entirely.

**Nothing draws and the HUD says "SOLO ONLY".** Working as intended. The rest of the line says
which condition failed.

**Boxes are offset or the wrong size.** The camera is resolved through `MainCameraSingleton` with a
`Camera.main` fallback; if the singleton exposes its camera under a member name this doesn't try,
the fallback may be picking a scope or UI camera. The `Probe` warnings in the log name the member
that missed.

**Warnings like `probe: no static property or field named X`.** That's the late-binding layer
reporting a member it couldn't resolve on your build, once each. Everything downstream of a miss
fails closed, so the mod stays quiet rather than misbehaving — but that line names exactly what to
fix.

---

## How it's put together

| File | Does |
|---|---|
| `Main.cs` | MelonMod entry, gate chain, hotkeys |
| `SessionGate.cs` | Solo detection — the load-bearing one |
| `NvgProbe.cs` | Quad-tube and phosphor state off `NvgLensFollower` |
| `EcotiDetector.cs` | Clip-on detection plus the `F9` inventory dump |
| `EnemyTracker.cs` | Walks the `BrainAI.AllBrainAI` registry, filters dead and friendly |
| `Overlay.cs` | Projects world bounds to screen, draws the boxes |
| `Probe.cs` | Late-bound reads with cached accessors and once-only miss logging |

Two decisions worth explaining.

**Everything game-facing is late-bound.** The toolkit's field notes put it bluntly: compiling is
not evidence. A successful build proves an interop member exists, not that it's static at runtime,
not that it's a property rather than a field, not that it's populated when read. So members are
read by name, property first then field, accessor cached after the first resolve, miss logged once,
fallback returned. It costs a dictionary lookup and buys the mod surviving a game patch that moves
a member instead of hard-crashing on load.

**The overlay is IMGUI, not chams or a custom pass.** A second material has to fight HDRP's ZTest
setup, and blind-swapping onto a Shader Graph material loses its maps. An injected `CustomPass`
subclass needs `ClassInjector` and lands in a stack already running eleven-plus passes including a
compute-shader NVG autogate. Both are real options and both need a live build to tune. Screen-space
boxes need no shader, no asset and no pipeline surgery, so this is the path that works on first
launch. If you want true 3D chams later, `Overlay.Draw` is the single seam to replace.

Box corners are projected rather than centre-plus-height, so the outline follows posture: a prone
bot gives a flat wide box, a standing one a tall narrow box.

### What's verified and what isn't

Everything here compiles clean, and the logic was checked against a stub harness. It has **not**
been run against the game — no OPERATOR install was available. Split by confidence:

**Documented in the toolkit, high confidence:** the Mirror statics behind the solo gate;
`NvgLensFollower.AnyActive` / `LensCount` / `PhosphorColorValue` (the toolkit calls these the
cheapest NVG probe in the game, and the NVG autogate pass reads the same registry);
`BrainAI.AllBrainAI` as the bot registry with `Awake`/`OnDestroy` maintaining it; `TeamIdentifier.TeamID`
as the shared friend/foe model; `Health.isDead`.

**Inferred, expect to tune on a live build:** the ECOTI item name and the socket member that holds
it; the local-team resolution path through `PlayerMaster.MyPlayerMaster`; the `MainCameraSingleton`
camera member name. Each is behind `Probe`, so a wrong guess logs a named warning and falls back
rather than crashing — and for the team lookup the fallback is "treat all AI as hostile", which is
the right answer in Lone Wolf anyway.

---

## Licence

MIT.
