package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.SeatEntityEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber({Dist.CLIENT})
public class ManualAimerFireProcedure {
   @SubscribeEvent
   public static void onLeftClick(LeftClickEmpty event) {
      PacketDistributor.sendToServer(new ManualAimerFireProcedure.ManualAimerFireMessage());
      execute(event.getLevel(), event.getEntity());
   }

   public static void execute(LevelAccessor world, Entity entity) {
      try {
      execute(null, world, entity);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ManualAimerFireProcedure.execute", _wtSafe);
      }
   }

   private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
      try {
      if (entity != null) {
         if (entity.getRootVehicle() instanceof SeatEntityEntity
            && world.getBlockState(BlockPos.containing(entity.getRootVehicle().getX(), entity.getRootVehicle().getY(), entity.getRootVehicle().getZ())).getBlock()
               == CrustyChunksModBlocks.MANUAL_AIMER.get()) {
            int _value = 5;
            BlockPos _pos = BlockPos.containing(entity.getRootVehicle().getX(), entity.getRootVehicle().getY(), entity.getRootVehicle().getZ());
            BlockState _bs = world.getBlockState(_pos);
            if (_bs.getBlock().getStateDefinition().getProperty("firing") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value)) {
               world.setBlock(_pos, (BlockState)_bs.setValue(_integerProp, _value), 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ManualAimerFireProcedure.execute", _wtSafe);
      }
   }

   public record ManualAimerFireMessage() implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<ManualAimerFireMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "manual_aimer_fire_message"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ManualAimerFireMessage> STREAM_CODEC = StreamCodec.unit(new ManualAimerFireMessage());

      @Override
      public Type<ManualAimerFireMessage> type() {
         return TYPE;
      }

      public static void handleData(final ManualAimerFireMessage message, final IPayloadContext context) {
         if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
               Entity sender = context.player();
               if (sender instanceof net.minecraft.server.level.ServerPlayer _sp && _sp.isAlive() && !_sp.isSpectator()) {
                  ManualAimerFireProcedure.execute(sender.level(), sender);
               }
            }).exceptionally(e -> {
               context.connection().disconnect(Component.literal(e.getMessage()));
               return null;
            });
         }
      }
   }

   @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
   public static class MessageRegistration {
      @SubscribeEvent
      public static void registerMessage(FMLCommonSetupEvent event) {
         CrustyChunksMod.addNetworkMessage(ManualAimerFireMessage.TYPE, ManualAimerFireMessage.STREAM_CODEC, ManualAimerFireMessage::handleData);
      }
   }
}
