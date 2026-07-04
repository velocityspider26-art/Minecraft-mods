package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.item.BattleRifleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class BattleRifleReloadProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
            if ((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Loaded")) {
               if (entity instanceof LivingEntity _entity) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.LARGE_MAGAZINE.get()).copy();
                  _setstack.setCount(1);
                  _entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
                  if (_entity instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               { final var _fvcc1 = (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo"); CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getOffhandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", false));
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", 0.0));
               { final var _fvcc1 = (entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type"); CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getOffhandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", _fvcc1)); }
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "NULL"));
               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _level, x, y + 1.0, z, entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY
                  );
                  entityToSpawn.setPickUpDelay(10);
                  entityToSpawn.setUnlimitedLifetime();
                  _level.addFreshEntity(entityToSpawn);
               }

               CrustyChunksMod.queueServerWork(
                  1,
                  () -> {
                     if (world instanceof Level _level) {
                        if (!_level.isClientSide()) {
                           _level.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:riflemagazine")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _level.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:riflemagazine")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }
                  }
               );
               if (entity instanceof LivingEntity _entityx) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.LARGE_MAGAZINE.get()).copy();
                  _setstack.setCount(0);
                  _entityx.setItemInHand(InteractionHand.OFF_HAND, _setstack);
                  if (_entityx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BattleRifleItem) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "MagOut"));
               }
            }
         } else if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.LARGE_MAGAZINE.get()
            && !(entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Loaded")) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("action", true));
            { final var _fvcc1 = (entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo"); CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
            { final var _fvcc1 = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type"); CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", _fvcc1)); }
            if (entity instanceof LivingEntity _entityxx) {
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.LARGE_MAGAZINE.get()).copy();
               _setstack.setCount(0);
               _entityxx.setItemInHand(InteractionHand.OFF_HAND, _setstack);
               if (_entityxx instanceof Player _player) {
                  _player.getInventory().setChanged();
               }
            }

            CrustyChunksMod.queueServerWork(
               25,
               () -> {
                  if (world instanceof Level _level) {
                     if (!_level.isClientSide()) {
                        _level.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:riflemagazine")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           1.0F
                        );
                     } else {
                        _level.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:riflemagazine")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           1.0F,
                           false
                        );
                     }
                  }
               }
            );
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BattleRifleItem
               )
             {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "MagIn"));
            }
         }

         if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Loaded")
            && (entity instanceof LivingEntity _livEntx ? _livEntx.getOffhandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.LARGE_MAGAZINE.get()
            && entity instanceof Player _player
            && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal("§4Weapon still contains magazine."), true);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BattleRifleReloadProcedure.execute", _wtSafe);
      }
   }
}
