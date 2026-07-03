package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class PistolReceiverItem extends Item {
   public PistolReceiverItem() {
      super(new Properties().stacksTo(16).rarity(Rarity.COMMON));
   }
}
