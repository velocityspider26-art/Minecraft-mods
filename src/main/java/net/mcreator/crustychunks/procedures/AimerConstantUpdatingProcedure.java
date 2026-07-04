package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AimerConstantUpdatingProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         String WeaponAttatched = "";
         boolean Sneaking = false;
         if (!(entity instanceof Player _plrCldCheck1) || !_plrCldCheck1.getCooldowns().isOnCooldown(itemstack.getItem())) {
            if (entity.isShiftKeyDown()) {
               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .LeftKey) {
                  LeftPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
               }

               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .RightKey) {
                  RightPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
               }

               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .UpKey) {
                  UpPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
               }

               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .DownKey) {
                  DownPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
               }
            } else {
               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .LeftKey) {
                  LeftPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown(itemstack.getItem(), 4);
                  }
               }

               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .RightKey) {
                  RightPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown(itemstack.getItem(), 4);
                  }
               }

               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .UpKey) {
                  UpPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown(itemstack.getItem(), 4);
                  }
               }

               if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .DownKey) {
                  DownPullOnKeyPressedProcedure.execute(world, x, y, z, entity);
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown(itemstack.getItem(), 4);
                  }
               }
            }
         }

         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSX") != 0.0 || itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSY") != 0.0 || itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSZ") != 0.0) {
            DirectionUpdateProcedure.execute(world, entity, itemstack);
            if ((
                  entity.getPersistentData().getBoolean("Mode")
                     || entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                        .LeftKey
                     || entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                        .RightKey
                     || entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                        .UpKey
                     || entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                        .DownKey
               )
               && !world.isClientSide()) {
               BlockPos _bp = BlockPos.containing(
                  itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSX"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSY"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSZ")
               );
               BlockEntity _blockEntity = world.getBlockEntity(_bp);
               BlockState _bs = world.getBlockState(_bp);
               if (_blockEntity != null) {
                  _blockEntity.getPersistentData().putDouble("Updated", 1.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bp, _bs, _bs, 3);
               }
            }
         }

         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("ATGMMode")
            && (!(entity instanceof Player _plrCldCheck28) || !_plrCldCheck28.getCooldowns().isOnCooldown(itemstack.getItem()))) {
            CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Yaw", 0.0));
            CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Pitch", 0.0));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AimerConstantUpdatingProcedure.execute", _wtSafe);
      }
   }
}
