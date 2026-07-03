package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.procedures.CasingDropProcedure;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class LargeCasingItem extends Item {
   public LargeCasingItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }

   public boolean onDroppedByPlayer(ItemStack itemstack, Player entity) {
      CasingDropProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ());
      return true;
   }
}
