package com.velocityspider.crashphysics.damage;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.effects.CrashEffects;
import com.velocityspider.crashphysics.material.CrashMaterial;
import com.velocityspider.crashphysics.material.CrashMaterials;
import com.velocityspider.crashphysics.material.FractureMode;
import com.velocityspider.crashphysics.physics.Ejecta;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Breaks blocks for crashes, and decides what is left over: rubble thrown out of craters, wreckage flying off vehicles,
 * torn turf, crushed plants, skid marks, fires and explosions.
 * <p>
 * Breaking is queued and limited per tick, so one enormous crash can't freeze the server.
 */
public final class Destruction {

    /**
     * Thrown wreckage never leaves faster than this [m/s]
     */
    private static final double MAX_DEBRIS_SPEED = 80.0;

    private final ServerLevel level;
    private final BlockDamageTracker damage;
    private final CrashEffects effects;
    private final CollapseHandler collapse;
    private final RandomSource random = RandomSource.create();
    private final RandomGenerator ejectaRandom = new java.util.SplittableRandom();

    private final ArrayDeque<Break> queue = new ArrayDeque<>();
    private final List<Volatile> volatiles = new ArrayList<>();

    private int blocksBroken;
    private int flyingDebris;
    private int maxBlocks = 600;
    private int maxFlyingDebris = 48;

    // Lifetime totals, for tests and debugging
    private long terrainBroken;
    private long vehicleBlocksBroken;
    private long jointsTorn;

    private record Break(@Nullable ServerSubLevel vehicle, BlockPos pos, BlockState expected, CrashMaterial material,
                         double motionX, double motionY, double motionZ, double penetrationSpeed, boolean pulverize) {
    }

    private record Volatile(double x, double y, double z, float power, boolean fire) {
    }

    /**
     * A block a vehicle slid over.
     */
    public record Skid(BlockPos pos, BlockState state, BlockState into, double speed) {
    }

    public Destruction(final ServerLevel level, final BlockDamageTracker damage, final CrashEffects effects, final CollapseHandler collapse) {
        this.level = level;
        this.damage = damage;
        this.effects = effects;
        this.collapse = collapse;
    }

    public void beginTick() {
        this.blocksBroken = 0;
        this.flyingDebris = 0;
        this.maxBlocks = CrashConfig.MAX_BLOCKS_BROKEN_PER_TICK.getAsInt();
        this.maxFlyingDebris = CrashConfig.MAX_FLYING_DEBRIS_PER_TICK.getAsInt();
    }

    /**
     * @return how many more blocks may be broken this tick, counting the ones already queued
     */
    public int remainingBlockBudget() {
        return this.maxBlocks - this.blocksBroken - this.queue.size();
    }

    /**
     * Queues a terrain block that was crushed through.
     *
     * @param motionX          direction the vehicle was moving (for where the rubble goes)
     * @param penetrationSpeed how fast the block was being penetrated [m/s]
     */
    public void breakTerrain(final BlockPos pos, final BlockState expected, final CrashMaterial material,
                             final double motionX, final double motionY, final double motionZ, final double penetrationSpeed) {
        this.queue.add(new Break(null, pos.immutable(), expected, material, motionX, motionY, motionZ, penetrationSpeed, false));
    }

    /**
     * Queues a block of a vehicle that was crushed or torn off.
     *
     * @param pulverize if the block is destroyed outright instead of possibly flying off as wreckage
     */
    public void breakVehicleBlock(final ServerSubLevel vehicle, final BlockPos plotPos, final BlockState expected, final CrashMaterial material, final boolean pulverize) {
        this.queue.add(new Break(vehicle, plotPos.immutable(), expected, material, 0.0, 0.0, 0.0, 0.0, pulverize));
    }

    /**
     * Breaks queued blocks, within this tick's budget.
     */
    public void process() {
        while (!this.queue.isEmpty() && this.blocksBroken < this.maxBlocks) {
            final Break entry = this.queue.poll();
            try {
                if (entry.vehicle == null) {
                    this.breakTerrainNow(entry);
                } else {
                    this.breakVehicleBlockNow(entry);
                }
            } catch (final RuntimeException e) {
                CrashPhysics.LOGGER.error("Failed to break {} at {}", entry.expected, entry.pos, e);
            }
        }
    }

    /**
     * Runs the explosions and leftover breaks queued during this tick.
     */
    public void endTick() {
        this.process();

        int explosions = 0;
        for (final Volatile entry : this.volatiles) {
            if (explosions++ >= 4) {
                break;
            }
            this.level.explode(null, entry.x, entry.y, entry.z, entry.power, entry.fire, Level.ExplosionInteraction.BLOCK);
        }
        this.volatiles.clear();
    }

