package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.WariumSoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class LightAutocannonFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (!world.isClientSide()) {
         if (world instanceof Level level) {
            RandomSource rand = level.getRandom();
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 80.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:tinyexplosionfar", 80.0F, (float)Mth.nextDouble(rand, 0.95, 1.1)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 20.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:autocannonshot", 20.0F, (float)Mth.nextDouble(rand, 0.9, 1.1)));
         }
      }
   }
}
