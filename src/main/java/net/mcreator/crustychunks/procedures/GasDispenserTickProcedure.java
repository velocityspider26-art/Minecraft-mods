package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.entity.ToxicCloudDetectorEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GasDispenserTickProcedure {
   public static void execute(final LevelAccessor world, double x, double y, double z) {
      try {
      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") <= 0.0) {
         if (null != world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x + 0.5, y, z + 0.5), 5.0, 5.0, 5.0), e -> true).stream().sorted((new Object() {
            Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
               return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
            }
         }).compareDistOf(x + 0.5, y, z + 0.5)).findFirst().orElse(null)) {
            if (!world.isClientSide()) {
               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockEntity _blockEntity = world.getBlockEntity(_bp);
               BlockState _bs = world.getBlockState(_bp);
               if (_blockEntity != null) {
                  _blockEntity.getPersistentData().putDouble("Cooldown", 120.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bp, _bs, _bs, 3);
               }
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     1.0F,
                     false
                  );
               }
            }
         }
      } else if (!world.isClientSide()) {
         BlockPos _bpx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
         BlockState _bsx = world.getBlockState(_bpx);
         if (_blockEntityx != null) {
            _blockEntityx.getPersistentData().putDouble("Cooldown", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") - 1.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") >= 110.0 && world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
               SoundSource.NEUTRAL,
               4.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.6, 1.8)
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
               SoundSource.NEUTRAL,
               4.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.6, 1.8),
               false
            );
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") == 110.0) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new ToxicCloudDetectorEntity((EntityType<? extends ToxicCloudDetectorEntity>)CrustyChunksModEntities.TOXIC_CLOUD_DETECTOR.get(), level) {
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
            _entityToSpawn.setPos(
               x
                  + (double)(new Object() {
                        public Direction getDirection(BlockPos pos) {
                           BlockState _bs = world.getBlockState(pos);
                           Property<?> property = _bs.getBlock().getStateDefinition().getProperty("facing");
                           if (property != null) {
                              Comparable var5 = _bs.getValue(property);
                              if (var5 instanceof Direction) {
                                 return (Direction)var5;
                              }
                           }

                           if (_bs.hasProperty(BlockStateProperties.AXIS)) {
                              return Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.AXIS), AxisDirection.POSITIVE);
                           } else {
                              return _bs.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
                                 ? Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.HORIZONTAL_AXIS), AxisDirection.POSITIVE)
                                 : Direction.NORTH;
                           }
                        }
                     })
                     .getDirection(BlockPos.containing(x, y, z))
                     .getStepX()
                  + 0.5,
               y + 0.5,
               z
                  + (double)(new Object() {
                        public Direction getDirection(BlockPos pos) {
                           BlockState _bs = world.getBlockState(pos);
                           Property<?> property = _bs.getBlock().getStateDefinition().getProperty("facing");
                           if (property != null) {
                              Comparable var5 = _bs.getValue(property);
                              if (var5 instanceof Direction) {
                                 return (Direction)var5;
                              }
                           }

                           if (_bs.hasProperty(BlockStateProperties.AXIS)) {
                              return Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.AXIS), AxisDirection.POSITIVE);
                           } else {
                              return _bs.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
                                 ? Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.HORIZONTAL_AXIS), AxisDirection.POSITIVE)
                                 : Direction.NORTH;
                           }
                        }
                     })
                     .getDirection(BlockPos.containing(x, y, z))
                     .getStepZ()
                  + 0.5
            );
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 1.0F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof ServerLevel _levelxx) {
            _levelxx.sendParticles(
               (SimpleParticleType)CrustyChunksModParticleTypes.GAS_CLOUD.get(),
               x
                  + (double)(new Object() {
                        public Direction getDirection(BlockPos pos) {
                           BlockState _bs = world.getBlockState(pos);
                           Property<?> property = _bs.getBlock().getStateDefinition().getProperty("facing");
                           if (property != null) {
                              Comparable var5 = _bs.getValue(property);
                              if (var5 instanceof Direction) {
                                 return (Direction)var5;
                              }
                           }

                           if (_bs.hasProperty(BlockStateProperties.AXIS)) {
                              return Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.AXIS), AxisDirection.POSITIVE);
                           } else {
                              return _bs.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
                                 ? Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.HORIZONTAL_AXIS), AxisDirection.POSITIVE)
                                 : Direction.NORTH;
                           }
                        }
                     })
                     .getDirection(BlockPos.containing(x, y, z))
                     .getStepX()
                  + 0.5,
               y + 0.5,
               z
                  + (double)(new Object() {
                        public Direction getDirection(BlockPos pos) {
                           BlockState _bs = world.getBlockState(pos);
                           Property<?> property = _bs.getBlock().getStateDefinition().getProperty("facing");
                           if (property != null) {
                              Comparable var5 = _bs.getValue(property);
                              if (var5 instanceof Direction) {
                                 return (Direction)var5;
                              }
                           }

                           if (_bs.hasProperty(BlockStateProperties.AXIS)) {
                              return Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.AXIS), AxisDirection.POSITIVE);
                           } else {
                              return _bs.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
                                 ? Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.HORIZONTAL_AXIS), AxisDirection.POSITIVE)
                                 : Direction.NORTH;
                           }
                        }
                     })
                     .getDirection(BlockPos.containing(x, y, z))
                     .getStepZ()
                  + 0.5,
               25,
               0.0,
               0.0,
               0.0,
               0.25
            );
         }

         if (world instanceof Level _levelxx) {
            if (!_levelxx.isClientSide()) {
               _levelxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.launch")),
                  SoundSource.NEUTRAL,
                  4.0F,
                  1.0F
               );
            } else {
               _levelxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.dispenser.launch")),
                  SoundSource.NEUTRAL,
                  4.0F,
                  1.0F,
                  false
               );
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GasDispenserTickProcedure.execute", _wtSafe);
      }
   }
}
