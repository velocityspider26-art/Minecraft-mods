package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.GrenadeLauncherItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GrenadeLauncherItemModel extends GeoModel<GrenadeLauncherItem> {
   public ResourceLocation getAnimationResource(GrenadeLauncherItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/grenadelauncher.animation.json");
   }

   public ResourceLocation getModelResource(GrenadeLauncherItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/grenadelauncher.geo.json");
   }

   public ResourceLocation getTextureResource(GrenadeLauncherItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/grenadelauncher.png");
   }
}
