package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public class NuclearSecondEffectRenderProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
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
         if (0.0 <= immediatesourceentity.getPersistentData().getDouble("T") && 20.0 >= immediatesourceentity.getPersistentData().getDouble("T")) {
            for (int index0 = 0; index0 < 5; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  y - 7.0 + Mth.nextDouble(RandomSource.create(), -40.0, 15.0),
                  z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), 0.0, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
               );
            }

            for (int index1 = 0; index1 < 7; index1++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_STATIC_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -6.0, 6.0),
                  y - 7.0 + Mth.nextDouble(RandomSource.create(), 7.0, 15.0),
                  z + Mth.nextDouble(RandomSource.create(), -6.0, 6.0),
                  Mth.nextDouble(RandomSource.create(), -0.6, 0.6),
                  Mth.nextDouble(RandomSource.create(), -0.7, 0.25),
                  Mth.nextDouble(RandomSource.create(), -0.6, 0.6)
               );
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -11.0, 11.0),
                  y - 7.0 + Mth.nextDouble(RandomSource.create(), 9.0, 25.0),
                  z + Mth.nextDouble(RandomSource.create(), -11.0, 11.0),
                  Mth.nextDouble(RandomSource.create(), -1.25, 1.25),
                  Mth.nextDouble(RandomSource.create(), 0.25, 1.25),
                  Mth.nextDouble(RandomSource.create(), -1.25, 1.25)
               );
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_FIREBALL.get(),
                  x + Mth.nextDouble(RandomSource.create(), -9.0, 9.0),
                  y - 7.0 + Mth.nextDouble(RandomSource.create(), 9.0, 30.0),
                  z + Mth.nextDouble(RandomSource.create(), -10.0, 10.0),
                  Mth.nextDouble(RandomSource.create(), -1.25, 1.25),
                  Mth.nextDouble(RandomSource.create(), 0.25, 1.25),
                  Mth.nextDouble(RandomSource.create(), -1.25, 1.25)
               );
            }
         }

         if (40.0 == immediatesourceentity.getPersistentData().getDouble("T")) {
            sx = -80.0;

            for (int index2 = 0; index2 < 160; index2++) {
               sz = -80.0;

               for (int index3 = 0; index3 < 160; index3++) {
                  if (1 == Mth.nextInt(RandomSource.create(), 1, 1600) && Math.sqrt(Math.pow(sz, 2.0) + Math.pow(sx, 2.0)) < 80.0) {
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

         if (400.0 <= immediatesourceentity.getPersistentData().getDouble("T") && 410.0 >= immediatesourceentity.getPersistentData().getDouble("T")) {
            for (int index4 = 0; index4 < 25; index4++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  y + Mth.nextDouble(RandomSource.create(), -80.0, 55.0) - 1.0,
                  z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), 0.0, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
               );
            }

            for (int index5 = 0; index5 < 35; index5++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -15.0, 15.0),
                  y + Mth.nextDouble(RandomSource.create(), 55.0, 65.0) - 1.0,
                  z + Mth.nextDouble(RandomSource.create(), -15.0, 15.0),
                  Mth.nextDouble(RandomSource.create(), -0.6, 0.6),
                  Mth.nextDouble(RandomSource.create(), -0.5, 0.8),
                  Mth.nextDouble(RandomSource.create(), -0.6, 0.6)
               );
            }

            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("NuclearSecondEffectRenderProcedure.execute", _wtSafe);
      }
   }
}
