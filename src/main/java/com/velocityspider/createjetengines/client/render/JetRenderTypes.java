package com.velocityspider.createjetengines.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/** Render states for the exhaust plume. */
public final class JetRenderTypes {

    /**
     * Additive, unlit, depth-tested but not depth-writing, and double sided.
     *
     * <p>Additive is what makes overlapping shells accumulate into a hot core instead of flatly
     * compositing. Not writing depth lets the concentric shells blend with each other rather than
     * occluding one another, and {@code NO_CULL} means the tube reads correctly when the camera is
     * inside it.
     */
    public static final RenderType PLUME = RenderType.create(
            "create_jet_engines:plume",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            2048,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    private JetRenderTypes() {
    }
}
