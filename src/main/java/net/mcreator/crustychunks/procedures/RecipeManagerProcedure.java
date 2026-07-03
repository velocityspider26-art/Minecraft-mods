package net.mcreator.crustychunks.procedures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class RecipeManagerProcedure {
   @SubscribeEvent
   public static void onWorldLoad(Load event) {
      execute(event, event.getLevel());
   }

   public static void execute(LevelAccessor world) {
      execute(null, world);
   }

   private static void execute(@Nullable Event event, LevelAccessor world) {
      double indexclear = 0.0;
      if (world instanceof ServerLevel srvlvl_) {
         List<JsonObject> jsons = new ArrayList<>();

         class Output implements ResourceOutput {
            private List<JsonObject> jsonObjects;
            private PackResources packResources;

            public Output(List<JsonObject> jsonObjects) {
               this.jsonObjects = jsonObjects;
            }

            public void setPackResources(PackResources packResources) {
               this.packResources = packResources;
            }

            public void accept(ResourceLocation resourceLocation, IoSupplier<InputStream> ioSupplier) {
               try {
                  JsonObject jsonObject = (JsonObject)new Gson()
                     .fromJson(
                        new BufferedReader(new InputStreamReader((InputStream)ioSupplier.get(), StandardCharsets.UTF_8))
                           .lines()
                           .collect(Collectors.joining("\n")),
                        JsonObject.class
                     );
                  this.jsonObjects.add(jsonObject);
               } catch (Exception var4) {
               }
            }
         }

         Output output = new Output(jsons);
         ResourceManager rm = srvlvl_.getServer().getResourceManager();
         rm.listPacks().forEach(resource -> {
            output.setPackResources(resource);
            resource.listResources(PackType.SERVER_DATA, "crusty_chunks", "recipes/assembly", output);
         });

         for (JsonObject jsoniterator : jsons) {
            CrustyChunksModVariables.recipesloaded.add(jsoniterator);
         }
      }
   }
}
