package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.WariumSoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class StealthPistolFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (!world.isClientSide()) {
         if (world instanceof Level level) {
            RandomSource rand = level.getRandom();
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 8.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:distantgunfire", 8.0F, (float)Mth.nextDouble(rand, 0.9, 1.05)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 5.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:silencedshot", 5.0F, (float)Mth.nextDouble(rand, 0.95, 1.05)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 1.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:gunmechanism", 1.0F, (float)Mth.nextDouble(rand, 0.95, 1.05)));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("StealthPistolFireSoundProcedure.execute", _wtSafe);
      }
   }
}
