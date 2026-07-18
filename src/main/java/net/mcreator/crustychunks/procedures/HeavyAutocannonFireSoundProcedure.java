package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.WariumSoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class HeavyAutocannonFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (!world.isClientSide()) {
         if (world instanceof Level level) {
            RandomSource rand = level.getRandom();
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 80.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:explosionsmallfar", 80.0F, (float)Mth.nextDouble(rand, 0.95, 1.05)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 20.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:cannonfar", 20.0F, (float)Mth.nextDouble(rand, 1.3, 1.4)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 10.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:heavyautocannonshot", 10.0F, (float)Mth.nextDouble(rand, 0.95, 1.05)));
         }
      }
   }
}
