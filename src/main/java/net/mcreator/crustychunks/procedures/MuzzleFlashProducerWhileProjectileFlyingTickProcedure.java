package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class MuzzleFlashProducerWhileProjectileFlyingTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.setNoGravity(true);
         CrustyChunksMod.queueServerWork(2, () -> {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         });
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MuzzleFlashProducerWhileProjectileFlyingTickProcedure.execute", _wtSafe);
      }
   }
}
