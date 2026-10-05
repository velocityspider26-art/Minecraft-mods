package com.vantage.client.visual;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.vantage.Vantage;
import com.vantage.core.VisualClass;
import com.vantage.core.Voxel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns visual ids to {@code (BlockState, biome)} pairs and remembers them across sessions.
 *
 * <p>The biome only takes part for states whose colour depends on it, so a world typically needs a
 * few thousand ids. The mapping is appended to {@code visuals.txt} as ids are created; stored LOD
 * data refers to these ids, so the file must never be rewritten with different numbers.
 */
public final class VisualRegistry implements VisualClass.Table, AutoCloseable {
    /** One registered visual. {@code state} is null when the block no longer exists. */
    public record Entry(@Nullable BlockState state, @Nullable Holder<Biome> biome, byte cls, int[] colors) {
    }

    private record BiomeKeyed(BlockState state, ResourceKey<Biome> biome) {
    }

    private static final int[] MISSING_COLORS = {0xFF7F7F7F, 0xFF7F7F7F, 0xFF7F7F7F, 0xFF7F7F7F, 0xFF7F7F7F, 0xFF7F7F7F};
    /**
     * Filler shows only at cave mouths and at the edge of explored land, where it stands in for
     * rock; lighting from the neighbouring air darkens it inside caves.
     */
    private static final int[] FILLER_COLORS = {0xFF706E6C, 0xFF706E6C, 0xFF706E6C, 0xFF706E6C, 0xFF706E6C, 0xFF706E6C};

    private final VisualAnalyzer analyzer;
    private final Registry<Biome> biomes;
    private final Path file;
    private final ConcurrentHashMap<Object, Integer> ids = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private volatile Entry[] entries = new Entry[4096];
    private volatile byte[] classes = new byte[4096];
    private volatile int size = Voxel.FILLER_VID + 1;
    private final AtomicInteger colorRevision = new AtomicInteger();
    private BufferedWriter writer;

    public VisualRegistry(Path file, VisualAnalyzer analyzer, Registry<Biome> biomes) {
        this.file = file;
        this.analyzer = analyzer;
        this.biomes = biomes;
        this.entries[0] = new Entry(Blocks.AIR.defaultBlockState(), null, VisualClass.AIR, new int[6]);
        this.put(Voxel.FILLER_VID, new Entry(null, null, VisualClass.OPAQUE, FILLER_COLORS));
        this.load();
        try {
            Files.createDirectories(file.getParent());
            this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Vantage.LOGGER.error("Cannot write {}; LOD data will not survive a restart", file, e);
        }
    }

    public VisualAnalyzer analyzer() {
        return this.analyzer;
    }

    @Override
    public byte classOf(int vid) {
        byte[] c = this.classes;
        return vid < c.length ? c[vid] : VisualClass.AIR;
    }

    public @Nullable Entry entry(int vid) {
        Entry[] e = this.entries;
        return vid < e.length ? e[vid] : null;
    }

    /** One past the highest id in use. */
    public int size() {
        return this.size;
    }

    /** Bumped whenever existing colours change (resource reload). */
    public int colorRevision() {
        return this.colorRevision.get();
    }

    /**
     * Id for a state that is drawn as a voxel. Pass the biome only for biome-tinted states.
     */
    public int idFor(BlockState state, @Nullable Holder<Biome> biome) {
        Object key;
        if (biome != null && biome.unwrapKey().isPresent()) {
            key = new BiomeKeyed(state, biome.unwrapKey().get());
        } else {
            key = state;
            biome = null;
        }
        Integer id = this.ids.get(key);
        if (id != null) {
            return id;
        }
        Holder<Biome> b = biome;
        return this.ids.computeIfAbsent(key, k -> this.register(state, b));
    }

