package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.FlarePistolItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FlarePistolItemModel extends GeoModel<FlarePistolItem> {
   public ResourceLocation getAnimationResource(FlarePistolItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/flarepistol.animation.json");
   }

   public ResourceLocation getModelResource(FlarePistolItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/flarepistol.geo.json");
   }

   public ResourceLocation getTextureResource(FlarePistolItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/flarepistol.png");
   }
}
