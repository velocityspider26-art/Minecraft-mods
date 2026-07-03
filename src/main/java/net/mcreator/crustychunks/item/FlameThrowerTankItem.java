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
import java.util.EnumMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.mcreator.crustychunks.client.model.ModelFlamePack;
import net.mcreator.crustychunks.procedures.AmmoCountFluidProcedure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public abstract class FlameThrowerTankItem extends ArmorItem {
   public static Holder<ArmorMaterial> ARMOR_MATERIAL = null;

   @SubscribeEvent
   public static void registerArmorMaterial(RegisterEvent event) {
      event.register(Registries.ARMOR_MATERIAL, registerHelper -> {
         ArmorMaterial armorMaterial = new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.BOOTS, 0);
            map.put(ArmorItem.Type.LEGGINGS, 0);
            map.put(ArmorItem.Type.CHESTPLATE, 1);
            map.put(ArmorItem.Type.HELMET, 0);
            map.put(ArmorItem.Type.BODY, 1);
         }), 9, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY), () -> Ingredient.of(), List.of(new ArmorMaterial.Layer(ResourceLocation.parse("crusty_chunks:flame_thrower_tank"))), 0.0f, 0.0f);
         registerHelper.register(ResourceLocation.parse("crusty_chunks:flame_thrower_tank"), armorMaterial);
         ARMOR_MATERIAL = BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(armorMaterial);
      });
   }

   public FlameThrowerTankItem(ArmorItem.Type type, Item.Properties properties) {
      super(ARMOR_MATERIAL, type, properties);
   }

   public static class Chestplate extends FlameThrowerTankItem {
      public Chestplate() {
         super(Type.CHESTPLATE, new Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(25)));
      }

      public void initializeClient(Consumer<IClientItemExtensions> consumer) {
         consumer.accept(
            new IClientItemExtensions() {
               @OnlyIn(Dist.CLIENT)
               public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                  HumanoidModel armorModel = new HumanoidModel(
                     new ModelPart(
                        Collections.emptyList(),
                        Map.of(
                           "body",
                           (new ModelFlamePack(Minecraft.getInstance().getEntityModels().bakeLayer(ModelFlamePack.LAYER_LOCATION))).Pack,
                           "left_arm",
                           (new ModelFlamePack(Minecraft.getInstance().getEntityModels().bakeLayer(ModelFlamePack.LAYER_LOCATION))).bone2,
                           "right_arm",
                           (new ModelFlamePack(Minecraft.getInstance().getEntityModels().bakeLayer(ModelFlamePack.LAYER_LOCATION))).bone,
                           "head",
                           new ModelPart(Collections.emptyList(), Collections.emptyMap()),
                           "hat",
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

      public void appendHoverText(ItemStack itemstack, net.minecraft.world.item.Item.TooltipContext level, List<Component> list, TooltipFlag flag) {
         super.appendHoverText(itemstack, level, list, flag);
         Entity entity = itemstack.getEntityRepresentation();
         String hoverText = AmmoCountFluidProcedure.execute(itemstack);
         if (hoverText != null) {
            for (String line : hoverText.split("\n")) {
               list.add(Component.literal(line));
            }
         }
      }

      public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
         return ResourceLocation.parse("crusty_chunks:textures/entities/fuelpack.png");
      }
   }
}
