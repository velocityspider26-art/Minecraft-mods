package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class FuelRodsLoadProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (CrustyChunksModItems.FUEL_ROD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            && CrustyChunksModBlocks.FUEL_RODS_4.get() != world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
            if (CrustyChunksModBlocks.FUEL_RODS_3.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
               world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FUEL_RODS_4.get()).defaultBlockState(), 3);
            }

            if (CrustyChunksModBlocks.FUEL_RODS_2.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
               world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FUEL_RODS_3.get()).defaultBlockState(), 3);
            }

            if (CrustyChunksModBlocks.FUEL_RODS_1.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
               world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FUEL_RODS_2.get()).defaultBlockState(), 3);
            }

            if (CrustyChunksModBlocks.EMPTY_FUEL_RODS.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
               world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FUEL_RODS_1.get()).defaultBlockState(), 3);
            }

            if (entity instanceof LivingEntity _entity) {
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.FUEL_ROD.get()).copy();
               _setstack.setCount((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getCount() - 1);
               _entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
               if (_entity instanceof Player _player) {
                  _player.getInventory().setChanged();
               }
            }

            CrustyChunksMod.queueServerWork(
               10,
               () -> {
                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lever.click")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.4F
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lever.click")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.4F,
                           false
                        );
                     }
                  }
               }
            );
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }
         } else if (ItemStack.EMPTY.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            && CrustyChunksModBlocks.EMPTY_FUEL_RODS.get() != world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
            FuelRodsDepleteProcedure.execute(world, x, y, z);
            if (world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x + 1.0, y + 1.0, z + 1.0, new ItemStack((ItemLike)CrustyChunksModItems.FUEL_ROD.get()));
               entityToSpawn.setPickUpDelay(5);
               _levelx.addFreshEntity(entityToSpawn);
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lever.click")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lever.click")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.3F
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.3F,
                     false
                  );
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FuelRodsLoadProcedure.execute", _wtSafe);
      }
   }
}
