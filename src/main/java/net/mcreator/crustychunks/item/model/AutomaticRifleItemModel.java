package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.AutomaticRifleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AutomaticRifleItemModel extends GeoModel<AutomaticRifleItem> {
   public ResourceLocation getAnimationResource(AutomaticRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/fullautorifle.animation.json");
   }

   public ResourceLocation getModelResource(AutomaticRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/fullautorifle.geo.json");
   }

   public ResourceLocation getTextureResource(AutomaticRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/fullautomaticrifle.png");
   }
}
