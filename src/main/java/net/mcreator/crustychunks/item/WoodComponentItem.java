package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.procedures.WoodComponentCraftedProcedure;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class WoodComponentItem extends Item {
   public WoodComponentItem() {
      super(new Properties().stacksTo(16).rarity(Rarity.COMMON));
   }

   public void onCraftedBy(ItemStack itemstack, Level world, Player entity) {
      super.onCraftedBy(itemstack, world, entity);
      WoodComponentCraftedProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ());
   }
}
