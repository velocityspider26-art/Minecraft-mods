package shipwrights.genesis.client.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.OrbitalSurveyController;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.space.surface.PlanetLodVolumeTile;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.surface.SparsePlanetLodService;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** High-quality textured 3D structure/terrain shell shared by ascent and orbit. */
public final class PlanetVolumeLodRenderer {
    private static final ResourceLocation EARTH_ID = SparsePlanetLodService.EARTH_ID;
    private static final RenderType SOLID_TYPE =
            RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType TRANSLUCENT_TYPE =
            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final int SOLID_BUFFER_BYTES = 16 * 1024 * 1024;
    private static final int TRANSLUCENT_BUFFER_BYTES = 4 * 1024 * 1024;
    private static final Object BUFFER_LOCK = new Object();
    /**
     * Persistent native builders. Reallocating tens of megabytes every render
     * frame caused avoidable GC/native-memory pressure exactly while climbing
     * through the LOD transition. ByteBufferBuilder grows when needed and is
     * cleared after each completed draw without losing its capacity.
     */
    private static final ByteBufferBuilder SOLID_MEMORY = new ByteBufferBuilder(SOLID_BUFFER_BYTES);
    private static final ByteBufferBuilder TRANSLUCENT_MEMORY = new ByteBufferBuilder(TRANSLUCENT_BUFFER_BYTES);
    private static volatile boolean loggedOverworld;
    private static volatile boolean loggedOrbit;
    private static volatile boolean renderDisabled;

    private PlanetVolumeLodRenderer() {
    }

    public static void renderOverworld(Matrix4f view, Vec3 camera,
                                       CubeNetSurfaceTransform transform,
                                       CubeNetSurfaceTransform.Face currentFace,
                                       PlanetSurfaceData surface,
                                       float lodAlpha, float foldAlpha,
                                       float centerFillAlpha, int vanillaRadius,
                                       double vanillaGroundRadius) {
        if (lodAlpha < 0.12f) return;
        if (renderDisabled) return;
        PlanetLodVolumeTile[] tiles = PlanetLodVolumeClientCache.snapshot(EARTH_ID);
        if (renderDisabled) return;
        if (tiles.length == 0) return;

        int quality = GenesisClientConfig.getWorldLodQuality();
        int maximum = quality >= 3 ? 1024 : quality == 2 ? 512 : 192;
        double radius = quality >= 3 ? 4096.0 : quality == 2 ? 2560.0 : 1536.0;
        List<PlanetLodVolumeTile> candidates = new ArrayList<>();
        for (PlanetLodVolumeTile tile : tiles) {
            if (tile.face() != currentFace) continue;
            double dx = tile.worldMinX() + 8.0 - camera.x;
            double dz = tile.worldMinZ() + 8.0 - camera.z;
            if (dx * dx + dz * dz <= radius * radius) candidates.add(tile);
        }
        candidates.sort(Comparator.comparingDouble(tile -> {
            double dx = tile.worldMinX() + 8.0 - camera.x;
            double dz = tile.worldMinZ() + 8.0 - camera.z;
            return dx * dx + dz * dz;
        }));
        if (candidates.isEmpty()) return;

        double currentCenterX = transform.faceCenterX(currentFace);
        double currentCenterZ = transform.faceCenterZ(currentFace);
        double cubeCenterY = surface.seaLevel() - transform.faceHalfSpan();
        double renderScale = renderDistanceCompression(camera, currentCenterX, cubeCenterY,
                currentCenterZ, vanillaRadius);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        int rendered = renderWithDedicatedBuffers((solid, translucent) -> {
            int count = 0;
            for (PlanetLodVolumeTile tile : candidates) {
                if (count >= maximum) break;
                double centerX = tile.worldMinX() + 8.0;
                double centerZ = tile.worldMinZ() + 8.0;
                double distance = Math.sqrt(square(centerX - camera.x) + square(centerZ - camera.z));
                double near0 = Math.max(0.0, vanillaGroundRadius * 0.65 - 48.0);
                double near1 = Math.max(32.0, vanillaGroundRadius * 1.05 + 24.0);
                float visible = Math.max(foldAlpha, centerFillAlpha);
                visible = Math.max(visible, smoothstep((float) ((distance - near0)
                        / Math.max(1.0, near1 - near0))));
                if (visible < 0.08f) continue;

                PlanetVolumeMeshCache.Mesh mesh = PlanetVolumeMeshCache.getOrSchedule(EARTH_ID, tile);
                if (mesh == null) continue;
                renderWorldMesh(view, camera, solid, translucent, tile, mesh, transform,
                        currentFace, surface, foldAlpha, currentCenterX, cubeCenterY,
                        currentCenterZ, renderScale, visible);
                count++;
            }
            return count;
        });
        if (rendered > 0 && !loggedOverworld) {
            loggedOverworld = true;
            GenesisMod.LOGGER.info("[VOLUME-LOD] textured 3D block-volume terrain active during ascent; {} exact chunks rendered", rendered);
        }
    }

