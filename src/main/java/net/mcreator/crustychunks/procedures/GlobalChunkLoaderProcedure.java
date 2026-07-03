package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class GlobalChunkLoaderProcedure {
   public static final TagKey<EntityType<?>> CHUNK_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:chunkloading"));
   private static final ConcurrentHashMap<Long, Long> EXPIRING_CHUNKS = new ConcurrentHashMap<>();
   private static final int LIFETIME_TICKS = 40;

   @SubscribeEvent
   public static void onEntityTick(LevelTickEvent.Post event) {
      if (true && !event.getLevel().isClientSide) {
         ServerLevel level = (ServerLevel)event.getLevel();
         long currentTime = level.getGameTime();
         level.getEntities().getAll().forEach(entity -> {
            if (entity.getType().is(CHUNK_TAG)) {
               ChunkPos pos = entity.chunkPosition();
               long keyx = pos.toLong();
               if (!level.getForcedChunks().contains(keyx)) {
                  level.setChunkForced(pos.x, pos.z, true);
               }

               EXPIRING_CHUNKS.put(keyx, currentTime + 40L);
            }
         });
         Iterator<Entry<Long, Long>> it = EXPIRING_CHUNKS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Long, Long> entry = it.next();
            if (currentTime >= entry.getValue()) {
               long key = entry.getKey();
               level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
               it.remove();
            }
         }
      }
   }
}
