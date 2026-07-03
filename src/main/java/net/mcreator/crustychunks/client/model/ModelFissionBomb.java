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

public class ModelFissionBomb<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_fission_bomb"), "main");
   public final ModelPart FissionBomb;
   public final ModelPart CoreAddition;
   public final ModelPart Warhead;
   public final ModelPart Fins;
   public final ModelPart Core;

   public ModelFissionBomb(ModelPart root) {
      this.FissionBomb = root.getChild("FissionBomb");
      this.CoreAddition = this.FissionBomb.getChild("CoreAddition");
      this.Warhead = this.FissionBomb.getChild("Warhead");
      this.Fins = this.FissionBomb.getChild("Fins");
      this.Core = this.FissionBomb.getChild("Core");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition FissionBomb = partdefinition.addOrReplaceChild(
         "FissionBomb", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 24.0F, -8.0F, -1.5708F, 0.0F, 0.0F)
      );
      PartDefinition CoreAddition = FissionBomb.addOrReplaceChild(
         "CoreAddition",
         CubeListBuilder.create().texOffs(58, 65).addBox(-12.0F, -12.0F, 0.0F, 12.0F, 12.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offset(6.0F, -2.0F, -24.0F)
      );
      PartDefinition Warhead = FissionBomb.addOrReplaceChild(
         "Warhead",
         CubeListBuilder.create()
            .texOffs(2, 67)
            .addBox(-9.0F, -9.0F, 2.0F, 12.0F, 12.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(2, 93)
            .addBox(-6.0F, -6.0F, 0.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, -5.0F, -40.0F)
      );
      PartDefinition Fins = FissionBomb.addOrReplaceChild(
         "Fins",
         CubeListBuilder.create()
            .texOffs(0, 28)
            .addBox(-7.0F, 2.0F, -2.0F, 12.0F, 12.0F, 13.0F, new CubeDeformation(0.0F))
            .texOffs(0, 53)
            .addBox(-5.0F, 4.0F, 10.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.0F, -16.0F, 10.0F)
      );
      PartDefinition cube_r1 = Fins.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create().texOffs(44, 42).addBox(-5.0F, 1.0F, 4.0F, 10.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 8.0F, 14.0F, -1.5708F, 0.0F, 1.5708F)
      );
      PartDefinition cube_r2 = Fins.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(44, 42).addBox(-5.0F, 1.0F, 4.0F, 10.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 8.0F, 14.0F, -1.5708F, 0.0F, -1.5708F)
      );
      PartDefinition cube_r3 = Fins.addOrReplaceChild(
         "cube_r3",
         CubeListBuilder.create().texOffs(44, 42).addBox(-5.0F, 1.0F, 4.0F, 10.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 8.0F, 13.0F, -1.5708F, 0.0F, 3.1416F)
      );
      PartDefinition cube_r4 = Fins.addOrReplaceChild(
         "cube_r4",
         CubeListBuilder.create().texOffs(44, 42).addBox(-5.0F, 1.0F, 4.0F, 10.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 8.0F, 13.0F, -1.5708F, 0.0F, 0.0F)
      );
      PartDefinition Core = FissionBomb.addOrReplaceChild(
         "Core",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-5.0F, -12.0F, -5.0F, 12.0F, 12.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(24, 53)
            .addBox(0.0F, -14.0F, 0.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(56, 0)
            .addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-1.0F, -2.0F, -3.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.FissionBomb.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
