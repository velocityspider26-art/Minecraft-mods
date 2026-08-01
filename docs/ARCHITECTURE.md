# Architecture report — Create: Jet Engines

This is the Phase A deliverable: what the Sable/Aeronautics propulsion architecture actually looks
like in the shipped 1.21.1 builds, and how this addon plugs into it.

Every signature below was read out of the real distributed jars with `javap` and CFR, not recalled
from memory. Where this addon mirrors an upstream pattern, the upstream class is named.

## 1. Where the dependency code came from

| Artifact | Version | Source |
|---|---|---|
| `sable-neoforge-1.21.1-2.0.3.jar` | 2.0.3 | Modrinth (`sable`) |
| `sable-companion-common-1.21.1-1.6.0.jar` | 1.6.0 | JarJar'd inside the Sable jar |
| `veil-neoforge-1.21.1-4.1.4.jar` | 4.1.4 | JarJar'd inside the Sable jar |
| `sable_rapier` | 2.0.3 | JarJar'd inside the Sable jar (physics backend) |
| `create-aeronautics-bundled-1.21.1-1.3.0.jar` | 1.3.0 | Modrinth (`create-aeronautics`) — contains Aeronautics 1.3.0, Simulated 1.3.0, Offroad 1.3.0 |

Licences, which decide what could be reused:

* **Create Aeronautics / Simulated — code is MIT**, assets are All Rights Reserved. So the
  *architecture* of their propeller actor could legitimately be studied and adapted; none of their
  art was used, and all textures/models/sounds here are generated from scratch by
  `tools/generate_assets.py` and `tools/generate_sounds.py`.
* **Sable — PolyForm Shield 1.0.0**. Fine to build against and depend on. No Sable code is copied,
  and no Sable class ships inside our jar.

## 2. The propulsion API, as it actually exists

### 2.1 How a block entity becomes a propulsion actor

```java
// dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor
public interface BlockEntitySubLevelActor {
    default void sable$tick(ServerSubLevel subLevel);
    default void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep);
    default Iterable<SubLevel> sable$getLoadingDependencies();
    default Iterable<SubLevel> sable$getConnectionDependencies();
}
```

**Registration is implicit.** `dev.ryanhcode.sable.sublevel.plot.LevelPlot#onBlockChange` does:

```java
BlockEntity blockEntity = level.getBlockEntity(pos);
actor = blockEntity instanceof BlockEntitySubLevelActor ? (BlockEntitySubLevelActor) blockEntity : null;
if (actor != null) this.blockEntityActors.put(pos, actor);
else               this.blockEntityActors.remove(pos);
```

Three consequences this addon relies on rather than reimplements:

1. Implementing the interface is the *entire* registration step.
2. The map is keyed by `BlockPos`, so one position can never contribute force twice — that is the
   structural answer to "no duplicate force after chunk reload".
3. Removing the block removes the entry, so a deleted block cannot leave a ghost force behind.

### 2.2 How force is applied

`ServerSubLevel#prePhysicsTick` drives the actors:

```java
for (BlockEntitySubLevelActor actor : plot.getBlockEntityActors())
    actor.sable$physicsTick(this, handle, timeStep);
```

and the force itself goes through a queued group:

```java
QueuedForceGroup group = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
group.applyAndRecordPointForce(point, force);   // (position, force) — in that order
```

`QueuedForceGroup#applyAndRecordPointForce` forwards to
`ForceTotal#applyImpulseAtPoint(MassTracker, point, force)`, and `ServerSubLevel#applyQueuedForces`
later calls `handle.applyForcesAndReset(group.getForceTotal())`.

**The critical detail: these coordinates are sublevel-LOCAL, not world.** Sable's own
`BlockEntitySubLevelPropellerActor` passes `JOMLConversion.atCenterOf(prop.getBlockPos())` — a raw
local block centre — and a direction built from `prop.getBlockDirection().getNormal()`. Sable does
the local→world conversion itself. Transforming by hand before calling this would apply the rotation
twice.

The value is an **impulse**, so a force is converted by multiplying by `timeStep`, exactly as the
reference propeller does (`thrustDirection.scale(prop.getScaledThrust() * timeStep)`).

### 2.3 Transforms (this is "Sable Companion")

`Sable Companion` is `dev.ryanhcode.sable.companion`, a separate library JarJar'd inside Sable.
It supplies the maths:

```java
// dev.ryanhcode.sable.companion.math.Pose3dc
Vec3 transformPosition(Vec3);        // local -> world  (includes translation)
Vec3 transformNormal(Vec3);          // local -> world  (rotation only)
Vec3 transformPositionInverse(Vec3);
Vec3 transformNormalInverse(Vec3);
```

