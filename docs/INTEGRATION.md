# Building the Create Radar auto-aim integration

By default this mod builds as a standalone engine + the `/radarballistics` command. Passing
`-PwithCbc` additionally compiles the **auto-aim integration**: two Mixins that replace Create
Radar's aiming math with this mod's verified, drag-accurate solver, so cannons lead targets
correctly **without** anyone running a command.

## What the integration does

It targets the Create: Radars NeoForge 1.21.1 port (`com.happysg.radar`) and intercepts the two
methods that decide where a tracked cannon points:

| Target (in Create: Radars) | Mixin | Why |
| --- | --- | --- |
| `compat.cbc.CannonTargeting#calculatePitch(mount, origin, target, level)` | `CannonTargetingMixin` | The original solves elevation with a **continuous-time** closed form that treats CBC's per-tick drag as a continuous coefficient and ignores quadratic drag — systematically off at range. We return a pitch found by simulating CBC's **exact per-tick** flight. |
| `compat.cbc.CannonLead#solveLeadPerTickWithAcceleration(...)` | `CannonLeadMixin` | Replaces the moving-target lead with a coupled intercept solve (target velocity **and** acceleration + real muzzle speed/gravity/drag), so the predicted aim point and time-of-flight are correct. |

Both mixins delegate to `integration.cbc.CbcBridge`, which pulls the shell's real muzzle speed
(charge count), gravity, drag and quadratic-drag flag straight from the loaded cannon via
Create Radar's own `CannonUtil`, feeds them to the engine, and hands the result back in the
exact types the original methods return. On any failure (out of range, laser cannon, or the
feature disabled in config) the mixin does nothing and the original code runs — so it degrades
safely. Everything respects `enableAutoTrackLead` in the config.

The command still works and is now **optional** — purely a diagnostic.

## Prerequisites

The `withCbc` build resolves everything from `./libs/` plus one small Maven artifact (Flywheel).
The Create ecosystem mavens are flaky, so downloading the mod jars from Modrinth/CurseForge is
the reliable route. You need these jars in `./libs/` (NeoForge 1.21.1 builds):

- **Create: Radars** — e.g. `create_radar-0.4.9.4-1.21.1.jar` (Modrinth)
- **Create** — e.g. `create-1.21.1-6.0.10.jar` (Modrinth)
- **Create: Big Cannons** — e.g. `createbigcannons-5.11.7+mc.1.21.1.jar` (Modrinth)
- **Ponder**, **Registrate**, **Flywheel** — Create bundles these as jar-in-jar; extract them
  from Create's jar (`META-INF/jarjar/*.jar`) into `./libs/` so the compiler can see them.

Flywheel also resolves from `https://maven.createmod.net` (already declared under `withCbc`), so
extracting it is optional. These are all `compileOnly` — none are bundled into the output jar.

## Build it

```
mkdir -p libs
# 1. Put the three mod jars in ./libs
cp create_radar-0.4.9.4-1.21.1.jar create-1.21.1-6.0.10.jar createbigcannons-5.11.7+mc.1.21.1.jar libs/
# 2. Extract Create's bundled Ponder/Registrate/Flywheel so they resolve at compile time
cd libs && unzip -o create-1.21.1-6.0.10.jar 'META-INF/jarjar/*.jar' -d _jj && cp _jj/META-INF/jarjar/*.jar . && rm -rf _jj && cd ..
# 3. Build the integrated jar
./gradlew build -PwithCbc
```

Override versions with `-Pflywheel_version=…` if the Flywheel Maven coordinate drifts.

The result in `build/libs/radarballistics-1.0.0.jar` now contains the mixins, the
`radarballistics.mixins.json` config, and a `neoforge.mods.toml` that registers the mixin config
and declares Create Radar + CBC as optional dependencies.

## Install

Put that jar in `mods/` alongside Create, Create: Big Cannons and Create: Radars (all for
NeoForge 1.21.1). Build a radar-controlled auto-cannon as normal — the aiming is now corrected
automatically. Verify with `/radarballistics solve` while looking at a moving target.

## Note on this build environment

The standalone engine + command jar is built and its math is unit-tested and numerically
verified here. The `-PwithCbc` integration jar must be built in a workspace that has the
Create: Radars port jar (it isn't published to Maven), so the final mixin binary is produced
there rather than in this sandbox. The mixin code is written against the port's actual source.
