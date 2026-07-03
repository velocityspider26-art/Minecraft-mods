package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.ArmorPeelerAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ArmorPeelerAnimatedItemModel extends GeoModel<ArmorPeelerAnimatedItem> {
   public ResourceLocation getAnimationResource(ArmorPeelerAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/3darmorpeeler.animation.json");
   }

   public ResourceLocation getModelResource(ArmorPeelerAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/3darmorpeeler.geo.json");
   }

   public ResourceLocation getTextureResource(ArmorPeelerAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/armorpeeler3d.png");
   }
}
