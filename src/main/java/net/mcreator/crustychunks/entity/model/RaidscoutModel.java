package net.mcreator.crustychunks.entity.model;

import net.mcreator.crustychunks.entity.RaidscoutEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RaidscoutModel extends GeoModel<RaidscoutEntity> {
   public ResourceLocation getAnimationResource(RaidscoutEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/scout.animation.json");
   }

   public ResourceLocation getModelResource(RaidscoutEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/scout.geo.json");
   }

   public ResourceLocation getTextureResource(RaidscoutEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/entities/" + entity.getTexture() + ".png");
   }
}
