package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class IncindiaryRocketFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean Trigger = false;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectilelibsProcedure.execute();
         immediatesourceentity.getPersistentData().putDouble("MaxTime", 40.0);
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("Time") <= 40.0) {
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
            if (immediatesourceentity.getPersistentData().getDouble("Time") / 10.0
               == (double)Math.round(immediatesourceentity.getPersistentData().getDouble("Time") / 10.0)) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        15.0F,
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        15.0F,
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0),
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
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0)
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketfar")),
                        SoundSource.NEUTRAL,
                        40.0F,
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0),
                        false
                     );
                  }
               }
            }

            if (world instanceof ServerLevel _levelxx) {
               _levelxx.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
            }

            Vec3 motion = immediatesourceentity.getDeltaMovement().scale(1.0 + 0.02 * mvmultiplier);
            immediatesourceentity.setDeltaMovement(motion);
         }

         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }

            CrustyChunksMod.queueServerWork(1, () -> IncindiaryRocketHitProcedure.execute(world, immediatesourceentity));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("IncindiaryRocketFlightProcedure.execute", _wtSafe);
      }
   }
}
