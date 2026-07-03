package net.mcreator.crustychunks.item.model;

import net.mcreator.crustychunks.item.BattleRifleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BattleRifleItemModel extends GeoModel<BattleRifleItem> {
   public ResourceLocation getAnimationResource(BattleRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/battlerifle.animation.json");
   }

   public ResourceLocation getModelResource(BattleRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/battlerifle.geo.json");
   }

   public ResourceLocation getTextureResource(BattleRifleItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/item/battlerifletexture.png");
   }
}
