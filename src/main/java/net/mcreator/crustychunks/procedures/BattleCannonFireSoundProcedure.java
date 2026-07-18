package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.WariumSoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class BattleCannonFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (!world.isClientSide()) {
         if (world instanceof Level level) {
            RandomSource rand = level.getRandom();
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 120.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:farblast", 120.0F, (float)Mth.nextDouble(rand, 1.2, 1.3)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 60.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:cannonfar", 60.0F, (float)Mth.nextDouble(rand, 1.2, 1.25)));
            CrustyChunksMod.sendToNear(world, x, y, z, Math.max(192.0, 15.0 * 20.0), new WariumSoundEvent(x, y, z, "crusty_chunks:battlecannon", 15.0F, (float)Mth.nextDouble(rand, 0.9, 1.1)));
         }
      }
   }
}
