package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class HardpointReloadProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.EMPTY_MISSILE_HARDPOINT.get()) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.FIRE_SPEAR_ROCKET.get()) {
               if (entity instanceof LivingEntity _entity) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.FIRE_SPEAR_ROCKET.get()).copy();
                  _setstack.setCount(0);
                  _entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entity instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.FIRE_SPEAR_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> _be = _bso.getProperties().iterator();

               while (_be.hasNext()) {

                  Property<?> entry_prop = _be.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var36) {
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
                  BlockEntity var127 = world.getBlockEntity(_bp);
                  if (var127 != null) {
                     try {
                        var127.loadWithComponents(_bnbt, world.registryAccess());
                     } catch (Exception var35) {
                     }
                  }
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            } else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.SEEKER_SPEAR_ROCKET.get()) {
               if (entity instanceof LivingEntity _entityx) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.SEEKER_SPEAR_ROCKET.get()).copy();
                  _setstack.setCount(0);
                  _entityx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.SEEKER_SPEAR_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> var128 = _bso.getProperties().iterator();

               while (var128.hasNext()) {

                  Property<?> entry_prop = var128.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var34) {
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
                  BlockEntity var130 = world.getBlockEntity(_bp);
                  if (var130 != null) {
                     try {
                        var130.loadWithComponents(_bnbtx, world.registryAccess());
                     } catch (Exception var33) {
                     }
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            } else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.STRIKE_SPEAR_MISSILE.get()) {
               if (entity instanceof LivingEntity _entityxx) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.STRIKE_SPEAR_MISSILE.get()).copy();
                  _setstack.setCount(0);
                  _entityxx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityxx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.STRIKE_SPEAR_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> var131 = _bso.getProperties().iterator();

               while (var131.hasNext()) {

                  Property<?> entry_prop = var131.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var32) {
                     }
                  }
               }

               BlockEntity _bexx = world.getBlockEntity(_bp);
               CompoundTag _bnbtxx = null;
               if (_bexx != null) {
                  _bnbtxx = _bexx.saveWithFullMetadata(world.registryAccess());
                  _bexx.setRemoved();
               }

               world.setBlock(_bp, _bs, 3);
               if (_bnbtxx != null) {
                  BlockEntity var133 = world.getBlockEntity(_bp);
                  if (var133 != null) {
                     try {
                        var133.loadWithComponents(_bnbtxx, world.registryAccess());
                     } catch (Exception var31) {
                     }
                  }
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            } else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.RADAR_SPEAR_MISSILE.get()) {
               if (entity instanceof LivingEntity _entityxxx) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.RADAR_SPEAR_MISSILE.get()).copy();
                  _setstack.setCount(0);
                  _entityxxx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityxxx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.RADAR_SPEAR_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> var134 = _bso.getProperties().iterator();

               while (var134.hasNext()) {

                  Property<?> entry_prop = var134.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var30) {
                     }
                  }
               }

               BlockEntity _bexxx = world.getBlockEntity(_bp);
               CompoundTag _bnbtxxx = null;
               if (_bexxx != null) {
                  _bnbtxxx = _bexxx.saveWithFullMetadata(world.registryAccess());
                  _bexxx.setRemoved();
               }

               world.setBlock(_bp, _bs, 3);
               if (_bnbtxxx != null) {
                  BlockEntity var136 = world.getBlockEntity(_bp);
                  if (var136 != null) {
                     try {
                        var136.loadWithComponents(_bnbtxxx, world.registryAccess());
                     } catch (Exception var29) {
                     }
                  }
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.FIRE_SPEAR_MISSILE_HARDPOINT.get()) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
               if (world instanceof ServerLevel _levelxxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelxxxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.FIRE_SPEAR_ROCKET.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxxx.addFreshEntity(entityToSpawn);
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.EMPTY_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> var82 = _bso.getProperties().iterator();

               while (var82.hasNext()) {

                  Property<?> entry_prop = var82.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var28) {
                     }
                  }
               }

               BlockEntity _bexxxx = world.getBlockEntity(_bp);
               CompoundTag _bnbtxxxx = null;
               if (_bexxxx != null) {
                  _bnbtxxxx = _bexxxx.saveWithFullMetadata(world.registryAccess());
                  _bexxxx.setRemoved();
               }

               world.setBlock(_bp, _bs, 3);
               if (_bnbtxxxx != null) {
                  BlockEntity var84 = world.getBlockEntity(_bp);
                  if (var84 != null) {
                     try {
                        var84.loadWithComponents(_bnbtxxxx, world.registryAccess());
                     } catch (Exception var27) {
                     }
                  }
               }

               if (world instanceof Level _levelxxxx) {
                  if (!_levelxxxx.isClientSide()) {
                     _levelxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _levelxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.SEEKER_SPEAR_MISSILE_HARDPOINT.get()) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
               if (world instanceof ServerLevel _levelxxxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelxxxxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SEEKER_SPEAR_ROCKET.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxxxx.addFreshEntity(entityToSpawn);
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.EMPTY_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> var85 = _bso.getProperties().iterator();

               while (var85.hasNext()) {

                  Property<?> entry_prop = var85.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var26) {
                     }
                  }
               }

               BlockEntity _bexxxxx = world.getBlockEntity(_bp);
               CompoundTag _bnbtxxxxx = null;
               if (_bexxxxx != null) {
                  _bnbtxxxxx = _bexxxxx.saveWithFullMetadata(world.registryAccess());
                  _bexxxxx.setRemoved();
               }

               world.setBlock(_bp, _bs, 3);
               if (_bnbtxxxxx != null) {
                  BlockEntity var87 = world.getBlockEntity(_bp);
                  if (var87 != null) {
                     try {
                        var87.loadWithComponents(_bnbtxxxxx, world.registryAccess());
                     } catch (Exception var25) {
                     }
                  }
               }

               if (world instanceof Level _levelxxxxx) {
                  if (!_levelxxxxx.isClientSide()) {
                     _levelxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _levelxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.STRIKE_SPEAR_MISSILE_HARDPOINT.get()) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
               if (world instanceof ServerLevel _levelxxxxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelxxxxxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STRIKE_SPEAR_MISSILE.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxxxxx.addFreshEntity(entityToSpawn);
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.EMPTY_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> var88 = _bso.getProperties().iterator();

               while (var88.hasNext()) {

                  Property<?> entry_prop = var88.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var24) {
                     }
                  }
               }

               BlockEntity _bexxxxxx = world.getBlockEntity(_bp);
               CompoundTag _bnbtxxxxxx = null;
               if (_bexxxxxx != null) {
                  _bnbtxxxxxx = _bexxxxxx.saveWithFullMetadata(world.registryAccess());
                  _bexxxxxx.setRemoved();
               }

               world.setBlock(_bp, _bs, 3);
               if (_bnbtxxxxxx != null) {
                  BlockEntity var90 = world.getBlockEntity(_bp);
                  if (var90 != null) {
                     try {
                        var90.loadWithComponents(_bnbtxxxxxx, world.registryAccess());
                     } catch (Exception var23) {
                     }
                  }
               }

               if (world instanceof Level _levelxxxxxx) {
                  if (!_levelxxxxxx.isClientSide()) {
                     _levelxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _levelxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.RADAR_SPEAR_MISSILE_HARDPOINT.get()) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
               if (world instanceof ServerLevel _levelxxxxxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelxxxxxxx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.RADAR_SPEAR_MISSILE.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxxxxxx.addFreshEntity(entityToSpawn);
               }

               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockState _bs = ((Block)CrustyChunksModBlocks.EMPTY_MISSILE_HARDPOINT.get()).defaultBlockState();
               BlockState _bso = world.getBlockState(_bp);
               java.util.Iterator<Property<?>> var91 = _bso.getProperties().iterator();

               while (var91.hasNext()) {

                  Property<?> entry_prop = var91.next();

                  Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                  if (_property != null && _bs.getValue(_property) != null) {
                     try {
                        _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                     } catch (Exception var22) {
                     }
                  }
               }

               BlockEntity _bexxxxxxx = world.getBlockEntity(_bp);
               CompoundTag _bnbtxxxxxxx = null;
               if (_bexxxxxxx != null) {
                  _bnbtxxxxxxx = _bexxxxxxx.saveWithFullMetadata(world.registryAccess());
                  _bexxxxxxx.setRemoved();
               }

               world.setBlock(_bp, _bs, 3);
               if (_bnbtxxxxxxx != null) {
                  BlockEntity var93 = world.getBlockEntity(_bp);
                  if (var93 != null) {
                     try {
                        var93.loadWithComponents(_bnbtxxxxxxx, world.registryAccess());
                     } catch (Exception var21) {
                     }
                  }
               }

               if (world instanceof Level _levelxxxxxxx) {
                  if (!_levelxxxxxxx.isClientSide()) {
                     _levelxxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F
                     );
                  } else {
                     _levelxxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.3F,
                        false
                     );
                  }
               }
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ORDINANCE_CONTROLLER.get()
            && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == ((Block)CrustyChunksModBlocks.ORDINANCE_CORE.get()).asItem()) {
            (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).shrink(1);
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockState _bs = ((Block)CrustyChunksModBlocks.ORDINANCE_CORE.get()).defaultBlockState();
            BlockState _bso = world.getBlockState(_bp);
            java.util.Iterator<Property<?>> var94 = _bso.getProperties().iterator();

            while (var94.hasNext()) {

               Property<?> entry_prop = var94.next();

               Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
               if (_property != null && _bs.getValue(_property) != null) {
                  try {
                     _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                  } catch (Exception var20) {
                  }
               }
            }

            BlockEntity _bexxxxxxxx = world.getBlockEntity(_bp);
            CompoundTag _bnbtxxxxxxxx = null;
            if (_bexxxxxxxx != null) {
               _bnbtxxxxxxxx = _bexxxxxxxx.saveWithFullMetadata(world.registryAccess());
               _bexxxxxxxx.setRemoved();
            }

            world.setBlock(_bp, _bs, 3);
            if (_bnbtxxxxxxxx != null) {
               BlockEntity var96 = world.getBlockEntity(_bp);
               if (var96 != null) {
                  try {
                     var96.loadWithComponents(_bnbtxxxxxxxx, world.registryAccess());
                  } catch (Exception var19) {
                  }
               }
            }

            if (world instanceof Level _levelxxxxxxxx) {
               if (!_levelxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.3F
                  );
               } else {
                  _levelxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.open")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.3F,
                     false
                  );
               }
            }
         }
      }
   }
}
