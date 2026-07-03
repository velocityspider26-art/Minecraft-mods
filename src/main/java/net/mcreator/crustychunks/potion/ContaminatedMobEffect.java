package net.mcreator.crustychunks.potion;

import net.minecraft.world.effect.MobEffectInstance;
import java.util.ArrayList;
import java.util.List;
import net.mcreator.crustychunks.procedures.ContaminatedEffectProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ContaminatedMobEffect extends MobEffect {
   public ContaminatedMobEffect() {
      super(MobEffectCategory.HARMFUL, -16449281);
   }
   @Override
   public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures, MobEffectInstance effectInstance) {
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      ContaminatedEffectProcedure.execute(entity);
      return true;
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }
}
