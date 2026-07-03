package net.mcreator.crustychunks.procedures;

import net.neoforged.fml.ModList;

public class ProjectileLibsSmallArmsProcedure {
   public static double execute() {
      double multiplier = 0.0;
      if (ModList.get().isLoaded("ritchiesprojectilelib")) {
         multiplier = 1.75;
      } else {
         multiplier = 1.0;
      }

      return multiplier;
   }
}
