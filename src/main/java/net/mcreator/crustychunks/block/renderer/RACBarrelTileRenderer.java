package net.mcreator.crustychunks.block.renderer;

import net.mcreator.crustychunks.block.entity.RACBarrelTileEntity;
import net.mcreator.crustychunks.block.model.RACBarrelBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class RACBarrelTileRenderer extends GeoBlockRenderer<RACBarrelTileEntity> {
   public RACBarrelTileRenderer() {
      super(new RACBarrelBlockModel());
   }

   public RenderType getRenderType(RACBarrelTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucent(this.getTextureLocation(animatable));
   }
}
