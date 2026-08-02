package shipwrights.genesis.client.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.OrbitalSurveyController;
import shipwrights.genesis.client.PlanetSurfaceTextures;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.renderer.CelestialRenderCoordinates;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.surface.SparsePlanetLodService;
import shipwrights.genesis.space.surface.SparsePlanetLodTile;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders the same server/DH terrain tiles on Earth after the seamless crossing.
 *
 * <p>The previous version submitted thousands of 16x16 tiles and exact chunks
 * every frame. Besides stalling the render thread, that cannot make a house
 * readable while the whole 8192-block face is fitted on screen: the house is
 * physically smaller than a pixel. This pass keeps a cheap complete cube and
 * spends block-scale geometry only around the camera-facing/surveyed point.
 * Orbital survey mode supplies genuine optical magnification and exact surface
 * coordinates instead of enlarging structures or changing planet scale.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class OrbitalDhLivePatchRenderer {
    private static final ResourceLocation OVERWORLD_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final RenderType RENDER_TYPE = RenderType.debugQuads();

    private OrbitalDhLivePatchRenderer() {
    }

    private record FocusRegion(CubeNetSurfaceTransform.Face face,
                               double worldX, double worldZ,
                               boolean crosshairHit) {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !GenesisMod.isSpaceDimension(level)) {
            OrbitalSurveyController.reset();
            return;
        }

        SparsePlanetLodTile[] sparseTiles =
                SparsePlanetLodClientCache.snapshot(SparsePlanetLodService.EARTH_ID);
        DhLiveChunkPatchCache.Patch[] patches = DhLiveChunkPatchCache.snapshot();
        boolean voxelEarth = PlanetVoxelRenderer.hasVoxelData();

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        Celestial earth = findEarth(registry);
        if (earth == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        VantagePoint vantage = VantagePoint.get(level,
                new Vector3d(camera.x, camera.y, camera.z), ticks, partialTick);
        CelestialRenderCoordinates.RenderTransform renderTransform =
                CelestialRenderCoordinates.getRenderTransform(
                        earth, vantage, camera, ticks, partialTick, registry);

        Vector3d simulatedPlanetPosition = new Vector3d(renderTransform.position());
        Quaterniond planetRotation = new Quaterniond(renderTransform.rotation());
        CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
        boolean voxelCoverage = voxelEarth
                && PlanetVoxelRenderer.hasCompleteCoarseCoverage(transform);

        // Display the cube in the same 1:1 Minecraft-block frame used during
        // ascent. Genesis' physics remains 1:16; only the render coordinates
        // are expanded and the empty orbital altitude is compressed.
        EarthLodVisualFrame.OrbitFrame visualFrame = EarthLodVisualFrame.orbitFrame(
                simulatedPlanetPosition, earth.getActualSize() * 0.5, transform);
        Vector3d planetPosition = new Vector3d(visualFrame.planetPosition());
        double renderedHalfExtent = visualFrame.halfExtent();
        double worldToRendered = visualFrame.worldToRendered();

        // Generic far-plane protection may still scale the complete visual
        // frame. It scales planet distance, cube size and block geometry by the
        // same factor, so apparent detail is preserved.
        double renderDistanceScale =
                CelestialRenderCoordinates.getPlanetRenderDistanceScale(planetPosition);
        if (renderDistanceScale != 1.0) {
            planetPosition = CelestialRenderCoordinates.scaleRenderPosition(
                    planetPosition, renderDistanceScale);
            renderedHalfExtent *= renderDistanceScale;
            worldToRendered *= renderDistanceScale;
        }

        Quaterniond inverseRotation = new Quaterniond(planetRotation).conjugate();
        Vector3d localCamera = new Vector3d(planetPosition).negate().rotate(inverseRotation);
        Vector3d localToCamera = new Vector3d(localCamera).normalize();
        Vector3f look = event.getCamera().getLookVector();
        Vector3d localLook = new Vector3d(look.x, look.y, look.z)
                .rotate(inverseRotation).normalize();
        FocusRegion focus = findFocus(localCamera, localLook, renderedHalfExtent,
                worldToRendered, transform);
        OrbitalSurveyController.updateTarget(
                focus.face(), focus.worldX(), focus.worldZ(), focus.crosshairHit());

        double distance = Math.max(renderedHalfExtent, planetPosition.length());
        double apparentHalf = renderedHalfExtent / distance;
        PlanetSurfaceData coarseSurface = PlanetSurfaceTextures.surface(OVERWORLD_ID);
        int seaLevel = coarseSurface != null && coarseSurface.isValid()
                ? coarseSurface.seaLevel() : DhLiveChunkPatchCache.seaLevel();

        MultiBufferSource.BufferSource source = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = source.getBuffer(RENDER_TYPE);
        Matrix4f view = event.getModelViewMatrix();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        if (!voxelCoverage) {
            renderSparseTiles(buffer, view, sparseTiles, transform, planetPosition,
                    planetRotation, renderedHalfExtent, worldToRendered,
                    localToCamera, seaLevel, apparentHalf, focus);
            renderExactDhPatches(buffer, view, patches, transform, planetPosition,
                    planetRotation, renderedHalfExtent, worldToRendered,
                    localToCamera, apparentHalf, focus);
        }

        source.endBatch(RENDER_TYPE);
        if (!voxelCoverage) {
            PlanetVolumeLodRenderer.renderOrbit(view, transform, planetPosition, planetRotation,
                    renderedHalfExtent, worldToRendered, focus.face(), focus.worldX(), focus.worldZ(),
                    apparentHalf);
        }
        // This call also emits the throttled viewport-interest request. It must
        // run with an empty cache or a fresh client can never receive its first
        // brick. Partial voxel geometry overlays the temporary coarse fallback
        // until all six faces have genuine occupancy coverage.
        PlanetVoxelRenderer.renderOrbit(view, transform, planetPosition, planetRotation,
                renderedHalfExtent, worldToRendered, localCamera, localLook, focus.face(),
                apparentHalf, seaLevel);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static Celestial findEarth(Registry<Celestial> registry) {
        for (Celestial candidate : registry) {
            if (OVERWORLD_ID.equals(registry.getKey(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /** Ray/AABB targeting in the planet's rotating local frame. */
    private static FocusRegion findFocus(Vector3d localCamera, Vector3d localLook,
                                         double halfExtent, double worldToRendered,
                                         CubeNetSurfaceTransform transform) {
        double hitDistance = intersectCube(localCamera, localLook, halfExtent + 2.0);
        boolean hit = Double.isFinite(hitDistance) && hitDistance >= 0.0;
        Vector3d point;
        if (hit) {
            point = new Vector3d(localLook).mul(hitDistance).add(localCamera);
        } else {
            // Fall back to the radial point under the camera. This keeps detail
            // stable when looking away from Earth and gives the normal renderer
            // a deterministic face to prioritize.
            double reach = maxAbs(localCamera);
            if (reach < 1.0E-9) {
                point = new Vector3d(0.0, halfExtent, 0.0);
            } else {
                point = new Vector3d(localCamera).mul(halfExtent / reach);
            }
        }
        CubeNetSurfaceTransform.Face face = dominantFace(point);
        CubeFaceFrame.Axes axes = CubeFaceFrame.axes(face);
        double u = point.dot(axes.right()) / Math.max(1.0E-9, worldToRendered);
        double v = point.dot(axes.south()) / Math.max(1.0E-9, worldToRendered);
        double half = transform.faceHalfSpan();
        u = Math.max(-half, Math.min(half, u));
        v = Math.max(-half, Math.min(half, v));
        return new FocusRegion(face,
                transform.faceCenterX(face) + u,
                transform.faceCenterZ(face) + v,
                hit);
    }

    private static double intersectCube(Vector3d origin, Vector3d direction, double halfExtent) {
        double near = 0.0;
        double far = Double.POSITIVE_INFINITY;
        double[] origins = {origin.x, origin.y, origin.z};
        double[] directions = {direction.x, direction.y, direction.z};
        for (int axis = 0; axis < 3; axis++) {
            double o = origins[axis];
            double d = directions[axis];
            if (Math.abs(d) < 1.0E-10) {
                if (o < -halfExtent || o > halfExtent) {
                    return Double.NaN;
                }
                continue;
            }
            double t0 = (-halfExtent - o) / d;
            double t1 = (halfExtent - o) / d;
            if (t0 > t1) {
                double swap = t0;
                t0 = t1;
                t1 = swap;
            }
            near = Math.max(near, t0);
            far = Math.min(far, t1);
            if (far < near) {
                return Double.NaN;
            }
        }
        return near;
    }

    private static CubeNetSurfaceTransform.Face dominantFace(Vector3dc point) {
        double ax = Math.abs(point.x());
        double ay = Math.abs(point.y());
        double az = Math.abs(point.z());
        if (ay >= ax && ay >= az) {
            return point.y() >= 0.0 ? CubeNetSurfaceTransform.Face.UP
                    : CubeNetSurfaceTransform.Face.DOWN;
        }
        if (az >= ax) {
            return point.z() >= 0.0 ? CubeNetSurfaceTransform.Face.SOUTH
                    : CubeNetSurfaceTransform.Face.NORTH;
        }
        return point.x() >= 0.0 ? CubeNetSurfaceTransform.Face.EAST
                : CubeNetSurfaceTransform.Face.WEST;
    }

    private static double maxAbs(Vector3dc vector) {
        return Math.max(Math.abs(vector.x()),
                Math.max(Math.abs(vector.y()), Math.abs(vector.z())));
    }

    private static void renderSparseTiles(VertexConsumer buffer, Matrix4f view,
                                          SparsePlanetLodTile[] tiles,
                                          CubeNetSurfaceTransform transform,
                                          Vector3d planetPosition, Quaterniondc planetRotation,
                                          double renderedHalfExtent, double worldToRendered,
                                          Vector3d localToCamera, int seaLevel,
                                          double apparentHalf, FocusRegion focus) {
        if (tiles.length == 0) {
            return;
        }
        boolean survey = OrbitalSurveyController.isActive();
        double detailRadius = OrbitalSurveyController.detailRadius();
        int maximum = survey ? 512 : 224;
        int rendered = 0;

        // Coarse-to-fine keeps finer tiles on top without sorting all cached
        // tiles every frame. Only the surveyed/camera-facing region receives
        // expensive detail; all visible faces retain their 128-block fallback.
        int[] passes = {128, 64, 16, 4, 1};
        for (int cellSize : passes) {
            for (SparsePlanetLodTile tile : tiles) {
                if (rendered >= maximum) {
                    return;
                }
                if (tile.cellSize() != cellSize) {
                    continue;
                }
                if (tile.exact() && PlanetLodVolumeClientCache.hasTileAt(
                        SparsePlanetLodService.EARTH_ID, tile.face(),
                        tile.originX() + tile.span() * 0.5,
                        tile.originZ() + tile.span() * 0.5)) {
                    // Never draw the old flat exact tile where the real
                    // textured block-volume chunk is available.
                    continue;
                }
                Vector3dc normal = CubeFaceFrame.axes(tile.face()).up();
                if (localToCamera.dot(normal) < -0.035) {
                    continue;
                }
                double centerX = tile.originX() + tile.span() * 0.5;
                double centerZ = tile.originZ() + tile.span() * 0.5;
                double distance = distance(centerX, centerZ, focus.worldX(), focus.worldZ());
                if (!shouldRenderTile(tile, focus, distance, detailRadius)) {
                    continue;
                }
                int step = sparseStep(tile, apparentHalf, survey);
                double nudge = tile.exact() ? 0.035
                        : tile.cellSize() <= 4 ? 0.026
                        : tile.cellSize() <= 16 ? 0.018
                        : tile.cellSize() <= 64 ? 0.011 : 0.006;
                renderSparseTile(buffer, view, tile, transform, planetPosition,
                        planetRotation, renderedHalfExtent, worldToRendered,
                        seaLevel, step, nudge, survey);
                rendered++;
            }
        }
    }

    private static boolean shouldRenderTile(SparsePlanetLodTile tile, FocusRegion focus,
                                            double distance, double detailRadius) {
        if (tile.cellSize() >= 128) {
            return true; // complete six-face fallback: only 96 tiles total
        }
        if (tile.face() != focus.face()) {
            return false;
        }
        if (tile.cellSize() <= 1) return distance <= detailRadius;
        if (tile.cellSize() <= 4) return distance <= detailRadius * 2.0;
        if (tile.cellSize() <= 16) return distance <= detailRadius * 5.0;
        return distance <= detailRadius * 10.0;
    }

    private static int sparseStep(SparsePlanetLodTile tile, double apparentHalf, boolean survey) {
        if (survey && tile.cellSize() <= 4) return 1;
        if (tile.cellSize() >= 128) return apparentHalf < 0.08 ? 2 : 1;
        if (tile.cellSize() >= 64) return 2;
        if (tile.cellSize() >= 16 && apparentHalf < 0.12) return 2;
        return 1;
    }

    private static void renderSparseTile(VertexConsumer buffer, Matrix4f view,
                                         SparsePlanetLodTile tile,
                                         CubeNetSurfaceTransform transform,
                                         Vector3d planetPosition, Quaterniondc rotation,
                                         double renderedHalfExtent, double worldToRendered,
                                         int seaLevel, int step, double nudge,
                                         boolean survey) {
        for (int z = 0; z < SparsePlanetLodTile.RESOLUTION; z += step) {
            int z1 = Math.min(SparsePlanetLodTile.RESOLUTION, z + step);
            for (int x = 0; x < SparsePlanetLodTile.RESOLUTION; x += step) {
                int x1 = Math.min(SparsePlanetLodTile.RESOLUTION, x + step);
                int index = tile.index(x, z);
                int colour = tile.colours()[index];
                if ((colour >>> 24) == 0) continue;
                int height = tile.heights()[index];
                double wx0 = tile.originX() + (double) x * tile.cellSize();
                double wx1 = tile.originX() + (double) x1 * tile.cellSize();
                double wz0 = tile.originZ() + (double) z * tile.cellSize();
                double wz1 = tile.originZ() + (double) z1 * tile.cellSize();
                Vector3d p00 = sparseOrbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx0, wz0, height, seaLevel, nudge);
                Vector3d p10 = sparseOrbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx1, wz0, height, seaLevel, nudge);
                Vector3d p11 = sparseOrbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx1, wz1, height, seaLevel, nudge);
                Vector3d p01 = sparseOrbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx0, wz1, height, seaLevel, nudge);
                int top = tile.cellSize() <= 4 ? brighten(colour, 1.10) : colour;
                addVertex(buffer, view, p01, top);
                addVertex(buffer, view, p11, top);
                addVertex(buffer, view, p10, top);
                addVertex(buffer, view, p00, top);

                if ((survey || tile.exact()) && tile.cellSize() <= 4 && step == 1) {
                    int east = x1 < SparsePlanetLodTile.RESOLUTION
                            ? tile.heights()[tile.index(x1, z)] : height;
                    if (east != height) {
                        addSparseWall(buffer, view, tile.face(), transform, planetPosition, rotation,
                                renderedHalfExtent, worldToRendered, wx1, wz0, wx1, wz1,
                                height, east, seaLevel, darken(colour, 0.62), nudge + 0.003);
                    }
                    int south = z1 < SparsePlanetLodTile.RESOLUTION
                            ? tile.heights()[tile.index(x, z1)] : height;
                    if (south != height) {
                        addSparseWall(buffer, view, tile.face(), transform, planetPosition, rotation,
                                renderedHalfExtent, worldToRendered, wx0, wz1, wx1, wz1,
                                height, south, seaLevel, darken(colour, 0.74), nudge + 0.003);
                    }
                }
            }
        }
    }

    private static void renderExactDhPatches(VertexConsumer buffer, Matrix4f view,
                                             DhLiveChunkPatchCache.Patch[] patches,
                                             CubeNetSurfaceTransform transform,
                                             Vector3d planetPosition, Quaterniondc rotation,
                                             double renderedHalfExtent, double worldToRendered,
                                             Vector3d localToCamera, double apparentHalf,
                                             FocusRegion focus) {
        if (patches.length == 0) return;
        boolean survey = OrbitalSurveyController.isActive();
        if (!survey && apparentHalf < 0.16) {
            return; // exact blocks are sub-pixel at full-planet distance
        }
        double radius = survey ? OrbitalSurveyController.detailRadius()
                : Math.min(512.0, OrbitalSurveyController.detailRadius());
        List<DhLiveChunkPatchCache.Patch> candidates = new ArrayList<>();
        for (DhLiveChunkPatchCache.Patch patch : patches) {
            if (patch.face() != focus.face()) continue;
            if (localToCamera.dot(CubeFaceFrame.axes(patch.face()).up()) < -0.035) continue;
            if (PlanetLodVolumeClientCache.hasTileAt(
                    SparsePlanetLodService.EARTH_ID, patch.face(),
                    patch.worldMinX() + 8.0, patch.worldMinZ() + 8.0)) {
                continue; // actual 3D block-volume chunk replaces this patch
            }
            if (SparsePlanetLodClientCache.hasExactChunk(
                    SparsePlanetLodService.EARTH_ID, patch.face(), patch.chunkX(), patch.chunkZ())) {
                continue; // server exact tile is authoritative; do not draw twice
            }
            double centerX = patch.worldMinX() + 8.0;
            double centerZ = patch.worldMinZ() + 8.0;
            if (distance(centerX, centerZ, focus.worldX(), focus.worldZ()) <= radius) {
                candidates.add(patch);
            }
        }
        candidates.sort(Comparator.comparingDouble(patch -> distanceSquared(
                patch.worldMinX() + 8.0, patch.worldMinZ() + 8.0,
                focus.worldX(), focus.worldZ())));
        int maximum = survey ? 256 : 64;
        int step = survey ? 1 : 2;
        for (int index = 0; index < candidates.size() && index < maximum; index++) {
            renderPatch(buffer, view, candidates.get(index), transform,
                    planetPosition, rotation, renderedHalfExtent, worldToRendered, step);
        }
    }

    private static void addSparseWall(VertexConsumer buffer, Matrix4f view,
                                      CubeNetSurfaceTransform.Face face,
                                      CubeNetSurfaceTransform transform,
                                      Vector3d planetPosition, Quaterniondc rotation,
                                      double renderedHalfExtent, double worldToRendered,
                                      double x0, double z0, double x1, double z1,
                                      int heightA, int heightB, int seaLevel,
                                      int colour, double nudge) {
        int low = Math.min(heightA, heightB);
        int high = Math.max(heightA, heightB);
        Vector3d p0l = sparseOrbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x0, z0, low, seaLevel, nudge);
        Vector3d p1l = sparseOrbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x1, z1, low, seaLevel, nudge);
        Vector3d p1h = sparseOrbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x1, z1, high, seaLevel, nudge);
        Vector3d p0h = sparseOrbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x0, z0, high, seaLevel, nudge);
        addVertex(buffer, view, p0l, colour);
        addVertex(buffer, view, p1l, colour);
        addVertex(buffer, view, p1h, colour);
        addVertex(buffer, view, p0h, colour);
    }

    private static Vector3d sparseOrbitalPoint(CubeNetSurfaceTransform.Face face,
                                               CubeNetSurfaceTransform transform,
                                               Vector3d planetPosition, Quaterniondc rotation,
                                               double renderedHalfExtent, double worldToRendered,
                                               double worldX, double worldZ, double height,
                                               int seaLevel, double nudge) {
        double u = (worldX - transform.faceCenterX(face)) * worldToRendered;
        double v = (worldZ - transform.faceCenterZ(face)) * worldToRendered;
        double elevation = (height - seaLevel) * worldToRendered + nudge;
        return CubeFaceFrame.cubePoint(face, u, v, renderedHalfExtent, elevation)
                .rotate(rotation).add(planetPosition);
    }

    private static void renderPatch(VertexConsumer buffer, Matrix4f view,
                                    DhLiveChunkPatchCache.Patch patch,
                                    CubeNetSurfaceTransform transform,
                                    Vector3d planetPosition, Quaterniondc rotation,
                                    double renderedHalfExtent, double worldToRendered,
                                    int step) {
        int worldMinX = patch.worldMinX();
        int worldMinZ = patch.worldMinZ();
        for (int z = 0; z < DhLiveChunkPatchCache.PATCH_RESOLUTION; z += step) {
            int z1 = Math.min(DhLiveChunkPatchCache.PATCH_RESOLUTION, z + step);
            for (int x = 0; x < DhLiveChunkPatchCache.PATCH_RESOLUTION; x += step) {
                int x1 = Math.min(DhLiveChunkPatchCache.PATCH_RESOLUTION, x + step);
                int index = patch.index(x, z);
                int colour = patch.colours()[index];
                if ((colour >>> 24) == 0) continue;
                int height = patch.heights()[index];
                double wx0 = worldMinX + x;
                double wx1 = worldMinX + x1;
                double wz0 = worldMinZ + z;
                double wz1 = worldMinZ + z1;
                Vector3d p00 = orbitalPoint(patch.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx0, wz0, height, 0.042);
                Vector3d p10 = orbitalPoint(patch.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx1, wz0, height, 0.042);
                Vector3d p11 = orbitalPoint(patch.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx1, wz1, height, 0.042);
                Vector3d p01 = orbitalPoint(patch.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, wx0, wz1, height, 0.042);
                int top = brighten(colour, 1.18);
                addVertex(buffer, view, p01, top);
                addVertex(buffer, view, p11, top);
                addVertex(buffer, view, p10, top);
                addVertex(buffer, view, p00, top);
                if (step == 1) {
                    int east = x1 < DhLiveChunkPatchCache.PATCH_RESOLUTION
                            ? patch.heights()[patch.index(x1, z)] : height;
                    if (east != height) {
                        addWall(buffer, view, patch.face(), transform, planetPosition, rotation,
                                renderedHalfExtent, worldToRendered, wx1, wz0, wx1, wz1,
                                height, east, darken(colour, 0.62));
                    }
                    int south = z1 < DhLiveChunkPatchCache.PATCH_RESOLUTION
                            ? patch.heights()[patch.index(x, z1)] : height;
                    if (south != height) {
                        addWall(buffer, view, patch.face(), transform, planetPosition, rotation,
                                renderedHalfExtent, worldToRendered, wx0, wz1, wx1, wz1,
                                height, south, darken(colour, 0.74));
                    }
                }
            }
        }
    }

    private static void addWall(VertexConsumer buffer, Matrix4f view,
                                CubeNetSurfaceTransform.Face face,
                                CubeNetSurfaceTransform transform,
                                Vector3d planetPosition, Quaterniondc rotation,
                                double renderedHalfExtent, double worldToRendered,
                                double x0, double z0, double x1, double z1,
                                int heightA, int heightB, int colour) {
        int low = Math.min(heightA, heightB);
        int high = Math.max(heightA, heightB);
        Vector3d p0l = orbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x0, z0, low, 0.045);
        Vector3d p1l = orbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x1, z1, low, 0.045);
        Vector3d p1h = orbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x1, z1, high, 0.045);
        Vector3d p0h = orbitalPoint(face, transform, planetPosition, rotation,
                renderedHalfExtent, worldToRendered, x0, z0, high, 0.045);
        addVertex(buffer, view, p0l, colour);
        addVertex(buffer, view, p1l, colour);
        addVertex(buffer, view, p1h, colour);
        addVertex(buffer, view, p0h, colour);
    }

    private static Vector3d orbitalPoint(CubeNetSurfaceTransform.Face face,
                                         CubeNetSurfaceTransform transform,
                                         Vector3d planetPosition, Quaterniondc rotation,
                                         double renderedHalfExtent, double worldToRendered,
                                         double worldX, double worldZ, double height,
                                         double outwardNudge) {
        double u = (worldX - transform.faceCenterX(face)) * worldToRendered;
        double v = (worldZ - transform.faceCenterZ(face)) * worldToRendered;
        double elevation = (height - DhLiveChunkPatchCache.seaLevel()) * worldToRendered
                + outwardNudge;
        return CubeFaceFrame.cubePoint(face, u, v, renderedHalfExtent, elevation)
                .rotate(rotation).add(planetPosition);
    }

    private static double distance(double x0, double z0, double x1, double z1) {
        return Math.sqrt(distanceSquared(x0, z0, x1, z1));
    }

    private static double distanceSquared(double x0, double z0, double x1, double z1) {
        double dx = x0 - x1;
        double dz = z0 - z1;
        return dx * dx + dz * dz;
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f view,
                                  Vector3d point, int argb) {
        buffer.addVertex(view, (float) point.x, (float) point.y, (float) point.z)
                .setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF,
                        argb & 0xFF, 255);
    }

    private static int brighten(int argb, double factor) {
        int r = clampColour((int) (((argb >> 16) & 0xFF) * factor));
        int g = clampColour((int) (((argb >> 8) & 0xFF) * factor));
        int b = clampColour((int) ((argb & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int argb, double factor) {
        return brighten(argb, factor);
    }

    private static int clampColour(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
