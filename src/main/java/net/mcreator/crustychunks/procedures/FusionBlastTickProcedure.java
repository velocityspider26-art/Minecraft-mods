package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public class FusionBlastTickProcedure {
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
         if (1.0 == immediatesourceentity.getPersistentData().getDouble("T")) {
            FusionEffectsProcedure.execute(world, x, immediatesourceentity.getY() - 15.0, z);
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(500.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               CrustyChunksMod.queueServerWork(
                  (int)Math.abs(Math.round(Math.sqrt(Math.pow(x - entityiterator.getX(), 2.0) + Math.pow(z - entityiterator.getZ(), 2.0)) * 0.5)),
                  () -> {
                     immediatesourceentity.lookAt(Anchor.EYES, new Vec3(entityiterator.getX(), entityiterator.getY() + 1.0, entityiterator.getZ()));
                     if (Math.abs(entityiterator.getY() + 0.5 - y)
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
                                             .getY()
                                          - y
                                    )
                                    + 0.5
                              )
                           <= 0.0
                        && Math.abs(entityiterator.getX() - x)
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
                        && Math.abs(entityiterator.getZ() - z)
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
                        entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.EXPLOSION)), 7.0F);
                        entityiterator.setDeltaMovement(
                           new Vec3(
                              immediatesourceentity.getLookAngle().x * 2.0 + entityiterator.getDeltaMovement().x(),
                              immediatesourceentity.getLookAngle().y * 2.0 + entityiterator.getDeltaMovement().y(),
                              immediatesourceentity.getLookAngle().z * 2.0 + entityiterator.getDeltaMovement().z()
                           )
                        );
                     }
                  }
               );
            }
         }

         immediatesourceentity.setNoGravity(true);
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         xRadius = 1.0 + immediatesourceentity.getPersistentData().getDouble("T");
         zRadius = 1.0 + immediatesourceentity.getPersistentData().getDouble("T");

         for (double var31 = (double)(1L + Math.round(xRadius * 0.9)); loop < var31; loop++) {
            if (immediatesourceentity.getPersistentData().getDouble("T") / 2.0
               == (double)Math.round(immediatesourceentity.getPersistentData().getDouble("T") / 2.0)) {
               if (1 == Mth.nextInt(RandomSource.create(), 1, 5)) {
                  if (y + 10.0
                        > (double)world.getHeight(
                           Types.MOTION_BLOCKING_NO_LEAVES,
                           (int)(x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius),
                           (int)(z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius)
                        )
                     && !ModList.get().isLoaded("explosionoverhaul")
                     && world instanceof Level) {
                     Level _level = (Level)world;
                     if (!_level.isClientSide()) {
                        _level.explode(
                           null,
                           x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius,
                           (double)(
                              world.getHeight(
                                    Types.MOTION_BLOCKING_NO_LEAVES,
                                    (int)(x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius),
                                    (int)(z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius)
                                 )
                                 + 9
                           ),
                           z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius,
                           6.0F,
                           ExplosionInteraction.BLOCK
                        );
                     }
                  }

                  if (5.0 <= immediatesourceentity.getPersistentData().getDouble("T")
                     && !ModList.get().isLoaded("explosionoverhaul")
                     && y + 5.0
                        > (double)world.getHeight(
                           Types.MOTION_BLOCKING_NO_LEAVES,
                           (int)(x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius),
                           (int)(z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius)
                        )
                     && world instanceof Level) {
                     Level _level = (Level)world;
                     if (!_level.isClientSide()) {
                        _level.explode(
                           null,
                           x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius,
                           Math.max(
                              (double)(
                                 world.getHeight(
                                       Types.MOTION_BLOCKING_NO_LEAVES,
                                       (int)(x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius),
                                       (int)(z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius)
                                    )
                                    + 22
                              ),
                              y + 8.0
                           ),
                           z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius,
                           (float)(16.0 / Math.ceil(xRadius / 75.0) + 4.0),
                           ExplosionInteraction.BLOCK
                        );
                     }
                  }

                  if (1 == Mth.nextInt(RandomSource.create(), 1, 2) && 550.0 >= immediatesourceentity.getPersistentData().getDouble("T")) {
                     Groundatspot = (double)world.getHeight(
                        Types.MOTION_BLOCKING_NO_LEAVES,
                        (int)(x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius),
                        (int)(z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius)
                     );
                     if (5.0 <= immediatesourceentity.getPersistentData().getDouble("T")) {
                        world.addParticle(
                           (SimpleParticleType)CrustyChunksModParticleTypes.DUST_WAVE.get(),
                           x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius,
                           (double)(
                              world.getHeight(
                                    Types.MOTION_BLOCKING,
                                    Mth.floor(x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius),
                                    Mth.floor(z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius)
                                 )
                                 + 4
                           ),
                           z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius,
                           Math.cos((Math.PI * 2) / var31 * loop),
                           0.0,
                           Math.sin((Math.PI * 2) / var31 * loop)
                        );
                        world.addParticle(
                           (SimpleParticleType)CrustyChunksModParticleTypes.NUCLEAR_SHOCK_PARTICLE.get(),
                           x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * xRadius,
                           y + Mth.nextDouble(RandomSource.create(), 25.0, 30.0),
                           z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * zRadius,
                           Math.cos((Math.PI * 2) / var31 * loop),
                           0.0,
                           Math.sin((Math.PI * 2) / var31 * loop)
                        );
                        if (30.0 <= immediatesourceentity.getPersistentData().getDouble("T")) {
                           world.addParticle(
                              (SimpleParticleType)CrustyChunksModParticleTypes.NUCLEAR_SHOCK_PARTICLE.get(),
                              x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * (xRadius - 30.0),
                              y + Mth.nextDouble(RandomSource.create(), 75.0, 80.0),
                              z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * (zRadius - 30.0),
                              Math.cos((Math.PI * 2) / var31 * loop),
                              0.0,
                              Math.sin((Math.PI * 2) / var31 * loop)
                           );
                        }
                     }
                  }
               }

               if (Mth.nextInt(RandomSource.create(), 1, 10) == 1
                  && 130.0 > immediatesourceentity.getPersistentData().getDouble("T")
                  && 5.0 <= immediatesourceentity.getPersistentData().getDouble("T")) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.GROUND_HUGE_SMOKE.get(),
                     x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * (xRadius - 20.0),
                     (double)(
                        world.getHeight(
                              Types.MOTION_BLOCKING,
                              Mth.floor(x + 0.5 + Math.cos((Math.PI * 2) / var31 * loop) * (xRadius - 20.0)),
                              Mth.floor(z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * (zRadius - 20.0))
                           )
                           + 8
                     ),
                     z + 0.5 + Math.sin((Math.PI * 2) / var31 * loop) * (zRadius - 20.0),
                     0.0,
                     0.25,
                     0.0
                  );
               }
            }
         }

         immediatesourceentity.getPersistentData().putDouble("Iterations", immediatesourceentity.getPersistentData().getDouble("Iterations") + 1.0);
         if (450.0 <= immediatesourceentity.getPersistentData().getDouble("T") && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   }
}
