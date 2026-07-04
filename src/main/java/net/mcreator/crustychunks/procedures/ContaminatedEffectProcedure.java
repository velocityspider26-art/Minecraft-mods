package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class ContaminatedEffectProcedure {
   public static void execute(Entity entity) {
      try {
      if (entity != null) {
         if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.RADIATION, 20, 1, false, false));
         }

         int var10000;
         label23: {
            if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(CrustyChunksModMobEffects.CONTAMINATED)) {
               var10000 = _livEnt.getEffect(CrustyChunksModMobEffects.CONTAMINATED).getAmplifier();
               break label23;
            }

            var10000 = 0;
         }

         if (var10000 >= Mth.nextInt(RandomSource.create(), 0, 12)) {
            entity.getPersistentData().putDouble("Radiation", entity.getPersistentData().getDouble("Radiation") + 1.0);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ContaminatedEffectProcedure.execute", _wtSafe);
      }
   }
}
