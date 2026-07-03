package net.mcreator.crustychunks.procedures;

import javax.annotation.Nullable;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber({Dist.CLIENT})
public class WeaponCrossOverlayProcedure {
   public static ComputeFov provider = null;

   public static void setFOV(double fov) {
      provider.setFOV(fov);
   }

   @SubscribeEvent
   public static void computeFOV(ComputeFov event) {
      provider = event;
      ClientLevel level = Minecraft.getInstance().level;
      Entity entity = provider.getCamera().getEntity();
      if (level != null && entity != null) {
         Vec3 entPos = entity.getPosition((float)provider.getPartialTick());
         execute(provider, level, entity);
      }
   }

   public static void execute(LevelAccessor world, Entity entity) {
      execute(null, world, entity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:firearm")))) {
            if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  != CrustyChunksModItems.PUMP_ACTION_SHOTGUN_ANIMATED.get()
               && (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  != CrustyChunksModItems.BREAK_ACTION_SHOTGUN_ANIMATED.get()) {
               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .AimDownSights) {
                  if (world.isClientSide()) {
                     Minecraft.getInstance().getTextureManager().bindForSetup(ResourceLocation.parse("crusty_chunks:textures/screens/iconssneak.png"));
                     Minecraft.getInstance()
                        .getTextureManager()
                        .register(
                           ResourceLocation.parse("minecraft:textures/gui/icons.png"),
                           Minecraft.getInstance().getTextureManager().getTexture(ResourceLocation.parse("crusty_chunks:textures/screens/iconssneak.png"))
                        );
                  }
               } else if (world.isClientSide()) {
                  Minecraft.getInstance().getTextureManager().bindForSetup(ResourceLocation.parse("crusty_chunks:textures/screens/icons.png"));
                  Minecraft.getInstance()
                     .getTextureManager()
                     .register(
                        ResourceLocation.parse("minecraft:textures/gui/icons.png"),
                        Minecraft.getInstance().getTextureManager().getTexture(ResourceLocation.parse("crusty_chunks:textures/screens/icons.png"))
                     );
               }
            } else if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
               .AimDownSights) {
               if (world.isClientSide()) {
                  Minecraft.getInstance().getTextureManager().bindForSetup(ResourceLocation.parse("crusty_chunks:textures/screens/iconshotgunsneak.png"));
                  Minecraft.getInstance()
                     .getTextureManager()
                     .register(
                        ResourceLocation.parse("minecraft:textures/gui/icons.png"),
                        Minecraft.getInstance().getTextureManager().getTexture(ResourceLocation.parse("crusty_chunks:textures/screens/iconshotgunsneak.png"))
                     );
               }
            } else if (world.isClientSide()) {
               Minecraft.getInstance().getTextureManager().bindForSetup(ResourceLocation.parse("crusty_chunks:textures/screens/iconshotgun.png"));
               Minecraft.getInstance()
                  .getTextureManager()
                  .register(
                     ResourceLocation.parse("minecraft:textures/gui/icons.png"),
                     Minecraft.getInstance().getTextureManager().getTexture(ResourceLocation.parse("crusty_chunks:textures/screens/iconshotgun.png"))
                  );
            }
         } else if (world.isClientSide()) {
            Minecraft.getInstance().getTextureManager().bindForSetup(ResourceLocation.parse("crusty_chunks:textures/screens/iconsdefault.png"));
            Minecraft.getInstance()
               .getTextureManager()
               .register(
                  ResourceLocation.parse("minecraft:textures/gui/icons.png"),
                  Minecraft.getInstance().getTextureManager().getTexture(ResourceLocation.parse("crusty_chunks:textures/screens/iconsdefault.png"))
               );
         }
      }
   }
}
