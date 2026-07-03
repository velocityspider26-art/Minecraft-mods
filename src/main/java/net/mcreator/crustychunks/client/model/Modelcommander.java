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

public class Modelcommander<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "modelcommander"), "main");
   public final ModelPart Commander;
   public final ModelPart RightArm;
   public final ModelPart Barrel;
   public final ModelPart LeftLeg;
   public final ModelPart LeftFoot;
   public final ModelPart Rightleg;
   public final ModelPart RightFoot;
   public final ModelPart Torso;
   public final ModelPart LeftArm;

   public Modelcommander(ModelPart root) {
      this.Commander = root.getChild("Commander");
      this.RightArm = this.Commander.getChild("RightArm");
      this.Barrel = this.RightArm.getChild("Barrel");
      this.LeftLeg = this.Commander.getChild("LeftLeg");
      this.LeftFoot = this.LeftLeg.getChild("LeftFoot");
      this.Rightleg = this.Commander.getChild("Rightleg");
      this.RightFoot = this.Rightleg.getChild("RightFoot");
      this.Torso = this.Commander.getChild("Torso");
      this.LeftArm = this.Commander.getChild("LeftArm");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Commander = partdefinition.addOrReplaceChild("Commander", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -3.0F));
      PartDefinition RightArm = Commander.addOrReplaceChild(
         "RightArm",
         CubeListBuilder.create()
            .texOffs(0, 15)
            .addBox(-1.0F, -1.0F, -9.0F, 1.0F, 3.0F, 11.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0)
            .addBox(-2.0F, -2.0F, -8.0F, 3.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-5.0F, 0.0F, 5.0F)
      );
      PartDefinition Barrel = RightArm.addOrReplaceChild("Barrel", CubeListBuilder.create(), PartPose.offset(4.0F, 1.0F, -15.0F));
      PartDefinition cube_r1 = Barrel.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create().texOffs(16, 20).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-4.0F, -1.0F, 2.0F, 0.0F, 0.0F, -0.829F)
      );
      PartDefinition cube_r2 = Barrel.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(15, 19).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.829F)
      );
      PartDefinition LeftLeg = Commander.addOrReplaceChild(
         "LeftLeg",
         CubeListBuilder.create().texOffs(31, 19).mirror().addBox(-1.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(4.0F, 9.0F, 5.0F)
      );
      PartDefinition LeftFoot = LeftLeg.addOrReplaceChild(
         "LeftFoot",
         CubeListBuilder.create()
            .texOffs(4, 4)
            .addBox(-0.5F, 1.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 31)
            .mirror()
            .addBox(-1.0F, 5.0F, -2.0F, 2.0F, 10.0F, 4.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Rightleg = Commander.addOrReplaceChild(
         "Rightleg",
         CubeListBuilder.create().texOffs(31, 19).addBox(-1.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-4.0F, 9.0F, 5.0F)
      );
      PartDefinition RightFoot = Rightleg.addOrReplaceChild(
         "RightFoot",
         CubeListBuilder.create()
            .texOffs(4, 4)
            .addBox(-0.5F, 1.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 31)
            .addBox(-1.0F, 5.0F, -2.0F, 2.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Torso = Commander.addOrReplaceChild(
         "Torso",
         CubeListBuilder.create()
            .texOffs(20, 0)
            .addBox(-2.0F, -4.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(36, 0)
            .addBox(-3.0F, -11.0F, -2.0F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(15, 15)
            .addBox(-4.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(15, 15)
            .addBox(-4.0F, -10.0F, -1.0F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(28, 32)
            .addBox(-3.0F, -13.0F, -2.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.5F)),
         PartPose.offset(0.0F, 9.0F, 5.0F)
      );
      PartDefinition Antenna_r1 = Torso.addOrReplaceChild(
         "Antenna_r1",
         CubeListBuilder.create()
            .texOffs(48, 33)
            .addBox(3.0F, -8.0F, -1.0F, 0.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(48, 33)
            .addBox(-3.0F, -8.0F, -1.0F, 0.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -12.0F, 1.0F, -0.4363F, 0.0F, 0.0F)
      );
      PartDefinition LeftArm = Commander.addOrReplaceChild(
         "LeftArm",
         CubeListBuilder.create()
            .texOffs(12, 33)
            .addBox(-1.0F, -2.0F, -8.0F, 2.0F, 2.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(16, 32)
            .addBox(-0.5F, -1.0F, -12.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
            .texOffs(0, 15)
            .addBox(-0.5F, -1.0F, -9.0F, 1.0F, 3.0F, 11.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(5.0F, 0.0F, 5.0F, 0.7854F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Commander.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.RightArm.yRot = headPitch / (180.0F / (float)Math.PI);
      this.LeftLeg.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
      this.Rightleg.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
      this.LeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
      this.Commander.yRot = netHeadYaw / (180.0F / (float)Math.PI);
   }
}
