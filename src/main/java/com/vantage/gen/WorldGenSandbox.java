package com.vantage.gen;

import com.mojang.serialization.MapCodec;
import com.vantage.Vantage;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongFunction;

/**
 * Makes real terrain for places nobody has explored, the way Minecraft would: biomes, noise,
 * surface and the features you can see from afar (trees, plants, rocks, icebergs, snow), in
 * throwaway chunks that never enter the world or the save. Underground features (ores, geodes,
 * dungeons...) and structures are skipped: they cannot be seen from a distance and cost most.
 *
 * <p>Terrain shape is only computed in a band of each chunk, from a little under the surface to
 * a little above it: the band comes from a quick survey of the batch with Minecraft's own
 * surface estimate, and Minecraft's noise state is made just that high. A chunk whose terrain
 * turns out not to fit its band is made again with the full height. Everything under the band
 * counts as solid rock; above it the chunk is open for trees to grow into.
 *
 * <p>Works in batches of {@link #BATCH}² chunks. Features may spill into neighbouring chunks
 * (tree crowns), so every chunk of the batch is decorated, and only the inner
 * {@link #INNER}² chunks, which got everything their neighbours put into them, are returned.
 * Thread-safe: each call works on its own chunks.
 */
public final class WorldGenSandbox {
    /** Chunks returned per batch, along x and z. */
    public static final int INNER = 8;
    /** Chunks generated per batch, along x and z. */
    public static final int BATCH = INNER + 2;
    /**
     * Terrain shape is computed from this far under the chunk's lowest surface estimate (rounded
     * down to a section) to {@link #ABOVE_SURFACE} above its highest. Minecraft's estimate leaves
     * out the finer terrain shape (mountain jaggedness above all): real terrain leaves such a band
     * in about 1% of chunks, which are then made again with the full height. Above the band the
     * chunk is open for trees to grow into.
     */
    private static final int BELOW_SURFACE = 8;
    private static final int ABOVE_SURFACE = 24;
    /** Biomes are worked out this far above the terrain band (snow on tree tops depends on them). */
    private static final int BIOME_HEADROOM = 32;
    /**
     * Features that only ever place underground (caves, ores, buried lava lakes) or are too small
     * to see. Blobs of dirt, gravel and the stone kinds stay: they show on hillsides.
     */
    private static final List<String> HIDDEN_FEATURES = List.of("lichen", "geode", "dripstone", "sculk", "fossil", "monster_room",
            "cave", "spore_blossom", "underwater_magma", "spring_", "underground", "ore_coal", "ore_iron", "ore_gold", "ore_redstone",
            "ore_diamond", "ore_lapis", "ore_copper", "ore_emerald", "ore_infested", "ore_tuff", "ore_debris", "ore_quartz",
            // Stone kinds placed from y 0 to 60: rarely near the surface, and costly.
            "_lower");

    /** Decoration steps that change what the surface looks like. */
    private static final Set<GenerationStep.Decoration> VISIBLE_STEPS = EnumSet.of(
            GenerationStep.Decoration.RAW_GENERATION,
            GenerationStep.Decoration.LAKES,
            GenerationStep.Decoration.LOCAL_MODIFICATIONS,
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            // Sand, gravel and clay patches by water, and the stone blobs.
            GenerationStep.Decoration.UNDERGROUND_ORES,
            GenerationStep.Decoration.VEGETAL_DECORATION,
            GenerationStep.Decoration.TOP_LAYER_MODIFICATION);
    private static final EnumSet<Heightmap.Types> FEATURE_HEIGHTMAPS = EnumSet.of(Heightmap.Types.MOTION_BLOCKING,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE);

    /**
     * One generated chunk. Only blocks from {@code bottom} up were generated: everything under
     * that is solid {@code stone}. Sky light is per section, in Minecraft's nibble layout
     * ({@code null} for sections with nothing in them and those under {@code bottom}), or null
     * altogether in dimensions without sky light.
     */
    public record Chunk(ChunkPos pos, ProtoChunk chunk, byte @Nullable [][] skyLight, int bottom, BlockState stone) {
    }

