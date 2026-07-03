package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class MechanicalPressItem extends Item {
   public MechanicalPressItem() {
      super(new Properties().durability(4096).rarity(Rarity.COMMON));
   }
}
