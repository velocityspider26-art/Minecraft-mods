package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.procedures.CuttersRightclickedOnBlockProcedure;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class CuttersItem extends Item {
   public CuttersItem() {
      super(new Properties().durability(64).rarity(Rarity.COMMON));
   }

   public boolean hasCraftingRemainingItem() {
      return true;
   }

   public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
      ItemStack retval = new ItemStack(this);
      retval.setDamageValue(itemstack.getDamageValue() + 1);
      return retval.getDamageValue() >= retval.getMaxDamage() ? ItemStack.EMPTY : retval;
   }

   public boolean isRepairable(ItemStack itemstack) {
      return false;
   }

   public InteractionResult useOn(UseOnContext context) {
      super.useOn(context);
      CuttersRightclickedOnBlockProcedure.execute(
         context.getLevel(),
         (double)context.getClickedPos().getX(),
         (double)context.getClickedPos().getY(),
         (double)context.getClickedPos().getZ(),
         context.getItemInHand()
      );
      return InteractionResult.SUCCESS;
   }
}
