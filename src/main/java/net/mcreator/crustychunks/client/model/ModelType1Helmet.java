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

public class ModelType1Helmet<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_type_1_helmet"), "main");
   public final ModelPart group;

   public ModelType1Helmet(ModelPart root) {
      this.group = root.getChild("group");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition group = partdefinition.addOrReplaceChild(
         "group",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.5F, -10.0F, -4.5F, 9.0F, 5.0F, 9.0F, new CubeDeformation(0.1F))
            .texOffs(0, 14)
            .addBox(4.5F, -8.5F, -4.5F, 1.0F, 6.0F, 9.0F, new CubeDeformation(0.0F))
            .texOffs(0, 14)
            .mirror()
            .addBox(-5.5F, -8.5F, -4.5F, 1.0F, 6.0F, 9.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      PartDefinition cube_r1 = group.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create().texOffs(0, 29).addBox(-0.5F, -6.5F, -1.5F, 1.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(3.0F, -2.0F, 5.0F, 0.0F, -1.5708F, 0.0F)
      );
      PartDefinition cube_r2 = group.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(20, 29).addBox(-4.5F, -5.0F, -0.5F, 9.0F, 5.0F, 1.0F, new CubeDeformation(0.15F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, -0.4363F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.group.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }
}
