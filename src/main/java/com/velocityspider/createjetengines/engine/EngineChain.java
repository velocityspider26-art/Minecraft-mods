package com.velocityspider.createjetengines.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * An immutable snapshot of one assembled engine, produced by {@link EngineScanner}.
 *
 * <p>This is cached on the combustion core and only recomputed when a relevant neighbour changes,
 * so a running engine costs no scanning work per tick.
 */
public record EngineChain(
        boolean valid,
        @Nullable Invalidity reason,
        Direction exhaustDirection,
        @Nullable BlockPos fanPos,
        @Nullable BlockPos corePos,
        @Nullable BlockPos afterburnerPos,
        @Nullable BlockPos nozzlePos,
        int compressorStages,
        boolean hasAfterburner,
        boolean intakeObstructed,
        boolean exhaustObstructed) {

    /** Why a chain failed to assemble. Surfaced to the player and useful when debugging. */
    public enum Invalidity {
        NO_FAN("no_fan"),
        NO_COMPRESSOR("no_compressor"),
        TOO_MANY_COMPRESSORS("too_many_compressors"),
        NO_NOZZLE("no_nozzle"),
        MISALIGNED("misaligned"),
        DUPLICATE_MODULE("duplicate_module");

        private final String key;

        Invalidity(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "engine.create_jet_engines.invalid." + key;
        }
    }

    public static EngineChain invalid(Direction facing, Invalidity reason) {
        return new EngineChain(false, reason, facing, null, null, null, null, 0, false, false, false);
    }

    /** Total module count, intake to exhaust. Used for sound/visual scaling. */
    public int moduleCount() {
        if (!valid) {
            return 0;
        }
        return 3 + compressorStages - 1 + (hasAfterburner ? 1 : 0);
    }

    /**
     * The point thrust is applied at, in sublevel-local block coordinates.
     * Physically the reaction acts along the engine axis; using the nozzle centre gives correct
     * torque for off-centre installations.
     */
    @Nullable
    public BlockPos thrustApplicationPos() {
        return nozzlePos;
    }
}
