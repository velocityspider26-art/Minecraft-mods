package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class EmberParticleProjectileHitsLivingEntityProcedure {
   public static void execute(LevelAccessor world, Entity entity, Entity immediatesourceentity) {
      try {
      if (entity != null && immediatesourceentity != null) {
         entity.igniteForSeconds(20);
         entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ON_FIRE)), 4.0F);
         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("EmberParticleProjectileHitsLivingEntityProcedure.execute", _wtSafe);
      }
   }
}
