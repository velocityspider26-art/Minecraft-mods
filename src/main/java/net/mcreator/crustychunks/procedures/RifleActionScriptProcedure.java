package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.item.AutomaticRifleItem;
import net.mcreator.crustychunks.item.BattleRifleItem;
import net.mcreator.crustychunks.item.BurstRifleItem;
import net.mcreator.crustychunks.item.EradicationItem;
import net.mcreator.crustychunks.item.LMGAnimatedItem;
import net.mcreator.crustychunks.item.LeverRifleItem;
import net.mcreator.crustychunks.item.MachineCarbineItem;
import net.mcreator.crustychunks.item.SemiAutomaticRifleAnimatedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class RifleActionScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("action")) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof SemiAutomaticRifleAnimatedItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof LMGAnimatedItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BurstRifleItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BattleRifleItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof MachineCarbineItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof LeverRifleItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof EradicationItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof AutomaticRifleItem) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
            }

            CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("action", false));
            if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem() != CrustyChunksModItems.AUTOMATIC_RIFLE.get()
               && (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  != CrustyChunksModItems.SEMI_AUTOMATIC_RIFLE_ANIMATED.get()
               && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() != CrustyChunksModItems.MACHINE_CARBINE.get()) {
               if ((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == CrustyChunksModItems.LEVER_RIFLE.get()) {
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown(itemstack.getItem(), 7);
                  }

                  CrustyChunksMod.queueServerWork(
                     1,
                     () -> {
                        if (world instanceof Level _level) {
                           if (!_level.isClientSide()) {
                              _level.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:leveraction")),
                                 SoundSource.NEUTRAL,
                                 0.3F,
                                 1.0F
                              );
                           } else {
                              _level.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:leveraction")),
                                 SoundSource.NEUTRAL,
                                 0.3F,
                                 1.0F,
                                 false
                              );
                           }
                        }
                     }
                  );
               } else {
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown(itemstack.getItem(), 20);
                  }

                  CrustyChunksMod.queueServerWork(
                     1,
                     () -> {
                        if (world instanceof Level _level) {
                           if (!_level.isClientSide()) {
                              _level.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt3")),
                                 SoundSource.NEUTRAL,
                                 0.3F,
                                 1.2F
                              );
                           } else {
                              _level.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt3")),
                                 SoundSource.NEUTRAL,
                                 0.3F,
                                 1.2F,
                                 false
                              );
                           }
                        }
                     }
                  );
               }
            } else {
               if (entity instanceof Player _player) {
                  _player.getCooldowns().addCooldown(itemstack.getItem(), 20);
               }

               CrustyChunksMod.queueServerWork(
                  1,
                  () -> {
                     if (world instanceof Level _level) {
                        if (!_level.isClientSide()) {
                           _level.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt2")),
                              SoundSource.NEUTRAL,
                              0.3F,
                              1.2F
                           );
                        } else {
                           _level.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt2")),
                              SoundSource.NEUTRAL,
                              0.3F,
                              1.2F,
                              false
                           );
                        }
                     }
                  }
               );
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RifleActionScriptProcedure.execute", _wtSafe);
      }
   }
}
