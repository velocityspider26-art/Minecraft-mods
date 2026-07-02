package com.example.examplemod.client;

import com.example.examplemod.content.thruster.ThrusterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders a rocket exhaust plume as a real 3D mesh instead of particles.
 *
 * <p>The plume is built from three nested, additively-blended tubes extruded along the thruster's
 * facing direction - a wide translucent red rim, an orange mid layer, and a bright white-hot core.
 * Because the layers add together, the centre reads white, the flanks orange and the outer edge
 * red, giving the smooth white/orange lens look of a real rocket flame. The profile is a rounded
 * teardrop: a bulge just past the nozzle tapering to a sharp point, with subtle animated turbulence.</p>
 *
 * <p>All geometry uses {@link RenderType#lightning()} - an untextured, additively-blended,
 * cull-off POSITION_COLOR pass - which is ideal for a glowing volumetric flame.</p>
 */
public class ThrusterPlumeRenderer implements BlockEntityRenderer<ThrusterBlockEntity> {

    private static final int RADIAL = 20;      // segments around each tube
    private static final int SEGMENTS = 64;    // segments along each tube

    // Pre-computed unit circle so we do not call sin/cos per vertex.
    private static final float[] COS = new float[RADIAL + 1];
    private static final float[] SIN = new float[RADIAL + 1];
    static {
        for (int j = 0; j <= RADIAL; j++) {
            float a = (float) (j / (double) RADIAL * Math.PI * 2.0);
            COS[j] = Mth.cos(a);
            SIN[j] = Mth.sin(a);
        }
    }

    public ThrusterPlumeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(ThrusterBlockEntity blockEntity) {
        // The plume can extend many blocks past the nozzle; inflate so it is not culled.
        return new AABB(blockEntity.getBlockPos()).inflate(14.0);
    }

    @Override
    public boolean shouldRenderOffScreen(ThrusterBlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(ThrusterBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        float throttle = be.getThrottle(partialTick);
        if (throttle < 0.03f) {
            return;
        }

        float time = be.getLevel().getGameTime() + partialTick;

        Direction facing = be.getFacing();
        Vector3f dir = new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ());

        // Orthonormal basis around the plume axis.
        Vector3f helper = Math.abs(dir.y) < 0.99f ? new Vector3f(0f, 1f, 0f) : new Vector3f(1f, 0f, 0f);
        Vector3f u = new Vector3f(dir).cross(helper).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        // Start at the nozzle tip (the ripped model's nozzle sticks ~0.25 blocks past the face).
        Vector3f start = new Vector3f(0.5f, 0.5f, 0.5f).add(dir.x * 0.72f, dir.y * 0.72f, dir.z * 0.72f);

        float length = 2.0f + throttle * 9.0f;
        float maxRadius = 0.50f + throttle * 0.28f;

        // Overall flicker so the flame breathes.
        float flicker = 0.88f + 0.12f * Mth.sin(time * 1.9f) * Mth.cos(time * 0.83f);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        Matrix4f pose = ms.last().pose();

        // Outer translucent red rim.
        renderLayer(vc, pose, start, dir, u, w, length, maxRadius, 1.00f, throttle, time, flicker,
                1.00f, 0.50f, 0.14f, 0.92f, 0.14f, 0.04f, 0.30f, 1.25f, 0.06f);
        // Orange mid layer.
        renderLayer(vc, pose, start, dir, u, w, length * 0.94f, maxRadius, 0.62f, throttle, time, flicker,
                1.00f, 0.82f, 0.42f, 1.00f, 0.42f, 0.12f, 0.42f, 1.55f, 0.045f);
        // Bright white-hot core.
        renderLayer(vc, pose, start, dir, u, w, length * 0.80f, maxRadius, 0.30f, throttle, time, flicker,
                1.00f, 0.98f, 0.92f, 1.00f, 0.80f, 0.48f, 0.60f, 1.90f, 0.03f);
    }

    /**
     * Renders one concentric tube layer of the plume.
     *
     * @param radiusScale   fraction of {@code maxRadius} for this layer
     * @param nr,ng,nb      colour near the nozzle
     * @param fr,fg,fb      colour near the tip
     * @param baseAlpha     alpha at the nozzle
     * @param alphaFalloff  exponent controlling how fast alpha fades toward the tip
     * @param turbAmp       amplitude of the animated radial turbulence
     */
    private static void renderLayer(VertexConsumer vc, Matrix4f pose, Vector3f start, Vector3f dir,
                                    Vector3f u, Vector3f w, float length, float maxRadius, float radiusScale,
                                    float throttle, float time, float flicker,
                                    float nr, float ng, float nb, float fr, float fg, float fb,
                                    float baseAlpha, float alphaFalloff, float turbAmp) {
        float layerRadius = maxRadius * radiusScale;
        float[] c0 = new float[4];
        float[] c1 = new float[4];

        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;

            float r0 = layerRadius * radiusProfile(t0) * turbulence(t0, time, turbAmp);
            float r1 = layerRadius * radiusProfile(t1) * turbulence(t1, time, turbAmp);

            float ax0 = start.x + dir.x * (t0 * length);
            float ay0 = start.y + dir.y * (t0 * length);
            float az0 = start.z + dir.z * (t0 * length);
            float ax1 = start.x + dir.x * (t1 * length);
            float ay1 = start.y + dir.y * (t1 * length);
            float az1 = start.z + dir.z * (t1 * length);

            colorAt(t0, nr, ng, nb, fr, fg, fb, baseAlpha, alphaFalloff, throttle, flicker, c0);
            colorAt(t1, nr, ng, nb, fr, fg, fb, baseAlpha, alphaFalloff, throttle, flicker, c1);

            for (int j = 0; j < RADIAL; j++) {
                float cA = COS[j],     sA = SIN[j];
                float cB = COS[j + 1], sB = SIN[j + 1];

                putVertex(vc, pose, ax0, ay0, az0, u, w, cA, sA, r0, c0);
                putVertex(vc, pose, ax0, ay0, az0, u, w, cB, sB, r0, c0);
                putVertex(vc, pose, ax1, ay1, az1, u, w, cB, sB, r1, c1);
                putVertex(vc, pose, ax1, ay1, az1, u, w, cA, sA, r1, c1);
            }
        }
    }

    private static void putVertex(VertexConsumer vc, Matrix4f pose, float ax, float ay, float az,
                                  Vector3f u, Vector3f w, float cos, float sin, float radius, float[] color) {
        float x = ax + (u.x * cos + w.x * sin) * radius;
        float y = ay + (u.y * cos + w.y * sin) * radius;
        float z = az + (u.z * cos + w.z * sin) * radius;
        vc.addVertex(pose, x, y, z).setColor(color[0], color[1], color[2], color[3]);
    }

    /**
     * Normalised radius in [0,1] along the plume: a rounded bulge just past the nozzle that tapers
     * smoothly to a sharp point at the tip (teardrop shape).
     */
    private static float radiusProfile(float t) {
        final float head = 0.16f;
        if (t < head) {
            // Rounded leading edge.
            return (float) Math.sqrt(t / head);
        }
        // Long smooth taper to a point.
        float k = (t - head) / (1f - head);
        return (float) Math.pow(1f - k, 0.7);
    }

    /** Subtle animated wobble so the plume surface is not perfectly rigid. */
    private static float turbulence(float t, float time, float amp) {
        if (amp <= 0f) {
            return 1f;
        }
        return 1f + amp * Mth.sin(t * 16f - time * 6f) + amp * 0.5f * Mth.sin(t * 37f - time * 9f);
    }

    /**
     * Fills {@code out} with the RGBA at fraction {@code t} along the plume, lerping the layer's
     * near/far colours and fading alpha toward the tip.
     */
    private static void colorAt(float t, float nr, float ng, float nb, float fr, float fg, float fb,
                                float baseAlpha, float alphaFalloff, float throttle, float flicker, float[] out) {
        out[0] = Mth.lerp(t, nr, fr);
        out[1] = Mth.lerp(t, ng, fg);
        out[2] = Mth.lerp(t, nb, fb);
        float alpha = baseAlpha * (float) Math.pow(1f - t, alphaFalloff) * throttle * flicker;
        out[3] = Mth.clamp(alpha, 0f, 1f);
    }
}
