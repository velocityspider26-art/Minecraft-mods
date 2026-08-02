package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import net.minecraft.core.Registry;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.PlanetProperties;

import java.lang.Math;

import static shipwrights.genesis.client.ShaderRegistry.getPlanetAtmosphereRenderType;

public class PlanetAtmosphereRenderer implements CelestialRenderer {
    private static final ResourceLocation OVERWORLD_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final int SPHERE_MESH_RESOLUTION = 24;

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Vector3dc position = toRender.getPosition(ticks, partialTick, registry);
        ResourceLocation celestialId = registry.getResourceKey(toRender).orElseThrow().location();
        CelestialRenderCoordinates.RenderTransform renderTransform =
                CelestialRenderCoordinates.getRenderTransform(toRender, vantagePoint, event.getCamera().getPosition(), ticks, partialTick, registry);
        position = renderTransform.position();
        Quaterniondc rotation = renderTransform.rotation();
        double size = OVERWORLD_ID.equals(celestialId)
                ? toRender.getActualSize()
                : CelestialRenderCoordinates.getVisualHalfExtent(toRender) * 2.0;
        double renderDistanceScale = CelestialRenderCoordinates.getPlanetRenderDistanceScale(position);
        if (renderDistanceScale != 1.0) {
            position = CelestialRenderCoordinates.scaleRenderPosition(position, renderDistanceScale);
            size *= renderDistanceScale;
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer atmosphereBuffer = bufferSource.getBuffer(getPlanetAtmosphereRenderType());
        // Earth is a cube planet. Its atmosphere follows the same cube shell;
        // the old spherical special case belonged to the detached-globe pass.
        boolean spherical = false;
        renderAtmosphere(event.getPoseStack(), atmosphereBuffer, size, position, rotation, event, toRender, vantagePoint, spherical);
        bufferSource.endBatch(getPlanetAtmosphereRenderType());
    }

    private void renderAtmosphere(PoseStack poseStack, VertexConsumer buffer, double size, Vector3dc center, Quaterniondc localRotation, @NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, VantagePoint vantagePoint, boolean spherical) {

        // Camera is at render origin; planet center is at `center` in render space.
        // Camera relative to planet = -center; transform into planet local space for shader.
        Vector3f cameraPos0 = new Vector3f((float) -center.x(), (float) -center.y(), (float) -center.z());
        Vector3f rotatedCameraPos = new Vector3f(cameraPos0);
        new Quaternionf(localRotation).conjugate().transform(rotatedCameraPos);

        float halfSize = (float) size / 2;

        if (!(toRender.properties() instanceof PlanetProperties props)) return;

        // densityFade only applies when leaving a planet's atmosphere (y-based fade in planet dimension).
        // When viewing from space, use full density.
        double densityFade = (vantagePoint instanceof VantagePoint.InSpace)
                ? 1.0
                : Mth.clamp((event.getCamera().getPosition().y - 320.0) / (GenesisCommonConfig.getAtmosphereEntryHeight() - 320.0), 0.0, 1.0);

        double starBrightness = PlanetDimensionEffects.cachedStarBrightness;

        if (vantagePoint instanceof VantagePoint.OnCelestial oc && !oc.celestial().equals(toRender)) {
            densityFade = starBrightness;
        }

        float relativeAtmosphereSize = 1.0f + 0.3f * (float) props.atmosphere().thickness();

        // Fade the shell out right as the camera crosses into it — a translucent
        // shell drawn from inside has no stable face order and flickers, which
        // is the shimmer while approaching a planet. The band is tight around
        // the shell surface, so the glow seen from any distance is untouched
        // and only the instant of crossing fades.
        double cameraDistance = new Vector3d(center).length();
        double shellRadius = (size / 2.0) * relativeAtmosphereSize;
        double insideFade = Mth.clamp((cameraDistance - shellRadius * 0.95) / (shellRadius * 0.1), 0.0, 1.0);
        densityFade *= insideFade;

        if (props.atmosphere().density() * densityFade < 0.01) return;

        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        Registry<Celestial> reg = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);

        Vector3dc starPosition = toRender.getNearestStar(ticks, partialTick, reg).getPosition(ticks, partialTick, reg);
        Vector3dc planetPosition = toRender.getPosition(ticks, partialTick, reg);

        Quaterniondc lightRot = toRender.getRotation(ticks, partialTick, reg);
        Vector3d lightDir = new Vector3d(planetPosition).sub(starPosition).rotate(new Quaterniond(lightRot).conjugate());
        ShaderInstance shader = ShaderRegistry.PLANET_ATMOSPHERE_SHADER.getShaderInstance();
        shader.safeGetUniform("LightDirection").set((float) lightDir.x, (float) lightDir.y, (float) lightDir.z);
        Uniform uniform = shader.getUniform("CameraPosition");
        if (uniform != null) {
            uniform.set(rotatedCameraPos.x, rotatedCameraPos.y, rotatedCameraPos.z);
        }

        Uniform uniform1 = shader.getUniform("HalfSize");
        if (uniform1 != null) {
            uniform1.set(halfSize);
        }

        Uniform uniformThickness = shader.getUniform("AtmosphereThickness");
        if (uniformThickness != null) {
            uniformThickness.set(relativeAtmosphereSize);
        }

        Uniform uniformDensity = shader.getUniform("Density");
        if (uniformDensity != null) {
            uniformDensity.set((float) (props.atmosphere().density() * densityFade));
        }

        Uniform sphericalUniform = shader.getUniform("Spherical");
        if (sphericalUniform != null) {
            sphericalUniform.set(spherical ? 1.0f : 0.0f);
        }

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        matrix.translate(cameraPos0.negate(new Vector3f()));

