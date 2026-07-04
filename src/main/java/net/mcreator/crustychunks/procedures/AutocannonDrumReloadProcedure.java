package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.text.DecimalFormat;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AutocannonDrumReloadProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double Rounds = 0.0;
         if ((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Ammo") == 0.0 && !world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putString("AmmoType", "Null");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("Null") || (new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("Small")) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.HUGE_BULLET.get()) {
               if ((new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") < 200.0) {
                  if ((double)(entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getCount() >= 200.0 - (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) {
                     (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).shrink((int)(200.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")));
                     Rounds = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") + 200.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo");
                     if (!world.isClientSide()) {
                        BlockPos _bpx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                        BlockState _bsx = world.getBlockState(_bpx);
                        if (_blockEntityx != null) {
                           _blockEntityx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                        }
                     }
                  } else if (200.0
                     > (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getCount());
                     if (!world.isClientSide()) {
                        BlockPos _bpxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                        BlockState _bsxx = world.getBlockState(_bpxx);
                        if (_blockEntityxx != null) {
                           _blockEntityxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                        }
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                     BlockState _bsxxx = world.getBlockState(_bpxxx);
                     if (_blockEntityxxx != null) {
                        _blockEntityxxx.getPersistentData().putString("AmmoType", "Small");
                     }

                     if (world instanceof Level _level) {
                        _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                     }
                  }

                  if (entity instanceof Player _player && !_player.level().isClientSide()) {
                     _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§6").format((new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) + "200"), true);
                  }

                  if (world instanceof Level _level) {
                     if (!_level.isClientSide()) {
                        _level.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F
                        );
                     } else {
                        _level.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F,
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()
               && (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") > 0.0) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x + 0.5, y + 1.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.HUGE_BULLET.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
                  BlockState _bsxxxx = world.getBlockState(_bpxxxx);
                  if (_blockEntityxxxx != null) {
                     _blockEntityxxxx.getPersistentData().putDouble("Ammo", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") - 1.0);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
                  }
               }

               if (entity instanceof Player _player && !_player.level().isClientSide()) {
                  _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§6").format((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) + "200"), true);
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F,
                        false
                     );
                  }
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("Null") || (new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("SmallHE")) {
            if ((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.HUGE_HE_BULLET.get()) {
               if ((new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") < 200.0) {
                  if ((double)(entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getCount() >= 200.0 - (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).shrink((int)(200.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")));
                     Rounds = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") + 200.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo");
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
                        BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
                        if (_blockEntityxxxxx != null) {
                           _blockEntityxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxx) {
                           _levelxx.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
                        }
                     }
                  } else if (200.0
                     > (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount());
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
                        BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
                        if (_blockEntityxxxxxx != null) {
                           _blockEntityxxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxx) {
                           _levelxx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
                        }
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
                     BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
                     if (_blockEntityxxxxxxx != null) {
                        _blockEntityxxxxxxx.getPersistentData().putString("AmmoType", "SmallHE");
                     }

                     if (world instanceof Level _levelxx) {
                        _levelxx.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
                     }
                  }

                  if (entity instanceof Player _player && !_player.level().isClientSide()) {
                     _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§6").format((new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) + "200"), true);
                  }

                  if (world instanceof Level _levelxx) {
                     if (!_levelxx.isClientSide()) {
                        _levelxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F
                        );
                     } else {
                        _levelxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F,
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == ItemStack.EMPTY.getItem()
               && (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") > 0.0) {
               if (world instanceof ServerLevel _levelxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelxxx, x + 0.5, y + 1.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.HUGE_HE_BULLET.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxx.addFreshEntity(entityToSpawn);
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
                  BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
                  if (_blockEntityxxxxxxxx != null) {
                     _blockEntityxxxxxxxx.getPersistentData().putDouble("Ammo", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") - 1.0);
                  }

                  if (world instanceof Level _levelxxx) {
                     _levelxxx.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
                  }
               }

               if (entity instanceof Player _player && !_player.level().isClientSide()) {
                  _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§6").format((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) + "200"), true);
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F,
                        false
                     );
                  }
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("Null") || (new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("Large")) {
            if ((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.SMALL_SHELL.get()) {
               if ((new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") < 75.0) {
                  if ((double)(entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount()
                     >= 75.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).shrink((int)(75.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")));
                     Rounds = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") + 75.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo");
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxx);
                        BlockState _bsxxxxxxxxx = world.getBlockState(_bpxxxxxxxxx);
                        if (_blockEntityxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxxxx) {
                           _levelxxxx.sendBlockUpdated(_bpxxxxxxxxx, _bsxxxxxxxxx, _bsxxxxxxxxx, 3);
                        }
                     }
                  } else if (75.0
                     > (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount());
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxx);
                        BlockState _bsxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxx);
                        if (_blockEntityxxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxxxx) {
                           _levelxxxx.sendBlockUpdated(_bpxxxxxxxxxx, _bsxxxxxxxxxx, _bsxxxxxxxxxx, 3);
                        }
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxxxxxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxx);
                     BlockState _bsxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxx);
                     if (_blockEntityxxxxxxxxxxx != null) {
                        _blockEntityxxxxxxxxxxx.getPersistentData().putString("AmmoType", "Large");
                     }

                     if (world instanceof Level _levelxxxx) {
                        _levelxxxx.sendBlockUpdated(_bpxxxxxxxxxxx, _bsxxxxxxxxxxx, _bsxxxxxxxxxxx, 3);
                     }
                  }

                  if (entity instanceof Player _player && !_player.level().isClientSide()) {
                     _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§675").format((new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo"))), true);
                  }

                  if (world instanceof Level _levelxxxx) {
                     if (!_levelxxxx.isClientSide()) {
                        _levelxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F
                        );
                     } else {
                        _levelxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F,
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == ItemStack.EMPTY.getItem()
               && (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") > 0.0) {
               if (world instanceof ServerLevel _levelxxxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelxxxxx, x + 0.5, y + 1.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_SHELL.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxxxx.addFreshEntity(entityToSpawn);
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxx);
                  BlockState _bsxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxx);
                  if (_blockEntityxxxxxxxxxxxx != null) {
                     _blockEntityxxxxxxxxxxxx.getPersistentData().putDouble("Ammo", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") - 1.0);
                  }

                  if (world instanceof Level _levelxxxxx) {
                     _levelxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxx, _bsxxxxxxxxxxxx, _bsxxxxxxxxxxxx, 3);
                  }
               }

               if (entity instanceof Player _player && !_player.level().isClientSide()) {
                  _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§675").format((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Ammo"))), true);
               }

               if (world instanceof Level _levelxxxxx) {
                  if (!_levelxxxxx.isClientSide()) {
                     _levelxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F
                     );
                  } else {
                     _levelxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F,
                        false
                     );
                  }
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("Null") || (new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("LargePF")) {
            if ((entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.SMALL_FLAK_SHELL.get()) {
               if ((new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") < 75.0) {
                  if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxxx ? _livEntxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount()
                     >= 75.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                        .shrink((int)(75.0 - (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")));
                     Rounds = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") + 75.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo");
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxx);
                        BlockState _bsxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxx);
                        if (_blockEntityxxxxxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxxxxxx) {
                           _levelxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxx, _bsxxxxxxxxxxxxx, _bsxxxxxxxxxxxxx, 3);
                        }
                     }
                  } else if (75.0
                     > (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxx ? _livEntxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount());
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxx);
                        BlockState _bsxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxx);
                        if (_blockEntityxxxxxxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxxxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxxxxxx) {
                           _levelxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxx, 3);
                        }
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxx);
                     BlockState _bsxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxx);
                     if (_blockEntityxxxxxxxxxxxxxxx != null) {
                        _blockEntityxxxxxxxxxxxxxxx.getPersistentData().putString("AmmoType", "LargePF");
                     }

                     if (world instanceof Level _levelxxxxxx) {
                        _levelxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxx, 3);
                     }
                  }

                  if (entity instanceof Player _player && !_player.level().isClientSide()) {
                     _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§675").format((new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo"))), true);
                  }

                  if (world instanceof Level _levelxxxxxx) {
                     if (!_levelxxxxxx.isClientSide()) {
                        _levelxxxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F
                        );
                     } else {
                        _levelxxxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F,
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == ItemStack.EMPTY.getItem()
               && (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") > 0.0) {
               if (world instanceof ServerLevel _levelxxxxxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelxxxxxxx, x + 0.5, y + 1.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_FLAK_SHELL.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxxxxxx.addFreshEntity(entityToSpawn);
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxx);
                  BlockState _bsxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxx);
                  if (_blockEntityxxxxxxxxxxxxxxxx != null) {
                     _blockEntityxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Ammo", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") - 1.0);
                  }

                  if (world instanceof Level _levelxxxxxxx) {
                     _levelxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxx, 3);
                  }
               }

               if (entity instanceof Player _player && !_player.level().isClientSide()) {
                  _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§675").format((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Ammo"))), true);
               }

               if (world instanceof Level _levelxxxxxxx) {
                  if (!_levelxxxxxxx.isClientSide()) {
                     _levelxxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F
                     );
                  } else {
                     _levelxxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F,
                        false
                     );
                  }
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("Null") || (new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "AmmoType").equals("LargeAP")) {
            if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxx ? _livEntxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.SMALL_AP_SHELL.get()) {
               if ((new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") < 75.0) {
                  if ((double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount()
                     >= 75.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo")) {
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                        .shrink((int)(75.0 - (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")));
                     Rounds = (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") + 75.0 - (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo");
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxx);
                        BlockState _bsxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxx);
                        if (_blockEntityxxxxxxxxxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxxxxxxxx) {
                           _levelxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxx, 3);
                        }
                     }
                  } else if (75.0
                     > (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount()) {
                     Rounds = (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Ammo")
                        + (double)(entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount();
                     (entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY)
                        .shrink((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getCount());
                     if (!world.isClientSide()) {
                        BlockPos _bpxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxx);
                        BlockState _bsxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxx);
                        if (_blockEntityxxxxxxxxxxxxxxxxxx != null) {
                           _blockEntityxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Ammo", Rounds);
                        }

                        if (world instanceof Level _levelxxxxxxxx) {
                           _levelxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxx, 3);
                        }
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxx);
                     BlockState _bsxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxx);
                     if (_blockEntityxxxxxxxxxxxxxxxxxxx != null) {
                        _blockEntityxxxxxxxxxxxxxxxxxxx.getPersistentData().putString("AmmoType", "LargeAP");
                     }

                     if (world instanceof Level _levelxxxxxxxx) {
                        _levelxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxx, 3);
                     }
                  }

                  if (entity instanceof Player _player && !_player.level().isClientSide()) {
                     _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                        public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§675").format((new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo"))), true);
                  }

                  if (world instanceof Level _levelxxxxxxxx) {
                     if (!_levelxxxxxxxx.isClientSide()) {
                        _levelxxxxxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F
                        );
                     } else {
                        _levelxxxxxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                           SoundSource.NEUTRAL,
                           3.0F,
                           2.0F,
                           false
                        );
                     }
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntxxxxxxxxxxxxxxx ? _livEntxxxxxxxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                  == ItemStack.EMPTY.getItem()
               && (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo") > 0.0) {
               if (world instanceof ServerLevel _levelxxxxxxxxx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelxxxxxxxxx, x + 0.5, y + 1.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_AP_SHELL.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelxxxxxxxxx.addFreshEntity(entityToSpawn);
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxx);
                  BlockState _bsxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxx);
                  if (_blockEntityxxxxxxxxxxxxxxxxxxxx != null) {
                     _blockEntityxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Ammo", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Ammo") - 1.0);
                  }

                  if (world instanceof Level _levelxxxxxxxxx) {
                     _levelxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxx, 3);
                  }
               }

               if (entity instanceof Player _player && !_player.level().isClientSide()) {
                  _player.displayClientMessage(Component.literal(new DecimalFormat("§4" + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "AmmoType") + (new Object() {
                     public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), " ") + "§6####§8/§675").format((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Ammo"))), true);
               }

               if (world instanceof Level _levelxxxxxxxxx) {
                  if (!_levelxxxxxxxxx.isClientSide()) {
                     _levelxxxxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F
                     );
                  } else {
                     _levelxxxxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lantern.place")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        2.0F,
                        false
                     );
                  }
               }
            }
         }

         if ((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Ammo") == 0.0 && !world.isClientSide()) {
            BlockPos _bpxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxx);
            BlockState _bsxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxx);
            if (_blockEntityxxxxxxxxxxxxxxxxxxxxx != null) {
               _blockEntityxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putString("AmmoType", "Null");
            }

            if (world instanceof Level _levelxxxxxxxxxx) {
               _levelxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxx, 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AutocannonDrumReloadProcedure.execute", _wtSafe);
      }
   }
}
