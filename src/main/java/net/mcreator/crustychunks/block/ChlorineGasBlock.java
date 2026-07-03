package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.procedures.ToxicDisperseProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ChlorineGasBlock extends LiquidBlock {
   public ChlorineGasBlock() {
      super(CrustyChunksModFluids.CHLORINE_GAS.get(),
         Properties.of()
            .mapColor(MapColor.WATER)
            .strength(100.0F)
            .noCollission()
            .noLootTable()
            .liquid()
            .pushReaction(PushReaction.DESTROY)
            .sound(SoundType.EMPTY)
            .replaceable()
      );
   }

   public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
      super.neighborChanged(blockstate, world, pos, neighborBlock, fromPos, moving);
      ToxicDisperseProcedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }

   public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
      super.tick(blockstate, world, pos, random);
      ToxicDisperseProcedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }
}
