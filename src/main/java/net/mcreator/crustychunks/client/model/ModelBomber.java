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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class ModelBomber<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_bomber"), "main");
   public final ModelPart LegFL;
   public final ModelPart LegFR;
   public final ModelPart LegBR;
   public final ModelPart LegBL;
   public final ModelPart bb_main;

   public ModelBomber(ModelPart root) {
      this.LegFL = root.getChild("LegFL");
      this.LegFR = root.getChild("LegFR");
      this.LegBR = root.getChild("LegBR");
      this.LegBL = root.getChild("LegBL");
      this.bb_main = root.getChild("bb_main");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition LegFL = partdefinition.addOrReplaceChild("LegFL", CubeListBuilder.create(), PartPose.offset(7.0F, 10.0F, -10.0F));
      PartDefinition cube_r1 = LegFL.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create()
            .texOffs(0, 65)
            .addBox(-1.5F, -6.0F, -5.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(44, 30)
            .addBox(-1.0F, -5.0F, -5.0F, 2.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
      );
      PartDefinition LegFR = partdefinition.addOrReplaceChild("LegFR", CubeListBuilder.create(), PartPose.offset(-7.0F, 10.0F, -10.0F));
      PartDefinition cube_r2 = LegFR.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create()
            .texOffs(0, 65)
            .addBox(-1.5F, -6.0F, -5.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(44, 30)
            .addBox(-1.0F, -5.0F, -5.0F, 2.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
      );
      PartDefinition LegBR = partdefinition.addOrReplaceChild("LegBR", CubeListBuilder.create(), PartPose.offset(-7.0F, 10.0F, 8.0F));
      PartDefinition cube_r3 = LegBR.addOrReplaceChild(
         "cube_r3",
         CubeListBuilder.create()
            .texOffs(0, 65)
            .addBox(-1.5F, -6.0F, -5.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(44, 30)
            .addBox(-1.0F, -5.0F, -5.0F, 2.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
      );
      PartDefinition LegBL = partdefinition.addOrReplaceChild("LegBL", CubeListBuilder.create(), PartPose.offset(7.0F, 10.0F, 8.0F));
      PartDefinition cube_r4 = LegBL.addOrReplaceChild(
         "cube_r4",
         CubeListBuilder.create()
            .texOffs(44, 30)
            .addBox(-1.0F, -5.0F, -5.0F, 2.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(0, 65)
            .addBox(-1.5F, -6.0F, -5.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
      );
      PartDefinition bb_main = partdefinition.addOrReplaceChild(
         "bb_main",
         CubeListBuilder.create()
            .texOffs(0, 53)
            .addBox(-6.0F, -15.0F, -11.0F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.5F))
            .texOffs(1, 0)
            .addBox(-4.0F, -17.0F, -12.0F, 8.0F, 6.0F, 24.0F, new CubeDeformation(0.0F))
            .texOffs(0, 30)
            .addBox(-3.0F, -16.0F, -13.0F, 6.0F, 7.0F, 16.0F, new CubeDeformation(-0.25F))
            .texOffs(0, 59)
            .addBox(-6.0F, -15.0F, 7.0F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.5F))
            .texOffs(44, 0)
            .addBox(-3.0F, -23.0F, -9.0F, 6.0F, 6.0F, 18.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.LegFL.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.LegFR.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.LegBR.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.LegBL.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.LegBR.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
      this.LegFR.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
      this.LegBL.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
      this.LegFL.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
   }
}
