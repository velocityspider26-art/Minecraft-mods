package com.vantage.client.ingest;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Raw block, biome and light data of one chunk column, decoupled from where it came from (a live
 * client chunk or a region file on disk). Sections are indexed from the dimension floor.
 */
public final class ColumnSource {
    public final int chunkX;
    public final int chunkZ;
    public final Section[] sections;
    /** False if the light data cannot be trusted; then everything is treated as sky-lit. */
    public final boolean lightValid;

    public ColumnSource(int chunkX, int chunkZ, int sectionCount, boolean lightValid) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.sections = new Section[sectionCount];
        this.lightValid = lightValid;
    }

    /**
     * One 16³ section. {@code indices} is null when the palette has one entry. Light arrays use
     * Minecraft's nibble layout ({@code y<<8 | z<<4 | x}, low nibble first) and may be null.
     */
    public record Section(BlockState[] palette, short @Nullable [] indices, Holder<Biome>[] biomes,
                          byte @Nullable [] skyLight, byte @Nullable [] blockLight) {
        public static int nibble(byte[] data, int index) {
            return (data[index >> 1] >> ((index & 1) << 2)) & 0xF;
        }
    }
}
