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
import java.util.List;

/** Debug aid: prints stored LOD columns. Runs only with -Dvantage.dump=<dimension dir>. */
class DumpLodData {
    @Test
    void dump() throws Exception {
        String dir = System.getProperty("vantage.dump");
        Assumptions.assumeTrue(dir != null);
        Path root = Path.of(dir);
        List<String> visuals = Files.readAllLines(root.resolve("visuals.txt"));
        String[] names = new String[200000];
        for (String l : visuals) {
            String[] p = l.split("\t");
            names[Integer.parseInt(p[0])] = p[1] + (p[2].equals("-") ? "" : "@" + p[2]);
        }
        names[1] = "FILLER";
        int cx = Integer.getInteger("vantage.dump.x", 0);
        int cz = Integer.getInteger("vantage.dump.z", 0);
        try (RegionStore store = new RegionStore(root, 384)) {
            for (int bx = cx - 64; bx <= cx + 64; bx += 32) {
                int x = bx;
                int z = cz;
                StringBuilder sb = new StringBuilder("column x=" + x + " z=" + z + ":");
                for (int sy = 0; sy < 12; sy++) {
                    long key = SectionKey.of(0, Math.floorDiv(x, 32), sy, Math.floorDiv(z, 32));
                    long e = store.entry(key);
                    if (!RegionStore.isPresent(e)) {
                        sb.append("\n  sy=").append(sy).append(" absent");
                        continue;
                    }
                    if (RegionStore.isUniform(e)) {
                        int v = RegionStore.uniformValue(e);
                        sb.append("\n  sy=").append(sy).append(" uniform ").append(describe(v, names));
                        continue;
                    }
                    int[] vox = SectionCodec.decode(store.readBlob(key), null);
                    int lx = Math.floorMod(x, 32), lz = Math.floorMod(z, 32);
                    int last = Integer.MIN_VALUE;
                    for (int y = 0; y < 32; y++) {
                        int v = vox[Lod.index(lx, y, lz)];
                        if (v != last) {
                            sb.append("\n  y=").append(-64 + sy * 32 + y).append(' ').append(describe(v, names));
                            last = v;
                        }
                    }
                }
                System.out.println(sb);
            }
        }
    }

    private static String describe(int v, String[] names) {
        int vid = Voxel.vid(v);
        return (vid == 0 ? "air" : names[vid]) + " b" + Voxel.blockLight(v) + " s" + Voxel.skyLight(v);
    }
}
