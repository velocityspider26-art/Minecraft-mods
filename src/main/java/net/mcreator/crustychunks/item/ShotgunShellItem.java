package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class ShotgunShellItem extends Item {
   public ShotgunShellItem() {
      super(new Properties().stacksTo(32).rarity(Rarity.COMMON));
   }
}
