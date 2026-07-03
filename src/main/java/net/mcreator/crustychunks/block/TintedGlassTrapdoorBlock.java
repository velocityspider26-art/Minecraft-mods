package net.mcreator.crustychunks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class TintedGlassTrapdoorBlock extends TrapDoorBlock {
   public TintedGlassTrapdoorBlock() {
      super(BlockSetType.STONE, Properties.of().instrument(NoteBlockInstrument.HAT).sound(SoundType.GLASS).strength(2.0F).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 0;
   }
}
