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

public class GunMechanismSoundProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double Movementinnacuracy = 0.0;
      double recoil = 0.0;
      CrustyChunksMod.queueServerWork(
         2,
         () -> {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:gunmechanism")),
                     SoundSource.BLOCKS,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:gunmechanism")),
                     SoundSource.BLOCKS,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                     false
                  );
               }
            }
         }
      );
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GunMechanismSoundProcedure.execute", _wtSafe);
      }
   }
}
