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
 * Clientbound positional sound cue with distance-based delay/attenuation handled
 * on the client (speed-of-sound simulation). Only the physical client touches
 * {@link net.mcreator.crustychunks.procedures.ClientSoundUtils}.
 */
public record WariumSoundEvent(double x, double y, double z, String soundPath, float volume, float pitch) implements CustomPacketPayload {
	public static final Type<WariumSoundEvent> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "warium_sound_event"));

	public static final StreamCodec<RegistryFriendlyByteBuf, WariumSoundEvent> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buf, WariumSoundEvent msg) -> {
				buf.writeDouble(msg.x);
				buf.writeDouble(msg.y);
				buf.writeDouble(msg.z);
				buf.writeUtf(msg.soundPath);
				buf.writeFloat(msg.volume);
				buf.writeFloat(msg.pitch);
			},
			(RegistryFriendlyByteBuf buf) -> new WariumSoundEvent(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUtf(), buf.readFloat(), buf.readFloat()));

	@Override
	public Type<WariumSoundEvent> type() {
		return TYPE;
	}

	public static void handleData(final WariumSoundEvent message, final IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> {
			if (FMLEnvironment.dist == Dist.CLIENT)
				net.mcreator.crustychunks.procedures.ClientSoundUtils.playSoundAtDistance(message.x(), message.y(), message.z(), message.soundPath(), message.volume(), message.pitch());
		});
	}

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class Registration {
		@SubscribeEvent
		public static void register(FMLCommonSetupEvent event) {
			CrustyChunksMod.addNetworkMessage(TYPE, STREAM_CODEC, WariumSoundEvent::handleData);
		}
	}
}
