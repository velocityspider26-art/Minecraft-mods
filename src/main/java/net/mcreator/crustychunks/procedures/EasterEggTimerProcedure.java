package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class EasterEggTimerProcedure {
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (true) {
         execute(event, event.getEntity());
      }
   }

   public static void execute(Entity entity) {
      execute(null, entity);
   }

   private static void execute(@Nullable Event event, Entity entity) {
      if (entity != null) {
         if (0.0
            < entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
               .eastereggcooldown) {
            double _setval = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .eastereggcooldown
               - 1.0;
            {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
               _vars.eastereggcooldown = _setval;
               _vars.syncPlayerVariables(entity);
            
         }
         }
      }
   }
}