    private final ServerLevel level;
    private final ChunkGenerator generator;
    private final RandomState random;
    private final Registry<Biome> biomes;
    private final Registry<PlacedFeature> features;
    @Nullable
    private final NoiseBasedChunkGenerator noise;
    private final int minY;
    private final int maxY;
    private final int seaLevel;
    /** Whether bands can be used: noise cells must fit whole into sections. */
    private final boolean banded;
    private final boolean[] visibleStep;
    private final Set<PlacedFeature> hidden = Collections.newSetFromMap(new IdentityHashMap<>());
    private final BlockState stone;
    /** Looks like {@link #stone} but is not it, so surface rules leave it alone (see {@link #hideDeepStone}). */
    @Nullable
    private final BlockState deepStone;
    /** Whether the generator uses Minecraft's overworld surface rules, whose depths are known. */
    private final boolean vanillaSurface;
    private final Map<Holder<Biome>, Integer> ruleDepths = new ConcurrentHashMap<>();
    private final AtomicBoolean featureFailureLogged = new AtomicBoolean();
    /** Nanoseconds spent per stage: survey and biomes, noise, surface, features, light. */
    private final LongAdder[] stageNanos = {new LongAdder(), new LongAdder(), new LongAdder(), new LongAdder(), new LongAdder()};
    private final LongAdder batches = new LongAdder();
    private final LongAdder chunksMade = new LongAdder();
    private final LongAdder bandBlocks = new LongAdder();
    private final LongAdder remadeDeeper = new LongAdder();
    private final LongAdder remadeTaller = new LongAdder();
    private final ConcurrentHashMap<PlacedFeature, LongAdder> featureNanos = new ConcurrentHashMap<>();
    private final ThreadLocal<Reference2ByteOpenHashMap<BlockState>> opacity = ThreadLocal.withInitial(() -> {
        Reference2ByteOpenHashMap<BlockState> m = new Reference2ByteOpenHashMap<>();
        m.defaultReturnValue((byte) -1);
        return m;
    });

    public WorldGenSandbox(ServerLevel level) {
        this.level = level;
        this.generator = level.getChunkSource().getGenerator();
        this.noise = this.generator instanceof NoiseBasedChunkGenerator n ? n : null;
        RandomState caveless = this.noise == null ? null : withoutCaves(level.registryAccess().lookupOrThrow(Registries.DENSITY_FUNCTION),
                level.registryAccess().lookupOrThrow(Registries.NOISE), this.noise.generatorSettings().value(), level.getSeed());
        this.random = caveless != null ? caveless : level.getChunkSource().randomState();
        this.biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        this.features = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        this.minY = level.getMinBuildHeight();
        this.maxY = level.getMaxBuildHeight();
        this.seaLevel = this.generator.getSeaLevel();
        this.banded = this.noise != null && 16 % this.noise.generatorSettings().value().noiseSettings().getCellHeight() == 0
                && Math.floorMod(this.minY, 16) == 0;
        this.visibleStep = new boolean[GenerationStep.Decoration.values().length];
        for (GenerationStep.Decoration step : VISIBLE_STEPS) {
            this.visibleStep[step.ordinal()] = true;
        }
        for (var e : this.features.entrySet()) {
            String path = e.getKey().location().getPath();
            if (HIDDEN_FEATURES.stream().anyMatch(path::contains)) {
                this.hidden.add(e.getValue());
            }
        }
        this.stone = this.noise != null ? this.noise.generatorSettings().value().defaultBlock() : Blocks.STONE.defaultBlockState();
        this.deepStone = this.stone.is(Blocks.STONE) ? Blocks.INFESTED_STONE.defaultBlockState()
                : this.stone.is(Blocks.DEEPSLATE) ? Blocks.INFESTED_DEEPSLATE.defaultBlockState()
                : null;
        this.vanillaSurface = this.noise != null && this.noise.generatorSettings().value().surfaceRule().equals(SurfaceRuleData.overworld());
    }

    public ServerLevel level() {
        return this.level;
    }

    /** The rock everything under the generated band is taken to be. */
    public BlockState stone() {
        return this.stone;
    }

