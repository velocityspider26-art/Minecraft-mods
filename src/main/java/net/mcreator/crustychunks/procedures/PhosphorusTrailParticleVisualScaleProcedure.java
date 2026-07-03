package net.mcreator.crustychunks.procedures;

public class PhosphorusTrailParticleVisualScaleProcedure {
   public static double execute(double age) {
      double size = 0.0;
      return Math.min(Math.max(0.25, age / 4.0 + 0.5), 10.0);
   }
}
