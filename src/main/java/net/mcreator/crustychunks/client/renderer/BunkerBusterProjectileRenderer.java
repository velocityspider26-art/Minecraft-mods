package net.mcreator.crustychunks.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcreator.crustychunks.client.model.ModelSuperheavyBomb;
import net.mcreator.crustychunks.entity.BunkerBusterProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BunkerBusterProjectileRenderer extends EntityRenderer<BunkerBusterProjectileEntity> {
   private static final ResourceLocation texture = ResourceLocation.parse("crusty_chunks:textures/entities/bunkerbuster.png");
   private final ModelSuperheavyBomb model;

   public BunkerBusterProjectileRenderer(Context context) {
      super(context);
      this.model = new ModelSuperheavyBomb(context.bakeLayer(ModelSuperheavyBomb.LAYER_LOCATION));
   }

   public void render(
      BunkerBusterProjectileEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn
   ) {
      VertexConsumer vb = bufferIn.getBuffer(RenderType.entityCutout(this.getTextureLocation(entityIn)));
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 90.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F + Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
      this.model.renderToBuffer(poseStack, vb, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
      poseStack.popPose();
      super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
   }

   public ResourceLocation getTextureLocation(BunkerBusterProjectileEntity entity) {
      return texture;
   }
}
