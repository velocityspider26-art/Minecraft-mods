package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public class FusionEffectRendererProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         boolean found = false;
         double particleRadius = 0.0;
         double particleAmount = 0.0;
         double sx = 0.0;
         double sy = 0.0;
         double sz = 0.0;
         immediatesourceentity.setNoGravity(true);
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if (0.0 <= immediatesourceentity.getPersistentData().getDouble("T") && 17.0 >= immediatesourceentity.getPersistentData().getDouble("T")) {
            for (int index0 = 0; index0 < 7; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  y - 14.0 + Mth.nextDouble(RandomSource.create(), -80.0, 15.0),
                  z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), 0.0, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
               );
            }

            for (int index1 = 0; index1 < 7; index1++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_STATIC_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -8.0, 8.0),
                  y - 14.0 + Mth.nextDouble(RandomSource.create(), 7.0, 24.0),
                  z + Mth.nextDouble(RandomSource.create(), -8.0, 8.0),
                  Mth.nextDouble(RandomSource.create(), -0.8, 0.8),
                  Mth.nextDouble(RandomSource.create(), -0.7, 0.25),
                  Mth.nextDouble(RandomSource.create(), -0.8, 0.8)
               );
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -19.0, 19.0),
                  y - 14.0 + Mth.nextDouble(RandomSource.create(), 12.0, 35.0),
                  z + Mth.nextDouble(RandomSource.create(), -19.0, 19.0),
                  Mth.nextDouble(RandomSource.create(), -1.7, 1.7),
                  Mth.nextDouble(RandomSource.create(), 0.25, 1.5),
                  Mth.nextDouble(RandomSource.create(), -1.7, 1.7)
               );
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -12.0, 12.0),
                  y - 14.0 + Mth.nextDouble(RandomSource.create(), 14.0, 44.0),
                  z + Mth.nextDouble(RandomSource.create(), -12.0, 12.0),
                  Mth.nextDouble(RandomSource.create(), -1.7, 1.7),
                  Mth.nextDouble(RandomSource.create(), 0.25, 1.5),
                  Mth.nextDouble(RandomSource.create(), -1.7, 1.7)
               );
            }
         }

         if (40.0 == immediatesourceentity.getPersistentData().getDouble("T")) {
            sx = -80.0;

            for (int index2 = 0; index2 < 160; index2++) {
               sz = -80.0;

               for (int index3 = 0; index3 < 160; index3++) {
                  if (1 == Mth.nextInt(RandomSource.create(), 1, 1200) && Math.sqrt(Math.pow(sz, 2.0) + Math.pow(sx, 2.0)) < 200.0) {
                     world.addParticle(
                        (SimpleParticleType)CrustyChunksModParticleTypes.GROUND_HUGE_SMOKE.get(),
                        x + sx,
                        (double)world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z) + Mth.nextDouble(RandomSource.create(), 8.0, 17.0),
                        z + sz,
                        Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                        Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                        Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
                     );
                  }

                  sz++;
               }

               sx++;
            }
         }

         if (700.0 <= immediatesourceentity.getPersistentData().getDouble("T") && 710.0 >= immediatesourceentity.getPersistentData().getDouble("T")) {
            for (int index4 = 0; index4 < 30; index4++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  y - 7.0 + Mth.nextDouble(RandomSource.create(), -335.0, 55.0) + 40.0,
                  z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  Mth.nextDouble(RandomSource.create(), -0.15, 0.15),
                  Mth.nextDouble(RandomSource.create(), 0.0, 0.2),
                  Mth.nextDouble(RandomSource.create(), -0.15, 0.15)
               );
            }

            for (int index5 = 0; index5 < 40; index5++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -35.0, 35.0),
                  y - 7.0 + Mth.nextDouble(RandomSource.create(), 52.0, 80.0) + 40.0,
                  z + Mth.nextDouble(RandomSource.create(), -35.0, 35.0),
                  Mth.nextDouble(RandomSource.create(), -1.2, 1.2),
                  Mth.nextDouble(RandomSource.create(), -0.5, 1.3),
                  Mth.nextDouble(RandomSource.create(), -1.2, 1.2)
               );
            }

            for (int index6 = 0; index6 < 20; index6++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -15.0, 15.0),
                  y - 7.0 + Mth.nextDouble(RandomSource.create(), 60.0, 87.0) + 40.0,
                  z + Mth.nextDouble(RandomSource.create(), -15.0, 15.0),
                  Mth.nextDouble(RandomSource.create(), -1.1, 1.1),
                  Mth.nextDouble(RandomSource.create(), -0.5, 1.3),
                  Mth.nextDouble(RandomSource.create(), -1.1, 1.1)
               );
            }

            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }
      }
   }
}
