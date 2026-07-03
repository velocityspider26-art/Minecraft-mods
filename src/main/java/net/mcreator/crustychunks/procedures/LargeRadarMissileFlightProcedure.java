package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LargeRadarMissileFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         boolean detonate = false;
         boolean Trigger = false;
         new Vec3(0.0, 0.0, 0.0);
         BlockState Lblock = Blocks.AIR.defaultBlockState();
         double distancetotarget = 0.0;
         double speed = 0.0;
         double targetspeed = 0.0;
         double leadvariable = 0.0;
         double Limiter = 0.0;
         double mx = 0.0;
         double my = 0.0;
         double mz = 0.0;
         double Xvector = 0.0;
         double Zvector = 0.0;
         double Pitch = 0.0;
         double LX = 0.0;
         double LY = 0.0;
         double LZ = 0.0;
         double RadarTargetX = 0.0;
         double RadarTargetY = 0.0;
         double RadarTargetZ = 0.0;
         double RadarVelocityX = 0.0;
         double RadarVelocityY = 0.0;
         double RadarVelocityZ = 0.0;
         Lblock = world.getBlockState(
            BlockPos.containing(
               immediatesourceentity.getPersistentData().getDouble("LX"),
               immediatesourceentity.getPersistentData().getDouble("LY"),
               immediatesourceentity.getPersistentData().getDouble("LZ")
            )
         );
         LX = immediatesourceentity.getPersistentData().getDouble("LX");
         LY = immediatesourceentity.getPersistentData().getDouble("LY");
         LZ = immediatesourceentity.getPersistentData().getDouble("LZ");
         RadarTargetX = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "TargetX");
         RadarTargetY = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "TargetY");
         RadarTargetZ = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "TargetZ");
         RadarVelocityX = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "MX");
         RadarVelocityY = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "MY");
         RadarVelocityZ = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "MZ");
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         immediatesourceentity.getPersistentData().putDouble("MaxTime", 140.0);
         if (immediatesourceentity.getPersistentData().getDouble("Time") <= 140.0) {
            for (int index0 = 0; index0 < 2; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.ROCKET_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                  y + Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                  z + Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                  Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                  Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                  Mth.nextDouble(RandomSource.create(), -0.05, 0.05)
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
               == (double)Math.round(immediatesourceentity.getPersistentData().getDouble("Time") / 5.0)) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        15.0F,
                        (float)(1.8 - immediatesourceentity.getPersistentData().getDouble("Time") / 80.0)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        15.0F,
                        (float)(1.8 - immediatesourceentity.getPersistentData().getDouble("Time") / 80.0),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketfar")),
                        SoundSource.NEUTRAL,
                        40.0F,
                        (float)(1.8 - immediatesourceentity.getPersistentData().getDouble("Time") / 80.0)
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketfar")),
                        SoundSource.NEUTRAL,
                        40.0F,
                        (float)(1.8 - immediatesourceentity.getPersistentData().getDouble("Time") / 80.0),
                        false
                     );
                  }
               }
            }

            if (world instanceof ServerLevel _levelxx) {
               _levelxx.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
            }

            Vec3 motion = immediatesourceentity.getDeltaMovement().scale(1.02);
            immediatesourceentity.setDeltaMovement(motion);
            immediatesourceentity.setNoGravity(true);
         } else {
            ArtilleryHitProcedure.execute(world, x, y, z, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         if (immediatesourceentity.getPersistentData().getDouble("Time") >= 10.0 && speed > 10.0) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x() * 0.9,
                  immediatesourceentity.getDeltaMovement().y() * 0.9,
                  immediatesourceentity.getDeltaMovement().z() * 0.9
               )
            );
         }

         mx = RadarVelocityX * 1.0;
         my = RadarVelocityY * 1.0;
         mz = RadarVelocityZ * 1.0;
         distancetotarget = Math.sqrt(
            Math.pow(Math.abs(RadarTargetY - y), 2.0) + Math.pow(Math.abs(RadarTargetX - x), 2.0) + Math.pow(Math.abs(RadarTargetZ - z), 2.0)
         );
         speed = Math.sqrt(
            Math.pow(Math.abs(immediatesourceentity.getDeltaMovement().x()), 2.0)
               + Math.pow(Math.abs(immediatesourceentity.getDeltaMovement().y()), 2.0)
               + Math.pow(Math.abs(immediatesourceentity.getDeltaMovement().z()), 2.0)
         );
         Limiter = 0.04 * speed;
         leadvariable = 1.0;
         leadvariable = leadvariable * distancetotarget / speed;
         if (immediatesourceentity.getPersistentData().getDouble("Time") >= 10.0 && (0.0 != RadarTargetX || 0.0 != RadarTargetY || 0.0 != RadarTargetZ)) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  Math.min(
                     immediatesourceentity.getDeltaMovement().x() + Limiter,
                     Math.max(
                        (immediatesourceentity.getDeltaMovement().x() * 7.0 + (RadarTargetX + mx * leadvariable - x) * speed / distancetotarget) / 8.0,
                        immediatesourceentity.getDeltaMovement().x() - Limiter
                     )
                  ),
                  Math.min(
                     immediatesourceentity.getDeltaMovement().y() + Limiter,
                     Math.max(
                        (immediatesourceentity.getDeltaMovement().y() * 7.0 + (RadarTargetY + my * leadvariable - y) * speed / distancetotarget) / 8.0,
                        immediatesourceentity.getDeltaMovement().y() - Limiter
                     )
                  ),
                  Math.min(
                     immediatesourceentity.getDeltaMovement().z() + Limiter,
                     Math.max(
                        (immediatesourceentity.getDeltaMovement().z() * 7.0 + (RadarTargetZ + mz * leadvariable - z) * speed / distancetotarget) / 8.0,
                        immediatesourceentity.getDeltaMovement().z() - Limiter
                     )
                  )
               )
            );
         }

         if (immediatesourceentity.getPersistentData().getDouble("Time") >= 10.0) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(7.5), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:warm")))) {
                  detonate = true;
                  if (0.0 < (entityiterator instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) && !entityiterator.level().isClientSide()) {
                     entityiterator.discard();
                  }
               }
            }
         }

         if (detonate) {
            CrustyChunksMod.queueServerWork(1, () -> {
               ArtilleryHitProcedure.execute(world, x, y, z, immediatesourceentity);
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }

         if (immediatesourceentity.isUnderWater()) {
            ArtilleryHitProcedure.execute(world, x, y, z, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         Vec3 _center = new Vec3(x, y, z);

         for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.0), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))) {
               if (!entityiteratorx.level().isClientSide()) {
                  entityiteratorx.discard();
               }

               Trigger = true;
            }
         }

         if (Trigger) {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }

            CrustyChunksMod.queueServerWork(1, () -> ArtilleryHitProcedure.execute(world, x, y, z, immediatesourceentity));
         }
      }
   }
}
