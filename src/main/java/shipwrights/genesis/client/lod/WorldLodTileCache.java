package shipwrights.genesis.client.lod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import shipwrights.genesis.GenesisMod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incremental snapshots of real client chunks for the near/mid altitude LOD.
 *
 * <p>The global six-face sample guarantees that the planet never becomes an
 * empty void. These tiles are the higher-detail overlay: they are captured from
 * chunks the client genuinely received, so terrain edits and player-built
 * structures can survive after vanilla unloads the original render sections.
 * No chunk is force-loaded and no world scan happens from the render thread.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class WorldLodTileCache {
    public static final int TILE_SIZE = 64;
    public static final int CELLS = 8;
    public static final int VERTICES = CELLS + 1;

    private static final int MAX_TILES = 4096;
    private static final int NORMAL_REFRESH_TICKS = 400;
    private static final int NEAR_REFRESH_TICKS = 80;
    private static final int MAX_QUEUE = 2048;

    private static final Map<Long, Tile> TILES = new LinkedHashMap<>(256, 0.75f, true);
    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final BlockPos.MutableBlockPos MUTABLE = new BlockPos.MutableBlockPos();

    private static ClientLevel activeLevel;

    private WorldLodTileCache() {
    }

    public record Tile(int tileX, int tileZ, short[] heights, int[] colours, long sampledAt) {
        public int minX() {
            return tileX * TILE_SIZE;
        }

        public int minZ() {
            return tileZ * TILE_SIZE;
        }

        public int index(int x, int z) {
            return z * VERTICES + x;
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        int chunkX = event.getChunk().getPos().x;
        int chunkZ = event.getChunk().getPos().z;
        queueTile(Math.floorDiv(chunkX * 16, TILE_SIZE), Math.floorDiv(chunkZ * 16, TILE_SIZE), true);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        // Capture the tile before ClientChunkCache drops the chunk. The event is
        // posted before removal, so the full tile can still be read here on the
        // client thread. Failure simply leaves the previous snapshot intact.
        int chunkX = event.getChunk().getPos().x;
        int chunkZ = event.getChunk().getPos().z;
        int tileX = Math.floorDiv(chunkX * 16, TILE_SIZE);
        int tileZ = Math.floorDiv(chunkZ * 16, TILE_SIZE);
        Tile tile = sample(level, tileX, tileZ);
        if (tile != null) {
            put(tile);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !level.dimension().equals(Level.OVERWORLD)) {
            if (activeLevel != null) {
                clear();
            }
            activeLevel = null;
            return;
        }

        if (activeLevel != level) {
            clear();
            activeLevel = level;
        }

        if (minecraft.gameRenderer == null) {
            return;
        }

        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        int centerTileX = Math.floorDiv(Mth.floor(camera.x), TILE_SIZE);
        int centerTileZ = Math.floorDiv(Mth.floor(camera.z), TILE_SIZE);
        int renderBlocks = Math.max(64, minecraft.options.getEffectiveRenderDistance() * 16);
        int radius = Math.max(2, (int) Math.ceil(renderBlocks / (double) TILE_SIZE) + 1);
        long now = level.getGameTime();

        // Nearest first. The queue is bounded and only references chunks the
        // client is already expected to hold.
        for (int ring = 0; ring <= radius; ring++) {
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int tileX = centerTileX + dx;
                    int tileZ = centerTileZ + dz;
                    Tile existing = peek(tileX, tileZ);
                    long refresh = ring <= 1 ? NEAR_REFRESH_TICKS : NORMAL_REFRESH_TICKS;
                    if (existing == null || now - existing.sampledAt() >= refresh) {
                        queueTile(tileX, tileZ, false);
                    }
                }
            }
        }

        int budget = camera.y >= 512.0 ? 4 : 2;
        while (budget-- > 0 && !PENDING.isEmpty()) {
            long key = PENDING.removeFirst();
            QUEUED.remove(key);
            int tileX = keyX(key);
            int tileZ = keyZ(key);
            Tile tile = sample(level, tileX, tileZ);
            if (tile != null) {
                put(tile);
            }
        }
    }

    public static List<Tile> tilesNear(double worldX, double worldZ, double radiusBlocks) {
        int minTileX = Math.floorDiv(Mth.floor(worldX - radiusBlocks), TILE_SIZE);
        int maxTileX = Math.floorDiv(Mth.floor(worldX + radiusBlocks), TILE_SIZE);
        int minTileZ = Math.floorDiv(Mth.floor(worldZ - radiusBlocks), TILE_SIZE);
        int maxTileZ = Math.floorDiv(Mth.floor(worldZ + radiusBlocks), TILE_SIZE);
        List<Tile> result = new ArrayList<>((maxTileX - minTileX + 1) * (maxTileZ - minTileZ + 1));
        synchronized (TILES) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                    Tile tile = TILES.get(key(tileX, tileZ));
                    if (tile != null) {
                        result.add(tile);
                    }
                }
            }
        }
        return result;
    }

    public static boolean covers(double worldX, double worldZ) {
        int tileX = Math.floorDiv(Mth.floor(worldX), TILE_SIZE);
        int tileZ = Math.floorDiv(Mth.floor(worldZ), TILE_SIZE);
        synchronized (TILES) {
            return TILES.containsKey(key(tileX, tileZ));
        }
    }

    public static void clear() {
        synchronized (TILES) {
            TILES.clear();
        }
        PENDING.clear();
        QUEUED.clear();
    }

    private static Tile peek(int tileX, int tileZ) {
        synchronized (TILES) {
            return TILES.get(key(tileX, tileZ));
        }
    }

    private static void put(Tile tile) {
        synchronized (TILES) {
            TILES.put(key(tile.tileX(), tile.tileZ()), tile);
            while (TILES.size() > MAX_TILES) {
                var iterator = TILES.entrySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next();
                iterator.remove();
            }
        }
    }

    private static void queueTile(int tileX, int tileZ, boolean front) {
        long key = key(tileX, tileZ);
        if (QUEUED.contains(key) || PENDING.size() >= MAX_QUEUE) {
            return;
        }
        QUEUED.add(key);
        if (front) {
            PENDING.addFirst(key);
        } else {
            PENDING.addLast(key);
        }
    }

    private static Tile sample(ClientLevel level, int tileX, int tileZ) {
        int minX = tileX * TILE_SIZE;
        int minZ = tileZ * TILE_SIZE;
        int maxX = minX + TILE_SIZE;
        int maxZ = minZ + TILE_SIZE;

        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                if (level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
                    return null;
                }
            }
        }

        short[] heights = new short[VERTICES * VERTICES];
        int[] colours = new int[VERTICES * VERTICES];
        for (int z = 0; z < VERTICES; z++) {
            int worldZ = minZ + z * TILE_SIZE / CELLS;
            for (int x = 0; x < VERTICES; x++) {
                int worldX = minX + x * TILE_SIZE / CELLS;
                LevelChunk chunk = level.getChunkSource().getChunk(
                        Math.floorDiv(worldX, 16), Math.floorDiv(worldZ, 16), ChunkStatus.FULL, false);
                if (chunk == null) {
                    return null;
                }
                int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, worldX & 15, worldZ & 15) - 1;
                height = Mth.clamp(height, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
                MUTABLE.set(worldX, height, worldZ);
                BlockState state = level.getBlockState(MUTABLE);
                Holder<Biome> biome = level.getBiome(MUTABLE);
                int index = z * VERTICES + x;
                heights[index] = (short) height;
                colours[index] = colourFor(level, MUTABLE, state, biome);
            }
        }
        return new Tile(tileX, tileZ, heights, colours, level.getGameTime());
    }

    private static int colourFor(ClientLevel level, BlockPos pos, BlockState state, Holder<Biome> biome) {
        MapColor mapColor = state.getMapColor(level, pos);
        int rgb;
        if (!state.getFluidState().isEmpty() || mapColor == MapColor.WATER) {
            rgb = biome.value().getWaterColor();
        } else if (mapColor == MapColor.GRASS) {
            rgb = biome.value().getGrassColor(pos.getX(), pos.getZ());
        } else if (mapColor == MapColor.PLANT) {
            rgb = biome.value().getFoliageColor();
        } else if (mapColor != MapColor.NONE && mapColor.col != 0) {
            rgb = mapColor.col;
        } else {
            rgb = biome.value().getGrassColor(pos.getX(), pos.getZ());
        }
        return 0xFF000000 | rgb;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int keyX(long key) {
        return (int) (key >> 32);
    }

    private static int keyZ(long key) {
        return (int) key;
    }
}
