package com.vantage.client.ingest;

import com.vantage.Vantage;
import com.vantage.client.LodSession;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Singleplayer only: reads every already-generated chunk of the save in the background, nearest
 * regions first, so distant terrain shows up without having to visit it this session.
 *
 * <p>Chunk timestamps from the region headers are remembered, so later sessions only re-read
 * chunks the game has saved since. Chunks loaded on the client are skipped; they are ingested
 * live. Work is throttled so it never competes with mesh building.
 */
public final class RegionImporter implements AutoCloseable {
    private static final int MAX_PENDING = 64;
    private static final long RESCAN_NANOS = 60_000_000_000L;

    private final LodSession session;
    private final Path regionDir;
    private final Path logFile;
    private final Map<Long, int[]> imported = new HashMap<>();
    private final Thread thread;
    private final AtomicInteger pending = new AtomicInteger();
    private volatile boolean running = true;
    private volatile boolean logDirty;
    private volatile int chunksDone;
    private volatile int chunksTotal;
    private volatile boolean scanning;
    /** Region headers of the save (chunk locations), by region; for {@link #pendingAt}. */
    private volatile Map<Long, int[]> savedChunks = Map.of();
    private volatile boolean firstPassDone;
    private final Registry<Biome> biomes;
    private final Holder<Biome> plains;
    private final int minSection;
    private final int sectionCount;
    private final int dataVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();

