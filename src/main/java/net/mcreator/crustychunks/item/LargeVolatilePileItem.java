package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class LargeVolatilePileItem extends Item {
   public LargeVolatilePileItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }
}
