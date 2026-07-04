package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map.Entry;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;

public class MassBurnBlockProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      CleaningProcedureProcedure.execute(world, x, y, z);
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.GRASS_BLOCK && !ModList.get().isLoaded("burnt")) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.BURNTGRASS.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<java.util.Map.Entry<Property<?>, Comparable<?>>> var10 = _bso.getValues().entrySet().iterator();

         while (var10.hasNext()) {

            java.util.Map.Entry<Property<?>, Comparable<?>> entry = var10.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, (Comparable) entry.getValue());
               } catch (Exception var18) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
         if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == Blocks.AIR) {
            _bp = BlockPos.containing(x, y + 1.0, z);
            _bs = Blocks.FIRE.defaultBlockState();
            _bso = world.getBlockState(_bp);
            var10 = _bso.getValues().entrySet().iterator();

            while (var10.hasNext()) {
               Entry<Property<?>, Comparable<?>> entry = (Entry<Property<?>, Comparable<?>>)var10.next();
               Property _property = _bs.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
               if (_property != null && _bs.getValue(_property) != null) {
                  try {
                     _bs = (BlockState)_bs.setValue(_property, (Comparable) entry.getValue());
                  } catch (Exception var17) {
                  }
               }
            }

            world.setBlock(_bp, _bs, 3);
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:burnable")))
         && !ModList.get().isLoaded("burnt")) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.CHARRED_BLOCK.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<java.util.Map.Entry<Property<?>, Comparable<?>>> var32 = _bso.getValues().entrySet().iterator();

         while (var32.hasNext()) {

            java.util.Map.Entry<Property<?>, Comparable<?>> entry = var32.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, (Comparable) entry.getValue());
               } catch (Exception var16) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
         if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == Blocks.AIR) {
            _bp = BlockPos.containing(x, y + 1.0, z);
            _bs = Blocks.FIRE.defaultBlockState();
            _bso = world.getBlockState(_bp);
            var32 = _bso.getValues().entrySet().iterator();

            while (var32.hasNext()) {
               Entry<Property<?>, Comparable<?>> entry = (Entry<Property<?>, Comparable<?>>)var32.next();
               Property _property = _bs.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
               if (_property != null && _bs.getValue(_property) != null) {
                  try {
                     _bs = (BlockState)_bs.setValue(_property, (Comparable) entry.getValue());
                  } catch (Exception var15) {
                  }
               }
            }

            world.setBlock(_bp, _bs, 3);
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:inceneratable")))) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = Blocks.FIRE.defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<java.util.Map.Entry<Property<?>, Comparable<?>>> var34 = _bso.getValues().entrySet().iterator();

         while (var34.hasNext()) {

            java.util.Map.Entry<Property<?>, Comparable<?>> entry = var34.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, (Comparable) entry.getValue());
               } catch (Exception var14) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
      }

      if (1 == Mth.nextInt(RandomSource.create(), 1, 30)) {
         if (!ModList.get().isLoaded("burnt")) {
            if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() != Blocks.AIR
               && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y, z))
               && world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == Blocks.AIR) {
               CrustyChunksMod.queueServerWork(1, () -> {
                  BlockPos _bpx = BlockPos.containing(x, y + 1.0, z);
                  BlockState _bsx = Blocks.FIRE.defaultBlockState();
                  BlockState _bsox = world.getBlockState(_bpx);
                  java.util.Iterator<java.util.Map.Entry<Property<?>, Comparable<?>>> var10x = _bsox.getValues().entrySet().iterator();

                  while (var10x.hasNext()) {

                     java.util.Map.Entry<Property<?>, Comparable<?>> entryx = var10x.next();

                     Property _propertyx = _bsx.getBlock().getStateDefinition().getProperty(entryx.getKey().getName());
                     if (_propertyx != null && _bsx.getValue(_propertyx) != null) {
                        try {
                           _bsx = (BlockState)_bsx.setValue(_propertyx, (Comparable) entryx.getValue());
                        } catch (Exception var14x) {
                        }
                     }
                  }

                  world.setBlock(_bpx, _bsx, 3);
               });
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() != Blocks.AIR
            && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y, z))
            && world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == Blocks.AIR) {
            CrustyChunksMod.queueServerWork(
               1,
               () -> {
                  BlockPos _bpx = BlockPos.containing(x, y + 1.0, z);
                  BlockState _bsx = BuiltInRegistries.BLOCK
                     .getOrCreateTag(BlockTags.create(ResourceLocation.parse("crusty_chunks:firestarter")))
                     .getRandomElement(RandomSource.create()).map(_h -> (net.minecraft.world.level.block.Block) _h.value()).orElse(Blocks.AIR)
                     .defaultBlockState();
                  BlockState _bsox = world.getBlockState(_bpx);
                  java.util.Iterator<java.util.Map.Entry<Property<?>, Comparable<?>>> var10x = _bsox.getValues().entrySet().iterator();

                  while (var10x.hasNext()) {

                     java.util.Map.Entry<Property<?>, Comparable<?>> entryx = var10x.next();

                     Property _propertyx = _bsx.getBlock().getStateDefinition().getProperty(entryx.getKey().getName());
                     if (_propertyx != null && _bsx.getValue(_propertyx) != null) {
                        try {
                           _bsx = (BlockState)_bsx.setValue(_propertyx, (Comparable) entryx.getValue());
                        } catch (Exception var14x) {
                        }
                     }
                  }

                  world.setBlock(_bpx, _bsx, 3);
               }
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MassBurnBlockProcedure.execute", _wtSafe);
      }
   }
}
