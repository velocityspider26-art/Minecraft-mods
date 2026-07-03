package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.HandDrillItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HandDrillItemModel extends GeoModel<HandDrillItem> {
   public ResourceLocation getAnimationResource(HandDrillItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/handdrill.animation.json");
   }

   public ResourceLocation getModelResource(HandDrillItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/handdrill.geo.json");
   }

   public ResourceLocation getTextureResource(HandDrillItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/handdrill.png");
   }
}
