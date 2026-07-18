package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FireSpearFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean Trigger = false;
         immediatesourceentity.getPersistentData().putDouble("MaxTime", 70.0);
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("Time") <= 70.0) {
            for (int index0 = 0; index0 < 4; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  y + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  z + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
               );
            }

            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.ROCKET_FLAME.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x(),
               immediatesourceentity.getDeltaMovement().y(),
               immediatesourceentity.getDeltaMovement().z()
            );
            if (immediatesourceentity.getPersistentData().getDouble("Time") / 5.0
                  == (double)Math.round(immediatesourceentity.getPersistentData().getDouble("Time") / 5.0)
               && world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                     SoundSource.NEUTRAL,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.4)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                     SoundSource.NEUTRAL,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.4),
                     false
                  );
               }
            }

            Vec3 motion = immediatesourceentity.getDeltaMovement().scale(1.01);
            immediatesourceentity.setDeltaMovement(motion);
         }

         if (immediatesourceentity.isUnderWater()) {
            Trigger = true;
         }

         Vec3 _center = new Vec3(
            x - immediatesourceentity.getDeltaMovement().x() * 0.5,
            y - immediatesourceentity.getDeltaMovement().y() * 0.5,
            z - immediatesourceentity.getDeltaMovement().z() * 0.5
         );

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.25), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
               && entityiterator != immediatesourceentity) {
               if (!entityiterator.level().isClientSide()) {
                  entityiterator.discard();
               }

               Trigger = true;
            }
         }

         Vec3 _centerx = new Vec3(x, y, z);

         for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_centerx, _centerx).inflate(1.25), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_centerx)))
            .toList()) {
            if (entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
               && entityiteratorx != immediatesourceentity) {
               if (!entityiteratorx.level().isClientSide()) {
                  entityiteratorx.discard();
               }

               Trigger = true;
            }
         }

         Vec3 _centerxx = new Vec3(
            x + immediatesourceentity.getDeltaMovement().x() * 0.5,
            y + immediatesourceentity.getDeltaMovement().y() * 0.5,
            z + immediatesourceentity.getDeltaMovement().z() * 0.5
         );

         for (Entity entityiteratorxx : world.getEntitiesOfClass(Entity.class, new AABB(_centerxx, _centerxx).inflate(1.25), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_centerxx)))
            .toList()) {
            if (entityiteratorxx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
               && entityiteratorxx != immediatesourceentity) {
               if (!entityiteratorxx.level().isClientSide()) {
                  entityiteratorxx.discard();
               }

               Trigger = true;
            }
         }

         if (Trigger) {
            ArtilleryHitProcedure.execute(world, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FireSpearFlightProcedure.execute", _wtSafe);
      }
   }
}