    private void breakTerrainNow(final Break entry) {
        final BlockState current = this.level.getBlockState(entry.pos);
        if (current.isAir() || current.getBlock() != entry.expected.getBlock()) {
            this.damage.forget(entry.pos.asLong());
            return;
        }
        this.blocksBroken++;
        this.terrainBroken++;

        // Grass, flowers and snow on top are crushed with it
        this.crushCover(entry.pos.above());

        final double x = entry.pos.getX() + 0.5;
        final double y = entry.pos.getY() + 0.5;
        final double z = entry.pos.getZ() + 0.5;

        final BlockState ejecta = entry.material.ejecta();
        final boolean throwIt = ejecta != null && CrashConfig.EJECTA.getAsBoolean() && entry.penetrationSpeed > 3.0
                && this.flyingDebris < this.maxFlyingDebris && this.random.nextDouble() < CrashConfig.EJECTA_CHANCE.getAsDouble();

        if (throwIt) {
            this.level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, entry.pos, Block.getId(current));
            this.level.setBlock(entry.pos, current.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            this.throwEjecta(entry, ejecta, x, y, z);
        } else {
            final boolean drop = entry.material.fracture() != FractureMode.GRANULAR
                    && (current.hasBlockEntity() || this.random.nextDouble() < CrashConfig.DROP_CHANCE.getAsDouble());
            this.level.destroyBlock(entry.pos, drop);
        }

        this.scarFloor(entry.pos.below());
        this.handleVolatile(current, entry.material, x, y, z);
        this.collapse.seed(entry.pos);
    }

