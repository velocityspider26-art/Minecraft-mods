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
import net.minecraft.world.level.block.Blocks;

public class GasDisperseProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (!world.getBlockState(BlockPos.containing(x, y + 1.0, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x, y - 1.0, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x + 1.0, y, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x - 1.0, y, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x, y, z + 1.0)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x, y, z - 1.0)).canOcclude()) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
         if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMALL_PUFF.get(), x + 0.5, y + 0.5, z + 0.5, 7, 0.0, 0.0, 0.0, 1.0);
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                  SoundSource.BLOCKS,
                  1.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.8)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                  SoundSource.BLOCKS,
                  1.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.8),
                  false
               );
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GasDisperseProcedure.execute", _wtSafe);
      }
   }
}
