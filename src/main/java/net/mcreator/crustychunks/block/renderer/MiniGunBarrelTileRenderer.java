package net.mcreator.crustychunks.block.renderer;

import net.mcreator.crustychunks.block.entity.MiniGunBarrelTileEntity;
import net.mcreator.crustychunks.block.model.MiniGunBarrelBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MiniGunBarrelTileRenderer extends GeoBlockRenderer<MiniGunBarrelTileEntity> {
   public MiniGunBarrelTileRenderer() {
      super(new MiniGunBarrelBlockModel());
   }

   public RenderType getRenderType(MiniGunBarrelTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucent(this.getTextureLocation(animatable));
   }
}
