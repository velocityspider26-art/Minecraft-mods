package net.mcreator.crustychunks.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class APLargeBulletItem extends Item {
   public APLargeBulletItem() {
      super(new Properties().stacksTo(32).rarity(Rarity.COMMON));
   }

   public void appendHoverText(ItemStack itemstack, net.minecraft.world.item.Item.TooltipContext level, List<Component> list, TooltipFlag flag) {
      super.appendHoverText(itemstack, level, list, flag);
      list.add(Component.translatable("item.crusty_chunks.ap_large_bullet.description_0"));
      list.add(Component.translatable("item.crusty_chunks.ap_large_bullet.description_1"));
   }
}
