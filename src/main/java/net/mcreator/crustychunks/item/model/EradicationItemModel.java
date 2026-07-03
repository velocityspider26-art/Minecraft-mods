package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.EradicationItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EradicationItemModel extends GeoModel<EradicationItem> {
   public ResourceLocation getAnimationResource(EradicationItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/eradication.animation.json");
   }

   public ResourceLocation getModelResource(EradicationItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/eradication.geo.json");
   }

   public ResourceLocation getTextureResource(EradicationItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/eradicationgun.png");
   }
}
