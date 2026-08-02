package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;
import shipwrights.genesis.client.PlanetSurfaceTextures;
import shipwrights.genesis.client.lod.EarthLodVisualFrame;
import shipwrights.genesis.client.lod.PlanetVoxelRenderer;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.client.shading.FaceShadow;
import shipwrights.genesis.client.shading.ShadowProjection;
import shipwrights.genesis.client.shading.ShadowRenderer;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.mixin.FogRendererAccessor;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import net.minecraft.core.Registry;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.surface.PlanetSurfaceSampler;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import static shipwrights.genesis.client.ShaderRegistry.getPlanetShadowRenderType;
import static shipwrights.genesis.client.ShaderRegistry.getTexturedPlanetRenderType;

public class PlanetRenderer implements CelestialRenderer {

    /**
     * Relief mesh LODs. The sampled atlas remains 64x64 per face; distant
     * planets use fewer quads while close approaches expose every sample.
     */
    private static final int TERRAIN_MESH_LOW = 16;
    private static final int TERRAIN_MESH_MEDIUM = 32;
    private static final int TERRAIN_MESH_HIGH = PlanetSurfaceSampler.RESOLUTION;
    private static final double PLANET_BLOCKS_PER_SPACE_BLOCK =
            CubeNetSurfaceTransform.PLANET_BLOCKS_PER_SPACE_BLOCK;
    private static final ResourceLocation OVERWORLD_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final int PAINTED_GLOBE_MESH = 32;

    private static final boolean USE_TEST_SHADOWS = false; // Set to false to use real shadows

