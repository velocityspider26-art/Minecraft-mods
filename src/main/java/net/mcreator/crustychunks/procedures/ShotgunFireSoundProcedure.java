package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.WariumSoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class ShotgunFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (!world.isClientSide()) {
         if (world instanceof Level level) {
            RandomSource rand = level.getRandom();
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 80.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:distantshotmedium", 80.0F, (float)Mth.nextDouble(rand, 0.8, 0.9)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 20.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:midrangeshot", 20.0F, (float)Mth.nextDouble(rand, 0.7, 0.8)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 10.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:largeshot", 10.0F, (float)Mth.nextDouble(rand, 0.9, 1.1)));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ShotgunFireSoundProcedure.execute", _wtSafe);
      }
   }
}
