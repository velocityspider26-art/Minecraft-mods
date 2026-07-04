package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.item.BreakActionShotgunAnimatedItem;
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

public class BreakActionReloadScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double Rounds = 0.0;
         double Capacity = 0.0;
         Capacity = 2.0;
         if (0.0 == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "NULL"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
            if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C") <= 0.0
               && 1.0 <= (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
               Rounds = (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0;
               { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               if ((entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("BU")) {
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SHOTGUN_SHELL.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }
               } else if ((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                  .equals("AP")) {
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SLUG_SHELL.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }
               } else if ((entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                  .equals("BI")) {
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.BIRD_SHOT.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }
               } else if ((entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                     .equals("DB")
                  && world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.THERMAL_SHELL.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        0.2F,
                        1.5F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        0.2F,
                        1.5F,
                        false
                     );
                  }
               }

               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
               if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BreakActionShotgunAnimatedItem
                  )
                {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
               }
            }
         } else if ((entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               != CrustyChunksModItems.SHOTGUN_SHELL.get()
            || !(entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("NULL")
               && !(entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("BU")) {
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
                  != CrustyChunksModItems.SLUG_SHELL.get()
               || !(entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                     .equals("NULL")
                  && !(entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                     .equals("AP")) {
               if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
                     != CrustyChunksModItems.BIRD_SHOT.get()
                  || !(entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                        .equals("NULL")
                     && !(entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                        .equals("BI")) {
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
                        == CrustyChunksModItems.THERMAL_SHELL.get()
                     && (
                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                              .equals("NULL")
                           || (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                              .equals("DB")
                     )) {
                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C")
                        <= 0.0) {
                        if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                                 .getCount()
                              >= 2
                           && 0.0
                              == (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                           if (world instanceof Level _levelx) {
                              if (!_levelx.isClientSide()) {
                                 _levelx.playSound(
                                    null,
                                    BlockPos.containing(x, y, z),
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F
                                 );
                              } else {
                                 _levelx.playLocalSound(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F,
                                    false
                                 );
                              }
                           }

                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                              .shrink(2);
                           Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx
                                    ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                    : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                              + 2.0;
                           { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                           if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY)
                              .getItem() instanceof BreakActionShotgunAnimatedItem) {
                              CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx
                                    ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                    : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                           }

                           CrustyChunksMod.queueServerWork(
                              37,
                              () -> {
                                 if (world instanceof Level _levelxx) {
                                    if (!_levelxx.isClientSide()) {
                                       _levelxx.playSound(
                                          null,
                                          BlockPos.containing(x, y, z),
                                          (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                          SoundSource.NEUTRAL,
                                          1.0F,
                                          1.5F
                                       );
                                    } else {
                                       _levelxx.playLocalSound(
                                          x,
                                          y,
                                          z,
                                          (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                          SoundSource.NEUTRAL,
                                          1.0F,
                                          1.5F,
                                          false
                                       );
                                    }
                                 }
                              }
                           );
                        } else if (2.0
                           > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                           if (world instanceof Level _levelxx) {
                              if (!_levelxx.isClientSide()) {
                                 _levelxx.playSound(
                                    null,
                                    BlockPos.containing(x, y, z),
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F
                                 );
                              } else {
                                 _levelxx.playLocalSound(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F,
                                    false
                                 );
                              }
                           }

                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                              .shrink(1);
                           Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx
                                    ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                    : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                              + 1.0;
                           { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                           if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY)
                              .getItem() instanceof BreakActionShotgunAnimatedItem) {
                              CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx
                                    ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                    : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                           }

                           CrustyChunksMod.queueServerWork(
                              37,
                              () -> {
                                 if (world instanceof Level _levelxxx) {
                                    if (!_levelxxx.isClientSide()) {
                                       _levelxxx.playSound(
                                          null,
                                          BlockPos.containing(x, y, z),
                                          (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                          SoundSource.NEUTRAL,
                                          1.0F,
                                          1.5F
                                       );
                                    } else {
                                       _levelxxx.playLocalSound(
                                          x,
                                          y,
                                          z,
                                          (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                          SoundSource.NEUTRAL,
                                          1.0F,
                                          1.5F,
                                          false
                                       );
                                    }
                                 }
                              }
                           );
                        }
                     }

                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "DB"));
                  }
               } else {
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C")
                     <= 0.0) {
                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount() >= 2
                        && 0.0
                           == (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                        if (world instanceof Level _levelxxx) {
                           if (!_levelxxx.isClientSide()) {
                              _levelxxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F
                              );
                           } else {
                              _levelxxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F,
                                 false
                              );
                           }
                        }

                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .shrink(2);
                        Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           + 2.0;
                        { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                        if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                           .getItem() instanceof BreakActionShotgunAnimatedItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                        }

                        CrustyChunksMod.queueServerWork(
                           37,
                           () -> {
                              if (world instanceof Level _levelxxxx) {
                                 if (!_levelxxxx.isClientSide()) {
                                    _levelxxxx.playSound(
                                       null,
                                       BlockPos.containing(x, y, z),
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.5F
                                    );
                                 } else {
                                    _levelxxxx.playLocalSound(
                                       x,
                                       y,
                                       z,
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.5F,
                                       false
                                    );
                                 }
                              }
                           }
                        );
                     } else if (2.0
                        > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                        if (world instanceof Level _levelxxxx) {
                           if (!_levelxxxx.isClientSide()) {
                              _levelxxxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F
                              );
                           } else {
                              _levelxxxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F,
                                 false
                              );
                           }
                        }

                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .shrink(1);
                        Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           + 1.0;
                        { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                        if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                           .getItem() instanceof BreakActionShotgunAnimatedItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx
                                 ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                 : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                        }

                        CrustyChunksMod.queueServerWork(
                           37,
                           () -> {
                              if (world instanceof Level _levelxxxxx) {
                                 if (!_levelxxxxx.isClientSide()) {
                                    _levelxxxxx.playSound(
                                       null,
                                       BlockPos.containing(x, y, z),
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.5F
                                    );
                                 } else {
                                    _levelxxxxx.playLocalSound(
                                       x,
                                       y,
                                       z,
                                       (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                       SoundSource.NEUTRAL,
                                       1.0F,
                                       1.5F,
                                       false
                                    );
                                 }
                              }
                           }
                        );
                     }
                  }

                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "BI"));
               }
            } else {
               if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C")
                  <= 0.0) {
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .getCount()
                        >= 2
                     && 0.0
                        == (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                     if (world instanceof Level _levelxxxxx) {
                        if (!_levelxxxxx.isClientSide()) {
                           _levelxxxxx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.5F
                           );
                        } else {
                           _levelxxxxx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.5F,
                              false
                           );
                        }
                     }

                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(2);
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + 2.0;
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY)
                        .getItem() instanceof BreakActionShotgunAnimatedItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                     }

                     CrustyChunksMod.queueServerWork(
                        37,
                        () -> {
                           if (world instanceof Level _levelxxxxxx) {
                              if (!_levelxxxxxx.isClientSide()) {
                                 _levelxxxxxx.playSound(
                                    null,
                                    BlockPos.containing(x, y, z),
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F
                                 );
                              } else {
                                 _levelxxxxxx.playLocalSound(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F,
                                    false
                                 );
                              }
                           }
                        }
                     );
                  } else if (2.0
                     > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                     if (world instanceof Level _levelxxxxxx) {
                        if (!_levelxxxxxx.isClientSide()) {
                           _levelxxxxxx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.5F
                           );
                        } else {
                           _levelxxxxxx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.5F,
                              false
                           );
                        }
                     }

                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(1);
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + 1.0;
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                     if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY)
                        .getItem() instanceof BreakActionShotgunAnimatedItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                     }

                     CrustyChunksMod.queueServerWork(
                        37,
                        () -> {
                           if (world instanceof Level _levelxxxxxxx) {
                              if (!_levelxxxxxxx.isClientSide()) {
                                 _levelxxxxxxx.playSound(
                                    null,
                                    BlockPos.containing(x, y, z),
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F
                                 );
                              } else {
                                 _levelxxxxxxx.playLocalSound(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.5F,
                                    false
                                 );
                              }
                           }
                        }
                     );
                  }
               }

               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "AP"));
            }
         } else {
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C")
               <= 0.0) {
               if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .getCount()
                     >= 2
                  && 0.0
                     == (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                  if (world instanceof Level _levelxxxxxxx) {
                     if (!_levelxxxxxxx.isClientSide()) {
                        _levelxxxxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           1.5F
                        );
                     } else {
                        _levelxxxxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           1.5F,
                           false
                        );
                     }
                  }

                  (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                     .shrink(2);
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     + 2.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY)
                     .getItem() instanceof BreakActionShotgunAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                  }

                  CrustyChunksMod.queueServerWork(
                     37,
                     () -> {
                        if (world instanceof Level _levelxxxxxxxx) {
                           if (!_levelxxxxxxxx.isClientSide()) {
                              _levelxxxxxxxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F
                              );
                           } else {
                              _levelxxxxxxxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F,
                                 false
                              );
                           }
                        }
                     }
                  );
               } else if (2.0
                  > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("C", 40.0));
                  if (world instanceof Level _levelxxxxxxxx) {
                     if (!_levelxxxxxxxx.isClientSide()) {
                        _levelxxxxxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           1.5F
                        );
                     } else {
                        _levelxxxxxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           1.5F,
                           false
                        );
                     }
                  }

                  (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                     .shrink(1);
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     + 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                        : ItemStack.EMPTY)
                     .getItem() instanceof BreakActionShotgunAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                           ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                           : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                  }

                  CrustyChunksMod.queueServerWork(
                     37,
                     () -> {
                        if (world instanceof Level _levelxxxxxxxxx) {
                           if (!_levelxxxxxxxxx.isClientSide()) {
                              _levelxxxxxxxxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F
                              );
                           } else {
                              _levelxxxxxxxxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.close")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.5F,
                                 false
                              );
                           }
                        }
                     }
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "BU"));
         }

         if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(
               Component.literal(
                  new DecimalFormat(
                           "§4"
                              + (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                                    ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                    : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                              + "§6####§8/§6"
                        )
                        .format(
                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        )
                     + new DecimalFormat("####").format(Capacity)
               ),
               true
            );
         }

         if (entity instanceof Player _player) {
            _player.getCooldowns()
               .addCooldown(
                  (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                     .getItem(),
                  1
               );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BreakActionReloadScriptProcedure.execute", _wtSafe);
      }
   }
}
