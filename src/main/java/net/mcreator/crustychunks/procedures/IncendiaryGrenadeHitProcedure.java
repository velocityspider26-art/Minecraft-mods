package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.entity.IncendiaryGrenadeProjectileEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class IncendiaryGrenadeHitProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (world instanceof ServerLevel _level) {
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.INCENDIARY_GRENADE_PROJECTILE.get())
               .spawn(
                  _level,
                  BlockPos.containing(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ()),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(
                  immediatesourceentity.getDeltaMovement().x() * -0.1,
                  immediatesourceentity.getDeltaMovement().y() * -0.1,
                  immediatesourceentity.getDeltaMovement().z() * -0.1
               );
            }
         }

         Vec3 _center = new Vec3(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(0.05), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator instanceof IncendiaryGrenadeProjectileEntity && entityiterator != immediatesourceentity) {
               entityiterator.getPersistentData().putDouble("t", immediatesourceentity.getPersistentData().getDouble("t"));
            }
         }

         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("IncendiaryGrenadeHitProcedure.execute", _wtSafe);
      }
   }
}
