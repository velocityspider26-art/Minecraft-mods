package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class NuclearEffectProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double power, double time) {
      final double _wq = net.mcreator.crustychunks.compat.WariumNukeEffects.clientQualityMultiplier();
      boolean found = false;
      boolean done = false;
      double particleRadius = 0.0;
      double particleAmount = 0.0;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double eventtick = 0.0;
      double explosionpower = 0.0;
      double xRadius = 0.0;
      double loop = 0.0;
      double Groundatspot = 0.0;
      double zRadius = 0.0;
      eventtick = time;
      explosionpower = power / 60.0;
      if (SpaceLogicProcedure.execute(world, x, y, z)) {
         if (0.0 <= time && 20.0 >= time) {
            for (int index0 = 0; index0 < (int)Math.max(8, 64 * _wq); index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.SPACE_FIREBALL.get(),
                  x,
                  y,
                  z,
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0)
               );
            }
         }
      } else {
         if (time == 1.0) {
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.NUCLEAR_SHOCK_RING.get(), x, y, z, 0.0, 0.0, 0.0);
         }

         if (0.0 <= time && 20.0 >= time) {
            if (1.0 < explosionpower) {
               for (int index1 = 0; index1 < (int)(5.0 * explosionpower * _wq); index1++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), -40.0 * explosionpower, 15.0),
                     z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                     Mth.nextDouble(RandomSource.create(), 0.0, 0.1),
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
                  );
               }

               for (int index2 = 0; index2 < (int)(7.0 * explosionpower * _wq); index2++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_STATIC_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -11.0, 11.0) * explosionpower,
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), 7.0, 15.0 * explosionpower),
                     z + Mth.nextDouble(RandomSource.create(), -6.0, 6.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.7, 0.25) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower
                  );
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -11.0, 11.0) * explosionpower,
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), 9.0, 25.0) * explosionpower,
                     z + Mth.nextDouble(RandomSource.create(), -11.0, 11.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), 0.25, 1.25 * explosionpower),
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower
                  );
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -9.0, 9.0) * explosionpower,
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), 9.0, 30.0) * explosionpower,
                     z + Mth.nextDouble(RandomSource.create(), -10.0, 10.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), 0.25, 1.25 * explosionpower),
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower
                  );
               }
            } else {
               for (int index3 = 0; index3 < (int)(5.0 * explosionpower * _wq); index3++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), -40.0 * explosionpower, 15.0),
                     z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0),
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                     Mth.nextDouble(RandomSource.create(), 0.0, 0.1),
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
                  );
               }

               for (int index4 = 0; index4 < (int)(7.0 * explosionpower * _wq); index4++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_STATIC_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -11.0, 11.0) * explosionpower,
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), 7.0, 15.0 * explosionpower),
                     z + Mth.nextDouble(RandomSource.create(), -6.0, 6.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.7, 0.25) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower
                  );
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -11.0, 11.0) * explosionpower,
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), 9.0, 25.0) * explosionpower,
                     z + Mth.nextDouble(RandomSource.create(), -11.0, 11.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), 0.25, 1.25 * explosionpower),
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower
                  );
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_FIREBALL.get(),
                     x + Mth.nextDouble(RandomSource.create(), -9.0, 9.0) * explosionpower,
                     y - 7.0 + Mth.nextDouble(RandomSource.create(), 9.0, 30.0) * explosionpower,
                     z + Mth.nextDouble(RandomSource.create(), -10.0, 10.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), 0.25, 1.25 * explosionpower),
                     Mth.nextDouble(RandomSource.create(), -1.25, 1.25) * explosionpower
                  );
               }
            }
         }

         xRadius = 1.0 + time * 2.0;
         zRadius = 1.0 + time * 2.0;

         for (double var37 = (double)(1L + Math.round(xRadius)); loop < var37; loop++) {
            if (eventtick / 4.0 == (double)Math.round(eventtick / 4.0)) {
               if (1 == Mth.nextInt(RandomSource.create(), 1, 5) && 300.0 * explosionpower >= xRadius) {
                  Groundatspot = (double)world.getHeight(
                     Types.MOTION_BLOCKING_NO_LEAVES,
                     (int)(x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * xRadius),
                     (int)(z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * zRadius)
                  );
                  if (5.0 <= eventtick) {
                     if (eventtick < (double)Mth.nextInt(RandomSource.create(), 100, 400) * explosionpower) {
                        world.addParticle(
                           (SimpleParticleType)CrustyChunksModParticleTypes.LINGERING_CLOUD.get(),
                           x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * xRadius,
                           y + 10.0,
                           z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * zRadius,
                           Math.cos((Math.PI * 2) / var37 * loop),
                           0.0,
                           Math.sin((Math.PI * 2) / var37 * loop)
                        );
                     }

                     world.addParticle(
                        (SimpleParticleType)CrustyChunksModParticleTypes.DUST_WAVE.get(),
                        x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * xRadius,
                        (double)(
                           world.getHeight(
                                 Types.MOTION_BLOCKING,
                                 Mth.floor(x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * xRadius),
                                 Mth.floor(z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * zRadius)
                              )
                              + 4
                        ),
                        z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * zRadius,
                        Math.cos((Math.PI * 2) / var37 * loop),
                        0.0,
                        Math.sin((Math.PI * 2) / var37 * loop)
                     );
                     world.addParticle(
                        (SimpleParticleType)CrustyChunksModParticleTypes.NUCLEAR_SHOCK_PARTICLE.get(),
                        x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * xRadius,
                        y + Mth.nextDouble(RandomSource.create(), 22.0, 32.0),
                        z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * zRadius,
                        Math.cos((Math.PI * 2) / var37 * loop),
                        0.0,
                        Math.sin((Math.PI * 2) / var37 * loop)
                     );
                     if (30.0 <= xRadius) {
                        world.addParticle(
                           (SimpleParticleType)CrustyChunksModParticleTypes.NUCLEAR_SHOCK_PARTICLE.get(),
                           x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * (xRadius - 30.0),
                           y + Mth.nextDouble(RandomSource.create(), 70.0, 75.0),
                           z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * (zRadius - 30.0),
                           Math.cos((Math.PI * 2) / var37 * loop),
                           0.0,
                           Math.sin((Math.PI * 2) / var37 * loop)
                        );
                     }
                  }
               }

               if (Mth.nextInt(RandomSource.create(), 1, 10) == 1 && 130.0 > eventtick && 5.0 <= eventtick) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.GROUND_HUGE_SMOKE.get(),
                     x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * (xRadius - 20.0),
                     (double)(
                        world.getHeight(
                              Types.MOTION_BLOCKING,
                              Mth.floor(x + 0.5 + Math.cos((Math.PI * 2) / var37 * loop) * (xRadius - 20.0)),
                              Mth.floor(z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * (zRadius - 20.0))
                           )
                           + 8
                     ),
                     z + 0.5 + Math.sin((Math.PI * 2) / var37 * loop) * (zRadius - 20.0),
                     0.0,
                     0.25,
                     0.0
                  );
               }
            }
         }

         if (400.0 * explosionpower <= eventtick && 410.0 * explosionpower >= eventtick) {
            if (1.0 < explosionpower) {
               for (int index6 = 0; index6 < (int)(25.0 * explosionpower * _wq); index6++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_SMOKE.get(),
                     x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0) * explosionpower,
                     y + Mth.nextDouble(RandomSource.create(), -255.0, 55.0 * explosionpower) - 1.0,
                     z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), 0.0, 0.1) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * explosionpower
                  );
               }

               for (int index7 = 0; index7 < (int)(35.0 * explosionpower * _wq); index7++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.FUSION_SMOKE.get(),
                     x + Mth.nextDouble(RandomSource.create(), -15.0, 15.0) * explosionpower,
                     y + Mth.nextDouble(RandomSource.create(), 55.0, 65.0) * explosionpower - 1.0,
                     z + Mth.nextDouble(RandomSource.create(), -15.0, 15.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.5, 0.8) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower
                  );
               }
            } else {
               for (int index8 = 0; index8 < (int)(25.0 * explosionpower * _wq); index8++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SMOKE.get(),
                     x + Mth.nextDouble(RandomSource.create(), -2.0, 2.0) * explosionpower,
                     y + Mth.nextDouble(RandomSource.create(), -255.0, 55.0) * explosionpower - 1.0,
                     z + Mth.nextDouble(RandomSource.create(), -2.0, 2.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), 0.0, 0.1) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * explosionpower
                  );
               }

               for (int index9 = 0; index9 < (int)(35.0 * explosionpower * _wq); index9++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SMOKE.get(),
                     x + Mth.nextDouble(RandomSource.create(), -15.0, 15.0) * explosionpower,
                     y + Mth.nextDouble(RandomSource.create(), 55.0, 65.0) * explosionpower - 1.0,
                     z + Mth.nextDouble(RandomSource.create(), -15.0, 15.0) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.5, 0.8) * explosionpower,
                     Mth.nextDouble(RandomSource.create(), -0.6, 0.6) * explosionpower
                  );
               }
            }

            done = true;
         }
      }
   }
}
