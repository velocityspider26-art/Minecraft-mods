package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class HugeUnboredBarrelItem extends Item {
   public HugeUnboredBarrelItem() {
      super(new Properties().stacksTo(8).rarity(Rarity.COMMON));
   }
}
