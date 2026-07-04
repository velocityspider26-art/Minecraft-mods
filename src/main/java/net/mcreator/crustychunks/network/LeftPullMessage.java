package net.mcreator.crustychunks.network;

import net.mcreator.crustychunks.procedures.LKNProcedure;
import net.mcreator.crustychunks.procedures.LKYProcedure;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record LeftPullMessage(int eventType, int pressedms) implements CustomPacketPayload {
   public static final Type<LeftPullMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "key_left_pull"));
   public static final StreamCodec<RegistryFriendlyByteBuf, LeftPullMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, LeftPullMessage message) -> {
      buffer.writeInt(message.eventType);
      buffer.writeInt(message.pressedms);
   }, (RegistryFriendlyByteBuf buffer) -> new LeftPullMessage(buffer.readInt(), buffer.readInt()));

   @Override
   public Type<LeftPullMessage> type() {
      return TYPE;
   }

   public static void handleData(final LeftPullMessage message, final IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer _sender && _sender.isAlive() && !_sender.isSpectator())
               pressAction(_sender, message.eventType, message.pressedms);
         }).exceptionally(e -> {
            context.connection().disconnect(Component.literal(e.getMessage()));
            return null;
         });
      }
   }

   public static void pressAction(Player entity, int type, int pressedms) {
      Level world = entity.level();
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      if (world.hasChunkAt(entity.blockPosition())) {
         if (type == 0) {
            LKYProcedure.execute(entity);
         }

         if (type == 1) {
            LKNProcedure.execute(entity);
         }
      }
   }

   @SubscribeEvent
   public static void registerMessage(FMLCommonSetupEvent event) {
      CrustyChunksMod.addNetworkMessage(LeftPullMessage.TYPE, LeftPullMessage.STREAM_CODEC, LeftPullMessage::handleData);
   }
}
