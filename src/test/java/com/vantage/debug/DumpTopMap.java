package com.vantage.debug;

import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.core.Voxel;
import com.vantage.storage.RegionStore;
import com.vantage.storage.SectionCodec;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug aid: top-down map of stored level-0 data, one character per 4x4 blocks.
 * {@code ?} no section stored, {@code ' '} air only, {@code ~} water on top, {@code #} anything else.
 * Runs only with -Dvantage.dump=&lt;dimension dir&gt;; area via -Dvantage.dump.x0/z0/x1/z1 (blocks).
 */
class DumpTopMap {
    @Test
    void dump() throws Exception {
        String dir = System.getProperty("vantage.dump");
        Assumptions.assumeTrue(dir != null && System.getProperty("vantage.dump.x0") != null);
        int x0 = Integer.getInteger("vantage.dump.x0"), z0 = Integer.getInteger("vantage.dump.z0");
        int x1 = Integer.getInteger("vantage.dump.x1"), z1 = Integer.getInteger("vantage.dump.z1");
        Map<Integer, String> states = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(dir, "visuals.txt"))) {
            String[] p = line.split("\t");
            states.put(Integer.parseInt(p[0]), p[1]);
        }
        Map<Long, int[]> cache = new HashMap<>();
        try (RegionStore store = new RegionStore(Path.of(dir), 384)) {
            StringBuilder out = new StringBuilder();
            for (int z = z0; z < z1; z += 4) {
                out.append(String.format("%6d ", z));
                for (int x = x0; x < x1; x += 4) {
                    out.append(cell(store, cache, states, x + 1, z + 1));
                }
                out.append('\n');
            }
            System.out.println(out);
        }
    }

    private static char cell(RegionStore store, Map<Long, int[]> cache, Map<Integer, String> states, int bx, int bz) throws Exception {
        int sx = Math.floorDiv(bx, Lod.SIZE), sz = Math.floorDiv(bz, Lod.SIZE);
        int lx = Math.floorMod(bx, Lod.SIZE), lz = Math.floorMod(bz, Lod.SIZE);
        boolean any = false;
        for (int sy = 11; sy >= 0; sy--) {
            long key = SectionKey.of(0, sx, sy, sz);
            long e = store.entry(key);
            if (!RegionStore.isPresent(e)) {
                continue;
            }
            any = true;
            int[] vox;
            if (RegionStore.isUniform(e)) {
                int v = RegionStore.uniformValue(e);
                if (Voxel.isAir(v)) {
                    continue;
                }
                return classify(states, Voxel.vid(v));
            }
            vox = cache.get(key);
            if (vox == null) {
                vox = SectionCodec.decode(store.readBlob(key), null);
                cache.put(key, vox);
            }
            for (int y = Lod.SIZE - 1; y >= 0; y--) {
                int v = vox[Lod.index(lx, y, lz)];
                if (!Voxel.isAir(v)) {
                    return classify(states, Voxel.vid(v));
                }
            }
        }
        return any ? ' ' : '?';
    }

    private static char classify(Map<Integer, String> states, int vid) {
        String s = states.getOrDefault(vid, "");
        return s.startsWith("minecraft:water") ? '~' : '#';
    }
}
