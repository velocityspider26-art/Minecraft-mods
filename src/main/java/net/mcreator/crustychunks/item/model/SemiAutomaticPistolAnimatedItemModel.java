package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.SemiAutomaticPistolAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SemiAutomaticPistolAnimatedItemModel extends GeoModel<SemiAutomaticPistolAnimatedItem> {
   public ResourceLocation getAnimationResource(SemiAutomaticPistolAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/pistol.animation.json");
   }

   public ResourceLocation getModelResource(SemiAutomaticPistolAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/pistol.geo.json");
   }

   public ResourceLocation getTextureResource(SemiAutomaticPistolAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/3dpistol.png");
   }
}
