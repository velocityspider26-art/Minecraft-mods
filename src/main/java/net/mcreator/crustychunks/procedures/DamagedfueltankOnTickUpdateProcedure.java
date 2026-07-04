package net.mcreator.crustychunks.procedures;

import net.minecraft.world.level.LevelAccessor;

public class DamagedfueltankOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      FuelTankDamageTickProcedure.execute(world, x, y, z);
      FuelTankModuleOnTickUpdateProcedure.execute(world, x, y, z);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("DamagedfueltankOnTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
