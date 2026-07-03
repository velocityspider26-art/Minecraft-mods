package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;

public class CrudeOilItem extends BucketItem {
   public CrudeOilItem() {
      super(CrustyChunksModFluids.CRUDE_OIL.get(), new Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.COMMON));
   }
}
