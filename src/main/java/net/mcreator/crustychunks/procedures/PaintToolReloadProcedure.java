package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class PaintToolReloadProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid") <= 1.0) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", false));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "none"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.RED_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "red"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.GREEN_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "green"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.PURPLE_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "purple"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.CYAN_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "cyan"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.LIGHT_GRAY_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxx) {
               if (!_levelxxxx.isClientSide()) {
                  _levelxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "light_gray"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.GRAY_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxx) {
               if (!_levelxxxxx.isClientSide()) {
                  _levelxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "gray"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.PINK_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxx) {
               if (!_levelxxxxxx.isClientSide()) {
                  _levelxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "pink"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.LIME_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxx) {
               if (!_levelxxxxxxx.isClientSide()) {
                  _levelxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "lime"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.YELLOW_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxx) {
               if (!_levelxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "yellow"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.LIGHT_BLUE_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxx) {
               if (!_levelxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "light_blue"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.MAGENTA_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxxx) {
               if (!_levelxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "magenta"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.ORANGE_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "orange"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.BLACK_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "black"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.BROWN_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "brown"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.BLUE_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "blue"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == Items.WHITE_DYE) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "white"));
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.SHALE_OIL.get()) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Loaded", true));
            (entity instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof Level _levelxxxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.empty")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Fluid", 100.0));
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("Color", "remove"));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("PaintToolReloadProcedure.execute", _wtSafe);
      }
   }
}
