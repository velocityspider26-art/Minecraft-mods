package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class AmmoRackHitSystemProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      BlockState targetblock = Blocks.AIR.defaultBlockState();
      boolean explosive = false;
      double ZOffset = 0.0;
      double checkslotID = 0.0;
      double ItemCount = 0.0;
      double YOffset = 0.0;
      double XOffset = 0.0;
      double explosivetimer = 0.0;
      targetblock = world.getBlockState(BlockPos.containing(x, y, z));
      explosive = false;
      checkslotID = 0.0;
      if ((new Object() {
         public int getContainerSize(LevelAccessor world, BlockPos pos) {
            BlockEntity _ent = world.getBlockEntity(pos);
            return _ent != null && _ent instanceof BaseContainerBlockEntity _block ? _block.getContainerSize() : 0;
         }

         public int getAmount(LevelAccessor world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
               boolean var10000;
               label17: {
                  if (block.getStateDefinition().getProperty("type") instanceof EnumProperty _getep5 && world.getBlockState(pos).getValue(_getep5).toString().equals("SINGLE")) {
                     var10000 = true;
                     break label17;
                  }

                  var10000 = false;
               }

               boolean isSingle = var10000;
               if (!isSingle) {
                  return this.getContainerSize(world, pos) * 2;
               }
            }

            return this.getContainerSize(world, pos);
         }
      }).getAmount(world, new BlockPos((int)x, (int)y, (int)z)) > 0) {
         for (int index0 = 0;
            index0
               < (new Object() {
                     public int getContainerSize(LevelAccessor world, BlockPos pos) {
                        BlockEntity _ent = world.getBlockEntity(pos);
                        return _ent != null && _ent instanceof BaseContainerBlockEntity _block ? _block.getContainerSize() : 0;
                     }

                     public int getAmount(LevelAccessor world, BlockPos pos) {
                        Block block = world.getBlockState(pos).getBlock();
                        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                           boolean var10000;
                           label17: {
                              if (block.getStateDefinition().getProperty("type") instanceof EnumProperty _getep5
                                 && world.getBlockState(pos).getValue(_getep5).toString().equals("SINGLE")) {
                                 var10000 = true;
                                 break label17;
                              }

                              var10000 = false;
                           }

                           boolean isSingle = var10000;
                           if (!isSingle) {
                              return this.getContainerSize(world, pos) * 2;
                           }
                        }

                        return this.getContainerSize(world, pos);
                     }
                  })
                  .getAmount(world, new BlockPos((int)x, (int)y, (int)z));
            index0++
         ) {
            if ((new Object() {
                  public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                     AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                     BlockEntity _ent = world.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                     }

                     return _retval.get();
                  }
               })
               .getItemStack(world, BlockPos.containing(x, y, z), (int)checkslotID)
               .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:containerexplosive")))) {
               ItemCount = ItemCount
                  + 1.0
                  + (double)(
                     Math.round(
                           (float)(new Object() {
                                 public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                                    AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                                    BlockEntity _ent = world.getBlockEntity(pos);
                                    if (_ent != null) {
                                       {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                                    }

                                    return _retval.get();
                                 }
                              })
                              .getItemStack(world, BlockPos.containing(x, y, z), (int)checkslotID)
                              .getCount()
                        )
                        / 10
                  );
               explosive = true;
            }

            checkslotID++;
         }
      }

      if (explosive) {
         ExplosionExampleProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5, Math.min(40.0, Math.ceil(ItemCount) / (Math.ceil(ItemCount) / 20.0)));
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AmmoRackHitSystemProcedure.execute", _wtSafe);
      }
   }
}
