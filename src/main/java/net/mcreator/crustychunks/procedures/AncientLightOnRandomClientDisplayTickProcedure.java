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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class AncientLightOnRandomClientDisplayTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (1 == Mth.nextInt(RandomSource.create(), 1, 20)) {
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                  SoundSource.NEUTRAL,
                  4.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                  SoundSource.NEUTRAL,
                  4.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                  false
               );
            }
         }

         for (int index0 = 0; index0 < 15; index0++) {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(),
               x + 0.5,
               y - 0.1,
               z + 0.5,
               Mth.nextDouble(RandomSource.create(), -0.4, 0.4),
               Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
               Mth.nextDouble(RandomSource.create(), -0.4, 0.4)
            );
         }
      }

      if (1 == Mth.nextInt(RandomSource.create(), 1, 15) && world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:humm")),
               SoundSource.NEUTRAL,
               6.0F,
               1.2F
            );
         } else {
            _levelx.playLocalSound(
               x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:humm")), SoundSource.NEUTRAL, 6.0F, 1.2F, false
            );
         }
      }
   }
}
