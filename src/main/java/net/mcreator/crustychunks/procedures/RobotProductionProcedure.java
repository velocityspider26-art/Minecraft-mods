package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.ArrayList;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class RobotProductionProcedure {
   @SubscribeEvent
   public static void onWorldTick(LevelTickEvent.Post event) {
      if (true) {
         execute(event, event.getLevel());
      }
   }

   public static void execute(LevelAccessor world) {
      try {
      execute(null, world);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RobotProductionProcedure.execute", _wtSafe);
      }
   }

   private static void execute(@Nullable Event event, LevelAccessor world) {
      try {
      double spawnx = 0.0;
      double spawnz = 0.0;
      double attempts = 0.0;
      double locationx = 0.0;
      double locationz = 0.0;
      double productionvalue = 0.0;
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.WARIUM_APOCALYPSE_DYNAMIC_PRODUCTION)) {
         productionvalue = CrustyChunksModVariables.MapVariables.get(world).Production;
      } else {
         productionvalue = 2.0;
      }

      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)) {
         for (int index0 = 0; index0 < (int)(productionvalue * CrustyChunksModVariables.MapVariables.get(world).ApocalypseMultiplier); index0++) {
            if (Mth.nextInt(RandomSource.create(), 1, (int)(1600.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers + 1.0, 50.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(1600.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers + 1.0, 50.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(4000.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders + 1.0, 15.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(16000.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseWorkers)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseWorkers = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseWorkers + 1.0, 15.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(16000.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters + 1.0, 8.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(32000.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators + 1.0, 5.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(30000.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers + 1.0, 6.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(48000.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseEradicators)) == 1
               )
             {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseEradicators = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseEradicators + 1.0, 3.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            } else if (Mth.nextInt(RandomSource.create(), 1, (int)(1600.0 * CrustyChunksModVariables.MapVariables.get(world).ApocalypseBreachers)) == 1) {
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseBreachers = Math.min(
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseBreachers + 1.0, 15.0
               );
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            }
         }

         if (1.0 < productionvalue) {
            for (Entity entityiterator : new ArrayList<>(world.players())) {
               if (1
                     == Mth.nextInt(
                        RandomSource.create(), 1, (int)(4000.0 / CrustyChunksModVariables.MapVariables.get(world).ApocalypseMultiplier / productionvalue)
                     )
                  && world.canSeeSkyFromBelowWater(BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()))) {
                  CrustyChunksModVariables.MapVariables.get(world).motivation++;
                  CrustyChunksModVariables.MapVariables.get(world).syncData(world);
                  if (world instanceof ServerLevel _level19
                     && _level19.isVillage(BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()))) {
                     CrustyChunksModVariables.MapVariables.get(world).motivation++;
                     CrustyChunksModVariables.MapVariables.get(world).syncData(world);
                  }
               }

               if (20.0 < CrustyChunksModVariables.MapVariables.get(world).motivation
                  && 1
                     == Mth.nextInt(
                        RandomSource.create(), 1, (int)(2000.0 / CrustyChunksModVariables.MapVariables.get(world).ApocalypseMultiplier / productionvalue)
                     )
                  && world.canSeeSkyFromBelowWater(BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()))) {
                  if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
                     spawnx = (double)Mth.nextInt(RandomSource.create(), -100, -75);
                  } else {
                     spawnx = (double)Mth.nextInt(RandomSource.create(), 75, 100);
                  }

                  if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
                     spawnz = (double)Mth.nextInt(RandomSource.create(), -100, -75);
                  } else {
                     spawnz = (double)Mth.nextInt(RandomSource.create(), 75, 100);
                  }

                  for (;
                     world.getBlockState(
                              BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(spawnx), Mth.floor(spawnz)) - 2), spawnz)
                           )
                           .getBlock() instanceof LiquidBlock
                        || 10.0 < attempts;
                     attempts++
                  ) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
                        spawnx = (double)Mth.nextInt(RandomSource.create(), -100, -75);
                     } else {
                        spawnx = (double)Mth.nextInt(RandomSource.create(), 75, 100);
                     }

                     if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
                        spawnz = (double)Mth.nextInt(RandomSource.create(), -100, -75);
                     } else {
                        spawnz = (double)Mth.nextInt(RandomSource.create(), 75, 100);
                     }
                  }

                  CrustyChunksModVariables.MapVariables.get(world).motivation = 0.0;
                  CrustyChunksModVariables.MapVariables.get(world).syncData(world);
                  if (1 == Mth.nextInt(RandomSource.create(), 1, 3)) {
                     AirRaidProcedure.execute(world, entityiterator.getX() + spawnx, entityiterator.getZ() + spawnz);
                  } else {
                     SmallAttackProcedure.execute(world, entityiterator.getX() + spawnx, entityiterator.getZ() + spawnz);
                  }
               }

               locationx = entityiterator.getX();
               locationz = entityiterator.getZ();
            }

            if (0.0 != locationx && 0.0 != locationz) {
               Vec3 _center = new Vec3(
                  locationx + spawnx,
                  (double)world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)(locationx + spawnx), (int)(locationz + spawnz)),
                  locationz + spawnz
               );

               for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(20.0), e -> true)
                  .stream()
                  .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
                  .toList()) {
                  if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))) {
                     if (entityiterator instanceof Mob _entity) {
                        _entity.getNavigation().stop();
                     }

                     if (entityiterator instanceof Mob _entity) {
                        _entity.getNavigation()
                           .moveTo(locationx, (double)world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)locationx, (int)locationz), locationz, 1.0);
                     }
                  }
               }
            }
         }
      }

      locationx = 0.0;
      locationz = 0.0;
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RobotProductionProcedure.execute", _wtSafe);
      }
   }
}
