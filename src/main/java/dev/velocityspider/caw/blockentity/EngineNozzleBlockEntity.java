package dev.velocityspider.caw.blockentity;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.velocityspider.caw.block.EngineComponentBlock;
import dev.velocityspider.caw.engine.EngineChain;
import dev.velocityspider.caw.engine.EngineMath;
import dev.velocityspider.caw.engine.EngineTier;
import dev.velocityspider.caw.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.Optional;

public final class EngineNozzleBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private double spool;
    private boolean chainValid;
    private int lastSignal;
    private double flowMultiplier = 1.0;
    private double cachedThrustNewtons;
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

        Direction exhaustDirection = state.getValue(DirectionalBlock.FACING);
        Optional<EngineChain> chainResult = EngineChain.scan(level, pos, exhaustDirection);

        nozzle.chainValid = chainResult.isPresent();

        int signal = 0;
        double target = 0.0;
        double newFlowMultiplier = 1.0;

        if (chainResult.isPresent()) {
            EngineChain chain = chainResult.get();
            signal = chain.strongestRedstoneSignal(level);
            target = EngineMath.redstoneTarget(signal);
            newFlowMultiplier = chain.intakeMultiplier(level) * chain.exhaustMultiplier(level);
        }

        nozzle.lastSignal = signal;
        nozzle.flowMultiplier = newFlowMultiplier;
        nozzle.spool = EngineMath.stepSpool(nozzle.spool, target);

        if (!nozzle.chainValid && nozzle.spool < 0.0001) {
            nozzle.spool = 0.0;
        }

        EngineTier tier = EngineTier.NORMAL;
        if (state.getBlock() instanceof EngineComponentBlock component) {
            tier = component.tier();
        }

        nozzle.cachedThrustNewtons = nozzle.chainValid
                ? EngineMath.thrustNewtons(nozzle.spool, nozzle.flowMultiplier, tier)
                : 0.0;

        nozzle.tickCounter++;

        if ((nozzle.tickCounter & 1) == 0) {
            nozzle.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (!chainValid || cachedThrustNewtons <= 0.0 || spool <= 0.0001) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(DirectionalBlock.FACING)) {
            return;
        }

        Direction exhaustDirection = state.getValue(DirectionalBlock.FACING);
        Direction craftThrustDirection = exhaustDirection.getOpposite();

        Vector3d point = new Vector3d(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        );

        Vector3d impulse = new Vector3d(
                craftThrustDirection.getStepX(),
                craftThrustDirection.getStepY(),
                craftThrustDirection.getStepZ()
        ).mul(cachedThrustNewtons * timeStep);

        QueuedForceGroup propulsion =
                subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());

        propulsion.applyAndRecordPointForce(point, impulse);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("CAWSpool", spool);
        tag.putBoolean("CAWChainValid", chainValid);
        tag.putInt("CAWSignal", lastSignal);
        tag.putDouble("CAWFlow", flowMultiplier);
        tag.putDouble("CAWThrustN", cachedThrustNewtons);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spool = tag.getDouble("CAWSpool");
        chainValid = tag.getBoolean("CAWChainValid");
        lastSignal = tag.getInt("CAWSignal");
        flowMultiplier = tag.contains("CAWFlow") ? tag.getDouble("CAWFlow") : 1.0;
        cachedThrustNewtons = tag.getDouble("CAWThrustN");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("CAWSpool", spool);
        tag.putBoolean("CAWChainValid", chainValid);
        tag.putInt("CAWSignal", lastSignal);
        tag.putDouble("CAWFlow", flowMultiplier);
        tag.putDouble("CAWThrustN", cachedThrustNewtons);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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

    public double flowMultiplier() {
        return flowMultiplier;
    }

    public double thrustNewtons() {
        return cachedThrustNewtons;
    }
}
