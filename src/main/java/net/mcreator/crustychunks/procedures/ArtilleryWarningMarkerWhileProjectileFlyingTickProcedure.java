package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArtilleryWarningMarkerWhileProjectileFlyingTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         double targetrange = 0.0;
         immediatesourceentity.setNoGravity(true);
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         immediatesourceentity.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
         Vec3 _center = new Vec3(x, y, z);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(25.0), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))) {
               targetrange = Math.sqrt(Math.pow(Math.abs(z - entityiterator.getZ()), 2.0) + Math.pow(Math.abs(x - entityiterator.getX()), 2.0));
               if (entityiterator instanceof Mob _entity) {
                  _entity.getNavigation()
                     .moveTo(x + (entityiterator.getZ() - z) * 30.0 / targetrange, y, z + (entityiterator.getX() - x) * 30.0 / targetrange, 1.0);
               }
            }
         }

         if (200.0 < immediatesourceentity.getPersistentData().getDouble("T") && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ArtilleryWarningMarkerWhileProjectileFlyingTickProcedure.execute", _wtSafe);
      }
   }
}
