package shipwrights.genesis.client.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.OrbitalSurveyController;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.voxel.PlanetVoxelBrick;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickKey;
import shipwrights.genesis.space.voxel.PlanetVoxelGreedyMesher;
import shipwrights.genesis.space.voxel.PlanetVoxelMaterial;
import shipwrights.genesis.space.voxel.PlanetVoxelLodSelector;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clean-room 3D voxel-clipmap renderer for Earth.
 *
 * <p>Unlike the legacy height/color layer, every rendered item is a real
 * palette-compressed 16^3 voxel brick. LOD0 keeps actual blocks and structures;
 * coarser bricks are occupancy-aware 3D mip levels. The same bricks are drawn
 * flat during ascent, folded onto the cube, and then continued in orbit.</p>
 */
public final class PlanetVoxelRenderer {
    private static final ResourceLocation EARTH_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final RenderType SOLID_TYPE =
            RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType TRANSLUCENT_TYPE =
            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final ByteBufferBuilder SOLID_MEMORY = new ByteBufferBuilder(64 * 1024 * 1024);
    private static final ByteBufferBuilder TRANSLUCENT_MEMORY = new ByteBufferBuilder(16 * 1024 * 1024);
    private static final Object BUFFER_LOCK = new Object();
    private static final Object GPU_LOCK = new Object();
    private static final int MAX_GPU_UPLOADS_PER_FRAME = 8;
    private static final int MAX_PENDING_GPU_UPLOADS = 2048;
    private static final long GPU_BUDGET_BYTES = 384L * 1024L * 1024L;
    private static final LinkedHashMap<GpuKey, GpuEntry> GPU_CACHE =
            new LinkedHashMap<>(1024, 0.75f, true);
    private static final LinkedHashMap<GpuKey, PendingGpuUpload> GPU_UPLOADS =
            new LinkedHashMap<>();
    private static final List<GpuEntry> GPU_DISPOSALS = new ArrayList<>();
    private static long gpuBytes;
    private static long orbitResidentGeneration = Long.MIN_VALUE;
    private static List<PlanetVoxelBrick> orbitResidentBricks = List.of();
    private static List<PlanetVoxelLodSelector.Selection> orbitViewportSelection = List.of();
    private static Set<PlanetVoxelBrickKey> orbitPreviousSelection = Set.of();
    private static final Vector3d orbitLastCamera = new Vector3d(Double.NaN);
    private static final Vector3d orbitLastLook = new Vector3d(Double.NaN);
    private static double orbitLastWorldToRendered = Double.NaN;
    private static double orbitLastFov = Double.NaN;
    private static int orbitLastViewportHeight = -1;
    private static int orbitLastMaximumBricks = -1;
    private static int orbitSelectionAge = Integer.MAX_VALUE;
    private static long coverageGeneration = Long.MIN_VALUE;
    private static double coverageHalfExtent = Double.NaN;
    private static boolean completeCoarseCoverage;
    private static boolean loggedCompleteCoverage;
    private static volatile boolean disabled;
    private static volatile boolean loggedOverworld;
    private static volatile boolean loggedOrbit;

    private PlanetVoxelRenderer() {
    }

    public static boolean hasVoxelData() {
        return PlanetVoxelClientCache.hasAny(EARTH_ID);
    }

    public static boolean hasCompleteCoarseCoverage(CubeNetSurfaceTransform transform) {
        if (!hasVoxelData()) return false;
        long generation = PlanetVoxelClientCache.generation();
        double halfExtent = transform.faceHalfSpan();
        if (generation != coverageGeneration || coverageHalfExtent != halfExtent) {
            PlanetVoxelCoverage.Result result = PlanetVoxelCoverage.analyze(
                    PlanetVoxelClientCache.snapshot(EARTH_ID, key -> true, 100_000),
                    halfExtent);
            completeCoarseCoverage = result.complete();
            coverageGeneration = generation;
            coverageHalfExtent = halfExtent;
            if (completeCoarseCoverage && !loggedCompleteCoverage) {
                loggedCompleteCoverage = true;
                GenesisMod.LOGGER.info("[PLANET-VOXEL] all six Earth faces have coarse 3D occupancy "
                        + "coverage at LOD {}; retiring temporary legacy fallback", result.lod());
            }
        }
        return completeCoarseCoverage;
    }

