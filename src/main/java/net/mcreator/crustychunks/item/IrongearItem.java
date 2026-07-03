package net.mcreator.crustychunks.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;

public class IrongearItem extends TieredItem {
   public IrongearItem() {
      super(new Tier() {
         public int getUses() {
            return 100;
         }

         public float getSpeed() {
            return 1.0F;
         }

         public float getAttackDamageBonus() {
            return 2.0F;
         }

         public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
         }

         public int getEnchantmentValue() {
            return 0;
         }

         public Ingredient getRepairIngredient() {
            return Ingredient.of(new ItemStack[]{new ItemStack(Items.IRON_INGOT)});
         }
      }, new Properties().attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID, 3.0, Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(net.minecraft.world.item.Item.BASE_ATTACK_SPEED_ID, -3.0, Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
         .build()));
   }

   public boolean isCorrectToolForDrops(ItemStack _stack, BlockState blockstate) {
      return !blockstate.is(BlockTags.NEEDS_STONE_TOOL) && !blockstate.is(BlockTags.NEEDS_IRON_TOOL) && !blockstate.is(BlockTags.NEEDS_DIAMOND_TOOL);
   }

   public boolean canPerformAction(ItemStack stack, ItemAbility toolAction) {
      return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(toolAction)
         || ItemAbilities.DEFAULT_HOE_ACTIONS.contains(toolAction)
         || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(toolAction)
         || ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(toolAction)
         || ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(toolAction);
   }

   public float getDestroySpeed(ItemStack itemstack, BlockState blockstate) {
      return 1.0F;
   }


   public boolean mineBlock(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
      itemstack.hurtAndBreak(1, entity, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
      return true;
   }

   public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
      itemstack.hurtAndBreak(2, entity, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
      return true;
   }
}
