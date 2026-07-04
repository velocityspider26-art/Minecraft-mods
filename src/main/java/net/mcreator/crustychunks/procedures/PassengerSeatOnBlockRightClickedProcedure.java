package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.entity.SeatEntityEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PassengerSeatOnBlockRightClickedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (world instanceof ServerLevel _level) {
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.SEAT_ENTITY.get())
               .spawn(_level, BlockPos.containing(x + 0.5, y, z + 0.5), MobSpawnType.MOB_SUMMONED);
            if (entityToSpawn != null) {
               entityToSpawn.setYRot(45.0F);
               entityToSpawn.setYBodyRot(45.0F);
               entityToSpawn.setYHeadRot(45.0F);
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }

         Vec3 _center = new Vec3(x + 0.5, y, z + 0.5);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(0.125), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator instanceof SeatEntityEntity) {
               entityiterator.lookAt(Anchor.EYES, new Vec3(x + (double)(new Object() {
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
               }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepX() + 0.5, y, z + (double)(new Object() {
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
               }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepZ() + 0.5));
               entity.startRiding(entityiterator);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("PassengerSeatOnBlockRightClickedProcedure.execute", _wtSafe);
      }
   }
}
