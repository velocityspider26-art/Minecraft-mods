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

public class PistolFireSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      double Movementinnacuracy = 0.0;
      double recoil = 0.0;
      double pitch = 0.0;
      CrustyChunksMod.queueServerWork(
         1,
         () -> {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:distantgunfire")),
                     SoundSource.BLOCKS,
                     80.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.2)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:distantgunfire")),
                     SoundSource.BLOCKS,
                     80.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.2),
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallshot")),
                     SoundSource.BLOCKS,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallshot")),
                     SoundSource.BLOCKS,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                     false
                  );
               }
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallshot")),
                     SoundSource.BLOCKS,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallshot")),
                     SoundSource.BLOCKS,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                     false
                  );
               }
            }
         }
      );
   }
}
