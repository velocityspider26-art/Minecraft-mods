package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.procedures.PlutoniumTouchProcedure;
import net.mcreator.crustychunks.procedures.Rad10TickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class PlutoniumBlockBlock extends Block {
   public PlutoniumBlockBlock() {
      super(Properties.of().sound(SoundType.NETHERITE_BLOCK).strength(10.0F, 15.0F).lightLevel(s -> 8).requiresCorrectToolForDrops().randomTicks());
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 15;
   }

   public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
      super.tick(blockstate, world, pos, random);
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      Rad10TickProcedure.execute(world, (double)x, (double)y, (double)z);
   }

   public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
      super.entityInside(blockstate, world, pos, entity);
      PlutoniumTouchProcedure.execute(world, entity);
   }

   public void stepOn(Level world, BlockPos pos, BlockState blockstate, Entity entity) {
      super.stepOn(world, pos, blockstate, entity);
      PlutoniumTouchProcedure.execute(world, entity);
   }
}
