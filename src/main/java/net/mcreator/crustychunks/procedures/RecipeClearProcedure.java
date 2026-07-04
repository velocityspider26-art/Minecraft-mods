package net.mcreator.crustychunks.procedures;

import com.google.gson.JsonArray;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class RecipeClearProcedure {
   @SubscribeEvent
   public static void onWorldUnload(Unload event) {
      execute(event);
   }

   public static void execute() {
      try {
      execute(null);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RecipeClearProcedure.execute", _wtSafe);
      }
   }

   private static void execute(@Nullable Event event) {
      try {
      double indexclear = 0.0;
      CrustyChunksModVariables.recipesloaded = new JsonArray();
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RecipeClearProcedure.execute", _wtSafe);
      }
   }
}
