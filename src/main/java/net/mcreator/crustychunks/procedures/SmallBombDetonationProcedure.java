package net.mcreator.crustychunks.procedures;

import net.minecraft.world.level.LevelAccessor;

public class SmallBombDetonationProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      ExplosionExampleProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5, 4.0);
   }
}
