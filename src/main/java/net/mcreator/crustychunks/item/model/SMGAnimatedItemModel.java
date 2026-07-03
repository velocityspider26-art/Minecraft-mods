package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.SMGAnimatedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SMGAnimatedItemModel extends GeoModel<SMGAnimatedItem> {
   public ResourceLocation getAnimationResource(SMGAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/smg.animation.json");
   }

   public ResourceLocation getModelResource(SMGAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/smg.geo.json");
   }

   public ResourceLocation getTextureResource(SMGAnimatedItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/smgtexture.png");
   }
}
