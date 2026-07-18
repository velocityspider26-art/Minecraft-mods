package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class FragmentDespawnMechanicProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (!immediatesourceentity.getPersistentData().getBoolean("despawntimer")) {
            immediatesourceentity.getPersistentData().putBoolean("despawntimer", true);
            CrustyChunksMod.queueServerWork(Mth.nextInt(RandomSource.create(), 20, 30), () -> {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FragmentDespawnMechanicProcedure.execute", _wtSafe);
      }
   }
}