        matrix.rotate(new Quaternionf(localRotation));

        Vector3f testPos = new Vector3f(cameraPos0).rotate(new Quaternionf(localRotation).conjugate());

        float correctionDist = relativeAtmosphereSize;

        float outOverexposureCancel = 1.0f;

        float atmosphereRadius = halfSize * relativeAtmosphereSize;
        boolean cameraInsideAtmosphere = spherical
                ? (testPos.x * testPos.x + testPos.y * testPos.y + testPos.z * testPos.z)
                    < atmosphereRadius * atmosphereRadius
                : Math.abs(testPos.x) < atmosphereRadius
                    && Math.abs(testPos.y) < atmosphereRadius
                    && Math.abs(testPos.z) < atmosphereRadius;

        if (cameraInsideAtmosphere) {
            correctionDist = 1.001f;
        } else {
            outOverexposureCancel = 0.000000001f;
        }

        if (spherical) {
            addSphereAtmosphere(matrix, buffer, halfSize, correctionDist);
        } else {
            addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
            addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
            addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
            addCubeFaceAtmosphere(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
            addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
            addCubeFaceAtmosphere(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
        }
    }

    private static void addSphereAtmosphere(Matrix4f matrix, VertexConsumer buffer,
                                            float halfSize, float geometryScale) {
        float radius = halfSize * geometryScale;
        for (shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face face
                : shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face.values()) {
            for (int row = 0; row < SPHERE_MESH_RESOLUTION; row++) {
                double t0 = row / (double) SPHERE_MESH_RESOLUTION;
                double t1 = (row + 1) / (double) SPHERE_MESH_RESOLUTION;
                for (int column = 0; column < SPHERE_MESH_RESOLUTION; column++) {
                    double s0 = column / (double) SPHERE_MESH_RESOLUTION;
                    double s1 = (column + 1) / (double) SPHERE_MESH_RESOLUTION;
                    addSphereAtmosphereVertex(matrix, buffer, face, s0, t1, radius);
                    addSphereAtmosphereVertex(matrix, buffer, face, s1, t1, radius);
                    addSphereAtmosphereVertex(matrix, buffer, face, s1, t0, radius);
                    addSphereAtmosphereVertex(matrix, buffer, face, s0, t0, radius);
                }
            }
        }
    }

    private static void addSphereAtmosphereVertex(Matrix4f matrix, VertexConsumer buffer,
            shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face face,
            double s, double t, float radius) {
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
        Vector3d direction = new Vector3d(
                cube.x * Math.sqrt(Math.max(0.0, 1.0 - y2 * 0.5 - z2 * 0.5 + y2 * z2 / 3.0)),
                cube.y * Math.sqrt(Math.max(0.0, 1.0 - z2 * 0.5 - x2 * 0.5 + z2 * x2 / 3.0)),
                cube.z * Math.sqrt(Math.max(0.0, 1.0 - x2 * 0.5 - y2 * 0.5 + x2 * y2 / 3.0))
        ).normalize();
        int r = Mth.clamp((int) ((direction.x * 0.5 + 0.5) * 255.0), 0, 255);
        int g = Mth.clamp((int) ((direction.y * 0.5 + 0.5) * 255.0), 0, 255);
        int b = Mth.clamp((int) ((direction.z * 0.5 + 0.5) * 255.0), 0, 255);
        buffer.addVertex(matrix, (float) direction.x * radius, (float) direction.y * radius,
                (float) direction.z * radius).setColor(r, g, b, 255);
    }

    private static void addCubeFaceAtmosphere(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                              float x3, float y3, float z3, float x4, float y4, float z4,float halfSize,float zFightingCorrection,float atmosphereThickness,float outFact) {

        float size = 2 * halfSize;
        float sclFct;

        sclFct = zFightingCorrection;
        buffer.addVertex(matrix, x1 * sclFct, y1 * sclFct, z1 * sclFct).setColor((int)(255 * (x1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z1 * sclFct / atmosphereThickness + halfSize) / size), 255);
        buffer.addVertex(matrix, x2 * sclFct, y2 * sclFct, z2 * sclFct).setColor((int)(255 * (x2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z2 * sclFct / atmosphereThickness + halfSize) / size), 255);
        buffer.addVertex(matrix, x3 * sclFct, y3 * sclFct, z3 * sclFct).setColor((int)(255 * (x3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z3 * sclFct / atmosphereThickness + halfSize) / size), 255);
        buffer.addVertex(matrix, x4 * sclFct, y4 * sclFct, z4 * sclFct).setColor((int)(255 * (x4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z4 * sclFct / atmosphereThickness + halfSize) / size), 255);

        sclFct = -atmosphereThickness * outFact;
        buffer.addVertex(matrix, x1 * sclFct, y1 * sclFct, z1 * sclFct).setColor((int)(255 * (x1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z1 * sclFct / atmosphereThickness + halfSize) / size), 255);
        buffer.addVertex(matrix, x2 * sclFct, y2 * sclFct, z2 * sclFct).setColor((int)(255 * (x2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z2 * sclFct / atmosphereThickness + halfSize) / size), 255);
        buffer.addVertex(matrix, x3 * sclFct, y3 * sclFct, z3 * sclFct).setColor((int)(255 * (x3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z3 * sclFct / atmosphereThickness + halfSize) / size), 255);
        buffer.addVertex(matrix, x4 * sclFct, y4 * sclFct, z4 * sclFct).setColor((int)(255 * (x4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z4 * sclFct / atmosphereThickness + halfSize) / size), 255);
    }
}
