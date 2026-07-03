package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.LeverRifleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LeverRifleItemModel extends GeoModel<LeverRifleItem> {
   public ResourceLocation getAnimationResource(LeverRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/leveraction.animation.json");
   }

   public ResourceLocation getModelResource(LeverRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/leveraction.geo.json");
   }

   public ResourceLocation getTextureResource(LeverRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/leverrifle.png");
   }
}
