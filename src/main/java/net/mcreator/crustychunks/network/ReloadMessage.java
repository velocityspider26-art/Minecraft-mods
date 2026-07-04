package net.mcreator.crustychunks.network;

import net.mcreator.crustychunks.procedures.ReloadOnKeyPressedProcedure;
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
public record ReloadMessage(int eventType, int pressedms) implements CustomPacketPayload {
   public static final Type<ReloadMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "key_reload"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReloadMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ReloadMessage message) -> {
      buffer.writeInt(message.eventType);
      buffer.writeInt(message.pressedms);
   }, (RegistryFriendlyByteBuf buffer) -> new ReloadMessage(buffer.readInt(), buffer.readInt()));

   @Override
   public Type<ReloadMessage> type() {
      return TYPE;
   }

   public static void handleData(final ReloadMessage message, final IPayloadContext context) {
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
            ReloadOnKeyPressedProcedure.execute(world, x, y, z, entity);
         }
      }
   }

   @SubscribeEvent
   public static void registerMessage(FMLCommonSetupEvent event) {
      CrustyChunksMod.addNetworkMessage(ReloadMessage.TYPE, ReloadMessage.STREAM_CODEC, ReloadMessage::handleData);
   }
}
