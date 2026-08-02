package shipwrights.genesis.client.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetSurfaceTextures;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.surface.PlanetSurfaceSampler;
import shipwrights.genesis.space.surface.SparsePlanetLodTile;
import shipwrights.genesis.space.surface.SparsePlanetLodService;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Renders the overworld's own DH terrain as one continuous world-to-cube LOD.
 *
 * <p>This is not a second Earth object. Each vertex starts at its real
 * cube-net world coordinate. During ascent the five remote regions fold around
 * the unchanged face beneath the player, forming a six-sided planet from the
 * same cached terrain columns. A uniform camera-relative distance compression
 * keeps the terrain inside Minecraft's far plane while preserving its angular
 * size, so high altitude no longer turns the world into an empty void.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class WorldLodEarthRenderer {
    private static final ResourceLocation OVERWORLD_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final RenderType LOD_RENDER_TYPE = RenderType.debugQuads();

    private static volatile boolean replaceFlatDh;
    private static boolean loggedActive;
    private static boolean loggedFolded;

    private WorldLodEarthRenderer() {
    }

    /** Read by the DH before-render callback on the following render pass. */
    public static boolean shouldReplaceFlatDh() {
        return replaceFlatDh;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !level.dimension().equals(Level.OVERWORLD)) {
            replaceFlatDh = false;
            loggedActive = false;
            loggedFolded = false;
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        int lodStart = GenesisClientConfig.getWorldLodStartHeight();
        if (camera.y < lodStart) {
            replaceFlatDh = false;
            loggedActive = false;
            loggedFolded = false;
            return;
        }

        Celestial earth = GenesisMod.getCelestialForLevel(level);
        if (earth == null) {
            replaceFlatDh = false;
            return;
        }

        CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
        CubeNetSurfaceTransform.Face currentFace = transform.faceContaining(camera.x, camera.z);
        if (currentFace == null) {
            currentFace = CubeNetSurfaceTransform.Face.UP;
        }

        DhCubeLodBridge.update(level, transform, currentFace, camera.x, camera.z);
        PlanetSurfaceData surface = DhCubeLodBridge.surface();
        if (surface == null || !surface.isValid()) {
            // Keep the previous real-world sample as a temporary visual source
            // while DH's dedicated worker prepares the first face. As soon as a
            // DH face completes, the bridge replaces this data in both renderers.
            surface = PlanetSurfaceTextures.surface(OVERWORLD_ID);
        }
        if (surface == null || !surface.isValid()) {
            replaceFlatDh = false;
            return;
        }

        int lodFull = GenesisClientConfig.getWorldLodFullHeight();
        float lodAlpha = smoothstep((float) ((camera.y - lodStart) / Math.max(1.0, lodFull - lodStart)));
        float foldAlpha = cubeFoldAlpha(camera.y);
        int vanillaRadius = Math.max(64, minecraft.options.getEffectiveRenderDistance() * 16);
        double vanillaGroundRadius = vanillaGroundRadius(camera.y, level.getMaxBuildHeight(), vanillaRadius);
        float verticalReplacement = smoothstep((float) ((camera.y - (level.getMaxBuildHeight() - 64.0))
                / Math.max(128.0, vanillaRadius * 1.5)));
        lodAlpha = Math.max(lodAlpha, verticalReplacement);

        replaceFlatDh = camera.y >= lodFull
                && DhCubeLodBridge.hasFace(currentFace)
                && DhCubeLodBridge.hasAnyDhTerrain();

        // Rendering at the literal Y=16k..21k camera distance makes a house
        // physically smaller than a pixel even when its geometry is exact.
        // Compress only the empty altitude, never the terrain/chunk geometry.
        // The same mapping is used by the orbital renderer after crossing.
        float visualFrameInfluence = Math.max(lodAlpha, foldAlpha);
        Vec3 renderCamera = EarthLodVisualFrame.overworldRenderCamera(
                camera, surface.seaLevel(), visualFrameInfluence);

        MultiBufferSource.BufferSource source = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = source.getBuffer(LOD_RENDER_TYPE);
        Matrix4f view = event.getModelViewMatrix();
        PlanetVoxelRenderer.requestOverworldInterest(camera, event.getCamera().getLookVector(),
                transform, currentFace, surface.seaLevel());
        boolean voxelEarth = PlanetVoxelRenderer.hasVoxelData();
        boolean voxelCoverage = voxelEarth
                && PlanetVoxelRenderer.hasCompleteCoarseCoverage(transform);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        if (!voxelCoverage) {
            renderCubeNet(buffer, view, renderCamera, transform, currentFace, surface,
                    lodAlpha, foldAlpha, verticalReplacement, vanillaRadius, vanillaGroundRadius);
            renderSparseLodTiles(buffer, view, renderCamera, transform, currentFace, surface,
                    lodAlpha, foldAlpha, verticalReplacement, vanillaRadius, vanillaGroundRadius);
            renderLiveChunkPatches(buffer, view, renderCamera, transform, currentFace, surface,
                    lodAlpha, foldAlpha, verticalReplacement, vanillaRadius, vanillaGroundRadius);
        }
        source.endBatch(LOD_RENDER_TYPE);
        if (voxelEarth) {
            PlanetVoxelRenderer.renderOverworld(view, renderCamera, transform, currentFace, surface,
                    lodAlpha, foldAlpha, verticalReplacement, vanillaRadius, vanillaGroundRadius);
        } else {
            PlanetVolumeLodRenderer.renderOverworld(view, renderCamera, transform, currentFace, surface,
                    lodAlpha, foldAlpha, verticalReplacement, vanillaRadius, vanillaGroundRadius);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        if (!loggedActive) {
            loggedActive = true;
            GenesisMod.LOGGER.info(
                    "[DH-CUBE] world-to-planet LOD visible at y={}; face={}, DH columns={}",
                    Mth.floor(camera.y), currentFace, DhCubeLodBridge.successfulColumns() + " base / "
                            + DhLiveChunkPatchCache.patchCount() + " DH exact chunks / "
                            + SparsePlanetLodClientCache.tileCount(SparsePlanetLodService.EARTH_ID)
                            + " sparse tiles / "
                            + PlanetLodVolumeClientCache.tileCount(SparsePlanetLodService.EARTH_ID)
                            + " textured volume chunks");
        }
        if (foldAlpha >= 0.999f && !loggedFolded) {
            loggedFolded = true;
            GenesisMod.LOGGER.info(
                    "[DH-CUBE] the overworld itself is now the complete six-face Earth; no standalone ground model is active");
        }
    }

    private static void renderCubeNet(VertexConsumer buffer, Matrix4f view, Vec3 camera,
                                      CubeNetSurfaceTransform transform,
                                      CubeNetSurfaceTransform.Face currentFace,
                                      PlanetSurfaceData surface,
                                      float lodAlpha, float foldAlpha, float centerFillAlpha,
                                      int vanillaRadius, double vanillaGroundRadius) {
        int quality = GenesisClientConfig.getWorldLodQuality();
        int currentStep = quality == 1 ? 2 : 1;
        int remoteStep = quality == 3 ? 1 : quality == 2 ? 2 : 4;
        int resolution = PlanetSurfaceSampler.RESOLUTION;
        double faceSpan = transform.faceSpan();
        double faceHalf = transform.faceHalfSpan();
        double currentCenterX = transform.faceCenterX(currentFace);
        double currentCenterZ = transform.faceCenterZ(currentFace);
        double cubeCenterY = surface.seaLevel() - faceHalf;

        double renderScale = renderDistanceCompression(
                camera, currentCenterX, cubeCenterY, currentCenterZ, vanillaRadius);
        int lodRadius = GenesisClientConfig.getWorldLodRadius();

        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            boolean current = face == currentFace;
            if (!current && foldAlpha <= 0.001f) {
                continue;
            }
            float faceAlpha = current ? lodAlpha : foldAlpha;
            if (faceAlpha <= 0.002f) {
                continue;
            }
            int step = current ? currentStep : remoteStep;
            for (int row = 0; row < resolution - 1; row += step) {
                int row1 = Math.min(resolution - 1, row + step);
                double v0 = gridCoordinate(row, resolution, faceSpan);
                double v1 = gridCoordinate(row1, resolution, faceSpan);
                for (int column = 0; column < resolution - 1; column += step) {
                    int column1 = Math.min(resolution - 1, column + step);
                    double u0 = gridCoordinate(column, resolution, faceSpan);
                    double u1 = gridCoordinate(column1, resolution, faceSpan);

                    float alpha = faceAlpha;
                    if (current) {
                        double cellX = currentCenterX + (u0 + u1) * 0.5;
                        double cellZ = currentCenterZ + (v0 + v1) * 0.5;
                        double distance = horizontalDistance(camera.x, camera.z, cellX, cellZ);
                        double near0 = Math.max(0.0, vanillaGroundRadius * 0.72 - 48.0);
                        double near1 = Math.max(32.0, vanillaGroundRadius * 1.12 + 24.0);
                        float outerBand = distanceBand(distance, near0, near1,
                                lodRadius * 0.94, lodRadius);
                        // At extreme altitude vanilla's three-dimensional far
                        // plane has no terrain directly underneath the camera.
                        // Fold progress closes that hole continuously rather than
                        // waiting for a model swap at the atmosphere boundary.
                        alpha *= Math.max(outerBand, Math.max(foldAlpha, centerFillAlpha));
                    }
                    if (alpha <= 0.002f) {
                        continue;
                    }

                    Vector3d p00 = blendedPoint(face, currentFace, transform, surface,
                            column, row, u0, v0, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ);
                    Vector3d p10 = blendedPoint(face, currentFace, transform, surface,
                            column1, row, u1, v0, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ);
                    Vector3d p11 = blendedPoint(face, currentFace, transform, surface,
                            column1, row1, u1, v1, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ);
                    Vector3d p01 = blendedPoint(face, currentFace, transform, surface,
                            column, row1, u0, v1, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ);

                    addVertex(buffer, view, camera, p01, renderScale,
                            colour(surface, face, column, row1), alpha);
                    addVertex(buffer, view, camera, p11, renderScale,
                            colour(surface, face, column1, row1), alpha);
                    addVertex(buffer, view, camera, p10, renderScale,
                            colour(surface, face, column1, row), alpha);
                    addVertex(buffer, view, camera, p00, renderScale,
                            colour(surface, face, column, row), alpha);
                }
            }
        }
    }


    /**
     * Draws the sparse server-authored hierarchy between the 64x64 global face
     * and exact chunks. Coarse tiles are rendered first and progressively finer
     * tiles sit a fraction farther outward, so an explored region replaces its
     * prediction without a sharp square or z-fighting.
     */
    private static void renderSparseLodTiles(VertexConsumer buffer, Matrix4f view, Vec3 camera,
                                             CubeNetSurfaceTransform transform,
                                             CubeNetSurfaceTransform.Face currentFace,
                                             PlanetSurfaceData surface,
                                             float lodAlpha, float foldAlpha,
                                             float centerFillAlpha,
                                             int vanillaRadius,
                                             double vanillaGroundRadius) {
        SparsePlanetLodTile[] tiles = SparsePlanetLodClientCache.snapshot(SparsePlanetLodService.EARTH_ID);
        if (tiles.length == 0) {
            return;
        }
        Arrays.sort(tiles, Comparator.comparingInt(SparsePlanetLodTile::cellSize).reversed());

        double currentCenterX = transform.faceCenterX(currentFace);
        double currentCenterZ = transform.faceCenterZ(currentFace);
        double cubeCenterY = surface.seaLevel() - transform.faceHalfSpan();
        double renderScale = renderDistanceCompression(
                camera, currentCenterX, cubeCenterY, currentCenterZ, vanillaRadius);
        int lodRadius = GenesisClientConfig.getWorldLodRadius();
        int quality = GenesisClientConfig.getWorldLodQuality();
        int rendered = 0;

        // The old pass submitted every cached tile every frame (889 tiles in the
        // supplied log, with up to 256 quads and walls each). Keep the complete
        // 128-block fallback on all faces, but spend finer geometry only around
        // the actual camera face. This preserves continuity without stalling the
        // render thread during ascent.
        for (SparsePlanetLodTile tile : tiles) {
            if (rendered >= 384) {
                break;
            }
            boolean current = tile.face() == currentFace;
            if (tile.exact() && PlanetLodVolumeClientCache.hasTileAt(
                    SparsePlanetLodService.EARTH_ID, tile.face(),
                    tile.originX() + tile.span() * 0.5,
                    tile.originZ() + tile.span() * 0.5)) {
                // The block-volume tile is the actual 3D chunk. Do not leave
                // the old one-height/one-colour patch underneath it, because
                // that is the PS1-looking square visible between buildings.
                continue;
            }
            if (!current && tile.cellSize() < 128) {
                continue;
            }
            float baseAlpha = current ? lodAlpha : foldAlpha;
            if (baseAlpha <= 0.002f) {
                continue;
            }

            double tileCenterX = tile.originX() + tile.span() * 0.5;
            double tileCenterZ = tile.originZ() + tile.span() * 0.5;
            double tileDistance = horizontalDistance(camera.x, camera.z, tileCenterX, tileCenterZ);
            if (current) {
                double allowed = tile.cellSize() <= 1 ? 1536.0
                        : tile.cellSize() <= 4 ? 3072.0
                        : tile.cellSize() <= 16 ? Math.max(4096.0, lodRadius)
                        : Double.POSITIVE_INFINITY;
                if (tileDistance > allowed) {
                    continue;
                }
            }

            int step = tile.exact() ? 1
                    : tile.cellSize() >= 128 ? (quality >= 2 ? 1 : 2)
                    : quality == 1 ? Math.max(1, tile.cellSize() >= 16 ? 2 : 1)
                    : quality == 2 && tile.cellSize() >= 64 ? 2 : 1;
            double nudge = tile.exact() ? 0.14
                    : tile.cellSize() <= 4 ? 0.10
                    : tile.cellSize() <= 16 ? 0.065 : 0.03;

            for (int z = 0; z < SparsePlanetLodTile.RESOLUTION; z += step) {
                int z1 = Math.min(SparsePlanetLodTile.RESOLUTION, z + step);
                for (int x = 0; x < SparsePlanetLodTile.RESOLUTION; x += step) {
                    int x1 = Math.min(SparsePlanetLodTile.RESOLUTION, x + step);
                    int index = tile.index(x, z);
                    int colour = tile.colours()[index];
                    if ((colour >>> 24) == 0) continue;
                    double wx0 = tile.originX() + (double) x * tile.cellSize();
                    double wx1 = tile.originX() + (double) x1 * tile.cellSize();
                    double wz0 = tile.originZ() + (double) z * tile.cellSize();
                    double wz1 = tile.originZ() + (double) z1 * tile.cellSize();
                    double centerX = (wx0 + wx1) * 0.5;
                    double centerZ = (wz0 + wz1) * 0.5;
                    float alpha = baseAlpha;
                    if (current) {
                        double distance = horizontalDistance(camera.x, camera.z, centerX, centerZ);
                        double near0 = Math.max(0.0, vanillaGroundRadius * 0.72 - 48.0);
                        double near1 = Math.max(32.0, vanillaGroundRadius * 1.12 + 24.0);
                        float outerBand = distanceBand(distance, near0, near1,
                                lodRadius * 0.995, lodRadius);
                        alpha *= Math.max(outerBand, Math.max(foldAlpha, centerFillAlpha));
                    }
                    if (alpha <= 0.002f) continue;

                    int height = tile.heights()[index];
                    Vector3d p00 = livePoint(tile.face(), currentFace, transform, surface,
                            wx0, wz0, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, nudge);
                    Vector3d p10 = livePoint(tile.face(), currentFace, transform, surface,
                            wx1, wz0, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, nudge);
                    Vector3d p11 = livePoint(tile.face(), currentFace, transform, surface,
                            wx1, wz1, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, nudge);
                    Vector3d p01 = livePoint(tile.face(), currentFace, transform, surface,
                            wx0, wz1, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, nudge);
                    addVertex(buffer, view, camera, p01, renderScale, colour, alpha);
                    addVertex(buffer, view, camera, p11, renderScale, colour, alpha);
                    addVertex(buffer, view, camera, p10, renderScale, colour, alpha);
                    addVertex(buffer, view, camera, p00, renderScale, colour, alpha);

                    if (tile.cellSize() <= 4 && step == 1) {
                        int eastHeight = x1 < SparsePlanetLodTile.RESOLUTION
                                ? tile.heights()[tile.index(x1, z)] : height;
                        if (eastHeight != height) {
                            addLiveWall(buffer, view, camera, tile.face(), currentFace, transform, surface,
                                    wx1, wz0, wx1, wz1, height, eastHeight, foldAlpha,
                                    currentCenterX, cubeCenterY, currentCenterZ, renderScale,
                                    darken(colour, 0.70), alpha);
                        }
                        int southHeight = z1 < SparsePlanetLodTile.RESOLUTION
                                ? tile.heights()[tile.index(x, z1)] : height;
                        if (southHeight != height) {
                            addLiveWall(buffer, view, camera, tile.face(), currentFace, transform, surface,
                                    wx0, wz1, wx1, wz1, height, southHeight, foldAlpha,
                                    currentCenterX, cubeCenterY, currentCenterZ, renderScale,
                                    darken(colour, 0.79), alpha);
                        }
                    }
                }
            }
            rendered++;
        }
    }


    /**
     * Draws exact DH chunk columns over the coarse planetary atlas.
     *
     * <p>The base face has one sample per 128 blocks; these patches retain one
     * sample per Minecraft block. Side skirts at height discontinuities keep
     * buildings and cliffs visibly voxel-shaped instead of smoothing them into
     * a coloured sheet.</p>
     */
    private static void renderLiveChunkPatches(VertexConsumer buffer, Matrix4f view, Vec3 camera,
                                               CubeNetSurfaceTransform transform,
                                               CubeNetSurfaceTransform.Face currentFace,
                                               PlanetSurfaceData surface,
                                               float lodAlpha, float foldAlpha,
                                               float centerFillAlpha,
                                               int vanillaRadius,
                                               double vanillaGroundRadius) {
        DhLiveChunkPatchCache.Patch[] patches = DhLiveChunkPatchCache.snapshot();
        if (patches.length == 0) return;

        double currentCenterX = transform.faceCenterX(currentFace);
        double currentCenterZ = transform.faceCenterZ(currentFace);
        double cubeCenterY = surface.seaLevel() - transform.faceHalfSpan();
        double renderScale = renderDistanceCompression(
                camera, currentCenterX, cubeCenterY, currentCenterZ, vanillaRadius);
        int lodRadius = GenesisClientConfig.getWorldLodRadius();

        List<DhLiveChunkPatchCache.Patch> candidates = new ArrayList<>();
        for (DhLiveChunkPatchCache.Patch patch : patches) {
            if (patch.face() != currentFace) continue;
            if (PlanetLodVolumeClientCache.hasTileAt(
                    SparsePlanetLodService.EARTH_ID, patch.face(),
                    patch.worldMinX() + 8.0, patch.worldMinZ() + 8.0)) {
                continue; // textured 3D volume tile replaces this flat patch
            }
            if (SparsePlanetLodClientCache.hasExactChunk(
                    SparsePlanetLodService.EARTH_ID, patch.face(), patch.chunkX(), patch.chunkZ())) {
                continue; // same exact chunk already arrived from the server
            }
            double centerX = patch.worldMinX() + 8.0;
            double centerZ = patch.worldMinZ() + 8.0;
            if (horizontalDistance(camera.x, camera.z, centerX, centerZ) <= 1536.0) {
                candidates.add(patch);
            }
        }
        candidates.sort(Comparator.comparingDouble(patch -> {
            double dx = patch.worldMinX() + 8.0 - camera.x;
            double dz = patch.worldMinZ() + 8.0 - camera.z;
            return dx * dx + dz * dz;
        }));

        int maximum = Math.min(96, candidates.size());
        int step = GenesisClientConfig.getWorldLodQuality() >= 3 ? 1 : 2;
        for (int patchIndex = 0; patchIndex < maximum; patchIndex++) {
            DhLiveChunkPatchCache.Patch patch = candidates.get(patchIndex);
            int worldMinX = patch.worldMinX();
            int worldMinZ = patch.worldMinZ();
            for (int z = 0; z < DhLiveChunkPatchCache.PATCH_RESOLUTION; z += step) {
                int z1 = Math.min(DhLiveChunkPatchCache.PATCH_RESOLUTION, z + step);
                for (int x = 0; x < DhLiveChunkPatchCache.PATCH_RESOLUTION; x += step) {
                    int x1 = Math.min(DhLiveChunkPatchCache.PATCH_RESOLUTION, x + step);
                    int index = patch.index(x, z);
                    int colour = patch.colours()[index];
                    if ((colour >>> 24) == 0) continue;
                    double centerX = worldMinX + (x + x1) * 0.5;
                    double centerZ = worldMinZ + (z + z1) * 0.5;
                    double distance = horizontalDistance(camera.x, camera.z, centerX, centerZ);
                    double near0 = Math.max(0.0, vanillaGroundRadius * 0.72 - 48.0);
                    double near1 = Math.max(32.0, vanillaGroundRadius * 1.12 + 24.0);
                    float outerBand = distanceBand(distance, near0, near1,
                            lodRadius * 0.98, lodRadius);
                    float alpha = lodAlpha * Math.max(outerBand, Math.max(foldAlpha, centerFillAlpha));
                    if (alpha <= 0.002f) continue;

                    int height = patch.heights()[index];
                    double wx0 = worldMinX + x;
                    double wx1 = worldMinX + x1;
                    double wz0 = worldMinZ + z;
                    double wz1 = worldMinZ + z1;
                    Vector3d p00 = livePoint(patch.face(), currentFace, transform, surface,
                            wx0, wz0, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.16);
                    Vector3d p10 = livePoint(patch.face(), currentFace, transform, surface,
                            wx1, wz0, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.16);
                    Vector3d p11 = livePoint(patch.face(), currentFace, transform, surface,
                            wx1, wz1, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.16);
                    Vector3d p01 = livePoint(patch.face(), currentFace, transform, surface,
                            wx0, wz1, height, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.16);
                    addVertex(buffer, view, camera, p01, renderScale, colour, alpha);
                    addVertex(buffer, view, camera, p11, renderScale, colour, alpha);
                    addVertex(buffer, view, camera, p10, renderScale, colour, alpha);
                    addVertex(buffer, view, camera, p00, renderScale, colour, alpha);

                    if (step == 1) {
                        int eastHeight = x1 < DhLiveChunkPatchCache.PATCH_RESOLUTION
                                ? patch.heights()[patch.index(x1, z)] : height;
                        if (eastHeight != height) {
                            addLiveWall(buffer, view, camera, patch.face(), currentFace, transform, surface,
                                    wx1, wz0, wx1, wz1, height, eastHeight, foldAlpha,
                                    currentCenterX, cubeCenterY, currentCenterZ, renderScale,
                                    darken(colour, 0.68), alpha);
                        }
                        int southHeight = z1 < DhLiveChunkPatchCache.PATCH_RESOLUTION
                                ? patch.heights()[patch.index(x, z1)] : height;
                        if (southHeight != height) {
                            addLiveWall(buffer, view, camera, patch.face(), currentFace, transform, surface,
                                    wx0, wz1, wx1, wz1, height, southHeight, foldAlpha,
                                    currentCenterX, cubeCenterY, currentCenterZ, renderScale,
                                    darken(colour, 0.78), alpha);
                        }
                    }
                }
            }
        }
    }

    private static void addLiveWall(VertexConsumer buffer, Matrix4f view, Vec3 camera,
                                    CubeNetSurfaceTransform.Face face,
                                    CubeNetSurfaceTransform.Face currentFace,
                                    CubeNetSurfaceTransform transform, PlanetSurfaceData surface,
                                    double x0, double z0, double x1, double z1,
                                    int heightA, int heightB, float foldAlpha,
                                    double currentCenterX, double cubeCenterY, double currentCenterZ,
                                    double renderScale, int colour, float alpha) {
        int low = Math.min(heightA, heightB);
        int high = Math.max(heightA, heightB);
        Vector3d p0l = livePoint(face, currentFace, transform, surface, x0, z0, low,
                foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.08);
        Vector3d p1l = livePoint(face, currentFace, transform, surface, x1, z1, low,
                foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.08);
        Vector3d p1h = livePoint(face, currentFace, transform, surface, x1, z1, high,
                foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.08);
        Vector3d p0h = livePoint(face, currentFace, transform, surface, x0, z0, high,
                foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.08);
        addVertex(buffer, view, camera, p0l, renderScale, colour, alpha);
        addVertex(buffer, view, camera, p1l, renderScale, colour, alpha);
        addVertex(buffer, view, camera, p1h, renderScale, colour, alpha);
        addVertex(buffer, view, camera, p0h, renderScale, colour, alpha);
    }

    private static Vector3d livePoint(CubeNetSurfaceTransform.Face face,
                                      CubeNetSurfaceTransform.Face currentFace,
                                      CubeNetSurfaceTransform transform, PlanetSurfaceData surface,
                                      double worldX, double worldZ, double height,
                                      float foldAlpha,
                                      double currentCenterX, double cubeCenterY, double currentCenterZ,
                                      double outwardNudge) {
        double u = worldX - transform.faceCenterX(face);
        double v = worldZ - transform.faceCenterZ(face);
        double elevation = height - surface.seaLevel() + outwardNudge;
        Vector3d celestial = CubeFaceFrame.cubePoint(face, u, v,
                transform.faceHalfSpan(), elevation);
        Vector3d folded = CubeFaceFrame.celestialToSurface(currentFace, celestial)
                .add(currentCenterX, cubeCenterY, currentCenterZ);
        return new Vector3d(
                Mth.lerp(foldAlpha, worldX, folded.x),
                Mth.lerp(foldAlpha, height + outwardNudge, folded.y),
                Mth.lerp(foldAlpha, worldZ, folded.z));
    }

    private static int coarseHeight(PlanetSurfaceData surface, CubeNetSurfaceTransform.Face face,
                                    CubeNetSurfaceTransform transform, double worldX, double worldZ) {
        int resolution = PlanetSurfaceSampler.RESOLUTION;
        double localX = worldX - transform.faceCenterX(face);
        double localZ = worldZ - transform.faceCenterZ(face);
        int sampleX = Mth.clamp((int) Math.floor((localX / transform.faceSpan() + 0.5) * resolution),
                0, resolution - 1);
        int sampleZ = Mth.clamp((int) Math.floor((localZ / transform.faceSpan() + 0.5) * resolution),
                0, resolution - 1);
        return surface.heights()[PlanetSurfaceData.index(face, sampleX, sampleZ)];
    }

    private static int darken(int argb, double factor) {
        int r = Mth.clamp((int) (((argb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp((int) (((argb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((argb & 0xFF) * factor), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static Vector3d blendedPoint(CubeNetSurfaceTransform.Face face,
                                         CubeNetSurfaceTransform.Face currentFace,
                                         CubeNetSurfaceTransform transform,
                                         PlanetSurfaceData surface,
                                         int gridX, int gridZ, double u, double v,
                                         float foldAlpha,
                                         double currentCenterX, double cubeCenterY, double currentCenterZ) {
        double elevation = elevation(surface, face, gridX, gridZ);
        double flatX = transform.faceCenterX(face) + u;
        double flatY = surface.seaLevel() + elevation;
        double flatZ = transform.faceCenterZ(face) + v;

        Vector3d celestial = CubeFaceFrame.cubePoint(face, u, v,
                transform.faceHalfSpan(), elevation);
        Vector3d folded = CubeFaceFrame.celestialToSurface(currentFace, celestial)
                .add(currentCenterX, cubeCenterY, currentCenterZ);

        return new Vector3d(
                Mth.lerp(foldAlpha, flatX, folded.x),
                Mth.lerp(foldAlpha, flatY, folded.y),
                Mth.lerp(foldAlpha, flatZ, folded.z));
    }

    /**
     * Moves the entire distant world uniformly toward the camera when it would
     * be clipped. Uniform scaling of every camera-relative vector preserves the
     * apparent size and direction exactly; unlike shrinking a standalone model,
     * the terrain still occupies the same pixels and remains continuous across
     * the dimension handoff.
     */
    private static double renderDistanceCompression(Vec3 camera,
                                                    double centerX, double centerY, double centerZ,
                                                    int vanillaRadius) {
        double dx = centerX - camera.x;
        double dy = centerY - camera.y;
        double dz = centerZ - camera.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double target = Math.max(640.0, vanillaRadius * 3.25);
        double fullScale = distance <= target ? 1.0 : target / distance;

        double start = Math.max(512.0, GenesisClientConfig.getWorldLodStartHeight());
        double full = Math.max(start + 1.0, GenesisClientConfig.getWorldCubeCompressionFullHeight());
        float influence = smoothstep((float) ((camera.y - start) / (full - start)));
        return Mth.lerp(influence, 1.0, fullScale);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f view, Vec3 camera,
                                  Vector3d world, double renderScale, int argb, float alpha) {
        double x = (world.x - camera.x) * renderScale;
        double y = (world.y - camera.y) * renderScale;
        double z = (world.z - camera.z) * renderScale;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        buffer.addVertex(view, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    private static double elevation(PlanetSurfaceData surface, CubeNetSurfaceTransform.Face face,
                                    int x, int z) {
        int resolution = PlanetSurfaceSampler.RESOLUTION;
        int sx = Mth.clamp(x, 0, resolution - 1);
        int sz = Mth.clamp(z, 0, resolution - 1);
        return surface.heights()[PlanetSurfaceData.index(face, sx, sz)] - surface.seaLevel();
    }

    private static int colour(PlanetSurfaceData surface, CubeNetSurfaceTransform.Face face,
                              int x, int z) {
        int resolution = PlanetSurfaceSampler.RESOLUTION;
        int sx = Mth.clamp(x, 0, resolution - 1);
        int sz = Mth.clamp(z, 0, resolution - 1);
        return surface.colours()[PlanetSurfaceData.index(face, sx, sz)];
    }

    private static double gridCoordinate(int index, int resolution, double faceSpan) {
        return (index / (double) (resolution - 1) - 0.5) * faceSpan;
    }

    private static float cubeFoldAlpha(double cameraY) {
        double start = GenesisClientConfig.getWorldCubeFoldStartHeight();
        double end = Math.max(start + 1.0, GenesisClientConfig.getCurrentPlanetRevealHeight());
        return smoothstep((float) ((cameraY - start) / (end - start)));
    }

    private static float distanceBand(double distance, double near0, double near1,
                                      double far0, double far1) {
        float near = smoothstep((float) ((distance - near0) / Math.max(1.0, near1 - near0)));
        float far = 1.0f - smoothstep((float) ((distance - far0) / Math.max(1.0, far1 - far0)));
        return Mth.clamp(near * far, 0.0f, 1.0f);
    }

    private static double vanillaGroundRadius(double cameraY, double terrainCeiling, double renderRadius) {
        double vertical = Math.max(0.0, cameraY - terrainCeiling);
        double squared = renderRadius * renderRadius - vertical * vertical;
        return squared <= 0.0 ? 0.0 : Math.sqrt(squared);
    }

    private static double horizontalDistance(double x0, double z0, double x1, double z1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }
}
