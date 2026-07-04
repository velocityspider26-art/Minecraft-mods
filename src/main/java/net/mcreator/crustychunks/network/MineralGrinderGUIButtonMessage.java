package net.mcreator.crustychunks.network;

import java.util.HashMap;
import net.mcreator.crustychunks.procedures.MineralGrinderProcessProcedure;
import net.mcreator.crustychunks.world.inventory.MineralGrinderGUIMenu;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.core.BlockPos;
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
public record MineralGrinderGUIButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {
   public static final Type<MineralGrinderGUIButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "mineral_grinder_gui_buttons"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MineralGrinderGUIButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, MineralGrinderGUIButtonMessage message) -> {
      buffer.writeInt(message.buttonID);
      buffer.writeInt(message.x);
      buffer.writeInt(message.y);
      buffer.writeInt(message.z);
   }, (RegistryFriendlyByteBuf buffer) -> new MineralGrinderGUIButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

   @Override
   public Type<MineralGrinderGUIButtonMessage> type() {
      return TYPE;
   }

   public static void handleData(final MineralGrinderGUIButtonMessage message, final IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            Player entity = context.player();
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer _sender) || !_sender.isAlive() || _sender.isSpectator())
               return;
            handleButtonAction(entity, message.buttonID, message.x, message.y, message.z);
         }).exceptionally(e -> {
            context.connection().disconnect(Component.literal(e.getMessage()));
            return null;
         });
      }
   }

   public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
      Level world = entity.level();
      HashMap guistate = MineralGrinderGUIMenu.guistate;
      if (world.hasChunkAt(new BlockPos(x, y, z))) {
         if (buttonID == 0) {
            MineralGrinderProcessProcedure.execute(world, (double)x, (double)y, (double)z);
         }
      }
   }

   @SubscribeEvent
   public static void registerMessage(FMLCommonSetupEvent event) {
      CrustyChunksMod.addNetworkMessage(MineralGrinderGUIButtonMessage.TYPE, MineralGrinderGUIButtonMessage.STREAM_CODEC, MineralGrinderGUIButtonMessage::handleData);
   }
}
