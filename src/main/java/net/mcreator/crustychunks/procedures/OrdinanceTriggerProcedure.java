package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OrdinanceTriggerProcedure {
   public static boolean execute(LevelAccessor world, Entity immediatesourceentity) {
      if (immediatesourceentity == null) {
         return false;
      } else {
         boolean Trigger = false;
         double ypos = 0.0;
         double zpos = 0.0;
         double xpos = 0.0;
         double zvel = 0.0;
         double yvel = 0.0;
         double xvel = 0.0;
         double speed = 0.0;
         if (immediatesourceentity.isUnderWater()) {
            Trigger = true;
         }

         xpos = immediatesourceentity.getX();
         ypos = immediatesourceentity.getY();
         zpos = immediatesourceentity.getZ();
         xvel = immediatesourceentity.getDeltaMovement().x();
         yvel = immediatesourceentity.getDeltaMovement().y();
         zvel = immediatesourceentity.getDeltaMovement().z();
         speed = Math.sqrt(Math.pow(xvel, 2.0) + Math.pow(yvel, 2.0) + Math.pow(zvel, 2.0));
         Vec3 _center = new Vec3(xpos - xvel * 0.5, ypos - yvel * 0.5, zpos - zvel * 0.5);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.25), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
               && entityiterator != immediatesourceentity) {
               if (!entityiterator.level().isClientSide()) {
                  entityiterator.discard();
               }

               Trigger = true;
            }
         }

         Vec3 _centerx = new Vec3(xpos, ypos, zpos);

         for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_centerx, _centerx).inflate(1.25), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_centerx)))
            .toList()) {
            if (entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
               && entityiteratorx != immediatesourceentity) {
               if (!entityiteratorx.level().isClientSide()) {
                  entityiteratorx.discard();
               }

               Trigger = true;
            }
         }

         Vec3 _centerxx = new Vec3(xpos + xvel * 0.5, ypos + yvel * 0.5, zpos + zvel * 0.5);

         for (Entity entityiteratorxx : world.getEntitiesOfClass(Entity.class, new AABB(_centerxx, _centerxx).inflate(1.25), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_centerxx)))
            .toList()) {
            if (entityiteratorxx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
               && entityiteratorxx != immediatesourceentity) {
               if (!entityiteratorxx.level().isClientSide()) {
                  entityiteratorxx.discard();
               }

               Trigger = true;
            }
         }

         return Trigger;
      }
   }
}
