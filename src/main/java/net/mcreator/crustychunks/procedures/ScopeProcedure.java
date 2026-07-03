package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber({Dist.CLIENT})
public class ScopeProcedure {
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
         execute(provider, entity);
      }
   }

   public static void execute(Entity entity) {
      execute(null, entity);
   }

   private static void execute(@Nullable Event event, Entity entity) {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:firearm")))) {
            if (entity instanceof Player _plrCldCheck3
               && _plrCldCheck3.getCooldowns().isOnCooldown((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getItem())) {
               return;
            }

            if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
               .AimDownSights) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("sight", true));
               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.SCOPED_BREECH_RIFLE.get()) {
                  setFOV(7.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.SCOPED_BOLT_ACTION_RIFLE_ANIMATED.get()) {
                  setFOV(7.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.SINGLE_SHOT_RIFLE.get()) {
                  setFOV(12.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.SEMI_AUTOMATIC_RIFLE_ANIMATED.get()) {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.BURST_RIFLE.get()) {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.BATTLE_RIFLE.get()
                  )
                {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.BOLT_ACTION_RIFLE_ANIMATED.get()) {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.LMG_ANIMATED.get()
                  )
                {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.MACHINE_CARBINE.get()) {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.PUMP_ACTION_SHOTGUN_ANIMATED.get()) {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.BREAK_ACTION_SHOTGUN_ANIMATED.get()) {
                  setFOV(50.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.SEMI_AUTOMATIC_PISTOL_ANIMATED.get()) {
                  setFOV(60.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.AUTO_PISTOL.get()) {
                  setFOV(60.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.REVOLVER_ANIMATED.get()) {
                  setFOV(60.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.SMG_ANIMATED.get()
                  )
                {
                  setFOV(60.0);
               }

               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.BREECH_RIFLE.get()
                  )
                {
                  setFOV(50.0);
               }
            } else {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("sight", false));
            }
         }
      }
   }
}
