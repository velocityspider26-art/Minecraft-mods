package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class SmallFlakProjectileItem extends Item {
   public SmallFlakProjectileItem() {
      super(new Properties().stacksTo(8).rarity(Rarity.COMMON));
   }
}
