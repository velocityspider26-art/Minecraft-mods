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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class DrillProjectileHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      if ((
            world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("minecraft:mineable/pickaxe")))
               || world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("minecraft:mineable/shovel")))
         )
         && 5.0F >= world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z))
         && !world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:concrete")))) {
         BlockPos _pos = BlockPos.containing(x, y, z);
         Block.dropResources(world.getBlockState(_pos), world, BlockPos.containing(x + 0.5, y + 0.5, z + 0.5), null);
         world.destroyBlock(_pos, false);
         if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 0.5, z + 0.5, 1, 0.0, 0.0, 0.0, 0.5);
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x + 0.5, y + 0.5, z + 0.5),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  6.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.2)
               );
            } else {
               _level.playLocalSound(
                  x + 0.5,
                  y + 0.5,
                  z + 0.5,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  6.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.2),
                  false
               );
            }
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:concrete")))) {
         HeavyCrackProcedureProcedure.execute(world, x, y, z);
         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 0.5, z + 0.5, 1, 0.0, 0.0, 0.0, 0.5);
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x + 0.5, y + 0.5, z + 0.5),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  6.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.2)
               );
            } else {
               _levelx.playLocalSound(
                  x + 0.5,
                  y + 0.5,
                  z + 0.5,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  6.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.2),
                  false
               );
            }
         }
      }
   }
}
