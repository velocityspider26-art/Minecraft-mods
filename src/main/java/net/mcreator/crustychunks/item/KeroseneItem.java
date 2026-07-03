package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class KeroseneItem extends BucketItem {
   public KeroseneItem() {
      super(CrustyChunksModFluids.KEROSENE.get(), new Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.COMMON));
   }
}
