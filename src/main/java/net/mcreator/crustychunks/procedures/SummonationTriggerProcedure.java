package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import java.util.Map.Entry;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SummonationTriggerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double Modules = 0.0;
      double spawnx = 0.0;
      double spawnz = 0.0;
      boolean PlayerFound = false;
      BlockPos _bp = BlockPos.containing(x, y, z);
      BlockState _bs = ((Block)CrustyChunksModBlocks.OPEN_SUMMONATION.get()).defaultBlockState();
      BlockState _bso = world.getBlockState(_bp);
      java.util.Iterator<Property<?>> entityiterator = _bso.getProperties().iterator();

      while (entityiterator.hasNext()) {

         Property<?> entry_prop = entityiterator.next();

         Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
         if (_property != null && _bs.getValue(_property) != null) {
            try {
               _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
            } catch (Exception var21) {
            }
         }
      }

      world.setBlock(_bp, _bs, 3);
      if (world instanceof ServerLevel _level) {
         _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 3.0, z + 0.5, 5, 0.0, 3.0, 0.0, 0.01);
      }

      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemidle")),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:golemidle")),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F,
               false
            );
         }
      }

      PlayerFound = false;
      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(256.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         if (entityiteratorx instanceof Player && !PlayerFound) {
            if (entityiteratorx instanceof LivingEntity) {
               LivingEntity _entity = (LivingEntity)entityiteratorx;
               if (!_entity.level().isClientSide()) {
                  _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.IMPENDING_DOOM, 40000, 2, false, false));
               }
            }

            PlayerFound = true;
         }
      }

      CrustyChunksMod.queueServerWork(
         20,
         () -> {
            for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 6, 9); index0++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelx, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.PLUTONIUM_NUGGET.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index1 = 0; index1 < Mth.nextInt(RandomSource.create(), 5, 6); index1++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelx, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index2 = 0; index2 < Mth.nextInt(RandomSource.create(), 8, 16); index2++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelx, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.TECH_COMPONENT.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index3 = 0; index3 < Mth.nextInt(RandomSource.create(), 16, 24); index3++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelx, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_COMPONENT.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index4 = 0; index4 < Mth.nextInt(RandomSource.create(), 8, 10); index4++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _levelx, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.NEUTRON_REFLECTOR.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index5 = 0; index5 < Mth.nextInt(RandomSource.create(), 8, 16); index5++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.FUEL_ROD.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }
         }
      );
      CrustyChunksMod.queueServerWork(
         80,
         () -> {
            if (world instanceof ServerLevel _levelx) {
               Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.COMMANDER.get())
                  .spawn(_levelx, BlockPos.containing(x + 0.5, y + 1.5, z + 0.5), MobSpawnType.MOB_SUMMONED);
               if (entityToSpawn != null) {
                  entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
               }
            }
         }
      );
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SummonationTriggerProcedure.execute", _wtSafe);
      }
   }
}
