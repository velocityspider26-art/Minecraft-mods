package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.entity.SmallBombProjectileEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public class SmallBombRedstoneOnProcedure {
   public static void execute(final LevelAccessor world, double x, double y, double z) {
      try {
      double launchoffsety = 0.0;
      launchoffsety = OffsetReturnProcedure.execute(world, x, y, z);
      if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback) {
                  AbstractArrow entityToSpawn = new SmallBombProjectileEntity((EntityType<? extends SmallBombProjectileEntity>)CrustyChunksModEntities.SMALL_BOMB_PROJECTILE.get(), level) {
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
         _entityToSpawn.setPos(x + 0.5, y + launchoffsety, z + 0.5);
         _entityToSpawn.shoot(
            (double)(new Object() {
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
               .getStepX(),
            (double)(new Object() {
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
               .getStepY(),
            (double)(new Object() {
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
               .getStepZ(),
            0.2F,
            0.0F
         );
         projectileLevel.addFreshEntity(_entityToSpawn);
      }

      world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmallBombRedstoneOnProcedure.execute", _wtSafe);
      }
   }
}