    public static void requestOverworldInterest(Vec3 camera, Vector3f surfaceLook,
                                                CubeNetSurfaceTransform transform,
                                                CubeNetSurfaceTransform.Face currentFace,
                                                int seaLevel) {
        int quality = GenesisClientConfig.getWorldLodQuality();
        int maximumBricks = quality >= 3 ? 4096 : quality == 2 ? 2300 : 1200;
        double targetVoxelPixels = quality >= 3 ? 1.10 : quality == 2 ? 1.75 : 2.75;
        OrbitProjection projection = orbitProjection();
        Vector3d localCamera = CubeFaceFrame.cubePoint(currentFace,
                camera.x - transform.faceCenterX(currentFace),
                camera.z - transform.faceCenterZ(currentFace),
                transform.faceHalfSpan(), camera.y - seaLevel);
        Vector3d localLook = new Vector3d(surfaceLook.x, surfaceLook.y, surfaceLook.z)
                .rotate(CubeFaceFrame.surfaceToCelestial(currentFace));
        PlanetVoxelInterestClient.update(localCamera, localLook,
                projection.viewportHeight(), projection.aspectRatio(),
                projection.verticalFovRadians(), targetVoxelPixels, maximumBricks);
    }

    public static void renderOverworld(Matrix4f view, Vec3 camera,
                                       CubeNetSurfaceTransform transform,
                                       CubeNetSurfaceTransform.Face currentFace,
                                       PlanetSurfaceData surface,
                                       float lodAlpha, float foldAlpha,
                                       float centerFillAlpha, int vanillaRadius,
                                       double vanillaGroundRadius) {
        if (disabled || lodAlpha < 0.04f || !hasVoxelData()) return;
        int quality = GenesisClientConfig.getWorldLodQuality();
        int maxLod = quality >= 3 ? 7 : quality == 2 ? 6 : 5;
        int maxBricks = quality >= 3 ? 1400 : quality == 2 ? 800 : 420;
        double exactRadius = quality >= 3 ? 1024.0 : quality == 2 ? 640.0 : 384.0;

        List<PlanetVoxelBrick> bricks = PlanetVoxelClientCache.snapshot(EARTH_ID,
                key -> key.lod() <= maxLod
                        && (key.face() == currentFace || foldAlpha > 0.15f),
                20_000);
        if (bricks.isEmpty()) return;
        List<Selection> selected = select(bricks, transform, currentFace,
                camera.x, camera.z, exactRadius, maxLod, maxBricks, false);
        if (selected.isEmpty()) return;

        double currentCenterX = transform.faceCenterX(currentFace);
        double currentCenterZ = transform.faceCenterZ(currentFace);
        double cubeCenterY = surface.seaLevel() - transform.faceHalfSpan();
        double renderScale = renderDistanceCompression(camera, currentCenterX, cubeCenterY,
                currentCenterZ, vanillaRadius);

        int rendered = renderWithBuffers((solid, translucent) -> {
            int count = 0;
            for (Selection selection : selected) {
                PlanetVoxelGreedyMesher.Mesh mesh = PlanetVoxelMeshCache.getOrSchedule(
                        EARTH_ID, selection.brick);
                if (mesh == null) continue;
                float faceAlpha = selection.brick.key().face() == currentFace
                        ? Math.max(lodAlpha, centerFillAlpha) : foldAlpha;
                if (faceAlpha < 0.03f) continue;
                renderMesh(view, solid, translucent, selection.brick, mesh,
                        point -> livePoint(selection.brick.key().face(), currentFace,
                                transform, surface, point.u, point.y, point.v,
                                foldAlpha, currentCenterX, cubeCenterY, currentCenterZ),
                        camera, renderScale, faceAlpha);
                count++;
            }
            return count;
        });
        if (rendered > 0 && !loggedOverworld) {
            loggedOverworld = true;
            GenesisMod.LOGGER.info("[PLANET-VOXEL] real 3D voxel bricks visible during ascent; "
                    + "{} bricks selected from {} MiB client cache", rendered,
                    PlanetVoxelClientCache.usedBytes(EARTH_ID) / (1024L * 1024L));
        }
    }

