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
import shipwrights.genesis.space.voxel.PlanetVoxelService;

/**
 * Server-authoritative dirty tracking for the planet voxel pyramid.
 *
 * <p>Hooking {@code LevelChunk.setBlockState} rather than a placement event is
 * deliberate: every authoritative world change funnels through here, so
 * explosions, pistons, fluids, commands, worldgen writes, Create machinery,
 * orbital bombardment and arbitrary modded {@code setBlock} calls all mark the
 * orbital terrain dirty, not just player placement and breaking.</p>
 */
@Mixin(value = LevelChunk.class, priority = 1490)
public abstract class LevelChunkVoxelDirtyMixin {
    @Shadow @Final private Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"), require = 0)
    private void genesis$markPlanetVoxelDirty(BlockPos pos, BlockState state, boolean isMoving,
                                              CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null && this.level instanceof ServerLevel serverLevel) {
            PlanetVoxelService.markChunkDirty(serverLevel, pos.getX() >> 4, pos.getZ() >> 4);
        }
    }
}
