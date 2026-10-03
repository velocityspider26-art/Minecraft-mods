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
        Direction direction,
        BlockPos fan,
        BlockPos compressor,
        BlockPos combustor,
        BlockPos turbine,
        BlockPos nozzle,
        Optional<BlockPos> inlet
) {
    public static Optional<EngineChain> scan(Level level, BlockPos nozzlePos, Direction airflowDirection) {
        Direction backwards = airflowDirection.getOpposite();

        BlockPos turbine = nozzlePos.relative(backwards, 1);
        BlockPos combustor = nozzlePos.relative(backwards, 2);
        BlockPos compressor = nozzlePos.relative(backwards, 3);
        BlockPos fan = nozzlePos.relative(backwards, 4);
        BlockPos inletCandidate = nozzlePos.relative(backwards, 5);

        if (!EngineComponentBlock.matches(level, turbine, EngineComponentType.TURBINE, airflowDirection)
                || !EngineComponentBlock.matches(level, combustor, EngineComponentType.COMBUSTOR, airflowDirection)
                || !EngineComponentBlock.matches(level, compressor, EngineComponentType.COMPRESSOR, airflowDirection)
                || !EngineComponentBlock.matches(level, fan, EngineComponentType.FAN, airflowDirection)) {
            return Optional.empty();
        }

        Optional<BlockPos> inlet = EngineComponentBlock.matches(
                level, inletCandidate, EngineComponentType.INLET, airflowDirection
        ) ? Optional.of(inletCandidate) : Optional.empty();

        return Optional.of(new EngineChain(
                airflowDirection, fan, compressor, combustor, turbine, nozzlePos, inlet
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

    public double inletMultiplier() {
        return inlet.isPresent() ? 1.15 : 1.0;
    }
}
