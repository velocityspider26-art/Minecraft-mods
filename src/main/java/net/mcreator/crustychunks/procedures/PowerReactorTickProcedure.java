package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PowerReactorTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getBlockState(BlockPos.containing(x, y - 2.0, z)).getBlock() == CrustyChunksModBlocks.POWER_REACTOR_PORT.get()
         && world.getBlockState(BlockPos.containing(x, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.BREEDER_REACTOR_CORE.get()
         && world.getBlockState(BlockPos.containing(x, y - 1.0, z - 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x, y - 1.0, z + 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x - 2.0, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x + 2.0, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y - 1.0, z - 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y - 1.0, z + 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x - 2.0, y - 1.0, z), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x + 2.0, y - 1.0, z), "Ready")
         && world.getBlockState(BlockPos.containing(x, y, z - 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x, y, z + 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x - 2.0, y, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x + 2.0, y, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z - 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z + 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x - 2.0, y, z), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x + 2.0, y, z), "Ready")) {
         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y - 2.0, z));
         int _amount = 3000;
         if (_ent != null) {
            {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) capability.receiveEnergy(_amount, false);
}
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:turbine")),
                  SoundSource.NEUTRAL,
                  5.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:turbine")),
                  SoundSource.NEUTRAL,
                  5.0F,
                  1.0F,
                  false
               );
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("PowerReactorTickProcedure.execute", _wtSafe);
      }
   }
}
