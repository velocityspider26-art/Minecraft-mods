package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.procedures.Contamination1Procedure;
import net.mcreator.crustychunks.procedures.Rad5TickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class RadioactiveAshFullBlockBlock extends FallingBlock {
   public RadioactiveAshFullBlockBlock() {
      super(Properties.of().instrument(NoteBlockInstrument.SNARE).sound(SoundType.SOUL_SAND).strength(0.5F, 15.0F));
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
      return true;
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 0;
   }

   public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(blockstate, world, pos, oldState, moving);
      world.scheduleTick(pos, this, 60);
   }

   public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
      super.tick(blockstate, world, pos, random);
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      Rad5TickProcedure.execute(world, (double)x, (double)y, (double)z);
      world.scheduleTick(pos, this, 60);
   }

   public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
      super.entityInside(blockstate, world, pos, entity);
      Contamination1Procedure.execute(entity);
   }

   public void stepOn(Level world, BlockPos pos, BlockState blockstate, Entity entity) {
      super.stepOn(world, pos, blockstate, entity);
      Contamination1Procedure.execute(entity);
   }

   @Override
   protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() {
      return simpleCodec(properties -> new RadioactiveAshFullBlockBlock());
   }
}
