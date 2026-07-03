package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.procedures.ConcreteDamage0Procedure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

public class StructuralConcreteBlock extends Block {
   public StructuralConcreteBlock() {
      super(
         Properties.of()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .mapColor(MapColor.TERRACOTTA_CYAN)
            .sound(SoundType.BASALT)
            .strength(50.0F, 15.0F)
            .requiresCorrectToolForDrops()
      );
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 15;
   }

   public boolean onDestroyedByPlayer(BlockState blockstate, Level world, BlockPos pos, Player entity, boolean willHarvest, FluidState fluid) {
      boolean retval = super.onDestroyedByPlayer(blockstate, world, pos, entity, willHarvest, fluid);
      ConcreteDamage0Procedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      return retval;
   }

   public void wasExploded(Level world, BlockPos pos, Explosion e) {
      super.wasExploded(world, pos, e);
      ConcreteDamage0Procedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }
}
