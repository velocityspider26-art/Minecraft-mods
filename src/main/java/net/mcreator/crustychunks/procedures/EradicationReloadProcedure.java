package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.item.EradicationItem;
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

public class EradicationReloadProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double Rounds = 0.0;
         double Capacity = 0.0;
         Capacity = 200.0;
         if (0.0 == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "NULL"));
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("NULL")
            || (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("HP")) {
            if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (1.0 <= (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  Rounds = (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_HOLLOW_POINT_BULLET.get()));
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
            } else if ((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.SMALL_HOLLOW_POINT_BULLET.get()) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "HP"));
               if (Capacity > (entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.15)
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.15),
                           false
                        );
                     }
                  }

                  if ((entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof EradicationItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                  }

                  if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     >= Capacity
                        - (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(
                           (int)(
                              Capacity
                                 - (entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                        );
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + Capacity
                        - (entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  } else if (Capacity
                     > (entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount());
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  }
               }
            }
         }

         if ((entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("NULL")
            || (entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("AP")) {
            if ((entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (1.0 <= (entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     - 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if (world instanceof ServerLevel _levelxx) {
                     ItemEntity entityToSpawn = new ItemEntity(_levelxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALLBULLET.get()));
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
            } else if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.SMALLBULLET.get()) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "AP"));
               if (Capacity
                  > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (world instanceof Level _levelxxx) {
                     if (!_levelxxx.isClientSide()) {
                        _levelxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.15)
                        );
                     } else {
                        _levelxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.15),
                           false
                        );
                     }
                  }

                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof EradicationItem
                     )
                   {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                  }

                  if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     >= Capacity
                        - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(
                           (int)(
                              Capacity
                                 - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                        );
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + Capacity
                        - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  } else if (Capacity
                     > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(
                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                        );
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  }
               }
            }
         }

         if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
               .equals("NULL")
            || (entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
               .equals("ST")) {
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (1.0
                  <= (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     - 1.0;
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  if (world instanceof ServerLevel _levelxxxx) {
                     ItemEntity entityToSpawn = new ItemEntity(_levelxxxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_STEALTH_BULLET.get()));
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
            } else if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.SMALL_STEALTH_BULLET.get()) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Type", "ST"));
               if (Capacity
                  > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (world instanceof Level _levelxxxxx) {
                     if (!_levelxxxxx.isClientSide()) {
                        _levelxxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.15)
                        );
                     } else {
                        _levelxxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.15),
                           false
                        );
                     }
                  }

                  if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof EradicationItem
                     )
                   {
                     CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "Reload"));
                  }

                  if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .getCount()
                     >= Capacity
                        - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(
                           (int)(
                              Capacity
                                 - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx
                                       ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                                       : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                        );
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx
                              ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem()
                              : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + Capacity
                        - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  } else if (Capacity
                     > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .getCount()) {
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(
                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                              .getCount()
                        );
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  }
               }
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal(
                     new DecimalFormat(
                              "§4"
                                 + (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                                 + "§6####§8/§6"
                           )
                           .format(
                              (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                        + new DecimalFormat("####").format(Capacity)
                  ),
                  true
               );
            }
         }

         if (entity instanceof Player _player) {
            _player.getCooldowns()
               .addCooldown((entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 1);
         }

         if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(
               Component.literal(
                  new DecimalFormat(
                           "§4"
                              + (entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type")
                              + "§6####§8/§6"
                        )
                        .format(
                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        )
                     + new DecimalFormat("####").format(Capacity)
               ),
               true
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("EradicationReloadProcedure.execute", _wtSafe);
      }
   }
}
