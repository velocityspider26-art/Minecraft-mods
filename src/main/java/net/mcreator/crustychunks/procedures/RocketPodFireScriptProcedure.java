package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.RocketEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.SimpleParticleType;
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

public class RocketPodFireScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
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
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") < 1.0
         && (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Ammo") >= 1.0
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
         }).getDirection(blockstate).getStepX() * -1) + 0.5, y + (double)((new Object() {
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
         }).getDirection(blockstate).getStepY() * -1) + 0.5, z + (double)((new Object() {
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
         }).getDirection(blockstate).getStepZ() * -1) + 0.5)).getBlock() == CrustyChunksModBlocks.ROCKET_POD_CHAMBER.get()
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
         }).getDirection(blockstate).getStepX() * -2) + 0.5, y + (double)((new Object() {
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
         }).getDirection(blockstate).getStepY() * -2) + 0.5, z + (double)((new Object() {
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
         }).getDirection(blockstate).getStepZ() * -2) + 0.5)).getBlock() == CrustyChunksModBlocks.ROCKET_POD_CHAMBER.get()) {
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpod")),
                  SoundSource.NEUTRAL,
                  10.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpod")),
                  SoundSource.NEUTRAL,
                  10.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                  false
               );
            }
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpodfar")),
                  SoundSource.NEUTRAL,
                  80.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
               );
            } else {
               _levelx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpodfar")),
                  SoundSource.NEUTRAL,
                  80.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                  false
               );
            }
         }

         if (world instanceof ServerLevel _levelxx) {
            _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.PUFF.get(), x + (double)((new Object() {
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
            }).getDirection(blockstate).getStepX() * -4) + 0.5, y + (double)((new Object() {
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
            }).getDirection(blockstate).getStepY() * -4) + 0.5, z + (double)((new Object() {
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
            }).getDirection(blockstate).getStepZ() * -4) + 0.5, 20, 0.1, 0.1, 0.1, 0.5);
         }

         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                  AbstractArrow entityToSpawn = new RocketEntity((EntityType<? extends RocketEntity>)CrustyChunksModEntities.ROCKET.get(), level) {
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
            }).getArrow(projectileLevel, 10.0F, 2, (byte)10);
            _entityToSpawn.setPos(x + (double)((new Object() {
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
            }).getDirection(blockstate).getStepZ() * 2) + 0.5);
            _entityToSpawn.shoot(Xvector, Pitch, Zvector, 6.0F, 2.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putDouble("Ammo", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") - 1.0);
            }

            if (world instanceof Level _levelxx) {
               _levelxx.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
            BlockState _bsx = world.getBlockState(_bpx);
            if (_blockEntityx != null) {
               _blockEntityx.getPersistentData().putDouble("Cooldown", 5.0);
            }

            if (world instanceof Level _levelxx) {
               _levelxx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RocketPodFireScriptProcedure.execute", _wtSafe);
      }
   }
}
