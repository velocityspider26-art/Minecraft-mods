package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.WariumSoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class MediumFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (!world.isClientSide()) {
         if (world instanceof Level level) {
            RandomSource rand = level.getRandom();
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 80.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:distantgunfire", 80.0F, (float)Mth.nextDouble(rand, 0.9, 1.1)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 20.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:midrangeshot", 20.0F, (float)Mth.nextDouble(rand, 0.9, 1.1)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 10.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:mediumshot", 10.0F, (float)Mth.nextDouble(rand, 0.9, 1.02)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 10.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:mediumshot", 10.0F, (float)Mth.nextDouble(rand, 0.9, 1.02)));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MediumFireSoundProcedure.execute", _wtSafe);
      }
   }
}
