package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.ScopedBreechRifleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ScopedBreechRifleItemModel extends GeoModel<ScopedBreechRifleItem> {
   public ResourceLocation getAnimationResource(ScopedBreechRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/scopedbreechrifle.animation.json");
   }

   public ResourceLocation getModelResource(ScopedBreechRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/scopedbreechrifle.geo.json");
   }

   public ResourceLocation getTextureResource(ScopedBreechRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/breechrifle.png");
   }
}
