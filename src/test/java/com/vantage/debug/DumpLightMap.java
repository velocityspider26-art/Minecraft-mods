package com.vantage.debug;

import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.core.Voxel;
import com.vantage.storage.RegionStore;
import com.vantage.storage.SectionCodec;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug aid: top-down image of stored level-0 data, one pixel per block column. Red: sky light
 * over the top surface; green: water on top; blue: no data. Runs only with
 * -Dvantage.dump=&lt;dimension dir&gt; -Dvantage.dump.png=&lt;output&gt; and the area as for {@link DumpTopMap}.
 */
class DumpLightMap {
    @Test
    void dump() throws Exception {
        String dir = System.getProperty("vantage.dump");
        String png = System.getProperty("vantage.dump.png");
        Assumptions.assumeTrue(dir != null && png != null && System.getProperty("vantage.dump.x0") != null);
        int x0 = Integer.getInteger("vantage.dump.x0"), z0 = Integer.getInteger("vantage.dump.z0");
        int x1 = Integer.getInteger("vantage.dump.x1"), z1 = Integer.getInteger("vantage.dump.z1");
        Map<Integer, String> states = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(dir, "visuals.txt"))) {
            String[] p = line.split("\t");
            states.put(Integer.parseInt(p[0]), p[1]);
        }
        Map<Long, int[]> cache = new HashMap<>();
        BufferedImage img = new BufferedImage(x1 - x0, z1 - z0, BufferedImage.TYPE_INT_RGB);
        int[] histogram = new int[17];
        try (RegionStore store = new RegionStore(Path.of(dir), 384)) {
            for (int z = z0; z < z1; z++) {
                for (int x = x0; x < x1; x++) {
                    int rgb = column(store, cache, states, x, z, histogram);
                    img.setRGB(x - x0, z - z0, rgb);
                }
            }
        }
        ImageIO.write(img, "png", new File(png));
        StringBuilder h = new StringBuilder("[dump] sky light over the surface:");
        for (int i = 0; i < 16; i++) {
            h.append(' ').append(i).append('=').append(histogram[i]);
        }
        System.out.println(h.append(" none=").append(histogram[16]));
    }

    private static int column(RegionStore store, Map<Long, int[]> cache, Map<Integer, String> states, int bx, int bz,
                              int[] histogram) throws Exception {
        int sx = Math.floorDiv(bx, Lod.SIZE), sz = Math.floorDiv(bz, Lod.SIZE);
        int lx = Math.floorMod(bx, Lod.SIZE), lz = Math.floorMod(bz, Lod.SIZE);
        int above = -1;
        for (int sy = 11; sy >= 0; sy--) {
            long key = SectionKey.of(0, sx, sy, sz);
            long e = store.entry(key);
            if (!RegionStore.isPresent(e)) {
                above = -1;
                continue;
            }
            int[] vox;
            if (RegionStore.isUniform(e)) {
                int v = RegionStore.uniformValue(e);
                if (Voxel.isAir(v)) {
                    above = v;
                    continue;
                }
                return color(states, v, above, histogram);
            }
            vox = cache.get(key);
            if (vox == null) {
                vox = SectionCodec.decode(store.readBlob(key), null);
                cache.put(key, vox);
            }
            for (int y = Lod.SIZE - 1; y >= 0; y--) {
                int v = vox[Lod.index(lx, y, lz)];
                if (!Voxel.isAir(v)) {
                    return color(states, v, above, histogram);
                }
                above = v;
            }
        }
        histogram[16]++;
        return 0x0000FF;
    }

    private static int color(Map<Integer, String> states, int v, int above, int[] histogram) {
        int sky = above == -1 ? 0 : Voxel.skyLight(above);
        histogram[sky]++;
        boolean water = states.getOrDefault(Voxel.vid(v), "").startsWith("minecraft:water");
        return (sky * 17) << 16 | (water ? 0xFF : 0) << 8;
    }
}
