package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.procedures.AshUpdateProcedure;
import net.mcreator.crustychunks.procedures.CharredBlockOnRandomClientDisplayTickProcedure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class CharredBlockBlock extends Block {
   public CharredBlockBlock() {
      super(Properties.of().ignitedByLava().instrument(NoteBlockInstrument.BASS).sound(SoundType.SOUL_SAND).strength(0.5F, 5.0F).randomTicks());
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 15;
   }

   public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
      super.tick(blockstate, world, pos, random);
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      AshUpdateProcedure.execute(world, (double)x, (double)y, (double)z);
   }

   @OnlyIn(Dist.CLIENT)
   public void animateTick(BlockState blockstate, Level world, BlockPos pos, RandomSource random) {
      super.animateTick(blockstate, world, pos, random);
      Player entity = Minecraft.getInstance().player;
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      CharredBlockOnRandomClientDisplayTickProcedure.execute(world, (double)x, (double)y, (double)z);
   }
}
