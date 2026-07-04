package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class PlayerRadiationDoseProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      try {
      if (entity != null) {
         if (Mth.nextInt(RandomSource.create(), 1, 100) == 1
            && (double)Mth.nextInt(RandomSource.create(), 10, 1000) <= entity.getPersistentData().getDouble("Radiation")
            && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:radiation_proof")))
            && 50.0 <= entity.getPersistentData().getDouble("Radiation")) {
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0, false, false));
            }

            if (250.0 <= entity.getPersistentData().getDouble("Radiation")) {
               if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                  _entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0, false, false));
               }

               entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 2.0F);
            }
         }

         if (4000.0 <= entity.getPersistentData().getDouble("Radiation")
            && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:radiation_proof")))) {
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 1, false, false));
            }

            entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 2.0F);
         }

         if (entity.getPersistentData().getDouble("Radiation") >= 1.0) {
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.RADIATION, 20, 1, false, false));
            }

            if (Mth.nextInt(RandomSource.create(), 1, 10) == 1) {
               entity.getPersistentData().putDouble("Radiation", entity.getPersistentData().getDouble("Radiation") - 1.0);
            }
         } else {
            entity.getPersistentData().putDouble("Radiation", 0.0);
            if (entity instanceof LivingEntity _entity) {
               _entity.removeEffect(CrustyChunksModMobEffects.RADIATION);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("PlayerRadiationDoseProcedure.execute", _wtSafe);
      }
   }
}
