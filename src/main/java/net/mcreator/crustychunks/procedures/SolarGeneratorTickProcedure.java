package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SolarGeneratorTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double Kinetic = 0.0;
      if (world instanceof Level _lvl0 && _lvl0.isDay() && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y, z))) {
         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y - 1.0, z));
         int _amount = 10;
         if (_ent != null) {
            {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) capability.receiveEnergy(_amount, false);
}
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SolarGeneratorTickProcedure.execute", _wtSafe);
      }
   }
}
