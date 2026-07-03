package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.SingleShotRifleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SingleShotRifleItemModel extends GeoModel<SingleShotRifleItem> {
   public ResourceLocation getAnimationResource(SingleShotRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/atrifle.animation.json");
   }

   public ResourceLocation getModelResource(SingleShotRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/atrifle.geo.json");
   }

   public ResourceLocation getTextureResource(SingleShotRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/atrifle.png");
   }
}
