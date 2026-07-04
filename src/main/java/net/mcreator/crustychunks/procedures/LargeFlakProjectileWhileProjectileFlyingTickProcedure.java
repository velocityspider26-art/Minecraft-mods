package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LargeFlakProjectileWhileProjectileFlyingTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean trigger = false;
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.LARGE_BULLET_TRAIL.get(),
            x,
            y,
            z,
            immediatesourceentity.getDeltaMovement().x() * 0.1,
            immediatesourceentity.getDeltaMovement().y() * 0.1,
            immediatesourceentity.getDeltaMovement().z() * 0.1
         );
         if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() instanceof LiquidBlock) {
            trigger = true;
         }

         if (immediatesourceentity.getPersistentData().getDouble("T") > 60.0) {
            trigger = true;
         }

         if (0.0 < immediatesourceentity.getPersistentData().getDouble("Range")
            && immediatesourceentity.getPersistentData().getDouble("T")
               >= immediatesourceentity.getPersistentData().getDouble("Range")
                  / (immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0)) {
            trigger = true;
         }

         Vec3 _center = new Vec3(x, y, z);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10.0), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (immediatesourceentity.getPersistentData().getDouble("T") >= 7.0
               && (
                  entityiterator instanceof LivingEntity
                     || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:warm")))
               )) {
               trigger = true;
               if ((entityiterator instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) > 0.0 && !entityiterator.level().isClientSide()) {
                  entityiterator.discard();
               }
            }
         }

         if (trigger) {
            trigger = false;
            CrustyChunksMod.queueServerWork(
               1,
               () -> TankFireProjectileHitsBlockProcedure.execute(
                     world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), immediatesourceentity
                  )
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LargeFlakProjectileWhileProjectileFlyingTickProcedure.execute", _wtSafe);
      }
   }
}
