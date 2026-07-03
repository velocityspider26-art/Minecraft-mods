package net.mcreator.crustychunks.client.renderer;

import net.mcreator.crustychunks.client.model.ModelPassengerSeat_Converted;
import net.mcreator.crustychunks.entity.SeatEntityEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class SeatEntityRenderer extends MobRenderer<SeatEntityEntity, ModelPassengerSeat_Converted<SeatEntityEntity>> {
   public SeatEntityRenderer(Context context) {
      super(context, new ModelPassengerSeat_Converted(context.bakeLayer(ModelPassengerSeat_Converted.LAYER_LOCATION)), 0.0F);
   }

   public ResourceLocation getTextureLocation(SeatEntityEntity entity) {
      return ResourceLocation.parse("crusty_chunks:textures/entities/invisentity.png");
   }
}
