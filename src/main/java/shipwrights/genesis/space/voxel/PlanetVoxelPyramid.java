package shipwrights.genesis.space.voxel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Maintains leaf bricks and all affected mip ancestors. */
public final class PlanetVoxelPyramid {
    public record Update(List<PlanetVoxelBrick> upserts,
                         List<PlanetVoxelBrickKey> removals) {
        public Update {
            upserts = List.copyOf(upserts);
            removals = List.copyOf(removals);
        }
    }

    private final PlanetVoxelStore memory;
    private final PlanetVoxelRegionStore disk;
    private final AtomicLong revisions;
    private final int maximumLod;

    public PlanetVoxelPyramid(PlanetVoxelStore memory, PlanetVoxelRegionStore disk,
                              AtomicLong revisions, int maximumLod) {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.disk = disk;
        this.revisions = Objects.requireNonNull(revisions, "revisions");
        if (maximumLod < 1 || maximumLod > PlanetVoxelBrickKey.MAX_LOD) {
            throw new IllegalArgumentException("invalid maximumLod " + maximumLod);
        }
        this.maximumLod = maximumLod;
    }

    public Update acceptLeaf(PlanetVoxelBrick leaf) throws IOException {
        return applyBatch(List.of(leaf), List.of());
    }

    /** Accepts a predicted or exact brick at any pyramid level. */
    public Update acceptBrick(PlanetVoxelBrick brick) throws IOException {
        Objects.requireNonNull(brick, "brick");
        if (brick.key().lod() > maximumLod) {
            throw new IllegalArgumentException("brick exceeds configured pyramid LOD");
        }
        List<PlanetVoxelBrick> upserts = new ArrayList<>();
        List<PlanetVoxelBrickKey> removals = new ArrayList<>();
        PlanetVoxelBrick stored = storeMerged(brick);
        record(stored, upserts, removals);
        PlanetVoxelBrickKey parentKey = stored.key().parent();
        for (int lod = stored.key().lod() + 1; lod <= maximumLod; lod++) {
            PlanetVoxelBrick[] children = children(parentKey);
            if (!hasData(children)) {
                if (delete(parentKey)) removals.add(parentKey);
            } else {
                PlanetVoxelBrick parent = PlanetVoxelReducer.reduce(children,
                        existing(parentKey), revisions.incrementAndGet());
                record(storeMerged(parent), upserts, removals);
            }
            parentKey = parentKey.parent();
        }
        return new Update(upserts, removals);
    }

    public Update removeLeaf(PlanetVoxelBrickKey leafKey) throws IOException {
        return applyBatch(List.of(), List.of(leafKey));
    }

    /** Rebuilds each affected parent exactly once, even when many sections changed. */
    public Update applyBatch(List<PlanetVoxelBrick> leafUpserts,
                             List<PlanetVoxelBrickKey> leafRemovals) throws IOException {
        List<PlanetVoxelBrick> upserts = new ArrayList<>();
        List<PlanetVoxelBrickKey> removals = new ArrayList<>();
        Set<PlanetVoxelBrickKey> currentParents = new HashSet<>();

        for (PlanetVoxelBrick brick : leafUpserts) {
            if (brick.key().lod() != 0) throw new IllegalArgumentException("batch upsert is not LOD0");
            record(storeMerged(brick), upserts, removals);
            currentParents.add(brick.key().parent());
        }
        for (PlanetVoxelBrickKey key : leafRemovals) {
            if (key.lod() != 0) throw new IllegalArgumentException("batch removal is not LOD0");
            if (delete(key)) removals.add(key);
            currentParents.add(key.parent());
        }

        for (int lod = 1; lod <= maximumLod && !currentParents.isEmpty(); lod++) {
            Set<PlanetVoxelBrickKey> nextParents = new HashSet<>();
            for (PlanetVoxelBrickKey parentKey : currentParents) {
                PlanetVoxelBrick[] children = children(parentKey);
                if (!hasData(children)) {
                    if (delete(parentKey)) removals.add(parentKey);
                } else {
                    PlanetVoxelBrick parent = PlanetVoxelReducer.reduce(children,
                            existing(parentKey), revisions.incrementAndGet());
                    record(storeMerged(parent), upserts, removals);
                }
                if (lod < maximumLod) nextParents.add(parentKey.parent());
            }
            currentParents = nextParents;
        }
        return new Update(upserts, removals);
    }

