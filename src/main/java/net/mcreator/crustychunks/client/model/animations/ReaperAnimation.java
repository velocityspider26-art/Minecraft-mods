package net.mcreator.crustychunks.client.model.animations;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationChannel.Interpolations;
import net.minecraft.client.animation.AnimationChannel.Targets;
import net.minecraft.client.animation.AnimationDefinition.Builder;

public class ReaperAnimation {
   public static final AnimationDefinition idle = Builder.withLength(1.0F).looping().build();
   public static final AnimationDefinition Walk = Builder.withLength(2.0F)
      .looping()
      .addAnimation(
         "Reaper",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), Interpolations.LINEAR),
               new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition Death = Builder.withLength(2.0F)
      .looping()
      .addAnimation(
         "Reaper",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 15.0F, 180.0F), Interpolations.LINEAR),
               new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 360.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
}
