package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FlakShellTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         boolean trigger = false;
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) >= 2.0 && !immediatesourceentity.isNoGravity()) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y() + 0.01, immediatesourceentity.getDeltaMovement().z()
               )
            );
         }

         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.BULLET_TRAIL.get(),
            immediatesourceentity.getX(),
            immediatesourceentity.getY(),
            immediatesourceentity.getZ(),
            0.0,
            0.0,
            0.0
         );
         if (immediatesourceentity.getPersistentData().getDouble("T") > 60.0) {
            trigger = true;
         }

         if (0.0 < immediatesourceentity.getPersistentData().getDouble("Range")
            && immediatesourceentity.getPersistentData().getDouble("T")
               >= immediatesourceentity.getPersistentData().getDouble("Range")
                  / (immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0)) {
            trigger = true;
         }

         if (immediatesourceentity.isUnderWater()) {
            trigger = true;
         }

         Vec3 _center = new Vec3(x, y, z);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(6.0), e -> true)
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
               () -> {
                  SmallShellHitProcedure.execute(
                     world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), immediatesourceentity
                  );
                  if (!immediatesourceentity.level().isClientSide()) {
                     immediatesourceentity.discard();
                  }
               }
            );
         }
      }
   }
}
