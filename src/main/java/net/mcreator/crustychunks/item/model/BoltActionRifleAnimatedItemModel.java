package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.BoltActionRifleAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BoltActionRifleAnimatedItemModel extends GeoModel<BoltActionRifleAnimatedItem> {
   public ResourceLocation getAnimationResource(BoltActionRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/boltaction.animation.json");
   }

   public ResourceLocation getModelResource(BoltActionRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/boltaction.geo.json");
   }

   public ResourceLocation getTextureResource(BoltActionRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/boltactionrifle.png");
   }
}
