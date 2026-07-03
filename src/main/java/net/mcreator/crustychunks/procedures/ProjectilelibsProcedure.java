package net.mcreator.crustychunks.procedures;

import net.neoforged.fml.ModList;

public class ProjectilelibsProcedure {
   public static double execute() {
      double multiplier = 0.0;
      if (ModList.get().isLoaded("ritchiesprojectilelib")) {
         multiplier = 2.0;
      } else {
         multiplier = 1.0;
      }

      return multiplier;
   }
}
