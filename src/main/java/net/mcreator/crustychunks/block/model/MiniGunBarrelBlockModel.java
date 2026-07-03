package net.mcreator.crustychunks.block.model;

import net.mcreator.crustychunks.block.entity.MiniGunBarrelTileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MiniGunBarrelBlockModel extends GeoModel<MiniGunBarrelTileEntity> {
   public ResourceLocation getAnimationResource(MiniGunBarrelTileEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/minigunbarrel.animation.json");
   }

   public ResourceLocation getModelResource(MiniGunBarrelTileEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/minigunbarrel.geo.json");
   }

   public ResourceLocation getTextureResource(MiniGunBarrelTileEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/block/minigunbarrel.png");
   }
}
