package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.ChaffEntity;
import net.mcreator.crustychunks.entity.VehicleFlareProjectileEntity;
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

public class CountermeasureDispenserFireProcedure {
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
      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") <= 0.0) {
         if ((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Ammo") >= 1.0) {
            if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Type") == 1.0) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.large_blast")),
                        SoundSource.BLOCKS,
                        25.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.large_blast")),
                        SoundSource.BLOCKS,
                        25.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                        SoundSource.BLOCKS,
                        15.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                        SoundSource.BLOCKS,
                        15.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.launch")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.4, 0.6)
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.launch")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.4, 0.6),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.dispense")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.dispense")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelxxxx) {
                  _levelxxxx.sendParticles(ParticleTypes.FLAME, x + 0.5 + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepX(), y + 0.5 + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY(), z + 0.5 + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ(), 20, 0.0, 0.0, 0.0, 0.05);
               }

               if (world instanceof ServerLevel projectileLevel) {
                  Projectile _entityToSpawn = (new Object() {
                        public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                           AbstractArrow entityToSpawn = new VehicleFlareProjectileEntity((EntityType<? extends VehicleFlareProjectileEntity>)CrustyChunksModEntities.VEHICLE_FLARE_PROJECTILE.get(), level) {
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
                  _entityToSpawn.setPos(x + 0.5 + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepX() * 1.5, y + 0.5 + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY() * 1.5, z + 0.5 + (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ() * 1.5);
                  _entityToSpawn.shoot((double)(new Object() {
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
                  }).getDirection(blockstate).getStepX(), (double)(new Object() {
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
                  }).getDirection(blockstate).getStepY(), (double)(new Object() {
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
                  }).getDirection(blockstate).getStepZ(), 2.0F, 11.0F);
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

                  if (world instanceof Level _levelxxxx) {
                     _levelxxxx.sendBlockUpdated(_bp, _bs, _bs, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                  BlockState _bsx = world.getBlockState(_bpx);
                  if (_blockEntityx != null) {
                     _blockEntityx.getPersistentData().putDouble("Cooldown", 4.0);
                  }

                  if (world instanceof Level _levelxxxx) {
                     _levelxxxx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }
            } else if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Type") == 2.0) {
               if (world instanceof Level _levelxxxx) {
                  if (!_levelxxxx.isClientSide()) {
                     _levelxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.large_blast")),
                        SoundSource.BLOCKS,
                        15.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8)
                     );
                  } else {
                     _levelxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.large_blast")),
                        SoundSource.BLOCKS,
                        15.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxxxx) {
                  if (!_levelxxxxx.isClientSide()) {
                     _levelxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                        SoundSource.BLOCKS,
                        15.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                     );
                  } else {
                     _levelxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                        SoundSource.BLOCKS,
                        15.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxxxxx) {
                  if (!_levelxxxxxx.isClientSide()) {
                     _levelxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.twinkle")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.4, 0.6)
                     );
                  } else {
                     _levelxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.twinkle")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.4, 0.6),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxxxxxx) {
                  if (!_levelxxxxxxx.isClientSide()) {
                     _levelxxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.twinkle_far")),
                        SoundSource.BLOCKS,
                        25.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.4, 0.6)
                     );
                  } else {
                     _levelxxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.twinkle_far")),
                        SoundSource.BLOCKS,
                        25.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.4, 0.6),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxxxxxxx) {
                  if (!_levelxxxxxxxx.isClientSide()) {
                     _levelxxxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.dispense")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
                     );
                  } else {
                     _levelxxxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.dispense")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
                        false
                     );
                  }
               }

               for (int index0 = 0; index0 < 6; index0++) {
                  if (world instanceof ServerLevel projectileLevel) {
                     Projectile _entityToSpawn = (new Object() {
                        public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                           AbstractArrow entityToSpawn = new ChaffEntity((EntityType<? extends ChaffEntity>)CrustyChunksModEntities.CHAFF.get(), level) {
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
                     }).getArrow(projectileLevel, 10.0F, 2, (byte)10);
                     _entityToSpawn.setPos(x + 0.5 + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepX() * 1.5, y + 0.5 + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY() * 1.5, z + 0.5 + (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ() * 1.5);
                     _entityToSpawn.shoot((double)(new Object() {
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
                     }).getDirection(blockstate).getStepX(), (double)(new Object() {
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
                     }).getDirection(blockstate).getStepY(), (double)(new Object() {
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
                     }).getDirection(blockstate).getStepZ(), 1.0F, 15.0F);
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                  BlockState _bsxx = world.getBlockState(_bpxx);
                  if (_blockEntityxx != null) {
                     _blockEntityxx.getPersistentData().putDouble("Ammo", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") - 1.0);
                  }

                  if (world instanceof Level _levelxxxxxxxxx) {
                     _levelxxxxxxxxx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                  BlockState _bsxxx = world.getBlockState(_bpxxx);
                  if (_blockEntityxxx != null) {
                     _blockEntityxxx.getPersistentData().putDouble("Cooldown", 4.0);
                  }

                  if (world instanceof Level _levelxxxxxxxxx) {
                     _levelxxxxxxxxx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                  }
               }
            }
         } else if (!world.isClientSide()) {
            BlockPos _bpxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
            BlockState _bsxxxx = world.getBlockState(_bpxxxx);
            if (_blockEntityxxxx != null) {
               _blockEntityxxxx.getPersistentData().putDouble("Type", 0.0);
            }

            if (world instanceof Level _levelxxxxxxxxx) {
               _levelxxxxxxxxx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("CountermeasureDispenserFireProcedure.execute", _wtSafe);
      }
   }
}
