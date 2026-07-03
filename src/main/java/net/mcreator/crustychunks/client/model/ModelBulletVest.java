package net.mcreator.crustychunks.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ModelBulletVest<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_bullet_vest"), "main");
   public final ModelPart Body;
   public final ModelPart Left;
   public final ModelPart Right;

   public ModelBulletVest(ModelPart root) {
      this.Body = root.getChild("Body");
      this.Left = root.getChild("Left");
      this.Right = root.getChild("Right");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Body = partdefinition.addOrReplaceChild(
         "Body",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.26F))
            .texOffs(0, 16)
            .addBox(-4.0F, 2.0F, -3.0F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(18, 16)
            .addBox(-4.0F, 2.0F, 2.0F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      PartDefinition Left = partdefinition.addOrReplaceChild("Left", CubeListBuilder.create(), PartPose.offset(4.0F, 14.0F, 0.0F));
      PartDefinition Right = partdefinition.addOrReplaceChild("Right", CubeListBuilder.create(), PartPose.offset(-4.0F, 14.0F, 0.0F));
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.Left.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.Right.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }
}