    public static void renderOrbit(Matrix4f view,
                                   CubeNetSurfaceTransform transform,
                                   Vector3d planetPosition, Quaterniondc rotation,
                                   double renderedHalfExtent, double worldToRendered,
                                   CubeNetSurfaceTransform.Face focusFace,
                                   double focusX, double focusZ,
                                   double apparentHalf) {
        PlanetLodVolumeTile[] tiles = PlanetLodVolumeClientCache.snapshot(EARTH_ID);
        if (tiles.length == 0) return;
        boolean survey = OrbitalSurveyController.isActive();
        double radius = survey ? Math.max(OrbitalSurveyController.detailRadius(), 4096.0)
                : Math.max(1024.0, Math.min(4096.0, 1024.0 + apparentHalf * 4096.0));
        int maximum = survey ? 1536 : 1024;
        List<PlanetLodVolumeTile> candidates = new ArrayList<>();
        for (PlanetLodVolumeTile tile : tiles) {
            if (tile.face() != focusFace) continue;
            double dx = tile.worldMinX() + 8.0 - focusX;
            double dz = tile.worldMinZ() + 8.0 - focusZ;
            if (dx * dx + dz * dz <= radius * radius) candidates.add(tile);
        }
        candidates.sort(Comparator.comparingDouble(tile ->
                square(tile.worldMinX() + 8.0 - focusX) + square(tile.worldMinZ() + 8.0 - focusZ)));
        if (candidates.isEmpty()) return;

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        int rendered = renderWithDedicatedBuffers((solid, translucent) -> {
            int count = 0;
            for (PlanetLodVolumeTile tile : candidates) {
                if (count >= maximum) break;
                PlanetVolumeMeshCache.Mesh mesh = PlanetVolumeMeshCache.getOrSchedule(EARTH_ID, tile);
                if (mesh == null) continue;
                renderOrbitalMesh(view, solid, translucent, tile, mesh, transform,
                        planetPosition, rotation, renderedHalfExtent, worldToRendered);
                count++;
            }
            return count;
        });
        if (rendered > 0 && !loggedOrbit) {
            loggedOrbit = true;
            GenesisMod.LOGGER.info("[VOLUME-LOD] textured 3D block-volume terrain active in orbit; {} exact chunks rendered on {}", rendered, focusFace);
        }
    }

    public static void resetLogging() {
        loggedOverworld = false;
        loggedOrbit = false;
        renderDisabled = false;
    }

