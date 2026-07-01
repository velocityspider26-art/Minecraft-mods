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

You need a workspace where the modded classes resolve at compile time:

1. **Create** and **Create: Big Cannons** come from Maven (repositories are already declared in
   `build.gradle` under the `withCbc` block).
2. **Create: Radars** (the port at
   `github.com/Maqwr/Create-radars-port-1.21.1-neoforge`) is **not** on Maven. Build or download
   its NeoForge 1.21.1 jar and drop it into `./libs/` (any `*.jar` there is put on the compile
   classpath).

The dependency versions default to those in the port's `gradle.properties`; override any with
`-Pcreate_version=… -Pcbc_version=… -Pflywheel_version=…` if they drift.

## Build it

```
# 1. Put the Create: Radars port jar here:
mkdir -p libs && cp /path/to/create_radar-*.jar libs/

# 2. Build the integrated jar:
./gradlew build -PwithCbc
```

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
