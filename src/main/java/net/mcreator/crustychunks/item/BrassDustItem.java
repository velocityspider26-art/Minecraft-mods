package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class BrassDustItem extends Item {
   public BrassDustItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }
}
