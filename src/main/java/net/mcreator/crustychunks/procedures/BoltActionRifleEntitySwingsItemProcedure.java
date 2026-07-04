package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.item.BoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.item.ScopedBoltActionRifleAnimatedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class BoltActionRifleEntitySwingsItemProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("action")) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BoltActionRifleAnimatedItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("action", false));
            if (entity instanceof Player _player) {
               _player.getCooldowns().addCooldown(itemstack.getItem(), 30);
            }

            CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Cooldown", 30.0));
            CrustyChunksMod.queueServerWork(
               10,
               () -> {
                  if (world instanceof Level _level) {
                     if (!_level.isClientSide()) {
                        _level.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt")),
                           SoundSource.NEUTRAL,
                           0.5F,
                           1.0F
                        );
                     } else {
                        _level.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt")),
                           SoundSource.NEUTRAL,
                           0.5F,
                           1.0F,
                           false
                        );
                     }
                  }

                  if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Casing")) {
                     if (world instanceof ServerLevel _levelx) {
                        ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.LARGE_CASING.get()));
                        entityToSpawn.setPickUpDelay(10);
                        _levelx.addFreshEntity(entityToSpawn);
                     }

                     CasingDropProcedure.execute(world, x, y, z);
                     CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("Casing", false));
                  }
               }
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BoltActionRifleEntitySwingsItemProcedure.execute", _wtSafe);
      }
   }
}
