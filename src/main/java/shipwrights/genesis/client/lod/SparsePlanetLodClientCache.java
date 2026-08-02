package shipwrights.genesis.client.lod;

import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.surface.SparsePlanetLodTile;

import java.util.LinkedHashMap;
import java.util.Map;

/** Client-side bounded cache for server-authored sparse cube-face terrain. */
public final class SparsePlanetLodClientCache {
    private static final int MAX_TILES_PER_PLANET = 12_288;
    private static final Object LOCK = new Object();
    private static final Map<ResourceLocation, LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile>> TILES =
            new LinkedHashMap<>();

    private SparsePlanetLodClientCache() {
    }

    public static void accept(ResourceLocation planet, SparsePlanetLodTile tile) {
        synchronized (LOCK) {
            LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile> planetTiles =
                    TILES.computeIfAbsent(planet, ignored -> new LinkedHashMap<>(256, 0.75f, true));
            SparsePlanetLodTile previous = planetTiles.get(tile.key());
            if (previous != null && previous.revision() > tile.revision()) {
                return;
            }
            planetTiles.put(tile.key(), tile);
            while (planetTiles.size() > MAX_TILES_PER_PLANET) {
                SparsePlanetLodTile.Key eldest = planetTiles.keySet().iterator().next();
                planetTiles.remove(eldest);
            }
        }
    }

    public static SparsePlanetLodTile[] snapshot(ResourceLocation planet) {
        synchronized (LOCK) {
            LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile> planetTiles = TILES.get(planet);
            if (planetTiles == null || planetTiles.isEmpty()) {
                return new SparsePlanetLodTile[0];
            }
            return planetTiles.values().toArray(SparsePlanetLodTile[]::new);
        }
    }

    public static int tileCount(ResourceLocation planet) {
        synchronized (LOCK) {
            LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile> planetTiles = TILES.get(planet);
            return planetTiles == null ? 0 : planetTiles.size();
        }
    }


    public static boolean hasExactChunk(ResourceLocation planet,
                                        shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face face,
                                        int chunkX, int chunkZ) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        synchronized (LOCK) {
            LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile> planetTiles = TILES.get(planet);
            if (planetTiles == null) {
                return false;
            }
            SparsePlanetLodTile tile = planetTiles.get(new SparsePlanetLodTile.Key(face, originX, originZ, 1));
            return tile != null && tile.exact();
        }
    }

    public static int bestCellSizeAt(ResourceLocation planet,
                                     shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face face,
                                     double worldX, double worldZ) {
        // Tile origins are aligned to their 16-cell span, so the best tile can
        // be found with five direct map lookups instead of scanning up to 12k
        // cached tiles every render frame while the survey crosshair moves.
        int blockX = (int) Math.floor(worldX);
        int blockZ = (int) Math.floor(worldZ);
        int[] cellSizes = {1, 4, 16, 64, 128};
        synchronized (LOCK) {
            LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile> planetTiles = TILES.get(planet);
            if (planetTiles == null) {
                return Integer.MAX_VALUE;
            }
            for (int cellSize : cellSizes) {
                int span = SparsePlanetLodTile.RESOLUTION * cellSize;
                int originX = Math.floorDiv(blockX, span) * span;
                int originZ = Math.floorDiv(blockZ, span) * span;
                SparsePlanetLodTile tile = planetTiles.get(
                        new SparsePlanetLodTile.Key(face, originX, originZ, cellSize));
                if (tile != null) {
                    return cellSize;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Four 2048-block tiles cover one 8192-block face at cell size 128. */
    public static boolean hasGlobalCoverage(ResourceLocation planet,
                                            shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face face) {
        int count = 0;
        synchronized (LOCK) {
            LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile> planetTiles = TILES.get(planet);
            if (planetTiles == null) {
                return false;
            }
            for (SparsePlanetLodTile tile : planetTiles.values()) {
                if (tile.face() == face && tile.cellSize() == 128) {
                    count++;
                }
            }
        }
        return count >= 16;
    }

    public static boolean hasCompleteGlobalCube(ResourceLocation planet) {
        for (shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face face
                : shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face.values()) {
            if (!hasGlobalCoverage(planet, face)) {
                return false;
            }
        }
        return true;
    }

    public static void clear() {
        synchronized (LOCK) {
            TILES.clear();
        }
    }
}
