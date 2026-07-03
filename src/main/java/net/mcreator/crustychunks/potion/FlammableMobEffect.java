package net.mcreator.crustychunks.potion;

import net.mcreator.crustychunks.procedures.FlammableOnEffectActiveTickProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class FlammableMobEffect extends MobEffect {
   public FlammableMobEffect() {
      super(MobEffectCategory.HARMFUL, -18804);
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      FlammableOnEffectActiveTickProcedure.execute(entity);
      return true;
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }
}
