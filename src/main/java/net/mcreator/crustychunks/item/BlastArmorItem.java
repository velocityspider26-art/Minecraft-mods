package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.Util;
import java.util.List;
import java.util.EnumMap;
import com.google.common.collect.Iterables;
import net.mcreator.crustychunks.procedures.BlastArmorChestplateTickEventProcedure;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public abstract class BlastArmorItem extends ArmorItem {
   public static Holder<ArmorMaterial> ARMOR_MATERIAL = null;

   @SubscribeEvent
   public static void registerArmorMaterial(RegisterEvent event) {
      event.register(Registries.ARMOR_MATERIAL, registerHelper -> {
         ArmorMaterial armorMaterial = new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.BOOTS, 2);
            map.put(ArmorItem.Type.LEGGINGS, 5);
            map.put(ArmorItem.Type.CHESTPLATE, 6);
            map.put(ArmorItem.Type.HELMET, 2);
            map.put(ArmorItem.Type.BODY, 6);
         }), 9, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY), () -> Ingredient.of(), List.of(new ArmorMaterial.Layer(ResourceLocation.parse("crusty_chunks:blast_armor"))), 0.0f, 0.25f);
         registerHelper.register(ResourceLocation.parse("crusty_chunks:blast_armor"), armorMaterial);
         ARMOR_MATERIAL = BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(armorMaterial);
      });
   }

   public BlastArmorItem(ArmorItem.Type type, Item.Properties properties) {
      super(ARMOR_MATERIAL, type, properties);
   }

   public static class Boots extends BlastArmorItem {
      public Boots() {
         super(Type.BOOTS, new Properties().durability(ArmorItem.Type.BOOTS.getDurability(20)));
      }

      public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
         return ResourceLocation.parse("crusty_chunks:textures/models/armor/blastarmor_layer_1.png");
      }
   }

   public static class Chestplate extends BlastArmorItem {
      public Chestplate() {
         super(Type.CHESTPLATE, new Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(20)));
      }

      public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
         return ResourceLocation.parse("crusty_chunks:textures/models/armor/blastarmor_layer_1.png");
      }

      public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
         super.inventoryTick(itemstack, world, entity, slot, selected);
         if (entity instanceof Player player && Iterables.contains(player.getArmorSlots(), itemstack)) {
            BlastArmorChestplateTickEventProcedure.execute(entity);
         }
      }
   }

   public static class Helmet extends BlastArmorItem {
      public Helmet() {
         super(Type.HELMET, new Properties().durability(ArmorItem.Type.HELMET.getDurability(20)));
      }

      public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
         return ResourceLocation.parse("crusty_chunks:textures/models/armor/blastarmor_layer_1.png");
      }
   }

   public static class Leggings extends BlastArmorItem {
      public Leggings() {
         super(Type.LEGGINGS, new Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(20)));
      }

      public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
         return ResourceLocation.parse("crusty_chunks:textures/models/armor/blastarmor_layer_2.png");
      }
   }
}
