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

public class ModelFireSpearRocket_Converted<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_fire_spear_rocket_converted"), "main"
   );
   public final ModelPart Rocket;
   public final ModelPart fins;

   public ModelFireSpearRocket_Converted(ModelPart root) {
      this.Rocket = root.getChild("Rocket");
      this.fins = this.Rocket.getChild("fins");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Rocket = partdefinition.addOrReplaceChild("Rocket", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, -1.5708F, 0.0F, 0.0F));
      PartDefinition cube_r1 = Rocket.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create()
            .texOffs(0, 69)
            .addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(58, 56)
            .addBox(-2.5F, -2.5F, 7.0F, 5.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 48)
            .addBox(-2.5F, -2.5F, 29.0F, 5.0F, 5.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0)
            .addBox(-2.0F, -2.0F, 3.0F, 4.0F, 4.0F, 44.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, -23.0F, 0.0F, 0.0F, 0.7854F)
      );
      PartDefinition fins = Rocket.addOrReplaceChild(
         "fins",
         CubeListBuilder.create()
            .texOffs(42, 48)
            .addBox(-8.0F, 0.0F, 0.0F, 16.0F, 0.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(42, 56)
            .addBox(0.0F, -8.0F, 0.0F, 0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(42, 80)
            .addBox(0.0F, -8.0F, -32.0F, 0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(41, 80)
            .addBox(-8.0F, 0.0F, -32.0F, 16.0F, 0.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 16.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Rocket.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }
}
