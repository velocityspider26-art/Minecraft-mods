package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class FlammableOnEffectActiveTickProcedure {
   public static void execute(Entity entity) {
      try {
      if (entity != null) {
         if (entity.getRemainingFireTicks() > 0) {
            int var10001;
            label17: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(CrustyChunksModMobEffects.FLAMMABLE)) {
                  var10001 = _livEnt.getEffect(CrustyChunksModMobEffects.FLAMMABLE).getAmplifier();
                  break label17;
               }

               var10001 = 0;
            }

            entity.igniteForSeconds(var10001);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FlammableOnEffectActiveTickProcedure.execute", _wtSafe);
      }
   }
}
