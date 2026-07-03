package net.mcreator.crustychunks.entity.model;

import net.mcreator.crustychunks.entity.ScoutEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ScoutModel extends GeoModel<ScoutEntity> {
   public ResourceLocation getAnimationResource(ScoutEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/scout.animation.json");
   }

   public ResourceLocation getModelResource(ScoutEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/scout.geo.json");
   }

   public ResourceLocation getTextureResource(ScoutEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/entities/" + entity.getTexture() + ".png");
   }
}
