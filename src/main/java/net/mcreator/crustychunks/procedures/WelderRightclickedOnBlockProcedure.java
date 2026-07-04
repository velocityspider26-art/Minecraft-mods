package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class WelderRightclickedOnBlockProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         double valuebefore = 0.0;
         boolean success = false;
         int horizontalRadiusSquare = 1;
         int verticalRadiusSquare = 1;
         int yIterationsSquare = verticalRadiusSquare;

         for (int i = -verticalRadiusSquare; i <= yIterationsSquare; i++) {
            for (int xi = -horizontalRadiusSquare; xi <= horizontalRadiusSquare; xi++) {
               for (int zi = -horizontalRadiusSquare; zi <= horizontalRadiusSquare; zi++) {
                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:variablearmor")))) {
                     valuebefore = world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock().getStateDefinition().getProperty("damage") instanceof IntegerProperty _getip9
                        ? (double)((Integer)world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getValue(_getip9)).intValue()
                        : -1.0;
                     int _value = (
                           world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock().getStateDefinition().getProperty("damage") instanceof IntegerProperty _getip17
                              ? (Integer)world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getValue(_getip17)
                              : -1
                        )
                        - 1;
                     BlockPos _pos = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                     BlockState _bs = world.getBlockState(_pos);
                     Property var22 = _bs.getBlock().getStateDefinition().getProperty("damage");
                     if (var22 instanceof IntegerProperty) {
                        IntegerProperty _integerProp = (IntegerProperty)var22;
                        if (_integerProp.getPossibleValues().contains(_value)) {
                           world.setBlock(_pos, (BlockState)_bs.setValue(_integerProp, _value), 3);
                        }
                     }

                     if (valuebefore
                        != (double)(
                           world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock().getStateDefinition().getProperty("damage") instanceof IntegerProperty _getip23
                              ? (Integer)world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getValue(_getip23)
                              : -1
                        )) {
                        if (world instanceof ServerLevel _level) {
                           _level.sendParticles(
                              (SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(),
                              x + (double)xi + 0.5,
                              y + (double)i + 0.5,
                              z + (double)zi + 0.5,
                              15,
                              0.5,
                              0.5,
                              0.5,
                              1.0
                           );
                        }

                        if (world instanceof Level) {
                           Level _level = (Level)world;
                           if (!_level.isClientSide()) {
                              _level.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F
                              );
                           } else {
                              _level.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }

                        if (world instanceof ServerLevel _srvlvl) itemstack.hurtAndBreak(1, _srvlvl, null, _itmcns -> {});
                     }
                  }
               }
            }
         }

         if (entity instanceof Player _player) {
            _player.getCooldowns().addCooldown(itemstack.getItem(), 20);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("WelderRightclickedOnBlockProcedure.execute", _wtSafe);
      }
   }
}
