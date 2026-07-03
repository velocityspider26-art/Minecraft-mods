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

public class ModelSmallBomb_Converted<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_small_bomb_converted"), "main");
   public final ModelPart bone;

   public ModelSmallBomb_Converted(ModelPart root) {
      this.bone = root.getChild("bone");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition bone = partdefinition.addOrReplaceChild(
         "bone",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-11.0F, -15.0F, 5.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 14)
            .addBox(-10.0F, -16.0F, 6.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(8.0F, 24.0F, -8.0F)
      );
      PartDefinition cube_r1 = bone.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create()
            .texOffs(16, 6)
            .addBox(-1.0F, -6.0F, -4.0F, 2.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(16, 19)
            .addBox(-4.0F, -6.0F, -1.0F, 8.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-8.0F, 0.0F, 8.0F, 0.0F, 0.7854F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
