package net.mcreator.crustychunks.network;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Clientbound request to shake the camera. Purely visual; the handler only runs
 * on the physical client, so dedicated servers never touch client classes.
 */
public record ScreenShakeMessage(float intensity, int durationTicks) implements CustomPacketPayload {
	public static final Type<ScreenShakeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "screen_shake"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ScreenShakeMessage> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buf, ScreenShakeMessage msg) -> {
				buf.writeFloat(msg.intensity);
				buf.writeInt(msg.durationTicks);
			},
			(RegistryFriendlyByteBuf buf) -> new ScreenShakeMessage(buf.readFloat(), buf.readInt()));

	@Override
	public Type<ScreenShakeMessage> type() {
		return TYPE;
	}

	public static void handleData(final ScreenShakeMessage message, final IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> {
			if (FMLEnvironment.dist == Dist.CLIENT)
				net.mcreator.crustychunks.client.WariumScreenShake.add(message.intensity(), message.durationTicks());
		});
	}

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class Registration {
		@SubscribeEvent
		public static void register(FMLCommonSetupEvent event) {
			CrustyChunksMod.addNetworkMessage(TYPE, STREAM_CODEC, ScreenShakeMessage::handleData);
		}
	}
}
