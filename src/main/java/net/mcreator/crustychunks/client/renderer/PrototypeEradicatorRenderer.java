package net.mcreator.crustychunks.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.crustychunks.entity.PrototypeEradicatorEntity;
import net.mcreator.crustychunks.entity.layer.PrototypeEradicatorLayer;
import net.mcreator.crustychunks.entity.model.PrototypeEradicatorModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PrototypeEradicatorRenderer extends GeoEntityRenderer<PrototypeEradicatorEntity> {
   public PrototypeEradicatorRenderer(Context renderManager) {
      super(renderManager, new PrototypeEradicatorModel());
      this.shadowRadius = 0.5F;
      this.addRenderLayer(new PrototypeEradicatorLayer(this));
   }

   public RenderType getRenderType(PrototypeEradicatorEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucent(this.getTextureLocation(animatable));
   }

   public void preRender(
      PoseStack poseStack,
      PrototypeEradicatorEntity entity,
      BakedGeoModel model,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int colour) {
      float scale = 1.5F;
      this.scaleHeight = scale;
      this.scaleWidth = scale;
      super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
   }

   protected float getDeathMaxRotation(PrototypeEradicatorEntity entityLivingBaseIn) {
      return 0.0F;
   }
}