    public static void renderOrbit(Matrix4f view,
                                   CubeNetSurfaceTransform transform,
                                   Vector3d planetPosition, Quaterniondc rotation,
                                   double renderedHalfExtent, double worldToRendered,
                                   Vector3d localCamera, Vector3d localLook,
                                   CubeNetSurfaceTransform.Face focusFace,
                                   double apparentHalf, int seaLevel) {
        if (disabled) return;
        boolean survey = OrbitalSurveyController.isActive();
        int quality = GenesisClientConfig.getWorldLodQuality();
        int maximumBricks = survey ? 6144 : quality >= 3 ? 4096 : quality == 2 ? 2300 : 1200;
        double targetVoxelPixels = survey ? 0.72 : quality >= 3 ? 1.10 : quality == 2 ? 1.75 : 2.75;
        OrbitProjection projection = orbitProjection();
        PlanetVoxelInterestClient.update(new Vector3d(localCamera).div(worldToRendered), localLook,
                projection.viewportHeight(), projection.aspectRatio(),
                projection.verticalFovRadians(), targetVoxelPixels, maximumBricks);
        if (!hasVoxelData()) return;
        long generation = PlanetVoxelClientCache.generation();
        if (generation != orbitResidentGeneration) {
            orbitResidentBricks = PlanetVoxelClientCache.snapshot(EARTH_ID,
                    key -> true, 100_000);
            orbitResidentGeneration = generation;
            orbitSelectionAge = Integer.MAX_VALUE;
        }
        if (orbitResidentBricks.isEmpty()) return;

        boolean viewChanged = orbitViewportSelection.isEmpty()
                || orbitLastCamera.distanceSquared(localCamera) > 0.25
                || orbitLastLook.dot(localLook) < 0.9995
                || relativeDifference(orbitLastWorldToRendered, worldToRendered) > 0.001
                || Math.abs(orbitLastFov - projection.verticalFovRadians()) > 0.001
                || orbitLastViewportHeight != projection.viewportHeight()
                || orbitLastMaximumBricks != maximumBricks;
        if ((orbitSelectionAge >= 2 && viewChanged)
                || orbitSelectionAge == Integer.MAX_VALUE) {
            PlanetVoxelLodSelector.OrbitView orbitView = new PlanetVoxelLodSelector.OrbitView(
                    localCamera, localLook, transform.faceHalfSpan(), worldToRendered,
                    seaLevel, projection.viewportHeight(), projection.aspectRatio(),
                    projection.verticalFovRadians(), targetVoxelPixels,
                    PlanetVoxelBrickKey.MAX_LOD, maximumBricks, true);
            orbitViewportSelection = PlanetVoxelLodSelector.selectOrbit(
                    orbitResidentBricks, orbitView, orbitPreviousSelection);
            orbitPreviousSelection = orbitViewportSelection.stream()
                    .map(selection -> selection.brick().key())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            orbitLastCamera.set(localCamera);
            orbitLastLook.set(localLook);
            orbitLastWorldToRendered = worldToRendered;
            orbitLastFov = projection.verticalFovRadians();
            orbitLastViewportHeight = projection.viewportHeight();
            orbitLastMaximumBricks = maximumBricks;
            orbitSelectionAge = 0;
        } else {
            orbitSelectionAge++;
        }

        List<Selection> selected = orbitViewportSelection.stream()
                .map(selection -> new Selection(selection.brick(), selection.distance()))
                .toList();
        if (selected.isEmpty()) return;

        for (Selection selection : selected) {
            PlanetVoxelGreedyMesher.Mesh mesh = PlanetVoxelMeshCache.getOrSchedule(
                    EARTH_ID, selection.brick);
            if (mesh != null) queueGpuUpload(selection.brick, mesh);
        }
        drainGpuDisposals();
        drainGpuUploads(transform, seaLevel);
        double renderScale = renderedHalfExtent / Math.max(1.0, transform.faceHalfSpan());
        int rendered = drawGpuOrbit(view, planetPosition, rotation, renderScale, selected);
        if (rendered > 0 && !loggedOrbit) {
            loggedOrbit = true;
            GenesisMod.LOGGER.info("[PLANET-VOXEL] real 3D voxel clipmap visible in orbit; "
                    + "{} bricks on/around {}", rendered, focusFace);
        }
    }

    public static void reset() {
        disabled = false;
        loggedOverworld = false;
        loggedOrbit = false;
        orbitResidentGeneration = Long.MIN_VALUE;
        orbitResidentBricks = List.of();
        orbitViewportSelection = List.of();
        orbitPreviousSelection = Set.of();
        orbitLastCamera.set(Double.NaN);
        orbitLastLook.set(Double.NaN);
        orbitLastWorldToRendered = Double.NaN;
        orbitLastFov = Double.NaN;
        orbitLastViewportHeight = -1;
        orbitLastMaximumBricks = -1;
        orbitSelectionAge = Integer.MAX_VALUE;
        coverageGeneration = Long.MIN_VALUE;
        coverageHalfExtent = Double.NaN;
        completeCoarseCoverage = false;
        loggedCompleteCoverage = false;
        PlanetVoxelInterestClient.reset();
        PlanetVoxelMeshCache.clear();
        synchronized (GPU_LOCK) {
            GPU_DISPOSALS.addAll(GPU_CACHE.values());
            GPU_CACHE.clear();
            GPU_UPLOADS.clear();
            gpuBytes = 0L;
        }
        if (RenderSystem.isOnRenderThread()) {
            drainGpuDisposals();
        } else {
            RenderSystem.recordRenderCall(PlanetVoxelRenderer::drainGpuDisposals);
        }
    }

    static void invalidateGpu(ResourceLocation planet, PlanetVoxelBrickKey key) {
        synchronized (GPU_LOCK) {
            GpuKey gpuKey = new GpuKey(planet, key);
            GpuEntry removed = GPU_CACHE.remove(gpuKey);
            if (removed != null) {
                gpuBytes -= removed.estimatedBytes;
                GPU_DISPOSALS.add(removed);
            }
            GPU_UPLOADS.remove(gpuKey);
        }
    }

