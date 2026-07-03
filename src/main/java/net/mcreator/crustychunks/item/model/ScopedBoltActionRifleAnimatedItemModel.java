package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.ScopedBoltActionRifleAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ScopedBoltActionRifleAnimatedItemModel extends GeoModel<ScopedBoltActionRifleAnimatedItem> {
   public ResourceLocation getAnimationResource(ScopedBoltActionRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/sniper.animation.json");
   }

   public ResourceLocation getModelResource(ScopedBoltActionRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/sniper.geo.json");
   }

   public ResourceLocation getTextureResource(ScopedBoltActionRifleAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/boltactionrifle.png");
   }
}
