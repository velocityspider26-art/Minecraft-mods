package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class SummonatorDamagedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double spawnx = 0.0;
      double spawnz = 0.0;
      if (world instanceof ServerLevel _level) {
         _level.sendParticles(ParticleTypes.CLOUD, x + 0.5, y + 0.5, z + 0.5, 5, 0.4, 0.4, 0.4, 1.0);
      }

      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemmad")),
               SoundSource.NEUTRAL,
               20.0F,
               0.2F
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemmad")),
               SoundSource.NEUTRAL,
               20.0F,
               0.2F,
               false
            );
         }
      }

      if (world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.deactivate")),
               SoundSource.NEUTRAL,
               20.0F,
               0.2F
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.deactivate")),
               SoundSource.NEUTRAL,
               20.0F,
               0.2F,
               false
            );
         }
      }

      for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 2, 4); index0++) {
         spawnx = (double)Mth.nextInt(RandomSource.create(), -10, 10);
         spawnz = (double)Mth.nextInt(RandomSource.create(), -10, 10);
         if (world instanceof ServerLevel _levelxx) {
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.STRIKER.get())
               .spawn(
                  _levelxx,
                  BlockPos.containing(
                     spawnx + x, (double)(2 + world.getHeight(Types.MOTION_BLOCKING, Mth.floor(spawnx + x), Mth.floor(spawnz + z))), spawnz + z
                  ),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }

         if (world instanceof ServerLevel _levelxxx) {
            _levelxxx.sendParticles(
               (SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(),
               spawnx + x,
               (double)(2 + world.getHeight(Types.MOTION_BLOCKING, Mth.floor(spawnx + x), Mth.floor(spawnz + z))),
               spawnz + z,
               5,
               0.25,
               0.25,
               0.25,
               0.25
            );
         }
      }

      if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxx, null, x, y, z, 3.0F, ExplosionInteraction.NONE);
      }

      world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SummonatorDamagedProcedure.execute", _wtSafe);
      }
   }
}
