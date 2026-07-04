package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class ShotgunFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double Movementinnacuracy = 0.0;
      CrustyChunksMod.queueServerWork(
         1,
         () -> {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:distantshotmedium")),
                     SoundSource.BLOCKS,
                     80.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:distantshotmedium")),
                     SoundSource.BLOCKS,
                     80.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:midrangeshot")),
                     SoundSource.BLOCKS,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:midrangeshot")),
                     SoundSource.BLOCKS,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8),
                     false
                  );
               }
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:largeshot")),
                     SoundSource.BLOCKS,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:largeshot")),
                     SoundSource.BLOCKS,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                     false
                  );
               }
            }
         }
      );
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ShotgunFireSoundProcedure.execute", _wtSafe);
      }
   }
}