    private static void queueGpuUpload(PlanetVoxelBrick brick,
                                       PlanetVoxelGreedyMesher.Mesh mesh) {
        GpuKey key = new GpuKey(EARTH_ID, brick.key());
        synchronized (GPU_LOCK) {
            GpuEntry resident = GPU_CACHE.get(key);
            if (resident != null && resident.revision == brick.revision()) return;
            if (resident != null) {
                GPU_CACHE.remove(key);
                gpuBytes -= resident.estimatedBytes;
                GPU_DISPOSALS.add(resident);
            }
            PendingGpuUpload pending = GPU_UPLOADS.get(key);
            if (pending == null || pending.brick.revision() < brick.revision()) {
                GPU_UPLOADS.put(key, new PendingGpuUpload(brick, mesh));
            }
            while (GPU_UPLOADS.size() > MAX_PENDING_GPU_UPLOADS) {
                GpuKey eldest = GPU_UPLOADS.keySet().iterator().next();
                GPU_UPLOADS.remove(eldest);
            }
        }
    }

    private static void drainGpuUploads(CubeNetSurfaceTransform transform, int seaLevel) {
        RenderSystem.assertOnRenderThread();
        for (int uploaded = 0; uploaded < MAX_GPU_UPLOADS_PER_FRAME; uploaded++) {
            GpuKey key;
            PendingGpuUpload pending;
            synchronized (GPU_LOCK) {
                if (GPU_UPLOADS.isEmpty()) break;
                Map.Entry<GpuKey, PendingGpuUpload> first =
                        GPU_UPLOADS.entrySet().iterator().next();
                key = first.getKey();
                pending = first.getValue();
                GPU_UPLOADS.remove(key);
            }
            PlanetVoxelBrick current = PlanetVoxelClientCache.get(key.planet, key.key);
            if (current == null || current.revision() != pending.brick.revision()) continue;
            GpuEntry entry;
            try {
                entry = uploadGpuMesh(pending, transform, seaLevel);
            } catch (Throwable error) {
                GenesisMod.LOGGER.warn("[PLANET-VOXEL] failed to upload GPU mesh {}",
                        key.key, error);
                continue;
            }
            synchronized (GPU_LOCK) {
                GpuEntry previous = GPU_CACHE.put(key, entry);
                if (previous != null) {
                    gpuBytes -= previous.estimatedBytes;
                    GPU_DISPOSALS.add(previous);
                }
                gpuBytes += entry.estimatedBytes;
                trimGpuCache();
            }
        }
    }

    private static GpuEntry uploadGpuMesh(PendingGpuUpload pending,
                                          CubeNetSurfaceTransform transform,
                                          int seaLevel) {
        int vertexBytes = DefaultVertexFormat.NEW_ENTITY.getVertexSize();
        int estimated = Math.max(1024,
                pending.mesh.quadCount() * 4 * vertexBytes);
        ByteBufferBuilder solidMemory = new ByteBufferBuilder(estimated);
        ByteBufferBuilder translucentMemory = new ByteBufferBuilder(
                Math.max(1024, estimated / 4));
        try {
            BufferBuilder solid = new BufferBuilder(
                    solidMemory, SOLID_TYPE.mode(), SOLID_TYPE.format());
            BufferBuilder translucent = new BufferBuilder(
                    translucentMemory, TRANSLUCENT_TYPE.mode(), TRANSLUCENT_TYPE.format());
            PlanetVoxelBrickKey key = pending.brick.key();
            renderMesh(new Matrix4f(), solid, translucent, pending.brick, pending.mesh,
                    point -> CubeFaceFrame.cubePoint(key.face(), point.u, point.v,
                            transform.faceHalfSpan(), point.y - seaLevel + 0.72),
                    null, 1.0, 1.0f);
            VertexBuffer solidBuffer = uploadBuffer(solid);
            VertexBuffer translucentBuffer = uploadBuffer(translucent);
            long bytes = (long) pending.mesh.quadCount() * 4L * vertexBytes;
            return new GpuEntry(pending.brick.revision(), solidBuffer,
                    translucentBuffer, bytes);
        } finally {
            solidMemory.close();
            translucentMemory.close();
        }
    }

