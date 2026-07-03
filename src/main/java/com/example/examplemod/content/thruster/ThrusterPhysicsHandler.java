package com.example.examplemod.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Turns a thruster's current state into a force sample and hands it to {@link SableThrustAdapter}.
 * Mirrors Propulsion's {@code ThrusterForceProvider}: the force acts along the thrust axis and is
 * applied at the nozzle point so Sable derives torque from the mounting offset automatically.
 */
public final class ThrusterPhysicsHandler {
    /** One game tick in seconds. */
    private static final double DT = 1.0 / 20.0;

    private ThrusterPhysicsHandler() {
    }

    /**
     * Applies this tick's thrust to the Sable body the thruster is mounted on (if any).
     *
     * @param level    the thruster's level
     * @param pos      the thruster block position
     * @param facing   the nozzle/exhaust direction (thrust pushes the opposite way)
     * @param thrust   magnitude in [0,1]-ish throttle already multiplied by the fuel multiplier
     * @return true if a physics force was applied
     */
    public static boolean applyThrust(Level level, BlockPos pos, Direction facing, double thrust) {
        if (thrust <= 1.0e-4) {
            return false;
        }
        double magnitude = thrust * ThrusterConfig.BASE_THRUST.get();

        // Thrust pushes opposite the exhaust (exhaust exits along FACING in our convention).
        double dx = -facing.getStepX();
        double dy = -facing.getStepY();
        double dz = -facing.getStepZ();

        // Application point: block centre, pushed along the thrust axis by the nozzle offset.
        double offset = ThrusterConfig.NOZZLE_OFFSET.get();
        double px = pos.getX() + 0.5 + dx * offset;
        double py = pos.getY() + 0.5 + dy * offset;
        double pz = pos.getZ() + 0.5 + dz * offset;

        return SableThrustAdapter.applyThrust(level, pos, px, py, pz,
                dx * magnitude, dy * magnitude, dz * magnitude, DT);
    }
}
