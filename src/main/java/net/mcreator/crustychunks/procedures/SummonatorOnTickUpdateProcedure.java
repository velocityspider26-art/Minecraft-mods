package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SummonatorOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double Modules = 0.0;
      double spawnx = 0.0;
      double spawnz = 0.0;
      boolean PlayerFound = false;
      if (Mth.nextInt(RandomSource.create(), 1, 10) == 10) {
         Rad1TickProcedure.execute(world, x, y, z);
      }

      Modules = 0.0;
      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(15.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         if (entityiterator instanceof LivingEntity) {
            LivingEntity _entity = (LivingEntity)entityiterator;
            if (!_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1, false, false));
            }
         }
      }

      if (world.getBlockState(BlockPos.containing(x + 20.0, y, z)).getBlock() == CrustyChunksModBlocks.SUMMONATOR_MODULE.get()) {
         if (world instanceof ServerLevel _level) {
            _level.sendParticles(ParticleTypes.GLOW, x + 10.0 + 0.5, y + 0.5, z + 0.5, 15, 5.0, 0.0, 0.0, 0.01);
         }

         Modules++;
      }

      if (world.getBlockState(BlockPos.containing(x, y, z + 20.0)).getBlock() == CrustyChunksModBlocks.SUMMONATOR_MODULE.get()) {
         if (world instanceof ServerLevel _level) {
            _level.sendParticles(ParticleTypes.GLOW, x + 0.5, y + 0.5, z + 10.0 + 0.5, 15, 0.0, 0.0, 5.0, 0.01);
         }

         Modules++;
      }

      if (world.getBlockState(BlockPos.containing(x - 20.0, y, z)).getBlock() == CrustyChunksModBlocks.SUMMONATOR_MODULE.get()) {
         if (world instanceof ServerLevel _level) {
            _level.sendParticles(ParticleTypes.GLOW, x - 10.0 + 0.5, y + 0.5, z + 0.5, 15, 5.0, 0.0, 0.0, 0.01);
         }

         Modules++;
      }

      if (world.getBlockState(BlockPos.containing(x, y, z - 20.0)).getBlock() == CrustyChunksModBlocks.SUMMONATOR_MODULE.get()) {
         if (world instanceof ServerLevel _level) {
            _level.sendParticles(ParticleTypes.GLOW, x + 0.5, y + 0.5, z - 10.0 + 0.5, 15, 0.0, 0.0, 5.0, 0.01);
         }

         Modules++;
      }

      if (Modules > 0.0) {
         if ((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Timer") > 0.0) {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.deactivate")),
                     SoundSource.NEUTRAL,
                     40.0F,
                     (float)((new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Timer") / 100.0)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.deactivate")),
                     SoundSource.NEUTRAL,
                     40.0F,
                     (float)((new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Timer") / 100.0),
                     false
                  );
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockEntity _blockEntity = world.getBlockEntity(_bp);
               BlockState _bs = world.getBlockState(_bp);
               if (_blockEntity != null) {
                  _blockEntity.getPersistentData().putDouble("Timer", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Timer") - 1.0);
               }

               if (world instanceof Level _levelx) {
                  _levelx.sendBlockUpdated(_bp, _bs, _bs, 3);
               }
            }
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.activate")),
                  SoundSource.NEUTRAL,
                  5.0F,
                  (float)(0.2 + Modules / 3.0)
               );
            } else {
               _levelx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.activate")),
                  SoundSource.NEUTRAL,
                  5.0F,
                  (float)(0.2 + Modules / 3.0),
                  false
               );
            }
         }

         if (world instanceof ServerLevel _levelxx) {
            _levelxx.sendParticles(ParticleTypes.FIREWORK, x + 0.5, y + 5.0, z + 0.5, (int)(5.0 * Modules), 0.0, 10.0, 0.0, 0.01);
         }

         if (null != world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 20.0, 20.0, 20.0), e -> true).stream().sorted((new Object() {
            Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
               return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
            }
         }).compareDistOf(x, y, z)).findFirst().orElse(null)) {
            if (1 == Mth.nextInt(RandomSource.create(), 1, 150)) {
               spawnx = x + (double)Mth.nextInt(RandomSource.create(), -30, 30);
               spawnz = z + (double)Mth.nextInt(RandomSource.create(), -30, 30);
               if (world instanceof ServerLevel _levelxx) {
                  Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.STRIKER.get())
                     .spawn(
                        _levelxx,
                        BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 2), spawnz),
                        MobSpawnType.MOB_SUMMONED
                     );
                  if (entityToSpawn != null) {
                     entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
                  }
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemmad")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        0.5F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemmad")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        0.5F,
                        false
                     );
                  }
               }
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 600)) {
               spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
               spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
               if (world instanceof ServerLevel _levelxxxx) {
                  Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.HUNTER.get())
                     .spawn(
                        _levelxxxx,
                        BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 45), spawnz),
                        MobSpawnType.MOB_SUMMONED
                     );
                  if (entityToSpawn != null) {
                     entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
                  }
               }

               if (world instanceof Level _levelxxxxx) {
                  if (!_levelxxxxx.isClientSide()) {
                     _levelxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemidle")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        0.5F
                     );
                  } else {
                     _levelxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemidle")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        0.5F,
                        false
                     );
                  }
               }
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 600)) {
               Vec3 _center1 = new Vec3(x, y, z);

               for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center1, _center1).inflate(256.0), e -> true)
                  .stream()
                  .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center1)))
                  .toList()) {
                  if (entityiteratorx instanceof Player && entityiteratorx instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiteratorx;
                     if (!_entity.level().isClientSide()) {
                        _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.IMPENDING_DOOM, 10000, 0, false, false));
                     }
                  }
               }
            }
         }
      } else {
         if (!world.isClientSide()) {
            BlockPos _bpx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
            BlockState _bsx = world.getBlockState(_bpx);
            if (_blockEntityx != null) {
               _blockEntityx.getPersistentData().putDouble("Timer", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Timer") + 1.0);
            }

            if (world instanceof Level _levelxxxxxx) {
               _levelxxxxxx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
            }
         }

         if (world instanceof Level _levelxxxxxx) {
            if (!_levelxxxxxx.isClientSide()) {
               _levelxxxxxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                  SoundSource.NEUTRAL,
                  40.0F,
                  (float)((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Timer") / 100.0)
               );
            } else {
               _levelxxxxxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                  SoundSource.NEUTRAL,
                  40.0F,
                  (float)((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Timer") / 100.0),
                  false
               );
            }
         }

         if ((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Timer") > 200.0) {
            PlayerFound = false;
            Vec3 _center2 = new Vec3(x, y, z);

            for (Entity entityiteratorxx : world.getEntitiesOfClass(Entity.class, new AABB(_center2, _center2).inflate(256.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center2)))
               .toList()) {
               if (entityiteratorxx instanceof Player && !PlayerFound) {
                  if (entityiteratorxx instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiteratorxx;
                     if (!_entity.level().isClientSide()) {
                        _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.IMPENDING_DOOM, 40000, 1, false, false));
                     }
                  }

                  PlayerFound = true;
               }
            }

            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            IncindiaryExplosionProcedure.execute(world, x, y, z);

            for (int index0 = 0; index0 < 10; index0++) {
               spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
               spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
               if (world instanceof ServerLevel) {
                  ServerLevel _levelxxxxxxx = (ServerLevel)world;
                  Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.STRIKER.get())
                     .spawn(
                        _levelxxxxxxx,
                        BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 2), spawnz),
                        MobSpawnType.MOB_SUMMONED
                     );
                  if (entityToSpawn != null) {
                     entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
                  }
               }
            }

            CrustyChunksModVariables.MapVariables.get(world).Production--;
            CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SummonatorOnTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
