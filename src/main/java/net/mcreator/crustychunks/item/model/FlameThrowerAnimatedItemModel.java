package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.FlameThrowerAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FlameThrowerAnimatedItemModel extends GeoModel<FlameThrowerAnimatedItem> {
   public ResourceLocation getAnimationResource(FlameThrowerAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/flamethrower.animation.json");
   }

   public ResourceLocation getModelResource(FlameThrowerAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/flamethrower.geo.json");
   }

   public ResourceLocation getTextureResource(FlameThrowerAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/3dflamethrower.png");
   }
}
