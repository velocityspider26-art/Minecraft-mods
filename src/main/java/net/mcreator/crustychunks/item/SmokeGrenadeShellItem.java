package net.mcreator.crustychunks.item;

import net.mcreator.crustychunks.procedures.MortarOnBlockRightClickedProcedure;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class SmokeGrenadeShellItem extends Item {
   public SmokeGrenadeShellItem() {
      super(new Properties().stacksTo(4).rarity(Rarity.COMMON));
   }

   public InteractionResult useOn(UseOnContext context) {
      super.useOn(context);
      MortarOnBlockRightClickedProcedure.execute(
         context.getLevel(),
         (double)context.getClickedPos().getX(),
         (double)context.getClickedPos().getY(),
         (double)context.getClickedPos().getZ(),
         context.getLevel().getBlockState(context.getClickedPos()),
         context.getPlayer()
      );
      return InteractionResult.SUCCESS;
   }
}
