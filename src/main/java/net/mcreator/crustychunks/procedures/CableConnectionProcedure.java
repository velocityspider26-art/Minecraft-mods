package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CableConnectionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         if (0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX")
            && 0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY")
            && 0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
            && world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ENERGY_NODE.get()) {
            { final var _fvcc1 = x; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedX", _fvcc1)); }
            { final var _fvcc1 = y; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedY", _fvcc1)); }
            { final var _fvcc1 = z; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedZ", _fvcc1)); }
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(Component.literal("§6Position 1 Selected!"), true);
            }

            if (world instanceof ServerLevel _level) {
               _level.sendParticles(ParticleTypes.SCRAPE, x + 0.5, y + 0.5, z + 0.5, 25, 0.25, 0.25, 0.25, 0.0);
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ENERGY_NODE.get()) {
            if (25.0 >= Math.abs(x - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"))
               && 25.0 >= Math.abs(y - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"))
               && 25.0 >= Math.abs(z - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ"))) {
               if (!world.isClientSide()) {
                  BlockPos _bp = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntity = world.getBlockEntity(_bp);
                  BlockState _bs = world.getBlockState(_bp);
                  if (_blockEntity != null) {
                     _blockEntity.getPersistentData().putDouble("PowerX", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"));
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                  BlockState _bsx = world.getBlockState(_bpx);
                  if (_blockEntityx != null) {
                     _blockEntityx.getPersistentData().putDouble("PowerY", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"));
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                  BlockState _bsxx = world.getBlockState(_bpxx);
                  if (_blockEntityxx != null) {
                     _blockEntityxx.getPersistentData().putDouble("PowerZ", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ"));
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxx = BlockPos.containing(
                     itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
                  );
                  BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                  BlockState _bsxxx = world.getBlockState(_bpxxx);
                  if (_blockEntityxxx != null) {
                     _blockEntityxxx.getPersistentData().putDouble("PowerX", x);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxx = BlockPos.containing(
                     itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
                  );
                  BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
                  BlockState _bsxxxx = world.getBlockState(_bpxxxx);
                  if (_blockEntityxxxx != null) {
                     _blockEntityxxxx.getPersistentData().putDouble("PowerY", y);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxx = BlockPos.containing(
                     itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"), itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
                  );
                  BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
                  BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
                  if (_blockEntityxxxxx != null) {
                     _blockEntityxxxxx.getPersistentData().putDouble("PowerZ", z);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
                  }
               }

               CrustyChunksMod.queueServerWork(
                  1,
                  () -> {
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
                        BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
                        if (_blockEntityxxxxxx != null) {
                           _blockEntityxxxxxx.getPersistentData().putDouble("PowerX", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"));
                        }

                        if (world instanceof Level _levelxxxxxx) {
                           _levelxxxxxx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
                        }
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bpx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpx);
                        BlockState _bsxxxxxxx = world.getBlockState(_bpx);
                        if (_blockEntityxxxxxxx != null) {
                           _blockEntityxxxxxxx.getPersistentData().putDouble("PowerY", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"));
                        }

                        if (world instanceof Level _levelx) {
                           _levelx.sendBlockUpdated(_bpx, _bsxxxxxxx, _bsxxxxxxx, 3);
                        }
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bpxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxx);
                        BlockState _bsxxxxxxxx = world.getBlockState(_bpxx);
                        if (_blockEntityxxxxxxxx != null) {
                           _blockEntityxxxxxxxx.getPersistentData().putDouble("PowerZ", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ"));
                        }

                        if (world instanceof Level _levelxxxx) {
                           _levelxxxx.sendBlockUpdated(_bpxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
                        }
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bpxxx = BlockPos.containing(
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"),
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"),
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
                        );
                        BlockEntity _blockEntityxxxxxxxxx = world.getBlockEntity(_bpxxx);
                        BlockState _bsxxxxxxxxx = world.getBlockState(_bpxxx);
                        if (_blockEntityxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxx.getPersistentData().putDouble("PowerX", x);
                        }

                        if (world instanceof Level _levelxxxxxxxx) {
                           _levelxxxxxxxx.sendBlockUpdated(_bpxxx, _bsxxxxxxxxx, _bsxxxxxxxxx, 3);
                        }
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bpxxxx = BlockPos.containing(
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"),
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"),
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
                        );
                        BlockEntity _blockEntityxxxxxxxxxx = world.getBlockEntity(_bpxxxx);
                        BlockState _bsxxxxxxxxxx = world.getBlockState(_bpxxxx);
                        if (_blockEntityxxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxxx.getPersistentData().putDouble("PowerY", y);
                        }

                        if (world instanceof Level _levelxxx) {
                           _levelxxx.sendBlockUpdated(_bpxxxx, _bsxxxxxxxxxx, _bsxxxxxxxxxx, 3);
                        }
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxx = BlockPos.containing(
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"),
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"),
                           itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
                        );
                        BlockEntity _blockEntityxxxxxxxxxxx = world.getBlockEntity(_bpxxxxx);
                        BlockState _bsxxxxxxxxxxx = world.getBlockState(_bpxxxxx);
                        if (_blockEntityxxxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxxxx.getPersistentData().putDouble("PowerZ", z);
                        }

                        if (world instanceof Level _levelxxxxxxx) {
                           _levelxxxxxxx.sendBlockUpdated(_bpxxxxx, _bsxxxxxxxxxxx, _bsxxxxxxxxxxx, 3);
                        }
                     }

                     if (entity instanceof Player _playerx && !_playerx.level().isClientSide()) {
                        _playerx.displayClientMessage(Component.literal("§6Link Succesful!"), true);
                     }

                     if (world instanceof ServerLevel _levelxx) {
                        _levelxx.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + 0.5, y + 0.5, z + 0.5, 25, 0.25, 0.25, 0.25, 0.0);
                     }

                     if (world instanceof Level _levelxxxxx) {
                        if (!_levelxxxxx.isClientSide()) {
                           _levelxxxxx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lever.click")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelxxxxx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lever.click")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedX", 0.0));
                     CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedY", 0.0));
                     CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedZ", 0.0));
                     itemstack.shrink(1);
                  }
               );
            } else {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        1.0F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if (entity instanceof Player _player && !_player.level().isClientSide()) {
                  _player.displayClientMessage(Component.literal("§6Link Failed: Out of Range."), true);
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("CableConnectionProcedure.execute", _wtSafe);
      }
   }
}
