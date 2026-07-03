package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.HugeBulletFireEntity;
import net.mcreator.crustychunks.entity.HugeHEBulletFireEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class RACFireScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      Direction playerdirection = Direction.NORTH;
      boolean found = false;
      boolean DetectedPlayer = false;
      BlockPos ammodrum = new BlockPos(0, 0, 0);
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double Pitch = 0.0;
      double Xvector = 0.0;
      double Zvector = 0.0;
      double Barrels = 0.0;
      double mvmultiplier = 0.0;
      mvmultiplier = ProjectilelibsProcedure.execute();
      if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.AUTOCANNON_DRUM.get()) {
         ammodrum = BlockPos.containing(x, y + 1.0, z);
      } else if (world.getBlockState(BlockPos.containing(x, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.AUTOCANNON_DRUM.get()) {
         ammodrum = BlockPos.containing(x, y - 1.0, z);
      } else if (world.getBlockState(BlockPos.containing(x, y, z + 1.0)).getBlock() == CrustyChunksModBlocks.AUTOCANNON_DRUM.get()) {
         ammodrum = BlockPos.containing(x, y, z + 1.0);
      } else if (world.getBlockState(BlockPos.containing(x, y, z - 1.0)).getBlock() == CrustyChunksModBlocks.AUTOCANNON_DRUM.get()) {
         ammodrum = BlockPos.containing(x, y, z - 1.0);
      } else if (world.getBlockState(BlockPos.containing(x + 1.0, y, z)).getBlock() == CrustyChunksModBlocks.AUTOCANNON_DRUM.get()) {
         ammodrum = BlockPos.containing(x + 1.0, y, z);
      } else if (world.getBlockState(BlockPos.containing(x - 1.0, y, z)).getBlock() == CrustyChunksModBlocks.AUTOCANNON_DRUM.get()) {
         ammodrum = BlockPos.containing(x - 1.0, y, z);
      }

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
      }).getDirection(blockstate).getStepX() - (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "X")) || 1.0 > Math.abs((double)(new Object() {
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
      }).getDirection(blockstate).getStepZ() - (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Z"))) {
         Pitch = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Pitch");
         Xvector = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "X") + (double)(new Object() {
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
         }).getDirection(blockstate).getStepX();
         Zvector = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Z") + (double)(new Object() {
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
         }).getDirection(blockstate).getStepZ();
      }

      if (0.0 == Xvector * Zvector) {
         Xvector = (double)(new Object() {
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
         }).getDirection(blockstate).getStepX();
         Zvector = (double)(new Object() {
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
         }).getDirection(blockstate).getStepZ();
      }

      if (world instanceof Level _level41 && _level41.hasNeighborSignal(BlockPos.containing(x, y, z))) {
         if ((new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "fired") && !world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putBoolean("fired", false);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }

         if (0.0 < (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing((double)ammodrum.getX(), (double)ammodrum.getY(), (double)ammodrum.getZ()), "Ammo")
            && (
               (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     })
                     .getValue(world, BlockPos.containing((double)ammodrum.getX(), (double)ammodrum.getY(), (double)ammodrum.getZ()), "AmmoType")
                     .equals("Small")
                  || (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     })
                     .getValue(world, BlockPos.containing((double)ammodrum.getX(), (double)ammodrum.getY(), (double)ammodrum.getZ()), "AmmoType")
                     .equals("SmallHE")
            )
            && !(new Object() {
               public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "fired")
            && world.getBlockState(BlockPos.containing(x + (double)((new Object() {
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
            }).getDirection(blockstate).getStepX() * 1) + 0.5, y + (double)((new Object() {
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
            }).getDirection(blockstate).getStepY() * 1) + 0.5, z + (double)((new Object() {
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
            }).getDirection(blockstate).getStepZ() * 1) + 0.5)).getBlock() == CrustyChunksModBlocks.RAC_BARREL.get()) {
            Barrels = 1.0;
            BlockPos _pos = BlockPos.containing(x + (double)((new Object() {
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
            }).getDirection(blockstate).getStepX() * 1), y + (double)((new Object() {
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
            }).getDirection(blockstate).getStepY() * 1), z + (double)((new Object() {
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
            }).getDirection(blockstate).getStepZ() * 1));
            BlockState _bsx = world.getBlockState(_pos);
            if (_bsx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp) {
               world.setBlock(_pos, (BlockState)_bsx.setValue(_integerProp, 0), 3);
            }

            int _value = 3;
            BlockPos _posx = BlockPos.containing(x + (double)((new Object() {
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
            }).getDirection(blockstate).getStepX() * 1), y + (double)((new Object() {
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
            }).getDirection(blockstate).getStepY() * 1), z + (double)((new Object() {
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
            }).getDirection(blockstate).getStepZ() * 1));
            BlockState _bsxx = world.getBlockState(_posx);
            if (_bsxx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value)) {
               world.setBlock(_posx, (BlockState)_bsxx.setValue(_integerProp, _value), 3);
            }

            if (world.getBlockState(BlockPos.containing(x + (double)((new Object() {
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
            }).getDirection(blockstate).getStepX() * 2) + 0.5, y + (double)((new Object() {
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
            }).getDirection(blockstate).getStepY() * 2) + 0.5, z + (double)((new Object() {
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
            }).getDirection(blockstate).getStepZ() * 2) + 0.5)).getBlock() == CrustyChunksModBlocks.RAC_BARREL.get()) {
               Barrels = 2.0;
               _pos = BlockPos.containing(x + (double)((new Object() {
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
               }).getDirection(blockstate).getStepX() * 2), y + (double)((new Object() {
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
               }).getDirection(blockstate).getStepY() * 2), z + (double)((new Object() {
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
               }).getDirection(blockstate).getStepZ() * 2));
               _bsx = world.getBlockState(_pos);
               if (_bsx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp) {
                  world.setBlock(_pos, (BlockState)_bsx.setValue(_integerProp, 0), 3);
               }

               int _valuex = 3;
               BlockPos _posxx = BlockPos.containing(x + (double)((new Object() {
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
               }).getDirection(blockstate).getStepX() * 2), y + (double)((new Object() {
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
               }).getDirection(blockstate).getStepY() * 2), z + (double)((new Object() {
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
               }).getDirection(blockstate).getStepZ() * 2));
               _bsxx = world.getBlockState(_posxx);
               if (_bsxx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_valuex)) {
                  world.setBlock(_posxx, (BlockState)_bsxx.setValue(_integerProp, _valuex), 3);
               }

               if (world.getBlockState(BlockPos.containing(x + (double)((new Object() {
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
               }).getDirection(blockstate).getStepX() * 3) + 0.5, y + (double)((new Object() {
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
               }).getDirection(blockstate).getStepY() * 3) + 0.5, z + (double)((new Object() {
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
               }).getDirection(blockstate).getStepZ() * 3) + 0.5)).getBlock() == CrustyChunksModBlocks.RAC_BARREL.get()) {
                  Barrels = 3.0;
                  _pos = BlockPos.containing(x + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepX() * 3), y + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepY() * 3), z + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepZ() * 3));
                  _bsx = world.getBlockState(_pos);
                  if (_bsx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp) {
                     world.setBlock(_pos, (BlockState)_bsx.setValue(_integerProp, 0), 3);
                  }

                  int _valuexx = 3;
                  BlockPos _posxxx = BlockPos.containing(x + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepX() * 3), y + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepY() * 3), z + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepZ() * 3));
                  _bsxx = world.getBlockState(_posxxx);
                  if (_bsxx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_valuexx)) {
                     world.setBlock(_posxxx, (BlockState)_bsxx.setValue(_integerProp, _valuexx), 3);
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepX() * 4) + 0.5, y + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepY() * 4) + 0.5, z + (double)((new Object() {
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
                  }).getDirection(blockstate).getStepZ() * 4) + 0.5)).getBlock() == CrustyChunksModBlocks.RAC_BARREL.get()) {
                     Barrels = 4.0;
                     _pos = BlockPos.containing(x + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepX() * 4), y + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepY() * 4), z + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepZ() * 4));
                     _bsx = world.getBlockState(_pos);
                     if (_bsx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp) {
                        world.setBlock(_pos, (BlockState)_bsx.setValue(_integerProp, 0), 3);
                     }

                     int _valuexxx = 3;
                     BlockPos _posxxxx = BlockPos.containing(x + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepX() * 4), y + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepY() * 4), z + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepZ() * 4));
                     _bsxx = world.getBlockState(_posxxxx);
                     if (_bsxx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp
                        && _integerProp.getPossibleValues().contains(_valuexxx)) {
                        world.setBlock(_posxxxx, (BlockState)_bsxx.setValue(_integerProp, _valuexxx), 3);
                     }

                     if (world.getBlockState(BlockPos.containing(x + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepX() * 5) + 0.5, y + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepY() * 5) + 0.5, z + (double)((new Object() {
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
                     }).getDirection(blockstate).getStepZ() * 5) + 0.5)).getBlock() == CrustyChunksModBlocks.RAC_BARREL.get()) {
                        Barrels = 5.0;
                        _pos = BlockPos.containing(x + (double)((new Object() {
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
                        }).getDirection(blockstate).getStepX() * 5), y + (double)((new Object() {
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
                        }).getDirection(blockstate).getStepY() * 5), z + (double)((new Object() {
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
                        }).getDirection(blockstate).getStepZ() * 5));
                        _bsx = world.getBlockState(_pos);
                        if (_bsx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp) {
                           world.setBlock(_pos, (BlockState)_bsx.setValue(_integerProp, 0), 3);
                        }

                        int _valuexxxx = 3;
                        BlockPos _posxxxxx = BlockPos.containing(x + (double)((new Object() {
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
                        }).getDirection(blockstate).getStepX() * 5), y + (double)((new Object() {
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
                        }).getDirection(blockstate).getStepY() * 5), z + (double)((new Object() {
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
                        }).getDirection(blockstate).getStepZ() * 5));
                        _bsxx = world.getBlockState(_posxxxxx);
                        if (_bsxx.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp
                           && _integerProp.getPossibleValues().contains(_valuexxxx)) {
                           world.setBlock(_posxxxxx, (BlockState)_bsxx.setValue(_integerProp, _valuexxxx), 3);
                        }
                     }
                  }
               }
            }

            if (!world.isClientSide()) {
               _pos = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityx = world.getBlockEntity(_pos);
               _bsxx = world.getBlockState(_pos);
               if (_blockEntityx != null) {
                  _blockEntityx.getPersistentData().putDouble("Barrels", Barrels);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_pos, _bsxx, _bsxx, 3);
               }
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepX() * (1.0 + Barrels) + 0.5, y + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY() * (1.0 + Barrels) + 0.5, z + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:brtttfar")),
                     SoundSource.BLOCKS,
                     60.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _level.playLocalSound(
                     x + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepX() * (1.0 + Barrels) + 0.5,
                     y + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY() * (1.0 + Barrels) + 0.5,
                     z + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:brtttfar")),
                     SoundSource.BLOCKS,
                     60.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepX() * (1.0 + Barrels) + 0.5, y + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY() * (1.0 + Barrels) + 0.5, z + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rac")),
                     SoundSource.BLOCKS,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                  );
               } else {
                  _levelx.playLocalSound(
                     x + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepX() * (1.0 + Barrels) + 0.5,
                     y + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY() * (1.0 + Barrels) + 0.5,
                     z + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rac")),
                     SoundSource.BLOCKS,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                     false
                  );
               }
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepX() * (1.0 + Barrels) + 0.5, y + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY() * (1.0 + Barrels) + 0.5, z + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:autocannonshot")),
                     SoundSource.BLOCKS,
                     8.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.6)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepX() * (1.0 + Barrels) + 0.5,
                     y + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY() * (1.0 + Barrels) + 0.5,
                     z + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:autocannonshot")),
                     SoundSource.BLOCKS,
                     8.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.6),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel _levelxxx) {
               _levelxxx.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + (double)(new Object() {
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
               }).getDirection(blockstate).getStepX() * (2.0 + Barrels) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (2.0 + Barrels) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (2.0 + Barrels) + 0.5, 3, 0.2, 0.2, 0.2, 0.03);
            }

            MuzzleFlashProcedure.execute(world, x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX() * (1.0 + Barrels) + 0.5, y + (double)(new Object() {
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
            }).getDirection(blockstate).getStepY() * (1.0 + Barrels) + 0.5, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5);
            if (!world.isClientSide()) {
               _pos = BlockPos.containing((double)ammodrum.getX(), (double)ammodrum.getY(), (double)ammodrum.getZ());
               BlockEntity _blockEntityxx = world.getBlockEntity(_pos);
               _bsxx = world.getBlockState(_pos);
               if (_blockEntityxx != null) {
                  _blockEntityxx.getPersistentData()
                     .putDouble(
                        "Ammo",
                        (new Object() {
                              public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                 BlockEntity blockEntity = world.getBlockEntity(pos);
                                 return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                              }
                           }).getValue(
                              world, BlockPos.containing((double)ammodrum.getX(), (double)ammodrum.getY(), (double)ammodrum.getZ()), "Ammo"
                           )
                           - 1.0
                     );
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_pos, _bsxx, _bsxx, 3);
               }
            }

            if (!world.isClientSide()) {
               _pos = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxx = world.getBlockEntity(_pos);
               _bsxx = world.getBlockState(_pos);
               if (_blockEntityxxx != null) {
                  _blockEntityxxx.getPersistentData().putBoolean("fired", true);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_pos, _bsxx, _bsxx, 3);
               }
            }

            if ((new Object() {
                  public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                  }
               })
               .getValue(world, BlockPos.containing((double)ammodrum.getX(), (double)ammodrum.getY(), (double)ammodrum.getZ()), "AmmoType")
               .equals("Small")) {
               if (world instanceof ServerLevel projectileLevel) {
                  Projectile _entityToSpawn = (new Object() {
                        public Projectile getArrow(Level level, float damage, int knockback) {
                           AbstractArrow entityToSpawn = new HugeBulletFireEntity((EntityType<? extends HugeBulletFireEntity>)CrustyChunksModEntities.HUGE_BULLET_FIRE.get(), level) {
               @Override
               protected void doKnockback(LivingEntity livingEntity, DamageSource damageSource) {
                  if (knockback > 0) {
                     double _kbres = Math.max(0.0, 1.0 - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                     Vec3 _kbvec = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * _kbres);
                     if (_kbvec.lengthSqr() > 0.0) {
                        livingEntity.push(_kbvec.x, 0.1, _kbvec.z);
                     }
                  }
               }
            };
                           entityToSpawn.setBaseDamage((double)damage);
                           entityToSpawn.setSilent(true);
                           return entityToSpawn;
                        }
                     })
                     .getArrow(projectileLevel, 5.0F, 1);
                  _entityToSpawn.setPos(x + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepX() * (2.0 + Barrels) + 0.5, y + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY() * (2.0 + Barrels) + 0.5, z + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ() * (2.0 + Barrels) + 0.5);
                  _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((5.0 + Barrels / 1.5) * mvmultiplier), (float)(8.0 / (Barrels * 2.0)));
                  projectileLevel.addFreshEntity(_entityToSpawn);
               }
            } else if ((new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  })
                  .getValue(world, BlockPos.containing((double)ammodrum.getX(), (double)ammodrum.getY(), (double)ammodrum.getZ()), "AmmoType")
                  .equals("SmallHE")
               && world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new HugeHEBulletFireEntity((EntityType<? extends HugeHEBulletFireEntity>)CrustyChunksModEntities.HUGE_HE_BULLET_FIRE.get(), level) {
               @Override
               protected void doKnockback(LivingEntity livingEntity, DamageSource damageSource) {
                  if (knockback > 0) {
                     double _kbres = Math.max(0.0, 1.0 - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                     Vec3 _kbvec = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * _kbres);
                     if (_kbvec.lengthSqr() > 0.0) {
                        livingEntity.push(_kbvec.x, 0.1, _kbvec.z);
                     }
                  }
               }
            };
                        entityToSpawn.setBaseDamage((double)damage);
                        entityToSpawn.setSilent(true);
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, 5.0F, 1);
               _entityToSpawn.setPos(x + (double)(new Object() {
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
               }).getDirection(blockstate).getStepX() * (2.0 + Barrels) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (2.0 + Barrels) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (2.0 + Barrels) + 0.5);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((5.0 + Barrels / 1.5) * mvmultiplier), (float)(8.0 / (Barrels * 2.0)));
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }
      }
   }
}
