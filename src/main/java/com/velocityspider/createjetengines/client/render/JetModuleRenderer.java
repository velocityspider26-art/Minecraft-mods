package com.velocityspider.createjetengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velocityspider.createjetengines.block.JetModuleBlock;
import com.velocityspider.createjetengines.blockentity.JetModuleBlockEntity;
import com.velocityspider.createjetengines.client.particle.ExhaustEffects;
import com.velocityspider.createjetengines.engine.ModuleType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Animated geometry for the fan, compressor, afterburner and nozzle.
 *
 * <p>Rotation rate is driven by the synchronised spool fraction, so what you see always matches
 * what the server thinks the engine is doing.
 */
public class JetModuleRenderer implements BlockEntityRenderer<JetModuleBlockEntity> {

    private static final int FULL_BRIGHT = LightTexture.pack(15, 15);
    private static final float FAN_DEG_PER_TICK = 42.0F;
    private static final float COMPRESSOR_DEG_PER_TICK = 68.0F;
    private static final int PETALS = 8;

    public JetModuleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(JetModuleBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof JetModuleBlock module)) {
            return;
        }
        Direction facing = state.getValue(JetModuleBlock.FACING);
        ModuleType type = module.getModuleType();
        float spool = be.getSpool();

        switch (type) {
            case FAN -> renderSpinner(be, state, pose, buffers, light, overlay, facing,
                    JetPartials.FAN_ROTOR, FAN_DEG_PER_TICK, partialTick);
            case COMPRESSOR -> renderSpinner(be, state, pose, buffers, light, overlay, facing,
                    JetPartials.COMPRESSOR_ROTOR, COMPRESSOR_DEG_PER_TICK, partialTick);
            case AFTERBURNER -> renderAfterburner(be, state, pose, buffers, overlay, facing, spool);
            case NOZZLE -> renderNozzle(be, state, pose, buffers, light, overlay, facing, spool);
            default -> {
            }
        }
    }

    private void renderSpinner(JetModuleBlockEntity be, BlockState state, PoseStack pose,
                               MultiBufferSource buffers, int light, int overlay,
                               Direction facing, net.minecraft.client.resources.model.ModelResourceLocation part,
                               float degPerTick, float partialTick) {
        be.advanceSpin(degPerTick);
        float angle = be.getSpinAngle(partialTick);

        pose.pushPose();
        JetPartials.orient(pose, facing);
        pose.translate(0.5F, 0.5F, 0.5F);
        // Models are authored along Z, so the shaft axis is the model's own Z.
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        JetPartials.render(pose, vc, state, JetPartials.model(part), light, overlay);
        pose.popPose();
    }

    /** Internal reheat glow; brightens and grows as the afterburner lights. */
    private void renderAfterburner(JetModuleBlockEntity be, BlockState state, PoseStack pose,
                                   MultiBufferSource buffers, int overlay, Direction facing, float spool) {
        boolean lit = be.isAfterburnerActive();
        float intensity = lit ? 0.55F + 0.45F * spool : spool * 0.18F;
        if (intensity <= 0.02F) {
            return;
        }
        pose.pushPose();
        JetPartials.orient(pose, facing);
        pose.translate(0.5F, 0.5F, 0.5F);
        float s = 0.55F + 0.45F * intensity;
        pose.scale(s, s, 1.0F);
        pose.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        // Blue core when lit, dull orange when merely hot.
        float r = lit ? 0.55F : 1.0F;
        float g = lit ? 0.70F : 0.55F;
        float b = lit ? 1.0F : 0.30F;
        JetPartials.render(pose, vc, state, JetPartials.model(JetPartials.AFTERBURNER_GLOW),
                FULL_BRIGHT, overlay, r * intensity, g * intensity, b * intensity);
        pose.popPose();
    }

    /**
     * Nozzle petals hinge outward with power, and the exhaust tunnel glows once the engine is hot.
     */
    private void renderNozzle(JetModuleBlockEntity be, BlockState state, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay,
                              Direction facing, float spool) {
        float open = 0.0F;
        var core = be.getCore();
        if (core != null) {
            open = core.getNozzleOpen();
            if (be.tryEffectTick()) {
                ExhaustEffects.emit(be, core, 0.0F);
            }
        }
        float openDegrees = -6.0F + open * 26.0F;

        VertexConsumer cutout = buffers.getBuffer(RenderType.cutout());
        var petal = JetPartials.model(JetPartials.NOZZLE_PETAL);
        for (int i = 0; i < PETALS; i++) {
            pose.pushPose();
            JetPartials.orient(pose, facing);
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.mulPose(Axis.ZP.rotationDegrees(i * (360.0F / PETALS)));
            pose.translate(-0.5F, -0.5F, -0.5F);
            // hinge at the collar end of the petal
            pose.translate(0.5F, 11.0F / 16.0F, 6.5F / 16.0F);
            pose.mulPose(Axis.XP.rotationDegrees(openDegrees));
            pose.translate(-0.5F, -11.0F / 16.0F, -6.5F / 16.0F);
            JetPartials.render(pose, cutout, state, petal, light, overlay);
            pose.popPose();
        }

        // hot nozzle interior — visible on dry power, not just with reheat
        if (spool > 0.25F) {
            float heat = (spool - 0.25F) / 0.75F;
            boolean lit = be.isAfterburnerActive();
            pose.pushPose();
            JetPartials.orient(pose, facing);
            VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
            float r = lit ? 0.7F : 1.0F;
            float g = lit ? 0.8F : 0.42F;
            float b = lit ? 1.0F : 0.16F;
            JetPartials.render(pose, vc, state, JetPartials.model(JetPartials.NOZZLE_INNER),
                    FULL_BRIGHT, overlay, r * heat, g * heat, b * heat);
            pose.popPose();

            renderPlume(state, pose, buffers, overlay, facing, spool, lit);
        }
    }

    /**
     * The exhaust plume, built as a stack of progressively smaller shells marching out of the
     * nozzle rather than one camera-facing quad. Alternate shells are rolled 22.5 degrees so the
     * silhouette stays convincing from any viewing angle, and the periodic scale ripple reads as
     * shock diamonds.
     */
    private void renderPlume(BlockState state, PoseStack pose, MultiBufferSource buffers,
                             int overlay, Direction facing, float spool, boolean lit) {
        int segments = lit ? 14 : 6;
        float length = (lit ? 3.4F : 0.9F) * (0.35F + 0.65F * spool);
        var glow = JetPartials.model(JetPartials.AFTERBURNER_GLOW);
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());

        for (int i = 0; i < segments; i++) {
            float t = i / (float) segments;
            // exhaust leaves along the model's -Z
            float dist = t * length;
            float taper = (1.0F - t) * (lit ? 0.85F : 0.55F);
            // shock diamonds: periodic pinch in the core flow
            float diamond = lit ? 1.0F + 0.18F * (float) Math.sin(t * Math.PI * 5.0) : 1.0F;
            float s = Math.max(0.02F, taper * diamond * (0.5F + 0.5F * spool));

            float alpha = (1.0F - t) * (lit ? 0.9F : 0.28F);
            float r = lit ? (t < 0.35F ? 0.45F : 1.0F) : 1.0F;
            float g = lit ? (t < 0.35F ? 0.55F : 0.62F) : 0.55F;
            float b = lit ? 1.0F : 0.38F;

            pose.pushPose();
            JetPartials.orient(pose, facing);
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.translate(0.0F, 0.0F, -dist);
            pose.mulPose(Axis.ZP.rotationDegrees(i % 2 == 0 ? 0.0F : 22.5F));
            pose.scale(s, s, Math.max(0.05F, length / segments * 1.6F));
            pose.translate(-0.5F, -0.5F, -0.5F);
            JetPartials.render(pose, vc, state, glow, FULL_BRIGHT, overlay,
                    r * alpha, g * alpha, b * alpha);
            pose.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(JetModuleBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
