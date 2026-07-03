package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class BerylliumDustItem extends Item {
   public BerylliumDustItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }
}
