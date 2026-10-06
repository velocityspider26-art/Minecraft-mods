package com.vantage.client.ingest;

import com.vantage.client.visual.VisualAnalyzer;
import com.vantage.client.visual.VisualRegistry;
import com.vantage.core.Mipper;
import com.vantage.core.VisualClass;
import com.vantage.core.Voxel;
import com.vantage.world.ColumnSource;
import com.vantage.world.VoxelColumn;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.Arrays;

/**
 * Turns a {@link ColumnSource} into LOD voxels for levels 0..4.
 *
 * <p>With cave culling on (and sky light available) it also strips what can never be seen from a
 * distance: unlit air becomes {@link Voxel#FILLER_VID}, then opaque voxels with six opaque
 * neighbours become filler too. Underground sections then compress to a single value.
 *
 * <p>Not thread-safe; use one per worker.
 */
public final class ColumnVoxelizer {
    private static final int VOLUME = 4096;
    @SuppressWarnings("unchecked")
    private static final ColumnSource.Section EMPTY_SECTION = new ColumnSource.Section(
            new BlockState[]{net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()}, null, new Holder[64], null, null);

    private final VisualRegistry registry;
    private final VisualAnalyzer analyzer;
    private final Mipper mipper;
    private final boolean hasSkyLight;

    private int[] pVid = new int[64];
    private byte[] pCls = new byte[64];
    private BlockState[] pState = new BlockState[64];
    private int[] biomeVidCache = new int[64 * 64];

    public ColumnVoxelizer(VisualRegistry registry, boolean hasSkyLight) {
        this.registry = registry;
        this.analyzer = registry.analyzer();
        this.mipper = new Mipper(registry);
        this.hasSkyLight = hasSkyLight;
    }

    public VoxelColumn voxelize(ColumnSource src, boolean caveCulling) {
        int n = src.sections.length;
        int[][] vox = new int[n][];
        byte[][] cls = new byte[n][];
        byte[][] sky = this.unpackSky(src);
        for (int i = 0; i < n; i++) {
            ColumnSource.Section s = src.sections[i];
            if (s == null) {
                // Saves leave out empty sections: they are air, and known to be.
                s = EMPTY_SECTION;
            }
            vox[i] = new int[VOLUME];
            cls[i] = new byte[VOLUME];
            this.fill(s, sky[i], vox[i], cls[i]);
        }

        if (caveCulling && this.hasSkyLight && src.lightValid) {
            for (int i = 0; i < n; i++) {
                if (vox[i] == null) {
                    continue;
                }
                int[] v = vox[i];
                byte[] c = cls[i];
                for (int k = 0; k < VOLUME; k++) {
                    if (c[k] == VisualClass.AIR && Voxel.light(v[k]) == 0) {
                        v[k] = Voxel.FILLER_VID;
                        c[k] = VisualClass.OPAQUE;
                    }
                }
            }
            this.fillFloodedCaves(vox, cls);
        }
        if (caveCulling) {
            this.fillEnclosed(vox, cls);
        }

        VoxelColumn out = new VoxelColumn(src.chunkX, src.chunkZ, n);
        for (int i = 0; i < n; i++) {
            if (vox[i] == null) {
                continue;
            }
            int[] l1 = new int[512];
            int[] l2 = new int[64];
            int[] l3 = new int[8];
            int[] l4 = new int[1];
            this.mipper.downsample(vox[i], 16, l1, 8, 0, 0, 0);
            this.mipper.downsample(l1, 8, l2, 4, 0, 0, 0);
            this.mipper.downsample(l2, 4, l3, 2, 0, 0, 0);
            this.mipper.downsample(l3, 2, l4, 1, 0, 0, 0);
            out.levels[i] = new int[][]{vox[i], l1, l2, l3, l4};
        }
        return out;
    }

    /**
     * Sky light per section, unpacked to one byte per voxel. Sections without stored sky light take
     * the bottom layer of the nearest stored section above them (as Minecraft does), or full sky.
     */
    private byte[][] unpackSky(ColumnSource src) {
        int n = src.sections.length;
        byte[][] out = new byte[n][];
        byte[] inherited = null;
        for (int i = n - 1; i >= 0; i--) {
            ColumnSource.Section s = src.sections[i];
            byte[] layer = new byte[VOLUME];
            if (!src.lightValid) {
                Arrays.fill(layer, (byte) 15);
            } else if (s != null && s.skyLight() != null) {
                byte[] d = s.skyLight();
                for (int k = 0; k < VOLUME; k++) {
                    layer[k] = (byte) ColumnSource.Section.nibble(d, k);
                }
                inherited = d;
            } else if (inherited != null) {
                for (int k = 0; k < VOLUME; k++) {
                    layer[k] = (byte) ColumnSource.Section.nibble(inherited, k & 0xFF);
                }
            } else {
                Arrays.fill(layer, (byte) (this.hasSkyLight ? 15 : 0));
            }
            out[i] = layer;
        }
        return out;
    }

