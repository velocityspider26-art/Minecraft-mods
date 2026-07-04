package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ReactionChamberTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      boolean Ready = false;
      Ready = false;
      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") <= 0.0) {
         if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:fuelrod")))) {
            FuelRodsDepleteProcedure.execute(world, x, y + 1.0, z);
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.extinguish_fire")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     2.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.extinguish_fire")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     2.0F,
                     false
                  );
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockEntity _blockEntity = world.getBlockEntity(_bp);
               BlockState _bs = world.getBlockState(_bp);
               if (_blockEntity != null) {
                  _blockEntity.getPersistentData().putDouble("Cooldown", 24000.0);
               }

               if (world instanceof Level _levelx) {
                  _levelx.sendBlockUpdated(_bp, _bs, _bs, 3);
               }
            }
         }
      } else if (!world.isClientSide()) {
         BlockPos _bpx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
         BlockState _bsx = world.getBlockState(_bpx);
         if (_blockEntityx != null) {
            _blockEntityx.getPersistentData().putDouble("Cooldown", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Cooldown") - 1.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z + 1.0)).getBlock() != CrustyChunksModBlocks.CONTROL_ROD.get()
         || world.getBlockState(BlockPos.containing(x, y, z - 1.0)).getBlock() != CrustyChunksModBlocks.CONTROL_ROD.get()
         || world.getBlockState(BlockPos.containing(x + 1.0, y, z)).getBlock() != CrustyChunksModBlocks.CONTROL_ROD.get()
         || world.getBlockState(BlockPos.containing(x - 1.0, y, z)).getBlock() != CrustyChunksModBlocks.CONTROL_ROD.get()
         || world.getBlockState(BlockPos.containing(x + 1.0, y, z + 1.0)).getBlock() != CrustyChunksModBlocks.REACTOR_CASING.get()
         || world.getBlockState(BlockPos.containing(x - 1.0, y, z - 1.0)).getBlock() != CrustyChunksModBlocks.REACTOR_CASING.get()
         || world.getBlockState(BlockPos.containing(x + 1.0, y, z - 1.0)).getBlock() != CrustyChunksModBlocks.REACTOR_CASING.get()
         || world.getBlockState(BlockPos.containing(x - 1.0, y, z + 1.0)).getBlock() != CrustyChunksModBlocks.REACTOR_CASING.get()) {
         Ready = false;
      } else if (world.getBlockState(BlockPos.containing(x, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()) {
         if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:fuelrod")))) {
            Ready = true;
         } else {
            Ready = false;
         }
      } else if (!world.getBlockState(BlockPos.containing(x, y + 1.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:fuelrod")))
         && world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() != CrustyChunksModBlocks.REACTION_CHAMBER.get()) {
         Ready = false;
      } else {
         Ready = true;
      }

      if (Ready && world instanceof ServerLevel _levelx) {
         _levelx.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x + 0.5, y + 0.7, z + 0.5, 8, 0.2, 0.4, 0.2, 0.01);
      }

      if (!world.isClientSide()) {
         BlockPos _bpxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
         BlockState _bsxx = world.getBlockState(_bpxx);
         if (_blockEntityxx != null) {
            _blockEntityxx.getPersistentData().putBoolean("Ready", Ready);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ReactionChamberTickProcedure.execute", _wtSafe);
      }
   }
}
