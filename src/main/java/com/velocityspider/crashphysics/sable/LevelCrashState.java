package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.damage.BlockDamageTracker;
import com.velocityspider.crashphysics.damage.CollapseHandler;
import com.velocityspider.crashphysics.damage.CrewHandler;
import com.velocityspider.crashphysics.damage.Destruction;
import com.velocityspider.crashphysics.damage.StructuralAnalyzer;
import com.velocityspider.crashphysics.effects.CrashEffects;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Everything crash physics tracks for one dimension.
 */
public final class LevelCrashState {
    private static final Map<ServerLevel, LevelCrashState> STATES = new ConcurrentHashMap<>();

    private final ServerLevel level;
    final ImpactCollector collector;
    private final ImpactResolver resolver;
    private final BlockDamageTracker damage;
    private final Destruction destruction;
    private final CrashEffects effects;
    private final CollapseHandler collapse;
    private final Reference2ObjectOpenHashMap<ServerSubLevel, BodyMotion> motions = new Reference2ObjectOpenHashMap<>();

    private long tick;
    private boolean tickStarted;
    private volatile int blockBreakAllowance = Integer.MAX_VALUE;

    private LevelCrashState(final ServerLevel level) {
        this.level = level;
        this.collector = new ImpactCollector(level, this);
        this.damage = new BlockDamageTracker(level);
        this.effects = new CrashEffects(level);
        this.collapse = new CollapseHandler(level);
        this.destruction = new Destruction(level, this.damage, this.effects, this.collapse);
        this.resolver = new ImpactResolver(level, this);
    }

    public static LevelCrashState get(final ServerLevel level) {
        return STATES.computeIfAbsent(level, LevelCrashState::new);
    }

    @Nullable
    public static LevelCrashState getIfPresent(final ServerLevel level) {
        return STATES.get(level);
    }

    static void remove(final ServerLevel level) {
        STATES.remove(level);
    }

    public ServerLevel level() {
        return this.level;
    }

    /**
     * @return the motion of a vehicle sampled before the current physics sub-step, or null if it hasn't been sampled
     */
    @Nullable
    public BodyMotion motion(final ServerSubLevel subLevel) {
        return this.motions.get(subLevel);
    }

    public BlockDamageTracker damage() {
        return this.damage;
    }

    public Destruction destruction() {
        return this.destruction;
    }

    public CrashEffects effects() {
        return this.effects;
    }

    /**
     * If the per-tick block breaking budget still has room. Checked from inside the physics step.
     */
    boolean canBreakMoreBlocks() {
        return this.blockBreakAllowance > 0;
    }

    /**
     * Called before every physics sub-step.
     */
    void prePhysics(final SubLevelPhysicsSystem system, final double dt) {
        final boolean firstSubstep = !this.tickStarted;
        this.tickStarted = true;

        if (firstSubstep && CrashConfig.isLoaded()) {
            this.blockBreakAllowance = CrashConfig.MAX_BLOCKS_BROKEN_PER_TICK.getAsInt();
            this.destruction.beginTick();
        }

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container != null) {
            final Vector3d gravity = DimensionPhysicsData.getGravity(this.level);
            for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
                if (subLevel.isRemoved()) {
                    continue;
                }

                final RigidBodyHandle handle = system.getPhysicsHandle(subLevel);
                if (handle == null || !handle.isValid()) {
                    continue;
                }

                final BodyMotion motion = this.motions.computeIfAbsent(subLevel, BodyMotion::new);
                motion.sample(handle, dt, gravity, firstSubstep);
                motion.lastSeenTick = this.tick;
            }
        }

        this.collector.beginSubstep(dt);
    }

    /**
     * Called after every physics sub-step, outside of Rapier.
     */
    void postPhysics(final SubLevelPhysicsSystem system) {
        this.resolver.resolve(system, this.collector.drainRecords(), this.collector.drainSkids());
        this.blockBreakAllowance = this.destruction.remainingBlockBudget();
    }

    /**
     * Called at the end of every game tick of this dimension.
     */
    void endTick() {
        this.tickStarted = false;

        if (CrashConfig.isLoaded() && CrashConfig.DEBUG_LOGGING.getAsBoolean() && this.collector.statCallbacks > 0) {
            CrashPhysics.LOGGER.info("[crashphysics] tick {}: {} callbacks, {} fast, {} evaluated, {} crushing ({} carried through); {} vehicles tracked",
                    this.tick, this.collector.statCallbacks, this.collector.statFast, this.collector.statEvaluated, this.collector.statCrushing,
                    this.collector.statThrough, this.motions.size());
        }
        this.collector.statCallbacks = 0;
        this.collector.statFast = 0;
        this.collector.statEvaluated = 0;
        this.collector.statCrushing = 0;
        this.collector.statThrough = 0;

        if (CrashConfig.isLoaded() && CrashConfig.ENABLED.getAsBoolean()) {
            final SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get(this.level);

            for (final BodyMotion motion : this.motions.values()) {
                if (motion.subLevel.isRemoved() || motion.peakG <= 0.0) {
                    continue;
                }

                if (CrashConfig.DEBUG_LOGGING.getAsBoolean() && motion.peakG > 2.0) {
                    CrashPhysics.LOGGER.info("[crashphysics] {} peaked at {} g with {} impact blocks", motion.subLevel,
                            String.format("%.1f", motion.peakG), motion.impactBlocks.size());
                }

                if (CrashConfig.CREW_INJURY.getAsBoolean()) {
                    CrewHandler.apply(this.level, motion.subLevel, motion.pose, motion.peakG, motion.peakLinearAcceleration,
                            motion.peakAngularAcceleration, motion.peakAngularVelocity, motion.tickStartLinear, motion.linear);
                }

                if (CrashConfig.STRUCTURAL_FAILURE.getAsBoolean() && CrashConfig.VEHICLE_DAMAGE.getAsBoolean()
                        && motion.peakG >= CrashConfig.STRUCTURAL_MIN_G.getAsDouble() && !motion.impactBlocks.isEmpty() && system != null) {
                    StructuralAnalyzer.analyze(this, motion.subLevel, motion.pose, motion.peakLinearAcceleration,
                            motion.peakAngularAcceleration, motion.peakAngularVelocity, motion.impactBlocks);
                }
            }

            this.destruction.endTick();
            this.collapse.process(this.destruction);
            this.effects.flush();
            this.damage.expire(this.tick, CrashConfig.DAMAGE_MEMORY_SECONDS.getAsInt() * 20L);
        }

        for (final BodyMotion motion : this.motions.values()) {
            motion.resetTick();
        }
        this.motions.values().removeIf(motion -> motion.subLevel.isRemoved() || this.tick - motion.lastSeenTick > 40);
        this.tick++;
    }

    public long tick() {
        return this.tick;
    }

    void recordImpact(final ServerSubLevel vehicle, final long plotBlock) {
        final BodyMotion motion = this.motions.get(vehicle);
        if (motion != null) {
            motion.impactBlocks.add(plotBlock);
        }
    }
}
