package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.procedures.SteelplateItemIsCraftedsmeltedProcedure;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class AluminumPlateItem extends Item {
   public AluminumPlateItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }

   public void onCraftedBy(ItemStack itemstack, Level world, Player entity) {
      super.onCraftedBy(itemstack, world, entity);
      SteelplateItemIsCraftedsmeltedProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ());
   }
}
