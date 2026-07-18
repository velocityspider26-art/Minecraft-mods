package net.mcreator.crustychunks.procedures;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class SpaceLogicProcedure {
   private static final Set<String> SPACE_DIMENSIONS_CACHE = new HashSet<>();
   private static boolean isLoaded = false;

   public static boolean execute(LevelAccessor world, double x, double y, double z) {
      if (y > 2000.0) {
         return true;
      } else {
         if (!isLoaded && world.getServer() != null) {
            loadSpaceDimensions(world.getServer());
         }

         String currentDim = world instanceof Level _lvl ? _lvl.dimension().location().toString() : "minecraft:overworld";
         return SPACE_DIMENSIONS_CACHE.contains(currentDim);
      }
   }

   private static void loadSpaceDimensions(MinecraftServer server) {
      try {
         Resource resource = server.getResourceManager().getResourceOrThrow(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "tags/dimension_type/space_dimensions.json"));
         JsonObject json = JsonParser.parseReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8)).getAsJsonObject();

         for (JsonElement element : json.getAsJsonArray("values")) {
            SPACE_DIMENSIONS_CACHE.add(element.getAsString());
         }

         isLoaded = true;
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }
}
