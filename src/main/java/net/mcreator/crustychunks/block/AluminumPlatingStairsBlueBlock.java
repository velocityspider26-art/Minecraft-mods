package net.mcreator.crustychunks.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class AluminumPlatingStairsBlueBlock extends StairBlock {
   public AluminumPlatingStairsBlueBlock() {
      super(Blocks.AIR.defaultBlockState(), Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.NETHERITE_BLOCK).strength(4.0F));
   }

   public float getExplosionResistance() {
      return 4.0F;
   }

   public boolean isRandomlyTicking(BlockState state) {
      return false;
   }
}
