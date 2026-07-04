package net.mcreator.crustychunks.procedures;

import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class BurntgrassUpdateTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (1 == Mth.nextInt(RandomSource.create(), 1, 5)) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = Blocks.DIRT.defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var10 = _bso.getProperties().iterator();

         while (var10.hasNext()) {

            Property<?> entry_prop = var10.next();

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
         net.mcreator.crustychunks.compat.WariumSafety.report("BurntgrassUpdateTickProcedure.execute", _wtSafe);
      }
   }
}
