package com.velocityspider.createjetengines.blockentity;

import com.velocityspider.createjetengines.CreateJetEngines;
import com.velocityspider.createjetengines.block.JetModuleBlock;
import com.velocityspider.createjetengines.config.JetEngineConfig;
import com.velocityspider.createjetengines.engine.EngineChain;
import com.velocityspider.createjetengines.engine.EngineScanner;
import com.velocityspider.createjetengines.engine.JetThrustModel;
import com.velocityspider.createjetengines.registry.JetBlockEntities;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import javax.annotation.Nullable;

/**
 * The combustion core: engine brain, state authority, and the chain's single Sable actor.
 *
 * <p>Sable discovers this automatically. {@code LevelPlot#onBlockChange} keys actors by position and
 * stores any block entity that implements {@link BlockEntitySubLevelActor}, so placing the core
 * registers it and breaking it removes it with no bookkeeping here — which is exactly what stops
 * ghost forces from deleted blocks and double-counting after a chunk reload.
 *
 * <p>Threading: {@link #serverTick()} runs on the server thread and owns all state. The physics
 * callback only reads a couple of volatile fields and pushes an impulse, so it never races the
 * game thread.
 */
public class CombustionCoreBlockEntity extends JetModuleBlockEntity implements BlockEntitySubLevelActor {

    // ---- authoritative state (server owns, client mirrors) ----------------------------------
    private float throttleTarget;
    private float throttle;
    private float spool;
    private boolean running;
    private boolean afterburnerActive;
    private float temperature;

    /** Newton-equivalent thrust computed on the game thread, consumed by the physics step. */
    private volatile double currentThrust;
    private volatile boolean actorActive;

    @Nullable
    private EngineChain chain;
    private boolean chainDirty = true;

    private int syncCooldown;
    private float lastSyncedSpool = -1.0F;
    private int debugCooldown;

    // ---- client-only interpolation ------------------------------------------------------------
    private float clientSpool;
    private float clientPrevSpool;

