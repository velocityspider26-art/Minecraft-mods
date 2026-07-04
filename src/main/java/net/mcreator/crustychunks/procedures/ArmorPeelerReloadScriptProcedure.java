package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class ArmorPeelerReloadScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getOffhandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.ARMOR_PEELER_ROCKET.get()
            && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.ARMOR_PEELER_UNLOADED.get()) {
            if (entity instanceof LivingEntity _entity) {
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.ARMOR_PEELER_ANIMATED.get()).copy();
               _setstack.setCount(1);
               _entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
               if (_entity instanceof Player _player) {
                  _player.getInventory().setChanged();
               }
            }

            if (entity instanceof LivingEntity _entityx) {
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.ARMOR_PEELER_ROCKET.get()).copy();
               _setstack.setCount(0);
               _entityx.setItemInHand(InteractionHand.OFF_HAND, _setstack);
               if (_entityx instanceof Player _player) {
                  _player.getInventory().setChanged();
               }
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.crossbow.loading_end")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.crossbow.loading_end")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.5F
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.5F,
                     false
                  );
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ArmorPeelerReloadScriptProcedure.execute", _wtSafe);
      }
   }
}
