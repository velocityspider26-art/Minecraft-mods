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

public class ModelGasMaskHelmet<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_gas_mask_helmet"), "main");
   public final ModelPart group;

   public ModelGasMaskHelmet(ModelPart root) {
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
            .mirror(false)
            .texOffs(30, 56)
            .addBox(-3.0F, -3.0F, -4.0F, 6.0F, 3.0F, 5.0F, new CubeDeformation(0.6F))
            .texOffs(30, 44)
            .addBox(-4.0F, -6.0F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.51F)),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      PartDefinition cube_r1 = group.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create()
            .texOffs(54, 49)
            .addBox(-1.0F, -5.2426F, -3.8284F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.25F))
            .texOffs(52, 56)
            .addBox(-1.0F, -5.2426F, -3.4142F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F)
      );
      PartDefinition cube_r2 = group.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(0, 29).addBox(-0.5F, -6.5F, -1.5F, 1.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(3.0F, -2.0F, 5.0F, 0.0F, -1.5708F, 0.0F)
      );
      PartDefinition cube_r3 = group.addOrReplaceChild(
         "cube_r3",
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
