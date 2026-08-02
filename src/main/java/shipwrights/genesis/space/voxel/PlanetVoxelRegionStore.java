package shipwrights.genesis.space.voxel;

import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Append-only persistent brick store grouped into 16x16x16 brick regions.
 *
 * <p>Each region owns a fixed offset/length table and appends replacement
 * records. Updating one chunk therefore never rewrites the rest of the planet.
 * Stale records are reclaimed by {@link #compactAll()} rather than during
 * gameplay.</p>
 */
public final class PlanetVoxelRegionStore implements Closeable {
    private static final int REGION_BITS = 4;
    private static final int REGION_EDGE = 1 << REGION_BITS;
    private static final int ENTRY_COUNT = REGION_EDGE * REGION_EDGE * REGION_EDGE;
    private static final int MAGIC = 0x47505652; // GPVR
    private static final int VERSION = 1;
    private static final int FIXED_HEADER_BYTES = 16;
    private static final int OFFSET_TABLE_BYTES = ENTRY_COUNT * Long.BYTES;
    private static final int LENGTH_TABLE_BYTES = ENTRY_COUNT * Integer.BYTES;
    private static final long DATA_START = FIXED_HEADER_BYTES + OFFSET_TABLE_BYTES + LENGTH_TABLE_BYTES;
    private static final int MAX_OPEN_REGIONS = 24;
    private static final int MAX_COMPRESSED_RECORD = 8 * 1024 * 1024;

    private final Path root;
    private final Object lock = new Object();
    private final LinkedHashMap<RegionKey, RegionFile> openRegions =
            new LinkedHashMap<>(32, 0.75f, true);

    public PlanetVoxelRegionStore(Path root) throws IOException {
        this.root = root;
        Files.createDirectories(root);
    }

    public record LoadResult(List<PlanetVoxelBrick> bricks, int corruptRecords,
                             int scannedRegions) {
        public LoadResult {
            bricks = List.copyOf(bricks);
        }
    }

    /**
     * Restores persisted bricks without allowing one corrupt record to discard
     * the rest of the database. Coarser LOD directories are read first so a
     * bounded restore always recovers a usable whole-planet baseline before
     * spending memory on exact leaves.
     */
    public LoadResult loadAll(int limit) throws IOException {
        if (limit <= 0) return new LoadResult(List.of(), 0, 0);
        synchronized (lock) {
            flush();
            List<Path> regions;
            try (var paths = Files.walk(root)) {
                regions = paths.filter(path -> Files.isRegularFile(path)
                                && path.toString().endsWith(".pvr"))
                        .sorted(Comparator.comparingInt(
                                PlanetVoxelRegionStore::lodFromPath).reversed()
                                .thenComparing(Path::toString))
                        .toList();
            }
            List<PlanetVoxelBrick> restored = new ArrayList<>(
                    Math.min(limit, regions.size() * 16));
            int corrupt = 0;
            int scanned = 0;
            for (Path regionPath : regions) {
                if (restored.size() >= limit) break;
                scanned++;
                try (RegionFile region = new RegionFile(regionPath)) {
                    for (int index = 0; index < ENTRY_COUNT && restored.size() < limit; index++) {
                        byte[] record;
                        try {
                            record = region.read(index);
                            if (record == null) continue;
                            PlanetVoxelBrick brick = deserialize(record);
                            if (!path(RegionKey.from(brick.key())).normalize()
                                    .equals(regionPath.normalize())) {
                                corrupt++;
                                continue;
                            }
                            restored.add(brick);
                        } catch (IOException | RuntimeException invalidRecord) {
                            corrupt++;
                        }
                    }
                } catch (IOException invalidRegion) {
                    corrupt++;
                }
            }
            return new LoadResult(restored, corrupt, scanned);
        }
    }

    public void write(PlanetVoxelBrick brick) throws IOException {
        RegionKey regionKey = RegionKey.from(brick.key());
        RegionFile region = region(regionKey);
        region.write(localIndex(brick.key()), serialize(brick));
    }


    public void delete(PlanetVoxelBrickKey key) throws IOException {
        RegionKey regionKey = RegionKey.from(key);
        Path path = path(regionKey);
        if (!Files.isRegularFile(path)) return;
        region(regionKey).delete(localIndex(key));
    }

    public Optional<PlanetVoxelBrick> read(PlanetVoxelBrickKey key) throws IOException {
        RegionKey regionKey = RegionKey.from(key);
        Path path = path(regionKey);
        if (!Files.isRegularFile(path)) return Optional.empty();
        RegionFile region = region(regionKey);
        byte[] record = region.read(localIndex(key));
        if (record == null) return Optional.empty();
        PlanetVoxelBrick brick = deserialize(record);
        return brick.key().equals(key) ? Optional.of(brick) : Optional.empty();
    }

    /**
     * Makes all writes since the previous flush durable as one batch.
     *
     * <p>Gameplay updates intentionally do not call {@code FileChannel.force}
     * per brick. A single changed chunk can rebuild many mip records, and
     * forcing every record was stalling the integrated server for seconds.</p>
     */
    public void flush() throws IOException {
        synchronized (lock) {
            IOException failure = null;
            for (RegionFile region : openRegions.values()) {
                try {
                    region.flush();
                } catch (IOException error) {
                    if (failure == null) failure = error;
                    else failure.addSuppressed(error);
                }
            }
            if (failure != null) throw failure;
        }
    }

    /** Rewrites all currently known records and drops unreachable append data. */
    public void compactAll() throws IOException {
        synchronized (lock) {
            closeOpenRegions();
            if (!Files.isDirectory(root)) return;
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(value -> value.toString().endsWith(".pvr")).toList()) {
                    compact(path);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            closeOpenRegions();
        }
    }

    private RegionFile region(RegionKey key) throws IOException {
        synchronized (lock) {
            RegionFile existing = openRegions.get(key);
            if (existing != null) return existing;
            while (openRegions.size() >= MAX_OPEN_REGIONS) {
                var iterator = openRegions.entrySet().iterator();
                RegionFile eldest = iterator.next().getValue();
                iterator.remove();
                eldest.close();
            }
            RegionFile opened = new RegionFile(path(key));
            openRegions.put(key, opened);
            return opened;
        }
    }

    private Path path(RegionKey key) {
        return root.resolve(key.face.name().toLowerCase())
                .resolve("lod" + key.lod)
                .resolve(key.regionU + "." + key.regionY + "." + key.regionV + ".pvr");
    }

    private static int lodFromPath(Path path) {
        Path parent = path.getParent();
        if (parent == null) return -1;
        String name = parent.getFileName().toString();
        if (!name.startsWith("lod")) return -1;
        try {
            return Integer.parseInt(name.substring(3));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void closeOpenRegions() throws IOException {
        IOException failure = null;
        for (RegionFile region : openRegions.values()) {
            try {
                region.close();
            } catch (IOException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        openRegions.clear();
        if (failure != null) throw failure;
    }

    private static int localIndex(PlanetVoxelBrickKey key) {
        int x = Math.floorMod(key.brickU(), REGION_EDGE);
        int y = Math.floorMod(key.brickY(), REGION_EDGE);
        int z = Math.floorMod(key.brickV(), REGION_EDGE);
        return x | (y << REGION_BITS) | (z << (REGION_BITS * 2));
    }

    private static byte[] serialize(PlanetVoxelBrick brick) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(24_000);
        try (DataOutputStream output = new DataOutputStream(raw)) {
            PlanetVoxelBrickKey key = brick.key();
            output.writeByte(key.face().ordinal());
            output.writeByte(key.lod());
            output.writeInt(key.brickU());
            output.writeInt(key.brickY());
            output.writeInt(key.brickV());
            output.writeLong(brick.revision());
            long[] palette = brick.paletteCopy();
            output.writeInt(palette.length);
            for (long material : palette) output.writeLong(material);
            for (short index : brick.indicesCopy()) output.writeShort(index);
            output.write(brick.coverageCopy());
            output.write(brick.authorityCopy());
        }
        byte[] uncompressed = raw.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(uncompressed);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream(uncompressed.length / 2);
        try (DataOutputStream header = new DataOutputStream(compressed)) {
            header.writeInt(uncompressed.length);
            header.writeInt((int) crc.getValue());
            try (DeflaterOutputStream deflater = new DeflaterOutputStream(
                    compressed, new Deflater(Deflater.BEST_SPEED), 8192, true)) {
                deflater.write(uncompressed);
                deflater.finish();
            }
        }
        return compressed.toByteArray();
    }

    private static PlanetVoxelBrick deserialize(byte[] stored) throws IOException {
        if (stored.length < 9 || stored.length > MAX_COMPRESSED_RECORD) {
            throw new IOException("invalid voxel record length " + stored.length);
        }
        try (DataInputStream header = new DataInputStream(new ByteArrayInputStream(stored))) {
            int expectedLength = header.readInt();
            int expectedCrc = header.readInt();
            if (expectedLength <= 0 || expectedLength > 2 * 1024 * 1024) {
                throw new IOException("invalid uncompressed voxel length " + expectedLength);
            }
            byte[] raw;
            try (InflaterInputStream inflater = new InflaterInputStream(header);
                 ByteArrayOutputStream output = new ByteArrayOutputStream(expectedLength)) {
                inflater.transferTo(output);
                raw = output.toByteArray();
            }
            if (raw.length != expectedLength) {
                throw new IOException("voxel record expanded to " + raw.length + " expected " + expectedLength);
            }
            CRC32 crc = new CRC32();
            crc.update(raw);
            if ((int) crc.getValue() != expectedCrc) {
                throw new IOException("voxel record CRC mismatch");
            }
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(raw))) {
                int faceOrdinal = input.readUnsignedByte();
                int lod = input.readUnsignedByte();
                CubeNetSurfaceTransform.Face[] faces = CubeNetSurfaceTransform.Face.values();
                if (faceOrdinal >= faces.length) throw new IOException("invalid face " + faceOrdinal);
                PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(faces[faceOrdinal], lod,
                        input.readInt(), input.readInt(), input.readInt());
                long revision = input.readLong();
                int paletteLength = input.readInt();
                if (paletteLength <= 0 || paletteLength > PlanetVoxelBrick.MAX_PALETTE) {
                    throw new IOException("invalid palette length " + paletteLength);
                }
                long[] palette = new long[paletteLength];
                for (int i = 0; i < paletteLength; i++) palette[i] = input.readLong();
                short[] indices = new short[PlanetVoxelBrick.CELL_COUNT];
                for (int i = 0; i < indices.length; i++) indices[i] = input.readShort();
                byte[] coverage = input.readNBytes(PlanetVoxelBrick.CELL_COUNT);
                if (coverage.length != PlanetVoxelBrick.CELL_COUNT) {
                    throw new IOException("truncated voxel coverage");
                }
                int remaining = input.available();
                if (remaining == 0) {
                    // Legacy records predate per-cell authority. Visible cells
                    // came from exact chunk ingestion; air remained unknown.
                    return new PlanetVoxelBrick(key, revision, palette, indices, coverage);
                }
                if (remaining != PlanetVoxelBrick.CELL_COUNT) {
                    throw new IOException("invalid voxel authority payload " + remaining);
                }
                byte[] authority = input.readNBytes(PlanetVoxelBrick.CELL_COUNT);
                if (authority.length != PlanetVoxelBrick.CELL_COUNT || input.read() != -1) {
                    throw new IOException("truncated or trailing voxel authority");
                }
                return new PlanetVoxelBrick(key, revision, palette, indices, coverage, authority);
            }
        }
    }

    private static void compact(Path source) throws IOException {
        Path temporary = source.resolveSibling(source.getFileName() + ".compact");
        try (RegionFile input = new RegionFile(source);
             RegionFile output = new RegionFile(temporary)) {
            for (int index = 0; index < ENTRY_COUNT; index++) {
                byte[] record = input.read(index);
                if (record != null) output.write(index, record);
            }
        }
        Files.move(temporary, source, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private record RegionKey(CubeNetSurfaceTransform.Face face, int lod,
                             int regionU, int regionY, int regionV) {
        static RegionKey from(PlanetVoxelBrickKey key) {
            return new RegionKey(key.face(), key.lod(),
                    Math.floorDiv(key.brickU(), REGION_EDGE),
                    Math.floorDiv(key.brickY(), REGION_EDGE),
                    Math.floorDiv(key.brickV(), REGION_EDGE));
        }
    }

    private static final class RegionFile implements Closeable {
        private final RandomAccessFile file;
        private final FileChannel channel;
        private boolean dirty;

        RegionFile(Path path) throws IOException {
            Files.createDirectories(path.getParent());
            boolean created = !Files.exists(path);
            RandomAccessFile openedFile = new RandomAccessFile(path.toFile(), "rw");
            FileChannel openedChannel = openedFile.getChannel();
            try {
                this.file = openedFile;
                this.channel = openedChannel;
                if (created || file.length() == 0L) initialize();
                else validate();
            } catch (IOException | RuntimeException error) {
                try {
                    openedFile.close();
                } catch (IOException closeError) {
                    error.addSuppressed(closeError);
                }
                throw error;
            }
        }

        synchronized void write(int index, byte[] record) throws IOException {
            if (index < 0 || index >= ENTRY_COUNT) throw new IndexOutOfBoundsException(index);
            if (record.length <= 0 || record.length > MAX_COMPRESSED_RECORD) {
                throw new IOException("invalid compressed record length " + record.length);
            }
            long offset = Math.max(file.length(), DATA_START);
            file.seek(offset);
            file.write(record);
            file.seek(FIXED_HEADER_BYTES + (long) index * Long.BYTES);
            file.writeLong(offset);
            file.seek(FIXED_HEADER_BYTES + OFFSET_TABLE_BYTES + (long) index * Integer.BYTES);
            file.writeInt(record.length);
            dirty = true;
        }

        synchronized void delete(int index) throws IOException {
            if (index < 0 || index >= ENTRY_COUNT) throw new IndexOutOfBoundsException(index);
            file.seek(FIXED_HEADER_BYTES + (long) index * Long.BYTES);
            file.writeLong(0L);
            file.seek(FIXED_HEADER_BYTES + OFFSET_TABLE_BYTES + (long) index * Integer.BYTES);
            file.writeInt(0);
            dirty = true;
        }

        synchronized byte[] read(int index) throws IOException {
            if (index < 0 || index >= ENTRY_COUNT) throw new IndexOutOfBoundsException(index);
            file.seek(FIXED_HEADER_BYTES + (long) index * Long.BYTES);
            long offset = file.readLong();
            file.seek(FIXED_HEADER_BYTES + OFFSET_TABLE_BYTES + (long) index * Integer.BYTES);
            int length = file.readInt();
            if (offset == 0L && length == 0) return null;
            if (offset < DATA_START || length <= 0 || length > MAX_COMPRESSED_RECORD
                    || offset + length > file.length()) {
                throw new IOException("corrupt voxel region entry " + index);
            }
            byte[] data = new byte[length];
            file.seek(offset);
            file.readFully(data);
            return data;
        }

        private void initialize() throws IOException {
            file.setLength(DATA_START);
            file.seek(0L);
            file.writeInt(MAGIC);
            file.writeInt(VERSION);
            file.writeInt(ENTRY_COUNT);
            file.writeInt(0);
            channel.force(true);
        }

        private void validate() throws IOException {
            file.seek(0L);
            if (file.readInt() != MAGIC || file.readInt() != VERSION
                    || file.readInt() != ENTRY_COUNT || file.length() < DATA_START) {
                throw new IOException("unsupported/corrupt Genesis voxel region");
            }
        }

        synchronized void flush() throws IOException {
            if (!dirty) return;
            channel.force(true);
            dirty = false;
        }

        @Override
        public synchronized void close() throws IOException {
            flush();
            file.close();
        }
    }
}
