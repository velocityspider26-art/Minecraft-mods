package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class EasterEggProcedure {
   @SubscribeEvent
   public static void onChat(ServerChatEvent event) {
      execute(
         event,
         event.getPlayer().level(),
         event.getPlayer().getX(),
         event.getPlayer().getY(),
         event.getPlayer().getZ(),
         event.getPlayer(),
         event.getRawText()
      );
   }

   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, String text) {
      execute(null, world, x, y, z, entity, text);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, String text) {
      if (entity != null && text != null) {
         if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)
            && 0.0
               == entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                  .eastereggcooldown
            && text.contains("clanker")) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(250.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))) {
                  CrustyChunksMod.queueServerWork(
                     Mth.nextInt(RandomSource.create(), 1, 20),
                     () -> {
                        if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent) {
                           _entity.setTarget(_ent);
                        }

                        if (entity instanceof Player _player && !_player.level().isClientSide()) {
                           _player.displayClientMessage(Component.literal("<Strategist> §cWHAT DID YOU SAY?!"), false);
                        }

                        if (world instanceof Level _level) {
                           if (!_level.isClientSide()) {
                              _level.playSound(
                                 null,
                                 BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:commanderalert")),
                                 SoundSource.HOSTILE,
                                 3.0F,
                                 (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                              );
                           } else {
                              _level.playLocalSound(
                                 entityiterator.getX(),
                                 entityiterator.getY(),
                                 entityiterator.getZ(),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:commanderalert")),
                                 SoundSource.HOSTILE,
                                 3.0F,
                                 (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                                 false
                              );
                           }
                        }

                        if (world instanceof Level _levelx) {
                           if (!_levelx.isClientSide()) {
                              _levelx.playSound(
                                 null,
                                 BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:commanderwaffing")),
                                 SoundSource.HOSTILE,
                                 3.0F,
                                 (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                              );
                           } else {
                              _levelx.playLocalSound(
                                 entityiterator.getX(),
                                 entityiterator.getY(),
                                 entityiterator.getZ(),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:commanderwaffing")),
                                 SoundSource.HOSTILE,
                                 3.0F,
                                 (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                                 false
                              );
                           }
                        }
                     }
                  );
                  double _setval = 400000.0;
                  {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
                     _vars.eastereggcooldown = _setval;
                     _vars.syncPlayerVariables(entity);
                  
         }
               }
            }
         }
      }
   }
}
