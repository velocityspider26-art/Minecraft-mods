package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import java.util.Map.Entry;
import net.mcreator.crustychunks.entity.HEATEntity;
import net.mcreator.crustychunks.entity.HugeFragmentEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ERAProcedureProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (world.getBlockState(BlockPos.containing(x - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepX(), y - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepY(), z - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepZ()))
         .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:breakable_metal")))) {
         world.destroyBlock(BlockPos.containing(x - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepX(), y - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepY(), z - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepZ()), false);
         world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ERA_4.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.ERA_3.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> entityiterator = _bso.getProperties().iterator();

         while (entityiterator.hasNext()) {

            Property<?> entry_prop = entityiterator.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var19) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ERA_3.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.ERA_2.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var39 = _bso.getProperties().iterator();

         while (var39.hasNext()) {

            Property<?> entry_prop = var39.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var18) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ERA_2.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.ERA_1.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var40 = _bso.getProperties().iterator();

         while (var40.hasNext()) {

            Property<?> entry_prop = var40.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var17) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ERA_1.get()) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.OFFSET_ERA_4.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.OFFSET_ERA_3.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var41 = _bso.getProperties().iterator();

         while (var41.hasNext()) {

            Property<?> entry_prop = var41.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var16) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.OFFSET_ERA_3.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.OFFSET_ERA_2.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var42 = _bso.getProperties().iterator();

         while (var42.hasNext()) {

            Property<?> entry_prop = var42.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var15) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.OFFSET_ERA_2.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.OFFSET_ERA_1.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var43 = _bso.getProperties().iterator();

         while (var43.hasNext()) {

            Property<?> entry_prop = var43.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var14) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.OFFSET_ERA_1.get()) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
      }

      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         if ((entityiterator instanceof HEATEntity || entityiterator instanceof HugeFragmentEntity) && !entityiterator.level().isClientSide()) {
            entityiterator.discard();
         }
      }

      MicroExplosionProcedure.execute(world, x + 0.5 + (double)(new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepX(), y + 0.5 + (double)(new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepY(), z + 0.5 + (double)(new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepZ());
      if (world instanceof Level _level && !_level.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x + 0.5 + (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepX(), y + 0.5 + (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepY(), z + 0.5 + (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepZ(), 2.0F, ExplosionInteraction.BLOCK);
      }
   }
}
