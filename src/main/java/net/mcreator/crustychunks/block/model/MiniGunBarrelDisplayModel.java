package net.mcreator.crustychunks.block.model;

import net.mcreator.crustychunks.block.display.MiniGunBarrelDisplayItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MiniGunBarrelDisplayModel extends GeoModel<MiniGunBarrelDisplayItem> {
   public ResourceLocation getAnimationResource(MiniGunBarrelDisplayItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/minigunbarrel.animation.json");
   }

   public ResourceLocation getModelResource(MiniGunBarrelDisplayItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/minigunbarrel.geo.json");
   }

   public ResourceLocation getTextureResource(MiniGunBarrelDisplayItem entity) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/block/minigunbarrel.png");
   }
}
