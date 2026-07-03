package net.mcreator.crustychunks.potion;

import net.minecraft.world.effect.MobEffectInstance;
import java.util.ArrayList;
import java.util.List;
import net.mcreator.crustychunks.procedures.AISiegeTriggerProcedure;
import net.mcreator.crustychunks.procedures.ImpendingDoomActiveTickConditionProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ImpendingDoomMobEffect extends MobEffect {
   public ImpendingDoomMobEffect() {
      super(MobEffectCategory.HARMFUL, -13434880);
   }
   @Override
   public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures, MobEffectInstance effectInstance) {
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      ImpendingDoomActiveTickConditionProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
      return true;
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }
}
