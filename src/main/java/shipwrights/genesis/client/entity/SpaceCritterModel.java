package shipwrights.genesis.client.entity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import shipwrights.genesis.content.entity.SpaceCritterEntity;

/** A simple six-ish-legged crawler: body, head, and four animated legs. */
public class SpaceCritterModel extends HierarchicalModel<SpaceCritterEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;

    public SpaceCritterModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -6.0f, -5.0f, 8.0f, 6.0f, 10.0f),
                PartPose.offset(0.0f, 20.0f, 0.0f));
        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 17).addBox(-3.0f, -3.0f, -4.0f, 6.0f, 5.0f, 5.0f),
                PartPose.offset(0.0f, -3.0f, -5.0f));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(36, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f);
        root.addOrReplaceChild("leg_front_left", leg, PartPose.offset(3.0f, 20.0f, -3.0f));
        root.addOrReplaceChild("leg_front_right", leg, PartPose.offset(-3.0f, 20.0f, -3.0f));
        root.addOrReplaceChild("leg_back_left", leg, PartPose.offset(3.0f, 20.0f, 3.0f));
        root.addOrReplaceChild("leg_back_right", leg, PartPose.offset(-3.0f, 20.0f, 3.0f));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SpaceCritterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0f);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0f);

        float swing = Mth.cos(limbSwing * 0.6662f) * 1.0f * limbSwingAmount;
        float swingOpposite = Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.0f * limbSwingAmount;
        this.legFrontLeft.xRot = swing;
        this.legBackRight.xRot = swing;
        this.legFrontRight.xRot = swingOpposite;
        this.legBackLeft.xRot = swingOpposite;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
