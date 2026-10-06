package com.vantage.world;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Reads chunks as Minecraft saves them (region file NBT) into {@link ColumnSource}s. Thread-safe. */
public final class ChunkNbt {
    private final Registry<Biome> biomes;
    private final Holder<Biome> fallback;
    private final int minSection;
    private final int sectionCount;
    private final int dataVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();

    public ChunkNbt(Registry<Biome> biomes, Holder<Biome> fallback, int minSection, int sectionCount) {
        this.biomes = biomes;
        this.fallback = fallback;
        this.minSection = minSection;
        this.sectionCount = sectionCount;
    }

    /**
     * The chunk's blocks, biomes and light, or null if it cannot be read or is not far enough
     * along: fully generated, or with {@code decoratedIsEnough} also once its features are placed
     * (chunks around explored land often stop there, and look just as they will).
     */
    public @Nullable ColumnSource parse(CompoundTag tag, int cx, int cz, boolean decoratedIsEnough) {
        try {
            int version = tag.contains("DataVersion", Tag.TAG_ANY_NUMERIC) ? tag.getInt("DataVersion") : 0;
            if (version < this.dataVersion) {
                tag = DataFixTypes.CHUNK.updateToCurrentVersion(DataFixers.getDataFixer(), tag, version);
            }
            String status = tag.getString("Status");
            if (!status.endsWith("full")) {
                ChunkStatus s = decoratedIsEnough ? ChunkStatus.byName(status) : null;
                if (s == null || !s.isOrAfter(ChunkStatus.FEATURES)) {
                    return null;
                }
            }
            boolean light = tag.getBoolean("isLightOn");
            ColumnSource src = new ColumnSource(cx, cz, this.sectionCount, light);
            ListTag sections = tag.getList("sections", Tag.TAG_COMPOUND);
            Map<String, Holder<Biome>> biomeCache = new HashMap<>();
            for (int s = 0; s < sections.size(); s++) {
                CompoundTag sec = sections.getCompound(s);
                int idx = sec.getByte("Y") - this.minSection;
                if (idx < 0 || idx >= this.sectionCount) {
                    continue;
                }
                src.sections[idx] = this.parseSection(sec, light, biomeCache);
            }
            return src;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ColumnSource.Section parseSection(CompoundTag sec, boolean light, Map<String, Holder<Biome>> biomeCache) {
        BlockState[] palette;
        short[] indices = null;
        if (sec.contains("block_states", Tag.TAG_COMPOUND)) {
            CompoundTag bs = sec.getCompound("block_states");
            ListTag pal = bs.getList("palette", Tag.TAG_COMPOUND);
            palette = new BlockState[Math.max(1, pal.size())];
            if (pal.isEmpty()) {
                palette[0] = Blocks.AIR.defaultBlockState();
            }
            for (int i = 0; i < pal.size(); i++) {
                palette[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), pal.getCompound(i));
            }
            if (palette.length > 1 && bs.contains("data", Tag.TAG_LONG_ARRAY)) {
                indices = unpack(bs.getLongArray("data"), Math.max(4, ceilLog2(palette.length)), 4096, palette.length);
            }
        } else {
            palette = new BlockState[]{Blocks.AIR.defaultBlockState()};
        }

        Holder<Biome>[] cells = new Holder[64];
        Arrays.fill(cells, this.fallback);
        if (sec.contains("biomes", Tag.TAG_COMPOUND)) {
            CompoundTag b = sec.getCompound("biomes");
            ListTag pal = b.getList("palette", Tag.TAG_STRING);
            Holder<Biome>[] hp = new Holder[pal.size()];
            for (int i = 0; i < pal.size(); i++) {
                String name = pal.getString(i);
                hp[i] = biomeCache.computeIfAbsent(name, n -> {
                    ResourceLocation loc = ResourceLocation.tryParse(n);
                    if (loc == null) {
                        return this.fallback;
                    }
                    return this.biomes.getHolder(ResourceKey.create(Registries.BIOME, loc)).map(h -> (Holder<Biome>) h).orElse(this.fallback);
                });
            }
            if (hp.length == 1) {
                Arrays.fill(cells, hp[0]);
            } else if (hp.length > 1 && b.contains("data", Tag.TAG_LONG_ARRAY)) {
                short[] bi = unpack(b.getLongArray("data"), ceilLog2(hp.length), 64, hp.length);
                for (int i = 0; i < 64; i++) {
                    cells[i] = hp[bi[i]];
                }
            }
        }
        byte[] sky = light && sec.contains("SkyLight", Tag.TAG_BYTE_ARRAY) ? sec.getByteArray("SkyLight") : null;
        byte[] block = light && sec.contains("BlockLight", Tag.TAG_BYTE_ARRAY) ? sec.getByteArray("BlockLight") : null;
        if (sky != null && sky.length != 2048) {
            sky = null;
        }
        if (block != null && block.length != 2048) {
            block = null;
        }
        return new ColumnSource.Section(palette, indices, cells, sky, block);
    }

    /** Unpacks Minecraft's packed storage (entries never span two longs). */
    public static short[] unpack(long[] data, int bits, int count, int paletteSize) {
        short[] out = new short[count];
        if (bits <= 0) {
            return out;
        }
        int perLong = 64 / bits;
        long mask = (1L << bits) - 1;
        for (int i = 0; i < count; i++) {
            int li = i / perLong;
            if (li >= data.length) {
                break;
            }
            int v = (int) ((data[li] >>> ((i % perLong) * bits)) & mask);
            out[i] = (short) (v < paletteSize ? v : 0);
        }
        return out;
    }

    public static int ceilLog2(int n) {
        return n <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(n - 1);
    }
}
