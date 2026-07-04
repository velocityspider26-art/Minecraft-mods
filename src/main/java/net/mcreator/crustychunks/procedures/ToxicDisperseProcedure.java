package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.ToxicCloudDetectorEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;

public class ToxicDisperseProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (!world.getBlockState(BlockPos.containing(x, y + 1.0, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x, y - 1.0, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x + 1.0, y, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x - 1.0, y, z)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x, y, z + 1.0)).canOcclude()
         || !world.getBlockState(BlockPos.containing(x, y, z - 1.0)).canOcclude()) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
         if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.GAS_CLOUD.get(), x + 0.5, y + 0.5, z + 0.5, 7, 0.0, 0.0, 0.0, 0.2);
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                  SoundSource.BLOCKS,
                  1.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.8)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")),
                  SoundSource.BLOCKS,
                  1.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.8),
                  false
               );
            }
         }

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
               .getArrow(projectileLevel, 0.0F, 0);
            _entityToSpawn.setPos(x + 0.5, y + 0.5, z + 0.5);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.1F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ToxicDisperseProcedure.execute", _wtSafe);
      }
   }
}
