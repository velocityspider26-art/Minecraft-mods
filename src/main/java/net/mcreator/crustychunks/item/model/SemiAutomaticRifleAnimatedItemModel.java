package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.SemiAutomaticRifleAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SemiAutomaticRifleAnimatedItemModel extends GeoModel<SemiAutomaticRifleAnimatedItem> {
   public ResourceLocation getAnimationResource(SemiAutomaticRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/automaticrifle.animation.json");
   }

   public ResourceLocation getModelResource(SemiAutomaticRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/automaticrifle.geo.json");
   }

   public ResourceLocation getTextureResource(SemiAutomaticRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/artexture.png");
   }
}
