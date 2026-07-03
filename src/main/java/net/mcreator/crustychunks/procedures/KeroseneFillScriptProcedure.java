package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class KeroseneFillScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if ((new Object() {
               public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                  }

                  return _retval.get();
               }
            }).getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
               public int getBlockTanks(LevelAccessor level, BlockPos pos) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTanks());
}
                  }

                  return _retval.get();
               }
            }).getBlockTanks(world, BlockPos.containing(x, y, z))) <= 4000
            && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)
               .is(ItemTags.create(ResourceLocation.parse("c:buckets/kerosene")))) {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)(
                        0.5
                           + (double)(
                              (new Object() {
                                       public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                                          AtomicInteger _retval = new AtomicInteger(0);
                                          BlockEntity _ent = level.getBlockEntity(pos);
                                          if (_ent != null) {
                                             {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                                          }

                                          return _retval.get();
                                       }
                                    })
                                    .getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
                                       public int getBlockTanks(LevelAccessor level, BlockPos pos) {
                                          AtomicInteger _retval = new AtomicInteger(0);
                                          BlockEntity _ent = level.getBlockEntity(pos);
                                          if (_ent != null) {
                                             {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTanks());
}
                                          }

                                          return _retval.get();
                                       }
                                    }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                                 / 2000
                           )
                     )
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)(
                        0.5
                           + (double)(
                              (new Object() {
                                       public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                                          AtomicInteger _retval = new AtomicInteger(0);
                                          BlockEntity _ent = level.getBlockEntity(pos);
                                          if (_ent != null) {
                                             {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                                          }

                                          return _retval.get();
                                       }
                                    })
                                    .getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
                                       public int getBlockTanks(LevelAccessor level, BlockPos pos) {
                                          AtomicInteger _retval = new AtomicInteger(0);
                                          BlockEntity _ent = level.getBlockEntity(pos);
                                          if (_ent != null) {
                                             {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTanks());
}
                                          }

                                          return _retval.get();
                                       }
                                    }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                                 / 2000
                           )
                     ),
                     false
                  );
               }
            }

            if (entity instanceof LivingEntity _entity) {
               ItemStack _setstack = new ItemStack(Items.BUCKET).copy();
               _setstack.setCount(1);
               _entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
               if (_entity instanceof Player _player) {
                  _player.getInventory().setChanged();
               }
            }

            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            int _amount = (new Object() {
                  public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(
                                    capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), amount), FluidAction.SIMULATE)
                                 )
                           ;
}
                     }

                     return _retval.get();
                  }
               })
               .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000);
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), _amount), FluidAction.EXECUTE);
}
            }
         }
      }
   }
}
