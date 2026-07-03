package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ArmorDegradeProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      double valuebefore = 0.0;
      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:variablearmor")))) {
         valuebefore = world.getBlockState(BlockPos.containing(x, y, z)).getBlock().getStateDefinition().getProperty("damage") instanceof IntegerProperty _getip3
            ? (double)((Integer)world.getBlockState(BlockPos.containing(x, y, z)).getValue(_getip3)).intValue()
            : -1.0;
         int _value = (
               world.getBlockState(BlockPos.containing(x, y, z)).getBlock().getStateDefinition().getProperty("damage") instanceof IntegerProperty _getip5
                  ? (Integer)world.getBlockState(BlockPos.containing(x, y, z)).getValue(_getip5)
                  : -1
            )
            + 1;
         BlockPos _pos = BlockPos.containing(x, y, z);
         BlockState _bs = world.getBlockState(_pos);
         if (_bs.getBlock().getStateDefinition().getProperty("damage") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value)) {
            world.setBlock(_pos, (BlockState)_bs.setValue(_integerProp, _value), 3);
         }

         if (valuebefore
            == (double)(
               world.getBlockState(BlockPos.containing(x, y, z)).getBlock().getStateDefinition().getProperty("damage") instanceof IntegerProperty _getip8
                  ? (Integer)world.getBlockState(BlockPos.containing(x, y, z)).getValue(_getip8)
                  : -1
            )) {
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
         }
      }
   }
}