    /**
     * Uses private native builders instead of Minecraft's shared BufferSource.
     * RenderLevelStageEvent fires while vanilla may still own/end batches in
     * the global source; writing into that source caused the fatal
     * {@code BufferBuilder: Not building!} crash during ascent.
     */
    private static int renderWithDedicatedBuffers(VolumePass pass) {
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
                renderDisabled = true;
                GenesisMod.LOGGER.error(
                        "[VOLUME-LOD] disabled for this level after a render failure; "
                                + "the coarse world LOD remains active instead of crashing the client",
                        error);
                return 0;
            } finally {
                SOLID_MEMORY.clear();
                TRANSLUCENT_MEMORY.clear();
            }
        }
    }

    private static void drawBuilt(RenderType type, BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) {
            type.draw(mesh);
        }
    }

    @FunctionalInterface
    private interface VolumePass {
        int render(VertexConsumer solid, VertexConsumer translucent);
    }

    private static void renderWorldMesh(Matrix4f view, Vec3 camera,
                                        VertexConsumer solid, VertexConsumer translucent,
                                        PlanetLodVolumeTile tile, PlanetVolumeMeshCache.Mesh mesh,
                                        CubeNetSurfaceTransform transform,
                                        CubeNetSurfaceTransform.Face currentFace,
                                        PlanetSurfaceData surface, float foldAlpha,
                                        double currentCenterX, double cubeCenterY, double currentCenterZ,
                                        double renderScale, float alpha) {
        int worldMinX = tile.worldMinX();
        int worldMinZ = tile.worldMinZ();
        for (PlanetVolumeMeshCache.TopFace top : mesh.tops()) {
            double x0 = worldMinX + top.x();
            double z0 = worldMinZ + top.z();
            Vector3d p00 = livePoint(tile.face(), currentFace, transform, surface,
                    x0, z0, top.y(), foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.34);
            Vector3d p10 = livePoint(tile.face(), currentFace, transform, surface,
                    x0 + 1.0, z0, top.y(), foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.34);
            Vector3d p11 = livePoint(tile.face(), currentFace, transform, surface,
                    x0 + 1.0, z0 + 1.0, top.y(), foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.34);
            Vector3d p01 = livePoint(tile.face(), currentFace, transform, surface,
                    x0, z0 + 1.0, top.y(), foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.34);
            drawTexturedQuad(view, chooseBuffer(solid, translucent, top.flags()), camera,
                    p01, p11, p10, p00, renderScale,
                    top.stateId(), Direction.UP, top.colour(), top.packedLight(), top.flags(),
                    alpha, new Vector3f(0, 1, 0));
        }
        for (PlanetVolumeMeshCache.SideFace side : mesh.sides()) {
            renderWorldSide(view, camera, solid, translucent, tile, side, transform,
                    currentFace, surface, foldAlpha, currentCenterX, cubeCenterY,
                    currentCenterZ, renderScale, alpha);
        }
    }

    private static void renderWorldSide(Matrix4f view, Vec3 camera,
                                        VertexConsumer solid, VertexConsumer translucent,
                                        PlanetLodVolumeTile tile, PlanetVolumeMeshCache.SideFace side,
                                        CubeNetSurfaceTransform transform,
                                        CubeNetSurfaceTransform.Face currentFace,
                                        PlanetSurfaceData surface, float foldAlpha,
                                        double currentCenterX, double cubeCenterY, double currentCenterZ,
                                        double renderScale, float alpha) {
        int unit = 1;
        int minX = tile.worldMinX();
        int minZ = tile.worldMinZ();
        for (int h = side.horizontal0(); h < side.horizontal1(); h += unit) {
            int h1 = Math.min(side.horizontal1(), h + unit);
            for (int y = side.y0(); y < side.y1(); y += unit) {
                int y1 = Math.min(side.y1(), y + unit);
                double x0, z0, x1, z1;
                Direction textureDirection;
                Vector3f normal;
                switch (side.side()) {
                    case WEST -> {
                        x0 = x1 = minX + side.fixed(); z0 = minZ + h; z1 = minZ + h1;
                        textureDirection = Direction.WEST; normal = new Vector3f(-1, 0, 0);
                    }
                    case EAST -> {
                        x0 = x1 = minX + side.fixed(); z0 = minZ + h1; z1 = minZ + h;
                        textureDirection = Direction.EAST; normal = new Vector3f(1, 0, 0);
                    }
                    case NORTH -> {
                        z0 = z1 = minZ + side.fixed(); x0 = minX + h1; x1 = minX + h;
                        textureDirection = Direction.NORTH; normal = new Vector3f(0, 0, -1);
                    }
                    default -> {
                        z0 = z1 = minZ + side.fixed(); x0 = minX + h; x1 = minX + h1;
                        textureDirection = Direction.SOUTH; normal = new Vector3f(0, 0, 1);
                    }
                }
                Vector3d p0l = livePoint(tile.face(), currentFace, transform, surface,
                        x0, z0, y, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.35);
                Vector3d p1l = livePoint(tile.face(), currentFace, transform, surface,
                        x1, z1, y, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.35);
                Vector3d p1h = livePoint(tile.face(), currentFace, transform, surface,
                        x1, z1, y1, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.35);
                Vector3d p0h = livePoint(tile.face(), currentFace, transform, surface,
                        x0, z0, y1, foldAlpha, currentCenterX, cubeCenterY, currentCenterZ, 0.35);
                drawTexturedQuad(view, chooseBuffer(solid, translucent, side.flags()), camera,
                        p0l, p1l, p1h, p0h, renderScale,
                        side.stateId(), textureDirection, side.colour(), side.packedLight(), side.flags(),
                        alpha, normal);
            }
        }
    }

    private static void renderOrbitalMesh(Matrix4f view,
                                          VertexConsumer solid, VertexConsumer translucent,
                                          PlanetLodVolumeTile tile, PlanetVolumeMeshCache.Mesh mesh,
                                          CubeNetSurfaceTransform transform,
                                          Vector3d planetPosition, Quaterniondc rotation,
                                          double renderedHalfExtent, double worldToRendered) {
        int worldMinX = tile.worldMinX();
        int worldMinZ = tile.worldMinZ();
        Vector3d rotatedFaceUp = new Vector3d(CubeFaceFrame.axes(tile.face()).up())
                .rotate(rotation);
        Vector3f topNormal = new Vector3f(
                (float) rotatedFaceUp.x(),
                (float) rotatedFaceUp.y(),
                (float) rotatedFaceUp.z()
        );
        for (PlanetVolumeMeshCache.TopFace top : mesh.tops()) {
            double x0 = worldMinX + top.x();
            double z0 = worldMinZ + top.z();
            Vector3d p00 = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                    renderedHalfExtent, worldToRendered, x0, z0, top.y(), 0.36);
            Vector3d p10 = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                    renderedHalfExtent, worldToRendered, x0 + 1.0, z0, top.y(), 0.36);
            Vector3d p11 = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                    renderedHalfExtent, worldToRendered, x0 + 1.0, z0 + 1.0, top.y(), 0.36);
            Vector3d p01 = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                    renderedHalfExtent, worldToRendered, x0, z0 + 1.0, top.y(), 0.36);
            drawTexturedQuad(view, chooseBuffer(solid, translucent, top.flags()), null,
                    p01, p11, p10, p00, 1.0,
                    top.stateId(), Direction.UP, top.colour(), top.packedLight(), top.flags(),
                    1.0f, topNormal);
        }
        for (PlanetVolumeMeshCache.SideFace side : mesh.sides()) {
            renderOrbitalSide(view, solid, translucent, tile, side, transform,
                    planetPosition, rotation, renderedHalfExtent, worldToRendered);
        }
    }

    private static void renderOrbitalSide(Matrix4f view,
                                          VertexConsumer solid, VertexConsumer translucent,
                                          PlanetLodVolumeTile tile, PlanetVolumeMeshCache.SideFace side,
                                          CubeNetSurfaceTransform transform,
                                          Vector3d planetPosition, Quaterniondc rotation,
                                          double renderedHalfExtent, double worldToRendered) {
        int unit = 1;
        int minX = tile.worldMinX();
        int minZ = tile.worldMinZ();
        CubeFaceFrame.Axes axes = CubeFaceFrame.axes(tile.face());
        for (int h = side.horizontal0(); h < side.horizontal1(); h += unit) {
            int h1 = Math.min(side.horizontal1(), h + unit);
            for (int y = side.y0(); y < side.y1(); y += unit) {
                int y1 = Math.min(side.y1(), y + unit);
                double x0, z0, x1, z1;
                Direction textureDirection;
                Vector3d localNormal;
                switch (side.side()) {
                    case WEST -> {
                        x0 = x1 = minX + side.fixed(); z0 = minZ + h; z1 = minZ + h1;
                        textureDirection = Direction.WEST; localNormal = new Vector3d(axes.right()).negate();
                    }
                    case EAST -> {
                        x0 = x1 = minX + side.fixed(); z0 = minZ + h1; z1 = minZ + h;
                        textureDirection = Direction.EAST; localNormal = new Vector3d(axes.right());
                    }
                    case NORTH -> {
                        z0 = z1 = minZ + side.fixed(); x0 = minX + h1; x1 = minX + h;
                        textureDirection = Direction.NORTH; localNormal = new Vector3d(axes.south()).negate();
                    }
                    default -> {
                        z0 = z1 = minZ + side.fixed(); x0 = minX + h; x1 = minX + h1;
                        textureDirection = Direction.SOUTH; localNormal = new Vector3d(axes.south());
                    }
                }
                Vector3d p0l = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, x0, z0, y, 0.37);
                Vector3d p1l = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, x1, z1, y, 0.37);
                Vector3d p1h = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, x1, z1, y1, 0.37);
                Vector3d p0h = orbitalPoint(tile.face(), transform, planetPosition, rotation,
                        renderedHalfExtent, worldToRendered, x0, z0, y1, 0.37);
                localNormal.rotate(rotation);
                Vector3f normal = new Vector3f(
                        (float) localNormal.x(),
                        (float) localNormal.y(),
                        (float) localNormal.z()
                );
                drawTexturedQuad(view, chooseBuffer(solid, translucent, side.flags()), null,
                        p0l, p1l, p1h, p0h, 1.0,
                        side.stateId(), textureDirection, side.colour(), side.packedLight(), side.flags(),
                        1.0f, normal);
            }
        }
    }

    private static VertexConsumer chooseBuffer(VertexConsumer solid, VertexConsumer translucent, byte flags) {
        return (flags & (PlanetLodVolumeTile.FLAG_TRANSLUCENT | PlanetLodVolumeTile.FLAG_LIQUID)) != 0
                ? translucent : solid;
    }

    private static void drawTexturedQuad(Matrix4f view, VertexConsumer buffer, Vec3 camera,
                                         Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3,
                                         double renderScale, int stateId, Direction direction,
                                         int colour, byte packedLight, byte flags, float alpha,
                                         Vector3f normal) {
        PlanetVolumeTextureCache.Sprite resolved = PlanetVolumeTextureCache.sprite(stateId, direction);
        TextureAtlasSprite sprite = resolved.texture();
        float shade = direction == Direction.UP ? 1.0f
                : direction == Direction.NORTH ? 0.72f
                : direction == Direction.SOUTH ? 0.88f : 0.80f;
        if ((flags & PlanetLodVolumeTile.FLAG_EMISSIVE) != 0) shade = 1.0f;
        int tint = resolved.tinted() ? colour : 0xFFFFFFFF;
        int r = Mth.clamp((int) (((tint >> 16) & 0xFF) * shade), 0, 255);
        int g = Mth.clamp((int) (((tint >> 8) & 0xFF) * shade), 0, 255);
        int b = Mth.clamp((int) ((tint & 0xFF) * shade), 0, 255);
        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        int light = (flags & PlanetLodVolumeTile.FLAG_EMISSIVE) != 0
                ? LightTexture.FULL_BRIGHT
                : LightTexture.pack(Byte.toUnsignedInt(packedLight) & 15,
                Math.max(10, (Byte.toUnsignedInt(packedLight) >>> 4) & 15));
        addTexturedVertex(view, buffer, camera, p0, renderScale, sprite.getU0(), sprite.getV1(),
                r, g, b, a, light, normal);
        addTexturedVertex(view, buffer, camera, p1, renderScale, sprite.getU1(), sprite.getV1(),
                r, g, b, a, light, normal);
        addTexturedVertex(view, buffer, camera, p2, renderScale, sprite.getU1(), sprite.getV0(),
                r, g, b, a, light, normal);
        addTexturedVertex(view, buffer, camera, p3, renderScale, sprite.getU0(), sprite.getV0(),
                r, g, b, a, light, normal);
    }

    private static void addTexturedVertex(Matrix4f view, VertexConsumer buffer, Vec3 camera,
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

    private static double renderDistanceCompression(Vec3 camera,
                                                     double centerX, double centerY, double centerZ,
                                                     int vanillaRadius) {
        double distance = Math.sqrt(square(centerX - camera.x)
                + square(centerY - camera.y) + square(centerZ - camera.z));
        double target = Math.max(640.0, vanillaRadius * 3.25);
        double fullScale = distance <= target ? 1.0 : target / distance;
        double start = Math.max(512.0, GenesisClientConfig.getWorldLodStartHeight());
        double full = Math.max(start + 1.0, GenesisClientConfig.getWorldCubeCompressionFullHeight());
        float influence = smoothstep((float) ((camera.y - start) / (full - start)));
        return Mth.lerp(influence, 1.0, fullScale);
    }

    private static double square(double value) {
        return value * value;
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }
}
