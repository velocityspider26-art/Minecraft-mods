package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.CannonMuzzleFlashProducerEntity;
import net.mcreator.crustychunks.entity.LargeAPFireEntity;
import net.mcreator.crustychunks.entity.LargeFlakProjectileEntity;
import net.mcreator.crustychunks.entity.LargeHEATFireEntity;
import net.mcreator.crustychunks.entity.LargeSmokeFireEntity;
import net.mcreator.crustychunks.entity.LargeSolidProjectileEntity;
import net.mcreator.crustychunks.entity.TankFireProjectileEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class BCFireScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      Direction playerdirection = Direction.NORTH;
      boolean found = false;
      boolean DetectedPlayer = false;
      boolean fire = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double Pitch = 0.0;
      double Xvector = 0.0;
      double Zvector = 0.0;
      double Barrels = 0.0;
      double mvmultiplier = 0.0;
      mvmultiplier = ProjectilelibsProcedure.execute();
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

      if ((new Object() {
         public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Loaded") && (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") <= 0.0 && world.getBlockState(BlockPos.containing(x + (double)((new Object() {
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
      }).getDirection(blockstate).getStepZ() * 1) + 0.5)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:bcbarrel")))) {
         Barrels = 1.0;

         for (int index0 = 0; index0 < 10; index0++) {
            if (world.getBlockState(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ() * (1.0 + Barrels) + 0.5)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:bcbarrel")))) {
               Barrels++;
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putDouble("Barrels", Barrels);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }

         if ((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Barrels") >= 1.0) {
            CrustyChunksMod.queueServerWork(5, () -> {
               if (world instanceof Level _levelx && !_levelx.isClientSide()) {
                  _levelx.explode(null, x + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepX() * (3.0 + (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Barrels")) + 0.5, y + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY() * (3.0 + (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Barrels")) + 0.5, z + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ() * (3.0 + (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Barrels")) + 0.5, 3.0F, ExplosionInteraction.NONE);
               }
            });
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
                  }).getDirection(blockstate).getStepX() * (Barrels + 2.0) + 0.5, y + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY() * (Barrels + 2.0) + 0.5, z + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ() * (Barrels + 2.0) + 0.5),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:battlecannon")),
                  SoundSource.NEUTRAL,
                  15.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
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
                  }).getDirection(blockstate).getStepX() * (Barrels + 2.0) + 0.5,
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
                  }).getDirection(blockstate).getStepY() * (Barrels + 2.0) + 0.5,
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
                  }).getDirection(blockstate).getStepZ() * (Barrels + 2.0) + 0.5,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:battlecannon")),
                  SoundSource.NEUTRAL,
                  15.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
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
                  }).getDirection(blockstate).getStepX() * (Barrels + 23.0) + 0.5, y + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY() * (Barrels + 2.0) + 0.5, z + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ() * (Barrels + 2.0) + 0.5),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:cannonfar")),
                  SoundSource.NEUTRAL,
                  60.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3)
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
                  }).getDirection(blockstate).getStepX() * (Barrels + 23.0) + 0.5,
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
                  }).getDirection(blockstate).getStepY() * (Barrels + 2.0) + 0.5,
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
                  }).getDirection(blockstate).getStepZ() * (Barrels + 2.0) + 0.5,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:cannonfar")),
                  SoundSource.NEUTRAL,
                  60.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3),
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
                  }).getDirection(blockstate).getStepX() * (Barrels + 2.0) + 0.5, y + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY() * (Barrels + 2.0) + 0.5, z + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ() * (Barrels + 2.0) + 0.5),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
                  SoundSource.NEUTRAL,
                  120.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3)
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
                  }).getDirection(blockstate).getStepX() * (Barrels + 2.0) + 0.5,
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
                  }).getDirection(blockstate).getStepY() * (Barrels + 2.0) + 0.5,
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
                  }).getDirection(blockstate).getStepZ() * (Barrels + 2.0) + 0.5,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
                  SoundSource.NEUTRAL,
                  120.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3),
                  false
               );
            }
         }

         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new CannonMuzzleFlashProducerEntity((EntityType<? extends CannonMuzzleFlashProducerEntity>)CrustyChunksModEntities.CANNON_MUZZLE_FLASH_PRODUCER.get(), level) {
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
               .getArrow(projectileLevel, 0.0F, 0);
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
            }).getDirection(blockstate).getStepX() * (Barrels + 1.0) + 0.5, y + (double)(new Object() {
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
            }).getDirection(blockstate).getStepY() * (Barrels + 1.0) + 0.5, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ() * (Barrels + 1.0) + 0.5);
            _entityToSpawn.shoot(Xvector, Pitch, Zvector, 1.0F, (float)(8.0 / (Barrels * 2.0)));
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if ((new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AP")) {
            if (!world.isClientSide()) {
               BlockPos _bpx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
               BlockState _bsx = world.getBlockState(_bpx);
               if (_blockEntityx != null) {
                  _blockEntityx.getPersistentData().putBoolean("AP", false);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new LargeAPFireEntity((EntityType<? extends LargeAPFireEntity>)CrustyChunksModEntities.LARGE_AP_FIRE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                        entityToSpawn.setCritArrow(true);
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, 7.0F, 4, (byte)10);
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
               }).getDirection(blockstate).getStepX() * (Barrels + 1.0) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (Barrels + 1.0) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (Barrels + 1.0) + 0.5);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((3.3 + Barrels / 1.25) * mvmultiplier), (float)(8.0 / (Barrels * 2.0)));
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if ((new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "SOLID")) {
            if (!world.isClientSide()) {
               BlockPos _bpxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
               BlockState _bsxx = world.getBlockState(_bpxx);
               if (_blockEntityxx != null) {
                  _blockEntityxx.getPersistentData().putBoolean("SOLID", false);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new LargeSolidProjectileEntity((EntityType<? extends LargeSolidProjectileEntity>)CrustyChunksModEntities.LARGE_SOLID_PROJECTILE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                        entityToSpawn.setCritArrow(true);
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, 7.0F, 4, (byte)8);
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
               }).getDirection(blockstate).getStepX() * (Barrels + 1.0) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (Barrels + 1.0) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (Barrels + 1.0) + 0.5);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((3.2 + Barrels / 1.25) * mvmultiplier), (float)(8.0 / (Barrels * 2.0)));
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if ((new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "HE")) {
            if (!world.isClientSide()) {
               BlockPos _bpxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
               BlockState _bsxxx = world.getBlockState(_bpxxx);
               if (_blockEntityxxx != null) {
                  _blockEntityxxx.getPersistentData().putBoolean("HE", false);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new TankFireProjectileEntity((EntityType<? extends TankFireProjectileEntity>)CrustyChunksModEntities.TANK_FIRE_PROJECTILE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                        entityToSpawn.setCritArrow(true);
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, 10.0F, 2, (byte)10);
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
               }).getDirection(blockstate).getStepX() * (Barrels + 1.0) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (Barrels + 1.0) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (Barrels + 1.0) + 0.5);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((3.0 + Barrels / 1.25) * mvmultiplier), (float)(10.0 / (Barrels * 2.0)));
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         } else if ((new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "HEAT")) {
            if (!world.isClientSide()) {
               BlockPos _bpxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
               BlockState _bsxxxx = world.getBlockState(_bpxxxx);
               if (_blockEntityxxxx != null) {
                  _blockEntityxxxx.getPersistentData().putBoolean("HEAT", false);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new LargeHEATFireEntity((EntityType<? extends LargeHEATFireEntity>)CrustyChunksModEntities.LARGE_HEAT_FIRE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                        entityToSpawn.setCritArrow(true);
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, 10.0F, 2, (byte)10);
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
               }).getDirection(blockstate).getStepX() * (Barrels + 1.0) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (Barrels + 1.0) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (Barrels + 1.0) + 0.5);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((3.0 + Barrels / 1.25) * mvmultiplier), (float)(10.0 / (Barrels * 2.0)));
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         } else if ((new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "FLAK")) {
            if (!world.isClientSide()) {
               BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
               BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
               if (_blockEntityxxxxx != null) {
                  _blockEntityxxxxx.getPersistentData().putBoolean("FLAK", false);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new LargeFlakProjectileEntity((EntityType<? extends LargeFlakProjectileEntity>)CrustyChunksModEntities.LARGE_FLAK_PROJECTILE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                  .getArrow(projectileLevel, 10.0F, 2, (byte)10);
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
               }).getDirection(blockstate).getStepX() * (Barrels + 1.0) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (Barrels + 1.0) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (Barrels + 1.0) + 0.5);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((3.0 + Barrels / 1.25) * mvmultiplier), (float)(10.0 / (Barrels * 2.0)));
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         } else if ((new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "SMOKE")) {
            if (!world.isClientSide()) {
               BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
               BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
               if (_blockEntityxxxxxx != null) {
                  _blockEntityxxxxxx.getPersistentData().putBoolean("SMOKE", false);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new LargeSmokeFireEntity((EntityType<? extends LargeSmokeFireEntity>)CrustyChunksModEntities.LARGE_SMOKE_FIRE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                  .getArrow(projectileLevel, 10.0F, 2, (byte)10);
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
               }).getDirection(blockstate).getStepX() * (Barrels + 1.0) + 0.5, y + (double)(new Object() {
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
               }).getDirection(blockstate).getStepY() * (Barrels + 1.0) + 0.5, z + (double)(new Object() {
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
               }).getDirection(blockstate).getStepZ() * (Barrels + 1.0) + 0.5);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)((3.0 + Barrels / 1.25) * mvmultiplier), (float)(10.0 / (Barrels * 2.0)));
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
            BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
            if (_blockEntityxxxxxxx != null) {
               _blockEntityxxxxxxx.getPersistentData().putDouble("Cooldown", 20.0);
            }

            if (world instanceof Level _levelxxx) {
               _levelxxx.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
            BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
            if (_blockEntityxxxxxxxx != null) {
               _blockEntityxxxxxxxx.getPersistentData().putBoolean("Loaded", false);
            }

            if (world instanceof Level _levelxxx) {
               _levelxxx.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
            }
         }
      }
   }
}
