package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.util.Locale;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class PaintToolFireProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, ItemStack itemstack) {
      try {
      String BlockType = "";
      String result = "";
      if (1.0 <= itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid")) {
         if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:steel_armor_blocks")))) {
            if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("slab")) {
               BlockType = "_slab";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("optic")) {
               BlockType = "_optic";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("stairs")) {
               BlockType = "_stairs";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("trapdoor")) {
               BlockType = "_trapdoor";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("steel_layer")) {
               BlockType = "steel_layer";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("crusty_chunks:")) {
               BlockType = "";
            } else {
               BlockType = "NULL";
            }

            if (!itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("")
               && !itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("none")
               && !itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("remove")) {
               result = "crusty_chunks:" + itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color") + "_armor" + BlockType;
            } else if ("steel_layer".equals(BlockType)) {
               result = "warium_extras:" + BlockType + "_" + itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color");
            }

            if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("remove")) {
               if (BlockType.equals("_optic")) {
                  result = "crusty_chunks:steel_optic";
               } else if (BlockType.equals("steel_layer")) {
                  result = "warium_extras:" + BlockType;
               } else {
                  result = "crusty_chunks:steel_plating" + BlockType;
               }
            }

            if (BuiltInRegistries.BLOCK.get(ResourceLocation.parse(result.toLowerCase(Locale.ENGLISH))) != Blocks.AIR) {
               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)BuiltInRegistries.BLOCK.get(ResourceLocation.parse(result.toLowerCase(Locale.ENGLISH)))).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> _be = _bso.getProperties().iterator();

               while (_be.hasNext()) {

                  Property<?> entry_prop = _be.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var20) {
                     }
                  }
               }

               BlockEntity _bex = world.getBlockEntity(_bp);
               CompoundTag _bnbt = null;
               if (_bex != null) {
                  _bnbt = _bex.saveWithFullMetadata(world.registryAccess());
                  _bex.setRemoved();
               }

               world.setBlock(_bp, _bs, 3);
               if (_bnbt != null) {
                  BlockEntity var32 = world.getBlockEntity(_bp);
                  if (var32 != null) {
                     try {
                        var32.loadWithComponents(_bnbt, world.registryAccess());
                     } catch (Exception var19) {
                     }
                  }
               }

               { final var _fvcc1 = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid") - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Fluid", _fvcc1)); }
               if (world instanceof ServerLevel _level) {
                  _level.sendParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.puffer_fish.blow_out")),
                        SoundSource.BLOCKS,
                        1.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 1.0)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.puffer_fish.blow_out")),
                        SoundSource.BLOCKS,
                        1.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 1.0),
                        false
                     );
                  }
               }
            }
         }

         if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:aluminum_armor_blocks")))
            && !BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("_dark_gray")) {
            if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("slab_wing")) {
               BlockType = "slab_wing";
            } else if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:full_armor")))) {
               BlockType = "_plating";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("plating_slab")) {
               BlockType = "_plating_slab";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("side_panel")) {
               BlockType = "_side_panel";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("stairs")) {
               BlockType = "_plating_stairs";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("trapdoor")) {
               BlockType = "_plating_trapdoor";
            } else if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("ac_barrel")) {
               BlockType = "_ac_barrel";
            } else {
               BlockType = "";
            }

            if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("control_surface")) {
               BlockType = "control_surface";
            }

            if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("vertical_control_surface")) {
               BlockType = "vertical_control_surface";
            }

            if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("control_surface_offset_top")) {
               BlockType = "control_surface_offset_top";
            }

            if (BuiltInRegistries.BLOCK.getKey(world.getBlockState(BlockPos.containing(x, y, z)).getBlock()).toString().contains("control_surface_offset_bottom")) {
               BlockType = "control_surface_offset_bottom";
            }

            if (!itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("") && !itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("none")) {
               if (!BlockType.contains("slab_wing") && !BlockType.contains("surface")) {
                  if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("remove")) {
                     result = "crusty_chunks:aluminum" + BlockType;
                     { final var _fvcc1 = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid") - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Fluid", _fvcc1)); }
                  } else {
                     result = "crusty_chunks:aluminum" + BlockType + "_" + itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color");
                     { final var _fvcc1 = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid") - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Fluid", _fvcc1)); }
                  }
               } else if (BlockType.contains("surface")) {
                  if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("remove")) {
                     result = "valkyrien_warium:" + BlockType;
                  } else {
                     result = "valkyrien_warium:" + itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color") + "_" + BlockType;
                  }
               } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("remove")) {
                  result = "valkyrien_warium:" + BlockType;
               } else {
                  result = "valkyrien_warium:" + BlockType + "_" + itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color");
               }

               if (BuiltInRegistries.BLOCK.get(ResourceLocation.parse(result.toLowerCase(Locale.ENGLISH))) != Blocks.AIR) {
                  BlockPos _bp = BlockPos.containing(x, y, z);
                  BlockState _bs = ((Block)BuiltInRegistries.BLOCK.get(ResourceLocation.parse(result.toLowerCase(Locale.ENGLISH)))).defaultBlockState();
                  BlockState _bso = world.getBlockState(_bp);
                  java.util.Iterator<Property<?>> var33 = _bso.getProperties().iterator();

                  while (var33.hasNext()) {

                     Property<?> entry_prop = var33.next();

                     Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                     if (_property != null && _bs.getValue(_property) != null) {
                        try {
                           _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                        } catch (Exception var18) {
                        }
                     }
                  }

                  BlockEntity _bex = world.getBlockEntity(_bp);
                  CompoundTag _bnbtx = null;
                  if (_bex != null) {
                     _bnbtx = _bex.saveWithFullMetadata(world.registryAccess());
                     _bex.setRemoved();
                  }

                  world.setBlock(_bp, _bs, 3);
                  if (_bnbtx != null) {
                     BlockEntity var35 = world.getBlockEntity(_bp);
                     if (var35 != null) {
                        try {
                           var35.loadWithComponents(_bnbtx, world.registryAccess());
                        } catch (Exception var17) {
                        }
                     }
                  }

                  { final var _fvcc1 = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid") - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Fluid", _fvcc1)); }
                  if (world instanceof ServerLevel _levelx) {
                     _levelx.sendParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
                  }

                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.puffer_fish.blow_out")),
                           SoundSource.BLOCKS,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.7, 1.0)
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.puffer_fish.blow_out")),
                           SoundSource.BLOCKS,
                           1.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.7, 1.0),
                           false
                        );
                     }
                  }
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("PaintToolFireProcedure.execute", _wtSafe);
      }
   }
}
