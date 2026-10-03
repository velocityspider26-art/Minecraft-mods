package dev.velocityspider.caw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.velocityspider.caw.blockentity.EngineNozzleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Procedural 3D jet plume. No particles and no flat billboard texture.
 *
 * The exhaust is several nested translucent tubes made from real vertices:
 * a narrow white-blue core, a blue shear layer, and a faint outer hot-gas envelope.
 * Radius, length, turbulence, and Mach-diamond compression all vary down the plume.
 */
public final class EngineNozzleRenderer implements BlockEntityRenderer<EngineNozzleBlockEntity> {
    private static final int RADIAL = 18;
    private static final int SEGMENTS = 52;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private static final float[] COS = new float[RADIAL + 1];
    private static final float[] SIN = new float[RADIAL + 1];

    static {
        for (int i = 0; i <= RADIAL; i++) {
            float angle = TWO_PI * i / RADIAL;
            COS[i] = Mth.cos(angle);
            SIN[i] = Mth.sin(angle);
        }
    }

    private final float[] color0 = new float[4];
    private final float[] color1 = new float[4];

    public EngineNozzleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public AABB getRenderBoundingBox(EngineNozzleBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(12.0);
    }

    @Override
    public boolean shouldRenderOffScreen(EngineNozzleBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

    @Override
    public void render(
            EngineNozzleBlockEntity nozzle,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (nozzle.getLevel() == null || !nozzle.chainValid()) {
            return;
        }

        float spool = (float) Mth.clamp(nozzle.spool(), 0.0, 1.0);
        if (spool < 0.035f) {
            return;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double maxDistance = 160.0;
        if (camera.distanceToSqr(Vec3.atCenterOf(nozzle.getBlockPos())) > maxDistance * maxDistance) {
            return;
        }

        Direction facing = nozzle.getBlockState().getValue(DirectionalBlock.FACING);
        Vector3f axis = new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ()).normalize();

        Vector3f helper = Math.abs(axis.y) < 0.95f
                ? new Vector3f(0.0f, 1.0f, 0.0f)
                : new Vector3f(1.0f, 0.0f, 0.0f);

        Vector3f u = new Vector3f(axis).cross(helper).normalize();
        Vector3f v = new Vector3f(axis).cross(u).normalize();

        Vector3f origin = new Vector3f(0.5f, 0.5f, 0.5f)
                .add(axis.x * 0.55f, axis.y * 0.55f, axis.z * 0.55f);

        float time = nozzle.getLevel().getGameTime() + partialTick;
        float pulse = 0.975f + 0.025f * Mth.sin(time * 1.65f);

        // A fighter-style dry jet: nearly transparent blue-white gas, not a cartoon fire cone.
        float length = (1.15f + spool * 6.8f) * pulse;
        float mouthRadius = 0.24f + 0.035f * spool;

        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Matrix4f pose = poseStack.last().pose();

        // Outer hot-gas envelope: wide and faint.
        renderTube(
                consumer, pose, origin, axis, u, v,
                length,
                mouthRadius * 1.18f,
                0.10f,
                0.17f,
                time,
                0.070f,
                0.16f,
                new float[]{0.30f, 0.58f, 1.00f},
                new float[]{0.18f, 0.34f, 0.72f},
                spool
        );

        // Main shear layer.
        renderTube(
                consumer, pose, origin, axis, u, v,
                length * 0.90f,
                mouthRadius * 0.76f,
                0.055f,
                0.095f,
                time + 1.7f,
                0.045f,
                0.24f,
                new float[]{0.55f, 0.78f, 1.00f},
                new float[]{0.30f, 0.46f, 0.92f},
                spool
        );

        // Dense white-blue core. It ends earlier than the outer plume.
        renderTube(
                consumer, pose, origin, axis, u, v,
                length * 0.62f,
                mouthRadius * 0.38f,
                0.020f,
                0.040f,
                time + 3.1f,
                0.020f,
                0.46f,
                new float[]{0.96f, 0.99f, 1.00f},
                new float[]{0.56f, 0.80f, 1.00f},
                spool
        );

        // Distinct compression / expansion cells at high power. These are actual mesh bulges,
        // not spawned particles or flat sprites.
        if (spool > 0.58f) {
            renderShockCells(consumer, pose, origin, axis, u, v, length, mouthRadius, time, spool);
        }
    }

    private void renderTube(
            VertexConsumer out,
            Matrix4f pose,
            Vector3f origin,
            Vector3f axis,
            Vector3f u,
            Vector3f v,
            float length,
            float baseRadius,
            float tailRadius,
            float turbulence,
            float time,
            float nearAlpha,
            float coreBoost,
            float[] nearColor,
            float[] farColor,
            float spool
    ) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float t0 = segment / (float) SEGMENTS;
            float t1 = (segment + 1) / (float) SEGMENTS;

            colorAt(t0, nearColor, farColor, nearAlpha, coreBoost, spool, color0);
            colorAt(t1, nearColor, farColor, nearAlpha, coreBoost, spool, color1);

            float axial0 = length * t0;
            float axial1 = length * t1;

            for (int side = 0; side < RADIAL; side++) {
                float r00 = radiusAt(t0, side, baseRadius, tailRadius, turbulence, time, spool);
                float r01 = radiusAt(t0, side + 1, baseRadius, tailRadius, turbulence, time, spool);
                float r10 = radiusAt(t1, side, baseRadius, tailRadius, turbulence, time, spool);
                float r11 = radiusAt(t1, side + 1, baseRadius, tailRadius, turbulence, time, spool);

                putVertex(out, pose, origin, axis, u, v, axial0, COS[side], SIN[side], r00, color0);
                putVertex(out, pose, origin, axis, u, v, axial0, COS[side + 1], SIN[side + 1], r01, color0);
                putVertex(out, pose, origin, axis, u, v, axial1, COS[side + 1], SIN[side + 1], r11, color1);
                putVertex(out, pose, origin, axis, u, v, axial1, COS[side], SIN[side], r10, color1);
            }
        }
    }

    private static float radiusAt(
            float t,
            int side,
            float baseRadius,
            float tailRadius,
            float turbulence,
            float time,
            float spool
    ) {
        // Jet contracts just after the nozzle, then slowly spreads before fading.
        float coreProfile;
        if (t < 0.12f) {
            coreProfile = Mth.lerp(t / 0.12f, 1.0f, 0.72f);
        } else {
            float downstream = (t - 0.12f) / 0.88f;
            coreProfile = Mth.lerp(downstream, 0.72f, tailRadius / Math.max(0.001f, baseRadius));
        }

        // Mach-cell pressure oscillation. Strongest near the nozzle and at high throttle.
        float shock = 1.0f;
        if (spool > 0.58f) {
            float strength = (spool - 0.58f) / 0.42f;
            shock += 0.12f * strength * (float) Math.exp(-2.4f * t)
                    * Mth.cos(t * TWO_PI * 5.4f - time * 0.18f);
        }

        float angle = TWO_PI * (side % RADIAL) / RADIAL;
        float fray = 1.0f + turbulence * t
                * (0.58f * Mth.sin(angle * 3.0f + t * 21.0f - time * 1.8f)
                + 0.42f * Mth.sin(angle * 5.0f - t * 13.0f + time * 1.2f));

        return Math.max(0.002f, baseRadius * coreProfile * shock * fray);
    }

    private static void colorAt(
            float t,
            float[] near,
            float[] far,
            float alpha,
            float coreBoost,
            float spool,
            float[] out
    ) {
        float fade = (float) Math.pow(1.0f - t, 1.35f);
        float brightness = 0.72f + 0.28f * spool;

        out[0] = Mth.lerp(t, near[0], far[0]) * brightness;
        out[1] = Mth.lerp(t, near[1], far[1]) * brightness;
        out[2] = Mth.lerp(t, near[2], far[2]) * brightness;
        out[3] = Mth.clamp((alpha + coreBoost * (1.0f - t) * spool) * fade, 0.0f, 0.82f);
    }

    private static void renderShockCells(
            VertexConsumer out,
            Matrix4f pose,
            Vector3f origin,
            Vector3f axis,
            Vector3f u,
            Vector3f v,
            float plumeLength,
            float mouthRadius,
            float time,
            float spool
    ) {
        int cells = spool > 0.90f ? 5 : 4;
        float strength = (spool - 0.58f) / 0.42f;

        for (int cell = 0; cell < cells; cell++) {
            float center = plumeLength * (0.18f + cell * 0.135f);
            float halfLength = plumeLength * 0.034f;
            float radius = mouthRadius * (0.33f - cell * 0.035f) * (0.8f + 0.2f * strength);
            float alpha = (0.16f - cell * 0.022f) * strength;
            float flicker = 0.92f + 0.08f * Mth.sin(time * 0.7f + cell * 1.8f);

            // Two short cones back-to-back form a translucent Mach diamond.
            renderDiamondHalf(out, pose, origin, axis, u, v,
                    center - halfLength, center, radius * 0.30f, radius,
                    alpha * flicker);
            renderDiamondHalf(out, pose, origin, axis, u, v,
                    center, center + halfLength, radius, radius * 0.22f,
                    alpha * flicker);
        }
    }

    private static void renderDiamondHalf(
            VertexConsumer out,
            Matrix4f pose,
            Vector3f origin,
            Vector3f axis,
            Vector3f u,
            Vector3f v,
            float axial0,
            float axial1,
            float radius0,
            float radius1,
            float alpha
    ) {
        float[] c0 = {0.78f, 0.91f, 1.00f, alpha};
        float[] c1 = {0.56f, 0.78f, 1.00f, alpha * 0.55f};

        for (int side = 0; side < RADIAL; side++) {
            putVertex(out, pose, origin, axis, u, v, axial0, COS[side], SIN[side], radius0, c0);
            putVertex(out, pose, origin, axis, u, v, axial0, COS[side + 1], SIN[side + 1], radius0, c0);
            putVertex(out, pose, origin, axis, u, v, axial1, COS[side + 1], SIN[side + 1], radius1, c1);
            putVertex(out, pose, origin, axis, u, v, axial1, COS[side], SIN[side], radius1, c1);
        }
    }

    private static void putVertex(
            VertexConsumer out,
            Matrix4f pose,
            Vector3f origin,
            Vector3f axis,
            Vector3f u,
            Vector3f v,
            float axial,
            float cos,
            float sin,
            float radius,
            float[] color
    ) {
        float radialX = (u.x * cos + v.x * sin) * radius;
        float radialY = (u.y * cos + v.y * sin) * radius;
        float radialZ = (u.z * cos + v.z * sin) * radius;

        float x = origin.x + axis.x * axial + radialX;
        float y = origin.y + axis.y * axial + radialY;
        float z = origin.z + axis.z * axial + radialZ;

        out.addVertex(pose, x, y, z)
                .setColor(color[0], color[1], color[2], color[3]);
    }
}
