package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.PumpActionShotgunAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PumpActionShotgunAnimatedItemModel extends GeoModel<PumpActionShotgunAnimatedItem> {
   public ResourceLocation getAnimationResource(PumpActionShotgunAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/3dshotgun.animation.json");
   }

   public ResourceLocation getModelResource(PumpActionShotgunAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/3dshotgun.geo.json");
   }

   public ResourceLocation getTextureResource(PumpActionShotgunAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/3dshotgun.png");
   }
}
