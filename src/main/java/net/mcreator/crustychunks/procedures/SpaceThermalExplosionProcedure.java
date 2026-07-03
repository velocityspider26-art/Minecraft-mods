package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SpaceThermalExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         boolean found = false;
         double particleRadius = 0.0;
         double particleAmount = 0.0;
         double sx = 0.0;
         double sy = 0.0;
         double sz = 0.0;
         double xRadius = 0.0;
         double loop = 0.0;
         double zRadius = 0.0;
         double Groundatspot = 0.0;
         immediatesourceentity.setNoGravity(true);
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
         if (immediatesourceentity.getPersistentData().getDouble("T") <= 20.0) {
            immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);

            for (int index0 = 0; index0 < 256; index0++) {
               immediatesourceentity.setYRot((float)Mth.nextDouble(RandomSource.create(), -360.0, 360.0));
               immediatesourceentity.setXRot((float)Mth.nextDouble(RandomSource.create(), -360.0, 360.0));
               immediatesourceentity.setYBodyRot(immediatesourceentity.getYRot());
               immediatesourceentity.setYHeadRot(immediatesourceentity.getYRot());
               immediatesourceentity.yRotO = immediatesourceentity.getYRot();
               immediatesourceentity.xRotO = immediatesourceentity.getXRot();
               if (immediatesourceentity instanceof LivingEntity _entity) {
                  _entity.yBodyRotO = _entity.getYRot();
                  _entity.yHeadRotO = _entity.getYRot();
               }

               if (!world.isEmptyBlock(
                     new BlockPos(
                        immediatesourceentity.level()
                           .clip(
                              new ClipContext(
                                 immediatesourceentity.getEyePosition(1.0F),
                                 immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(200.0)),
                                 Block.COLLIDER,
                                 Fluid.ANY,
                                 immediatesourceentity
                              )
                           )
                           .getBlockPos()
                           .getX(),
                        immediatesourceentity.level()
                           .clip(
                              new ClipContext(
                                 immediatesourceentity.getEyePosition(1.0F),
                                 immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(200.0)),
                                 Block.COLLIDER,
                                 Fluid.ANY,
                                 immediatesourceentity
                              )
                           )
                           .getBlockPos()
                           .getY(),
                        immediatesourceentity.level()
                           .clip(
                              new ClipContext(
                                 immediatesourceentity.getEyePosition(1.0F),
                                 immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(200.0)),
                                 Block.COLLIDER,
                                 Fluid.ANY,
                                 immediatesourceentity
                              )
                           )
                           .getBlockPos()
                           .getZ()
                     )
                  )
                  && world instanceof Level _level
                  && !_level.isClientSide()) {
                  _level.explode(
                     null,
                     (double)immediatesourceentity.level()
                        .clip(
                           new ClipContext(
                              immediatesourceentity.getEyePosition(1.0F),
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(200.0)),
                              Block.COLLIDER,
                              Fluid.ANY,
                              immediatesourceentity
                           )
                        )
                        .getBlockPos()
                        .getX(),
                     (double)immediatesourceentity.level()
                        .clip(
                           new ClipContext(
                              immediatesourceentity.getEyePosition(1.0F),
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(200.0)),
                              Block.COLLIDER,
                              Fluid.ANY,
                              immediatesourceentity
                           )
                        )
                        .getBlockPos()
                        .getY(),
                     (double)immediatesourceentity.level()
                        .clip(
                           new ClipContext(
                              immediatesourceentity.getEyePosition(1.0F),
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(200.0)),
                              Block.COLLIDER,
                              Fluid.ANY,
                              immediatesourceentity
                           )
                        )
                        .getBlockPos()
                        .getZ(),
                     5.0F,
                     ExplosionInteraction.BLOCK
                  );
               }

               if (1 == Mth.nextInt(RandomSource.create(), 1, 3)) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.SPACE_FIREBALL.get(),
                     x,
                     y,
                     z,
                     immediatesourceentity.getLookAngle().x * Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                     immediatesourceentity.getLookAngle().y * Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                     immediatesourceentity.getLookAngle().z * Mth.nextDouble(RandomSource.create(), -1.0, 1.0)
                  );
               }
            }

            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(40.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiterator != immediatesourceentity
                  && entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:projectile")))
                  && !entityiterator.level().isClientSide()) {
                  entityiterator.discard();
               }
            }
         } else if (21.0 == immediatesourceentity.getPersistentData().getDouble("T")) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(250.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiteratorx instanceof LivingEntity) {
                  immediatesourceentity.lookAt(Anchor.EYES, new Vec3(entityiteratorx.getX(), entityiteratorx.getY() + 1.0, entityiteratorx.getZ()));
                  if (Math.abs(entityiteratorx.getX() - x)
                           - (
                              Math.abs(
                                    (double)immediatesourceentity.level()
                                          .clip(
                                             new ClipContext(
                                                immediatesourceentity.getEyePosition(1.0F),
                                                immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(250.0)),
                                                Block.COLLIDER,
                                                Fluid.ANY,
                                                immediatesourceentity
                                             )
                                          )
                                          .getBlockPos()
                                          .getX()
                                       - x
                                 )
                                 + 0.5
                           )
                        <= 0.0
                     && Math.abs(entityiteratorx.getZ() - z)
                           - (
                              Math.abs(
                                    (double)immediatesourceentity.level()
                                          .clip(
                                             new ClipContext(
                                                immediatesourceentity.getEyePosition(1.0F),
                                                immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(250.0)),
                                                Block.COLLIDER,
                                                Fluid.ANY,
                                                immediatesourceentity
                                             )
                                          )
                                          .getBlockPos()
                                          .getZ()
                                       - z
                                 )
                                 + 0.5
                           )
                        <= 0.0) {
                     entityiteratorx.igniteForSeconds(40);
                  }
               }
            }

            immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         } else if (immediatesourceentity.getPersistentData().getDouble("T") <= 250.0) {
            immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         } else if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   }
}
