package shipwrights.genesis.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.surface.PlanetSurfaceSampler;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side home for planet surfaces sampled from the real world.
 *
 * <p>Each planet's six block-derived LOD grids are uploaded once as a colour
 * atlas while the matching height grids drive the cube terrain mesh. The
 * atlas is only the material layer; the planet's shape comes from real generated
 * world columns. A body with no sample keeps its painted fallback.</p>
 */
public final class PlanetSurfaceTextures {
    private static final Map<ResourceLocation, ResourceLocation> UPLOADED = new HashMap<>();

    private PlanetSurfaceTextures() {
    }

    /** Columns and rows of the cube-face atlas the planet shader expects. */
    private static final int ATLAS_COLUMNS = 3;
    private static final int ATLAS_ROWS = 2;

    /**
     * The sampled shape of each planet, kept for the geometry pass. The texture
     * says what a world looks like; this says what it IS, and a cube wearing a
     * picture of mountains is not the same thing as a cube with mountains on it.
     */
    private static final Map<ResourceLocation, PlanetSurfaceData> SURFACES = new HashMap<>();

    /** The sampled relief for a planet, or null if it has none yet. */
    public static PlanetSurfaceData surface(ResourceLocation planet) {
        return SURFACES.get(planet);
    }

    /** Uploads a freshly received surface, replacing any earlier one. */
    public static void accept(ResourceLocation planet, PlanetSurfaceData data) {
        int resolution = PlanetSurfaceSampler.RESOLUTION;
        if (data == null || !data.isValid()) {
            GenesisMod.LOGGER.warn("[SURFACE] {} sent an unusable surface grid", planet);
            return;
        }
        SURFACES.put(planet, data);
        int[] pixels = data.colours();
        try {
            // The packet is already face-major in atlas order. Copy each real
            // cube-net region into its matching 3x2 cell instead of repeating a
            // single origin-centred square over every side of the planet.
            NativeImage image = new NativeImage(
                    resolution * ATLAS_COLUMNS, resolution * ATLAS_ROWS, false);
            for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
                int offset = PlanetSurfaceData.faceOffset(face);
                int atlasX = face.atlasColumn() * resolution;
                int atlasY = face.atlasRow() * resolution;
                for (int y = 0; y < resolution; y++) {
                    for (int x = 0; x < resolution; x++) {
                        int argb = pixels[offset + y * resolution + x];
                        // NativeImage is ABGR; the sample grid is ARGB.
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                        image.setPixelRGBA(atlasX + x, atlasY + y, abgr);
                    }
                }
            }

            // Two different names for the same thing, and they have to match up
            // or the upload is wasted. getTexturedPlanetRenderType takes the
            // SHORT name and expands it to "textures/<path>.png" to bind, so the
            // texture manager has to be given the EXPANDED one — registering the
            // short name instead meant the render type looked up a file that has
            // never existed on disk, logged "Failed to load texture", and drew
            // the missing-texture checkerboard over every planet.
            ResourceLocation shortName = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID,
                    "planets/live/" + planet.getNamespace() + "/" + planet.getPath());
            ResourceLocation textureFile = ResourceLocation.fromNamespaceAndPath(shortName.getNamespace(),
                    "textures/" + shortName.getPath() + ".png");
            Minecraft.getInstance().getTextureManager().register(textureFile, new DynamicTexture(image));
            UPLOADED.put(planet, shortName);
            GenesisMod.LOGGER.info("[SURFACE] {} orbital cube and overworld LOD now share six real world regions", planet);
        } catch (Throwable error) {
            GenesisMod.LOGGER.warn("[SURFACE] could not upload {}", planet, error);
        }
    }

    /** The live surface for a planet, or null to fall back to its painted one. */
    public static ResourceLocation get(ResourceLocation planet) {
        return UPLOADED.get(planet);
    }

    /** Dropped on disconnect so a different world cannot inherit these. */
    public static void clear() {
        UPLOADED.clear();
        SURFACES.clear();
    }
}
