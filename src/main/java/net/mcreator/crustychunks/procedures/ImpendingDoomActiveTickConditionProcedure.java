package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class ImpendingDoomActiveTickConditionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         CompoundTag var10000;
         double var10002;
         label23: {
            var10000 = entity.getPersistentData();
            if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(CrustyChunksModMobEffects.IMPENDING_DOOM)) {
               var10002 = (double)_livEnt.getEffect(CrustyChunksModMobEffects.IMPENDING_DOOM).getAmplifier();
               break label23;
            }

            var10002 = 0.0;
         }

         var10000.putDouble("DoomType", var10002);
         if (world instanceof ServerLevel _level2 && _level2.isVillage(BlockPos.containing(x, y, z)) && entity instanceof LivingEntity _entity) {
            _entity.removeEffect(CrustyChunksModMobEffects.IMPENDING_DOOM);
         }
      }
   }
}
