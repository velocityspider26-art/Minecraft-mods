package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class ExplosiveBarrelTriggerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      world.destroyBlock(BlockPos.containing(x, y, z), false);
      SmallExplosionProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ExplosiveBarrelTriggerProcedure.execute", _wtSafe);
      }
   }
}
