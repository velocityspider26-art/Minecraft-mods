package com.vantage.client.gen;

import com.vantage.client.net.ClientTerrain;
import com.vantage.core.Lod;
import com.vantage.gen.BiomeLook;
import com.vantage.net.PlanetInfo;
import com.vantage.net.TerrainColumns;
import com.vantage.net.TerrainRequest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/** Asks the server (which has the world generator) for terrain samples. */
public final class RemoteSource implements DistantGenerator.Source {
    private static final int CAPACITY = 16;

    private final ResourceKey<Level> dimension;
    private final Registry<Biome> biomes;
    private final int seaLevel;
    private final BlockState stone;

    public RemoteSource(ResourceKey<Level> dimension, Registry<Biome> biomes, PlanetInfo info) {
        this.dimension = dimension;
        this.biomes = biomes;
        this.seaLevel = info.seaLevel();
        this.stone = Block.stateById(info.stone());
    }

    @Override
    public boolean local() {
        return false;
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }

    @Override
    public int seaLevel() {
        return this.seaLevel;
    }

    @Override
    public BlockState stone() {
        return this.stone;
    }

    @Override
    public void sample(int level, int sx, int sz, boolean[] columns, Consumer<DistantGenerator.Samples> done) {
        TerrainRequest request = new TerrainRequest(this.dimension, level, sx, sz, TerrainRequest.mask(columns));
        boolean sent = ClientTerrain.INSTANCE.send(request, answer -> done.accept(answer == null || answer.status() != TerrainColumns.OK
                ? null : this.decode(answer)));
        if (!sent) {
            done.accept(null);
        }
    }

    @SuppressWarnings("unchecked")
    private DistantGenerator.Samples decode(TerrainColumns a) {
        int p = a.palette().length;
        Holder<Biome>[] paletteBiomes = new Holder[p];
        BiomeLook[] paletteLooks = new BiomeLook[p];
        for (int i = 0; i < p; i++) {
            paletteBiomes[i] = this.biomes.getHolder(a.palette()[i]).<Holder<Biome>>map(h -> h).orElse(null);
            int[] l = a.looks();
            paletteLooks[i] = new BiomeLook(state(l[i * 4]), state(l[i * 4 + 1]), state(l[i * 4 + 2]),
                    l[i * 4 + 3] == 0 ? null : state(l[i * 4 + 3]), a.canopy()[i], a.treeHeight()[i]);
        }
        boolean[] sampled = TerrainRequest.unmask(a.columns());
        int[] heights = new int[Lod.AREA];
        Holder<Biome>[] biomes = new Holder[Lod.AREA];
        BiomeLook[] looks = new BiomeLook[Lod.AREA];
        int k = 0;
        for (int i = 0; i < Lod.AREA; i++) {
            heights[i] = Integer.MIN_VALUE;
            if (!sampled[i] || k >= a.heights().length) {
                continue;
            }
            int b = a.biomes()[k] & 0xFF;
            short h = a.heights()[k++];
            if (b >= p || paletteBiomes[b] == null) {
                continue;
            }
            heights[i] = h;
            biomes[i] = paletteBiomes[b];
            looks[i] = paletteLooks[b];
        }
        return new DistantGenerator.Samples(heights, biomes, looks);
    }

    private static BlockState state(int idPlusOne) {
        return Block.stateById(Math.max(0, idPlusOne - 1));
    }
}
