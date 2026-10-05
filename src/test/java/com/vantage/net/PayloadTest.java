package com.vantage.net;

import com.vantage.core.Lod;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadTest {
    private static <T> T roundTrip(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        codec.encode(buf, value);
        T back = codec.decode(buf);
        assertEquals(0, buf.readableBytes(), "every byte should be read");
        return back;
    }

    @Test
    void requestsSurviveTheTrip() {
        boolean[] columns = new boolean[Lod.AREA];
        for (int i = 0; i < columns.length; i += 7) {
            columns[i] = true;
        }
        TerrainRequest r = new TerrainRequest(Level.OVERWORLD, 9, -1234567, 89, TerrainRequest.mask(columns));
        TerrainRequest back = roundTrip(TerrainRequest.CODEC, r);
        assertEquals(r.dimension(), back.dimension());
        assertEquals(9, back.level());
        assertEquals(-1234567, back.sx());
        assertEquals(89, back.sz());
        assertArrayEquals(columns, TerrainRequest.unmask(back.columns()));
    }

    @Test
    void columnsSurviveTheTrip() {
        int[] palette = new int[200];
        int[] looks = new int[palette.length * 4];
        float[] canopy = new float[palette.length];
        byte[] treeHeight = new byte[palette.length];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = i * 3;
            looks[i * 4] = i + 1;
            looks[i * 4 + 3] = i % 2 == 0 ? 0 : 20_000;
            canopy[i] = i / 200f;
            treeHeight[i] = (byte) (i % 30);
        }
        short[] heights = {-64, 63, 319, 2031};
        byte[] biomes = {0, 127, (byte) 128, (byte) 199};
        long[] mask = new long[TerrainRequest.MASK_LONGS];
        mask[0] = 0b1111;
        TerrainColumns c = new TerrainColumns(Level.NETHER, 3, 5, -6, TerrainColumns.OK, mask, heights, biomes, palette, looks,
                canopy, treeHeight);
        TerrainColumns back = roundTrip(TerrainColumns.CODEC, c);
        assertEquals(Level.NETHER, back.dimension());
        assertArrayEquals(mask, back.columns());
        assertArrayEquals(heights, back.heights());
        assertArrayEquals(biomes, back.biomes());
        assertEquals(199, back.biomes()[3] & 0xFF);
        assertArrayEquals(palette, back.palette());
        assertArrayEquals(looks, back.looks());
        assertArrayEquals(canopy, back.canopy());
        assertArrayEquals(treeHeight, back.treeHeight());

        TerrainColumns refusal = roundTrip(TerrainColumns.CODEC, TerrainColumns.refusal(
                new TerrainRequest(Level.END, 1, 2, 3, new long[TerrainRequest.MASK_LONGS]), TerrainColumns.BUSY));
        assertEquals(TerrainColumns.BUSY, refusal.status());
        assertEquals(0, refusal.heights().length);
    }

    @Test
    void oversizedColumnsAreRejected() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeResourceKey(Level.OVERWORLD);
        buf.writeByte(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeByte(0);
        for (int i = 0; i < TerrainRequest.MASK_LONGS; i++) {
            buf.writeLong(0);
        }
        buf.writeVarInt(Lod.AREA + 1);
        assertThrows(IllegalArgumentException.class, () -> TerrainColumns.CODEC.decode(buf));
    }

    @Test
    void planetInfoSurvivesTheTrip() {
        PlanetInfo p = new PlanetInfo(Level.OVERWORLD, 30000, true, 63, 1);
        assertEquals(p, roundTrip(PlanetInfo.CODEC, p));
    }
}
