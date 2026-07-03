package net.mcreator.crustychunks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public class LightWoodStairsBlock extends StairBlock {
   public LightWoodStairsBlock() {
      super(Blocks.AIR.defaultBlockState(), Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.0F));
   }

   public float getExplosionResistance() {
      return 1.0F;
   }

   public boolean isRandomlyTicking(BlockState state) {
      return false;
   }

   public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
      return 30;
   }

   public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
      return 40;
   }
}
