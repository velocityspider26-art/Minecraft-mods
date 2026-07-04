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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HoseConnectionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         if (0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX")
            && 0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY")
            && 0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ")
            && world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:engines")))) {
            { final var _fvcc1 = x; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedX", _fvcc1)); }
            { final var _fvcc1 = y; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedY", _fvcc1)); }
            { final var _fvcc1 = z; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("SelectedZ", _fvcc1)); }
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(Component.literal("§6Position 1 Selected!"), true);
            }

            if (world instanceof ServerLevel _level) {
               _level.sendParticles(ParticleTypes.SCRAPE, x + 0.5, y + 0.5, z + 0.5, 25, 0.25, 0.25, 0.25, 0.0);
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.FUEL_TANK.get()) {
            if (25.0 >= Math.abs(x - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"))
               && 25.0 >= Math.abs(y - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"))
               && 25.0 >= Math.abs(z - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ"))) {
               CrustyChunksMod.queueServerWork(
                  1,
                  () -> {
                     if (!world.isClientSide()) {
                        BlockPos _bp = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntity = world.getBlockEntity(_bp);
                        BlockState _bs = world.getBlockState(_bp);
                        if (_blockEntity != null) {
                           _blockEntity.getPersistentData().putDouble("ConnectionX", x - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedX"));
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
                           _blockEntityx.getPersistentData().putDouble("ConnectionY", y - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedY"));
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
                           _blockEntityxx.getPersistentData().putDouble("ConnectionZ", z - itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("SelectedZ"));
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                        }
                     }

                     if (entity instanceof Player _playerx && !_playerx.level().isClientSide()) {
                        _playerx.displayClientMessage(Component.literal("§6Link Succesful!"), true);
                     }

                     if (world instanceof ServerLevel _level) {
                        _level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + 0.5, y + 0.5, z + 0.5, 25, 0.25, 0.25, 0.25, 0.0);
                     }

                     if (world instanceof Level _level) {
                        if (!_level.isClientSide()) {
                           _level.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lever.click")),
                              SoundSource.NEUTRAL,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _level.playLocalSound(
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
         net.mcreator.crustychunks.compat.WariumSafety.report("HoseConnectionProcedure.execute", _wtSafe);
      }
   }
}
