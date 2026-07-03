package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.RevolverAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RevolverAnimatedItemModel extends GeoModel<RevolverAnimatedItem> {
   public ResourceLocation getAnimationResource(RevolverAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/revolver.animation.json");
   }

   public ResourceLocation getModelResource(RevolverAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/revolver.geo.json");
   }

   public ResourceLocation getTextureResource(RevolverAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/3drevolver.png");
   }
}
