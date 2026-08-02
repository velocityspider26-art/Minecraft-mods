package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.client.lod.PlanetVoxelClientCache;
import shipwrights.genesis.space.voxel.PlanetVoxelBrick;
import shipwrights.genesis.space.voxel.PlanetVoxelAuthority;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickKey;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

/** RLE-compressed server-to-client update for one voxel brick. */
public record PlanetVoxelBrickPacket(
        ResourceLocation planet,
        PlanetVoxelBrickKey key,
        long revision,
        boolean remove,
        long[] palette,
        short[] indices,
        byte[] coverage,
        byte[] authority) implements CustomPacketPayload {

    public static final Type<PlanetVoxelBrickPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("genesis", "planet_voxel_brick"));
    public static final StreamCodec<ByteBuf, PlanetVoxelBrickPacket> STREAM_CODEC =
            StreamCodec.of(PlanetVoxelBrickPacket::write, PlanetVoxelBrickPacket::read);

    public static PlanetVoxelBrickPacket put(ResourceLocation planet, PlanetVoxelBrick brick) {
        return new PlanetVoxelBrickPacket(planet, brick.key(), brick.revision(), false,
                brick.paletteCopy(), brick.indicesCopy(), brick.coverageCopy(),
                brick.authorityCopy());
    }

    public static PlanetVoxelBrickPacket remove(ResourceLocation planet,
                                                 PlanetVoxelBrickKey key, long revision) {
        return new PlanetVoxelBrickPacket(planet, key, revision, true,
                new long[0], new short[0], new byte[0], new byte[0]);
    }

    private static void write(ByteBuf raw, PlanetVoxelBrickPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        ResourceLocation.STREAM_CODEC.encode(buffer, packet.planet);
        ByteBufCodecs.VAR_INT.encode(raw, packet.key.face().ordinal());
        raw.writeByte(packet.key.lod());
        raw.writeInt(packet.key.brickU());
        raw.writeInt(packet.key.brickY());
        raw.writeInt(packet.key.brickV());
        raw.writeLong(packet.revision);
        raw.writeBoolean(packet.remove);
        if (packet.remove) return;

        ByteBufCodecs.VAR_INT.encode(raw, packet.palette.length);
        for (long material : packet.palette) raw.writeLong(material);

        int runs = countRuns(packet.indices, packet.coverage, packet.authority);
        ByteBufCodecs.VAR_INT.encode(raw, runs);
        int cursor = 0;
        while (cursor < PlanetVoxelBrick.CELL_COUNT) {
            short index = packet.indices[cursor];
            byte amount = packet.coverage[cursor];
            byte source = packet.authority[cursor];
            int end = cursor + 1;
            while (end < PlanetVoxelBrick.CELL_COUNT
                    && packet.indices[end] == index && packet.coverage[end] == amount
                    && packet.authority[end] == source) end++;
            ByteBufCodecs.VAR_INT.encode(raw, end - cursor);
            ByteBufCodecs.VAR_INT.encode(raw, Short.toUnsignedInt(index));
            raw.writeByte(amount);
            raw.writeByte(source);
            cursor = end;
        }
    }

    private static PlanetVoxelBrickPacket read(ByteBuf raw) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        ResourceLocation planet = ResourceLocation.STREAM_CODEC.decode(buffer);
        int faceOrdinal = ByteBufCodecs.VAR_INT.decode(raw);
        CubeNetSurfaceTransform.Face[] faces = CubeNetSurfaceTransform.Face.values();
        if (faceOrdinal < 0 || faceOrdinal >= faces.length) {
            throw new IllegalArgumentException("invalid voxel face " + faceOrdinal);
        }
        int lod = raw.readUnsignedByte();
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(faces[faceOrdinal], lod,
                raw.readInt(), raw.readInt(), raw.readInt());
        long revision = raw.readLong();
        boolean remove = raw.readBoolean();
        if (remove) {
            return remove(planet, key, revision);
        }

        int paletteLength = ByteBufCodecs.VAR_INT.decode(raw);
        if (paletteLength <= 0 || paletteLength > PlanetVoxelBrick.MAX_PALETTE) {
            throw new IllegalArgumentException("invalid voxel palette " + paletteLength);
        }
        long[] palette = new long[paletteLength];
        for (int i = 0; i < paletteLength; i++) palette[i] = raw.readLong();

        int runCount = ByteBufCodecs.VAR_INT.decode(raw);
        if (runCount <= 0 || runCount > PlanetVoxelBrick.CELL_COUNT) {
            throw new IllegalArgumentException("invalid voxel run count " + runCount);
        }
        short[] indices = new short[PlanetVoxelBrick.CELL_COUNT];
        byte[] coverage = new byte[PlanetVoxelBrick.CELL_COUNT];
        byte[] authority = new byte[PlanetVoxelBrick.CELL_COUNT];
        int cursor = 0;
        for (int run = 0; run < runCount; run++) {
            int length = ByteBufCodecs.VAR_INT.decode(raw);
            int paletteIndex = ByteBufCodecs.VAR_INT.decode(raw);
            byte amount = raw.readByte();
            byte source = raw.readByte();
            PlanetVoxelAuthority.fromCode(source);
            if (length <= 0 || cursor + length > PlanetVoxelBrick.CELL_COUNT
                    || paletteIndex < 0 || paletteIndex >= paletteLength) {
                throw new IllegalArgumentException("invalid voxel RLE run");
            }
            for (int i = 0; i < length; i++) {
                indices[cursor] = (short) paletteIndex;
                coverage[cursor] = amount;
                authority[cursor] = source;
                cursor++;
            }
        }
        if (cursor != PlanetVoxelBrick.CELL_COUNT) {
            throw new IllegalArgumentException("voxel RLE produced " + cursor + " cells");
        }
        return new PlanetVoxelBrickPacket(planet, key, revision, false,
                palette, indices, coverage, authority);
    }

    private static int countRuns(short[] indices, byte[] coverage, byte[] authority) {
        if (indices.length != PlanetVoxelBrick.CELL_COUNT
                || coverage.length != PlanetVoxelBrick.CELL_COUNT
                || authority.length != PlanetVoxelBrick.CELL_COUNT) {
            throw new IllegalArgumentException("invalid voxel packet arrays");
        }
        int runs = 1;
        for (int i = 1; i < indices.length; i++) {
            if (indices[i] != indices[i - 1] || coverage[i] != coverage[i - 1]
                    || authority[i] != authority[i - 1]) runs++;
        }
        return runs;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanetVoxelBrickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.remove) {
                PlanetVoxelClientCache.remove(packet.planet, packet.key, packet.revision);
            } else {
                PlanetVoxelBrick brick = new PlanetVoxelBrick(packet.key, packet.revision,
                        packet.palette, packet.indices, packet.coverage, packet.authority);
                PlanetVoxelClientCache.accept(packet.planet, brick);
                shipwrights.genesis.space.planet.PlanetRenderDiagnostics
                        .onBrickReceived(brick.estimatedBytes());
            }
        });
    }
}
