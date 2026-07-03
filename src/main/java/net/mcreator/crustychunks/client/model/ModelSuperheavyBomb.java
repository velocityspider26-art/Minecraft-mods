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

public class ModelSuperheavyBomb<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "model_superheavy_bomb"), "main");
   public final ModelPart bone;
   public final ModelPart Core;
   public final ModelPart CoreAddition;
   public final ModelPart Warhead;
   public final ModelPart Fins;

   public ModelSuperheavyBomb(ModelPart root) {
      this.bone = root.getChild("bone");
      this.Core = this.bone.getChild("Core");
      this.CoreAddition = this.bone.getChild("CoreAddition");
      this.Warhead = this.bone.getChild("Warhead");
      this.Fins = this.bone.getChild("Fins");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 24.0F, -8.0F, -1.5708F, 0.0F, 0.0F));
      PartDefinition Core = bone.addOrReplaceChild(
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
      PartDefinition CoreAddition = bone.addOrReplaceChild(
         "CoreAddition",
         CubeListBuilder.create().texOffs(58, 65).addBox(-12.0F, -12.0F, 0.0F, 12.0F, 12.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offset(6.0F, -2.0F, -24.0F)
      );
      PartDefinition Warhead = bone.addOrReplaceChild(
         "Warhead",
         CubeListBuilder.create()
            .texOffs(2, 67)
            .addBox(-9.0F, -9.0F, 2.0F, 12.0F, 12.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(2, 93)
            .addBox(-6.0F, -6.0F, 0.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, -5.0F, -40.0F)
      );
      PartDefinition Fins = bone.addOrReplaceChild(
         "Fins",
         CubeListBuilder.create()
            .texOffs(0, 28)
            .addBox(-7.0F, 2.0F, -2.0F, 12.0F, 12.0F, 13.0F, new CubeDeformation(0.0F))
            .texOffs(0, 53)
            .addBox(-5.0F, 4.0F, 10.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(50, 28)
            .addBox(-9.0F, 7.0F, 0.0F, 4.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(50, 28)
            .addBox(3.0F, 7.0F, 0.0F, 4.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(50, 44)
            .addBox(-2.0F, 12.0F, 0.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(50, 44)
            .addBox(-2.0F, 0.0F, 0.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.0F, -16.0F, 10.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
