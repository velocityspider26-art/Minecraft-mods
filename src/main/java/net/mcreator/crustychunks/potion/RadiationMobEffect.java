package net.mcreator.crustychunks.potion;

import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.mcreator.crustychunks.procedures.PlayerRadiationDoseProcedure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class RadiationMobEffect extends MobEffect {
   public RadiationMobEffect() {
      super(MobEffectCategory.HARMFUL, -16777216);
   }
   @Override
   public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures, MobEffectInstance effectInstance) {
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      PlayerRadiationDoseProcedure.execute(entity.level(), entity);
      return true;
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   @SubscribeEvent
   public static void registerMobEffectExtensions(RegisterClientExtensionsEvent event) {
      event.registerMobEffect(new IClientMobEffectExtensions() {
            public boolean isVisibleInInventory(MobEffectInstance effect) {
               return false;
            }

            public boolean renderInventoryText(
               MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset
            ) {
               return false;
            }

            public boolean isVisibleInGui(MobEffectInstance effect) {
               return false;
            }
         }, net.mcreator.crustychunks.init.CrustyChunksModMobEffects.RADIATION);
   }
}
