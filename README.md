# Realistic Crash Physics

A NeoForge 1.21.1 addon for **Create: Aeronautics** (and anything else built on the **Sable** physics engine) that
makes crashes behave like crashes. A plane that hits a field ploughs a trench, a ship that hits rock crumples, a heavy
ram punches through a wall and carries on, and loads that are too big tear vehicles apart.

## What happens in a crash

- **Craters and trenches.** Hitting the ground fast enough crushes it. How deep depends on speed, angle, the vehicle's
  mass, and what the ground is made of: soft dirt and sand give way easily, rock barely marks. Soil isn't deleted,
  it's shoved out of the way. A dive pushes it out all round into a raised rim; a shallow slide ploughs a trench with a
  bow wave ahead and berms along both sides. Very fast impacts also blast some of it out as flying debris. Afterwards
  the loose walls slump until they are no steeper than soil can stand, so craters settle into bowls over a few
  seconds. Crater floors turn to coarse dirt, and grass and snow on top are crushed.
- **Water.** Water has no strength, only inertia, so hitting it fast is like hitting a wall. Slamming flat into the
  surface puts a short pressure peak on the hull, several times ½ρv². Above a block's strength it cracks, and at twice
  its strength it shatters: wooden hulls break up above roughly 45 m/s, and metal holds to over 100 m/s. A gentle
  splashdown does nothing. Under water, every leading face is slowed by drag, ½·ρ·Cd·A·v², so sinking wreckage
  reaches a realistic terminal speed. Lava works the same way at three times the density. Floating itself is
  Sable's buoyancy.
- **Crumpling.** Whichever side is weaker gives way. A wooden hull hitting stone is crushed while the stone stays;
  a steel ram drives into soft ground without a scratch. Blocks crack first, and break once crushed far enough.
- **Breaking through.** A block with open air behind it (a house wall, a post, a tree trunk) is held only by its
  joints to its neighbours. A hard enough hit knocks it out whole, and it flies off ahead of the vehicle. Solid ground
  behind a block means it can only be crushed.
- **No tunnelling.** Before every physics step, each moving vehicle is swept along its path against the terrain. A
  vehicle covering most of a block per step can't skip through thin walls the way it otherwise would.
- **Breaking apart.** Every crash puts each block of the vehicle under the deceleration it is going through, spin
  included. Joints that can't carry the load fail: wings and tails shear off, overloaded structures snap. Sable then
  splits the loose parts off into their own physics objects. Compression, tension and shear are handled separately, so
  a stack of blocks squeezed by a crash holds while a cantilevered wing tears off.
- **Collapses.** Cut through the base of a tower or a tree and the part above comes down as a physics object.
- **Wreckage.** Broken vehicle blocks fly off with the vehicle's momentum, or are pulverised at high speed.
- **Crew.** Riders take damage from g-forces. Seated riders survive about 25 g, people standing loose about 10 g.
  Loose riders keep going when the vehicle stops.
- **Effects.** Material-appropriate impact sounds, dust and debris particles, camera shake for nearby players, skid
  marks in grass and snow. Destroyed burners and engines can explode or start fires.

## How it works

Contacts come from Sable's Rapier physics engine through a collision callback on every block, plus the pre-step
sweep. Each contact is solved with a physical model rather than hit points:

- **Penetration:** Tate–Alekseevskii hydrodynamic penetration. The interface pressure between the two blocks
  (strength plus ½ρv² for each side) decides which one erodes, and how fast. Stepped over the physics sub-step with
  a second-order integrator, it reproduces the analytical Poncelet penetration depth to within 1%.
- **Onset:** a contact only starts to crush once its share of the kinetic energy exceeds what the material can absorb
  elastically. This is why a gentle landing leaves the grass alone, and why a flat belly landing is gentler per block
  than a nose-first hit.
- **Load sharing:** blocks that hit at the same time share the vehicle's effective mass at their combined centre.
- **Hold or give way:** if the material can stop the vehicle within the step, Rapier keeps the contact and stops it
  there. Only damage is added. If a block fails, or the material can't stop the vehicle, the vehicle carries on into
  it, and the resisting force is applied as an impulse, plus friction from ploughing through.
- **Knock-outs:** a block with nothing behind it fails once the contact's force exceeds what its joints to its
  neighbours can take, and the energy to break them is there. The vehicle then shares its momentum with the block.
- **Structure:** a load-path analysis of the vehicle's block graph against the crash accelerations.

Everything uses SI units. Block masses come from Sable (planks 500 kg, stone 2 t, metal blocks 4 t per block by
default).

## Performance

- A vehicle flying with nothing but air around it costs almost nothing: the pre-step sweep first checks whether the
  space it is moving through holds anything at all, a whole chunk section at a time.
- Each vehicle's block layout is gathered once, skipping empty chunk sections, and then updated block by block as
  crashes break pieces off, rather than rescanned.
- Soil is moved as blocks, not thrown as entities, except in very fast impacts.
- Velocities are only read back after a physics step for vehicles that actually hit something.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.228 or newer
- Sable 2.x (required)
- Create: Aeronautics (optional, the mod works with any Sable vehicle)

## Configuration

Server settings are in `crashphysics-server.toml` (per world, under `serverconfig`):

