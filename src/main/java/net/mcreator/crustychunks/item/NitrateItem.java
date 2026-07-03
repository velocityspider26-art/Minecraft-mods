package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class NitrateItem extends Item {
   public NitrateItem() {
      super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
   }
}
