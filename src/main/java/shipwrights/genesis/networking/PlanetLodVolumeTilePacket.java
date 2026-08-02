package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.lod.PlanetLodVolumeClientCache;
import shipwrights.genesis.space.surface.PlanetLodVolumeTile;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

/** Streams one exact 16x16 vertical-volume LOD tile. */
public record PlanetLodVolumeTilePacket(
        ResourceLocation planet,
        int faceOrdinal,
        int chunkX,
        int chunkZ,
        long revision,
        int[] columnOffsets,
        short[] bottomY,
        short[] topY,
        int[] blockStateIds,
        int[] colours,
        byte[] packedLight,
        byte[] flags) implements CustomPacketPayload {

    public static final Type<PlanetLodVolumeTilePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_lod_volume_tile"));
    public static final StreamCodec<ByteBuf, PlanetLodVolumeTilePacket> STREAM_CODEC =
            StreamCodec.of(PlanetLodVolumeTilePacket::write, PlanetLodVolumeTilePacket::read);

    public static PlanetLodVolumeTilePacket of(ResourceLocation planet, PlanetLodVolumeTile tile) {
        return new PlanetLodVolumeTilePacket(planet, tile.face().ordinal(), tile.chunkX(), tile.chunkZ(),
                tile.revision(), tile.columnOffsets(), tile.bottomY(), tile.topY(), tile.blockStateIds(),
                tile.colours(), tile.packedLight(), tile.flags());
    }

    public PlanetLodVolumeTile toTile() {
        CubeNetSurfaceTransform.Face[] faces = CubeNetSurfaceTransform.Face.values();
        if (faceOrdinal < 0 || faceOrdinal >= faces.length) {
            throw new IllegalArgumentException("Invalid cube face ordinal " + faceOrdinal);
        }
        return new PlanetLodVolumeTile(faces[faceOrdinal], chunkX, chunkZ, revision,
                columnOffsets, bottomY, topY, blockStateIds, colours, packedLight, flags);
    }

    private static void write(ByteBuf raw, PlanetLodVolumeTilePacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        ResourceLocation.STREAM_CODEC.encode(buffer, packet.planet());
        ByteBufCodecs.VAR_INT.encode(raw, packet.faceOrdinal());
        raw.writeInt(packet.chunkX());
        raw.writeInt(packet.chunkZ());
        raw.writeLong(packet.revision());
        int segments = packet.bottomY().length;
        ByteBufCodecs.VAR_INT.encode(raw, segments);
        for (int offset : packet.columnOffsets()) {
            raw.writeShort(offset);
        }
        for (int i = 0; i < segments; i++) {
            raw.writeShort(packet.bottomY()[i]);
            raw.writeShort(packet.topY()[i]);
            ByteBufCodecs.VAR_INT.encode(raw, packet.blockStateIds()[i]);
            raw.writeInt(packet.colours()[i]);
            raw.writeByte(packet.packedLight()[i]);
            raw.writeByte(packet.flags()[i]);
        }
    }

    private static PlanetLodVolumeTilePacket read(ByteBuf raw) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        ResourceLocation planet = ResourceLocation.STREAM_CODEC.decode(buffer);
        int face = ByteBufCodecs.VAR_INT.decode(raw);
        int chunkX = raw.readInt();
        int chunkZ = raw.readInt();
        long revision = raw.readLong();
        int segments = ByteBufCodecs.VAR_INT.decode(raw);
        if (segments < 0 || segments > PlanetLodVolumeTile.MAX_SEGMENTS) {
            throw new IllegalArgumentException("Invalid volume tile segment count " + segments);
        }
        int[] offsets = new int[PlanetLodVolumeTile.OFFSET_COUNT];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = raw.readUnsignedShort();
        }
        if (offsets[offsets.length - 1] != segments) {
            throw new IllegalArgumentException("Volume tile offsets do not match segment count");
        }
        short[] bottom = new short[segments];
        short[] top = new short[segments];
        int[] states = new int[segments];
        int[] colours = new int[segments];
        byte[] light = new byte[segments];
        byte[] flags = new byte[segments];
        for (int i = 0; i < segments; i++) {
            bottom[i] = raw.readShort();
            top[i] = raw.readShort();
            states[i] = ByteBufCodecs.VAR_INT.decode(raw);
            colours[i] = raw.readInt();
            light[i] = raw.readByte();
            flags[i] = raw.readByte();
        }
        return new PlanetLodVolumeTilePacket(planet, face, chunkX, chunkZ, revision,
                offsets, bottom, top, states, colours, light, flags);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanetLodVolumeTilePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> PlanetLodVolumeClientCache.accept(packet.planet(), packet.toTile()));
    }
}
