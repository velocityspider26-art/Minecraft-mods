package net.mcreator.crustychunks.procedures;

import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class CleaningProcedureProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.MOSSY_COBBLESTONE) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = Blocks.COBBLESTONE.defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var10 = _bso.getProperties().iterator();

         while (var10.hasNext()) {

            Property<?> entry_prop = var10.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var16) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.MOSSY_STONE_BRICKS) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = Blocks.STONE_BRICKS.defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var23 = _bso.getProperties().iterator();

         while (var23.hasNext()) {

            Property<?> entry_prop = var23.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var15) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.OVERGROWN_REENFORCED_CONCRETE.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.REENFORCED_CONCRETE.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var24 = _bso.getProperties().iterator();

         while (var24.hasNext()) {

            Property<?> entry_prop = var24.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var14) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("CleaningProcedureProcedure.execute", _wtSafe);
      }
   }
}
