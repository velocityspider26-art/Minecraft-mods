package net.mcreator.crustychunks.fluid.types;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import org.joml.Vector3f;

public class DieselFluidType extends FluidType {
   public DieselFluidType() {
      super(
         Properties.create()
            .fallDistanceModifier(0.0F)
            .canExtinguish(true)
            .supportsBoating(true)
            .canHydrate(true)
            .motionScale(0.007)
            .viscosity(750)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
      );
   }

   public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
      consumer.accept(
         new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL_TEXTURE = ResourceLocation.parse("crusty_chunks:block/diesel");
            private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.parse("crusty_chunks:block/diesel");

            public ResourceLocation getStillTexture() {
               return STILL_TEXTURE;
            }

            public ResourceLocation getFlowingTexture() {
               return FLOWING_TEXTURE;
            }

            public Vector3f modifyFogColor(
               Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor
            ) {
               return new Vector3f(0.5764706F, 0.4745098F, 0.30980393F);
            }

            public void modifyFogRender(
               Camera camera, FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape
            ) {
               Entity entity = camera.getEntity();
               Level world = entity.level();
               RenderSystem.setShaderFogShape(FogShape.SPHERE);
               RenderSystem.setShaderFogStart(0.0F);
               RenderSystem.setShaderFogEnd(Math.min(25.0F, renderDistance));
            }
         }
      );
   }
}
