package com.vantage.client.gen;

import com.vantage.client.ingest.RegionImporter;
import com.vantage.core.Lod;
import com.vantage.gen.BiomeLook;
import com.vantage.gen.ColumnSampler;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Samples the singleplayer world generator directly (the integrated server runs in this game).
 * While the save's first import pass runs, places the save already holds are left to the import.
 */
public final class LocalSource implements DistantGenerator.Source {
    private final ColumnSampler sampler;
    private final Registry<Biome> clientBiomes;
    private final Supplier<@Nullable RegionImporter> importer;

    public LocalSource(ServerLevel level, Registry<Biome> clientBiomes, Supplier<@Nullable RegionImporter> importer) {
        this.sampler = new ColumnSampler(level);
        this.clientBiomes = clientBiomes;
        this.importer = importer;
    }

    @Override
    public boolean local() {
        return true;
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int seaLevel() {
        return this.sampler.seaLevel();
    }

    @Override
    public BlockState stone() {
        return this.sampler.defaultBlock();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sample(int level, int sx, int sz, boolean[] columns, Consumer<DistantGenerator.Samples> done) {
        int[] heights = new int[Lod.AREA];
        Holder<Biome>[] server = new Holder[Lod.AREA];
        RegionImporter imp = this.importer.get();
        this.sampler.sample(level, sx, sz, columns, heights, server, imp == null ? null : imp::pendingAt);
        Holder<Biome>[] client = new Holder[Lod.AREA];
        BiomeLook[] looks = new BiomeLook[Lod.AREA];
        for (int i = 0; i < Lod.AREA; i++) {
            if (heights[i] == Integer.MIN_VALUE) {
                continue;
            }
            Holder<Biome> biome = server[i];
            // The client has its own copy of the biome registry; tints come from that one.
            client[i] = biome.unwrapKey().flatMap(this.clientBiomes::getHolder).<Holder<Biome>>map(h -> h).orElse(biome);
            looks[i] = this.sampler.look(biome);
        }
        done.accept(new DistantGenerator.Samples(heights, client, looks));
    }
}
