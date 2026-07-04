package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FusionHeatTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
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

            for (int index0 = 0; index0 < 200; index0++) {
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
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(150.0)),
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
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(150.0)),
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
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(160.0)),
                              Block.COLLIDER,
                              Fluid.ANY,
                              immediatesourceentity
                           )
                        )
                        .getBlockPos()
                        .getZ()
                  )
               )) {
                  BurnBlockProcedure.execute(
                     world,
                     (double)immediatesourceentity.level()
                        .clip(
                           new ClipContext(
                              immediatesourceentity.getEyePosition(1.0F),
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(150.0)),
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
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(150.0)),
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
                              immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(150.0)),
                              Block.COLLIDER,
                              Fluid.ANY,
                              immediatesourceentity
                           )
                        )
                        .getBlockPos()
                        .getZ()
                  );
               }
            }
         } else {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(500.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiterator instanceof LivingEntity) {
                  immediatesourceentity.lookAt(Anchor.EYES, new Vec3(entityiterator.getX(), entityiterator.getY() + 1.0, entityiterator.getZ()));
                  if (Math.abs(entityiterator.getX() - x)
                           - (
                              Math.abs(
                                    (double)immediatesourceentity.level()
                                          .clip(
                                             new ClipContext(
                                                immediatesourceentity.getEyePosition(1.0F),
                                                immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(512.0)),
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
                     && Math.abs(entityiterator.getZ() - z)
                           - (
                              Math.abs(
                                    (double)immediatesourceentity.level()
                                          .clip(
                                             new ClipContext(
                                                immediatesourceentity.getEyePosition(1.0F),
                                                immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(512.0)),
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
                     entityiterator.igniteForSeconds(40);
                  }
               }
            }

            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FusionHeatTickProcedure.execute", _wtSafe);
      }
   }
}
