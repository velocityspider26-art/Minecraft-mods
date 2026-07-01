# Create Radar Ballistics

A fire-control add-on for **Create Radar** and **Create: Big Cannons** (NeoForge 1.21.1).

Auto-tracking cannons normally aim at where the target *is*. But a shell takes time to fly,
and during that flight the target keeps moving and the shell drops under gravity and slows in
the air — so the shot lands behind and short. **This mod solves the intercept instead:** it
leads the target using its velocity (and acceleration) and computes the barrel elevation that
accounts for the shell's muzzle speed, gravity and drag. Moving targets get hit, not trailed.

## How it works

The core is a pure-Java engine in `com.velocityspider.radarballistics.ballistics` that solves
two coupled problems:

1. **Lead** — the intercept point depends on the flight time, but the flight time depends on
   how far away the intercept point is. A damped fixed-point iteration converges the two.
2. **Elevation** — with air drag there's no closed-form parabola, so the engine simulates the
   exact per-tick projectile motion the game uses and searches the launch angle with a
   bracket-and-bisection root find, preferring the flatter (low) arc.

Because drag acts along the line of flight and gravity acts vertically, the trajectory stays
in one vertical plane, which makes the elevation search a fast, exact 2D simulation while the
yaw is simply the bearing to the lead point.

The engine has no Minecraft dependencies, so it is unit tested and numerically verified: every
scenario computes a solution, then simulates the shell and the moving target forward in full 3D
and confirms the closest approach is a fraction of a block — including fast crossing targets,
incoming targets, climbing targets and falling (accelerating) targets.

## Try it in-game

```
/radarballistics profile    # show the projectile model the solver assumes
/radarballistics solve      # look at any entity — prints the yaw, elevation, lead distance
                            # and time-of-flight a cannon at your eye would need to hit it
```

Point at a moving mob and watch the reported lead grow with its speed.

## Configuration

`config/radarballistics-common.toml` lets you match the solver to your shell:

| Option | Meaning |
| --- | --- |
| `enableAutoTrackLead` | Master switch for the lead correction |
| `leadMovingTargets` | Predict target motion (the core fix); off = vanilla behaviour |
| `projectile.muzzleSpeedBlocksPerTick` | Shell speed — higher with more propellant charges |
| `projectile.gravityBlocksPerTickSquared` | Downward acceleration on the shell |
| `projectile.dragPerTick` | Per-tick air-resistance multiplier (1.0 = none) |
| `solver.*` | Advanced convergence tuning |

## Hooking into the cannons

The `/radarballistics` command runs the full engine today. Wiring the same solver into Create
Radar's auto-tracking and Create: Big Cannons' projectiles takes a small Mixin adapter that
must be compiled against those mods — see **[docs/INTEGRATION.md](docs/INTEGRATION.md)** and the
template in `src/integration-templates/`.

## Building

```
./gradlew build     # builds the mod
./gradlew test      # runs the ballistics unit tests
```

## License

MIT.
