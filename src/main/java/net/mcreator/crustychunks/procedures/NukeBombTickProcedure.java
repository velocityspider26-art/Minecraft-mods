package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class NukeBombTickProcedure {
   public static void execute(LevelAccessor world, Entity entity, Entity immediatesourceentity) {
      try {
      if (entity != null && immediatesourceentity != null) {
         boolean Trigger = false;
         if (entity.getVehicle() != null) {
            Trigger = true;
         }

         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if (immediatesourceentity.isUnderWater()) {
            NuclearBombHitsBlockProcedure.execute(world, immediatesourceentity);
         }

         if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) >= 2.0
            && immediatesourceentity.getPersistentData().getDouble("T") > 25.0
            && !immediatesourceentity.isNoGravity()) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y() + 0.045, immediatesourceentity.getDeltaMovement().z()
               )
            );
         }

         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }

            CrustyChunksMod.queueServerWork(1, () -> LargeRocketHitProcedure.execute(world, immediatesourceentity));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("NukeBombTickProcedure.execute", _wtSafe);
      }
   }
}
