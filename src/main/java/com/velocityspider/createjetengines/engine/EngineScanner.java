package com.velocityspider.createjetengines.engine;

import com.velocityspider.createjetengines.block.JetModuleBlock;
import com.velocityspider.createjetengines.config.JetEngineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Walks the engine axis outwards from a combustion core and classifies the result.
 *
 * <p>The walk is strictly bounded: at most {@code maxCompressorStages} steps upstream plus two
 * downstream, so this never scans an unbounded distance. Callers cache the result and only rescan
 * on neighbour/load/unload changes.
 */
public final class EngineScanner {

    private EngineScanner() {
    }

    /**
     * @param corePos position of the combustion core
     * @param facing  the module facing, which points downstream toward the exhaust
     */
    public static EngineChain scan(LevelReader level, BlockPos corePos, Direction facing) {
        Direction upstream = facing.getOpposite();

        // ---- upstream: compressors then the fan --------------------------------------------
        int maxStages = JetEngineConfig.INSTANCE.maxCompressorStages.get();
        int compressors = 0;
        BlockPos cursor = corePos.relative(upstream);
        BlockPos fanPos = null;

        // +1 so one compressor too many is detected rather than silently truncated.
        for (int i = 0; i <= maxStages; i++) {
            ModuleType type = moduleAt(level, cursor, facing);
            if (type == ModuleType.COMPRESSOR) {
                compressors++;
                cursor = cursor.relative(upstream);
                continue;
            }
            if (type == ModuleType.FAN) {
                fanPos = cursor;
            }
            break;
        }

        if (compressors > maxStages) {
            return EngineChain.invalid(facing, EngineChain.Invalidity.TOO_MANY_COMPRESSORS);
        }
        if (compressors == 0) {
            return EngineChain.invalid(facing, EngineChain.Invalidity.NO_COMPRESSOR);
        }
        if (fanPos == null) {
            return EngineChain.invalid(facing, EngineChain.Invalidity.NO_FAN);
        }

        // ---- downstream: optional afterburner then the nozzle -------------------------------
        BlockPos afterburnerPos = null;
        cursor = corePos.relative(facing);
        ModuleType next = moduleAt(level, cursor, facing);
        if (next == ModuleType.AFTERBURNER) {
            afterburnerPos = cursor;
            cursor = cursor.relative(facing);
            next = moduleAt(level, cursor, facing);
        }
        if (next != ModuleType.NOZZLE) {
            return EngineChain.invalid(facing, EngineChain.Invalidity.NO_NOZZLE);
        }
        BlockPos nozzlePos = cursor;

        // ---- obstruction checks -------------------------------------------------------------
        boolean intakeBlocked = isObstructing(level, fanPos.relative(upstream));
        boolean exhaustBlocked = isObstructing(level, nozzlePos.relative(facing));

        return new EngineChain(
                true, null, facing,
                fanPos, corePos, afterburnerPos, nozzlePos,
                compressors, afterburnerPos != null,
                intakeBlocked, exhaustBlocked);
    }

    /**
     * Returns the module type at {@code pos}, but only if it is aligned with {@code facing}.
     * A module rotated off-axis reads as "not a module", which correctly breaks the chain.
     */
    private static ModuleType moduleAt(LevelReader level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof JetModuleBlock module)) {
            return null;
        }
        if (state.getValue(JetModuleBlock.FACING) != facing) {
            return null;
        }
        return module.getModuleType();
    }

    /** A full collision cube in front of the intake or behind the nozzle chokes the engine. */
    private static boolean isObstructing(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        return state.isCollisionShapeFullBlock(level, pos);
    }
}
