package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.world.entity.Entity;

public class LKNProcedure {
   public static void execute(Entity entity) {
      try {
      if (entity != null) {
         boolean _setval = false;
         {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
            _vars.LeftKey = _setval;
            _vars.syncPlayerVariables(entity);
         
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LKNProcedure.execute", _wtSafe);
      }
   }
}
