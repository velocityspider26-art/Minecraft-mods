package shipwrights.genesis.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.space.surface.SparsePlanetLodService;
import shipwrights.genesis.space.voxel.PlanetVoxelService;

/** Marks only the real loaded chunk that actually changed; no polling scan. */
@Mixin(value = LevelChunk.class, priority = 1490)
public abstract class LevelChunkSparseLodMixin {
    @Shadow @Final private Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"), require = 0)
    private void genesis$markSparsePlanetLodDirty(BlockPos pos, BlockState state, boolean isMoving,
                                                   CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null && this.level instanceof ServerLevel serverLevel) {
            SparsePlanetLodService.markChunkDirty(serverLevel, pos.getX() >> 4, pos.getZ() >> 4);
            PlanetVoxelService.markChunkDirty(serverLevel, pos.getX() >> 4, pos.getZ() >> 4);
        }
    }
}
