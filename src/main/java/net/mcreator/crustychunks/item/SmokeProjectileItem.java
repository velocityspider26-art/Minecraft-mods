package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class SmokeProjectileItem extends Item {
   public SmokeProjectileItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
   }
}
