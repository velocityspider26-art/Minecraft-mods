package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Rad01ItemProcedure {
   public static void execute(Entity entity, ItemStack itemstack) {
      if (entity != null) {
         if (Mth.nextInt(RandomSource.create(), 1, 40) == 1) {
            entity.getPersistentData().putDouble("Radiation", entity.getPersistentData().getDouble("Radiation") + 0.1 * (double)itemstack.getCount());
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.RADIATION, 60, 1, false, false));
            }
         }
      }
   }
}
