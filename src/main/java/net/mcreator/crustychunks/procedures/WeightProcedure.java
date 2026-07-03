package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.utils.WariumCaps;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandler;

@EventBusSubscriber
public class WeightProcedure {
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (true) {
         execute(event, event.getEntity().level(), event.getEntity());
      }
   }

   public static void execute(LevelAccessor world, Entity entity) {
      execute(null, world, entity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
      if (entity != null) {
         double Weight = 0.0;
         if (1 == Mth.nextInt(RandomSource.create(), 1, 20)
            && (new Object() {
                  public boolean checkGamemode(Entity _ent) {
                     if (_ent instanceof ServerPlayer _serverPlayer) {
                        return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
                     } else {
                        return _ent.level().isClientSide() && _ent instanceof Player _player
                           ? Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
                              && Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SURVIVAL
                           : false;
                     }
                  }
               })
               .checkGamemode(entity)) {
            Weight = 0.0;
            AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
            WariumCaps.itemHandler(entity, null).ifPresent(_iitemhandlerref::set);
            if (_iitemhandlerref.get() != null) {
               for (int _idx = 0; _idx < _iitemhandlerref.get().getSlots(); _idx++) {
                  ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idx).copy();
                  if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("crusty_chunks:mass1")))) {
                     Weight += (double)(itemstackiterator.getCount() * 50);
                  } else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("crusty_chunks:mass2")))) {
                     Weight += (double)(itemstackiterator.getCount() * 100);
                  } else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("crusty_chunks:mass3")))) {
                     Weight += (double)(itemstackiterator.getCount() * 200);
                  } else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("crusty_chunks:mass4")))) {
                     Weight += (double)(itemstackiterator.getCount() * 300);
                  } else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("crusty_chunks:mass5")))) {
                     Weight += (double)(itemstackiterator.getCount() * 600);
                  } else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("crusty_chunks:mass6")))) {
                     Weight += (double)(itemstackiterator.getCount() * 750);
                  }

                  if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("crusty_chunks:heavy")))) {
                     Weight += 2000.0;
                  }
               }
            }

            if (Weight > 1000.0 && entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
            }

            if (Weight > 1500.0) {
               if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                  _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false));
               }

               if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                  _entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, false, false));
               }
            }
         }
      }
   }
}
