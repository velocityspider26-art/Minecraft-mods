package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class HammerItem extends Item {
   public HammerItem() {
      super(new Properties().durability(64).rarity(Rarity.COMMON));
   }

   public boolean hasCraftingRemainingItem() {
      return true;
   }

   public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
      ItemStack retval = new ItemStack(this);
      retval.setDamageValue(itemstack.getDamageValue() + 1);
      return retval.getDamageValue() >= retval.getMaxDamage() ? ItemStack.EMPTY : retval;
   }

   public boolean isRepairable(ItemStack itemstack) {
      return false;
   }
}
