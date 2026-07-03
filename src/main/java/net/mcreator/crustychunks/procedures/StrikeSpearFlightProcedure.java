package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StrikeSpearFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         boolean detonate = false;
         boolean Trigger = false;
         BlockState Lblock = Blocks.AIR.defaultBlockState();
         double distancetotarget = 0.0;
         double speed = 0.0;
         double targetspeed = 0.0;
         double leadvariable = 0.0;
         double Limiter = 0.0;
         double mx = 0.0;
         double my = 0.0;
         double mz = 0.0;
         double Xvector = 0.0;
         double Zvector = 0.0;
         double Pitch = 0.0;
         double LX = 0.0;
         double LY = 0.0;
         double LZ = 0.0;
         double TargX = 0.0;
         double TargY = 0.0;
         double TargZ = 0.0;
         double startx = 0.0;
         double starty = 0.0;
         double startz = 0.0;
         double vecx = 0.0;
         double vecy = 0.0;
         double vecz = 0.0;
         double distancetostart = 0.0;
         double beamlength = 0.0;
         Lblock = world.getBlockState(
            BlockPos.containing(
               immediatesourceentity.getPersistentData().getDouble("LX"),
               immediatesourceentity.getPersistentData().getDouble("LY"),
               immediatesourceentity.getPersistentData().getDouble("LZ")
            )
         );
         LX = immediatesourceentity.getPersistentData().getDouble("LX");
         LY = immediatesourceentity.getPersistentData().getDouble("LY");
         LZ = immediatesourceentity.getPersistentData().getDouble("LZ");
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         if (1.0 > Math.abs((double)(new Object() {
            public Direction getDirection(BlockState _bs) {
               if (_bs.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty _dp) {
                  return (Direction)_bs.getValue(_dp);
               } else {
                  if (_bs.getBlock().getStateDefinition().getProperty("axis") instanceof EnumProperty _ep && _ep.getPossibleValues().toArray()[0] instanceof Axis) {
                     return Direction.fromAxisAndDirection((Axis)_bs.getValue(_ep), AxisDirection.POSITIVE);
                  }

                  return Direction.NORTH;
               }
            }
         }).getDirection(Lblock).getStepX() - (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "X")) || 0.5 > Math.abs((double)(new Object() {
            public Direction getDirection(BlockState _bs) {
               if (_bs.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty _dp) {
                  return (Direction)_bs.getValue(_dp);
               } else {
                  if (_bs.getBlock().getStateDefinition().getProperty("axis") instanceof EnumProperty _ep && _ep.getPossibleValues().toArray()[0] instanceof Axis) {
                     return Direction.fromAxisAndDirection((Axis)_bs.getValue(_ep), AxisDirection.POSITIVE);
                  }

                  return Direction.NORTH;
               }
            }
         }).getDirection(Lblock).getStepZ() - (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(LX, LY, LZ), "Z"))) {
            beamlength = 600.0;
            startx = (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserStartX");
            starty = (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserStartY");
            startz = (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserStartZ");
            distancetostart = Math.sqrt(Math.pow(Math.abs(starty - y), 2.0) + Math.pow(Math.abs(startx - x), 2.0) + Math.pow(Math.abs(startz - z), 2.0));
            vecx = ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserEndX") - startx) / beamlength;
            vecy = ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserEndY") - starty) / beamlength;
            vecz = ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserEndZ") - startz) / beamlength;
            TargX = (immediatesourceentity.getX() - (startx + vecx * (distancetostart + speed + 1.0))) * -30.0;
            TargY = (immediatesourceentity.getY() - (starty + vecy * (distancetostart + speed + 1.0))) * -30.0;
            TargZ = (immediatesourceentity.getZ() - (startz + vecz * (distancetostart + speed + 1.0))) * -30.0;
            distancetotarget = Math.sqrt(Math.pow(Math.abs((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserEndY") - y), 2.0) + Math.pow(Math.abs((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserEndX") - x), 2.0) + Math.pow(Math.abs((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(LX, LY, LZ), "LaserEndZ") - z), 2.0));
         }

         immediatesourceentity.getPersistentData().putDouble("MaxTime", 220.0);
         if (immediatesourceentity.getPersistentData().getDouble("Time") <= 220.0) {
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.LARGE_BULLET_TRAIL.get(), x, y, z, 0.0, 0.0, 0.0);
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.ROCKET_FLAME.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x(),
               immediatesourceentity.getDeltaMovement().y(),
               immediatesourceentity.getDeltaMovement().z()
            );
            if (immediatesourceentity.getPersistentData().getDouble("Time") / 5.0
               == (double)Math.round(immediatesourceentity.getPersistentData().getDouble("Time") / 5.0)) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(2.0 - immediatesourceentity.getPersistentData().getDouble("Time") / 140.0)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(2.0 - immediatesourceentity.getPersistentData().getDouble("Time") / 140.0),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketfar")),
                        SoundSource.NEUTRAL,
                        25.0F,
                        (float)(2.0 - immediatesourceentity.getPersistentData().getDouble("Time") / 140.0)
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketfar")),
                        SoundSource.NEUTRAL,
                        25.0F,
                        (float)(2.0 - immediatesourceentity.getPersistentData().getDouble("Time") / 140.0),
                        false
                     );
                  }
               }
            }

            Vec3 motion = immediatesourceentity.getDeltaMovement().scale(1.02);
            immediatesourceentity.setDeltaMovement(motion);
            immediatesourceentity.setNoGravity(true);
            speed = Math.sqrt(
               Math.pow(Math.abs(immediatesourceentity.getDeltaMovement().x()), 2.0)
                  + Math.pow(Math.abs(immediatesourceentity.getDeltaMovement().y()), 2.0)
                  + Math.pow(Math.abs(immediatesourceentity.getDeltaMovement().z()), 2.0)
            );
            Limiter = 0.2 * speed;
            if (immediatesourceentity.getPersistentData().getDouble("Time") > 5.0 && (TargX != 0.0 || TargY != 0.0 || TargZ != 0.0)) {
               immediatesourceentity.setDeltaMovement(
                  new Vec3(
                     Math.min(
                        immediatesourceentity.getDeltaMovement().x() + Limiter,
                        Math.max(immediatesourceentity.getDeltaMovement().x() + TargX / distancetotarget, immediatesourceentity.getDeltaMovement().x() - Limiter)
                     ),
                     Math.min(
                        immediatesourceentity.getDeltaMovement().y() + Limiter,
                        Math.max(immediatesourceentity.getDeltaMovement().y() + TargY / distancetotarget, immediatesourceentity.getDeltaMovement().y() - Limiter)
                     ),
                     Math.min(
                        immediatesourceentity.getDeltaMovement().z() + Limiter,
                        Math.max(immediatesourceentity.getDeltaMovement().z() + TargZ / distancetotarget, immediatesourceentity.getDeltaMovement().z() - Limiter)
                     )
                  )
               );
            }
         } else {
            LargeHEATHitProcedure.execute(world, x, y, z, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         if (speed > 4.0 && immediatesourceentity.getPersistentData().getDouble("Time") > 10.0) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x() * 0.8,
                  immediatesourceentity.getDeltaMovement().y() * 0.8,
                  immediatesourceentity.getDeltaMovement().z() * 0.8
               )
            );
         }

         if (immediatesourceentity.isUnderWater()) {
            LargeHEATHitProcedure.execute(world, x, y, z, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         Vec3 _center = new Vec3(x, y, z);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.0), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))) {
               if (!entityiterator.level().isClientSide()) {
                  entityiterator.discard();
               }

               Trigger = true;
            }
         }

         if (Trigger) {
            CrustyChunksMod.queueServerWork(1, () -> LargeHEATHitProcedure.execute(world, x, y, z, immediatesourceentity));
         }
      }
   }
}
