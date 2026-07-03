package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.entity.BlockBusterProjectileEntity;
import net.mcreator.crustychunks.entity.BunkerBusterProjectileEntity;
import net.mcreator.crustychunks.entity.ClusterBombProjectileEntity;
import net.mcreator.crustychunks.entity.ClusterRocketEntity;
import net.mcreator.crustychunks.entity.FireBombProjectileEntity;
import net.mcreator.crustychunks.entity.IRMissileEntity;
import net.mcreator.crustychunks.entity.IncindiaryRocketProjectileEntity;
import net.mcreator.crustychunks.entity.LargeBombProjectileEntity;
import net.mcreator.crustychunks.entity.LargeRadarMissileProjectileEntity;
import net.mcreator.crustychunks.entity.LargeRocketEntity;
import net.mcreator.crustychunks.entity.LargeTorpedoEntity;
import net.mcreator.crustychunks.entity.NuclearBombProjectileEntity;
import net.mcreator.crustychunks.entity.OrdinanceFusionBombProjectileEntity;
import net.mcreator.crustychunks.entity.SuperLargeBombProjectileEntity;
import net.mcreator.crustychunks.entity.TorpedoEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class OrdinanceCorePowerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      BlockState module1 = Blocks.AIR.defaultBlockState();
      BlockState module2 = Blocks.AIR.defaultBlockState();
      BlockState module3 = Blocks.AIR.defaultBlockState();
      BlockState rearmodule1 = Blocks.AIR.defaultBlockState();
      BlockState rearmodule2 = Blocks.AIR.defaultBlockState();
      double RocketVelocity = 0.0;
      double Xvector = 0.0;
      double Zvector = 0.0;
      double Pitch = 0.0;
      double launchoffsetx = 0.0;
      double launchoffsety = 0.0;
      double launchoffsetz = 0.0;
      double dropangle = 0.0;
      Direction blockdirection = Direction.NORTH;
      blockdirection = (new Object() {
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
      }).getDirection(blockstate);
      RocketVelocity = 4.5;
      if (1.0 > Math.abs((double)blockdirection.getStepX() - (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "X")) || 1.0 > Math.abs((double)blockdirection.getStepZ() - (new Object() {
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
         }).getValue(world, BlockPos.containing(x, y, z), "X") + (double)blockdirection.getStepX();
         Zvector = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Z") + (double)blockdirection.getStepZ();
      }

      if (0.0 == Xvector * Zvector) {
         Xvector = (double)blockdirection.getStepX();
         Zvector = (double)blockdirection.getStepZ();
      }

      if ((blockstate.getBlock().getStateDefinition().getProperty("face") instanceof EnumProperty _getep14 ? blockstate.getValue(_getep14).toString() : "").equals("FLOOR")) {
         blockdirection = Direction.UP;
      } else if ((blockstate.getBlock().getStateDefinition().getProperty("face") instanceof EnumProperty _getep16 ? blockstate.getValue(_getep16).toString() : "")
         .equals("CEILING")) {
         blockdirection = Direction.DOWN;
      }

      if (blockdirection == Direction.UP) {
         Pitch = 1.5;
         launchoffsetx = 0.5;
         launchoffsety = 3.0;
         launchoffsetz = 0.5;
      } else if (blockdirection == Direction.DOWN) {
         Pitch = -1.5;
         launchoffsetx = 0.5;
         launchoffsety = -3.0;
         launchoffsetz = 0.5;
      } else {
         launchoffsetx = 0.5;
         launchoffsetz = 0.5;
         launchoffsety = OffsetReturnProcedure.execute(world, x, y, z);
      }

      rearmodule2 = world.getBlockState(
         BlockPos.containing(
            x - (double)(blockdirection.getStepX() * 2), y - (double)(blockdirection.getStepY() * 2), z - (double)(blockdirection.getStepZ() * 2)
         )
      );
      rearmodule1 = world.getBlockState(
         BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ())
      );
      module1 = world.getBlockState(
         BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
      );
      module2 = world.getBlockState(
         BlockPos.containing(
            x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
         )
      );
      module3 = world.getBlockState(
         BlockPos.containing(
            x + (double)(blockdirection.getStepX() * 3), y + (double)(blockdirection.getStepY() * 3), z + (double)(blockdirection.getStepZ() * 3)
         )
      );
      dropangle = 1.0;
      if (rearmodule1.getBlock() == CrustyChunksModBlocks.ORDINANCE_THRUSTER.get()) {
         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_SARH_SEEKER.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockState _bs = ((Block)CrustyChunksModBlocks.ORDINANCE_CONTROLLER.get()).defaultBlockState();
            BlockState _bso = world.getBlockState(_bp);
            java.util.Iterator<Property<?>> entityiterator = _bso.getProperties().iterator();

            while (entityiterator.hasNext()) {

               Property<?> entry_prop = entityiterator.next();

               Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
               if (_property != null && _bs.getValue(_property) != null) {
                  try {
                     _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                  } catch (Exception var38) {
                  }
               }
            }

            BlockEntity _be = world.getBlockEntity(_bp);
            CompoundTag _bnbt = null;
            if (_be != null) {
               _bnbt = _be.saveWithFullMetadata(world.registryAccess());
               _be.setRemoved();
            }

            world.setBlock(_bp, _bs, 3);
            if (_bnbt != null) {
               BlockEntity var153 = world.getBlockEntity(_bp);
               if (var153 != null) {
                  try {
                     var153.loadWithComponents(_bnbt, world.registryAccess());
                  } catch (Exception var37) {
                  }
               }
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new LargeRadarMissileProjectileEntity((EntityType<? extends LargeRadarMissileProjectileEntity>)CrustyChunksModEntities.LARGE_RADAR_MISSILE_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)RocketVelocity, 3.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }

            Vec3 _center = new Vec3(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);

            for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(0.5), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiteratorx instanceof LargeRadarMissileProjectileEntity) {
                  entityiteratorx.getPersistentData().putDouble("LX", x);
                  entityiteratorx.getPersistentData().putDouble("LY", y);
                  entityiteratorx.getPersistentData().putDouble("LZ", z);
               }
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_IR_SEEKER_HEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new IRMissileEntity((EntityType<? extends IRMissileEntity>)CrustyChunksModEntities.IR_MISSILE.get(), level) {
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
               }).getArrow(projectileLevel, 5.0F, 1);
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)RocketVelocity, 3.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INCENDIARY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new IncindiaryRocketProjectileEntity((EntityType<? extends IncindiaryRocketProjectileEntity>)CrustyChunksModEntities.INCINDIARY_ROCKET_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)RocketVelocity, 3.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_CLUSTER_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new ClusterRocketEntity((EntityType<? extends ClusterRocketEntity>)CrustyChunksModEntities.CLUSTER_ROCKET.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)RocketVelocity, 3.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxx) {
               if (!_levelxxxx.isClientSide()) {
                  _levelxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:heavylaunch")),
                     SoundSource.NEUTRAL,
                     15.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new LargeRocketEntity((EntityType<? extends LargeRocketEntity>)CrustyChunksModEntities.LARGE_ROCKET.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, (float)RocketVelocity, 3.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }
      } else if (rearmodule1.getBlock() == CrustyChunksModBlocks.TORPEDO_THRUSTER.get()) {
         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxx) {
               if (!_levelxxxxx.isClientSide()) {
                  _levelxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpod")),
                     SoundSource.NEUTRAL,
                     5.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpod")),
                     SoundSource.NEUTRAL,
                     5.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof Level _levelxxxxxx) {
               if (!_levelxxxxxx.isClientSide()) {
                  _levelxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpodfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpodfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new TorpedoEntity((EntityType<? extends TorpedoEntity>)CrustyChunksModEntities.TORPEDO.get(), level) {
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
               }).getArrow(projectileLevel, 5.0F, 1);
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, 1.0F, 3.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
            && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxx) {
               if (!_levelxxxxxxx.isClientSide()) {
                  _levelxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpod")),
                     SoundSource.NEUTRAL,
                     5.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpod")),
                     SoundSource.NEUTRAL,
                     5.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof Level _levelxxxxxxxx) {
               if (!_levelxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpodfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:peelerpodfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new LargeTorpedoEntity((EntityType<? extends LargeTorpedoEntity>)CrustyChunksModEntities.LARGE_TORPEDO.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(Xvector, Pitch, Zvector, 1.0F, 3.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }
      } else if (rearmodule1.getBlock() == CrustyChunksModBlocks.ORDINANCE_FINS.get()) {
         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxx) {
               if (!_levelxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new LargeBombProjectileEntity((EntityType<? extends LargeBombProjectileEntity>)CrustyChunksModEntities.LARGE_BOMB_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INCENDIARY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxxx) {
               if (!_levelxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new FireBombProjectileEntity((EntityType<? extends FireBombProjectileEntity>)CrustyChunksModEntities.FIRE_BOMB_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_CLUSTER_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new ClusterBombProjectileEntity((EntityType<? extends ClusterBombProjectileEntity>)CrustyChunksModEntities.CLUSTER_BOMB_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
            && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_KINETIC_HEAD.get()) {
            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new BunkerBusterProjectileEntity((EntityType<? extends BunkerBusterProjectileEntity>)CrustyChunksModEntities.BUNKER_BUSTER_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
            && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_KINETIC_HEAD.get()) {
            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new BunkerBusterProjectileEntity((EntityType<? extends BunkerBusterProjectileEntity>)CrustyChunksModEntities.BUNKER_BUSTER_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
            && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new SuperLargeBombProjectileEntity((EntityType<? extends SuperLargeBombProjectileEntity>)CrustyChunksModEntities.SUPER_LARGE_BOMB_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_FISSION_WARHEAD.get()
            && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_FISSION_INITIATOR_HEAD.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     0
                  )
                  .getItem()
               == CrustyChunksModItems.FISSION_CORE.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     1
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     2
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     3
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     4
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     5
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     6
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()) {
            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX(), y, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ()));
            if (_ent != null) {
               int _slotid = 0;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX(), y, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ()));
            if (_ent != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX(), y, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ()));
            if (_ent != null) {
               int _slotid = 2;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(2, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX(), y, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ()));
            if (_ent != null) {
               int _slotid = 3;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(3, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX(), y, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ()));
            if (_ent != null) {
               int _slotid = 4;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(4, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX(), y, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ()));
            if (_ent != null) {
               int _slotid = 5;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(5, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x + (double)(new Object() {
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
            }).getDirection(blockstate).getStepX(), y, z + (double)(new Object() {
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
            }).getDirection(blockstate).getStepZ()));
            if (_ent != null) {
               int _slotid = 6;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(6, _setstack);
                  }
               }
}
            }

            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new NuclearBombProjectileEntity((EntityType<? extends NuclearBombProjectileEntity>)CrustyChunksModEntities.NUCLEAR_BOMB_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_1.get()
            && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_2.get()
            && module3.getBlock() == CrustyChunksModBlocks.ORDINANCE_FISSION_INITIATOR_HEAD.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     0
                  )
                  .getItem()
               == CrustyChunksModItems.FISSION_CORE.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     1
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     2
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     3
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     4
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     5
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()
            && (new Object() {
                     public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                        AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                        BlockEntity _ent = world.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getItemStack(
                     world,
                     BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
                     6
                  )
                  .getItem()
               == CrustyChunksModItems.IMPLOSION_LENS.get()) {
            BlockEntity _entx = world.getBlockEntity(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
            );
            if (_entx != null) {
               int _slotid = 0;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
                  }
               }
}
            }

            _entx = world.getBlockEntity(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
            );
            if (_entx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            _entx = world.getBlockEntity(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
            );
            if (_entx != null) {
               int _slotid = 2;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(2, _setstack);
                  }
               }
}
            }

            _entx = world.getBlockEntity(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
            );
            if (_entx != null) {
               int _slotid = 3;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(3, _setstack);
                  }
               }
}
            }

            _entx = world.getBlockEntity(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
            );
            if (_entx != null) {
               int _slotid = 4;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(4, _setstack);
                  }
               }
}
            }

            _entx = world.getBlockEntity(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
            );
            if (_entx != null) {
               int _slotid = 5;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(5, _setstack);
                  }
               }
}
            }

            _entx = world.getBlockEntity(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ())
            );
            if (_entx != null) {
               int _slotid = 6;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(6, _setstack);
                  }
               }
}
            }

            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 3), y + (double)(blockdirection.getStepY() * 3), z + (double)(blockdirection.getStepZ() * 3)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            if (world instanceof Level _levelxxxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new OrdinanceFusionBombProjectileEntity((EntityType<? extends OrdinanceFusionBombProjectileEntity>)CrustyChunksModEntities.ORDINANCE_FUSION_BOMB_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
            && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
            && module3.getBlock() == CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD.get()) {
            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 3), y + (double)(blockdirection.getStepY() * 3), z + (double)(blockdirection.getStepZ() * 3)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(
                  x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
               ),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(
               BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
               Blocks.AIR.defaultBlockState(),
               3
            );
            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (world instanceof Level _levelxxxxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
                  );
               } else {
                  _levelxxxxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new BlockBusterProjectileEntity((EntityType<? extends BlockBusterProjectileEntity>)CrustyChunksModEntities.BLOCK_BUSTER_PROJECTILE.get(), level) {
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
               _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
               _entityToSpawn.shoot(
                  (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }
      } else if (rearmodule2.getBlock() == CrustyChunksModBlocks.ORDINANCE_FINS.get()
         && rearmodule1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
         && module1.getBlock() == CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD.get()
         && module2.getBlock() == CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD.get()) {
         world.setBlock(
            BlockPos.containing(
               x + (double)(blockdirection.getStepX() * 2), y + (double)(blockdirection.getStepY() * 2), z + (double)(blockdirection.getStepZ() * 2)
            ),
            Blocks.AIR.defaultBlockState(),
            3
         );
         world.setBlock(
            BlockPos.containing(x + (double)blockdirection.getStepX(), y + (double)blockdirection.getStepY(), z + (double)blockdirection.getStepZ()),
            Blocks.AIR.defaultBlockState(),
            3
         );
         world.setBlock(
            BlockPos.containing(x - (double)blockdirection.getStepX(), y - (double)blockdirection.getStepY(), z - (double)blockdirection.getStepZ()),
            Blocks.AIR.defaultBlockState(),
            3
         );
         world.setBlock(
            BlockPos.containing(
               x - (double)(blockdirection.getStepX() * 2), y - (double)(blockdirection.getStepY() * 2), z - (double)(blockdirection.getStepZ() * 2)
            ),
            Blocks.AIR.defaultBlockState(),
            3
         );
         world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
         if (world instanceof Level _levelxxxxxxxxxxxxxxxxxx) {
            if (!_levelxxxxxxxxxxxxxxxxxx.isClientSide()) {
               _levelxxxxxxxxxxxxxxxxxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19)
               );
            } else {
               _levelxxxxxxxxxxxxxxxxxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.17, 0.19),
                  false
               );
            }
         }

         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new BlockBusterProjectileEntity((EntityType<? extends BlockBusterProjectileEntity>)CrustyChunksModEntities.BLOCK_BUSTER_PROJECTILE.get(), level) {
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
            _entityToSpawn.setPos(x + launchoffsetx, y + launchoffsety, z + launchoffsetz);
            _entityToSpawn.shoot(
               (double)blockdirection.getStepX(), (double)blockdirection.getStepY() - dropangle, (double)blockdirection.getStepZ(), 0.2F, 0.0F
            );
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }
   }
}
