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
import net.minecraft.world.level.block.Blocks;

public class SmallBulletHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:dirts")))) {
         if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x + 0.5, y + 1.0, z + 0.5, 4, 0.0, 2.0, 0.0, 1.0);
         }

         world.levelEvent(2001, BlockPos.containing(x, y + 1.0, z), Block.getId(Blocks.DIRT.defaultBlockState()));
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:shatterable")))) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")),
                  SoundSource.NEUTRAL,
                  3.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")), SoundSource.NEUTRAL, 3.0F, 1.0F, false
               );
            }
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:chippable")))
         && world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F,
               false
            );
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:resistant")))) {
         if (Mth.nextInt(RandomSource.create(), 1, 3) == 3) {
            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bounce")),
                     SoundSource.NEUTRAL,
                     1.5F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bounce")),
                     SoundSource.NEUTRAL,
                     1.5F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }
         } else if (world instanceof Level _levelxxx) {
            if (!_levelxxx.isClientSide()) {
               _levelxxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
               );
            } else {
               _levelxxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                  false
               );
            }
         }

         if (world instanceof ServerLevel _levelxxxx) {
            _levelxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMALL_PUFF.get(), x + 0.5, y + 1.0, z + 0.5, 4, 0.0, 0.0, 0.0, 0.7);
         }

         if (world instanceof ServerLevel _levelxxxx) {
            _levelxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x + 0.5, y + 1.0, z + 0.5, 4, 0.3, 0.3, 0.3, 1.5);
         }
      } else if (world instanceof Level _levelxxxx) {
         if (!_levelxxxx.isClientSide()) {
            _levelxxxx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:wizz")),
               SoundSource.NEUTRAL,
               1.5F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
            );
         } else {
            _levelxxxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:wizz")),
               SoundSource.NEUTRAL,
               1.5F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
               false
            );
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:sands")))) {
         if (world instanceof ServerLevel _levelxxxxx) {
            _levelxxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SAND.get(), x + 0.5, y + 1.0, z + 0.5, 5, 0.0, 2.0, 0.0, 1.0);
         }

         world.levelEvent(2001, BlockPos.containing(x, y + 1.0, z), Block.getId(Blocks.SAND.defaultBlockState()));
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:dusts")))) {
         if (world instanceof ServerLevel _levelxxxxx) {
            _levelxxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.WHITE_DUST.get(), x + 0.5, y + 1.0, z + 0.5, 5, 0.0, 2.0, 0.0, 1.0);
         }

         world.levelEvent(2001, BlockPos.containing(x, y + 1.0, z), Block.getId(Blocks.GRAVEL.defaultBlockState()));
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:metals")))) {
         if (world instanceof Level _levelxxxxx) {
            if (!_levelxxxxx.isClientSide()) {
               _levelxxxxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  1.5F
               );
            } else {
               _levelxxxxx.playLocalSound(
                  x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")), SoundSource.NEUTRAL, 2.0F, 1.5F, false
               );
            }
         }

         if (world instanceof ServerLevel _levelxxxxxx) {
            _levelxxxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x + 0.5, y + 1.0, z + 0.5, 7, 0.3, 0.3, 0.3, 1.0);
         }
      }

      world.levelEvent(2001, BlockPos.containing(x, y + 1.0, z), Block.getId(world.getBlockState(BlockPos.containing(x, y, z))));
      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:splinterable")))
         && world instanceof Level _levelxxxxxx) {
         if (!_levelxxxxxx.isClientSide()) {
            _levelxxxxxx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F
            );
         } else {
            _levelxxxxxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F,
               false
            );
         }
      }
   }
}