    public RegionImporter(LodSession session, ClientLevel level, Path regionDir, Path logFile) {
        this.session = session;
        this.regionDir = regionDir;
        this.logFile = logFile;
        this.biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        this.plains = this.biomes.getHolderOrThrow(Biomes.PLAINS);
        this.minSection = level.getMinSection();
        this.sectionCount = level.getSectionsCount();
        this.loadLog();
        this.thread = new Thread(this::loop, "Vantage importer");
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.MIN_PRIORITY);
        this.thread.start();
    }

    public int done() {
        return this.chunksDone;
    }

    public int total() {
        return this.chunksTotal;
    }

    public boolean scanning() {
        return this.scanning;
    }

    /**
     * True while the first pass over the save is still running and the save holds the chunk at
     * this block position: distant generation leaves such places to the import.
     */
    public boolean pendingAt(int blockX, int blockZ) {
        if (this.firstPassDone) {
            return false;
        }
        int cx = blockX >> 4;
        int cz = blockZ >> 4;
        int[] header = this.savedChunks.get(((long) (cx >> 5) << 32) | ((cz >> 5) & 0xFFFFFFFFL));
        return header != null && header[(cx & 31) | ((cz & 31) << 5)] != 0;
    }

    private void loop() {
        long lastScan = 0;
        while (this.running) {
            if (lastScan != 0 && System.nanoTime() - lastScan < RESCAN_NANOS) {
                LockSupport.parkNanos(1_000_000_000L);
                continue;
            }
            lastScan = System.nanoTime();
            try {
                this.scanning = true;
                this.scan();
            } catch (Throwable t) {
                Vantage.LOGGER.error("Vantage world import failed", t);
            } finally {
                this.scanning = false;
                this.firstPassDone = true;
                this.saveLog();
            }
        }
    }

    private void scan() throws IOException {
        if (!Files.isDirectory(this.regionDir)) {
            return;
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(this.regionDir)) {
            s.filter(p -> p.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca")).forEach(files::add);
        }
        double px = this.session.camX();
        double pz = this.session.camZ();
        files.sort((a, b) -> Double.compare(regionDistance(a, px, pz), regionDistance(b, px, pz)));

        int total = 0;
        List<int[]> headers = new ArrayList<>();
        Map<Long, int[]> saved = new HashMap<>();
        for (Path f : files) {
            int[] header = readHeader(f);
            headers.add(header);
            if (header != null) {
                String[] parts = f.getFileName().toString().split("\\.");
                saved.put(((long) Integer.parseInt(parts[1]) << 32) | (Integer.parseInt(parts[2]) & 0xFFFFFFFFL), header);
                for (int i = 0; i < 1024; i++) {
                    if (header[i] != 0) {
                        total++;
                    }
                }
            }
        }
        this.savedChunks = saved;
        this.chunksTotal = total;
        int done = 0;
        for (int fi = 0; fi < files.size() && this.running; fi++) {
            int[] header = headers.get(fi);
            if (header == null) {
                continue;
            }
            Path f = files.get(fi);
            String[] parts = f.getFileName().toString().split("\\.");
            int rx = Integer.parseInt(parts[1]);
            int rz = Integer.parseInt(parts[2]);
            long rkey = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
            int[] seen = this.imported.computeIfAbsent(rkey, k -> new int[1024]);
            try (RandomAccessFile raf = new RandomAccessFile(f.toFile(), "r")) {
                for (int i = 0; i < 1024 && this.running; i++) {
                    int loc = header[i];
                    if (loc == 0) {
                        continue;
                    }
                    done++;
                    this.chunksDone = done;
                    int stamp = header[1024 + i];
                    int seenStamp;
                    synchronized (seen) {
                        seenStamp = seen[i];
                    }
                    if (stamp != 0 && stamp == seenStamp) {
                        continue;
                    }
                    int cx = rx * 32 + (i & 31);
                    int cz = rz * 32 + (i >> 5);
                    ClientLevel level = Minecraft.getInstance().level;
                    if (level != null && level.getChunkSource().getChunk(cx, cz, false) != null) {
                        continue;
                    }
                    CompoundTag tag = readChunk(raf, loc);
                    if (tag == null) {
                        continue;
                    }
                    ColumnSource src = this.parse(tag, cx, cz);
                    if (src == null) {
                        // Not fully generated (or unreadable): look again once the game rewrites it.
                        markSeen(seen, i, stamp);
                        continue;
                    }
                    while (this.running && (this.pending.get() >= MAX_PENDING || this.session.meshes.inFlight() > 64
                            || this.session.world.dirtyCount() > 4096)) {
                        LockSupport.parkNanos(20_000_000L);
                    }
                    this.pending.incrementAndGet();
                    double dx = cx * 16 + 8 - this.session.camX();
                    double dz = cz * 16 + 8 - this.session.camZ();
                    int slot = i;
                    this.session.pool.submit(100_000 + Math.sqrt(dx * dx + dz * dz), () -> {
                        try {
                            this.session.ingest(src);
                            this.markSeen(seen, slot, stamp);
                        } finally {
                            this.pending.decrementAndGet();
                        }
                    });
                }
            } catch (IOException e) {
                Vantage.LOGGER.debug("Skipping unreadable region {}: {}", f, e.toString());
            }
            if (fi % 8 == 7) {
                this.saveLog();
            }
        }
    }

    private void markSeen(int[] seen, int slot, int stamp) {
        synchronized (seen) {
            seen[slot] = stamp;
        }
        this.logDirty = true;
    }

    private static double regionDistance(Path p, double px, double pz) {
        String[] parts = p.getFileName().toString().split("\\.");
        double cx = Integer.parseInt(parts[1]) * 512 + 256;
        double cz = Integer.parseInt(parts[2]) * 512 + 256;
        return Math.hypot(cx - px, cz - pz);
    }

    /** Offsets (0..1023) and timestamps (1024..2047), or null if unreadable. */
    private static int[] readHeader(Path f) {
        try (RandomAccessFile raf = new RandomAccessFile(f.toFile(), "r")) {
            if (raf.length() < 8192) {
                return null;
            }
            byte[] buf = new byte[8192];
            raf.readFully(buf);
            ByteBuffer bb = ByteBuffer.wrap(buf);
            int[] out = new int[2048];
            for (int i = 0; i < 2048; i++) {
                out[i] = bb.getInt();
            }
            return out;
        } catch (IOException e) {
            return null;
        }
    }

    private static CompoundTag readChunk(RandomAccessFile raf, int loc) {
        try {
            long offset = (long) (loc >>> 8) * 4096;
            int sectors = loc & 0xFF;
            if (sectors == 0 || offset + 5 > raf.length()) {
                return null;
            }
            raf.seek(offset);
            int length = raf.readInt();
            if (length <= 1 || length > sectors * 4096) {
                return null;
            }
            int type = raf.readUnsignedByte();
            byte[] data = new byte[length - 1];
            raf.readFully(data);
            InputStream raw = new ByteArrayInputStream(data);
            InputStream in = switch (type) {
                case 1 -> new GZIPInputStream(raw);
                case 2 -> new InflaterInputStream(raw);
                case 3 -> raw;
                case 4 -> new LZ4BlockInputStream(raw);
                default -> null;
            };
            if (in == null) {
                return null;
            }
            try (DataInputStream dis = new DataInputStream(in)) {
                return NbtIo.read(dis, NbtAccounter.unlimitedHeap());
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private ColumnSource parse(CompoundTag tag, int cx, int cz) {
        try {
            int version = tag.contains("DataVersion", Tag.TAG_ANY_NUMERIC) ? tag.getInt("DataVersion") : 0;
            if (version < this.dataVersion) {
                tag = DataFixTypes.CHUNK.updateToCurrentVersion(DataFixers.getDataFixer(), tag, version);
            }
            String status = tag.getString("Status");
            if (!status.endsWith("full")) {
                return null;
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
        Arrays.fill(cells, this.plains);
        if (sec.contains("biomes", Tag.TAG_COMPOUND)) {
            CompoundTag b = sec.getCompound("biomes");
            ListTag pal = b.getList("palette", Tag.TAG_STRING);
            Holder<Biome>[] hp = new Holder[pal.size()];
            for (int i = 0; i < pal.size(); i++) {
                String name = pal.getString(i);
                hp[i] = biomeCache.computeIfAbsent(name, n -> {
                    ResourceLocation loc = ResourceLocation.tryParse(n);
                    if (loc == null) {
                        return this.plains;
                    }
                    return this.biomes.getHolder(ResourceKey.create(Registries.BIOME, loc)).map(h -> (Holder<Biome>) h).orElse(this.plains);
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
    static short[] unpack(long[] data, int bits, int count, int paletteSize) {
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

    static int ceilLog2(int n) {
        return n <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(n - 1);
    }

    private void loadLog() {
        if (!Files.exists(this.logFile)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(this.logFile)))) {
            int n = in.readInt();
            for (int r = 0; r < n; r++) {
                long key = in.readLong();
                int[] stamps = new int[1024];
                for (int i = 0; i < 1024; i++) {
                    stamps[i] = in.readInt();
                }
                this.imported.put(key, stamps);
            }
        } catch (IOException e) {
            this.imported.clear();
        }
    }

    private synchronized void saveLog() {
        if (!this.logDirty) {
            return;
        }
        this.logDirty = false;
        Map<Long, int[]> copy = new HashMap<>(this.imported);
        Path tmp = this.logFile.resolveSibling(this.logFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(this.logFile.getParent());
            try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(copy.size());
                for (var e : copy.entrySet()) {
                    out.writeLong(e.getKey());
                    int[] stamps;
                    synchronized (e.getValue()) {
                        stamps = e.getValue().clone();
                    }
                    for (int v : stamps) {
                        out.writeInt(v);
                    }
                }
            }
            Files.move(tmp, this.logFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Vantage.LOGGER.debug("Could not save import log: {}", e.toString());
        }
    }

    @Override
    public void close() {
        this.running = false;
        LockSupport.unpark(this.thread);
        try {
            this.thread.join(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.saveLog();
    }
}
