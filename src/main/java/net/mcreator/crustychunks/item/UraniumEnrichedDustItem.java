package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.procedures.Rad05TickItemProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class UraniumEnrichedDustItem extends Item {
   public UraniumEnrichedDustItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }

   public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
      super.inventoryTick(itemstack, world, entity, slot, selected);
      Rad05TickItemProcedure.execute(entity, itemstack);
   }
}
