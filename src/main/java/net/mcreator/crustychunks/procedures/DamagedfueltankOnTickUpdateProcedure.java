package net.mcreator.crustychunks.procedures;

import net.minecraft.world.level.LevelAccessor;

public class DamagedfueltankOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      FuelTankDamageTickProcedure.execute(world, x, y, z);
      FuelTankModuleOnTickUpdateProcedure.execute(world, x, y, z);
   }
}
