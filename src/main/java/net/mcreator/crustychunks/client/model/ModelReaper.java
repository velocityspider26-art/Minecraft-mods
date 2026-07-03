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

public class ModelReaper<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_reaper"), "main");
   public final ModelPart Reaper;
   public final ModelPart Engine;
   public final ModelPart LeftWing;
   public final ModelPart RightWing;
   public final ModelPart LeftTail;
   public final ModelPart RightTail;

   public ModelReaper(ModelPart root) {
      this.Reaper = root.getChild("Reaper");
      this.Engine = this.Reaper.getChild("Engine");
      this.LeftWing = this.Reaper.getChild("LeftWing");
      this.RightWing = this.Reaper.getChild("RightWing");
      this.LeftTail = this.Reaper.getChild("LeftTail");
      this.RightTail = this.Reaper.getChild("RightTail");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Reaper = partdefinition.addOrReplaceChild(
         "Reaper",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-6.0F, -12.0F, -39.0F, 12.0F, 12.0F, 72.0F, new CubeDeformation(0.0F))
            .texOffs(72, 157)
            .addBox(-4.5F, -12.5F, 33.0F, 9.0F, 11.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(0, 110)
            .addBox(-10.5F, -11.75F, -18.0F, 21.0F, 11.0F, 36.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      PartDefinition cube_r1 = Reaper.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create().texOffs(114, 110).addBox(-47.8355F, -1.225F, -12.1105F, 48.0F, 2.0F, 12.0F, new CubeDeformation(-0.1F)),
         PartPose.offsetAndRotation(-10.5F, -6.0F, 18.0F, 0.0F, -0.6981F, 0.0F)
      );
      PartDefinition cube_r2 = Reaper.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create()
            .texOffs(114, 110)
            .mirror()
            .addBox(0.1645F, -1.225F, -12.1105F, 48.0F, 2.0F, 12.0F, new CubeDeformation(-0.1F))
            .mirror(false),
         PartPose.offsetAndRotation(10.5F, -6.0F, 18.0F, 0.0F, 0.6981F, 0.0F)
      );
      PartDefinition cube_r3 = Reaper.addOrReplaceChild(
         "cube_r3",
         CubeListBuilder.create().texOffs(0, 157).addBox(-6.0F, 0.0F, -24.0F, 12.0F, 12.0F, 24.0F, new CubeDeformation(0.1F)),
         PartPose.offsetAndRotation(0.0F, -12.0F, -39.0F, 0.1745F, 0.0F, 0.0F)
      );
      PartDefinition Engine = Reaper.addOrReplaceChild(
         "Engine",
         CubeListBuilder.create()
            .texOffs(114, 124)
            .addBox(-5.0F, -20.0F, 16.0F, 10.0F, 10.0F, 32.0F, new CubeDeformation(-0.25F))
            .texOffs(142, 166)
            .addBox(-4.0F, -19.0F, 47.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftWing = Reaper.addOrReplaceChild("LeftWing", CubeListBuilder.create(), PartPose.offset(10.5F, -6.0F, 6.0F));
      PartDefinition cube_r4 = LeftWing.addOrReplaceChild(
         "cube_r4",
         CubeListBuilder.create()
            .texOffs(0, 84)
            .mirror()
            .addBox(0.0F, -1.25F, -24.0F, 72.0F, 2.0F, 24.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.4363F, 0.0F)
      );
      PartDefinition RightWing = Reaper.addOrReplaceChild("RightWing", CubeListBuilder.create(), PartPose.offset(-10.5F, -6.0F, 6.0F));
      PartDefinition cube_r5 = RightWing.addOrReplaceChild(
         "cube_r5",
         CubeListBuilder.create().texOffs(0, 84).addBox(-72.0F, -1.25F, -24.0F, 72.0F, 2.0F, 24.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.4363F, 0.0F)
      );
      PartDefinition LeftTail = Reaper.addOrReplaceChild("LeftTail", CubeListBuilder.create(), PartPose.offsetAndRotation(3.75F, -12.0F, 33.75F, 0.0F, 0.0F, 0.7854F));
      PartDefinition cube_r6 = LeftTail.addOrReplaceChild(
         "cube_r6",
         CubeListBuilder.create().texOffs(114, 166).addBox(-1.25F, -24.0F, -0.75F, 2.0F, 24.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F)
      );
      PartDefinition RightTail = Reaper.addOrReplaceChild("RightTail", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.75F, -12.0F, 33.75F, 0.0F, 0.0F, -0.7854F));
      PartDefinition cube_r7 = RightTail.addOrReplaceChild(
         "cube_r7",
         CubeListBuilder.create()
            .texOffs(114, 166)
            .mirror()
            .addBox(-1.25F, -24.0F, -0.75F, 2.0F, 24.0F, 12.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 256, 256);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Reaper.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.Reaper.yRot = netHeadYaw / (180.0F / (float)Math.PI);
      this.Reaper.xRot = headPitch / (180.0F / (float)Math.PI);
   }
}
