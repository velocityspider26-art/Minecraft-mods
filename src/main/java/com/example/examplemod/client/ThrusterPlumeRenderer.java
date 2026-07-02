package com.example.examplemod.client;

import com.example.examplemod.content.thruster.PlumeType;
import com.example.examplemod.content.thruster.ThrusterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders a rocket exhaust plume as a real 3D mesh instead of particles.
 *
 * <p>The plume is built from nested, additively-blended tubes extruded along the thruster's facing
 * direction. Because the layers add together, overlap reads brighter and the outer flanks keep the
 * layer's edge colour, giving a smooth volumetric flame. The profile is a rounded teardrop (a bulge
 * just past the nozzle tapering to a point) with subtle animated turbulence, and - for engines that
 * have them - animated mach-diamond shock nodes near the throat.</p>
 *
 * <p>The exact colours, width, length and shock structure come from a {@link Profile} chosen by the
 * thruster's {@link PlumeType}, so each of the five real-world-inspired variants looks distinct.</p>
 *
 * <p>All geometry uses {@link RenderType#lightning()} - an untextured, additively-blended, cull-off
 * POSITION_COLOR pass - which is ideal for a glowing flame.</p>
 */
public class ThrusterPlumeRenderer implements BlockEntityRenderer<ThrusterBlockEntity> {

    private static final int RADIAL = 20;      // segments around each tube
    private static final int SEGMENTS = 64;    // segments along each tube

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
        return new AABB(blockEntity.getBlockPos()).inflate(16.0);
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

        Profile profile = Profile.forType(be.getPlumeType());
        float time = be.getLevel().getGameTime() + partialTick;

        Direction facing = be.getFacing();
        Vector3f dir = new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ());

        Vector3f helper = Math.abs(dir.y) < 0.99f ? new Vector3f(0f, 1f, 0f) : new Vector3f(1f, 0f, 0f);
        Vector3f u = new Vector3f(dir).cross(helper).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        // Start at the nozzle tip (the ripped model's nozzle sticks ~0.25 blocks past the face).
        Vector3f start = new Vector3f(0.5f, 0.5f, 0.5f).add(dir.x * 0.72f, dir.y * 0.72f, dir.z * 0.72f);

        float length = (2.0f + throttle * 9.0f) * profile.lengthMul;
        float maxRadius = (0.50f + throttle * 0.28f) * profile.widthMul;
        float flicker = 0.88f + 0.12f * Mth.sin(time * 1.9f) * Mth.cos(time * 0.83f);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        Matrix4f pose = ms.last().pose();

        Vector3f drag = computeDrag(be, pose, time, length);

        for (float[] layer : profile.layers) {
            renderLayer(vc, pose, start, dir, u, w, length, maxRadius, throttle, time, flicker, layer,
                    profile.diamondAmp, profile.diamondCount, drag);
        }
    }

    /**
     * Measures the thruster's true world-space velocity from the render pose (so it works on
     * Valkyrien Skies ships / Create contraptions, whose transform is baked into the pose) and turns
     * it into a block-local "drag" vector - the direction the plume trails as the craft moves.
     */
    private static Vector3f computeDrag(ThrusterBlockEntity be, Matrix4f pose, float time, float length) {
        final float DRAG_FACTOR = 6.0f;

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f originRel = pose.transformPosition(new Vector3f(0f, 0f, 0f));
        be.updateRenderMotion(cam.x + originRel.x, cam.y + originRel.y, cam.z + originRel.z, time);

        Vector3f worldVel = be.getSmoothedWorldVelocity();
        if (worldVel.lengthSquared() < 1.0e-6f) {
            return new Vector3f(0f, 0f, 0f);
        }
        // World velocity -> block-local space (inverse of the pose's rotation/scale).
        Matrix3f invRot = new Matrix3f(pose);
        try {
            invRot.invert();
        } catch (RuntimeException e) {
            return new Vector3f(0f, 0f, 0f);
        }
        Vector3f localVel = invRot.transform(new Vector3f(worldVel));
        // The plume trails opposite the motion.
        Vector3f drag = localVel.mul(-DRAG_FACTOR);
        float max = length * 0.85f;
        if (drag.length() > max) {
            drag.normalize().mul(max);
        }
        return drag;
    }

    /** layer = {nearR,nearG,nearB, farR,farG,farB, baseAlpha, alphaFalloff, radiusScale, lengthScale, turbAmp} */
    private static void renderLayer(VertexConsumer vc, Matrix4f pose, Vector3f start, Vector3f dir,
                                    Vector3f u, Vector3f w, float baseLength, float maxRadius,
                                    float throttle, float time, float flicker, float[] layer,
                                    float diamondAmp, float diamondCount, Vector3f drag) {
        float layerRadius = maxRadius * layer[8];
        float length = baseLength * layer[9];
        float turbAmp = layer[10];
        float phase = time * 2.2f;
        float[] c0 = new float[4];
        float[] c1 = new float[4];

        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;

            float r0 = layerRadius * radiusProfile(t0) * turbulence(t0, time, turbAmp) * diamond(t0, phase, diamondAmp, diamondCount);
            float r1 = layerRadius * radiusProfile(t1) * turbulence(t1, time, turbAmp) * diamond(t1, phase, diamondAmp, diamondCount);

            // Motion drag grows with distance down the plume, so the flame trails and bends.
            float d0 = (float) Math.pow(t0, 1.3);
            float d1 = (float) Math.pow(t1, 1.3);
            float ax0 = start.x + dir.x * (t0 * length) + drag.x * d0;
            float ay0 = start.y + dir.y * (t0 * length) + drag.y * d0;
            float az0 = start.z + dir.z * (t0 * length) + drag.z * d0;
            float ax1 = start.x + dir.x * (t1 * length) + drag.x * d1;
            float ay1 = start.y + dir.y * (t1 * length) + drag.y * d1;
            float az1 = start.z + dir.z * (t1 * length) + drag.z * d1;

            colorAt(t0, phase, layer, diamondAmp, diamondCount, throttle, flicker, c0);
            colorAt(t1, phase, layer, diamondAmp, diamondCount, throttle, flicker, c1);

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

    /** Rounded bulge past the nozzle tapering to a sharp point (teardrop). */
    private static float radiusProfile(float t) {
        final float head = 0.16f;
        if (t < head) {
            return (float) Math.sqrt(t / head);
        }
        float k = (t - head) / (1f - head);
        return (float) Math.pow(1f - k, 0.7);
    }

    private static float turbulence(float t, float time, float amp) {
        if (amp <= 0f) {
            return 1f;
        }
        return 1f + amp * Mth.sin(t * 16f - time * 6f) + amp * 0.5f * Mth.sin(t * 37f - time * 9f);
    }

    /** Mach-diamond radial modulation, strongest near the throat and decaying down the plume. */
    private static float diamond(float t, float phase, float amp, float count) {
        if (amp <= 0f) {
            return 1f;
        }
        return 1f + amp * (float) Math.exp(-3.0f * t) * Mth.cos(t * count * ((float) Math.PI * 2f) - phase);
    }

    private static void colorAt(float t, float phase, float[] layer, float diamondAmp, float diamondCount,
                                float throttle, float flicker, float[] out) {
        float r = Mth.lerp(t, layer[0], layer[3]);
        float g = Mth.lerp(t, layer[1], layer[4]);
        float b = Mth.lerp(t, layer[2], layer[5]);

        // Bright bluish-white shock nodes at mach-diamond peaks near the throat.
        if (diamondAmp > 0f) {
            float shock = Math.max(0f, Mth.cos(t * diamondCount * ((float) Math.PI * 2f) - phase)) * (float) Math.exp(-4.0f * t);
            float boost = shock * diamondAmp * 2.2f;
            r = Math.min(1f, r + boost * 0.7f);
            g = Math.min(1f, g + boost * 0.8f);
            b = Math.min(1f, b + boost);
        }

        float alpha = layer[6] * (float) Math.pow(1f - t, layer[7]) * throttle * flicker;
        out[0] = r;
        out[1] = g;
        out[2] = b;
        out[3] = Mth.clamp(alpha, 0f, 1f);
    }

    /** Per-plume-type appearance data. */
    private static final class Profile {
        final float[][] layers;
        final float lengthMul;
        final float widthMul;
        final float diamondAmp;
        final float diamondCount;

        private Profile(float[][] layers, float lengthMul, float widthMul, float diamondAmp, float diamondCount) {
            this.layers = layers;
            this.lengthMul = lengthMul;
            this.widthMul = widthMul;
            this.diamondAmp = diamondAmp;
            this.diamondCount = diamondCount;
        }

        static Profile forType(PlumeType type) {
            return switch (type) {
                // Kerolox (Merlin/F-1): warm, sooty orange, no shock diamonds.
                case KEROLOX -> new Profile(new float[][]{
                        {1.00f, 0.45f, 0.12f,  0.70f, 0.12f, 0.03f, 0.30f, 1.25f, 1.00f, 1.00f, 0.06f},
                        {1.00f, 0.75f, 0.35f,  1.00f, 0.35f, 0.10f, 0.42f, 1.55f, 0.62f, 0.94f, 0.05f},
                        {1.00f, 0.95f, 0.80f,  1.00f, 0.70f, 0.40f, 0.55f, 1.90f, 0.30f, 0.80f, 0.03f},
                }, 1.00f, 1.00f, 0.0f, 0.0f);
                // Methalox (Raptor): blue-white core with strong mach diamonds, long and slender.
                case METHALOX -> new Profile(new float[][]{
                        {0.55f, 0.45f, 1.00f,  0.30f, 0.18f, 0.65f, 0.28f, 1.30f, 1.00f, 1.00f, 0.05f},
                        {0.80f, 0.88f, 1.00f,  0.45f, 0.55f, 1.00f, 0.40f, 1.55f, 0.60f, 0.95f, 0.04f},
                        {1.00f, 1.00f, 1.00f,  0.75f, 0.85f, 1.00f, 0.62f, 1.95f, 0.30f, 0.82f, 0.03f},
                }, 1.18f, 0.84f, 0.24f, 6.0f);
                // Hydrolox (RS-25): faint, nearly transparent pale blue, long and thin, gentle diamonds.
                case HYDROLOX -> new Profile(new float[][]{
                        {0.55f, 0.66f, 1.00f,  0.30f, 0.42f, 0.85f, 0.10f, 1.60f, 1.00f, 1.00f, 0.03f},
                        {0.72f, 0.85f, 1.00f,  0.40f, 0.62f, 1.00f, 0.15f, 1.80f, 0.55f, 0.96f, 0.03f},
                        {0.88f, 0.96f, 1.00f,  0.62f, 0.82f, 1.00f, 0.22f, 2.00f, 0.26f, 0.84f, 0.02f},
                }, 1.12f, 0.70f, 0.10f, 7.0f);
                // Hypergolic (Draco/Proton): translucent reddish orange, rougher, shorter, thinner.
                case HYPERGOLIC -> new Profile(new float[][]{
                        {0.90f, 0.35f, 0.15f,  0.55f, 0.12f, 0.14f, 0.22f, 1.40f, 1.00f, 1.00f, 0.08f},
                        {1.00f, 0.55f, 0.30f,  0.80f, 0.28f, 0.20f, 0.30f, 1.60f, 0.58f, 0.94f, 0.07f},
                        {1.00f, 0.80f, 0.62f,  0.90f, 0.50f, 0.35f, 0.40f, 1.85f, 0.30f, 0.80f, 0.05f},
                }, 0.90f, 0.82f, 0.0f, 0.0f);
                // Solid (SRB/APCP): brilliant, wide, opaque white-orange.
                case SOLID -> new Profile(new float[][]{
                        {1.00f, 0.58f, 0.20f,  0.80f, 0.24f, 0.05f, 0.42f, 1.10f, 1.10f, 1.00f, 0.09f},
                        {1.00f, 0.84f, 0.48f,  1.00f, 0.45f, 0.14f, 0.54f, 1.30f, 0.70f, 0.95f, 0.07f},
                        {1.00f, 1.00f, 0.90f,  1.00f, 0.85f, 0.55f, 0.72f, 1.55f, 0.40f, 0.82f, 0.04f},
                }, 1.05f, 1.22f, 0.0f, 0.0f);
            };
        }
    }
}
