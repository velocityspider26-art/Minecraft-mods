package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.entity.AimerBeamEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class AimerNodeUpdateTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      boolean found = false;
      double Multiplier = 0.0;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double FX = 0.0;
      double FY = 0.0;
      double FZ = 0.0;
      double Barrels = 0.0;
      double Xvector = 0.0;
      double Zvector = 0.0;
      double Pitch = 0.0;
      Direction fdirection = Direction.NORTH;
      sx = -6.0;
      found = false;

      for (int index0 = 0; index0 < 12; index0++) {
         sy = -3.0;

         for (int index1 = 0; index1 < 6; index1++) {
            sz = -6.0;

            for (int index2 = 0; index2 < 12; index2++) {
               if (world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:cannon")))
                  && world.getBlockState(BlockPos.containing(x + sx - (double)(new Object() {
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
                  }).getDirection(world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz))).getStepX(), y + sy - (double)(new Object() {
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
                  }).getDirection(world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz))).getStepY(), z + sz - (double)(new Object() {
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
                  }).getDirection(world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz))).getStepZ())).getBlock() != CrustyChunksModBlocks.MANUAL_AIMER
                     .get()) {
                  FX = x + sx;
                  FY = y + sy;
                  FZ = z + sz;
                  fdirection = (new Object() {
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
                  }).getDirection(world.getBlockState(BlockPos.containing(FX, FY, FZ)));
                  if (fdirection != Direction.NORTH && fdirection != Direction.EAST) {
                     Multiplier = -1.0;
                  } else {
                     Multiplier = 1.0;
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bp = BlockPos.containing(FX, FY, FZ);
                     BlockEntity _blockEntity = world.getBlockEntity(_bp);
                     BlockState _bs = world.getBlockState(_bp);
                     if (_blockEntity != null) {
                        _blockEntity.getPersistentData().putDouble("Pitch", Math.tan((new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Pitch") * (Math.PI / 180.0)));
                     }

                     if (world instanceof Level _level) {
                        _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                     }
                  }

                  if (fdirection.getStepX() == 0) {
                     if (!world.isClientSide()) {
                        BlockPos _bpx = BlockPos.containing(FX, FY, FZ);
                        BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                        BlockState _bsx = world.getBlockState(_bpx);
                        if (_blockEntityx != null) {
                           _blockEntityx.getPersistentData().putDouble("X", Math.tan((new Object() {
                              public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                 BlockEntity blockEntity = world.getBlockEntity(pos);
                                 return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                              }
                           }).getValue(world, BlockPos.containing(x, y, z), "Yaw") * (Math.PI / 180.0)) * Multiplier);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                        }
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bpxx = BlockPos.containing(FX, FY, FZ);
                        BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                        BlockState _bsxx = world.getBlockState(_bpxx);
                        if (_blockEntityxx != null) {
                           _blockEntityxx.getPersistentData().putDouble("Z", 0.0);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                        }
                     }
                  }

                  if (fdirection.getStepZ() == 0) {
                     if (!world.isClientSide()) {
                        BlockPos _bpxxx = BlockPos.containing(FX, FY, FZ);
                        BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                        BlockState _bsxxx = world.getBlockState(_bpxxx);
                        if (_blockEntityxxx != null) {
                           _blockEntityxxx.getPersistentData().putDouble("Z", Math.tan((new Object() {
                              public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                 BlockEntity blockEntity = world.getBlockEntity(pos);
                                 return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                              }
                           }).getValue(world, BlockPos.containing(x, y, z), "Yaw") * (Math.PI / 180.0)) * Multiplier);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                        }
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bpxxxx = BlockPos.containing(FX, FY, FZ);
                        BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
                        BlockState _bsxxxx = world.getBlockState(_bpxxxx);
                        if (_blockEntityxxxx != null) {
                           _blockEntityxxxx.getPersistentData().putDouble("X", 0.0);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
                        }
                     }
                  }

                  if (1.0 >= Math.abs((double)fdirection.getStepX() - (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(FX, FY, FZ), "X")) || 0.5 >= Math.abs((double)(new Object() {
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
                  }).getDirection(world.getBlockState(BlockPos.containing(FX, FY, FZ))).getStepZ() - (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(FX, FY, FZ), "Z"))) {
                     Pitch = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(FX, FY, FZ), "Pitch") + 0.05;
                     Xvector = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(FX, FY, FZ), "X") + (double)fdirection.getStepX();
                     Zvector = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(FX, FY, FZ), "Z") + (double)fdirection.getStepZ();
                  }

                  if (0.0 == Xvector * Zvector) {
                     Xvector = (double)fdirection.getStepX();
                     Zvector = (double)fdirection.getStepZ();
                  }

                  if ((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Updated") > 0.0 && !world.isClientSide()) {
                     BlockPos _bpxxxxx = BlockPos.containing(FX, FY, FZ);
                     BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
                     BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
                     if (_blockEntityxxxxx != null) {
                        _blockEntityxxxxx.getPersistentData().putDouble("Updated", (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Updated"));
                     }

                     if (world instanceof Level _level) {
                        _level.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
                     }
                  }

                  if ((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(FX, FY, FZ), "Updated") > 0.0) {
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxx = BlockPos.containing(FX, FY, FZ);
                        BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
                        BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
                        if (_blockEntityxxxxxx != null) {
                           _blockEntityxxxxxx.getPersistentData().putDouble("Updated", (new Object() {
                              public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                 BlockEntity blockEntity = world.getBlockEntity(pos);
                                 return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                              }
                           }).getValue(world, BlockPos.containing(FX, FY, FZ), "Updated") - 1.0);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
                        }
                     }

                     if (world.getBlockState(BlockPos.containing(FX, FY, FZ)).getBlock() == CrustyChunksModBlocks.ORDINANCE_CORE.get()) {
                        if (world instanceof ServerLevel projectileLevel) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, float damage, int knockback) {
                                    AbstractArrow entityToSpawn = new AimerBeamEntity((EntityType<? extends AimerBeamEntity>)CrustyChunksModEntities.AIMER_BEAM.get(), level) {
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
                           _entityToSpawn.setPos(FX + (double)(2 * fdirection.getStepX()) + 0.5, FY + 0.5, FZ + (double)(2 * fdirection.getStepZ()) + 0.5);
                           _entityToSpawn.shoot(Xvector, Pitch, Zvector, 1.0F, 0.0F);
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     } else if (world.getBlockState(BlockPos.containing(FX, FY, FZ)).getBlock() == CrustyChunksModBlocks.MORTAR.get()) {
                        if (world instanceof ServerLevel projectileLevel) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, float damage, int knockback) {
                                    AbstractArrow entityToSpawn = new AimerBeamEntity((EntityType<? extends AimerBeamEntity>)CrustyChunksModEntities.AIMER_BEAM.get(), level) {
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
                           _entityToSpawn.setPos(FX + (double)(1 * fdirection.getStepX()) + 0.5, FY + 1.5, FZ + (double)(1 * fdirection.getStepZ()) + 0.5);
                           _entityToSpawn.shoot(Xvector / 2.0, Pitch + 0.5, Zvector / 2.0, 1.0F, 0.0F);
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     } else if (world instanceof ServerLevel projectileLevel) {
                        Projectile _entityToSpawn = (new Object() {
                              public Projectile getArrow(Level level, float damage, int knockback) {
                                 AbstractArrow entityToSpawn = new AimerBeamEntity((EntityType<? extends AimerBeamEntity>)CrustyChunksModEntities.AIMER_BEAM.get(), level) {
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
                        _entityToSpawn.setPos(
                           FX + ((new Object() {
                              public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                 BlockEntity blockEntity = world.getBlockEntity(pos);
                                 return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                              }
                           }).getValue(world, BlockPos.containing(FX, FY, FZ), "Barrels") + 1.0) * (double)fdirection.getStepX() + 0.5,
                           FY + 0.5,
                           FZ + ((new Object() {
                              public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                 BlockEntity blockEntity = world.getBlockEntity(pos);
                                 return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                              }
                           }).getValue(world, BlockPos.containing(FX, FY, FZ), "Barrels") + 1.0) * (double)(new Object() {
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
                           }).getDirection(world.getBlockState(BlockPos.containing(FX, FY, FZ))).getStepZ() + 0.5
                        );
                        _entityToSpawn.shoot(Xvector, Pitch, Zvector, 1.0F, 0.0F);
                        projectileLevel.addFreshEntity(_entityToSpawn);
                     }
                  }
               } else if (world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).getBlock() == CrustyChunksModBlocks.NODE_TRIGGER.get() && (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Trigger") > 0.0 && !world.isClientSide()) {
                  BlockPos _bpxxxxxxx = BlockPos.containing(x + sx, y + sy, z + sz);
                  BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
                  BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
                  if (_blockEntityxxxxxxx != null) {
                     _blockEntityxxxxxxx.getPersistentData().putDouble("Trigger", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Trigger"));
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
                  }
               }

               sz++;
            }

            sy++;
         }

         sx++;
      }

      if (!world.isClientSide()) {
         BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
         BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
         if (_blockEntityxxxxxxxx != null) {
            _blockEntityxxxxxxxx.getPersistentData().putDouble("Updated", 0.0);
         }

         if (world instanceof Level _level) {
            _level.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Trigger") > 0.0 && !world.isClientSide()) {
         BlockPos _bpxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxx);
         BlockState _bsxxxxxxxxx = world.getBlockState(_bpxxxxxxxxx);
         if (_blockEntityxxxxxxxxx != null) {
            _blockEntityxxxxxxxxx.getPersistentData().putDouble("Trigger", 0.0);
         }

         if (world instanceof Level _level) {
            _level.sendBlockUpdated(_bpxxxxxxxxx, _bsxxxxxxxxx, _bsxxxxxxxxx, 3);
         }
      }
   }
}
