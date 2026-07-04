package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.function.Supplier;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber({Dist.CLIENT})
public class SemiAutoReleaseProcedure {
   private static boolean wasDown = false;

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      if (true) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || mc.screen != null) {
            return;
         }

         boolean isDown = mc.options.keyUse.isDown();
         if (wasDown && !isDown) {
            PacketDistributor.sendToServer(new SemiAutoReleaseProcedure.SemiAutoReleaseMessage());
         }

         wasDown = isDown;
      }
   }

   public static void execute(Entity entity) {
      try {
      if (entity != null) {
         {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
            _vars.clickrelease = true;
            _vars.syncPlayerVariables(entity);
         
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SemiAutoReleaseProcedure.execute", _wtSafe);
      }
   }

   public record SemiAutoReleaseMessage() implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<SemiAutoReleaseMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "semi_auto_release_message"));
      public static final StreamCodec<RegistryFriendlyByteBuf, SemiAutoReleaseMessage> STREAM_CODEC = StreamCodec.unit(new SemiAutoReleaseMessage());

      @Override
      public Type<SemiAutoReleaseMessage> type() {
         return TYPE;
      }

      public static void handleData(final SemiAutoReleaseMessage message, final IPayloadContext context) {
         if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
               Entity sender = context.player();
               if (sender instanceof net.minecraft.server.level.ServerPlayer _sp && _sp.isAlive() && !_sp.isSpectator()) {
                  SemiAutoReleaseProcedure.execute(sender);
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
         CrustyChunksMod.addNetworkMessage(SemiAutoReleaseMessage.TYPE, SemiAutoReleaseMessage.STREAM_CODEC, SemiAutoReleaseMessage::handleData);
      }
   }
}
