# Hooking the engine into Create Radar + Create: Big Cannons

The ballistics engine in this mod (`com.velocityspider.radarballistics.ballistics`) is pure
Java and builds with **no** third-party mods on the classpath, which is why it can be unit
tested and numerically verified in isolation. Wiring it into the actual auto-tracking of
Create Radar and Create: Big Cannons (CBC) requires compiling against those two mods, whose
Maven artifacts are not available in every build environment. This document describes exactly
where the two hook points are and how to connect them.

The ready-to-adapt Mixin is in [`src/integration-templates/AutoAimMixin.java`](../src/integration-templates/AutoAimMixin.java).
It is deliberately **outside** `src/main/java` so the mod still builds without the Create
dependencies. Move it into a source set and fill in the marked class/method names once you add
the dependencies.

## 1. Add the dependencies

In `build.gradle`, add the Create ecosystem repositories and declare CBC and Create Radar as
`compileOnly` (mixin targets are resolved at runtime by the loaded mods):

```gradle
repositories {
    maven { url "https://mvn.devos.one/releases" }      // Create + library mods
    maven { url "https://maven.createbigcannons.com/" }  // Create: Big Cannons
    // ...plus wherever your Create Radar build is published
}

dependencies {
    compileOnly "com.simibubi.create:create-${mc_version}:<create_ver>:slim"
    compileOnly "com.jozufozu.flywheel:flywheel-neoforge-${mc_version}:<flywheel_ver>"
    compileOnly "rbasamoyai:createbigcannons-${mc_version}:<cbc_ver>"
    compileOnly "<group>:createradar-${mc_version}:<radar_ver>"
}
```

Exact coordinates and versions change per release — confirm them on each mod's distribution
page. Also add the three mods as `optional` dependencies in `neoforge.mods.toml`.

## 2. Register the mixin config

Uncomment the mixins block in `neoforge.mods.toml`:

```toml
[[mixins]]
config="radarballistics.mixins.json"
```

and add `src/main/resources/radarballistics.mixins.json`:

```json
{
  "required": true,
  "package": "com.velocityspider.radarballistics.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": ["AutoAimMixin"],
  "injectors": { "defaultRequire": 1 }
}
```

## 3. The two hook points

**Hook A — the target feed (Create Radar).** Create Radar's auto-aiming controller decides
where the cannon should point. Vanilla behaviour points it at the tracked target's *current*
position. Intercept the method that produces that aim direction/target and, instead, call:

```java
FireSolution sol = FireControl.solve(muzzlePos, new TargetState(targetPos, targetVelocity));
if (sol.converged()) {
    // drive the mount to sol.yawDegrees() / sol.pitchDegrees()
}
```

`targetVelocity` should be the tracked entity's `getDeltaMovement()` (for a moving
contraption, its measured per-tick displacement). This is the change that fixes the miss:
the mount is driven to the *predicted intercept*, not the stale position.

**Hook B — the projectile model (Create: Big Cannons).** The solver needs the shell's real
muzzle speed, gravity and drag so its elevation is correct. CBC exposes the initial speed
when a round is fired (it scales with the number of propellant charges). Read those values
and set them on the config, or better, pass a per-shot `ProjectileProfile`:

```java
ProjectileProfile profile = new ProjectileProfile(cbcMuzzleSpeed, cbcGravity, cbcDrag);
FireSolution sol = new BallisticSolver(...).solve(muzzlePos, target, profile);
```

CBC's projectile properties are data-driven; the muzzle speed for a given charge count is
available from the cannon's fired-projectile context. If you can't read it live, the config
default (`projectile.muzzleSpeedBlocksPerTick`) is a reasonable starting point — set it to the
speed you observe for your standard load.

## 4. Applying the solution to the mount

Create's cannon mounts (pitch bearing + rotation) rotate at a limited speed, so feed
`sol.yawDegrees()` and `sol.pitchDegrees()` to the mount's target-angle setter every tick and
let it slew. Because the solver already accounts for time-of-flight, the mount will lead the
target correctly once it is on-angle. Only fire when the mount is within a small tolerance of
the solution angles.

## Verifying it works

Without any of this wiring you can already exercise the exact math the hook uses, in-game:

```
/radarballistics profile        # shows the projectile model the solver assumes
/radarballistics solve          # look at any entity; prints the yaw/pitch/lead/time to hit it
```

Point at a moving mob and watch the reported lead distance grow with its speed — that is the
correction Hook A applies to the cannon.
