package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.text.DecimalFormat;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class GeigerCounterHandTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         String severitycolor = "";
         double radiation = 0.0;
         radiation = entity.getPersistentData().getDouble("Radiation");
         if (0.0 <= radiation) {
            severitycolor = "§a";
            if (25.0 <= radiation) {
               severitycolor = "§e";
               if (50.0 <= radiation) {
                  severitycolor = "§6";
                  if (100.0 <= radiation) {
                     severitycolor = "§c";
                     if (150.0 <= radiation) {
                        severitycolor = "§4";
                        if (200.0 <= radiation) {
                           severitycolor = "§4";
                           if (250.0 <= radiation) {
                              severitycolor = "§0";
                           }
                        }
                     }
                  }
               }
            }
         }

         if (entity instanceof LivingEntity _livEnt7
            && _livEnt7.hasEffect(CrustyChunksModMobEffects.RADIATION)
            && Mth.nextDouble(RandomSource.create(), 2.0, 150.0) <= radiation
            && world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:geiger")),
                  SoundSource.PLAYERS,
                  2.0F,
                  (float)(Mth.nextDouble(RandomSource.create(), 0.9, 1.1) + Math.min(radiation / 100.0, 0.5))
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:geiger")),
                  SoundSource.PLAYERS,
                  2.0F,
                  (float)(Mth.nextDouble(RandomSource.create(), 0.9, 1.1) + Math.min(radiation / 100.0, 0.5)),
                  false
               );
            }
         }

         if ((
               (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.GEIGER_COUNTER.get()
                  || (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.GEIGER_COUNTER.get()
            )
            && entity instanceof Player _player
            && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal(severitycolor + "Radiation Exposure:" + new DecimalFormat("####").format(radiation)), true);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GeigerCounterHandTickProcedure.execute", _wtSafe);
      }
   }
}
