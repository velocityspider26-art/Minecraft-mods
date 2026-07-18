package net.mcreator.crustychunks.procedures;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;

public class AshClientParticlesProcedure {
   private static long nextAllowedPlayTime = 0L;
   private static final long SOUND_DURATION_MS = 312000L;

   public static void execute(LevelAccessor world, double x, double y, double z) {
      for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 40, 60); index0++) {
         world.addParticle(
            ParticleTypes.ASH,
            x + Mth.nextDouble(RandomSource.create(), -7.0, 7.0),
            y + Mth.nextDouble(RandomSource.create(), 1.0, 10.0),
            z + Mth.nextDouble(RandomSource.create(), -7.0, 7.0),
            0.0,
            0.0,
            0.0
         );
      }

      long currentTime = System.currentTimeMillis();
      if (currentTime > nextAllowedPlayTime && Mth.nextInt(RandomSource.create(), 0, 200) == 0) {
         Minecraft mc = Minecraft.getInstance();
         mc.getMusicManager().stopPlaying();
         mc.level
            .playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("minecraft:music.nether.nether_wastes")),
               SoundSource.AMBIENT,
               1.0F,
               1.0F,
               false
            );
         nextAllowedPlayTime = currentTime + 312000L;
      }
   }
}
