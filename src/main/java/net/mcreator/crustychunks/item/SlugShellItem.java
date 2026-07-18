package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class SlugShellItem extends Item {
   public SlugShellItem() {
      super(new Properties().stacksTo(32).rarity(Rarity.COMMON));
   }
}
