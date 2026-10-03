package dev.velocityspider.caw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.velocityspider.caw.CreateAerialWarfare;
import dev.velocityspider.caw.blockentity.EngineNozzleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EngineNozzleRenderer implements BlockEntityRenderer<EngineNozzleBlockEntity> {
    private static final ResourceLocation PLUME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateAerialWarfare.MOD_ID,
                    "textures/effect/jet_plume.png"
            );

    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int SIDES = 8;

    public EngineNozzleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            EngineNozzleBlockEntity nozzle,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        double spool = nozzle.spool();
        if (!nozzle.chainValid() || spool < 0.06) {
            return;
        }

        BlockState state = nozzle.getBlockState();
        Direction exhaustDirection = state.getValue(DirectionalBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        Vector3f from = new Vector3f(0.0f, 0.0f, 1.0f);
        Vector3f to = new Vector3f(
                exhaustDirection.getStepX(),
                exhaustDirection.getStepY(),
                exhaustDirection.getStepZ()
        );

        poseStack.mulPose(new Quaternionf().rotationTo(from, to));

        VertexConsumer consumer =
                buffers.getBuffer(RenderType.entityTranslucent(PLUME_TEXTURE));

        float s = (float) Math.min(1.0, spool);
        float startZ = 0.50f;
        float length = 1.55f + 4.75f * s;

        renderFrustum(
                consumer, poseStack.last(),
                startZ, startZ + length,
                0.22f + 0.05f * s, 0.08f,
                150, 205, 255, 82,
                0.0f, 1.0f
        );

        renderFrustum(
                consumer, poseStack.last(),
                startZ + 0.02f, startZ + length * 0.76f,
                0.105f + 0.03f * s, 0.035f,
                235, 250, 255, 178,
                0.0f, 0.78f
        );

        if (s > 0.62f) {
            int cells = 3 + (s > 0.90f ? 1 : 0);
            float cellStart = startZ + 0.48f;
            float cellSpacing = 0.62f + 0.18f * s;

            for (int i = 0; i < cells; i++) {
                float z0 = cellStart + i * cellSpacing;
                if (z0 >= startZ + length * 0.86f) {
                    break;
                }

                float zMid = z0 + 0.18f;
                float z1 = z0 + 0.36f;
                float base = 0.045f + 0.012f * (cells - i);
                float peak = base * 1.75f;
                int alpha = Math.max(38, 92 - i * 14);

                renderFrustum(
                        consumer, poseStack.last(),
                        z0, zMid,
                        base, peak,
                        220, 244, 255, alpha,
                        0.08f * i, 0.08f * i + 0.06f
                );

                renderFrustum(
                        consumer, poseStack.last(),
                        zMid, z1,
                        peak, base,
                        190, 232, 255, alpha,
                        0.08f * i + 0.06f, 0.08f * i + 0.12f
                );
            }
        }

        poseStack.popPose();
    }

    private static void renderFrustum(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float z0,
            float z1,
            float r0,
            float r1,
            int red,
            int green,
            int blue,
            int alpha,
            float u0,
            float u1
    ) {
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;

            float a0 = (float) (Math.PI * 2.0 * i / SIDES);
            float a1 = (float) (Math.PI * 2.0 * next / SIDES);

            float x00 = (float) Math.cos(a0) * r0;
            float y00 = (float) Math.sin(a0) * r0;
            float x01 = (float) Math.cos(a1) * r0;
            float y01 = (float) Math.sin(a1) * r0;

            float x10 = (float) Math.cos(a0) * r1;
            float y10 = (float) Math.sin(a0) * r1;
            float x11 = (float) Math.cos(a1) * r1;
            float y11 = (float) Math.sin(a1) * r1;

            float mid = (a0 + a1) * 0.5f;
            float nx = (float) Math.cos(mid);
            float ny = (float) Math.sin(mid);

            float v0 = (float) i / SIDES;
            float v1 = (float) (i + 1) / SIDES;

            vertex(consumer, pose, x00, y00, z0, u0, v0, red, green, blue, alpha, nx, ny);
            vertex(consumer, pose, x01, y01, z0, u0, v1, red, green, blue, alpha, nx, ny);
            vertex(consumer, pose, x11, y11, z1, u1, v1, red, green, blue, 0, nx, ny);
            vertex(consumer, pose, x10, y10, z1, u1, v0, red, green, blue, 0, nx, ny);
        }
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha,
            float nx,
            float ny
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, 0.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(EngineNozzleBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
