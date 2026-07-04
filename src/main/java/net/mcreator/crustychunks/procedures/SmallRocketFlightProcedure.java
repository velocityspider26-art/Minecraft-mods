package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class SmallRocketFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         immediatesourceentity.getPersistentData().putDouble("MaxTime", 60.0);
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("Time") <= 60.0) {
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
                  == (double)Math.round(immediatesourceentity.getPersistentData().getDouble("Time") / 10.0)
               && world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.0)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.0),
                     false
                  );
               }
            }

            Vec3 motion = immediatesourceentity.getDeltaMovement().scale(1.01);
            immediatesourceentity.setDeltaMovement(motion);
         }

         if (immediatesourceentity.isUnderWater()) {
            RocketHitProcedure.execute(world, x, y, z, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmallRocketFlightProcedure.execute", _wtSafe);
      }
   }
}
