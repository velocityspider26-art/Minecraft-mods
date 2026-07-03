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

public class ModelFlamePack<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_flame_pack"), "main");
   public final ModelPart Pack;
   public final ModelPart bone;
   public final ModelPart bone2;

   public ModelFlamePack(ModelPart root) {
      this.Pack = root.getChild("Pack");
      this.bone = root.getChild("bone");
      this.bone2 = root.getChild("bone2");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Pack = partdefinition.addOrReplaceChild(
         "Pack",
         CubeListBuilder.create()
            .texOffs(12, 10)
            .addBox(-4.0F, 1.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.4F))
            .texOffs(12, 10)
            .addBox(-4.0F, 5.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.4F))
            .texOffs(0, 0)
            .addBox(0.0F, 0.0F, 2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(-0.3F))
            .texOffs(0, 0)
            .addBox(-4.0F, 0.0F, 2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(-0.3F)),
         PartPose.offset(0.0F, 15.0F, 0.0F)
      );
      PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition bone2 = partdefinition.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Pack.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.bone2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
