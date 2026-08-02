package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import shipwrights.genesis.hyperspace.HyperspaceTunnelRenderer;

/**
 * Sky for the hyperspace dimension ({@code genesis:subspace}): a procedural
 * tunnel of swirling blue/purple clouds and light rays streaming from a bright
 * core, rendered by the {@code genesis:hyperspace} core shader on a skybox cube.
 * Anything standing on a ship in this dimension is visibly "in hyperspace".
 */
public class WormholeDimensionEffects extends DimensionSpecialEffects {
    private static final float SKYBOX_RADIUS = 80.0f;

    @Nullable
    private VertexBuffer skyboxBuffer;

    public WormholeDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        // Deep indigo, matching the dark end of the tunnel palette.
        return new Vec3(0.03, 0.03, 0.12);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    public float @Nullable [] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(modelViewMatrix);
        FogRenderer.setupNoFog();

        ShaderInstance shader = ShaderRegistry.HYPERSPACE_SHADER.getShaderInstance();
        Vector3f axis = HyperspaceTunnelRenderer.tunnelAxis();
        shader.safeGetUniform("TunnelAxis").set(axis.x(), axis.y(), axis.z());

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        VertexBuffer buffer = getSkyboxBuffer();
        buffer.bind();
        buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
        VertexBuffer.unbind();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);

        setupFog.run();

        return true;
    }

    private VertexBuffer getSkyboxBuffer() {
        if (skyboxBuffer == null) {
            skyboxBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            skyboxBuffer.bind();
            skyboxBuffer.upload(buildSkyboxCube(SKYBOX_RADIUS));
            VertexBuffer.unbind();
        }
        return skyboxBuffer;
    }

    /**
     * A camera-centered cube; the fragment shader only uses the interpolated
     * vertex position as a view direction, so the exact size is irrelevant as
     * long as it sits inside the far plane.
     */
    private static MeshData buildSkyboxCube(float r) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        // -Z
        builder.addVertex(-r, -r, -r).addVertex(-r, r, -r).addVertex(r, r, -r).addVertex(r, -r, -r);
        // +Z
        builder.addVertex(-r, -r, r).addVertex(r, -r, r).addVertex(r, r, r).addVertex(-r, r, r);
        // -Y
        builder.addVertex(-r, -r, -r).addVertex(r, -r, -r).addVertex(r, -r, r).addVertex(-r, -r, r);
        // +Y
        builder.addVertex(-r, r, -r).addVertex(-r, r, r).addVertex(r, r, r).addVertex(r, r, -r);
        // -X
        builder.addVertex(-r, -r, -r).addVertex(-r, -r, r).addVertex(-r, r, r).addVertex(-r, r, -r);
        // +X
        builder.addVertex(r, -r, -r).addVertex(r, r, -r).addVertex(r, r, r).addVertex(r, -r, r);

        return builder.buildOrThrow();
    }
}
