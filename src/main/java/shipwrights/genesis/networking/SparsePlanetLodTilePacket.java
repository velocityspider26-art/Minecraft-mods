package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.lod.SparsePlanetLodClientCache;
import shipwrights.genesis.space.surface.SparsePlanetLodTile;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

/** Streams one sparse LOD patch; packets are small and independently replaceable. */
public record SparsePlanetLodTilePacket(
        ResourceLocation planet,
        int faceOrdinal,
        int originX,
        int originZ,
        int cellSize,
        boolean exact,
        long revision,
        short[] heights,
        int[] colours) implements CustomPacketPayload {

    public static final Type<SparsePlanetLodTilePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sparse_planet_lod_tile"));

    public static final StreamCodec<ByteBuf, SparsePlanetLodTilePacket> STREAM_CODEC =
            StreamCodec.of(SparsePlanetLodTilePacket::write, SparsePlanetLodTilePacket::read);

    public static SparsePlanetLodTilePacket of(ResourceLocation planet, SparsePlanetLodTile tile) {
        return new SparsePlanetLodTilePacket(
                planet, tile.face().ordinal(), tile.originX(), tile.originZ(), tile.cellSize(),
                tile.exact(), tile.revision(), tile.heights(), tile.colours());
    }

    public SparsePlanetLodTile toTile() {
        CubeNetSurfaceTransform.Face[] faces = CubeNetSurfaceTransform.Face.values();
        if (faceOrdinal < 0 || faceOrdinal >= faces.length) {
            throw new IllegalArgumentException("Invalid cube face ordinal " + faceOrdinal);
        }
        return new SparsePlanetLodTile(faces[faceOrdinal], originX, originZ, cellSize,
                exact, revision, heights, colours);
    }

    private static void write(ByteBuf raw, SparsePlanetLodTilePacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        ResourceLocation.STREAM_CODEC.encode(buffer, packet.planet());
        ByteBufCodecs.VAR_INT.encode(raw, packet.faceOrdinal());
        raw.writeInt(packet.originX());
        raw.writeInt(packet.originZ());
        ByteBufCodecs.VAR_INT.encode(raw, packet.cellSize());
        raw.writeBoolean(packet.exact());
        raw.writeLong(packet.revision());
        for (short height : packet.heights()) {
            raw.writeShort(height);
        }
        for (int colour : packet.colours()) {
            raw.writeInt(colour);
        }
    }

    private static SparsePlanetLodTilePacket read(ByteBuf raw) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        ResourceLocation planet = ResourceLocation.STREAM_CODEC.decode(buffer);
        int face = ByteBufCodecs.VAR_INT.decode(raw);
        int originX = raw.readInt();
        int originZ = raw.readInt();
        int cellSize = ByteBufCodecs.VAR_INT.decode(raw);
        boolean exact = raw.readBoolean();
        long revision = raw.readLong();
        short[] heights = new short[SparsePlanetLodTile.CELL_COUNT];
        int[] colours = new int[SparsePlanetLodTile.CELL_COUNT];
        for (int i = 0; i < heights.length; i++) {
            heights[i] = raw.readShort();
        }
        for (int i = 0; i < colours.length; i++) {
            colours[i] = raw.readInt();
        }
        return new SparsePlanetLodTilePacket(planet, face, originX, originZ, cellSize,
                exact, revision, heights, colours);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SparsePlanetLodTilePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> SparsePlanetLodClientCache.accept(packet.planet(), packet.toTile()));
    }
}
