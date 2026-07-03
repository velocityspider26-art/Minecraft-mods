package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.StealthPistolItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StealthPistolItemModel extends GeoModel<StealthPistolItem> {
   public ResourceLocation getAnimationResource(StealthPistolItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/stealthpistol.animation.json");
   }

   public ResourceLocation getModelResource(StealthPistolItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/stealthpistol.geo.json");
   }

   public ResourceLocation getTextureResource(StealthPistolItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/3dpistol.png");
   }
}
