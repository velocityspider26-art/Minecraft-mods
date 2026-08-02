package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetSurfaceTextures;
import shipwrights.genesis.space.surface.PlanetSurfaceData;

/**
 * Carries a planet's sampled surface to the clients that can see it — both what
 * it looks like and what shape it is.
 *
 * <p>Sent once per planet per session rather than continuously: the terrain a
 * generator produces does not change, and the parts that do change (what
 * players build) move far too slowly to be worth streaming.</p>
 */
public record PlanetSurfacePacket(ResourceLocation planet, int[] colours, short[] heights,
                                  int minHeight, int maxHeight, int seaLevel) implements CustomPacketPayload {
    public static final Type<PlanetSurfacePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_surface"));

    public static PlanetSurfacePacket of(ResourceLocation planet, PlanetSurfaceData data) {
        return new PlanetSurfacePacket(planet, data.colours(), data.heights(),
                data.minHeight(), data.maxHeight(), data.seaLevel());
    }

    public PlanetSurfaceData toData() {
        return new PlanetSurfaceData(colours, heights, minHeight, maxHeight, seaLevel);
    }

    public static final StreamCodec<ByteBuf, PlanetSurfacePacket> STREAM_CODEC = StreamCodec.of(
            PlanetSurfacePacket::write, PlanetSurfacePacket::read);

    private static void write(ByteBuf buffer, PlanetSurfacePacket packet) {
        ResourceLocation.STREAM_CODEC.encode(new net.minecraft.network.FriendlyByteBuf(buffer), packet.planet());
        ByteBufCodecs.VAR_INT.encode(buffer, packet.colours().length);
        for (int colour : packet.colours()) {
            buffer.writeInt(colour);
        }
        for (short height : packet.heights()) {
            buffer.writeShort(height);
        }
        buffer.writeInt(packet.minHeight());
        buffer.writeInt(packet.maxHeight());
        buffer.writeInt(packet.seaLevel());
    }

    private static PlanetSurfacePacket read(ByteBuf buffer) {
        ResourceLocation planet = ResourceLocation.STREAM_CODEC
                .decode(new net.minecraft.network.FriendlyByteBuf(buffer));
        int length = ByteBufCodecs.VAR_INT.decode(buffer);
        // A hostile or broken sender must not be able to make us allocate an
        // arbitrary array before a single byte of payload has been read.
        if (length < 0 || length > PlanetSurfaceData.expectedLength()) {
            throw new IllegalArgumentException("Implausible planet surface length " + length);
        }
        int[] colours = new int[length];
        for (int i = 0; i < length; i++) {
            colours[i] = buffer.readInt();
        }
        short[] heights = new short[length];
        for (int i = 0; i < length; i++) {
            heights[i] = buffer.readShort();
        }
        return new PlanetSurfacePacket(planet, colours, heights, buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanetSurfacePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> PlanetSurfaceTextures.accept(packet.planet(), packet.toData()));
    }
}
