package net.mcreator.crustychunks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class HugeBarrelFoundryTemplateItem extends Item {
   public HugeBarrelFoundryTemplateItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
   }
}
