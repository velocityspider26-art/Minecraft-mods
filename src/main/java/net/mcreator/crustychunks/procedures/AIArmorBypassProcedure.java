package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.AIStealthBulletEntity;
import net.mcreator.crustychunks.entity.HugeAIBulletEntity;
import net.mcreator.crustychunks.entity.MediumAIBulletEntity;
import net.mcreator.crustychunks.entity.SmallAIBulletEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class AIArmorBypassProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity immediatesourceentity) {
      if (entity != null && immediatesourceentity != null) {
         double Health = 0.0;
         double damagemultiplier = 0.0;
         double penetrationmult = 0.0;
         if (immediatesourceentity instanceof SmallAIBulletEntity) {
            penetrationmult = -0.1;
            damagemultiplier = 1.0;
         } else if (immediatesourceentity instanceof AIStealthBulletEntity) {
            penetrationmult = 0.5;
            damagemultiplier = 0.8;
         } else if (immediatesourceentity instanceof MediumAIBulletEntity) {
            penetrationmult = -0.1;
            damagemultiplier = 1.1;
         } else if (immediatesourceentity instanceof HugeAIBulletEntity) {
            penetrationmult = 0.0;
            damagemultiplier = 2.25;
         } else {
            penetrationmult = 0.0;
            damagemultiplier = 1.0;
         }

         if (!entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))) {
            if (!entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bulletproof")))) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:wizz")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        1.0F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:wizz")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if ((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY)
                  .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:bulletarmor")))) {
                  entity.hurt(
                     new DamageSource(
                        world.registryAccess()
                           .registryOrThrow(Registries.DAMAGE_TYPE)
                           .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crusty_chunks:armor_bypass_damage")))
                     ),
                     (float)(4.0 * damagemultiplier + penetrationmult)
                  );
                  ItemStack _ist = entity instanceof LivingEntity _entGetArmorx ? _entGetArmorx.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY;
                  if (world instanceof ServerLevel _srvlvl) _ist.hurtAndBreak(4, _srvlvl, null, _itmcns -> {});
               } else {
                  entity.hurt(
                     new DamageSource(
                        world.registryAccess()
                           .registryOrThrow(Registries.DAMAGE_TYPE)
                           .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crusty_chunks:armor_bypass_damage")))
                     ),
                     (float)(6.0 * damagemultiplier - penetrationmult)
                  );
               }

               if (immediatesourceentity.getY()
                     - immediatesourceentity.getLookAngle().y
                        * Math.sqrt(
                           Math.pow(entity.getX() - immediatesourceentity.getX(), 2.0)
                              + Math.pow(entity.getZ() - immediatesourceentity.getZ(), 2.0)
                        )
                  > entity.getY() + 1.55) {
                  if (!(entity instanceof LivingEntity _entGetArmorx ? _entGetArmorx.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY)
                     .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:bulletarmor")))) {
                     entity.hurt(
                        new DamageSource(
                           world.registryAccess()
                              .registryOrThrow(Registries.DAMAGE_TYPE)
                              .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crusty_chunks:armor_bypass_damage")))
                        ),
                        (float)(6.0 * damagemultiplier - penetrationmult)
                     );
                  } else {
                     entity.hurt(
                        new DamageSource(
                           world.registryAccess()
                              .registryOrThrow(Registries.DAMAGE_TYPE)
                              .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crusty_chunks:armor_bypass_damage")))
                        ),
                        (float)(3.0 * damagemultiplier + penetrationmult)
                     );
                     ItemStack _ist = entity instanceof LivingEntity _entGetArmorx ? _entGetArmorx.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
                     if (world instanceof ServerLevel _srvlvl) _ist.hurtAndBreak(7, _srvlvl, null, _itmcns -> {});
                  }
               }
            } else {
               if (Mth.nextInt(RandomSource.create(), 1, 2) == 1) {
                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                           SoundSource.NEUTRAL,
                           2.0F,
                           1.0F
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                           SoundSource.NEUTRAL,
                           2.0F,
                           1.0F,
                           false
                        );
                     }
                  }
               } else if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bounce")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        1.0F
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bounce")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelxxx) {
                  _levelxxx.sendParticles(ParticleTypes.POOF, x, y, z, 5, 1.0, 3.0, 1.0, 0.05);
               }

               entity.hurt(
                  new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ARROW)),
                  (float)(4.0 * damagemultiplier + penetrationmult)
               );
            }
         }

         if (entity.isInvulnerable() && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   }
}
