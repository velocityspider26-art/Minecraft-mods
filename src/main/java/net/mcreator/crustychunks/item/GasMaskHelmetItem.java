package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.Util;
import java.util.List;
import java.util.EnumMap;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import net.mcreator.crustychunks.client.model.ModelGasMaskHelmet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public abstract class GasMaskHelmetItem extends ArmorItem {
   public static Holder<ArmorMaterial> ARMOR_MATERIAL = null;

   @SubscribeEvent
   public static void registerArmorMaterial(RegisterEvent event) {
      event.register(Registries.ARMOR_MATERIAL, registerHelper -> {
         ArmorMaterial armorMaterial = new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.BOOTS, 0);
            map.put(ArmorItem.Type.LEGGINGS, 0);
            map.put(ArmorItem.Type.CHESTPLATE, 0);
            map.put(ArmorItem.Type.HELMET, 1);
            map.put(ArmorItem.Type.BODY, 0);
         }), 0, BuiltInRegistries.SOUND_EVENT.wrapAsHolder((SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.armor.equip_generic"))), () -> Ingredient.of(), List.of(new ArmorMaterial.Layer(ResourceLocation.parse("crusty_chunks:gas_mask_helmet"))), 0.0f, 0.0f);
         registerHelper.register(ResourceLocation.parse("crusty_chunks:gas_mask_helmet"), armorMaterial);
         ARMOR_MATERIAL = BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(armorMaterial);
      });
   }

   public GasMaskHelmetItem(ArmorItem.Type type, Item.Properties properties) {
      super(ARMOR_MATERIAL, type, properties);
   }

   public static class Helmet extends GasMaskHelmetItem {
      public Helmet() {
         super(Type.HELMET, new Properties().durability(ArmorItem.Type.HELMET.getDurability(25)));
      }

      public void initializeClient(Consumer<IClientItemExtensions> consumer) {
         consumer.accept(
            new IClientItemExtensions() {
               public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                  HumanoidModel armorModel = new HumanoidModel(
                     new ModelPart(
                        Collections.emptyList(),
                        Map.of(
                           "head",
                           (new ModelGasMaskHelmet(Minecraft.getInstance().getEntityModels().bakeLayer(ModelGasMaskHelmet.LAYER_LOCATION))).group,
                           "hat",
                           new ModelPart(Collections.emptyList(), Collections.emptyMap()),
                           "body",
                           new ModelPart(Collections.emptyList(), Collections.emptyMap()),
                           "right_arm",
                           new ModelPart(Collections.emptyList(), Collections.emptyMap()),
                           "left_arm",
                           new ModelPart(Collections.emptyList(), Collections.emptyMap()),
                           "right_leg",
                           new ModelPart(Collections.emptyList(), Collections.emptyMap()),
                           "left_leg",
                           new ModelPart(Collections.emptyList(), Collections.emptyMap())
                        )
                     )
                  );
                  armorModel.crouching = living.isShiftKeyDown();
                  armorModel.riding = defaultModel.riding;
                  armorModel.young = living.isBaby();
                  return armorModel;
               }
            }
         );
      }

      public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
         return ResourceLocation.parse("crusty_chunks:textures/entities/resistanthelmet.png");
      }
   }
}
