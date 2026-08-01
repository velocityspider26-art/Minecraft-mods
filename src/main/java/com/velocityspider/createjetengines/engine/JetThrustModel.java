package com.velocityspider.createjetengines.engine;

import com.velocityspider.createjetengines.config.JetEngineConfig;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * The thrust curve.
 *
 * <pre>
 *   dry   = baseThrust * spool^2 * throttle * compressorMult * intakeEff * atmosphericEff
 *   total = dry * (afterburnerLit ? afterburnerMult : 1)
 * </pre>
 *
 * <p>{@code spool^2} is what makes a freshly started engine nearly useless and full static thrust
 * something you have to spool up to. Nothing here knows the aircraft's mass — the physics body
 * applies F = ma on its own, so the same engine naturally accelerates a light frame harder.
 */
public final class JetThrustModel {

    private JetThrustModel() {
    }

    public static double computeThrust(Level level, BlockPos corePos, EngineChain chain,
                                       float throttle, float spool, boolean afterburnerLit) {
        if (!chain.valid() || throttle <= 0.0F || spool <= 0.0F) {
            return 0.0D;
        }
        JetEngineConfig cfg = JetEngineConfig.INSTANCE;

        double thrust = cfg.baseThrust.get();
        thrust *= spool * spool;
        thrust *= throttle;
        thrust *= compressorMultiplier(chain.compressorStages());

        if (chain.intakeObstructed()) {
            thrust *= cfg.obstructedIntakeEfficiency.get();
        }
        if (chain.exhaustObstructed()) {
            thrust *= cfg.obstructedExhaustEfficiency.get();
        }
        if (cfg.useAtmosphericEfficiency.get()) {
            thrust *= atmosphericEfficiency(level, corePos);
        }
        if (afterburnerLit) {
            thrust *= cfg.afterburnerMultiplier.get();
        }
        return Math.max(0.0D, thrust);
    }

    /** Diminishing returns per stage: 1 stage is the baseline, extra stages help progressively less. */
    public static double compressorMultiplier(int stages) {
        if (stages <= 1) {
            return 1.0D;
        }
        int capped = Math.min(stages, JetEngineConfig.INSTANCE.maxCompressorStages.get());
        double m = JetEngineConfig.INSTANCE.compressorStageMultiplier.get();
        return 1.0D + m * (1.0D - 1.0D / capped);
    }

    /**
     * A jet needs air. Sable exposes per-dimension air pressure, and the engine may be sitting
     * inside a moving sublevel, so the sample point is projected out to real world coordinates
     * first rather than using raw local coordinates.
     */
    private static double atmosphericEfficiency(Level level, BlockPos corePos) {
        try {
            Vec3 local = corePos.getCenter();
            Vec3 world = SableCompanion.INSTANCE.projectOutOfSubLevel(level, local);
            Vector3d joml = JOMLConversion.toJOML(world);
            double pressure = DimensionPhysicsData.getAirPressure(level, joml);
            double floor = JetEngineConfig.INSTANCE.minAtmosphericEfficiency.get();
            return Math.max(floor, Math.min(1.0D, pressure));
        } catch (Throwable t) {
            // Never let an atmosphere lookup take the engine down.
            return 1.0D;
        }
    }
}
