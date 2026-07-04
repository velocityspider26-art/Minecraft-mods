package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class ShotgunCasingDropProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:shotguncasing")),
               SoundSource.NEUTRAL,
               3.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:shotguncasing")),
               SoundSource.NEUTRAL,
               3.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
               false
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ShotgunCasingDropProcedure.execute", _wtSafe);
      }
   }
}