| Section | Setting | Default | What it does |
|---|---|---|---|
| general | `enabled` / `worldDamage` / `vehicleDamage` | true | Master switch; whether crashes break terrain; whether they break vehicles |
| physics | `kgPerSableMass` | 1000 | Kilograms per unit of Sable block mass |
| physics | `strengthMultiplier` | 1.0 | Scales every material's strength (lower = more destruction) |
| physics | `vehicleStrengthMultiplier` | 1.0 | Extra multiplier for vehicle blocks |
| physics | `jointStrengthMultiplier` | 1.0 | How much load vehicle joints carry before parts tear off |
| physics | `minImpactSpeed` | 1.5 m/s | Slower contacts never do damage |
| physics | `elasticTravel` | 0.02 m | Flex before crushing starts (higher = more forgiving landings) |
| physics | `breakDepth` | 0.35 | Fraction of a block that must be crushed before it breaks |
| physics | `plowFriction` | 0.6 | Friction while ploughing through material |
| structure | `structuralFailure` / `structuralMinG` / `maxStructuralBlocks` | true / 5 g / 8000 | Tearing vehicles apart under crash loads |
| crew | `crewInjury`, `seatedToleranceG`, `standingToleranceG`, `crewDamageScale`, `throwLooseCrew` | true, 25, 10, 8, true | G-force injuries |
| debris | `ejecta`, `ejectaChance`, `ejectaSpeedFactor` | true, 0.55, 0.35 | Material thrown out of craters |
| debris | `maxFlyingDebrisPerTick`, `vehicleDebrisChance`, `dropChance` | 48, 0.4, 0.2 | Wreckage and drops |
| debris | `collapse` / `maxCollapseBlocks` | true / 800 | Loose sections falling down |
| debris | `displaceSoil` | true | Dug-out soil is shoved into rims and berms instead of disappearing |
| debris | `slumping` | true | Loose crater and trench walls slump to a stable slope |
| water | `waterImpacts` | true | Hitting water or lava fast is a real impact (hull damage, braking) |
| water | `waterDrag` | true | Vehicles moving through water or lava are slowed by it |
| water | `dragCoefficient` | 1.0 | Drag coefficient of a block face in a liquid |
| effects | `skidMarks`, `fireAndExplosions`, `soundVolume`, `cameraShake` | true, true, 1.0, true | Effects |
| limits | `maxBlocksBrokenPerTick` | 600 | Destruction budget per tick per dimension |
| limits | `damageMemorySeconds` | 90 | How long cracks are remembered |
| limits | `debugLogging` | false | Logs every damaging impact |

Clients can turn camera shake off or scale it in `crashphysics-client.toml`.

## Materials (datapacks)

Every block gets a material guessed from its tags, sound and hardness. Datapacks can override it with JSON files in
`data/<namespace>/crashphysics/materials/`. The mod ships definitions for Create: Aeronautics, Simulated, Offroad and
Create blocks. Every field except `selector` is optional:

```json
{
  "selector": ["create:andesite_casing", "#minecraft:logs"],
  "priority": 1000,
  "density": 700,
  "strength": 4e6,
  "joint_strength": 1e6,
  "fracture": "fibrous",
  "unbreakable": false,
  "ejecta": "self",
  "scar": "minecraft:coarse_dirt",
  "skid": "minecraft:coarse_dirt",
  "volatility": 2.5,
  "ignites": true
}
```

| Field | Meaning |
|---|---|
| `density` | kg/m³ |
| `strength` | Crush strength, Pa |
| `joint_strength` | Load a full face connection carries, Pa |
| `fracture` | `granular`, `brittle`, `fibrous`, `ductile`, `soft`, `glass` or `plant` (sounds and debris) |
| `ejecta` | What is thrown out of a crater: `none`, `self` or a block ID |
| `scar` | What a crater floor turns into |
| `skid` | What the block turns into when a vehicle slides over it |
| `volatility` | Explosion power when the block is destroyed in a crash |
| `ignites` | Whether it starts fires when destroyed |
| `priority` | Higher values are applied later and win |

## Known limitations

- Sable simulates in 32-bit floats without origin shifting, so all of its physics, this mod's included, gets less
  precise millions of blocks from the world origin.
- Vehicle-against-vehicle contacts are only seen by Rapier; the pre-step sweep checks terrain only. Very fast
  vehicle-on-vehicle collisions can still pass through each other.
- Create kinematic contraptions (moving bearings and gantries, not Sable vehicles) are treated as rigid, and their
  blocks aren't damaged.
- Spinning propeller strikes are not modelled beyond the blocks themselves.
- Debris and knocked-out blocks are falling blocks, not physics objects, so they don't collide with vehicles. They
  land as blocks and stay where they land.
- Vehicles larger than about 200,000 blocks of plot volume skip the structural check and the pre-step sweep, for
  performance.
- Camera shake and crew injuries are implemented but have only been tested without a client.

## Development

- `./gradlew test` runs the unit tests of the physics core (penetration, contact stepping, knock-outs, structural
  solver, crew loads, ejecta).
- `./gradlew runGameTestServer` runs in-game tests against the real Sable engine on a fresh world each time:
  - a dive into dirt digs a crater with a rim, and conserves the soil;
  - a dive into stone crumples the hull;
  - a gentle landing does nothing;
  - a hard impact tears an overhanging arm off;
  - an iron ram punches through a plank wall;
  - a sled leaves tracks in grass;
  - ramming a tower brings its top down;
  - a fast ram breaks through a thin brick wall from a distance;
  - a slow ram stops dead against a thick wall;
  - a shallow crash ploughs a trench with berms;
  - a fast plunge into water smashes a wooden hull;
  - a slow splashdown is harmless and the hull floats.
- `./gradlew build` produces `build/libs/crashphysics-<version>.jar`.
