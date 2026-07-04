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

public class MGBoxScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double Rounds = 0.0;
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getCount() == 0) {
            if (1.0 <= (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
               Rounds = (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0;
               { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
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

               if ((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == 0.0) {
                  if (world instanceof ServerLevel _levelx) {
                     ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.LARGE_BULLET.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _levelx.addFreshEntity(entityToSpawn);
                  }
               } else if ((entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == 1.0) {
                  if (world instanceof ServerLevel _levelx) {
                     ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.EXTRA_LARGE_BULLET.get()));
                     entityToSpawn.setPickUpDelay(10);
                     _levelx.addFreshEntity(entityToSpawn);
                  }
               } else if ((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize")
                     == -1.0
                  && world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.BULLET.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }
         } else if ((entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
               != CrustyChunksModItems.LARGE_BULLET.get()
            || (entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") != 0.0
               && !(0.0 >= (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo"))) {
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
                  != CrustyChunksModItems.EXTRA_LARGE_BULLET.get()
               || (entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") != 1.0
                  && !(0.0 >= (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo"))) {
               if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getItem()
                     == CrustyChunksModItems.BULLET.get()
                  && (
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize")
                           == -1.0
                        || 0.0
                           >= (entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                  )) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("AmmoSize", -1.0));
                  if (400.0
                     > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
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

                     if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .getCount()
                        >= 400.0
                           - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .shrink(
                              (int)(
                                 400.0
                                    - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                              )
                           );
                        Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           + 400.0
                           - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                        { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                     } else if (400.0
                        > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                              .getCount()) {
                        Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                              .getCount();
                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .shrink(
                              (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                                 .getCount()
                           );
                        { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                     }
                  }
               }
            } else {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("AmmoSize", 1.0));
               if (200.0
                  > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
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

                  if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     >= 200.0
                        - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(
                           (int)(
                              200.0
                                 - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                        );
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + 200.0
                        - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  } else if (200.0
                     > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     )
                   {
                     Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                           .getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .shrink(
                           (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                              .getCount()
                        );
                     { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  }
               }
            }
         } else {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("AmmoSize", 0.0));
            if (200.0
               > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
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

               if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                  >= 200.0
                     - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                     .shrink(
                        (int)(
                           200.0
                              - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                        )
                     );
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     + 200.0
                     - (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo");
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               } else if (200.0
                  > (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()) {
                  Rounds = (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                     + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                        .getCount();
                  (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY)
                     .shrink(
                        (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxx.getOffhandItem() : ItemStack.EMPTY).getCount()
                     );
                  { final var _fvcc1 = Rounds; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               }
            }
         }

         if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize")
            == 1.0) {
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal(
                     "XL: "
                        + new DecimalFormat("####.")
                           .format(
                              (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                  ),
                  true
               );
            }

            if (entity instanceof Player _player) {
               _player.getCooldowns()
                  .addCooldown((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 1);
            }
         } else if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize")
            == 0.0) {
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal(
                     "L: "
                        + new DecimalFormat("####.")
                           .format(
                              (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                  ),
                  true
               );
            }

            if (entity instanceof Player _player) {
               _player.getCooldowns()
                  .addCooldown((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 1);
            }
         } else if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize")
            == -1.0) {
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal(
                     "M:"
                        + new DecimalFormat("####.")
                           .format(
                              (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           )
                  ),
                  true
               );
            }

            if (entity instanceof Player _player) {
               _player.getCooldowns()
                  .addCooldown((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 1);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MGBoxScriptProcedure.execute", _wtSafe);
      }
   }
}
