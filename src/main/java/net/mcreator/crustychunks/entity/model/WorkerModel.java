package net.mcreator.crustychunks.entity.model;

import net.mcreator.crustychunks.entity.WorkerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class WorkerModel extends GeoModel<WorkerEntity> {
   public ResourceLocation getAnimationResource(WorkerEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/worker.animation.json");
   }

   public ResourceLocation getModelResource(WorkerEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/worker.geo.json");
   }

   public ResourceLocation getTextureResource(WorkerEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/entities/" + entity.getTexture() + ".png");
   }

   public void setCustomAnimations(WorkerEntity animatable, long instanceId, AnimationState animationState) {
      GeoBone head = this.getAnimationProcessor().getBone("Turret");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
         head.setRotY(entityData.netHeadYaw() * (float) (Math.PI / 180.0));
      }
   }
}
