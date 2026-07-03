package net.mcreator.crustychunks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class TintedGlassStairsBlock extends StairBlock {
   public TintedGlassStairsBlock() {
      super(Blocks.AIR.defaultBlockState(),
         Properties.of().instrument(NoteBlockInstrument.HAT).sound(SoundType.GLASS).strength(3.0F).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)
      );
   }

   public float getExplosionResistance() {
      return 3.0F;
   }

   public boolean isRandomlyTicking(BlockState state) {
      return false;
   }

   public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
      return adjacentBlockState.getBlock() == this ? true : super.skipRendering(state, adjacentBlockState, side);
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 1;
   }
}