    private int register(BlockState state, @Nullable Holder<Biome> biome) {
        byte cls = this.analyzer.shape(state).cls();
        if (cls == VisualClass.AIR) {
            cls = VisualClass.OPAQUE;
        }
        int[] colors = this.analyzer.faceColors(state, biome);
        synchronized (this.lock) {
            int vid = this.size;
            if (vid > Voxel.MAX_VID) {
                Vantage.LOGGER.error("Out of visual ids; rendering {} as missing", state);
                return 1;
            }
            this.put(vid, new Entry(state, biome, cls, colors));
            this.size = vid + 1;
            this.append(vid, state, biome);
            return vid;
        }
    }

    private void put(int vid, Entry entry) {
        Entry[] e = this.entries;
        byte[] c = this.classes;
        if (vid >= e.length) {
            int n = Math.max(e.length * 2, vid + 1);
            e = Arrays.copyOf(e, n);
            c = Arrays.copyOf(c, n);
        }
        e[vid] = entry;
        c[vid] = entry.cls;
        // Volatile writes publish the element stores to readers on other threads.
        this.entries = e;
        this.classes = c;
    }

    private void append(int vid, BlockState state, @Nullable Holder<Biome> biome) {
        if (this.writer == null) {
            return;
        }
        String biomeName = biome != null ? biome.unwrapKey().map(k -> k.location().toString()).orElse("-") : "-";
        try {
            this.writer.write(vid + "\t" + BlockStateParser.serialize(state) + "\t" + biomeName + "\n");
            this.writer.flush();
        } catch (IOException e) {
            Vantage.LOGGER.warn("Failed to record visual id {}", vid, e);
        }
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(this.file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Vantage.LOGGER.error("Failed to read {}", this.file, e);
            return;
        }
        int max = 0;
        int missing = 0;
        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length < 3) {
                continue;
            }
            int vid;
            try {
                vid = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (vid <= Voxel.FILLER_VID || vid > Voxel.MAX_VID) {
                continue;
            }
            max = Math.max(max, vid);
            BlockState state = null;
            try {
                state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), parts[1], false).blockState();
            } catch (CommandSyntaxException | RuntimeException ignored) {
            }
            Holder<Biome> biome = null;
            if (!parts[2].equals("-")) {
                ResourceLocation loc = ResourceLocation.tryParse(parts[2]);
                if (loc != null) {
                    Optional<Holder.Reference<Biome>> h = this.biomes.getHolder(ResourceKey.create(Registries.BIOME, loc));
                    biome = h.orElse(null);
                }
            }
            if (state == null) {
                missing++;
                this.put(vid, new Entry(null, null, VisualClass.OPAQUE, MISSING_COLORS));
                continue;
            }
            byte cls = this.analyzer.shape(state).cls();
            if (cls == VisualClass.AIR) {
                cls = VisualClass.OPAQUE;
            }
            this.put(vid, new Entry(state, biome, cls, this.analyzer.faceColors(state, biome)));
            Object key = biome != null && biome.unwrapKey().isPresent() ? new BiomeKeyed(state, biome.unwrapKey().get()) : state;
            this.ids.putIfAbsent(key, vid);
        }
        this.size = Math.max(this.size, max + 1);
        if (missing > 0) {
            Vantage.LOGGER.warn("{} stored LOD block types no longer exist; they will render grey", missing);
        }
    }

    /** Recomputes colours after textures or models changed. */
    public void recomputeColors() {
        synchronized (this.lock) {
            Entry[] e = this.entries;
            for (int vid = 1; vid < this.size; vid++) {
                Entry old = e[vid];
                if (old == null || old.state == null) {
                    continue;
                }
                e[vid] = new Entry(old.state, old.biome, old.cls, this.analyzer.faceColors(old.state, old.biome));
            }
            this.entries = e;
        }
        this.colorRevision.incrementAndGet();
    }

    @Override
    public void close() {
        synchronized (this.lock) {
            if (this.writer != null) {
                try {
                    this.writer.close();
                } catch (IOException ignored) {
                }
                this.writer = null;
            }
        }
    }
}
