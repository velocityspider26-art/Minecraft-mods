package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AIPodTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean Trigger = false;
         if (!immediatesourceentity.isUnderWater()
            && immediatesourceentity.getDeltaMovement().y() <= -0.4
            && !immediatesourceentity.isNoGravity()
            && !world.canSeeSkyFromBelowWater(BlockPos.containing(x, y - 90.0, z))) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x() * 0.95,
                  immediatesourceentity.getDeltaMovement().y() * 0.95,
                  immediatesourceentity.getDeltaMovement().z() * 0.95
               )
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x() * 2.2 + 0.2,
               immediatesourceentity.getDeltaMovement().y() * 2.2,
               immediatesourceentity.getDeltaMovement().z() * 2.2 + 0.2
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x() * 2.2 - 0.2,
               immediatesourceentity.getDeltaMovement().y() * 2.2,
               immediatesourceentity.getDeltaMovement().z() * 2.2 + 0.2
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x() * 2.2 + 0.2,
               immediatesourceentity.getDeltaMovement().y() * 2.2,
               immediatesourceentity.getDeltaMovement().z() * 2.2 - 0.2
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x() * 2.2 - 0.2,
               immediatesourceentity.getDeltaMovement().y() * 2.2,
               immediatesourceentity.getDeltaMovement().z() * 2.2 - 0.2
            );
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
                     false
                  );
               }
            }
         }

         Vec3 _center = new Vec3(x, y, z);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(0.75), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))) {
               if (!entityiterator.level().isClientSide()) {
                  entityiterator.discard();
               }

               Trigger = true;
            }
         }

         if (Trigger) {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }

            CrustyChunksMod.queueServerWork(
               1,
               () -> ExplosionExampleProcedure.execute(
                     world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), 2.0
                  )
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AIPodTickProcedure.execute", _wtSafe);
      }
   }
}
