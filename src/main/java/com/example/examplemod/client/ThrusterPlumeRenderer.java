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
 * Renders a Starship-V3-Raptor-style exhaust plume as a real 3D mesh instead of particles.
 *
 * <p>The plume is a tube of stacked quad rings extruded along the thruster's facing direction.
 * Radius, colour and alpha vary along the length to reproduce the look of the reference photo:
 * a narrow white-hot throat, a bright magenta core with white "mach diamond" shock nodes near the
 * nozzle, fading into a translucent pink tail. Everything is drawn with {@link RenderType#lightning()}
 * which is an untextured, additively-blended, cull-off POSITION_COLOR pass - ideal for a glowing
 * volumetric flame.</p>
 */
public class ThrusterPlumeRenderer implements BlockEntityRenderer<ThrusterBlockEntity> {

    private static final int RADIAL = 18;      // segments around the tube
    private static final int SEGMENTS = 56;    // segments along the tube
    private static final float DIAMONDS = 6.0f; // number of mach-diamond nodes

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
        // The plume can extend several blocks past the nozzle; inflate so it is not culled.
        return new AABB(blockEntity.getBlockPos()).inflate(12.0);
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

        // Build an orthonormal basis around the plume axis.
        Vector3f helper = Math.abs(dir.y) < 0.99f ? new Vector3f(0f, 1f, 0f) : new Vector3f(1f, 0f, 0f);
        Vector3f u = new Vector3f(dir).cross(helper).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        // Nozzle exit = block centre pushed half a block along the facing direction.
        Vector3f start = new Vector3f(0.5f, 0.5f, 0.5f).add(dir.x * 0.5f, dir.y * 0.5f, dir.z * 0.5f);

        float length = 1.2f + throttle * 7.0f;
        float maxRadius = 0.30f + throttle * 0.22f;

        // Slight overall flicker so the flame feels alive.
        float flicker = 0.86f + 0.14f * Mth.sin(time * 1.7f) * Mth.cos(time * 0.73f);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        Matrix4f pose = ms.last().pose();

        // Two nested layers: a wide translucent magenta sheath and a bright inner core.
        renderLayer(vc, pose, start, dir, u, w, length, maxRadius, throttle, time, flicker, false);
        renderLayer(vc, pose, start, dir, u, w, length * 0.80f, maxRadius * 0.48f, throttle, time, flicker, true);
    }

    private static void renderLayer(VertexConsumer vc, Matrix4f pose, Vector3f start, Vector3f dir,
                                    Vector3f u, Vector3f w, float length, float maxRadius,
                                    float throttle, float time, float flicker, boolean core) {
        float phase = time * 2.2f;
        float[] c0 = new float[4];
        float[] c1 = new float[4];

        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;

            float r0 = maxRadius * radiusProfile(t0, phase);
            float r1 = maxRadius * radiusProfile(t1, phase);

            // Axis positions of the two rings.
            float ax0 = start.x + dir.x * (t0 * length);
            float ay0 = start.y + dir.y * (t0 * length);
            float az0 = start.z + dir.z * (t0 * length);
            float ax1 = start.x + dir.x * (t1 * length);
            float ay1 = start.y + dir.y * (t1 * length);
            float az1 = start.z + dir.z * (t1 * length);

            colorAt(t0, phase, throttle, flicker, core, c0);
            colorAt(t1, phase, throttle, flicker, core, c1);

            for (int j = 0; j < RADIAL; j++) {
                float cA = COS[j],     sA = SIN[j];
                float cB = COS[j + 1], sB = SIN[j + 1];

                // Ring 0, angle A / B
                putVertex(vc, pose, ax0, ay0, az0, u, w, cA, sA, r0, c0);
                putVertex(vc, pose, ax0, ay0, az0, u, w, cB, sB, r0, c0);
                // Ring 1, angle B / A
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

    /** Normalised radius in [0,1] along the plume, including animated mach-diamond bulges. */
    private static float radiusProfile(float t, float phase) {
        // Rapid flare out of the throat, then a long taper to the tip.
        float expand = Mth.clamp(t / 0.10f, 0f, 1f);
        float taper = 1f - Mth.clamp((t - 0.10f) / 0.90f, 0f, 1f);
        float env = expand * (0.30f + 0.70f * taper);
        // Mach diamonds: strongest near the nozzle, decaying down the plume.
        float diamond = 1f + 0.22f * (float) Math.exp(-3.5f * t) * Mth.cos(t * DIAMONDS * ((float) Math.PI * 2f) - phase);
        return Math.max(0f, env * diamond);
    }

    /**
     * Fills {@code out} with the RGBA for a point at fraction {@code t} along the plume.
     * Palette matches the reference: white-hot throat -> magenta core -> deep pink tail, with
     * bright bluish-white shock diamonds superimposed near the nozzle.
     */
    private static void colorAt(float t, float phase, float throttle, float flicker, boolean core, float[] out) {
        float r, g, b;
        if (t < 0.18f) {
            float k = t / 0.18f;
            if (core) {
                r = Mth.lerp(k, 1.00f, 1.00f);
                g = Mth.lerp(k, 0.97f, 0.60f);
                b = Mth.lerp(k, 1.00f, 0.85f);
            } else {
                r = Mth.lerp(k, 1.00f, 1.00f);
                g = Mth.lerp(k, 0.85f, 0.28f);
                b = Mth.lerp(k, 0.98f, 0.55f);
            }
        } else {
            float k = (t - 0.18f) / 0.82f;
            if (core) {
                r = Mth.lerp(k, 1.00f, 1.00f);
                g = Mth.lerp(k, 0.60f, 0.30f);
                b = Mth.lerp(k, 0.85f, 0.55f);
            } else {
                r = Mth.lerp(k, 1.00f, 0.65f);
                g = Mth.lerp(k, 0.28f, 0.06f);
                b = Mth.lerp(k, 0.55f, 0.22f);
            }
        }

        // Bluish-white mach-diamond shock nodes, concentrated near the throat.
        float shock = Math.max(0f, Mth.cos(t * DIAMONDS * ((float) Math.PI * 2f) - phase)) * (float) Math.exp(-4.0f * t);
        r = Math.min(1f, r + shock * 0.40f);
        g = Math.min(1f, g + shock * 0.50f);
        b = Math.min(1f, b + shock * 0.60f);

        float baseAlpha = core ? 0.70f : 0.50f;
        float falloff = core ? 1.2f : 1.6f;
        float alpha = baseAlpha * (float) Math.pow(1f - t, falloff) * throttle * flicker;

        out[0] = r;
        out[1] = g;
        out[2] = b;
        out[3] = Mth.clamp(alpha, 0f, 1f);
    }
}
