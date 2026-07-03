package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.item.BoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.item.ScopedBoltActionRifleAnimatedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

public class BoltReloadScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double Rounds = 0.0;
         double Capacity = 0.0;
         if (0.0 == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "NULL"));
         }

         Capacity = 8.0;
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
            if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C") <= 0.0
               && 1.0 <= (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
               Rounds = (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0;
               { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               if ((entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("MB")) {
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.LARGE_BULLET.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }
               } else if ((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                  .equals("AP")) {
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.AP_LARGE_BULLET.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }
               } else if ((entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                     .equals("ST")
                  && world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEALTH_LARGE_BULLET.get()));
                  entityToSpawn.setPickUpDelay(10);
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
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt")),
                              SoundSource.NEUTRAL,
                              0.2F,
                              1.0F
                           );
                        } else {
                           _level.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bolt")),
                              SoundSource.NEUTRAL,
                              0.2F,
                              1.0F,
                              false
                           );
                        }
                     }
                  }
               );
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("action", false));
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 20.0));
               if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BoltActionRifleAnimatedItem
                  )
                {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
               }

               if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof ScopedBoltActionRifleAnimatedItem
                  )
                {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "bolt"));
               }
            }
         } else if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               != CrustyChunksModItems.LARGE_BULLET.get()
            || !(entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("NULL")
               && !(entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("MB")) {
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
                  != CrustyChunksModItems.AP_LARGE_BULLET.get()
               || !(entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                     .equals("NULL")
                  && !(entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                     .equals("AP")) {
               if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
                     == CrustyChunksModItems.STEALTH_LARGE_BULLET.get()
                  && (
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                           .equals("NULL")
                        || (entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                           .equals("ST")
                  )) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "ST"));
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C")
                     <= 0.0) {
                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                           >= 4
                        && 4.0
                           >= (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                        CrustyChunksMod.queueServerWork(
                           5,
                           () -> {
                              if (world instanceof Level _level) {
                                 if (!_level.isClientSide()) {
                                    _level.playSound(
                                       null,
                                       BlockPos.containing(x, y, z),
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.0F
                                    );
                                 } else {
                                    _level.playLocalSound(
                                       x,
                                       y,
                                       z,
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.0F,
                                       false
                                    );
                                 }
                              }
                           }
                        );
                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .shrink(4);
                        Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           + 4.0;
                        { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                        if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                           .getItem() instanceof BoltActionRifleAnimatedItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadClip"));
                        }

                        if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                           .getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadClip"));
                        }
                     } else if (7.0
                        >= (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                        CrustyChunksMod.queueServerWork(
                           5,
                           () -> {
                              if (world instanceof Level _level) {
                                 if (!_level.isClientSide()) {
                                    _level.playSound(
                                       null,
                                       BlockPos.containing(x, y, z),
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.0F
                                    );
                                 } else {
                                    _level.playLocalSound(
                                       x,
                                       y,
                                       z,
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.0F,
                                       false
                                    );
                                 }
                              }
                           }
                        );
                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .shrink(1);
                        Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           + 1.0;
                        { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                        if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY)
                           .getItem() instanceof BoltActionRifleAnimatedItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadSingle"));
                        }

                        if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY)
                           .getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadSingle"));
                        }
                     }
                  }
               }
            } else {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "AP"));
               if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C")
                  <= 0.0) {
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .getCount()
                        >= 4
                     && 4.0
                        >= (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                     CrustyChunksMod.queueServerWork(
                        5,
                        () -> {
                           if (world instanceof Level _level) {
                              if (!_level.isClientSide()) {
                                 _level.playSound(
                                    null,
                                    BlockPos.containing(x, y, z),
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _level.playLocalSound(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }
                        }
                     );
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem()
                           : ItemStack.EMPTY)
                        .shrink(4);
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + 4.0;
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY)
                        .getItem() instanceof BoltActionRifleAnimatedItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadClip"));
                     }

                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY)
                        .getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadClip"));
                     }
                  } else if (7.0
                     >= (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                     CrustyChunksMod.queueServerWork(
                        5,
                        () -> {
                           if (world instanceof Level _level) {
                              if (!_level.isClientSide()) {
                                 _level.playSound(
                                    null,
                                    BlockPos.containing(x, y, z),
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _level.playLocalSound(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }
                        }
                     );
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem()
                           : ItemStack.EMPTY)
                        .shrink(1);
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + 1.0;
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY)
                        .getItem() instanceof BoltActionRifleAnimatedItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadSingle"));
                     }

                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY)
                        .getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadSingle"));
                     }
                  }
               }
            }
         } else {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "MB"));
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C")
               <= 0.0) {
               if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem()
                           : ItemStack.EMPTY)
                        .getCount()
                     >= 4
                  && 4.0
                     >= (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                  CrustyChunksMod.queueServerWork(
                     5,
                     () -> {
                        if (world instanceof Level _level) {
                           if (!_level.isClientSide()) {
                              _level.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F
                              );
                           } else {
                              _level.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }
                     }
                  );
                  (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem()
                        : ItemStack.EMPTY)
                     .shrink(4);
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     + 4.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY)
                     .getItem() instanceof BoltActionRifleAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadClip"));
                  }

                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY)
                     .getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadClip"));
                  }
               } else if (7.0
                  >= (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                  CrustyChunksMod.queueServerWork(
                     5,
                     () -> {
                        if (world instanceof Level _level) {
                           if (!_level.isClientSide()) {
                              _level.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F
                              );
                           } else {
                              _level.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }
                     }
                  );
                  (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem()
                        : ItemStack.EMPTY)
                     .shrink(1);
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     + 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY)
                     .getItem() instanceof BoltActionRifleAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadSingle"));
                  }

                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY)
                     .getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "ReloadSingle"));
                  }
               }
            }
         }

         if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(
               Component.literal(
                  new DecimalFormat(
                           "§4"
                              + (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                                    ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                    : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                              + "§6####§8/§6"
                        )
                        .format(
                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        )
                     + new DecimalFormat("####").format(Capacity)
               ),
               true
            );
         }

         if (entity instanceof Player _player) {
            _player.getCooldowns()
               .addCooldown((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 1);
         }
      }
   }
}
