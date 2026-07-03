package net.mcreator.crustychunks.block.renderer;

import net.mcreator.crustychunks.block.display.RACBarrelDisplayItem;
import net.mcreator.crustychunks.block.model.RACBarrelDisplayModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RACBarrelDisplayItemRenderer extends GeoItemRenderer<RACBarrelDisplayItem> {
   public RACBarrelDisplayItemRenderer() {
      super(new RACBarrelDisplayModel());
   }

   public RenderType getRenderType(RACBarrelDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucent(this.getTextureLocation(animatable));
   }
}
