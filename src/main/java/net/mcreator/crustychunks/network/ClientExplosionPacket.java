package net.mcreator.crustychunks.network;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.procedures.WariumExplosionClientProcedure;
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
 * Clientbound explosion VFX trigger (1.3.0 explosion rework). The server only
 * broadcasts position/power/type; all particle work happens client-side in
 * {@link WariumExplosionClientProcedure}. Referencing the nested BlastType enum
 * does not classload the (client-only) outer procedure on dedicated servers.
 */
public record ClientExplosionPacket(double x, double y, double z, double power, WariumExplosionClientProcedure.BlastType blastType) implements CustomPacketPayload {
	public static final Type<ClientExplosionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "client_explosion"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientExplosionPacket> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buf, ClientExplosionPacket msg) -> {
				buf.writeDouble(msg.x);
				buf.writeDouble(msg.y);
				buf.writeDouble(msg.z);
				buf.writeDouble(msg.power);
				buf.writeEnum(msg.blastType);
			},
			(RegistryFriendlyByteBuf buf) -> new ClientExplosionPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
					buf.readEnum(WariumExplosionClientProcedure.BlastType.class)));

	@Override
	public Type<ClientExplosionPacket> type() {
		return TYPE;
	}

	public static void handleData(final ClientExplosionPacket message, final IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> {
			if (FMLEnvironment.dist == Dist.CLIENT)
				WariumExplosionClientProcedure.execute(net.minecraft.client.Minecraft.getInstance().level, message.x(), message.y(), message.z(), message.power(), message.blastType());
		});
	}

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class Registration {
		@SubscribeEvent
		public static void register(FMLCommonSetupEvent event) {
			CrustyChunksMod.addNetworkMessage(TYPE, STREAM_CODEC, ClientExplosionPacket::handleData);
		}
	}
}
