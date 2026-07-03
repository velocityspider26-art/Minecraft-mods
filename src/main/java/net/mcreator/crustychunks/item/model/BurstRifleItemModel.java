package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.BurstRifleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BurstRifleItemModel extends GeoModel<BurstRifleItem> {
   public ResourceLocation getAnimationResource(BurstRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/burstrifle.animation.json");
   }

   public ResourceLocation getModelResource(BurstRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/burstrifle.geo.json");
   }

   public ResourceLocation getTextureResource(BurstRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/burstrifle.png");
   }
}
