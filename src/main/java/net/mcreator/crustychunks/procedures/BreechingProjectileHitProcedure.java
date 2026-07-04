package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class BreechingProjectileHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:chippable")))) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F,
                  false
               );
            }
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:breakable_metal")))) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F
               );
            } else {
               _levelx.playLocalSound(
                  x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")), SoundSource.NEUTRAL, 1.0F, 1.0F, false
               );
            }
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:splinterable")))) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:crushable")))) {
         if (world instanceof Level _levelxx) {
            if (!_levelxx.isClientSide()) {
               _levelxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)(1.0 + Mth.nextDouble(RandomSource.create(), -0.2, 0.4))
               );
            } else {
               _levelxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)(1.0 + Mth.nextDouble(RandomSource.create(), -0.2, 0.4)),
                  false
               );
            }
         }

         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = Blocks.GRAVEL.defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> var10 = _bso.getProperties().iterator();

         while (var10.hasNext()) {

            Property<?> entry_prop = var10.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var14) {
               }
            }
         }

         world.setBlock(_bp, _bs, 3);
         world.levelEvent(2001, BlockPos.containing(x, y, z), Block.getId(Blocks.COBBLESTONE.defaultBlockState()));
         if (world instanceof ServerLevel _levelxxx) {
            _levelxxx.sendParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 3, 0.4, 0.4, 0.4, 0.2);
         }
      }

      CrackProcedureProcedure.execute(world, x, y, z);
      SmallBulletHitProcedure.execute(world, x, y, z);
      world.levelEvent(2001, BlockPos.containing(x, y + 1.0, z), Block.getId(world.getBlockState(BlockPos.containing(x, y, z))));
      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:doors")))) {
         if (world instanceof Level _levelxxx) {
            if (!_levelxxx.isClientSide()) {
               _levelxxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.attack_iron_door")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)(1.0 + Mth.nextDouble(RandomSource.create(), -0.2, 0.4))
               );
            } else {
               _levelxxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.attack_iron_door")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)(1.0 + Mth.nextDouble(RandomSource.create(), -0.2, 0.4)),
                  false
               );
            }
         }

         world.destroyBlock(BlockPos.containing(x, y, z), false);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BreechingProjectileHitProcedure.execute", _wtSafe);
      }
   }
}
