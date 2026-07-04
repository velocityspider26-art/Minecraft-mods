package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class MagazineScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         double Rounds = 0.0;
         double Capacity = 0.0;
         Capacity = 30.0;
         if (0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
            CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("Type", "NULL"));
         }

         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("NULL") || itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("MB")) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (1.0 <= itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.BULLET.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof Level _level) {
                     if (!_level.isClientSide()) {
                        _level.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1)
                        );
                     } else {
                        _level.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1),
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntx ? _livEntx.getOffhandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.BULLET.get()) {
               CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("Type", "MB"));
               if (Capacity > itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1)
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1),
                           false
                        );
                     }
                  }

                  if ((double)(entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     >= Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink((int)(Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")));
                     Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") + Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  } else if (Capacity
                     > itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getOffhandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getOffhandItem() : ItemStack.EMPTY).getCount());
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  }
               }
            }
         }

         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("NULL") || itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("AP")) {
            if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (1.0 <= itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if (world instanceof ServerLevel _levelxx) {
                     ItemEntity entityToSpawn = new ItemEntity(_levelxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.MEDIUM_AP_BULLET.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _levelxx.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof Level _levelxx) {
                     if (!_levelxx.isClientSide()) {
                        _levelxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1)
                        );
                     } else {
                        _levelxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1),
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.MEDIUM_AP_BULLET.get()) {
               CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("Type", "AP"));
               if (Capacity > itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (world instanceof Level _levelxxx) {
                     if (!_levelxxx.isClientSide()) {
                        _levelxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1)
                        );
                     } else {
                        _levelxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1),
                           false
                        );
                     }
                  }

                  if ((double)(entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     >= Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink((int)(Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")));
                     Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") + Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  } else if (Capacity
                     > itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount());
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  }
               }
            }
         }

         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("NULL") || itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("ST")) {
            if ((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (1.0 <= itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if (world instanceof ServerLevel _levelxxxx) {
                     ItemEntity entityToSpawn = new ItemEntity(_levelxxxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.MEDIUM_STEALTH_BULLET.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _levelxxxx.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof Level _levelxxxx) {
                     if (!_levelxxxx.isClientSide()) {
                        _levelxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1)
                        );
                     } else {
                        _levelxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1),
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.MEDIUM_STEALTH_BULLET.get()) {
               CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("Type", "ST"));
               if (Capacity > itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (world instanceof Level _levelxxxxx) {
                     if (!_levelxxxxx.isClientSide()) {
                        _levelxxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1)
                        );
                     } else {
                        _levelxxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.step")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 3.0, 3.1),
                           false
                        );
                     }
                  }

                  if ((double)(entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     >= Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink((int)(Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")));
                     Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") + Capacity - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  } else if (Capacity
                     > itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount());
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  }
               }
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal(
                     new DecimalFormat("§4" + itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type") + "§6####§8/§6").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo"))
                        + new DecimalFormat("####").format(Capacity)
                  ),
                  true
               );
            }
         }

         if (entity instanceof Player _player) {
            _player.getCooldowns().addCooldown(itemstack.getItem(), 1);
         }

         if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(
               Component.literal(
                  new DecimalFormat("§4" + itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type") + "§6####§8/§6").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo"))
                     + new DecimalFormat("####").format(Capacity)
               ),
               true
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MagazineScriptProcedure.execute", _wtSafe);
      }
   }
}
