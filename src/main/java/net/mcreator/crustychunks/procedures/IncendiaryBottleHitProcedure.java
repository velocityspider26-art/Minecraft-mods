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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class IncendiaryBottleHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         GasolineExplosionProcedure.execute(
            world,
            x + immediatesourceentity.getLookAngle().x * 2.0,
            y + immediatesourceentity.getLookAngle().y * 2.0,
            z - immediatesourceentity.getLookAngle().z * 2.0
         );
         world.levelEvent(
            2001,
            BlockPos.containing(
               x + immediatesourceentity.getLookAngle().x * 2.0,
               y + immediatesourceentity.getLookAngle().y * 2.0,
               z - immediatesourceentity.getLookAngle().z * 2.0
            ),
            Block.getId(Blocks.GLASS.defaultBlockState())
         );
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                  false
               );
            }
         }

         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("IncendiaryBottleHitProcedure.execute", _wtSafe);
      }
   }
}
