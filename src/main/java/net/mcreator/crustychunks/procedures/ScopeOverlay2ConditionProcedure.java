package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ScopeOverlay2ConditionProcedure {
   public static boolean execute(Entity entity) {
      if (entity == null) {
         return false;
      } else {
         boolean Sneaking = false;
         if ((
               !(entity instanceof Player _plrCldCheck1)
                  || !_plrCldCheck1.getCooldowns().isOnCooldown((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getItem())
            )
            && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.SINGLE_SHOT_RIFLE.get()) {
            if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
               .AimDownSights) {
               Sneaking = true;
            } else {
               Sneaking = false;
            }
         }

         return Sneaking;
      }
   }
}