    /** Generates the {@link #INNER}² chunks from chunk {@code (minX, minZ)}. */
    public List<Chunk> generate(int minX, int minZ) {
        long t0 = System.nanoTime();
        int x0 = minX - 1;
        int z0 = minZ - 1;
        NoiseChunk survey = this.banded ? this.survey(SectionPos.sectionToBlockCoord(x0), SectionPos.sectionToBlockCoord(z0)) : null;
        Climate.Sampler sampler = survey != null
                ? survey.cachedClimateSampler(this.random.router(), this.noise.generatorSettings().value().spawnTarget())
                : this.random.sampler();

        Long2ObjectMap<ProtoChunk> chunks = new Long2ObjectOpenHashMap<>(BATCH * BATCH);
        Long2ObjectMap<ProtoChunk> outside = new Long2ObjectOpenHashMap<>();
        LongFunction<ProtoChunk> outsideChunk = key -> {
            synchronized (outside) {
                return outside.computeIfAbsent(key, k -> new ProtoChunk(new ChunkPos(k), UpgradeData.EMPTY, this.level, this.biomes, null));
            }
        };
        ProtoChunk[] grid = new ProtoChunk[BATCH * BATCH];
        int[] bottoms = new int[BATCH * BATCH];
        int[] tops = new int[BATCH * BATCH];
        for (int dz = 0; dz < BATCH; dz++) {
            for (int dx = 0; dx < BATCH; dx++) {
                int i = dz * BATCH + dx;
                ChunkPos pos = new ChunkPos(x0 + dx, z0 + dz);
                bottoms[i] = this.minY;
                tops[i] = this.maxY;
                if (survey != null) {
                    int[] band = this.band(survey, pos);
                    bottoms[i] = band[0];
                    tops[i] = band[1];
                }
                grid[i] = this.newChunk(pos, bottoms[i], tops[i], sampler);
                chunks.put(pos.toLong(), grid[i]);
            }
        }
        long t1 = System.nanoTime();

        for (int i = 0; i < grid.length; i++) {
            ProtoChunk chunk = grid[i];
            int bottom = bottoms[i];
            int top = tops[i];
            this.fillNoise(chunk, bottom, top, chunks, outsideChunk);
            if (this.banded) {
                // Terrain that does not fit the band: make the chunk again, with the full height where it did not fit.
                boolean deeper = false;
                boolean taller = false;
                Heightmap floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int above = floor.getFirstAvailable(x, z);
                        deeper |= above <= bottom && bottom > this.minY;
                        taller |= above >= top && top < this.maxY;
                    }
                }
                if (deeper || taller) {
                    if (deeper) {
                        this.remadeDeeper.increment();
                        bottom = this.minY;
                    }
                    if (taller) {
                        this.remadeTaller.increment();
                        top = this.maxY;
                    }
                    chunk = this.newChunk(chunk.getPos(), bottom, top, sampler);
                    grid[i] = chunk;
                    bottoms[i] = bottom;
                    tops[i] = top;
                    chunks.put(chunk.getPos().toLong(), chunk);
                    this.fillNoise(chunk, bottom, top, chunks, outsideChunk);
                }
                this.bandBlocks.add(top - bottom);
            }
            this.chunksMade.increment();
        }
        long t2 = System.nanoTime();

        for (int i = 0; i < grid.length; i++) {
            ProtoChunk chunk = grid[i];
            int[] hidden = this.hideDeepStone(chunk, bottoms[i]);
            SandboxRegion region = new SandboxRegion(this.level, chunk, chunks, outsideChunk);
            this.generator.buildSurface(region, this.level.structureManager().forWorldGenRegion(region), this.random, chunk);
            this.showDeepStone(chunk, hidden);
            // Carvers skipped: caves cannot be seen from afar.
            chunk.setPersistedStatus(ChunkStatus.CARVERS);
        }
        long t3 = System.nanoTime();
        for (ProtoChunk chunk : grid) {
            Heightmap.primeHeightmaps(chunk, FEATURE_HEIGHTMAPS);
            this.decorate(new SandboxRegion(this.level, chunk, chunks, outsideChunk), chunk);
        }
        long t4 = System.nanoTime();

        List<Chunk> out = new ArrayList<>(INNER * INNER);
        for (int dz = 1; dz <= INNER; dz++) {
            for (int dx = 1; dx <= INNER; dx++) {
                int i = dz * BATCH + dx;
                ProtoChunk chunk = grid[i];
                chunk.setPersistedStatus(ChunkStatus.FEATURES);
                out.add(new Chunk(chunk.getPos(), chunk, this.skyLight(chunk, bottoms[i]), bottoms[i], this.stone));
            }
        }
        long t5 = System.nanoTime();
        this.stageNanos[0].add(t1 - t0);
        this.stageNanos[1].add(t2 - t1);
        this.stageNanos[2].add(t3 - t2);
        this.stageNanos[3].add(t4 - t3);
        this.stageNanos[4].add(t5 - t4);
        this.batches.increment();
        return out;
    }

    /** Where the time went so far: milliseconds per batch by stage, and the costliest features. */
    public String stats() {
        long n = Math.max(1, this.batches.sum());
        long chunks = Math.max(1, this.chunksMade.sum());
        StringBuilder b = new StringBuilder(String.format(Locale.ROOT,
                "%d batches, ms per batch: biomes %.0f, noise %.0f, surface %.0f, features %.0f, light %.0f;"
                        + " band %.0f blocks, remade %.1f%% deeper %.1f%% taller; costliest features:",
                this.batches.sum(), this.stageNanos[0].sum() / 1e6 / n, this.stageNanos[1].sum() / 1e6 / n,
                this.stageNanos[2].sum() / 1e6 / n, this.stageNanos[3].sum() / 1e6 / n, this.stageNanos[4].sum() / 1e6 / n,
                this.bandBlocks.sum() / (double) chunks, 100.0 * this.remadeDeeper.sum() / chunks, 100.0 * this.remadeTaller.sum() / chunks));
        this.featureNanos.entrySet().stream()
                .sorted((x, y) -> Long.compare(y.getValue().sum(), x.getValue().sum()))
                .limit(12)
                .forEach(e -> b.append(String.format(Locale.ROOT, " %s %.1f",
                        this.features.getResourceKey(e.getKey()).map(k -> k.location().getPath()).orElse("?"), e.getValue().sum() / 1e6 / n)));
        return b.toString();
    }

    /**
     * Minecraft's noise state for the whole batch area at once, for its quick surface estimate
     * and its climate (biome) sampling. Both only use the column caches it fills on creation.
     */
    private NoiseChunk survey(int blockX, int blockZ) {
        NoiseGeneratorSettings settings = this.noise.generatorSettings().value();
        NoiseSettings ns = settings.noiseSettings().clampToHeightAccessor(this.level);
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(this.seaLevel, settings.defaultFluid());
        return new NoiseChunk(BATCH * 16 / ns.getCellWidth(), this.random, blockX, blockZ, ns, NoStructures.INSTANCE, settings,
                (x, y, z) -> sea, Blender.empty());
    }

    /**
     * Blocks {@code [bottom, top)} of a chunk to fill with noise: from {@link #BELOW_SURFACE}
     * under its lowest surface estimate (a section boundary) to {@link #ABOVE_SURFACE} above its
     * highest, and at least a section above sea level.
     */
    private int[] band(NoiseChunk survey, ChunkPos pos) {
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        int bx = pos.getMinBlockX();
        int bz = pos.getMinBlockZ();
        for (int z = 0; z <= 16; z += 4) {
            for (int x = 0; x <= 16; x += 4) {
                int y = survey.preliminarySurfaceLevel(bx + x, bz + z);
                if (y == Integer.MAX_VALUE) {
                    return new int[]{this.minY, this.maxY};
                }
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        }
        int bottom = Math.max(this.minY, Mth.floorDiv(lo - BELOW_SURFACE, 16) * 16);
        int top = Math.max(hi + ABOVE_SURFACE, this.seaLevel + 16);
        top = Math.min(this.maxY, Mth.floorDiv(top + 15, 16) * 16);
        return top <= bottom ? new int[]{this.minY, this.maxY} : new int[]{bottom, top};
    }

    /**
     * A new chunk, with biomes worked out from noise from {@code bottom} to a little above
     * {@code top} (the terrain band) and continued straight up and down from there.
     */
    private ProtoChunk newChunk(ChunkPos pos, int bottom, int top, Climate.Sampler sampler) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, this.level, this.biomes, null);
        int qx = QuartPos.fromBlock(pos.getMinBlockX());
        int qz = QuartPos.fromBlock(pos.getMinBlockZ());
        int first = chunk.getSectionIndex(bottom);
        int last = chunk.getSectionIndex(Math.min(this.maxY, top + BIOME_HEADROOM) - 1);
        for (int i = first; i <= last; i++) {
            chunk.getSection(i).fillBiomesFromNoise(this.generator.getBiomeSource(), sampler, qx,
                    QuartPos.fromSection(chunk.getSectionYFromSectionIndex(i)), qz);
        }
        LevelChunkSection lowest = chunk.getSection(first);
        LevelChunkSection highest = chunk.getSection(last);
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            if (i < first || i > last) {
                LevelChunkSection from = i < first ? lowest : highest;
                int y = i < first ? 0 : 3;
                chunk.getSection(i).fillBiomesFromNoise((x, qy, z, s) -> from.getNoiseBiome(x & 3, y, z & 3), sampler, qx,
                        QuartPos.fromSection(chunk.getSectionYFromSectionIndex(i)), qz);
            }
        }
        chunk.setPersistedStatus(ChunkStatus.BIOMES);
        return chunk;
    }

    /** Fills blocks {@code [bottom, top)} of the chunk with Minecraft's terrain noise. */
    private void fillNoise(ProtoChunk chunk, int bottom, int top, Long2ObjectMap<ProtoChunk> chunks, LongFunction<ProtoChunk> outside) {
        StructureManager structures = this.level.structureManager().forWorldGenRegion(new SandboxRegion(this.level, chunk, chunks, outside));
        if (this.noise == null) {
            this.generator.fillFromNoise(Blender.empty(), this.random, structures, chunk).join();
        } else {
            // Minecraft sizes its noise state to the chunk it is made for: give it one just the band high.
            ProtoChunk shape = new ProtoChunk(chunk.getPos(), UpgradeData.EMPTY, LevelHeightAccessor.create(bottom, top - bottom), this.biomes, null);
            NoiseChunk state = this.noise.createNoiseChunk(shape, structures, Blender.empty(), this.random);
            chunk.getOrCreateNoiseChunk(c -> state);
            NoiseSettings ns = this.noise.generatorSettings().value().noiseSettings().clampToHeightAccessor(shape);
            int cells = Mth.floorDiv(ns.height(), ns.getCellHeight());
            if (cells > 0) {
                this.noise.doFill(Blender.empty(), structures, this.random, chunk, Mth.floorDiv(ns.minY(), ns.getCellHeight()), cells);
            }
        }
        chunk.setPersistedStatus(ChunkStatus.NOISE);
    }

    /**
     * The world's noise without caves (entrances, noodles and the cheese, spaghetti and pillar
     * caves deeper down) and ore veins: none of them can be seen from afar, and evaluating them
     * is much of the cost of filling chunks with noise. The surface comes out the same, and so
     * do water and lava at it. Null if the generator's terrain is not built the vanilla way.
     */
    @Nullable
    static RandomState withoutCaves(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises,
                                    NoiseGeneratorSettings settings, long seed) {
        // As they look inside a router after mapAll (references to other functions become direct).
        List<DensityFunction> caves = new ArrayList<>();
        for (String path : List.of("overworld/caves/entrances", "overworld/caves/noodle")) {
            functions.get(ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(path)))
                    .ifPresent(f -> caves.add(f.value().mapAll(x -> x)));
        }
        if (caves.isEmpty()) {
            return null;
        }
        boolean[] removed = new boolean[1];
        DensityFunction.Visitor visitor = f -> {
            if (f instanceof DensityFunctions.HolderHolder h && caves.contains(h.function().value())) {
                removed[0] = true;
                return NoCave.INSTANCE;
            }
            if (f instanceof DensityFunctions.RangeChoice choice && choice.minInclusive() == -1000000.0 && choice.maxExclusive() == 1.5625) {
                // The terrain density, with cave entrances near the surface and the cave systems deep inside the ground: just the terrain.
                removed[0] = true;
                return choice.input();
            }
            return f;
        };
        NoiseRouter r = settings.noiseRouter();
        DensityFunction finalDensity = r.finalDensity().mapAll(visitor);
        if (!removed[0]) {
            return null;
        }
        NoiseRouter router = new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(), r.fluidLevelSpreadNoise(), r.lavaNoise(),
                r.temperature(), r.vegetation(), r.continents(), r.erosion(), r.depth(), r.ridges(), r.initialDensityWithoutJaggedness(),
                finalDensity, r.veinToggle(), r.veinRidged(), r.veinGap());
        NoiseGeneratorSettings caveless = new NoiseGeneratorSettings(settings.noiseSettings(), settings.defaultBlock(), settings.defaultFluid(),
                router, settings.surfaceRule(), settings.spawnTarget(), settings.seaLevel(), settings.disableMobGeneration(),
                settings.aquifersEnabled(), false, settings.useLegacyRandomSource());
        return RandomState.create(caveless, noises, seed);
    }

    /**
     * Minecraft's surface rules check the biome of every stone block above about its quick
     * surface estimate, which is slow where real terrain rises far above that (mountains), yet
     * they only change the top few blocks. Before they run, such stone deeper than they can reach
     * ({@link #ruleDepth}) is swapped for a block that looks the same but that the rules skip;
     * {@link #showDeepStone} puts it back. Returns, per column, the range that was swapped.
     */
    private int @Nullable [] hideDeepStone(ProtoChunk chunk, int bottom) {
        if (this.deepStone == null) {
            return null;
        }
        // Under the estimate (less the most the rules subtract) they stop before asking for biomes.
        int from = bottom;
        if (this.noise != null) {
            NoiseChunk nc = chunk.getOrCreateNoiseChunk(c -> {
                throw new IllegalStateException("no noise yet");
            });
            int bx = chunk.getPos().getMinBlockX();
            int bz = chunk.getPos().getMinBlockZ();
            int estimate = Math.min(Math.min(nc.preliminarySurfaceLevel(bx, bz), nc.preliminarySurfaceLevel(bx + 16, bz)),
                    Math.min(nc.preliminarySurfaceLevel(bx, bz + 16), nc.preliminarySurfaceLevel(bx + 16, bz + 16)));
            from = Math.max(bottom, estimate - 8);
        }
        int[] hidden = new int[257];
        hidden[256] = from;
        int minSection = chunk.getMinSection();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int top = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (top - 10 <= from) {
                    // Nothing to gain (no rule reaches less than 10 down).
                    hidden[z << 4 | x] = from;
                    continue;
                }
                Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z));
                int last = top - this.ruleDepth(biome);
                hidden[z << 4 | x] = last;
                for (int y = from; y < last; y++) {
                    LevelChunkSection s = chunk.getSection((y >> 4) - minSection);
                    if (s.getBlockState(x, y & 15, z) == this.stone) {
                        s.setBlockState(x, y & 15, z, this.deepStone, false);
                    }
                }
            }
        }
        return hidden;
    }

    /** Undoes {@link #hideDeepStone}, for features that look for stone (blobs of other stone kinds). */
    private void showDeepStone(ProtoChunk chunk, int @Nullable [] hidden) {
        if (hidden == null) {
            return;
        }
        int from = hidden[256];
        int minSection = chunk.getMinSection();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int last = hidden[z << 4 | x];
                for (int y = from; y < last; y++) {
                    LevelChunkSection s = chunk.getSection((y >> 4) - minSection);
                    if (s.getBlockState(x, y & 15, z) == this.deepStone) {
                        s.setBlockState(x, y & 15, z, this.stone, false);
                    }
                }
            }
        }
    }

    /**
     * How far under the top of a column surface rules can still change stone. Minecraft's own rules
     * reach at most 8 blocks down, except under beaches and warm oceans (sandstone, 14) and deserts
     * (sandstone, 38); badlands colour bands run down whole cliffs. Other rules get a safe margin.
     */
    private int ruleDepth(Holder<Biome> biome) {
        return this.ruleDepths.computeIfAbsent(biome, b -> {
            if (b.is(BiomeTags.IS_BADLANDS)) {
                return Integer.MAX_VALUE;
            }
            if (!this.vanillaSurface) {
                return 24;
            }
            if (b.is(Biomes.DESERT)) {
                return 40;
            }
            if (b.is(Biomes.BEACH) || b.is(Biomes.SNOWY_BEACH) || b.is(Biomes.WARM_OCEAN)) {
                return 16;
            }
            return 10;
        });
    }

    /**
     * Minecraft's feature placement for one chunk ({@code ChunkGenerator.applyBiomeDecoration}),
     * seeded exactly the same way, for the visible steps only and without structures. A feature
     * that throws is skipped rather than failing the batch.
     */
    private void decorate(SandboxRegion region, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        SectionPos section = SectionPos.of(pos, region.getMinSection());
        BlockPos origin = section.origin();
        List<FeatureSorter.StepFeatureData> steps = this.generator.featuresPerStep.get();
        WorldgenRandom rnd = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
        long seed = rnd.setDecorationSeed(region.getSeed(), origin.getX(), origin.getZ());
        Set<Holder<Biome>> present = new ObjectArraySet<>();
        ChunkPos.rangeClosed(section.chunk(), 1).forEach(p -> {
            if (region.hasChunk(p.x, p.z)) {
                for (LevelChunkSection s : region.getChunk(p.x, p.z).getSections()) {
                    s.getBiomes().getAll(present::add);
                }
            }
        });
        present.retainAll(this.generator.getBiomeSource().possibleBiomes());
        for (int step = 0; step < steps.size(); step++) {
            if (step >= this.visibleStep.length || !this.visibleStep[step]) {
                continue;
            }
            FeatureSorter.StepFeatureData data = steps.get(step);
            IntSet indices = new IntArraySet();
            for (Holder<Biome> biome : present) {
                List<HolderSet<PlacedFeature>> perStep = this.generator.generationSettingsGetter.apply(biome).features();
                if (step < perStep.size()) {
                    perStep.get(step).stream().map(Holder::value).forEach(f -> indices.add(data.indexMapping().applyAsInt(f)));
                }
            }
            int[] sorted = indices.toIntArray();
            Arrays.sort(sorted);
            for (int index : sorted) {
                PlacedFeature feature = data.features().get(index);
                if (this.hidden.contains(feature)) {
                    continue;
                }
                rnd.setFeatureSeed(seed, index, step);
                long ft = System.nanoTime();
                try {
                    feature.placeWithBiomeCheck(region, this.generator, rnd, origin);
                    this.featureNanos.computeIfAbsent(feature, f -> new LongAdder()).add(System.nanoTime() - ft);
                } catch (RuntimeException e) {
                    if (this.featureFailureLogged.compareAndSet(false, true)) {
                        Vantage.LOGGER.warn("Distant terrain: feature {} failed, skipping it where it fails",
                                this.features.getResourceKey(feature).map(Object::toString).orElse("?"), e);
                    }
                }
            }
        }
    }

    /**
     * Sky light straight down from the sky: full above the ground, less through water and leaves,
     * none under solid blocks. Minecraft also spreads light sideways, which only matters close up.
     */
    private byte @Nullable [][] skyLight(ProtoChunk chunk, int bottom) {
        if (!this.level.dimensionType().hasSkyLight()) {
            return null;
        }
        LevelChunkSection[] sections = chunk.getSections();
        byte[][] out = new byte[sections.length][];
        int[] light = new int[256];
        Arrays.fill(light, 15);
        Reference2ByteOpenHashMap<BlockState> cache = this.opacity.get();
        int first = chunk.getSectionIndex(bottom);
        for (int i = sections.length - 1; i >= first; i--) {
            LevelChunkSection s = sections[i];
            if (s.hasOnlyAir()) {
                // Air passes light on unchanged; leaving the section out says just that.
                continue;
            }
            byte[] data = new byte[2048];
            for (int y = 15; y >= 0; y--) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int c = z << 4 | x;
                        int l = light[c];
                        if (l > 0) {
                            BlockState state = s.getBlockState(x, y, z);
                            int o = cache.getByte(state);
                            if (o < 0) {
                                o = Math.min(15, state.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
                                cache.put(state, (byte) o);
                            }
                            l = Math.max(0, l - o);
                            light[c] = l;
                        }
                        int index = y << 8 | c;
                        data[index >> 1] |= (byte) (l << ((index & 1) << 2));
                    }
                }
            }
            out[i] = data;
        }
        return out;
    }

    /**
     * Stands in for a cave: far above any density the terrain reaches, so min(terrain, this) is the
     * terrain. Its stated range overlaps everything: Minecraft warns, at great length, every time
     * it rebuilds a min or max of two inputs whose ranges do not overlap.
     */
    private enum NoCave implements DensityFunction.SimpleFunction {
        INSTANCE;

        private static final KeyDispatchDataCodec<NoCave> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 1000.0;
        }

        @Override
        public double minValue() {
            return -1000.0;
        }

        @Override
        public double maxValue() {
            return 1000.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /** Stands in for structures' terrain adaptation in the survey, which never fills blocks. */
    private enum NoStructures implements DensityFunctions.BeardifierOrMarker {
        INSTANCE;

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 0.0;
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }
    }
}
