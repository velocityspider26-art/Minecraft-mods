package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.MusketItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MusketItemModel extends GeoModel<MusketItem> {
   public ResourceLocation getAnimationResource(MusketItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/musket.animation.json");
   }

   public ResourceLocation getModelResource(MusketItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/musket.geo.json");
   }

   public ResourceLocation getTextureResource(MusketItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/musket.png");
   }
}
