package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.BreechRifleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BreechRifleItemModel extends GeoModel<BreechRifleItem> {
   public ResourceLocation getAnimationResource(BreechRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/breechrifle.animation.json");
   }

   public ResourceLocation getModelResource(BreechRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/breechrifle.geo.json");
   }

   public ResourceLocation getTextureResource(BreechRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/breechrifle.png");
   }
}
