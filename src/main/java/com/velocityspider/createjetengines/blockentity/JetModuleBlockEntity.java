package com.velocityspider.createjetengines.blockentity;

import com.velocityspider.createjetengines.block.JetModuleBlock;
import com.velocityspider.createjetengines.engine.ModuleType;
import com.velocityspider.createjetengines.registry.JetBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Base block entity for the non-core modules.
 *
 * <p>Holds no authoritative state of its own. For rendering it locates the combustion core that
 * owns its chain and reads the core's synchronised engine state, which keeps one engine to one
 * synchronised state object instead of five.
 */
public class JetModuleBlockEntity extends BlockEntity {

    /** Longest possible chain: fan + 4 compressors + core + afterburner + nozzle. */
    public static final int MAX_CHAIN_REACH = 8;

    @Nullable
    private BlockPos cachedCorePos;
    private boolean coreLookupDone;

    /** Client-side accumulated rotation, in degrees, for spinning geometry. */
    private float spinAngle;
    private float lastSpinAngle;

    /** Rate-limits particle emission to once per game tick rather than once per frame. */
    private long lastEffectTick = -1L;

    /** Used by {@code BlockEntityType.Builder.of} for the four non-core modules. */
    public JetModuleBlockEntity(BlockPos pos, BlockState state) {
        this(JetBlockEntities.MODULE.get(), pos, state);
    }

    protected JetModuleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Called when any neighbouring module changes; forces the core to be looked up again. */
    public void invalidateStructure() {
        cachedCorePos = null;
        coreLookupDone = false;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof JetModuleBlock) {
            return state.getValue(JetModuleBlock.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Finds the combustion core of this module's chain by walking its own axis in both directions.
     * Bounded by {@link #MAX_CHAIN_REACH} and cached until a neighbour changes.
     */
    @Nullable
    public CombustionCoreBlockEntity getCore() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        if (coreLookupDone && cachedCorePos != null) {
            if (level.getBlockEntity(cachedCorePos) instanceof CombustionCoreBlockEntity core) {
                return core;
            }
            invalidateStructure();
        }
        if (coreLookupDone) {
            return null;
        }
        coreLookupDone = true;

        if (this instanceof CombustionCoreBlockEntity self) {
            cachedCorePos = getBlockPos();
            return self;
        }

        Direction facing = getFacing();
        for (Direction dir : new Direction[]{facing, facing.getOpposite()}) {
            BlockPos.MutableBlockPos cursor = getBlockPos().mutable();
            for (int i = 0; i < MAX_CHAIN_REACH; i++) {
                cursor.move(dir);
                BlockState state = level.getBlockState(cursor);
                if (!(state.getBlock() instanceof JetModuleBlock module)) {
                    break;
                }
                if (state.getValue(JetModuleBlock.FACING) != facing) {
                    break;
                }
                if (module.getModuleType() == ModuleType.COMBUSTION
                        && level.getBlockEntity(cursor) instanceof CombustionCoreBlockEntity core) {
                    cachedCorePos = cursor.immutable();
                    return core;
                }
            }
        }
        cachedCorePos = null;
        return null;
    }

    // ---- animation helpers, read by the renderers -------------------------------------------

    public float getSpool() {
        CombustionCoreBlockEntity core = getCore();
        return core == null ? 0.0F : core.getSpool();
    }

    public float getThrottle() {
        CombustionCoreBlockEntity core = getCore();
        return core == null ? 0.0F : core.getThrottle();
    }

    public boolean isAfterburnerActive() {
        CombustionCoreBlockEntity core = getCore();
        return core != null && core.isAfterburnerActive();
    }

    /**
     * Advances the spin accumulator. Called once per client tick by the renderer so the visual
     * rotation rate tracks the synchronised spool fraction rather than free-running.
     */
    public void advanceSpin(float degreesPerTickAtFullSpool) {
        lastSpinAngle = spinAngle;
        spinAngle = (spinAngle + getSpool() * degreesPerTickAtFullSpool) % 360.0F;
        if (spinAngle < lastSpinAngle) {
            lastSpinAngle -= 360.0F;
        }
    }

    /** True at most once per game tick, so renderers can emit effects at tick rate. */
    public boolean tryEffectTick() {
        Level level = getLevel();
        if (level == null) {
            return false;
        }
        long now = level.getGameTime();
        if (now == lastEffectTick) {
            return false;
        }
        lastEffectTick = now;
        return true;
    }

    public float getSpinAngle(float partialTick) {
        return lastSpinAngle + (spinAngle - lastSpinAngle) * partialTick;
    }
}
