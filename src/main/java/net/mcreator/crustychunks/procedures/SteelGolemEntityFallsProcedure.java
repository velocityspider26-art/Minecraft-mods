package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class SteelGolemEntityFallsProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      sx = -3.0;
      found = false;

      for (int index0 = 0; index0 < 8; index0++) {
         sy = -1.0;

         for (int index1 = 0; index1 < 3; index1++) {
            sz = -3.0;

            for (int index2 = 0; index2 < 8; index2++) {
               if (world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:splinterable")))
                  || world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:shatterable")))) {
                  found = true;
               }

               sz++;
            }

            sy++;
         }

         sx++;
      }

      if (found) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
      }

      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
               SoundSource.NEUTRAL,
               1.0F,
               0.5F
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
               SoundSource.NEUTRAL,
               1.0F,
               0.5F,
               false
            );
         }
      }

      if (world instanceof ServerLevel _levelx) {
         _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x, y, z, 15, 3.0, 1.0, 3.0, 0.1);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SteelGolemEntityFallsProcedure.execute", _wtSafe);
      }
   }
}
