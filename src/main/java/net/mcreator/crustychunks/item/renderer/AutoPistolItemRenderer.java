package net.mcreator.crustychunks.item.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.Set;
import net.mcreator.crustychunks.item.AutoPistolItem;
import net.mcreator.crustychunks.item.model.AutoPistolItemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class AutoPistolItemRenderer extends GeoItemRenderer<AutoPistolItem> {
   private static final float SCALE_RECIPROCAL = 0.0625F;
   protected boolean renderArms = false;
   protected MultiBufferSource currentBuffer;
   protected RenderType renderType;
   public ItemDisplayContext transformType;
   protected AutoPistolItem animatable;
   private final Set<String> hiddenBones = new HashSet<>();
   private final Set<String> suppressedBones = new HashSet<>();

   public AutoPistolItemRenderer() {
      super(new AutoPistolItemModel());
   }

   public RenderType getRenderType(AutoPistolItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucent(this.getTextureLocation(animatable));
   }

   public void renderByItem(
      ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_
   ) {
      this.transformType = transformType;
      super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
   }

   public void actuallyRender(
      PoseStack matrixStackIn,
      AutoPistolItem animatable,
      BakedGeoModel model,
      RenderType type,
      MultiBufferSource renderTypeBuffer,
      VertexConsumer vertexBuilder,
      boolean isRenderer,
      float partialTicks,
      int packedLightIn,
      int packedOverlayIn,
      int colour) {
      this.currentBuffer = renderTypeBuffer;
      this.renderType = type;
      this.animatable = animatable;
      super.actuallyRender(
         matrixStackIn,
         animatable,
         model,
         type,
         renderTypeBuffer,
         vertexBuilder,
         isRenderer,
         partialTicks,
         packedLightIn,
         packedOverlayIn,
         colour);
      if (this.renderArms) {
         this.renderArms = false;
      }
   }

   public void renderRecursively(
      PoseStack stack,
      AutoPistolItem animatable,
      GeoBone bone,
      RenderType type,
      MultiBufferSource buffer,
      VertexConsumer bufferIn,
      boolean isReRender,
      float partialTick,
      int packedLightIn,
      int packedOverlayIn,
      int colour) {
      String name = bone.getName();
      boolean isArmBone = name.equals("Left") || name.equals("Right");
      if (isArmBone) {
         bone.setHidden(true);
      }

      if (this.transformType != null && this.transformType.firstPerson() && isArmBone) {
         Minecraft mc = Minecraft.getInstance();
         AbstractClientPlayer player = mc.player;
         PlayerRenderer renderer = (PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(player);
         stack.pushPose();
         RenderUtil.translateMatrixToBone(stack, bone);
         RenderUtil.translateToPivotPoint(stack, bone);
         RenderUtil.rotateMatrixAroundBone(stack, bone);
         RenderUtil.scaleMatrixForBone(stack, bone);
         RenderUtil.translateAwayFromPivotPoint(stack, bone);
         stack.translate(0.0F, 0.0F, 0.0F);
         stack.mulPose(Axis.YP.rotationDegrees(180.0F));
         if (name.equals("Left")) {
            renderer.renderLeftHand(stack, buffer, packedLightIn, player);
         } else {
            renderer.renderRightHand(stack, buffer, packedLightIn, player);
         }

         stack.popPose();
         buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(animatable)));
      }

      super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, colour);
   }

   public ResourceLocation getTextureLocation(AutoPistolItem instance) {
      return super.getTextureLocation(instance);
   }
}
