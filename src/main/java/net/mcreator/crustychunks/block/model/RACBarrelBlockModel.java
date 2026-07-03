package net.mcreator.crustychunks.block.model;

import net.mcreator.crustychunks.block.entity.RACBarrelTileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RACBarrelBlockModel extends GeoModel<RACBarrelTileEntity> {
   public ResourceLocation getAnimationResource(RACBarrelTileEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "animations/racbarrel.animation.json");
   }

   public ResourceLocation getModelResource(RACBarrelTileEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geo/racbarrel.geo.json");
   }

   public ResourceLocation getTextureResource(RACBarrelTileEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("crusty_chunks", "textures/block/racbarrel.png");
   }
}
