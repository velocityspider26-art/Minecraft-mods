package net.mcreator.crustychunks.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.crustychunks.client.model.Modelcommander;
import net.mcreator.crustychunks.entity.CommanderEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class CommanderRenderer extends MobRenderer<CommanderEntity, Modelcommander<CommanderEntity>> {
   public CommanderRenderer(Context context) {
      super(context, new Modelcommander(context.bakeLayer(Modelcommander.LAYER_LOCATION)), 0.5F);
      this.addLayer(
         new RenderLayer<CommanderEntity, Modelcommander<CommanderEntity>>(this) {
            final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("crusty_chunks:textures/entities/commanderglow.png");

            public void render(
               PoseStack poseStack,
               MultiBufferSource bufferSource,
               int light,
               CommanderEntity entity,
               float limbSwing,
               float limbSwingAmount,
               float partialTicks,
               float ageInTicks,
               float netHeadYaw,
               float headPitch
            ) {
               VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(this.LAYER_TEXTURE));
               ((Modelcommander)this.getParentModel())
                  .renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
            }
         }
      );
   }

   public ResourceLocation getTextureLocation(CommanderEntity entity) {
      return ResourceLocation.parse("crusty_chunks:textures/entities/commander.png");
   }
}