    @Override
    public void teardown(@NotNull RenderLevelStageEvent event, @NotNull VantagePoint vantagePoint) {
        CelestialRenderer.super.teardown(event, vantagePoint);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        ResourceLocation planetID = registry.getResourceKey(toRender).orElseThrow().location();
        // The overworld draws its own terrain LOD while the player is still in
        // that world. Rendering a second celestial Earth here is exactly the
        // detached-model effect the world-to-planet pipeline is designed to
        // remove.
        if (OVERWORLD_ID.equals(planetID)
                && vantagePoint instanceof VantagePoint.OnCelestial onCelestial
                && onCelestial.celestial().equals(toRender)) {
            return;
        }
        Vector3dc position = toRender.getPosition(ticks, partialTick, registry);
        Quaterniondc rotation = toRender.getRotation(ticks, partialTick, registry);
        // Earth must use its physical size so the 8192-block surface face and
        // the 512-space-block orbital cube meet at exactly the same scale.
        double halfExtent = OVERWORLD_ID.equals(planetID)
                ? toRender.getActualSize() * 0.5
                : CelestialRenderCoordinates.getVisualHalfExtent(toRender);
        float alpha = 1f;

        Vector3dc starPosition = toRender.getNearestStar(ticks, partialTick, registry).getPosition(ticks, partialTick, registry);


        List<FaceShadow> shadows;
        if (USE_TEST_SHADOWS) {
            shadows = createTestShadows(halfExtent);
        } else {
            // Get all the data needed for shadow computation
            OBB selfOBB = toRender.getOBB(ticks, partialTick, registry);
            List<Celestial> allCelestials = registry.stream().filter(it -> it.type().castsShadow() && !it.equals(toRender)).toList();
            List<OBB> otherOBBs = allCelestials.stream().map(it -> it.getOBB(ticks, partialTick, registry)).toList();

            shadows = ShadowProjection.computeShadows(selfOBB, otherOBBs, starPosition);
        }

        Quaterniond observerRotation = CelestialRenderCoordinates.getObserverCelestialRotation(vantagePoint);
        Vector3d lightDir = new Vector3d(position).sub(starPosition).rotate(observerRotation.conjugate(new Quaterniond()));
        ShaderInstance shader = ShaderRegistry.PLANET_TEXTURED_SHADER.getShaderInstance();
        shader.safeGetUniform("LightDirection").set((float) lightDir.x, (float) lightDir.y, (float) lightDir.z);
        Vec3 skyColor = Vec3.ZERO;

        if (vantagePoint instanceof VantagePoint.OnCelestial oc) {
            skyColor = PlanetDimensionEffects.cachedSkyColor;

            double starBrightness = PlanetDimensionEffects.cachedStarBrightness;
            Vector3d observerPosition = CelestialRenderCoordinates.getObserverCelestialPosition(vantagePoint, event.getCamera().getPosition());
            double distanceSquared = observerPosition.distanceSquared(position);

            if (oc.celestial().equals(toRender)) {
                if (!GenesisClientConfig.shouldRenderCurrentPlanet()) {
                    return;
                }

                alpha = CelestialRenderCoordinates.getCurrentPlanetAlpha(event.getCamera().getPosition());
                if (alpha <= 0.001f) {
                    return;
                }
            } else if (starBrightness < 0.05 && distanceSquared > 4096.0 * 4096.0) {
                return;
            }
        }

        CelestialRenderCoordinates.RenderTransform renderTransform =
                CelestialRenderCoordinates.getRenderTransform(toRender, vantagePoint, event.getCamera().getPosition(), ticks, partialTick, registry);
        position = renderTransform.position();
        rotation = renderTransform.rotation();

        CubeNetSurfaceTransform earthTransform = null;
        if (OVERWORLD_ID.equals(planetID) && GenesisMod.isSpaceDimension(level)) {
            earthTransform = new CubeNetSurfaceTransform(
                    toRender.getActualSize(), shipwrights.genesis.teleportation.SpaceTravelManager.entryPadding(toRender.getActualSize()));
            EarthLodVisualFrame.OrbitFrame visualFrame = EarthLodVisualFrame.orbitFrame(
                    new Vector3d(position), toRender.getActualSize() * 0.5, earthTransform);
            position = visualFrame.planetPosition();
            halfExtent = visualFrame.halfExtent();
        }

        double renderDistanceScale = CelestialRenderCoordinates.getPlanetRenderDistanceScale(position);
        if (renderDistanceScale != 1.0) {
            position = CelestialRenderCoordinates.scaleRenderPosition(position, renderDistanceScale);
            halfExtent *= renderDistanceScale;
        }

        shader.safeGetUniform("SkyColor").set((float) skyColor.x, (float) skyColor.y, (float) skyColor.z);
        // In space the planet writes REAL depth so it genuinely occludes the star
        // behind it — the shader used to pin every planet pixel to the far plane
        // (gl_FragDepth = 1.0), which is why the sun always won the depth test and
        // bled through. On a planet surface it stays pinned to the far plane so
        // distant terrain still draws over it.
        boolean inSpace = GenesisMod.isSpaceDimension(level);
        shader.safeGetUniform("FlatDepth").set(inSpace ? 0.0f : 1.0f);

        // Cloud shell. Driven by the body's own atmosphere, so airless worlds
        // get none without needing to be listed anywhere, and a thick
        // atmosphere reads as heavier cover than a thin one.
        double cloudDensity = 0.0;
        if (toRender.properties() instanceof shipwrights.genesis.space.properties.PlanetProperties planetProps) {
            cloudDensity = net.minecraft.util.Mth.clamp(planetProps.atmosphere().density(), 0.0, 1.0);
        }
        shader.safeGetUniform("CloudDensity").set((float) cloudDensity);
        // Wall-clock drift: the clouds keep moving while the world is paused,
        // and at a rate that does not change with the planet's day length.
        shader.safeGetUniform("CloudTime").set((float) ((System.currentTimeMillis() % 100_000_000L) / 1000.0));

        // Earth is never a model. Whenever it is on screen from space it is
        // drawn as the save's own voxel terrain folded onto six cube faces, so
        // the village the player took off from is the village they can see and
        // fly back down to. Only genuinely painted bodies take the textured
        // path below.
        boolean voxelEarth = OVERWORLD_ID.equals(planetID) && inSpace && earthTransform != null;
        if (voxelEarth) {
            renderVoxelEarth(event, toRender, vantagePoint, earthTransform, level,
                    position, rotation, halfExtent);
        } else {
            renderPlanetAt(planetID, shadows, event.getPoseStack(), position.x(), position.y(),
                    position.z(), halfExtent, toRender.getActualSize() * 0.5, rotation, alpha);
        }

        new PlanetAtmosphereRenderer().invoke(event, toRender, vantagePoint);
    }

