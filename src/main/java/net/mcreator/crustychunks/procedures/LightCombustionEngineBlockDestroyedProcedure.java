package net.mcreator.crustychunks.procedures;

import net.minecraft.world.level.LevelAccessor;

public class LightCombustionEngineBlockDestroyedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      GasolineExplosionProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LightCombustionEngineBlockDestroyedProcedure.execute", _wtSafe);
      }
   }
}
