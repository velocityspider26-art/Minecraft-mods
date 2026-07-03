package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class FlareChargeItem extends Item {
   public FlareChargeItem() {
      super(new Properties().stacksTo(16).rarity(Rarity.COMMON));
   }
}