    /**
     * Draws Earth from space as the actual world's voxel terrain.
     *
     * <p>The camera is expressed in the planet's own rotating cube frame so the
     * renderer, the LOD selector and the streaming interest manager all measure
     * distance to the same terrain. The survey crosshair is resolved against the
     * same cube by an exact ray intersection, which is what lets the player pick
     * a real village out of orbit and descend to it.</p>
     */
    private void renderVoxelEarth(RenderLevelStageEvent event, Celestial toRender,
                                  VantagePoint vantagePoint,
                                  CubeNetSurfaceTransform earthTransform,
                                  ClientLevel level,
                                  Vector3dc renderedPosition, Quaterniondc rotation,
                                  double renderedHalfExtent) {
        shipwrights.genesis.space.planet.CubeSurfaceProjection projection =
                new shipwrights.genesis.space.planet.CubeSurfaceProjection(
                        earthTransform, level.getSeaLevel());

        // Camera in the planet's rotating frame: undo the draw translation, then
        // the planet's own rotation.
        Quaterniond inverseRotation = new Quaterniond(rotation).conjugate();
        Vector3d localCamera = new Vector3d(renderedPosition).negate().rotate(inverseRotation);
        Vector3d localLook = new Vector3d(
                event.getCamera().getLookVector().x(),
                event.getCamera().getLookVector().y(),
                event.getCamera().getLookVector().z())
                .rotate(CelestialRenderCoordinates.getObserverCelestialRotation(vantagePoint))
                .rotate(inverseRotation);
        if (localLook.lengthSquared() < 1.0E-12) localLook.set(0.0, 0.0, -1.0);

        double worldToRendered = renderedHalfExtent / Math.max(1.0, projection.faceHalfSpan());
        Vector3d cubeCamera = new Vector3d(localCamera);

        shipwrights.genesis.space.planet.CubeSurfaceProjection.CubeSurfaceHit hit =
                projection.cubeRayIntersection(
                        new Vector3d(cubeCamera).div(Math.max(1.0E-9, worldToRendered)),
                        localLook, 0.0);
        CubeNetSurfaceTransform.Face focusFace = hit != null ? hit.face()
                : projection.cubeToFaceLocal(new Vector3d(cubeCamera).negate()).face();
        if (hit != null) {
            shipwrights.genesis.client.OrbitalSurveyController.updateTarget(hit.face(),
                    projection.faceToWorldX(hit.face(), hit.surfaceX()),
                    projection.faceToWorldZ(hit.face(), hit.surfaceZ()), true);
            shipwrights.genesis.space.planet.PlanetRenderDiagnostics.setLocation(hit.face(),
                    hit.surfaceX(), hit.surfaceZ(), hit.distance(), hit.distance());
        }
        shipwrights.genesis.space.planet.PlanetRenderDiagnostics.setFold(1.0f);

        PlanetVoxelRenderer.renderOrbit(event.getModelViewMatrix(), projection,
                new Vector3d(renderedPosition), rotation, renderedHalfExtent, worldToRendered,
                cubeCamera, localLook, focusFace);
    }

