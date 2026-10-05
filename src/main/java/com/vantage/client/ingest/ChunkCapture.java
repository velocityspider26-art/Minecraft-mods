package com.vantage.client.ingest;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * A copy of a live client chunk, taken on the main thread so workers never touch data the game is
 * modifying. Copying is cheap (palette + packed array per section); decoding happens later on a
 * worker in {@link #toSource()}.
 */
public final class ChunkCapture {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final int chunkX;
    private final int chunkZ;
    private final boolean lightValid;
    private final PalettedContainer<BlockState>[] states;
    private final boolean[] airOnly;
    private final Holder<Biome>[][] biomes;
    private final DataLayer[] sky;
    private final DataLayer[] block;

    @SuppressWarnings("unchecked")
    private ChunkCapture(int chunkX, int chunkZ, int n, boolean lightValid) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.lightValid = lightValid;
        this.states = new PalettedContainer[n];
        this.airOnly = new boolean[n];
        this.biomes = new Holder[n][];
        this.sky = new DataLayer[n];
        this.block = new DataLayer[n];
    }

    public static boolean lightReady(ClientLevel level, int chunkX, int chunkZ) {
        return level.getLightEngine().lightOnInSection(SectionPos.of(chunkX, level.getMinSection(), chunkZ));
    }

    @SuppressWarnings("unchecked")
    public static ChunkCapture capture(ClientLevel level, LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        LevelLightEngine light = level.getLightEngine();
        ChunkCapture cap = new ChunkCapture(cx, cz, sections.length, lightReady(level, cx, cz));
        LayerLightEventListener skyLayer = light.getLayerListener(LightLayer.SKY);
        LayerLightEventListener blockLayer = light.getLayerListener(LightLayer.BLOCK);
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection s = sections[i];
            if (s == null) {
                continue;
            }
            SectionPos pos = SectionPos.of(cx, level.getSectionYFromSectionIndex(i), cz);
            DataLayer sl = skyLayer.getDataLayerData(pos);
            DataLayer bl = blockLayer.getDataLayerData(pos);
            cap.sky[i] = sl == null ? null : sl.copy();
            cap.block[i] = bl == null ? null : bl.copy();
            if (s.hasOnlyAir()) {
                cap.airOnly[i] = true;
            } else {
                cap.states[i] = s.getStates().copy();
            }
            PalettedContainerRO<Holder<Biome>> b = s.getBiomes();
            Holder<Biome>[] cells = new Holder[64];
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        cells[(y << 4) | (z << 2) | x] = b.get(x, y, z);
                    }
                }
            }
            cap.biomes[i] = cells;
        }
        return cap;
    }

    public int chunkX() {
        return this.chunkX;
    }

    public int chunkZ() {
        return this.chunkZ;
    }

    /** Decodes the copy into a {@link ColumnSource}. Runs on a worker. */
    public ColumnSource toSource() {
        int n = this.states.length;
        ColumnSource src = new ColumnSource(this.chunkX, this.chunkZ, n, this.lightValid);
        Reference2IntOpenHashMap<BlockState> lookup = new Reference2IntOpenHashMap<>();
        lookup.defaultReturnValue(-1);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (this.biomes[i] == null) {
                continue;
            }
            byte[] skyData = this.sky[i] == null ? null : this.sky[i].getData();
            byte[] blockData = this.block[i] == null || this.block[i].isEmpty() ? null : this.block[i].getData();
            if (this.airOnly[i] || this.states[i] == null) {
                src.sections[i] = new ColumnSource.Section(new BlockState[]{AIR}, null, this.biomes[i], skyData, blockData);
                continue;
            }
            lookup.clear();
            palette.clear();
            short[] indices = new short[4096];
            int[] at = {0};
            this.states[i].getAll(state -> {
                int p = lookup.getInt(state);
                if (p < 0) {
                    p = palette.size();
                    palette.add(state);
                    lookup.put(state, p);
                }
                indices[at[0]++] = (short) p;
            });
            BlockState[] pal = palette.toArray(new BlockState[0]);
            src.sections[i] = new ColumnSource.Section(pal, pal.length == 1 ? null : indices, this.biomes[i], skyData, blockData);
        }
        return src;
    }
}
