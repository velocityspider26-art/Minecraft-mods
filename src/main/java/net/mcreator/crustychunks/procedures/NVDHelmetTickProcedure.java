package net.mcreator.crustychunks.procedures;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class NVDHelmetTickProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         if (entity.getPersistentData().getDouble("HelmetState") == 0.0) {
            entity.getPersistentData().putDouble("HelmetState", 1.0);
         }

         if (entity.getPersistentData().getDouble("HelmetState") == 1.0) {
            entity.getPersistentData().putDouble("HelmetState", 2.0);
         } else if (entity.getPersistentData().getDouble("HelmetState") == 2.0) {
            entity.getPersistentData().putDouble("HelmetState", 1.0);
         }

         if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4, 3, false, false));
         }
      }
   }
}