    private void renderPlanetAt(ResourceLocation planetID, List<FaceShadow> shadows, PoseStack poseStack, double x, double y, double z, double halfExtent, double physicalHalfExtent, Quaterniondc localRotation, float alpha) {
        // Prefer the planet's real terrain, sampled from its own dimension, over
        // the painted texture. Bodies that have not been sampled — and any that
        // are pure set dressing with no world behind them — keep the painting.
        ResourceLocation live = shipwrights.genesis.client.PlanetSurfaceTextures.get(planetID);
        ResourceLocation textureLocation = live != null ? live
                : ResourceLocation.parse("genesis:planets/" + planetID.getNamespace() + "/" + planetID.getPath());

        // Set up buffer source
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var renderType = getTexturedPlanetRenderType(textureLocation);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        // Clone the matrix
        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        // Apply translation
        matrix.translate((float) x, (float) y, (float) z);

        // Apply rotation
        Quaternionf rotation = new Quaternionf(localRotation);
        matrix.rotate(rotation);

        float halfSize = (float) halfExtent;

        // UV layout (3x2 grid):
        // | north (0,0)     | west (1/3,0)   | south (2/3,0)  |
        // | east (0,0.5)    | down (1/3,0.5) | up (2/3,0.5)   |
        float third = 1.0f / 3.0f;
        float twoThirds = 2.0f / 3.0f;

        PlanetSurfaceData liveSurface = PlanetSurfaceTextures.surface(planetID);
        boolean worldLodCube = OVERWORLD_ID.equals(planetID);
        if (worldLodCube && live != null && liveSurface != null && liveSurface.isValid() && physicalHalfExtent > 0.0) {
            int meshResolution = terrainMeshResolution(x, y, z, halfExtent);
            addTerrainCube(matrix, buffer, halfSize, physicalHalfExtent, liveSurface,
                    meshResolution, rotation, alpha);
        } else {
            addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, 0.0f, 1.0f), rotation, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, twoThirds, 0.0f, 1.0f, 0.5f, alpha);        // South face (+Z)
            addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, 0.0f, -1.0f), rotation, halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, 0.0f, 0.0f, third, 0.5f, alpha);        // North face (-Z)
            addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(-1.0f, 0.0f, 0.0f), rotation, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, third, 0.0f, twoThirds, 0.5f, alpha);       // West face (-X)
            addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(1.0f, 0.0f, 0.0f), rotation, halfSize, -halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, 0.0f, 0.5f, third, 1.0f, alpha);            // East face (+X)
            addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, -1.0f, 0.0f), rotation, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, third, 0.5f, twoThirds, 1.0f, alpha);       // Down face (-Y)
            addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, 1.0f, 0.0f), rotation, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, twoThirds, 0.5f, 1.0f, 1.0f, alpha);        // Up face (+Y)
        }

        // End batch to flush planet rendering
        bufferSource.endBatch(renderType);

        MultiBufferSource.BufferSource bufferSource1 = Minecraft.getInstance().renderBuffers().bufferSource();
        bufferSource1.endBatch();
        // Render shadows on planet faces
        renderShadows(shadows, poseStack, x, y, z, halfExtent, localRotation);
        bufferSource1.endBatch();
    }


    /**
     * Chooses geometry detail from apparent size. A close planet gets one quad
     * per sampled texel; distant bodies retain the same texture and silhouette
     * at a fraction of the vertex cost.
     */
    private static int terrainMeshResolution(double x, double y, double z, double halfExtent) {
        double distance = Math.sqrt(x * x + y * y + z * z);
        double apparentHalfSize = halfExtent / Math.max(halfExtent, distance);
        if (apparentHalfSize >= 0.20) {
            return TERRAIN_MESH_HIGH;
        }
        if (apparentHalfSize >= 0.06) {
            return TERRAIN_MESH_MEDIUM;
        }
        return TERRAIN_MESH_LOW;
    }

    /** Draws the six world regions as the actual height-displaced cube Earth. */
    private static void addTerrainCube(Matrix4f matrix, VertexConsumer buffer, float halfSize,
                                       double physicalHalfExtent, PlanetSurfaceData surface,
                                       int meshResolution, Quaternionf rotation, float alpha) {
        double reliefScale = halfSize * PLANET_BLOCKS_PER_SPACE_BLOCK / physicalHalfExtent;
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (int row = 0; row < meshResolution; row++) {
                double t0 = row / (double) meshResolution;
                double t1 = (row + 1) / (double) meshResolution;
                for (int column = 0; column < meshResolution; column++) {
                    double s0 = column / (double) meshResolution;
                    double s1 = (column + 1) / (double) meshResolution;
                    addTerrainCubeVertex(matrix, buffer, face, s0, t1, halfSize, reliefScale,
                            surface, rotation, alpha);
                    addTerrainCubeVertex(matrix, buffer, face, s1, t1, halfSize, reliefScale,
                            surface, rotation, alpha);
                    addTerrainCubeVertex(matrix, buffer, face, s1, t0, halfSize, reliefScale,
                            surface, rotation, alpha);
                    addTerrainCubeVertex(matrix, buffer, face, s0, t0, halfSize, reliefScale,
                            surface, rotation, alpha);
                }
            }
        }
    }

    private static void addTerrainCubeVertex(Matrix4f matrix, VertexConsumer buffer,
                                             CubeNetSurfaceTransform.Face face, double s, double t,
                                             float halfSize, double reliefScale, PlanetSurfaceData surface,
                                             Quaternionf rotation, float alpha) {
        double elevation = sampleHeight(surface, face, s, t) - surface.seaLevel();
        double u = (s * 2.0 - 1.0) * halfSize;
        double v = (t * 2.0 - 1.0) * halfSize;
        Vector3d point = CubeFaceFrame.cubePoint(face, u, v, halfSize, elevation * reliefScale);
        float textureU = (float) ((face.atlasColumn() + s) / 3.0);
        float textureV = (float) ((face.atlasRow() + t) / 2.0);
        Vector3d normal = new Vector3d(CubeFaceFrame.axes(face).up());
        addTexturedVertexWithLighting(matrix, buffer, (float) point.x, (float) point.y, (float) point.z,
                textureU, textureV, new Vector3f((float) normal.x, (float) normal.y, (float) normal.z),
                rotation, alpha);
    }

    /** Legacy spherical implementation retained only for non-Earth experiments. */
    private static void addTerrainGlobe(Matrix4f matrix, VertexConsumer buffer, float halfSize,
                                        double physicalHalfExtent, PlanetSurfaceData surface,
                                        int meshResolution, Quaternionf rotation, float alpha) {
        // One planet block is 1/16 of a celestial-space block. Anchor the
        // sphere at sea level; only terrain above/below sea level changes radius.
        double reliefScale = halfSize * PLANET_BLOCKS_PER_SPACE_BLOCK / physicalHalfExtent;

        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (int row = 0; row < meshResolution; row++) {
                double t0 = row / (double) meshResolution;
                double t1 = (row + 1) / (double) meshResolution;
                for (int column = 0; column < meshResolution; column++) {
                    double s0 = column / (double) meshResolution;
                    double s1 = (column + 1) / (double) meshResolution;

                    addTerrainGlobeVertex(matrix, buffer, face, s0, t1, halfSize, reliefScale,
                            surface, rotation, alpha);
                    addTerrainGlobeVertex(matrix, buffer, face, s1, t1, halfSize, reliefScale,
                            surface, rotation, alpha);
                    addTerrainGlobeVertex(matrix, buffer, face, s1, t0, halfSize, reliefScale,
                            surface, rotation, alpha);
                    addTerrainGlobeVertex(matrix, buffer, face, s0, t0, halfSize, reliefScale,
                            surface, rotation, alpha);
                }
            }
        }
    }

    /** Painted fallback using identical globe geometry but no terrain displacement. */
    private static void addPaintedGlobe(Matrix4f matrix, VertexConsumer buffer, float halfSize,
                                        int meshResolution, Quaternionf rotation, float alpha) {
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (int row = 0; row < meshResolution; row++) {
                double t0 = row / (double) meshResolution;
                double t1 = (row + 1) / (double) meshResolution;
                for (int column = 0; column < meshResolution; column++) {
                    double s0 = column / (double) meshResolution;
                    double s1 = (column + 1) / (double) meshResolution;
                    addPaintedGlobeVertex(matrix, buffer, face, s0, t1, halfSize, rotation, alpha);
                    addPaintedGlobeVertex(matrix, buffer, face, s1, t1, halfSize, rotation, alpha);
                    addPaintedGlobeVertex(matrix, buffer, face, s1, t0, halfSize, rotation, alpha);
                    addPaintedGlobeVertex(matrix, buffer, face, s0, t0, halfSize, rotation, alpha);
                }
            }
        }
    }

    private static void addTerrainGlobeVertex(Matrix4f matrix, VertexConsumer buffer,
                                               CubeNetSurfaceTransform.Face face, double s, double t,
                                               float halfSize, double reliefScale, PlanetSurfaceData surface,
                                               Quaternionf rotation, float alpha) {
        double elevation = sampleHeight(surface, face, s, t) - surface.seaLevel();

        // The six flat world regions are not seam-wrapped yet. Force exact
        // shared edges to the same radius and blend relief back in over two LOD
        // cells, preventing cracks without changing any travel coordinates.
        double edge = Math.min(Math.min(s, 1.0 - s), Math.min(t, 1.0 - t));
        double edgeBand = 2.0 / PlanetSurfaceSampler.RESOLUTION;
        double edgeFade = smoothstep(Mth.clamp(edge / edgeBand, 0.0, 1.0));
        double radius = halfSize + elevation * reliefScale * edgeFade;

        Vector3f point = globePoint(face, s, t, radius);
        float textureU = (float) ((face.atlasColumn() + s) / 3.0);
        float textureV = (float) ((face.atlasRow() + t) / 2.0);
        addTexturedVertexWithLighting(matrix, buffer, point.x, point.y, point.z,
                textureU, textureV, new Vector3f(point).normalize(), rotation, alpha);
    }

    private static void addPaintedGlobeVertex(Matrix4f matrix, VertexConsumer buffer,
                                               CubeNetSurfaceTransform.Face face, double s, double t,
                                               float radius, Quaternionf rotation, float alpha) {
        Vector3f point = globePoint(face, s, t, radius);
        float textureU = (float) ((face.atlasColumn() + s) / 3.0);
        float textureV = (float) ((face.atlasRow() + t) / 2.0);
        addTexturedVertexWithLighting(matrix, buffer, point.x, point.y, point.z,
                textureU, textureV, new Vector3f(point).normalize(), rotation, alpha);
    }

    /** Bilinear height lookup; samples are texel-centred, so edges extend cleanly. */
    private static double sampleHeight(PlanetSurfaceData surface, CubeNetSurfaceTransform.Face face,
                                       double s, double t) {
        int resolution = PlanetSurfaceSampler.RESOLUTION;
        double sampleX = Mth.clamp(s * resolution - 0.5, 0.0, resolution - 1.0);
        double sampleY = Mth.clamp(t * resolution - 0.5, 0.0, resolution - 1.0);
        int x0 = (int) Math.floor(sampleX);
        int y0 = (int) Math.floor(sampleY);
        int x1 = Math.min(x0 + 1, resolution - 1);
        int y1 = Math.min(y0 + 1, resolution - 1);
        double fx = sampleX - x0;
        double fy = sampleY - y0;
        short[] heights = surface.heights();
        double h00 = heights[PlanetSurfaceData.index(face, x0, y0)];
        double h10 = heights[PlanetSurfaceData.index(face, x1, y0)];
        double h01 = heights[PlanetSurfaceData.index(face, x0, y1)];
        double h11 = heights[PlanetSurfaceData.index(face, x1, y1)];
        double top = Mth.lerp(fx, h00, h10);
        double bottom = Mth.lerp(fx, h01, h11);
        return Mth.lerp(fy, top, bottom);
    }

    /** Cube-face UV -> low-distortion spherified-cube direction -> radius. */
    private static Vector3f globePoint(CubeNetSurfaceTransform.Face face, double s, double t, double radius) {
        double u = s * 2.0 - 1.0;
        double v = t * 2.0 - 1.0;
        Vector3d cube = switch (face) {
            case NORTH -> new Vector3d(-u, -v, -1.0);
            case WEST -> new Vector3d(-1.0, -v, u);
            case SOUTH -> new Vector3d(u, -v, 1.0);
            case EAST -> new Vector3d(1.0, -v, -u);
            case DOWN -> new Vector3d(u, -1.0, -v);
            case UP -> new Vector3d(u, 1.0, v);
        };

        double x2 = cube.x * cube.x;
        double y2 = cube.y * cube.y;
        double z2 = cube.z * cube.z;
        double sx = cube.x * Math.sqrt(Math.max(0.0, 1.0 - y2 * 0.5 - z2 * 0.5 + y2 * z2 / 3.0));
        double sy = cube.y * Math.sqrt(Math.max(0.0, 1.0 - z2 * 0.5 - x2 * 0.5 + z2 * x2 / 3.0));
        double sz = cube.z * Math.sqrt(Math.max(0.0, 1.0 - x2 * 0.5 - y2 * 0.5 + x2 * y2 / 3.0));
        Vector3d sphere = new Vector3d(sx, sy, sz).normalize().mul(radius);
        return new Vector3f((float) sphere.x, (float) sphere.y, (float) sphere.z);
    }

    private static double smoothstep(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static void addTexturedCubeFace(Matrix4f matrix, VertexConsumer buffer, float halfSize,
                                            Vector3f normal, Quaternionf rotation,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float x4, float y4, float z4,
                                            float u1, float v1, float u2, float v2, float alpha) {
        addTexturedVertexWithLighting(matrix, buffer, x1, y1, z1, u1, v2, normal, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x2, y2, z2, u2, v2, normal, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x3, y3, z3, u2, v1, normal, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x4, y4, z4, u1, v1, normal, rotation, alpha);
    }

    private static void addTexturedVertexWithLighting(Matrix4f matrix, VertexConsumer buffer,
                                                      float x, float y, float z,
                                                      float u, float v,
                                                      Vector3f normal, Quaternionf rotation, float alpha) {
        // Pass fog color in RGB and alpha in A for shader interpolation
        int fogRed = (int) (255 * FogRendererAccessor.getFogRed());
        int fogGreen = (int) (255 * FogRendererAccessor.getFogGreen());
        int fogBlue = (int) (255 * FogRendererAccessor.getFogBlue());

        Vector3f rotatedNormal = new Vector3f(x,y,z);
        rotatedNormal = rotation.transform(rotatedNormal.normalize());

        buffer.addVertex(matrix, x, y, z)
            .setUv(u, v)
            .setColor(fogRed, fogGreen, fogBlue, (int)(alpha * 255))
            .setNormal(rotatedNormal.x, rotatedNormal.y, rotatedNormal.z)
            ;
    }

    private void renderShadows(List<FaceShadow> shadows, PoseStack poseStack, double x, double y, double z, double halfExtent, Quaterniondc localRotation) {
        if (shadows == null || shadows.isEmpty()) {
            return;
        }

        // Use a single edgeWidth across all face shadows so that the soft
        // falloff scale is consistent across cube edges.
        float sharedEdgeWidth = 0.0001f;
        for (FaceShadow shadow : shadows) {
            sharedEdgeWidth = Math.max(sharedEdgeWidth, ShadowRenderer.computeEdgeWidth(shadow));
        }

        // Render each shadow in its own batch so we can upload
        // per-shadow projection vertices to the shader.
        for (FaceShadow shadow : shadows) {
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer shadowBuffer = bufferSource.getBuffer(getPlanetShadowRenderType());

            // Clone and transform matrix (same as planet rendering)
            Matrix4f matrix;
            try {
                matrix = (Matrix4f) poseStack.last().pose().clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }

            matrix.translate((float) x, (float) y, (float) z);
            matrix.rotate(new Quaternionf(localRotation));

            // Compute up to 8 vertices for this shadow from the projected
            // polygon, transform them with the same matrix used for
            // rendering, and upload them as uniforms. This keeps the
            // shader's ShadowVertexN positions in the same space as the
            // Position/localPos attribute.
            ShaderInstance shader = ShaderRegistry.PLANET_SHADOW_SHADER.getShaderInstance();
            if (shader != null) {
                List<Vector2dc> polygon = shadow.polygon();
                AAPlane plane = shadow.plane();

                int count = Math.min(polygon.size(), 8);

                Uniform countUniform = shader.getUniform("ShadowVertexCount");
                if (countUniform != null) {
                    countUniform.set((float) count);
                }

                // Build a bitmask of edges that lie on cube face clipping boundaries.
                // These are artifacts of Sutherland-Hodgman clipping, not real shadow
                // silhouette edges, so they must not contribute to the fade falloff.
                int boundaryMask = 0;
                double eps = halfExtent * 1e-4;
                for (int i = 0; i < count; i++) {
                    int j = (i + 1) % count;
                    Vector2dc a = polygon.get(i);
                    Vector2dc b = polygon.get(j);
                    boolean onBoundary =
                        (Math.abs(a.x() - halfExtent) < eps && Math.abs(b.x() - halfExtent) < eps) ||
                        (Math.abs(a.x() + halfExtent) < eps && Math.abs(b.x() + halfExtent) < eps) ||
                        (Math.abs(a.y() - halfExtent) < eps && Math.abs(b.y() - halfExtent) < eps) ||
                        (Math.abs(a.y() + halfExtent) < eps && Math.abs(b.y() + halfExtent) < eps);
                    if (onBoundary) {
                        boundaryMask |= (1 << i);
                    }
                }
                Uniform maskUniform = shader.getUniform("ShadowEdgeMask");
                if (maskUniform != null) {
                    maskUniform.set((float) boundaryMask);
                }

                Uniform edgeWidthUniform = shader.getUniform("ShadowEdgeWidth");
                if (edgeWidthUniform != null) {
                    edgeWidthUniform.set(sharedEdgeWidth);
                }

                for (int i = 0; i < 8; i++) {
                    Uniform u = shader.getUniform("ShadowVertex[" + i + "]");
                    if (u != null) {
                        if (i < count) {
                        // Start from local-space projection and apply
                        // the same z-fighting offset and matrix that
                        // the geometry uses.
                        Vector3d v3d = ShadowRenderer.applyZFightingOffset(
                            ShadowRenderer.convertPlaneToLocal3D(polygon.get(i), plane),
                            plane,
                            halfExtent
                        );

                        Vector3f transformed = new Vector3f(
                            (float) v3d.x,
                            (float) v3d.y,
                            (float) v3d.z
                        );
                        matrix.transformPosition(transformed);

                        u.set(transformed.x, transformed.y, transformed.z);
                        } else {
                            u.set(0.0F, 0.0F, 0.0F);
                        }
                    }
                }
            }

            ShadowRenderer.renderShadow(shadow, matrix, shadowBuffer, halfExtent);

            // Flush this shadow's batch so its uniforms apply only to it
            bufferSource.endBatch(getPlanetShadowRenderType());

            MultiBufferSource.BufferSource bufferSource1 = Minecraft.getInstance().renderBuffers().bufferSource();
            bufferSource1.endBatch();
        }
    }

    /**
     * Creates test shadows for debugging the rendering pipeline.
     * Generates simple square shadows on each face of the cube.
     */
    private static List<FaceShadow> createTestShadows(double halfExtent) {
        List<FaceShadow> testShadows = new ArrayList<>();

        // Create a square shadow on the +Y face (top face) - TESTING IF Y AXIS WORKS
        AAPlane topPlane = new AAPlane(new Vector3i(0, 1, 0), halfExtent);
        List<Vector2dc> topSquare = List.of(
                new Vector2d(-halfExtent * 0.3, -halfExtent * 0.3),
                new Vector2d(halfExtent * 0.3, -halfExtent * 0.3),
                new Vector2d(halfExtent * 0.3, halfExtent * 0.3),
                new Vector2d(-halfExtent * 0.3, halfExtent * 0.3)
        );
        testShadows.add(new FaceShadow(topPlane, topSquare));

        // Create a triangle shadow on the +Z face (front face)
        // Note: Vertex order matters for face culling
        AAPlane frontPlane = new AAPlane(new Vector3i(0, 0, 1), halfExtent);
        List<Vector2dc> frontTriangle = List.of(
                new Vector2d(0, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.4, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.4, halfExtent * 0.4)
        );
        testShadows.add(new FaceShadow(frontPlane, frontTriangle));

        // Create a pentagon shadow on the +X face (right face)
        AAPlane rightPlane = new AAPlane(new Vector3i(1, 0, 0), halfExtent);
        List<Vector2dc> rightPentagon = List.of(
                new Vector2d(0, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.3, -halfExtent * 0.2),
                new Vector2d(halfExtent * 0.3, halfExtent * 0.2),
                new Vector2d(0, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.3, 0)
        );
        testShadows.add(new FaceShadow(rightPlane, rightPentagon));

        // Create a hexagon shadow on the -Y face (bottom face)
        AAPlane bottomPlane = new AAPlane(new Vector3i(0, -1, 0), -halfExtent);
        List<Vector2dc> bottomHexagon = List.of(
                new Vector2d(halfExtent * 0.3, 0),
                new Vector2d(halfExtent * 0.15, halfExtent * 0.3),
                new Vector2d(-halfExtent * 0.15, halfExtent * 0.3),
                new Vector2d(-halfExtent * 0.3, 0),
                new Vector2d(-halfExtent * 0.15, -halfExtent * 0.3),
                new Vector2d(halfExtent * 0.15, -halfExtent * 0.3)
        );
        testShadows.add(new FaceShadow(bottomPlane, bottomHexagon));

        // Create a diamond shadow on the -Z face (back face)
        AAPlane backPlane = new AAPlane(new Vector3i(0, 0, -1), -halfExtent);
        List<Vector2dc> backDiamond = List.of(
                new Vector2d(0, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.3, 0),
                new Vector2d(0, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.3, 0)
        );
        testShadows.add(new FaceShadow(backPlane, backDiamond));

        // Create a rectangle shadow on the -X face (left face)
        AAPlane leftPlane = new AAPlane(new Vector3i(-1, 0, 0), -halfExtent);
        List<Vector2dc> leftRectangle = List.of(
                new Vector2d(-halfExtent * 0.2, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.2, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.2, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.2, halfExtent * 0.4)
        );
        testShadows.add(new FaceShadow(leftPlane, leftRectangle));

        return testShadows;
    }
}
