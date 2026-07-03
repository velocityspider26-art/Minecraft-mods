package net.mcreator.crustychunks.entity.model;

import net.mcreator.crustychunks.entity.AssassinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class AssassinModel extends GeoModel<AssassinEntity> {
   public ResourceLocation getAnimationResource(AssassinEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/assassin.animation.json");
   }

   public ResourceLocation getModelResource(AssassinEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/assassin.geo.json");
   }

   public ResourceLocation getTextureResource(AssassinEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/entities/" + entity.getTexture() + ".png");
   }

   public void setCustomAnimations(AssassinEntity animatable, long instanceId, AnimationState animationState) {
      GeoBone head = this.getAnimationProcessor().getBone("Head");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
         head.setRotY(entityData.netHeadYaw() * (float) (Math.PI / 180.0));
      }
   }
}
