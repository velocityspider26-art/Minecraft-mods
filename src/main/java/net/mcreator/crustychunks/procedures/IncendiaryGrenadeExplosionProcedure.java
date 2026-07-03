package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class IncendiaryGrenadeExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         GasolineExplosionProcedure.execute(
            world,
            x + immediatesourceentity.getLookAngle().x * Mth.nextDouble(RandomSource.create(), 1.5, 2.5),
            y + immediatesourceentity.getLookAngle().y * Mth.nextDouble(RandomSource.create(), 1.5, 2.5),
            z - immediatesourceentity.getLookAngle().z * Mth.nextDouble(RandomSource.create(), 1.5, 2.5)
         );
         GasolineExplosionProcedure.execute(
            world,
            x + immediatesourceentity.getLookAngle().x * Mth.nextDouble(RandomSource.create(), 1.5, 2.5),
            y + immediatesourceentity.getLookAngle().y * Mth.nextDouble(RandomSource.create(), 1.5, 2.5),
            z - immediatesourceentity.getLookAngle().z * Mth.nextDouble(RandomSource.create(), 1.5, 2.5)
         );
         GasolineExplosionProcedure.execute(
            world,
            x + immediatesourceentity.getLookAngle().x * Mth.nextDouble(RandomSource.create(), 1.5, 2.5),
            y + immediatesourceentity.getLookAngle().y * Mth.nextDouble(RandomSource.create(), 1.5, 2.5),
            z - immediatesourceentity.getLookAngle().z * Mth.nextDouble(RandomSource.create(), 1.5, 2.5)
         );
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                  SoundSource.NEUTRAL,
                  10.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                  SoundSource.NEUTRAL,
                  10.0F,
                  1.0F,
                  false
               );
            }
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion_distant")),
                  SoundSource.NEUTRAL,
                  40.0F,
                  1.0F
               );
            } else {
               _levelx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion_distant")),
                  SoundSource.NEUTRAL,
                  40.0F,
                  1.0F,
                  false
               );
            }
         }
      }
   }
}
