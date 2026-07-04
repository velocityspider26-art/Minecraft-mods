package net.mcreator.crustychunks.procedures;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class SingleShotHandTickProcedure {
   public static void execute(Entity entity) {
      try {
      if (entity != null) {
         if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
         }

         entity.setDeltaMovement(new Vec3(0.0, entity.getDeltaMovement().y(), 0.0));
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SingleShotHandTickProcedure.execute", _wtSafe);
      }
   }
}
