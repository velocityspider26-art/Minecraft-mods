package net.mcreator.crustychunks.network;

import net.mcreator.crustychunks.procedures.RKNProcedure;
import net.mcreator.crustychunks.procedures.RKYProcedure;
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
public record RightPullMessage(int eventType, int pressedms) implements CustomPacketPayload {
   public static final Type<RightPullMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "key_right_pull"));
   public static final StreamCodec<RegistryFriendlyByteBuf, RightPullMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, RightPullMessage message) -> {
      buffer.writeInt(message.eventType);
      buffer.writeInt(message.pressedms);
   }, (RegistryFriendlyByteBuf buffer) -> new RightPullMessage(buffer.readInt(), buffer.readInt()));

   @Override
   public Type<RightPullMessage> type() {
      return TYPE;
   }

   public static void handleData(final RightPullMessage message, final IPayloadContext context) {
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
            RKYProcedure.execute(entity);
         }

         if (type == 1) {
            RKNProcedure.execute(entity);
         }
      }
   }

   @SubscribeEvent
   public static void registerMessage(FMLCommonSetupEvent event) {
      CrustyChunksMod.addNetworkMessage(RightPullMessage.TYPE, RightPullMessage.STREAM_CODEC, RightPullMessage::handleData);
   }
}
