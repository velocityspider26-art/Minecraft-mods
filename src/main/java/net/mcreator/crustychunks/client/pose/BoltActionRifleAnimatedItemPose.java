package net.mcreator.crustychunks.client.pose;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class BoltActionRifleAnimatedItemPose {
   public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
               if (arm != HumanoidArm.LEFT) {
                  model.rightArm.xRot = -1.5F + model.head.xRot;
                  model.rightArm.yRot = 0.0F + model.head.yRot;
                  model.leftArm.xRot = -1.5F + model.head.xRot;
                  model.leftArm.yRot = 0.75F + model.head.yRot;
               }
   });

   private BoltActionRifleAnimatedItemPose() {
   }
}
