package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ADSOnKeyPressedProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:firearm")))) {
            boolean _setval = !entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
               .AimDownSights;
            {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
               _vars.AimDownSights = _setval;
               _vars.syncPlayerVariables(entity);
            
         }
         } else {
            boolean _setval = false;
            {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
               _vars.AimDownSights = _setval;
               _vars.syncPlayerVariables(entity);
            
         }
         }
      }
   }
}
