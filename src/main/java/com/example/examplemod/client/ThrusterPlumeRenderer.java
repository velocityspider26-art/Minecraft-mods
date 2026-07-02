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
 * <p>The plume is drawn as nested, additively-blended tubes. Rather than a straight line, the tube's
 * centreline is traced through the path the nozzle actually moved through the world over the last
 * few ticks: each point down the plume is where an exhaust element emitted that long ago would be
 * now. So when the craft translates the plume trails behind it, and when it turns the tail curves -
 * like a real exhaust that keeps the momentum and direction it had when it left the bell.</p>
 *
 * <p>On top of that, the surface radius is perturbed per-vertex by turbulence that grows downstream,
 * so the flame frays and billows instead of being a smooth cone, and engines with shock structure get
 * animated mach diamonds near the throat. Colour, width, length and shock come from a {@link Profile}
 * chosen by the thruster's {@link PlumeType}. Everything uses {@link RenderType#lightning()} - an
 * untextured, additively-blended, cull-off pass ideal for a glowing flame.</p>
 */
public class ThrusterPlumeRenderer implements BlockEntityRenderer<ThrusterBlockEntity> {

    private static final int RADIAL = 20;      // segments around each tube
    private static final int SEGMENTS = 64;    // segments along each tube
    private static final float AGE_TICKS = 7f; // how many ticks of nozzle history the plume spans
    private static final float ANG_STEP = (float) (Math.PI * 2.0 / RADIAL);

    private static final float[] COS = new float[RADIAL + 1];
    private static final float[] SIN = new float[RADIAL + 1];
    static {
        for (int j = 0; j <= RADIAL; j++) {
            float a = j * ANG_STEP;
            COS[j] = Mth.cos(a);
            SIN[j] = Mth.sin(a);
        }
    }

    // Scratch buffers reused per render call (BER is a singleton, render is single-threaded).
    private final float[] cx = new float[SEGMENTS + 1];
    private final float[] cy = new float[SEGMENTS + 1];
    private final float[] cz = new float[SEGMENTS + 1];
    private final double[] samplePos = new double[3];
    private final float[] sampleDir = new float[3];
    private final float[] c0 = new float[4];
    private final float[] c1 = new float[4];

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

        Vector3f start = new Vector3f(0.5f, 0.5f, 0.5f).add(dir.x * 0.72f, dir.y * 0.72f, dir.z * 0.72f);

        float length = (2.0f + throttle * 9.0f) * profile.lengthMul;
        float maxRadius = (0.50f + throttle * 0.28f) * profile.widthMul;
        float flicker = 0.88f + 0.12f * Mth.sin(time * 1.9f) * Mth.cos(time * 0.83f);

        Matrix4f pose = ms.last().pose();

