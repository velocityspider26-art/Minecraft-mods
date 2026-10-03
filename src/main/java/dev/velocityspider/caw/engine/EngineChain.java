package dev.velocityspider.caw.engine;

import dev.velocityspider.caw.block.EngineComponentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record EngineChain(
        Direction exhaustDirection,
        BlockPos fan,
        BlockPos compressor,
        BlockPos combustor,
        BlockPos turbine,
        BlockPos nozzle,
        Optional<BlockPos> inlet
) {
    public static Optional<EngineChain> scan(Level level, BlockPos nozzlePos, Direction exhaustDirection) {
        Direction upstream = exhaustDirection.getOpposite();

        BlockPos turbine = nozzlePos.relative(upstream, 1);
        BlockPos combustor = nozzlePos.relative(upstream, 2);
        BlockPos compressor = nozzlePos.relative(upstream, 3);
        BlockPos fan = nozzlePos.relative(upstream, 4);
        BlockPos inletCandidate = nozzlePos.relative(upstream, 5);

        if (!EngineComponentBlock.matches(level, turbine, EngineComponentType.TURBINE, exhaustDirection)
                || !EngineComponentBlock.matches(level, combustor, EngineComponentType.COMBUSTOR, exhaustDirection)
                || !EngineComponentBlock.matches(level, compressor, EngineComponentType.COMPRESSOR, exhaustDirection)
                || !EngineComponentBlock.matches(level, fan, EngineComponentType.FAN, exhaustDirection)) {
            return Optional.empty();
        }

        Optional<BlockPos> inlet = EngineComponentBlock.matches(
                level, inletCandidate, EngineComponentType.INLET, exhaustDirection
        ) ? Optional.of(inletCandidate) : Optional.empty();

        return Optional.of(new EngineChain(
                exhaustDirection, fan, compressor, combustor, turbine, nozzlePos, inlet
        ));
    }

    public List<BlockPos> allPositions() {
        List<BlockPos> positions = new ArrayList<>(6);
        inlet.ifPresent(positions::add);
        positions.add(fan);
        positions.add(compressor);
        positions.add(combustor);
        positions.add(turbine);
        positions.add(nozzle);
        return Collections.unmodifiableList(positions);
    }

    public int strongestRedstoneSignal(Level level) {
        int strongest = 0;
        for (BlockPos pos : allPositions()) {
            strongest = Math.max(strongest, level.getBestNeighborSignal(pos));
            if (strongest >= 15) {
                return 15;
            }
        }
        return strongest;
    }

    /**
     * Models the first useful inlet behavior without requiring a full CFD solver.
     * A fitted inlet improves ram-air recovery when the intake mouth is clear.
     * Blocking the intake heavily starves the engine whether or not an inlet is fitted.
     */
    public double intakeMultiplier(Level level) {
        BlockPos frontMost = inlet.orElse(fan);
        BlockPos intakeMouth = frontMost.relative(exhaustDirection.getOpposite());
        boolean clear = level.getBlockState(intakeMouth).isAir();

        if (!clear) {
            return inlet.isPresent() ? 0.55 : 0.45;
        }

        return inlet.isPresent() ? 1.15 : 1.0;
    }

    /**
     * A nozzle buried directly into a solid block is severely back-pressured.
     * We do not explode/damage the engine in v0.1, but it loses most useful thrust.
     */
    public double exhaustMultiplier(Level level) {
        BlockPos exhaustMouth = nozzle.relative(exhaustDirection);
        return level.getBlockState(exhaustMouth).isAir() ? 1.0 : 0.15;
    }
}