    public CombustionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(JetBlockEntities.COMBUSTION_CORE.get(), pos, state);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        chainDirty = true;
    }

    public EngineChain getChain() {
        if (chainDirty || chain == null) {
            Level level = getLevel();
            if (level == null) {
                return EngineChain.invalid(getFacing(), EngineChain.Invalidity.NO_FAN);
            }
            Direction facing = getBlockState().getBlock() instanceof JetModuleBlock
                    ? getBlockState().getValue(JetModuleBlock.FACING)
                    : Direction.NORTH;
            chain = EngineScanner.scan(level, getBlockPos(), facing);
            chainDirty = false;
        }
        return chain;
    }

    // =========================================================================================
    // Server tick: throttle, spool, thrust
    // =========================================================================================

    public void serverTick() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        JetEngineConfig cfg = JetEngineConfig.INSTANCE;
        EngineChain c = getChain();

        // ---- throttle input -----------------------------------------------------------------
        int signal = level.getBestNeighborSignal(getBlockPos());
        float demanded = c.valid() ? signal / 15.0F : 0.0F;
        throttleTarget = demanded;

        float slew = cfg.throttleSlewRate.get().floatValue();
        throttle += Math.max(-slew, Math.min(slew, throttleTarget - throttle));
        throttle = clamp01(throttle);

        running = c.valid() && (throttle > 0.001F || spool > 0.001F);

        // ---- spool -------------------------------------------------------------------------
        float idle = cfg.idleSpool.get().floatValue();
        // Demanded spool never drops below idle while the engine is lit.
        float spoolTarget = c.valid() && throttleTarget > 0.001F
                ? Math.max(idle, throttleTarget)
                : 0.0F;

        float rate = spoolTarget > spool
                ? cfg.spoolUpRate.get().floatValue()
                : cfg.spoolDownRate.get().floatValue();
        spool += Math.max(-rate, Math.min(rate, spoolTarget - spool));
        spool = clamp01(spool);

        // ---- afterburner --------------------------------------------------------------------
        boolean wasLit = afterburnerActive;
        afterburnerActive = c.valid()
                && c.hasAfterburner()
                && spool >= cfg.afterburnerMinSpool.get().floatValue()
                && throttle >= cfg.afterburnerMinSpool.get().floatValue()
                && !c.exhaustObstructed();

        // ---- thrust -------------------------------------------------------------------------
        double thrust = JetThrustModel.computeThrust(level, getBlockPos(), c, throttle, spool, afterburnerActive);
        currentThrust = thrust;
        actorActive = c.valid() && thrust > 0.0D;

        // ---- temperature (cosmetic, drives glow) ---------------------------------------------
        float targetTemp = spool * (afterburnerActive ? 1.0F : 0.75F);
        temperature += (targetTemp - temperature) * 0.05F;

        maybeSync(level, wasLit);
        maybeLogDebug(level, c, thrust);
    }

    private void maybeSync(Level level, boolean wasAfterburnerLit) {
        boolean flagsChanged = wasAfterburnerLit != afterburnerActive;
        boolean spoolChanged = Math.abs(spool - lastSyncedSpool) > 0.02F;
        if (syncCooldown > 0) {
            syncCooldown--;
        }
        if (flagsChanged || (spoolChanged && syncCooldown == 0)) {
            lastSyncedSpool = spool;
            syncCooldown = 4;
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
            setChanged();
        }
    }

    private void maybeLogDebug(Level level, EngineChain c, double thrust) {
        if (!JetEngineConfig.INSTANCE.logPropulsionDebug.get()) {
            return;
        }
        if (debugCooldown-- > 0) {
            return;
        }
        debugCooldown = 20;
        CreateJetEngines.LOGGER.info(
                "[jet] core={} valid={} reason={} stages={} ab={} intakeBlocked={} exhaustBlocked={} throttle={} spool={} thrust={}",
                getBlockPos(), c.valid(), c.reason(), c.compressorStages(), c.hasAfterburner(),
                c.intakeObstructed(), c.exhaustObstructed(),
                String.format("%.3f", throttle), String.format("%.3f", spool), String.format("%.1f", thrust));
    }

    public void clientTick() {
        clientPrevSpool = clientSpool;
        clientSpool += (spool - clientSpool) * 0.2F;
    }

    // =========================================================================================
    // Sable propulsion
    // =========================================================================================

    /**
     * Applies jet thrust to the sublevel's rigid body.
     *
     * <p>Mirrors the integration pattern of Sable's own
     * {@code BlockEntitySubLevelPropellerActor}: build the thrust vector in <em>sublevel-local</em>
     * space, apply it at the module's local block centre, and let Sable transform it into world
     * space. Applying at the nozzle rather than the body centre is what produces real torque for
     * off-centre engines — and why two symmetric engines cancel each other's yaw.
     *
     * @param timeStep physics step length; multiplying by it turns force into the impulse
     *                 {@code applyAndRecordPointForce} expects.
     */
    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (!actorActive || !handle.isValid()) {
            return;
        }
        EngineChain c = chain;
        if (c == null || !c.valid()) {
            return;
        }
        BlockPos applyAt = c.thrustApplicationPos();
        if (applyAt == null) {
            return;
        }

        // Exhaust leaves along FACING, so the reaction pushes the aircraft the other way.
        Direction exhaust = c.exhaustDirection();
        Vector3d localThrustDir = new Vector3d(
                -exhaust.getStepX(), -exhaust.getStepY(), -exhaust.getStepZ());

        double magnitude = currentThrust;

        // Ram drag: as the airframe approaches exhaust velocity the engine stops adding speed,
        // which gives a natural top speed instead of unbounded acceleration.
        if (JetEngineConfig.INSTANCE.useRamEfficiency.get()) {
            Vector3d worldDir = subLevel.logicalPose().transformNormal(new Vector3d(localThrustDir));
            org.joml.Vector3dc vel = handle.getLinearVelocity();
            double along = vel.dot(worldDir.x, worldDir.y, worldDir.z);
            double maxSpeed = JetEngineConfig.INSTANCE.maxAirspeed.get();
            double ram = 1.0D - (along / maxSpeed);
            magnitude *= Math.max(0.0D, Math.min(1.0D, ram));
        }

        double maxForce = JetEngineConfig.INSTANCE.maxForcePerEngine.get();
        magnitude = Math.max(0.0D, Math.min(maxForce, magnitude));
        if (magnitude <= 1.0E-6D) {
            return;
        }

        Vector3d impulse = localThrustDir.mul(magnitude * timeStep, new Vector3d());
        Vector3d point = JOMLConversion.atCenterOf(applyAt);

        QueuedForceGroup group = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
        group.applyAndRecordPointForce(point, impulse);

        if (JetEngineConfig.INSTANCE.logPropulsionDebug.get() && debugCooldown == 20) {
            CreateJetEngines.LOGGER.info(
                    "[jet] actor fired: subLevel={} bodyValid={} point={} impulse={} dt={}",
                    subLevel.getUniqueId(), handle.isValid(), point, impulse, timeStep);
        }
    }

    // =========================================================================================
    // State accessors
    // =========================================================================================

    @Override
    public float getSpool() {
        return getLevel() != null && getLevel().isClientSide() ? clientSpool : spool;
    }

    public float getSpool(float partialTick) {
        return clientPrevSpool + (clientSpool - clientPrevSpool) * partialTick;
    }

    @Override
    public float getThrottle() {
        return throttle;
    }

    @Override
    public boolean isAfterburnerActive() {
        return afterburnerActive;
    }

    public boolean isRunning() {
        return running;
    }

    public float getTemperature() {
        return temperature;
    }

    public double getCurrentThrust() {
        return currentThrust;
    }

    /** 0 = fully closed nozzle, 1 = fully open. Opens with dry power and snaps wide on reheat. */
    public float getNozzleOpen() {
        float base = getSpool() * 0.6F;
        return clamp01(afterburnerActive ? base + 0.4F : base);
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
    }

    // =========================================================================================
    // Persistence and sync — real 1.21.1 HolderLookup.Provider signatures
    // =========================================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Throttle", throttle);
        tag.putFloat("ThrottleTarget", throttleTarget);
        tag.putFloat("Spool", spool);
        tag.putFloat("Temperature", temperature);
        tag.putBoolean("Running", running);
        tag.putBoolean("Afterburner", afterburnerActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        throttle = tag.getFloat("Throttle");
        throttleTarget = tag.getFloat("ThrottleTarget");
        spool = tag.getFloat("Spool");
        temperature = tag.getFloat("Temperature");
        running = tag.getBoolean("Running");
        afterburnerActive = tag.getBoolean("Afterburner");
        clientSpool = spool;
        clientPrevSpool = spool;
        // Geometry may have changed while unloaded.
        chainDirty = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
