package com.velocityspider.createjetengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velocityspider.createjetengines.CreateJetEngines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

/**
 * Standalone models for the moving parts, plus the shared transform helpers.
 *
 * <p>The animated geometry lives in ordinary block-model JSON, registered as standalone models and
 * drawn by the block entity renderers. That keeps the mod on vanilla rendering, which matters here:
 * Sable renders block entities inside sublevels through its own dispatcher, so a plain
 * {@code BlockEntityRenderer} keeps working when the aircraft moves and rotates.
 */
public final class JetPartials {

    public static final ModelResourceLocation FAN_ROTOR = standalone("block/fan_rotor");
    public static final ModelResourceLocation COMPRESSOR_ROTOR = standalone("block/compressor_rotor");
    public static final ModelResourceLocation COMBUSTION_GLOW = standalone("block/combustion_glow");
    public static final ModelResourceLocation AFTERBURNER_GLOW = standalone("block/afterburner_glow");
    public static final ModelResourceLocation NOZZLE_PETAL = standalone("block/nozzle_petal");
    public static final ModelResourceLocation NOZZLE_INNER = standalone("block/nozzle_inner");

    public static final ModelResourceLocation[] ALL = {
            FAN_ROTOR, COMPRESSOR_ROTOR, COMBUSTION_GLOW, AFTERBURNER_GLOW, NOZZLE_PETAL, NOZZLE_INNER
    };

    private JetPartials() {
    }

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(CreateJetEngines.MODID, path));
    }

    public static BakedModel model(ModelResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    /**
     * The blockstate rotation for a facing, as a quaternion.
     *
     * <p>Models are authored pointing north, exactly like the blockstate expects, and this
     * reproduces vanilla's {@code BlockModelRotation} convention
     * ({@code rotationYXZ(-y, -x, 0)}) so animated parts land in the same place as the static model.
     */
    public static Quaternionf facingRotation(Direction facing) {
        int x = 0;
        int y = 0;
        switch (facing) {
            case NORTH -> { }
            case EAST -> y = 90;
            case SOUTH -> y = 180;
            case WEST -> y = 270;
            case UP -> x = 270;
            case DOWN -> x = 90;
        }
        return new Quaternionf().rotationYXZ(
                (float) Math.toRadians(-y), (float) Math.toRadians(-x), 0.0F);
    }

    /** Applies the facing rotation about the block centre. */
    public static void orient(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(facingRotation(facing));
        pose.translate(-0.5F, -0.5F, -0.5F);
    }

    public static void render(PoseStack pose, VertexConsumer consumer, BlockState state,
                              BakedModel model, int light, int overlay) {
        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        renderer.renderModel(pose.last(), consumer, state, model, 1.0F, 1.0F, 1.0F, light, overlay);
    }

    public static void render(PoseStack pose, VertexConsumer consumer, BlockState state,
                              BakedModel model, int light, int overlay, float r, float g, float b) {
        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        renderer.renderModel(pose.last(), consumer, state, model, r, g, b, light, overlay);
    }
}