```java
// dev.ryanhcode.sable.companion.SableCompanion (INSTANCE, works on both sides)
SubLevelAccess getContaining(Level, BlockPos);      // "am I inside a sublevel?"
SubLevelAccess getContaining(BlockEntity);
Vec3           projectOutOfSubLevel(Level, Vec3);   // local -> world position
Vector3d       getVelocity(Level, Vector3dc, Vector3d);
```

Positions and directions need **different** transforms. Using `transformPosition` on a direction is
what makes effects point sideways or appear mirrored; `SubLevelClientUtil` keeps the two apart.

### 2.4 Atmosphere

`DimensionPhysicsData.getAirPressure(Level, Vector3dc worldPos)` is what Sable's own propeller uses
for its `getCurrentAirPressure()` default. The sample point must be projected out of the sublevel
first, which is why `JetThrustModel#atmosphericEfficiency` calls `projectOutOfSubLevel` before
sampling.

## 3. What this addon does with that

```
Fan → Compressor ×(1..4) → Combustion Core → [Afterburner] → Nozzle
```

* **`CombustionCoreBlockEntity` is the only `BlockEntitySubLevelActor`.** One chain, one actor, one
  force contribution. The other four modules carry a plain block entity that exists only so the
  renderer has animation state to read.
* **`EngineScanner`** walks the axis outward from the core, bounded by
  `maxCompressorStages + 2` steps — never an unbounded scan. The result is cached in an
  `EngineChain` record and invalidated only by `neighborChanged` / `onPlace` / `onRemove` / load.
* **Threading.** `serverTick()` (game thread) owns all state and writes a `volatile double
  currentThrust`. `sable$physicsTick` runs inside the physics step and only reads volatiles before
  pushing the impulse, so the two never race.
* **Thrust direction** is `FACING.getOpposite()` in local space — exhaust leaves along `FACING`, so
  the reaction pushes the other way. The application point is the **nozzle** centre, not the body
  centre, which is what generates genuine torque for an off-centre installation (verified: an
  off-centre engine produces |ω| ≈ 5.8e-3 while a centred one does not).

### Deliberate divergences from the reference propeller

| Sable propeller | This addon | Why |
|---|---|---|
| Applies at the propeller's own block | Applies at the nozzle | Thrust physically leaves the nozzle; gives correct moment arm |
| `getScaledThrust()` negates thrust | Explicit `-FACING` direction | The propeller's sign convention is easy to get backwards; this states it directly |
| Airflow/ram term via `getAirflowScaling()` | Own ram term from `handle.getLinearVelocity()` | Jets need a top-speed falloff, not a propeller airflow ratio |
| Force computed inside the physics callback | Force computed on the game thread | Keeps world/config/redstone reads off the physics thread |

## 4. Rendering choice

Vanilla `BlockEntityRenderer`, not Flywheel. Sable ships
`VanillaSubLevelBlockEntityRenderer` and a `SubLevelRenderDispatcher.BlockEntityRenderer` path,
i.e. it explicitly renders vanilla block entities inside sublevels — so a plain BER keeps working on
a moving, rotating aircraft with no Flywheel version coupling. Animated parts are ordinary block
models registered through `ModelEvent.RegisterAdditional` and drawn with
`ModelBlockRenderer#renderModel`.

Facing rotations in the renderer reproduce vanilla's `BlockModelRotation` convention
(`rotationYXZ(-y, -x, 0)`) so animated geometry lands exactly where the blockstate puts the static
model.

## 5. Why the previous builds failed, and why this one cannot fail the same way

The old jars were compiled against hand-written stubs, so the class files carried descriptors that
did not exist at runtime (`Registries.BLOCK`, `Shapes.or`, `Component.translatable`,
`BlockState.setValue`). This project compiles against the real
`neoforge-21.1.238` artifact and the real Sable/Companion/Veil jars, so any such mismatch is a
compile error rather than a runtime `NoSuchMethodError`. The emitted bytecode was checked directly:

```
dev/ryanhcode/sable/api/physics/force/QueuedForceGroup.applyAndRecordPointForce:(Lorg/joml/Vector3dc;...
dev/ryanhcode/sable/sublevel/ServerSubLevel.getOrCreateQueuedForceGroup:(Ldev/ryanhcode/sable/api/physics/force/ForceGroup;...
dev/ryanhcode/sable/companion/math/JOMLConversion.atCenterOf:(Lnet/minecraft/core/Vec3i;...
dev/ryanhcode/sable/companion/math/Pose3d.transformNormal:(Lorg/joml/Vector3d;...
```

The shipped jar contains **only** `com/velocityspider/**` classes — no Minecraft, NeoForge, Create,
Sable or Veil classes are bundled.
