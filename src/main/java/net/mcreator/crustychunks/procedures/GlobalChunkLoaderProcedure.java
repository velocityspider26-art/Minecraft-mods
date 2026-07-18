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
   public static final ConcurrentHashMap<Long, Long> EXPLOSION_CHUNKS = new ConcurrentHashMap<>();

   public static void registerChunk(ServerLevel level, int x, int z) {
      long key = new ChunkPos(x >> 4, z >> 4).toLong();
      EXPLOSION_CHUNKS.put(key, level.getGameTime() + 5L);
   }

   @SubscribeEvent
   public static void onEntityTick(LevelTickEvent.Post event) {
      if (true && !event.getLevel().isClientSide) {
         ServerLevel level = (ServerLevel)event.getLevel();
         long currentTime = level.getGameTime();
         level.getEntities().getAll().forEach(entity -> {
            if (entity.getType().is(CHUNK_TAG)) {
               ChunkPos pos = entity.chunkPosition();
               long key = pos.toLong();
               if (!level.getForcedChunks().contains(key)) {
                  level.setChunkForced(pos.x, pos.z, true);
               }

               EXPIRING_CHUNKS.put(key, currentTime + 40L);
            }
         });
         Iterator<Entry<Long, Long>> explosionIt = EXPLOSION_CHUNKS.entrySet().iterator();

         while (explosionIt.hasNext()) {
            Entry<Long, Long> entry = explosionIt.next();
            if (currentTime < entry.getValue()) {
               level.setChunkForced(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()), true);
            } else {
               level.setChunkForced(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()), false);
               explosionIt.remove();
            }
         }

         Iterator<Entry<Long, Long>> entityIt = EXPIRING_CHUNKS.entrySet().iterator();

         while (entityIt.hasNext()) {
            Entry<Long, Long> entry = entityIt.next();
            if (currentTime >= entry.getValue()) {
               level.setChunkForced(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()), false);
               entityIt.remove();
            }
         }
      }
   }
}
