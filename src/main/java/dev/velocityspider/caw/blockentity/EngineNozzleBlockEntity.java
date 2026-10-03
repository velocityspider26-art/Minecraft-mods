package dev.velocityspider.caw.blockentity;

import dev.velocityspider.caw.block.EngineComponentBlock;
import dev.velocityspider.caw.client.ExhaustPlume;
import dev.velocityspider.caw.compat.SablePhysicsBridge;
import dev.velocityspider.caw.engine.EngineChain;
import dev.velocityspider.caw.engine.EngineMath;
import dev.velocityspider.caw.engine.EngineTier;
import dev.velocityspider.caw.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class EngineNozzleBlockEntity extends BlockEntity {
    private static final double TICK_SECONDS = 1.0 / 20.0;

    private double spool;
    private boolean chainValid;
    private int lastSignal;
    private int tickCounter;

    public EngineNozzleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENGINE_NOZZLE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            EngineNozzleBlockEntity nozzle
    ) {
        if (level.isClientSide) {
            return;
        }

        // FACING is the direction the exhaust leaves the nozzle.
        Direction exhaustDirection = state.getValue(DirectionalBlock.FACING);
        Optional<EngineChain> chainResult = EngineChain.scan(level, pos, exhaustDirection);
        nozzle.chainValid = chainResult.isPresent();

        int signal = 0;
        double target = 0.0;
        double inletMultiplier = 1.0;

        if (chainResult.isPresent()) {
            EngineChain chain = chainResult.get();
            signal = chain.strongestRedstoneSignal(level);
            target = EngineMath.redstoneTarget(signal);
            inletMultiplier = chain.inletMultiplier();
        }

        nozzle.lastSignal = signal;
        nozzle.spool = EngineMath.stepSpool(nozzle.spool, target);

        if (!nozzle.chainValid && nozzle.spool < 0.0001) {
            nozzle.spool = 0.0;
        }

        if (nozzle.chainValid && nozzle.spool > 0.0001) {
            EngineTier tier = EngineTier.NORMAL;
            if (state.getBlock() instanceof EngineComponentBlock component) {
                tier = component.tier();
            }

            double thrustN = EngineMath.thrustNewtons(nozzle.spool, inletMultiplier, tier);
            double impulseNs = EngineMath.impulseNewtonSeconds(thrustN, TICK_SECONDS);

            // Newton's third law: the vehicle is pushed opposite the exhaust stream.
            Direction craftThrustDirection = exhaustDirection.getOpposite();
            SablePhysicsBridge.applyThrustImpulse(level, pos, craftThrustDirection, impulseNs);
        }

        nozzle.tickCounter++;

        if (level instanceof ServerLevel serverLevel
                && nozzle.chainValid
                && nozzle.spool > 0.10
                && (nozzle.tickCounter & 1) == 0) {
            ExhaustPlume.emit(serverLevel, pos, exhaustDirection, nozzle.spool);
        }
    }

    public double spool() {
        return spool;
    }

    public boolean chainValid() {
        return chainValid;
    }

    public int lastSignal() {
        return lastSignal;
    }
}
