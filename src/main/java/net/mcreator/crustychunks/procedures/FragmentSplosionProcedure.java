package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class FragmentSplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion_distant")),
               SoundSource.NEUTRAL,
               40.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion_distant")),
               SoundSource.NEUTRAL,
               40.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
               false
            );
         }
      }

      if (world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.NEUTRAL,
               7.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.NEUTRAL,
               7.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
               false
            );
         }
      }

      if (world instanceof ServerLevel _levelxx) {
         _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.LARGE_SMOKE.get(), x + 0.5, y + 0.5, z + 0.5, 5, 1.0, 1.0, 1.0, 1.0);
      }

      if (world instanceof ServerLevel _levelxx) {
         _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x + 0.5, y + 1.5, z + 0.5, 40, 0.5, 0.5, 0.5, 1.2);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FragmentSplosionProcedure.execute", _wtSafe);
      }
   }
}