    private void fill(ColumnSource.Section s, byte[] sky, int[] v, byte[] c) {
        BlockState[] palette = s.palette();
        int pn = palette.length;
        if (this.pVid.length < pn) {
            this.pVid = new int[pn];
            this.pCls = new byte[pn];
            this.pState = new BlockState[pn];
        }
        boolean anyTinted = false;
        for (int p = 0; p < pn; p++) {
            BlockState state = canonical(palette[p]);
            VisualAnalyzer.Shape shape = this.analyzer.shape(state);
            if (shape.kind() == VisualAnalyzer.Kind.DECORATION) {
                FluidState fluid = palette[p].getFluidState();
                if (!fluid.isEmpty()) {
                    state = canonical(fluid.createLegacyBlock());
                    shape = this.analyzer.shape(state);
                }
            }
            if (shape.kind() != VisualAnalyzer.Kind.SOLID) {
                this.pVid[p] = 0;
                this.pCls[p] = VisualClass.AIR;
                continue;
            }
            this.pState[p] = state;
            this.pCls[p] = shape.cls();
            if (shape.biomeTinted()) {
                this.pVid[p] = -1;
                anyTinted = true;
            } else {
                this.pVid[p] = this.registry.idFor(state, null);
                this.pCls[p] = this.registry.classOf(this.pVid[p]);
            }
        }
        if (anyTinted) {
            int need = pn * 64;
            if (this.biomeVidCache.length < need) {
                this.biomeVidCache = new int[need];
            }
            Arrays.fill(this.biomeVidCache, 0, need, -1);
        }

        short[] idx = s.indices();
        byte[] block = s.blockLight();
        Holder<Biome>[] biomes = s.biomes();
        for (int k = 0; k < VOLUME; k++) {
            int p = idx == null ? 0 : idx[k];
            int vid = this.pVid[p];
            byte cl = this.pCls[p];
            if (vid < 0) {
                int x = k & 15, z = (k >> 4) & 15, y = k >> 8;
                int cell = ((y >> 2) << 4) | ((z >> 2) << 2) | (x >> 2);
                int ci = p * 64 + cell;
                vid = this.biomeVidCache[ci];
                if (vid < 0) {
                    vid = this.registry.idFor(this.pState[p], biomes[cell]);
                    this.biomeVidCache[ci] = vid;
                }
                cl = this.registry.classOf(vid);
            }
            c[k] = cl;
            if (cl == VisualClass.OPAQUE) {
                v[k] = vid;
            } else {
                int bl = block == null ? 0 : ColumnSource.Section.nibble(block, k);
                v[k] = Voxel.pack(vid, bl, sky[k]);
            }
        }
    }

    /** Collapses fluids to their source block so flowing levels do not each need an id. */
    private static BlockState canonical(BlockState state) {
        if (state.getRenderShape() == RenderShape.INVISIBLE) {
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty() && fluid.getType() instanceof FlowingFluid flowing) {
                return flowing.getSource().defaultFluidState().createLegacyBlock();
            }
            if (!fluid.isEmpty() && fluid.getType() != Fluids.EMPTY) {
                return fluid.getType().defaultFluidState().createLegacyBlock();
            }
        }
        return state;
    }

    /**
     * Unlit water under a ceiling (flooded caves, aquifers) becomes filler, top down per column.
     * Open seas stay: their dark depths sit under more water, not under rock.
     */
    private void fillFloodedCaves(int[][] vox, byte[][] cls) {
        for (int xz = 0; xz < 256; xz++) {
            boolean ceiling = false;
            for (int i = vox.length - 1; i >= 0; i--) {
                if (vox[i] == null) {
                    ceiling = false;
                    continue;
                }
                int[] v = vox[i];
                byte[] c = cls[i];
                for (int y = 15; y >= 0; y--) {
                    int k = (y << 8) | xz;
                    byte cl = c[k];
                    if (cl == VisualClass.TRANSLUCENT && ceiling && Voxel.light(v[k]) == 0) {
                        v[k] = Voxel.FILLER_VID;
                        c[k] = VisualClass.OPAQUE;
                    } else {
                        ceiling = cl == VisualClass.OPAQUE;
                    }
                }
            }
        }
    }

    private void fillEnclosed(int[][] vox, byte[][] cls) {
        int n = vox.length;
        for (int i = 0; i < n; i++) {
            if (vox[i] == null) {
                continue;
            }
            byte[] c = cls[i];
            byte[] below = i > 0 ? cls[i - 1] : null;
            byte[] above = i + 1 < n ? cls[i + 1] : null;
            boolean bottomOfWorld = i == 0;
            int[] v = vox[i];
            for (int y = 0; y < 16; y++) {
                for (int z = 1; z < 15; z++) {
                    for (int x = 1; x < 15; x++) {
                        int k = (y << 8) | (z << 4) | x;
                        if (c[k] != VisualClass.OPAQUE || v[k] == Voxel.FILLER_VID) {
                            continue;
                        }
                        if (c[k - 1] != VisualClass.OPAQUE || c[k + 1] != VisualClass.OPAQUE
                                || c[k - 16] != VisualClass.OPAQUE || c[k + 16] != VisualClass.OPAQUE) {
                            continue;
                        }
                        boolean downOpaque;
                        if (y > 0) {
                            downOpaque = c[k - 256] == VisualClass.OPAQUE;
                        } else {
                            downOpaque = bottomOfWorld || (below != null && below[k + 15 * 256] == VisualClass.OPAQUE);
                        }
                        if (!downOpaque) {
                            continue;
                        }
                        boolean upOpaque;
                        if (y < 15) {
                            upOpaque = c[k + 256] == VisualClass.OPAQUE;
                        } else {
                            upOpaque = above != null && above[k - 15 * 256] == VisualClass.OPAQUE;
                        }
                        if (upOpaque) {
                            v[k] = Voxel.FILLER_VID;
                        }
                    }
                }
            }
        }
    }
}
