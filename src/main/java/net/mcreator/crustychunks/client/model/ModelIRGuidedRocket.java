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

public class ModelIRGuidedRocket<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_ir_guided_rocket"), "main");
   public final ModelPart Missile;
   public final ModelPart Core;
   public final ModelPart Fins;
   public final ModelPart Warhead;

   public ModelIRGuidedRocket(ModelPart root) {
      this.Missile = root.getChild("Missile");
      this.Core = this.Missile.getChild("Core");
      this.Fins = this.Missile.getChild("Fins");
      this.Warhead = this.Missile.getChild("Warhead");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Missile = partdefinition.addOrReplaceChild("Missile", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, -1.5708F, 0.0F, 0.0F));
      PartDefinition Core = Missile.addOrReplaceChild(
         "Core",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-5.0F, -12.0F, -5.0F, 12.0F, 12.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(24, 53)
            .addBox(0.0F, -14.0F, 0.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(56, 0)
            .addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-1.0F, 6.0F, -3.0F)
      );
      PartDefinition Fins = Missile.addOrReplaceChild(
         "Fins",
         CubeListBuilder.create()
            .texOffs(40, 12)
            .addBox(-7.0F, 2.0F, -2.0F, 12.0F, 12.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(40, 60)
            .addBox(-9.0F, 7.0F, 1.0F, 4.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(40, 60)
            .addBox(3.0F, 7.0F, 1.0F, 4.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(62, 56)
            .addBox(-2.0F, 12.0F, 1.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(62, 56)
            .addBox(-2.0F, 0.0F, 1.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.0F, -8.0F, 10.0F)
      );
      PartDefinition Warhead = Missile.addOrReplaceChild(
         "Warhead",
         CubeListBuilder.create()
            .texOffs(2, 67)
            .addBox(-10.0F, -9.0F, 2.0F, 12.0F, 12.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(2, 102)
            .addBox(-8.0F, -7.0F, 0.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(4.0F, 3.0F, -24.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Missile.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }
}
