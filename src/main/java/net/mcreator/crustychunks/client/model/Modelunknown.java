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

public class Modelunknown<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "modelunknown"), "main");
   public final ModelPart Head;
   public final ModelPart Antenna;
   public final ModelPart Antenna2;
   public final ModelPart mantlet;
   public final ModelPart Gun;

   public Modelunknown(ModelPart root) {
      this.Head = root.getChild("Head");
      this.Antenna = this.Head.getChild("Antenna");
      this.Antenna2 = this.Head.getChild("Antenna2");
      this.mantlet = this.Head.getChild("mantlet");
      this.Gun = this.mantlet.getChild("Gun");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Head = partdefinition.addOrReplaceChild(
         "Head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-21.5F, -15.0F, -19.5F, 44.0F, 15.0F, 66.0F, new CubeDeformation(0.0F))
            .texOffs(185, 151)
            .addBox(-11.0F, -16.5F, -33.0F, 23.0F, 18.0F, 71.0F, new CubeDeformation(0.0F))
            .texOffs(2, 81)
            .addBox(-26.55F, -13.55F, -17.95F, 55.0F, 12.0F, 58.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(0.0F, 14.0F, 3.0F)
      );
      PartDefinition Head_r1 = Head.addOrReplaceChild(
         "Head_r1",
         CubeListBuilder.create()
            .texOffs(218, 0)
            .mirror()
            .addBox(-15.5F, -6.0F, -1.5F, 26.0F, 12.0F, 20.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offsetAndRotation(-16.5F, -7.5F, -27.0F, 0.0F, 0.7854F, 0.0F)
      );
      PartDefinition Head_r2 = Head.addOrReplaceChild(
         "Head_r2",
         CubeListBuilder.create().texOffs(218, 0).addBox(-11.0F, -6.0F, -1.5F, 26.0F, 12.0F, 20.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(18.0F, -7.5F, -27.0F, 0.0F, -0.7854F, 0.0F)
      );
      PartDefinition Antenna = Head.addOrReplaceChild(
         "Antenna",
         CubeListBuilder.create().texOffs(152, 79).addBox(-3.5F, -0.5F, -3.0F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-22.5F, -12.0F, 43.5F)
      );
      PartDefinition Antenna2 = Head.addOrReplaceChild(
         "Antenna2",
         CubeListBuilder.create().texOffs(152, 79).addBox(-3.5F, -0.5F, -3.0F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(25.5F, -12.0F, 43.5F)
      );
      PartDefinition mantlet = Head.addOrReplaceChild(
         "mantlet",
         CubeListBuilder.create()
            .texOffs(228, 68)
            .addBox(-11.0F, -6.5F, -7.5F, 17.0F, 11.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(228, 106)
            .addBox(-7.5F, -1.5F, -15.0F, 3.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, -7.5F, -30.0F)
      );
      PartDefinition Gun = mantlet.addOrReplaceChild(
         "Gun",
         CubeListBuilder.create()
            .texOffs(226, 85)
            .addBox(-2.0F, -3.5F, -63.5F, 7.0F, 7.0F, 15.0F, new CubeDeformation(1.0F))
            .texOffs(218, 29)
            .addBox(-2.0F, -3.5F, -33.5F, 7.0F, 7.0F, 33.0F, new CubeDeformation(1.0F))
            .texOffs(0, 152)
            .addBox(-1.5F, -3.0F, -55.5F, 6.0F, 6.0F, 87.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-1.5F, 0.0F, -39.0F)
      );
      return LayerDefinition.create(meshdefinition, 384, 384);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }
}