        buildCenterline(be, pose, dir, start, length, time);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        for (float[] layer : profile.layers) {
            renderLayer(vc, pose, u, w, maxRadius, time, flicker, layer, profile.diamondAmp, profile.diamondCount);
        }
    }

    /**
     * Fills {@link #cx}/{@link #cy}/{@link #cz} with the plume centreline in block-local space, traced
     * through the nozzle's recent world path so the plume trails and curves with the craft's motion.
     */
    private void buildCenterline(ThrusterBlockEntity be, Matrix4f pose, Vector3f dir, Vector3f start, float length, float time) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        // Current nozzle world position and facing (from the render pose, so VS ships / contraptions work).
        Vector3f nozzleRel = pose.transformPosition(new Vector3f(start));
        double nwx = cam.x + nozzleRel.x;
        double nwy = cam.y + nozzleRel.y;
        double nwz = cam.z + nozzleRel.z;

        Matrix3f rot = new Matrix3f(pose);
        Vector3f dirWorld = rot.transform(new Vector3f(dir));
        if (dirWorld.lengthSquared() > 1.0e-6f) {
            dirWorld.normalize();
        } else {
            dirWorld.set(dir);
        }
        be.pushNozzleSample(time, nwx, nwy, nwz, dirWorld.x, dirWorld.y, dirWorld.z);

        Matrix3f invRot = new Matrix3f(pose);
        boolean invertible = true;
        try {
            invRot.invert();
        } catch (RuntimeException e) {
            invertible = false;
        }

        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            float target = time - t * AGE_TICKS;
            if (invertible && be.sampleNozzle(target, samplePos, sampleDir)) {
                // Where an element emitted at 'target' would be now: nozzle pos then + jet travel.
                double wx = samplePos[0] + sampleDir[0] * (length * t);
                double wy = samplePos[1] + sampleDir[1] * (length * t);
                double wz = samplePos[2] + sampleDir[2] * (length * t);
                Vector3f local = invRot.transform(new Vector3f((float) (wx - nwx), (float) (wy - nwy), (float) (wz - nwz)));
                cx[i] = start.x + local.x;
                cy[i] = start.y + local.y;
                cz[i] = start.z + local.z;
            } else {
                cx[i] = start.x + dir.x * (t * length);
                cy[i] = start.y + dir.y * (t * length);
                cz[i] = start.z + dir.z * (t * length);
            }
        }
    }

    /** layer = {nearR,nearG,nearB, farR,farG,farB, baseAlpha, alphaFalloff, radiusScale, lengthScale, turbAmp} */
    private void renderLayer(VertexConsumer vc, Matrix4f pose, Vector3f u, Vector3f w, float maxRadius,
                             float time, float flicker, float[] layer, float diamondAmp, float diamondCount) {
        float layerRadius = maxRadius * layer[8];
        float turbAmp = layer[10];
        float phase = time * 2.2f;

        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            colorAt(t0, phase, layer, diamondAmp, diamondCount, flicker, c0);
            colorAt(t1, phase, layer, diamondAmp, diamondCount, flicker, c1);

            for (int j = 0; j < RADIAL; j++) {
                float cA = COS[j], sA = SIN[j];
                float cB = COS[j + 1], sB = SIN[j + 1];
                float r0a = radiusAt(t0, j, layerRadius, turbAmp, time, phase, diamondAmp, diamondCount);
                float r0b = radiusAt(t0, j + 1, layerRadius, turbAmp, time, phase, diamondAmp, diamondCount);
                float r1a = radiusAt(t1, j, layerRadius, turbAmp, time, phase, diamondAmp, diamondCount);
                float r1b = radiusAt(t1, j + 1, layerRadius, turbAmp, time, phase, diamondAmp, diamondCount);

                putVertex(vc, pose, cx[i], cy[i], cz[i], u, w, cA, sA, r0a, c0);
                putVertex(vc, pose, cx[i], cy[i], cz[i], u, w, cB, sB, r0b, c0);
                putVertex(vc, pose, cx[i + 1], cy[i + 1], cz[i + 1], u, w, cB, sB, r1b, c1);
                putVertex(vc, pose, cx[i + 1], cy[i + 1], cz[i + 1], u, w, cA, sA, r1a, c1);
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

    /** Rounded bulge past the nozzle tapering toward the tip (teardrop), kept wide enough to fray. */
    private static float radiusProfile(float t) {
        final float head = 0.14f;
        if (t < head) {
            return (float) Math.sqrt(t / head);
        }
        float k = (t - head) / (1f - head);
        return (float) Math.pow(1f - k, 0.55);
    }

    /** Per-vertex radius including turbulence that grows downstream and mach-diamond modulation. */
    private static float radiusAt(float t, int jIndex, float baseRadius, float turbAmp,
                                  float time, float phase, float diamondAmp, float diamondCount) {
        float ang = (jIndex % RADIAL) * ANG_STEP;
        float base = baseRadius * radiusProfile(t);
        float fray = 1f + turbAmp * (0.25f + t)
                * (0.6f * Mth.sin(ang * 3f + t * 18f - time * 7f) + 0.4f * Mth.sin(ang * 5f - t * 11f + time * 5f));
        float dia = diamond(t, phase, diamondAmp, diamondCount);
        return Math.max(0f, base * fray * dia);
    }

    private static float diamond(float t, float phase, float amp, float count) {
        if (amp <= 0f) {
            return 1f;
        }
        return 1f + amp * (float) Math.exp(-3.0f * t) * Mth.cos(t * count * ((float) Math.PI * 2f) - phase);
    }

    private static void colorAt(float t, float phase, float[] layer, float diamondAmp, float diamondCount,
                                float flicker, float[] out) {
        float r = Mth.lerp(t, layer[0], layer[3]);
        float g = Mth.lerp(t, layer[1], layer[4]);
        float b = Mth.lerp(t, layer[2], layer[5]);

        if (diamondAmp > 0f) {
            float shock = Math.max(0f, Mth.cos(t * diamondCount * ((float) Math.PI * 2f) - phase)) * (float) Math.exp(-4.0f * t);
            float boost = shock * diamondAmp * 2.2f;
            r = Math.min(1f, r + boost * 0.7f);
            g = Math.min(1f, g + boost * 0.8f);
            b = Math.min(1f, b + boost);
        }

        float alpha = layer[6] * (float) Math.pow(1f - t, layer[7]) * flicker;
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
                        {1.00f, 0.45f, 0.12f,  0.70f, 0.12f, 0.03f, 0.30f, 1.25f, 1.05f, 1.00f, 0.10f},
                        {1.00f, 0.75f, 0.35f,  1.00f, 0.35f, 0.10f, 0.42f, 1.55f, 0.62f, 0.94f, 0.07f},
                        {1.00f, 0.95f, 0.80f,  1.00f, 0.70f, 0.40f, 0.55f, 1.90f, 0.30f, 0.80f, 0.04f},
                }, 1.00f, 1.00f, 0.0f, 0.0f);
                // Methalox (Raptor): blue-white core with strong mach diamonds, long and slender.
                case METHALOX -> new Profile(new float[][]{
                        {0.55f, 0.45f, 1.00f,  0.30f, 0.18f, 0.65f, 0.28f, 1.30f, 1.05f, 1.00f, 0.08f},
                        {0.80f, 0.88f, 1.00f,  0.45f, 0.55f, 1.00f, 0.40f, 1.55f, 0.60f, 0.95f, 0.05f},
                        {1.00f, 1.00f, 1.00f,  0.75f, 0.85f, 1.00f, 0.62f, 1.95f, 0.30f, 0.82f, 0.04f},
                }, 1.18f, 0.84f, 0.24f, 6.0f);
                // Hydrolox (RS-25): faint, nearly transparent pale blue, long and thin, gentle diamonds.
                case HYDROLOX -> new Profile(new float[][]{
                        {0.55f, 0.66f, 1.00f,  0.30f, 0.42f, 0.85f, 0.10f, 1.60f, 1.05f, 1.00f, 0.05f},
                        {0.72f, 0.85f, 1.00f,  0.40f, 0.62f, 1.00f, 0.15f, 1.80f, 0.55f, 0.96f, 0.04f},
                        {0.88f, 0.96f, 1.00f,  0.62f, 0.82f, 1.00f, 0.22f, 2.00f, 0.26f, 0.84f, 0.03f},
                }, 1.12f, 0.70f, 0.10f, 7.0f);
                // Hypergolic (Draco/Proton): translucent reddish orange, rougher, shorter, thinner.
                case HYPERGOLIC -> new Profile(new float[][]{
                        {0.90f, 0.35f, 0.15f,  0.55f, 0.12f, 0.14f, 0.22f, 1.40f, 1.05f, 1.00f, 0.12f},
                        {1.00f, 0.55f, 0.30f,  0.80f, 0.28f, 0.20f, 0.30f, 1.60f, 0.58f, 0.94f, 0.09f},
                        {1.00f, 0.80f, 0.62f,  0.90f, 0.50f, 0.35f, 0.40f, 1.85f, 0.30f, 0.80f, 0.06f},
                }, 0.90f, 0.82f, 0.0f, 0.0f);
                // Solid (SRB/APCP): brilliant, wide, opaque white-orange, heavy fray.
                case SOLID -> new Profile(new float[][]{
                        {1.00f, 0.58f, 0.20f,  0.80f, 0.24f, 0.05f, 0.42f, 1.10f, 1.15f, 1.00f, 0.13f},
                        {1.00f, 0.84f, 0.48f,  1.00f, 0.45f, 0.14f, 0.54f, 1.30f, 0.70f, 0.95f, 0.09f},
                        {1.00f, 1.00f, 0.90f,  1.00f, 0.85f, 0.55f, 0.72f, 1.55f, 0.40f, 0.82f, 0.05f},
                }, 1.05f, 1.22f, 0.0f, 0.0f);
            };
        }
    }
}
