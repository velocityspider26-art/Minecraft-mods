package net.mcreator.crustychunks.block.renderer;

import net.mcreator.crustychunks.block.display.MiniGunBarrelDisplayItem;
import net.mcreator.crustychunks.block.model.MiniGunBarrelDisplayModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MiniGunBarrelDisplayItemRenderer extends GeoItemRenderer<MiniGunBarrelDisplayItem> {
   public MiniGunBarrelDisplayItemRenderer() {
      super(new MiniGunBarrelDisplayModel());
   }

   public RenderType getRenderType(MiniGunBarrelDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucent(this.getTextureLocation(animatable));
   }
}
