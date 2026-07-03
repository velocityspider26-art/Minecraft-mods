package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class AISiegeTriggerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity.getPersistentData().getDouble("DoomType") == 0.0) {
            AssassinationAttemptProcedure.execute(world, x, y, z);
         } else if (entity.getPersistentData().getDouble("DoomType") == 1.0) {
            CrustyChunksMod.queueServerWork(
               140,
               () -> {
                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y + 40.0, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sonicboom")),
                           SoundSource.NEUTRAL,
                           100.0F,
                           1.0F
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y + 40.0,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sonicboom")),
                           SoundSource.NEUTRAL,
                           100.0F,
                           1.0F,
                           false
                        );
                     }
                  }
               }
            );
            CrustyChunksMod.queueServerWork(
               160,
               () -> {
                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y + 40.0, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:siren")),
                           SoundSource.NEUTRAL,
                           40.0F,
                           0.25F
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y + 40.0,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:siren")),
                           SoundSource.NEUTRAL,
                           40.0F,
                           0.25F,
                           false
                        );
                     }
                  }

                  AISiegeProcedure.execute(world, x, y, z);
               }
            );
         } else if (entity.getPersistentData().getDouble("DoomType") >= 2.0) {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y + 40.0, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:siren")),
                     SoundSource.NEUTRAL,
                     40.0F,
                     0.25F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y + 40.0,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:siren")),
                     SoundSource.NEUTRAL,
                     40.0F,
                     0.25F,
                     false
                  );
               }
            }

            CrustyChunksMod.queueServerWork(256, () -> AISuperSiegeProcedure.execute(world, x, z));
         }
      }
   }
}
