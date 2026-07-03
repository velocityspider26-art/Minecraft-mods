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

public class ModelLargeRocket<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_large_rocket"), "main");
   public final ModelPart Rocket;
   public final ModelPart Core;
   public final ModelPart Warhead;
   public final ModelPart Fins;

   public ModelLargeRocket(ModelPart root) {
      this.Rocket = root.getChild("Rocket");
      this.Core = this.Rocket.getChild("Core");
      this.Warhead = this.Rocket.getChild("Warhead");
      this.Fins = this.Rocket.getChild("Fins");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Rocket = partdefinition.addOrReplaceChild("Rocket", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 24.0F, -8.0F, -1.5708F, 0.0F, 0.0F));
      PartDefinition Core = Rocket.addOrReplaceChild(
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
      PartDefinition Warhead = Rocket.addOrReplaceChild(
         "Warhead",
         CubeListBuilder.create()
            .texOffs(2, 67)
            .addBox(-9.0F, -9.0F, 2.0F, 12.0F, 12.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(2, 93)
            .addBox(-6.0F, -6.0F, 0.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, -5.0F, -24.0F)
      );
      PartDefinition Fins = Rocket.addOrReplaceChild(
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
         PartPose.offset(1.0F, -16.0F, 10.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Rocket.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
