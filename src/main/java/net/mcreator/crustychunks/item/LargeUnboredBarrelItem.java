package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class LargeUnboredBarrelItem extends Item {
   public LargeUnboredBarrelItem() {
      super(new Properties().stacksTo(8).rarity(Rarity.COMMON));
   }
}
