package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RobotChuteOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (null != world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x + 0.5, y, z + 0.5), 10.0, 10.0, 10.0), e -> true).stream().sorted((new Object() {
         Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
            return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
         }
      }).compareDistOf(x + 0.5, y, z + 0.5)).findFirst().orElse(null) && (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Spawned") < 2.0) {
         if (world instanceof ServerLevel _level) {
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.STRIKER.get())
               .spawn(_level, BlockPos.containing(x + 0.5, y + 1.0, z + 0.5), MobSpawnType.MOB_SUMMONED);
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                  SoundSource.NEUTRAL,
                  10.0F,
                  1.0F
               );
            } else {
               _levelx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                  SoundSource.NEUTRAL,
                  10.0F,
                  1.0F,
                  false
               );
            }
         }

         if (world instanceof ServerLevel _levelxx) {
            _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x + 0.5, y + 1.0, z + 0.5, 5, 0.0, 0.0, 0.0, 0.25);
         }

         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putDouble("Spawned", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Spawned") + 1.0);
            }

            if (world instanceof Level _levelxx) {
               _levelxx.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RobotChuteOnTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
