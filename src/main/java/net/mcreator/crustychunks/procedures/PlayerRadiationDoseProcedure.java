package net.mcreator.crustychunks.procedures;

import net.minecraft.advancements.AdvancementHolder;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
         double radiation = 0.0;
         radiation = entity.getPersistentData().getDouble("Radiation");
         if (Mth.nextInt(RandomSource.create(), 1, 100) == 1
            && (double)Mth.nextInt(RandomSource.create(), 10, 1000) <= radiation
            && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:radiation_proof")))
            && 50.0 <= radiation) {
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0, false, false));
            }

            if (250.0 <= radiation) {
               if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                  _entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0, false, false));
               }

               entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 2.0F);
            }
         }

         if (4000.0 <= radiation && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:radiation_proof")))) {
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 1, false, false));
            }

            entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 2.0F);
         }

         if (radiation >= 1.0) {
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.RADIATION, 20, 1, false, false));
            }

            if (Mth.nextInt(RandomSource.create(), 1, 10) == 1) {
               entity.getPersistentData().putDouble("Radiation", radiation - 1.0);
            }
         } else {
            entity.getPersistentData().putDouble("Radiation", 0.0);
            if (entity instanceof LivingEntity _entity) {
               _entity.removeEffect(CrustyChunksModMobEffects.RADIATION);
            }
         }

         if ((
               !(entity instanceof ServerPlayer _plr17)
                  || !(_plr17.level() instanceof ServerLevel)
                  || !_plr17.getAdvancements().getOrStartProgress(_plr17.server.getAdvancements().get(ResourceLocation.parse("crusty_chunks:spicy_light"))).isDone()
            )
            && radiation >= 100.0
            && entity instanceof ServerPlayer _player) {
            AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("crusty_chunks:spicy_light"));
            AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
            if (!_ap.isDone()) {
               for (String criteria : _ap.getRemainingCriteria()) {
                  _player.getAdvancements().award(_adv, criteria);
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("PlayerRadiationDoseProcedure.execute", _wtSafe);
      }
   }
}
