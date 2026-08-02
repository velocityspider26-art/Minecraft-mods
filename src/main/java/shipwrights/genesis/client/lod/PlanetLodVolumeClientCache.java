package shipwrights.genesis.client.lod;

import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.surface.PlanetLodVolumeTile;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.LinkedHashMap;
import java.util.Map;

/** Client cache for exact 3D chunk-volume LOD tiles. */
public final class PlanetLodVolumeClientCache {
    private static final int MAX_TILES_PER_PLANET = 16384;
    private static final Object LOCK = new Object();
    private static final Map<ResourceLocation,
            LinkedHashMap<PlanetLodVolumeTile.Key, PlanetLodVolumeTile>> TILES = new LinkedHashMap<>();

    private PlanetLodVolumeClientCache() {
    }

    public static void accept(ResourceLocation planet, PlanetLodVolumeTile tile) {
        synchronized (LOCK) {
            LinkedHashMap<PlanetLodVolumeTile.Key, PlanetLodVolumeTile> planetTiles =
                    TILES.computeIfAbsent(planet, ignored -> new LinkedHashMap<>(256, 0.75f, true));
            PlanetLodVolumeTile previous = planetTiles.get(tile.key());
            if (previous != null && previous.revision() > tile.revision()) {
                return;
            }
            planetTiles.put(tile.key(), tile);
            PlanetVolumeMeshCache.invalidate(planet, tile.key());
            while (planetTiles.size() > MAX_TILES_PER_PLANET) {
                PlanetLodVolumeTile.Key eldest = planetTiles.keySet().iterator().next();
                planetTiles.remove(eldest);
                PlanetVolumeMeshCache.invalidate(planet, eldest);
            }
        }
    }

    public static PlanetLodVolumeTile[] snapshot(ResourceLocation planet) {
        synchronized (LOCK) {
            LinkedHashMap<PlanetLodVolumeTile.Key, PlanetLodVolumeTile> planetTiles = TILES.get(planet);
            return planetTiles == null || planetTiles.isEmpty()
                    ? new PlanetLodVolumeTile[0]
                    : planetTiles.values().toArray(PlanetLodVolumeTile[]::new);
        }
    }

    public static boolean hasTileAt(ResourceLocation planet, CubeNetSurfaceTransform.Face face,
                                    double worldX, double worldZ) {
        int chunkX = Math.floorDiv((int) Math.floor(worldX), 16);
        int chunkZ = Math.floorDiv((int) Math.floor(worldZ), 16);
        synchronized (LOCK) {
            LinkedHashMap<PlanetLodVolumeTile.Key, PlanetLodVolumeTile> planetTiles = TILES.get(planet);
            return planetTiles != null
                    && planetTiles.containsKey(new PlanetLodVolumeTile.Key(face, chunkX, chunkZ));
        }
    }

    public static int tileCount(ResourceLocation planet) {
        synchronized (LOCK) {
            LinkedHashMap<PlanetLodVolumeTile.Key, PlanetLodVolumeTile> planetTiles = TILES.get(planet);
            return planetTiles == null ? 0 : planetTiles.size();
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            TILES.clear();
        }
        PlanetVolumeMeshCache.clear();
    }
}
