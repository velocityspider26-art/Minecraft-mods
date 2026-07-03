package net.mcreator.crustychunks.client.pose;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class RevolverAnimatedItemPose {
   public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
               if (arm == HumanoidArm.LEFT) {
                  model.leftArm.xRot = -1.5F + model.head.xRot;
               } else {
                  model.rightArm.xRot = -1.5F + model.head.xRot;
               }
   });

   private RevolverAnimatedItemPose() {
   }
}
