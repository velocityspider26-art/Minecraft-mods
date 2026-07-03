package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.world.entity.Entity;

public class LKYProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         boolean _setval = true;
         {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
            _vars.LeftKey = _setval;
            _vars.syncPlayerVariables(entity);
         
         }
      }
   }
}