    private static VertexBuffer uploadBuffer(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh == null) return null;
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            buffer.bind();
            buffer.upload(mesh);
            return buffer;
        } catch (RuntimeException | Error error) {
            buffer.close();
            throw error;
        } finally {
            VertexBuffer.unbind();
            mesh.close();
        }
    }

    private static int drawGpuOrbit(Matrix4f view, Vector3d planetPosition,
                                    Quaterniondc rotation, double scale,
                                    List<Selection> selected) {
        RenderSystem.assertOnRenderThread();
        List<GpuDraw> draws = new ArrayList<>(selected.size());
        synchronized (GPU_LOCK) {
            for (Selection selection : selected) {
                GpuEntry entry = GPU_CACHE.get(new GpuKey(EARTH_ID, selection.brick.key()));
                if (entry != null && entry.revision == selection.brick.revision()) {
                    draws.add(new GpuDraw(entry, selection.distance));
                }
            }
        }
        if (draws.isEmpty()) return 0;
        Matrix4f modelView = new Matrix4f(view)
                .translate((float) planetPosition.x, (float) planetPosition.y,
                        (float) planetPosition.z)
                .rotate(new Quaternionf(rotation))
                .scale((float) scale);
        drawGpuLayer(SOLID_TYPE, modelView, draws, false);
        drawGpuLayer(TRANSLUCENT_TYPE, modelView, draws, true);
        return draws.size();
    }

    private static OrbitProjection orbitProjection() {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(1, minecraft.getWindow().getWidth());
        int height = Math.max(1, minecraft.getWindow().getHeight());
        double projectionScale = Math.abs(RenderSystem.getProjectionMatrix().m11());
        double verticalFov = projectionScale > 1.0E-6
                ? 2.0 * Math.atan(1.0 / projectionScale)
                : Math.toRadians(70.0);
        return new OrbitProjection(height, (double) width / height, verticalFov);
    }

    private static double relativeDifference(double a, double b) {
        if (!Double.isFinite(a) || !Double.isFinite(b)) return Double.POSITIVE_INFINITY;
        return Math.abs(a - b) / Math.max(1.0E-9, Math.max(Math.abs(a), Math.abs(b)));
    }

    private static void drawGpuLayer(RenderType type, Matrix4f modelView,
                                     List<GpuDraw> draws, boolean translucent) {
        type.setupRenderState();
        try {
            ShaderInstance shader = RenderSystem.getShader();
            if (shader == null) return;
            Matrix4f projection = RenderSystem.getProjectionMatrix();
            if (translucent) {
                for (int index = draws.size() - 1; index >= 0; index--) {
                    drawGpuBuffer(draws.get(index).entry.translucent, modelView,
                            projection, shader);
                }
            } else {
                for (GpuDraw draw : draws) {
                    drawGpuBuffer(draw.entry.solid, modelView, projection, shader);
                }
            }
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private static void drawGpuBuffer(VertexBuffer buffer, Matrix4f modelView,
                                      Matrix4f projection, ShaderInstance shader) {
        if (buffer == null) return;
        buffer.bind();
        buffer.drawWithShader(modelView, projection, shader);
    }

    private static void trimGpuCache() {
        var iterator = GPU_CACHE.entrySet().iterator();
        while (gpuBytes > GPU_BUDGET_BYTES && iterator.hasNext()) {
            GpuEntry removed = iterator.next().getValue();
            iterator.remove();
            gpuBytes -= removed.estimatedBytes;
            GPU_DISPOSALS.add(removed);
        }
    }

    private static void drainGpuDisposals() {
        RenderSystem.assertOnRenderThread();
        List<GpuEntry> disposals;
        synchronized (GPU_LOCK) {
            if (GPU_DISPOSALS.isEmpty()) return;
            disposals = new ArrayList<>(GPU_DISPOSALS);
            GPU_DISPOSALS.clear();
        }
        for (GpuEntry entry : disposals) entry.close();
    }

    private static List<Selection> select(List<PlanetVoxelBrick> bricks,
                                          CubeNetSurfaceTransform transform,
                                          CubeNetSurfaceTransform.Face focusFace,
                                          double focusX, double focusZ,
                                          double exactRadius, int maxLod,
                                          int maximum, boolean orbit) {
        List<Selection> selected = new ArrayList<>();
        for (PlanetVoxelBrick brick : bricks) {
            PlanetVoxelBrickKey key = brick.key();
            double centerX = transform.faceCenterX(key.face())
                    + key.minU() + key.brickSpan() * 0.5;
            double centerZ = transform.faceCenterZ(key.face())
                    + key.minV() + key.brickSpan() * 0.5;
            double distance;
            if (key.face() == focusFace) {
                distance = Math.hypot(centerX - focusX, centerZ - focusZ);
            } else {
                // Remote faces never spend exact geometry unless the camera is
                // targeting them. Their coarser mips still preserve large bases,
                // mountains and city silhouettes on the complete cube.
                distance = exactRadius * (orbit ? 16.0 : 12.0)
                        + Math.hypot(centerX - transform.faceCenterX(key.face()),
                        centerZ - transform.faceCenterZ(key.face()));
            }
            int desired = desiredLod(distance, exactRadius, maxLod);
            if (key.face() != focusFace) desired = Math.max(desired, 4);
            if (key.lod() != desired) continue;
            selected.add(new Selection(brick, distance));
        }
        selected.sort(Comparator.comparingDouble(Selection::distance)
                .thenComparingInt(selection -> selection.brick.key().lod()));
        if (selected.size() > maximum) {
            return new ArrayList<>(selected.subList(0, maximum));
        }
        return selected;
    }

    private static int desiredLod(double distance, double exactRadius, int maxLod) {
        if (distance <= exactRadius) return 0;
        double ratio = distance / Math.max(1.0, exactRadius);
        int lod = 1 + (int) Math.floor(Math.log(ratio) / Math.log(2.0));
        return Mth.clamp(lod, 1, maxLod);
    }

    private static int renderWithBuffers(RenderPass pass) {
        synchronized (BUFFER_LOCK) {
            try {
                RenderSystem.assertOnRenderThread();
                BufferBuilder solid = new BufferBuilder(
                        SOLID_MEMORY, SOLID_TYPE.mode(), SOLID_TYPE.format());
                BufferBuilder translucent = new BufferBuilder(
                        TRANSLUCENT_MEMORY, TRANSLUCENT_TYPE.mode(), TRANSLUCENT_TYPE.format());
                int rendered = pass.render(solid, translucent);
                drawBuilt(SOLID_TYPE, solid);
                drawBuilt(TRANSLUCENT_TYPE, translucent);
                return rendered;
            } catch (Throwable error) {
                disabled = true;
                GenesisMod.LOGGER.error("[PLANET-VOXEL] renderer disabled after failure; "
                        + "legacy coarse LOD remains available", error);
                return 0;
            } finally {
                SOLID_MEMORY.clear();
                TRANSLUCENT_MEMORY.clear();
            }
        }
    }

    private static void drawBuilt(RenderType type, BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) type.draw(mesh);
    }

    private static void renderMesh(Matrix4f view,
                                   VertexConsumer solid, VertexConsumer translucent,
                                   PlanetVoxelBrick brick,
                                   PlanetVoxelGreedyMesher.Mesh mesh,
                                   PointTransform transform,
                                   Vec3 camera, double renderScale, float alpha) {
        for (PlanetVoxelGreedyMesher.Quad quad : mesh.solid()) {
            renderQuad(view, solid, brick.key(), quad, transform, camera, renderScale, alpha);
        }
        for (PlanetVoxelGreedyMesher.Quad quad : mesh.translucent()) {
            renderQuad(view, translucent, brick.key(), quad, transform, camera, renderScale, alpha);
        }
    }

    private static void renderQuad(Matrix4f view, VertexConsumer buffer,
                                   PlanetVoxelBrickKey key,
                                   PlanetVoxelGreedyMesher.Quad quad,
                                   PointTransform transform,
                                   Vec3 camera, double renderScale, float alpha) {
        int flags = PlanetVoxelMaterial.flags(quad.material());
        int step = key.lod() == 0 && (flags & PlanetVoxelMaterial.FLAG_STRUCTURE) != 0 ? 1
                : key.lod() == 0 ? 2 : 1;
        for (int b = quad.b0(); b < quad.b1(); b += step) {
            int b1 = Math.min(quad.b1(), b + step);
            for (int a = quad.a0(); a < quad.a1(); a += step) {
                int a1 = Math.min(quad.a1(), a + step);
                FacePoints local = facePoints(key, quad.direction(), quad.plane(), a, b, a1, b1);
                Vector3d p0 = transform.apply(local.p0);
                Vector3d p1 = transform.apply(local.p1);
                Vector3d p2 = transform.apply(local.p2);
                Vector3d p3 = transform.apply(local.p3);
                Vector3f normal = normal(p0, p1, p3);
                drawTexturedQuad(view, buffer, camera, p0, p1, p2, p3,
                        renderScale, quad.material(), textureDirection(quad.direction()),
                        quad.coverage(), alpha, normal);
            }
        }
    }

    private static FacePoints facePoints(PlanetVoxelBrickKey key,
                                         PlanetVoxelGreedyMesher.FaceDirection direction,
                                         int plane, int a0, int b0, int a1, int b1) {
        double cell = key.cellSize();
        double uBase = key.minU();
        double yBase = key.minY();
        double vBase = key.minV();
        double fixed = plane * cell;
        double aa0 = a0 * cell, aa1 = a1 * cell;
        double bb0 = b0 * cell, bb1 = b1 * cell;
        return switch (direction) {
            case UP -> new FacePoints(
                    new SurfacePoint(uBase + aa0, yBase + fixed, vBase + bb1),
                    new SurfacePoint(uBase + aa1, yBase + fixed, vBase + bb1),
                    new SurfacePoint(uBase + aa1, yBase + fixed, vBase + bb0),
                    new SurfacePoint(uBase + aa0, yBase + fixed, vBase + bb0));
            case DOWN -> new FacePoints(
                    new SurfacePoint(uBase + aa0, yBase + fixed, vBase + bb0),
                    new SurfacePoint(uBase + aa1, yBase + fixed, vBase + bb0),
                    new SurfacePoint(uBase + aa1, yBase + fixed, vBase + bb1),
                    new SurfacePoint(uBase + aa0, yBase + fixed, vBase + bb1));
            case NORTH -> new FacePoints(
                    new SurfacePoint(uBase + aa1, yBase + bb0, vBase + fixed),
                    new SurfacePoint(uBase + aa0, yBase + bb0, vBase + fixed),
                    new SurfacePoint(uBase + aa0, yBase + bb1, vBase + fixed),
                    new SurfacePoint(uBase + aa1, yBase + bb1, vBase + fixed));
            case SOUTH -> new FacePoints(
                    new SurfacePoint(uBase + aa0, yBase + bb0, vBase + fixed),
                    new SurfacePoint(uBase + aa1, yBase + bb0, vBase + fixed),
                    new SurfacePoint(uBase + aa1, yBase + bb1, vBase + fixed),
                    new SurfacePoint(uBase + aa0, yBase + bb1, vBase + fixed));
            case WEST -> new FacePoints(
                    new SurfacePoint(uBase + fixed, yBase + bb0, vBase + aa0),
                    new SurfacePoint(uBase + fixed, yBase + bb0, vBase + aa1),
                    new SurfacePoint(uBase + fixed, yBase + bb1, vBase + aa1),
                    new SurfacePoint(uBase + fixed, yBase + bb1, vBase + aa0));
            case EAST -> new FacePoints(
                    new SurfacePoint(uBase + fixed, yBase + bb0, vBase + aa1),
                    new SurfacePoint(uBase + fixed, yBase + bb0, vBase + aa0),
                    new SurfacePoint(uBase + fixed, yBase + bb1, vBase + aa0),
                    new SurfacePoint(uBase + fixed, yBase + bb1, vBase + aa1));
        };
    }

    private static void drawTexturedQuad(Matrix4f view, VertexConsumer buffer, Vec3 camera,
                                         Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3,
                                         double renderScale, long material, Direction direction,
                                         int coverage, float alpha, Vector3f normal) {
        int stateId = PlanetVoxelMaterial.blockStateId(material);
        PlanetVolumeTextureCache.Sprite resolved = PlanetVolumeTextureCache.sprite(stateId, direction);
        TextureAtlasSprite sprite = resolved.texture();
        int flags = PlanetVoxelMaterial.flags(material);
        float shade = switch (direction) {
            case UP -> 1.0f;
            case DOWN -> 0.58f;
            case NORTH -> 0.74f;
            case SOUTH -> 0.90f;
            default -> 0.82f;
        };
        if ((flags & PlanetVoxelMaterial.FLAG_EMISSIVE) != 0) shade = 1.0f;
        int tint = resolved.tinted() ? PlanetVoxelMaterial.rgb(material) : 0xFFFFFF;
        int r = Mth.clamp((int) (((tint >> 16) & 0xFF) * shade), 0, 255);
        int g = Mth.clamp((int) (((tint >> 8) & 0xFF) * shade), 0, 255);
        int b = Mth.clamp((int) ((tint & 0xFF) * shade), 0, 255);
        float coverageFactor = 0.50f + 0.50f * (coverage / 255.0f);
        int a = Mth.clamp((int) (alpha * coverageFactor * 255.0f), 0, 255);
        int light = (flags & PlanetVoxelMaterial.FLAG_EMISSIVE) != 0
                ? LightTexture.FULL_BRIGHT
                : LightTexture.pack(PlanetVoxelMaterial.blockLight(material),
                Math.max(10, PlanetVoxelMaterial.skyLight(material)));
        addVertex(view, buffer, camera, p0, renderScale, sprite.getU0(), sprite.getV1(),
                r, g, b, a, light, normal);
        addVertex(view, buffer, camera, p1, renderScale, sprite.getU1(), sprite.getV1(),
                r, g, b, a, light, normal);
        addVertex(view, buffer, camera, p2, renderScale, sprite.getU1(), sprite.getV0(),
                r, g, b, a, light, normal);
        addVertex(view, buffer, camera, p3, renderScale, sprite.getU0(), sprite.getV0(),
                r, g, b, a, light, normal);
    }

    private static void addVertex(Matrix4f view, VertexConsumer buffer, Vec3 camera,
                                  Vector3d point, double scale, float u, float v,
                                  int r, int g, int b, int a, int light, Vector3f normal) {
        double x = camera == null ? point.x : (point.x - camera.x) * scale;
        double y = camera == null ? point.y : (point.y - camera.y) * scale;
        double z = camera == null ? point.z : (point.z - camera.z) * scale;
        buffer.addVertex(view, (float) x, (float) y, (float) z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normal.x, normal.y, normal.z);
    }

    private static Vector3d livePoint(CubeNetSurfaceTransform.Face face,
                                      CubeNetSurfaceTransform.Face currentFace,
                                      CubeNetSurfaceTransform transform,
                                      PlanetSurfaceData surface,
                                      double localU, double worldY, double localV,
                                      float foldAlpha,
                                      double currentCenterX, double cubeCenterY,
                                      double currentCenterZ) {
        double worldX = transform.faceCenterX(face) + localU;
        double worldZ = transform.faceCenterZ(face) + localV;
        double elevation = worldY - surface.seaLevel() + 0.70;
        Vector3d celestial = CubeFaceFrame.cubePoint(face, localU, localV,
                transform.faceHalfSpan(), elevation);
        Vector3d folded = CubeFaceFrame.celestialToSurface(currentFace, celestial)
                .add(currentCenterX, cubeCenterY, currentCenterZ);
        return new Vector3d(
                Mth.lerp(foldAlpha, worldX, folded.x),
                Mth.lerp(foldAlpha, worldY + 0.70, folded.y),
                Mth.lerp(foldAlpha, worldZ, folded.z));
    }

    private static Vector3d orbitalPoint(CubeNetSurfaceTransform.Face face,
                                         CubeNetSurfaceTransform transform,
                                         Vector3d planetPosition, Quaterniondc rotation,
                                         double renderedHalfExtent, double worldToRendered,
                                         double localU, double worldY, double localV) {
        double elevation = (worldY - DhLiveChunkPatchCache.seaLevel()) * worldToRendered + 0.72;
        return CubeFaceFrame.cubePoint(face, localU * worldToRendered,
                        localV * worldToRendered, renderedHalfExtent, elevation)
                .rotate(rotation).add(planetPosition);
    }

    private static Vector3f normal(Vector3d p0, Vector3d p1, Vector3d p3) {
        Vector3d a = new Vector3d(p1).sub(p0);
        Vector3d b = new Vector3d(p3).sub(p0);
        Vector3d normal = a.cross(b);
        if (normal.lengthSquared() < 1.0E-12) return new Vector3f(0, 1, 0);
        normal.normalize();
        return new Vector3f((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static Direction textureDirection(PlanetVoxelGreedyMesher.FaceDirection direction) {
        return switch (direction) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }

    private static double renderDistanceCompression(Vec3 camera,
                                                     double centerX, double centerY,
                                                     double centerZ, int vanillaRadius) {
        double distance = Math.sqrt(square(centerX - camera.x)
                + square(centerY - camera.y) + square(centerZ - camera.z));
        double target = Math.max(768.0, vanillaRadius * 3.5);
        double fullScale = distance <= target ? 1.0 : target / distance;
        double start = Math.max(512.0, GenesisClientConfig.getWorldLodStartHeight());
        double full = Math.max(start + 1.0,
                GenesisClientConfig.getWorldCubeCompressionFullHeight());
        float influence = smoothstep((float) ((camera.y - start) / (full - start)));
        return Mth.lerp(influence, 1.0, fullScale);
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static double square(double value) {
        return value * value;
    }

    @FunctionalInterface
    private interface RenderPass {
        int render(VertexConsumer solid, VertexConsumer translucent);
    }

    @FunctionalInterface
    private interface PointTransform {
        Vector3d apply(SurfacePoint point);
    }

    private record Selection(PlanetVoxelBrick brick, double distance) {
    }

    private record GpuKey(ResourceLocation planet, PlanetVoxelBrickKey key) {
    }

    private record PendingGpuUpload(PlanetVoxelBrick brick,
                                    PlanetVoxelGreedyMesher.Mesh mesh) {
    }

    private static final class GpuEntry {
        private final long revision;
        private final VertexBuffer solid;
        private final VertexBuffer translucent;
        private final long estimatedBytes;

        private GpuEntry(long revision, VertexBuffer solid,
                         VertexBuffer translucent, long estimatedBytes) {
            this.revision = revision;
            this.solid = solid;
            this.translucent = translucent;
            this.estimatedBytes = estimatedBytes;
        }

        private void close() {
            if (solid != null) solid.close();
            if (translucent != null) translucent.close();
        }
    }

    private record GpuDraw(GpuEntry entry, double distance) {
    }

    private record OrbitProjection(int viewportHeight, double aspectRatio,
                                   double verticalFovRadians) {
    }

    private record SurfacePoint(double u, double y, double v) {
    }

    private record FacePoints(SurfacePoint p0, SurfacePoint p1,
                              SurfacePoint p2, SurfacePoint p3) {
    }
}
