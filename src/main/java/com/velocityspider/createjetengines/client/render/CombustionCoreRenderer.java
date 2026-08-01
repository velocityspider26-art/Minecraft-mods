package com.velocityspider.createjetengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velocityspider.createjetengines.block.JetModuleBlock;
import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.client.sound.JetSoundHandler;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Internal combustion glow, driven by spool and throttle. */
public class CombustionCoreRenderer implements BlockEntityRenderer<CombustionCoreBlockEntity> {

    private static final int FULL_BRIGHT = LightTexture.pack(15, 15);

    public CombustionCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CombustionCoreBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof JetModuleBlock)) {
            return;
        }
        // Audio is driven from here: idempotent, and it stops on its own when the engine does.
        JetSoundHandler.ensurePlaying(be);

        float spool = be.getSpool(partialTick);
        if (spool <= 0.03F) {
            return;
        }
        float heat = Math.min(1.0F, spool * (0.6F + 0.4F * be.getThrottle()));
        // gentle flicker so the chamber never looks like a static lamp
        float flicker = 0.92F + 0.08F * (float) Math.sin((be.getLevel().getGameTime() + partialTick) * 0.9F);
        float intensity = heat * flicker;

        Direction facing = state.getValue(JetModuleBlock.FACING);
        pose.pushPose();
        JetPartials.orient(pose, facing);
        pose.translate(0.5F, 0.5F, 0.5F);
        float s = 0.6F + 0.4F * intensity;
        pose.scale(s, s, 1.0F);
        pose.translate(-0.5F, -0.5F, -0.5F);

        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        JetPartials.render(pose, vc, state, JetPartials.model(JetPartials.COMBUSTION_GLOW),
                FULL_BRIGHT, overlay, intensity, intensity * 0.62F, intensity * 0.26F);
        pose.popPose();
    }
}