    private void breakVehicleBlockNow(final Break entry) {
        final ServerSubLevel vehicle = entry.vehicle;
        if (vehicle == null || vehicle.isRemoved()) {
            return;
        }

        // Vehicle blocks live at absolute level coordinates inside the vehicle's plot
        final BlockState current = this.level.getBlockState(entry.pos);
        if (current.isAir() || current.getBlock() != entry.expected.getBlock()) {
            this.damage.forget(entry.pos.asLong());
            return;
        }
        this.blocksBroken++;
        this.vehicleBlocksBroken++;

        final Vector3d world = vehicle.logicalPose().transformPosition(new Vector3d(entry.pos.getX() + 0.5, entry.pos.getY() + 0.5, entry.pos.getZ() + 0.5));
        final FractureMode fracture = entry.material.fracture();

        final boolean wreckage = !entry.pulverize && !current.hasBlockEntity() && this.flyingDebris < this.maxFlyingDebris
                && fracture != FractureMode.GLASS && fracture != FractureMode.SOFT && fracture != FractureMode.PLANT
                && this.random.nextDouble() < CrashConfig.VEHICLE_DEBRIS_CHANCE.getAsDouble();

        if (wreckage) {
            this.level.setBlock(entry.pos, current.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            this.throwWreckage(vehicle, current, entry.pos, world);
        } else {
            final boolean drop = current.hasBlockEntity() || this.random.nextDouble() < CrashConfig.DROP_CHANCE.getAsDouble();
            this.level.destroyBlock(entry.pos, drop);
        }

        this.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, current), world.x, world.y, world.z, 10, 0.35, 0.35, 0.35, 0.2);
        this.handleVolatile(current, entry.material, world.x, world.y, world.z);
    }

    private void throwEjecta(final Break entry, final BlockState ejecta, final double x, final double y, final double z) {
        final double[] velocity = new double[3];
        Ejecta.launchVelocity(entry.motionX, entry.motionY, entry.motionZ, entry.penetrationSpeed,
                CrashConfig.EJECTA_SPEED_FACTOR.getAsDouble(), 22.0, this.ejectaRandom, velocity);

        final FallingBlockEntity rubble = new FallingBlockEntity(this.level, x, y - 0.5 + 0.6, z, ejecta);
        rubble.dropItem = false;
        rubble.setHurtsEntities(0.5f, 6);
        // Minecraft entity velocities are in blocks per tick
        rubble.setDeltaMovement(velocity[0] / 20.0, velocity[1] / 20.0, velocity[2] / 20.0);
        this.level.addFreshEntity(rubble);
        this.flyingDebris++;
    }

    private void throwWreckage(final ServerSubLevel vehicle, final BlockState state, final BlockPos plotPos, final Vector3d world) {
        // Sable wants the point in the vehicle's plot, not in the world
        final Vector3d plotCenter = new Vector3d(plotPos.getX() + 0.5, plotPos.getY() + 0.5, plotPos.getZ() + 0.5);
        final Vector3d velocity = Sable.HELPER.getVelocity(this.level, vehicle, plotCenter, new Vector3d());
        if (!velocity.isFinite()) {
            velocity.zero();
        }

        // Torn off outwards from the vehicle, with some scatter
        final Vector3d outward = new Vector3d(world).sub(vehicle.logicalPose().position());
        if (outward.lengthSquared() > 1.0e-6) {
            outward.normalize(2.0);
        }
        final double scatter = 1.5 + 0.12 * velocity.length();
        velocity.add(outward).add(
                (this.random.nextDouble() - 0.5) * scatter,
                (this.random.nextDouble() - 0.2) * scatter,
                (this.random.nextDouble() - 0.5) * scatter
        );

        if (velocity.length() > MAX_DEBRIS_SPEED) {
            velocity.normalize(MAX_DEBRIS_SPEED);
        }

        final FallingBlockEntity wreckage = new FallingBlockEntity(this.level, world.x, world.y - 0.5, world.z, state);
        wreckage.dropItem = true;
        wreckage.setHurtsEntities(2.0f, 40);
        wreckage.setDeltaMovement(velocity.x / 20.0, velocity.y / 20.0, velocity.z / 20.0);
        this.level.addFreshEntity(wreckage);
        this.flyingDebris++;
    }

    /**
     * Plants, snow layers and other things without a collision box are crushed along with the block they stand on.
     */
    private void crushCover(final BlockPos pos) {
        final BlockState cover = this.level.getBlockState(pos);
        if (!cover.isAir() && cover.getFluidState().isEmpty() && cover.getCollisionShape(this.level, pos).isEmpty()) {
            this.level.destroyBlock(pos, false);
        }
    }

    /**
     * The exposed floor of a crater gets torn up and compacted.
     */
    private void scarFloor(final BlockPos pos) {
        if (this.random.nextFloat() > 0.6f) {
            return;
        }

        final BlockState floor = this.level.getBlockState(pos);
        final BlockState scar = CrashMaterials.get(floor).scar();
        if (scar != null && floor.getBlock() != scar.getBlock() && this.level.getBlockState(pos.above()).getCollisionShape(this.level, pos.above()).isEmpty()) {
            this.level.setBlock(pos, scar, Block.UPDATE_ALL);
        }
    }

    private void handleVolatile(final BlockState state, final CrashMaterial material, final double x, final double y, final double z) {
        if (!CrashConfig.FIRE_AND_EXPLOSIONS.getAsBoolean()) {
            return;
        }

        if (material.volatility() > 0.0f) {
            this.volatiles.add(new Volatile(x, y, z, material.volatility(), material.ignites()));
            return;
        }

        final boolean hot = material.ignites() || (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT));
        if (hot) {
            this.ignite(x, y, z);
        }
    }

    private void ignite(final double x, final double y, final double z) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 6; attempt++) {
            pos.set(x + this.random.nextInt(5) - 2, y + this.random.nextInt(3) - 1, z + this.random.nextInt(5) - 2);
            if (this.level.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(this.level, pos, Direction.UP)) {
                this.level.setBlock(pos, BaseFireBlock.getState(this.level, pos), Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    /**
     * Turns grass under a sliding vehicle into torn turf, ploughs away snow and mows plants.
     */
    public void skid(final List<Skid> skids) {
        for (final Skid skid : skids) {
            if (this.blocksBroken >= this.maxBlocks) {
                return;
            }

            final BlockState current = this.level.getBlockState(skid.pos);
            if (current != skid.state) {
                continue;
            }

            final double chance = Math.min(0.9, 0.25 + (skid.speed - 6.0) / 30.0);
            if (this.random.nextDouble() >= chance) {
                continue;
            }

            this.crushCover(skid.pos.above());
            if (skid.into.isAir()) {
                this.level.destroyBlock(skid.pos, false);
            } else {
                this.level.setBlock(skid.pos, skid.into, Block.UPDATE_ALL);
            }
            this.blocksBroken++;

            this.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, current),
                    skid.pos.getX() + 0.5, skid.pos.getY() + 1.05, skid.pos.getZ() + 0.5, 6, 0.4, 0.05, 0.4, 0.08);
        }
    }

    /**
     * Breaks a vehicle block torn off by structural failure.
     */
    public void tearOff(final ServerSubLevel vehicle, final BlockPos plotPos) {
        final BlockState state = this.level.getBlockState(plotPos);
        if (!state.isAir()) {
            this.jointsTorn++;
            this.breakVehicleBlock(vehicle, plotPos, state, CrashMaterials.get(state), false);
        }
    }

    public CrashEffects effects() {
        return this.effects;
    }

    /**
     * @return terrain blocks broken by crashes so far
     */
    public long terrainBroken() {
        return this.terrainBroken;
    }

    /**
     * @return vehicle blocks broken by crashes so far
     */
    public long vehicleBlocksBroken() {
        return this.vehicleBlocksBroken;
    }

    /**
     * @return vehicle blocks broken because a joint failed under crash loads so far
     */
    public long jointsTorn() {
        return this.jointsTorn;
    }
}
