package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class EnrichedLithiumNuggetItem extends Item {
   public EnrichedLithiumNuggetItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }
}