    public boolean contains(PlanetVoxelBrickKey key) throws IOException {
        if (memory.contains(key)) return true;
        if (disk == null) return false;
        PlanetVoxelBrick loaded = disk.read(key).orElse(null);
        if (loaded != null) memory.put(loaded);
        return loaded != null;
    }

    private PlanetVoxelBrick[] children(PlanetVoxelBrickKey parent) throws IOException {
        int childLod = parent.lod() - 1;
        PlanetVoxelBrick[] children = new PlanetVoxelBrick[8];
        for (int slot = 0; slot < 8; slot++) {
            PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(parent.face(), childLod,
                    parent.brickU() * 2 + (slot & 1),
                    parent.brickY() * 2 + ((slot >>> 1) & 1),
                    parent.brickV() * 2 + ((slot >>> 2) & 1));
            PlanetVoxelBrick child = memory.get(key);
            if (child == null && disk != null) {
                child = disk.read(key).orElse(null);
                if (child != null) memory.put(child);
            }
            children[slot] = child;
        }
        return children;
    }

    private static boolean hasData(PlanetVoxelBrick[] bricks) {
        for (PlanetVoxelBrick brick : bricks) {
            if (brick != null && brick.hasData()) return true;
        }
        return false;
    }

    private PlanetVoxelBrick storeMerged(PlanetVoxelBrick brick) throws IOException {
        PlanetVoxelBrick previous = existing(brick.key());
        PlanetVoxelBrick merged = previous == null ? brick : merge(previous, brick);
        memory.put(merged);
        if (disk != null) disk.write(merged);
        return merged;
    }

    private PlanetVoxelBrick existing(PlanetVoxelBrickKey key) throws IOException {
        PlanetVoxelBrick brick = memory.get(key);
        if (brick == null && disk != null) {
            brick = disk.read(key).orElse(null);
            if (brick != null) memory.put(brick);
        }
        return brick;
    }

    private static PlanetVoxelBrick merge(PlanetVoxelBrick previous,
                                           PlanetVoxelBrick incoming) {
        if (!previous.key().equals(incoming.key())) {
            throw new IllegalArgumentException("cannot merge different voxel keys");
        }
        PlanetVoxelBrickBuilder output = new PlanetVoxelBrickBuilder(incoming.key(),
                Math.max(previous.revision(), incoming.revision()));
        boolean incomingIsNewer = incoming.revision() >= previous.revision();
        for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
            for (int z = 0; z < PlanetVoxelBrick.EDGE; z++) {
                for (int x = 0; x < PlanetVoxelBrick.EDGE; x++) {
                    PlanetVoxelAuthority oldSource = previous.authority(x, y, z);
                    PlanetVoxelAuthority newSource = incoming.authority(x, y, z);
                    PlanetVoxelBrick selected = newSource.outranks(oldSource)
                            || (newSource == oldSource && incomingIsNewer)
                            ? incoming : previous;
                    output.set(x, y, z, selected.material(x, y, z),
                            selected.coverage(x, y, z), selected.authority(x, y, z));
                }
            }
        }
        return output.build();
    }

    private static void record(PlanetVoxelBrick brick,
                               List<PlanetVoxelBrick> upserts,
                               List<PlanetVoxelBrickKey> removals) {
        if (brick.isEmpty()) removals.add(brick.key());
        else upserts.add(brick);
    }

    private boolean delete(PlanetVoxelBrickKey key) throws IOException {
        boolean existed = memory.remove(key) != null;
        if (disk != null) {
            if (!existed) existed = disk.read(key).isPresent();
            if (existed) disk.delete(key);
        }
        return existed;
    }
}
