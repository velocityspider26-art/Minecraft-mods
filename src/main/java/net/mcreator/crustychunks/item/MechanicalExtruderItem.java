package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class MechanicalExtruderItem extends Item {
   public MechanicalExtruderItem() {
      super(new Properties().durability(4096).rarity(Rarity.COMMON));
   }
}
