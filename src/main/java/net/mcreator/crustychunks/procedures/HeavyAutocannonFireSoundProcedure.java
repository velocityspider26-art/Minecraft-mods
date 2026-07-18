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
            CrustyChunksMod.sendToAll(new WariumSoundEvent(x, y, z, "crusty_chunks:explosionsmallfar", 80.0F, (float)Mth.nextDouble(rand, 0.95, 1.05)));
            CrustyChunksMod.sendToAll(new WariumSoundEvent(x, y, z, "crusty_chunks:cannonfar", 20.0F, (float)Mth.nextDouble(rand, 1.3, 1.4)));
            CrustyChunksMod.sendToAll(new WariumSoundEvent(x, y, z, "crusty_chunks:heavyautocannonshot", 10.0F, (float)Mth.nextDouble(rand, 0.95, 1.05)));
         }
      }
   }
}
