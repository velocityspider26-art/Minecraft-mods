package net.mcreator.crustychunks.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class RustyStairsBlock extends StairBlock {
   public RustyStairsBlock() {
      super(Blocks.AIR.defaultBlockState(),
         Properties.of().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.WOOD).sound(SoundType.NETHERITE_BLOCK).strength(13.0F, 5.0F)
      );
   }

   public float getExplosionResistance() {
      return 5.0F;
   }

   public boolean isRandomlyTicking(BlockState state) {
      return false;
   }
}
