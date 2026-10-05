package com.vantage.debug;

import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.core.Voxel;
import com.vantage.storage.RegionStore;
import com.vantage.storage.SectionCodec;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.TreeMap;

/** Debug aid: histogram of one stored section per 16^3 octant. Runs only with -Dvantage.dump. */
class DumpSection {
    @Test
    void dump() throws Exception {
        String dir = System.getProperty("vantage.dump");
        Assumptions.assumeTrue(dir != null);
        int level = Integer.getInteger("vantage.dump.level", 0);
        int sx = Integer.getInteger("vantage.dump.sx", -2);
        int sy = Integer.getInteger("vantage.dump.sy", 3);
        int sz = Integer.getInteger("vantage.dump.sz", 0);
        try (RegionStore store = new RegionStore(Path.of(dir), 384)) {
            long key = SectionKey.of(level, sx, sy, sz);
            long e = store.entry(key);
            System.out.println("entry " + SectionKey.toString(key) + " present=" + RegionStore.isPresent(e) + " uniform=" + RegionStore.isUniform(e));
            if (!RegionStore.isPresent(e) || RegionStore.isUniform(e)) {
                return;
            }
            int[] vox = SectionCodec.decode(store.readBlob(key), null);
            for (int o = 0; o < 8; o++) {
                int ox = (o & 1) * 16, oy = ((o >> 2) & 1) * 16, oz = ((o >> 1) & 1) * 16;
                TreeMap<Integer, Integer> hist = new TreeMap<>();
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            hist.merge(Voxel.vid(vox[Lod.index(ox + x, oy + y, oz + z)]), 1, Integer::sum);
                        }
                    }
                }
                System.out.println("octant " + o + " (" + ox + "," + oy + "," + oz + "): " + hist);
            }
        }
    }
}
