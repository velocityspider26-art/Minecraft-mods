package com.vantage.client.visual;

import com.mojang.blaze3d.platform.NativeImage;
import com.vantage.core.VisualClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Works out how a block state should look at a distance by reading its baked model: which LOD
 * class it belongs to, whether it is a decoration that disappears at a distance, whether its colour
 * depends on the biome, and its average colour per face.
 *
 * <p>Only reads immutable model and texture data, so it is safe on worker threads (vanilla
 * meshes chunks off-thread with the same calls).
 */
public final class VisualAnalyzer {
    /** Structural, biome-independent facts about a state. */
    public record Shape(Kind kind, byte cls, boolean biomeTinted) {
    }

    public enum Kind {
        /** Nothing to draw. */
        AIR,
        /** Small or sparse (plants, torches, rails): invisible at LOD distances. */
        DECORATION,
        /** Drawn as a voxel. */
        SOLID
    }

    private record SpriteStats(int r, int g, int b, float coverage, float alpha) {
    }

    private static final Shape AIR_SHAPE = new Shape(Kind.AIR, VisualClass.AIR, false);
    private static final Shape DECORATION_SHAPE = new Shape(Kind.DECORATION, VisualClass.AIR, false);
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ConcurrentHashMap<BlockState, Shape> shapes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TextureAtlasSprite, SpriteStats> sprites = new ConcurrentHashMap<>();
    private volatile Holder<Biome> fallbackBiome;

    public void setFallbackBiome(Holder<Biome> biome) {
        this.fallbackBiome = biome;
    }

    /** Drops everything derived from resources; call after a resource reload. */
    public void reset() {
        this.sprites.clear();
    }

    public Shape shape(BlockState state) {
        Shape s = this.shapes.get(state);
        if (s == null) {
            s = this.computeShape(state);
            this.shapes.put(state, s);
        }
        return s;
    }

    private Shape computeShape(BlockState state) {
        try {
            if (state.isAir()) {
                return AIR_SHAPE;
            }
            FluidState fluid = state.getFluidState();
            if (state.getRenderShape() == RenderShape.INVISIBLE) {
                if (fluid.isEmpty()) {
                    return AIR_SHAPE;
                }
                boolean lava = fluid.is(FluidTags.LAVA);
                return new Shape(Kind.SOLID, lava ? VisualClass.OPAQUE : VisualClass.TRANSLUCENT, !lava);
            }
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            RandomSource rand = RandomSource.create();
            boolean tinted = false;
            double upCoverage = 0;
            double coverageSum = 0;
            double areaSum = 0;
            for (int d = 0; d <= DIRECTIONS.length; d++) {
                Direction side = d < DIRECTIONS.length ? DIRECTIONS[d] : null;
                rand.setSeed(42L);
                for (BakedQuad q : model.getQuads(state, side, rand, ModelData.EMPTY, null)) {
                    SpriteStats ss = this.stats(q.getSprite());
                    double[] geo = quadGeometry(q.getVertices());
                    double area = geo[0];
                    tinted |= q.isTinted();
                    areaSum += area;
                    coverageSum += area * ss.coverage;
                    if (q.getDirection() == Direction.UP) {
                        upCoverage += geo[1] * ss.coverage;
                    }
                }
            }
            if (areaSum <= 0) {
                return DECORATION_SHAPE;
            }
            VoxelShape vs = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            boolean full = Block.isShapeFullBlock(vs);
            double height = vs.isEmpty() ? 0 : vs.max(Direction.Axis.Y);
            if (!full && (upCoverage < 0.5 || (height < 0.25 && upCoverage < 0.75))) {
                return DECORATION_SHAPE;
            }
            boolean translucentLayer = model.getRenderTypes(state, rand, ModelData.EMPTY).contains(RenderType.translucent());
            double coverage = coverageSum / areaSum;
            byte cls = translucentLayer || coverage < 0.3 ? VisualClass.TRANSLUCENT : VisualClass.OPAQUE;
            return new Shape(Kind.SOLID, cls, tinted);
        } catch (RuntimeException e) {
            return new Shape(Kind.SOLID, VisualClass.OPAQUE, false);
        }
    }

    /**
     * Per-face colours as packed RGBA ({@code r | g<<8 | b<<16 | a<<24}) in Direction order.
     */
    public int[] faceColors(BlockState state, @Nullable Holder<Biome> biome) {
        Holder<Biome> b = biome != null ? biome : this.fallbackBiome;
        try {
            Shape shape = this.shape(state);
            FluidState fluid = state.getFluidState();
            if (state.getRenderShape() == RenderShape.INVISIBLE && !fluid.isEmpty()) {
                return this.fluidColors(state, fluid, b);
            }
            return this.modelColors(state, b, shape.cls == VisualClass.TRANSLUCENT);
        } catch (RuntimeException e) {
            return fallbackColors(state);
        }
    }

    private int[] fluidColors(BlockState state, FluidState fluid, @Nullable Holder<Biome> biome) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        BiomeTintGetter getter = biome != null ? new BiomeTintGetter(state, biome) : null;
        ResourceLocation still = getter != null ? ext.getStillTexture(fluid, getter, BlockPos.ZERO) : ext.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        SpriteStats ss = this.stats(sprite);
        int tint = getter != null ? ext.getTintColor(fluid, getter, BlockPos.ZERO) : ext.getTintColor();
        boolean lava = fluid.is(FluidTags.LAVA);
        float alpha = lava ? 1f : Math.max(0.55f, ss.alpha * ((tint >>> 24) / 255f));
        int c = pack(ss.r * ((tint >> 16) & 255) / 255f, ss.g * ((tint >> 8) & 255) / 255f, ss.b * (tint & 255) / 255f, alpha);
        return new int[]{c, c, c, c, c, c};
    }

    private int[] modelColors(BlockState state, @Nullable Holder<Biome> biome, boolean translucent) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource rand = RandomSource.create();
        double[][] face = new double[6][5];
        double[] all = new double[5];
        int[] tintCache = new int[8];
        java.util.Arrays.fill(tintCache, Integer.MIN_VALUE);
        BiomeTintGetter getter = biome != null ? new BiomeTintGetter(state, biome) : null;
        for (int d = 0; d <= DIRECTIONS.length; d++) {
            Direction side = d < DIRECTIONS.length ? DIRECTIONS[d] : null;
            rand.setSeed(42L);
            for (BakedQuad q : model.getQuads(state, side, rand, ModelData.EMPTY, null)) {
                SpriteStats ss = this.stats(q.getSprite());
                double w = quadGeometry(q.getVertices())[0] * Math.max(ss.coverage, 0.05);
                float r = ss.r, g = ss.g, bl = ss.b;
                if (q.isTinted()) {
                    int tint = this.tint(state, getter, q.getTintIndex(), tintCache);
                    r *= ((tint >> 16) & 255) / 255f;
                    g *= ((tint >> 8) & 255) / 255f;
                    bl *= (tint & 255) / 255f;
                }
                double[] f = face[q.getDirection().ordinal()];
                accumulate(f, r, g, bl, ss.alpha, w);
                accumulate(all, r, g, bl, ss.alpha, w);
            }
        }
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) {
            double[] f = face[i][4] > 0 ? face[i] : all;
            if (f[4] <= 0) {
                return fallbackColors(state);
            }
            float alpha = translucent ? (float) Math.min(0.9, Math.max(0.25, f[3] / f[4])) : 1f;
            out[i] = pack((float) (f[0] / f[4]), (float) (f[1] / f[4]), (float) (f[2] / f[4]), alpha);
        }
        return out;
    }

    private int tint(BlockState state, @Nullable BiomeTintGetter getter, int index, int[] cache) {
        if (index >= 0 && index < cache.length && cache[index] != Integer.MIN_VALUE) {
            return cache[index];
        }
        int c;
        try {
            c = Minecraft.getInstance().getBlockColors().getColor(state, getter, getter != null ? BlockPos.ZERO : null, index);
        } catch (RuntimeException e) {
            c = -1;
        }
        if (c == -1) {
            c = 0xFFFFFF;
        }
        if (index >= 0 && index < cache.length) {
            cache[index] = c;
        }
        return c;
    }

    private static void accumulate(double[] f, float r, float g, float b, float a, double w) {
        f[0] += r * w;
        f[1] += g * w;
        f[2] += b * w;
        f[3] += a * w;
        f[4] += w;
    }

    private SpriteStats stats(TextureAtlasSprite sprite) {
        SpriteStats s = this.sprites.get(sprite);
        if (s == null) {
            s = computeStats(sprite);
            this.sprites.put(sprite, s);
        }
        return s;
    }

    private static SpriteStats computeStats(TextureAtlasSprite sprite) {
        NativeImage img = sprite.contents().getOriginalImage();
        int w = Math.min(sprite.contents().width(), img.getWidth());
        int h = Math.min(sprite.contents().height(), img.getHeight());
        long r = 0, g = 0, b = 0, aSum = 0;
        int opaque = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img.getPixelRGBA(x, y);
                int a = c >>> 24;
                r += (long) (c & 255) * a;
                g += (long) ((c >> 8) & 255) * a;
                b += (long) ((c >> 16) & 255) * a;
                aSum += a;
                if (a >= 128) {
                    opaque++;
                }
            }
        }
        int n = Math.max(1, w * h);
        if (aSum == 0) {
            return new SpriteStats(0, 0, 0, 0f, 0f);
        }
        return new SpriteStats((int) (r / aSum), (int) (g / aSum), (int) (b / aSum), opaque / (float) n, aSum / (255f * n));
    }

    /** {area, area projected on the XZ plane} of a quad in block units. */
    private static double[] quadGeometry(int[] v) {
        int stride = v.length / 4;
        float x0 = Float.intBitsToFloat(v[0]), y0 = Float.intBitsToFloat(v[1]), z0 = Float.intBitsToFloat(v[2]);
        double area = 0;
        double projected = 0;
        for (int t = 1; t <= 2; t++) {
            int i1 = t * stride, i2 = (t + 1) * stride;
            float ax = Float.intBitsToFloat(v[i1]) - x0, ay = Float.intBitsToFloat(v[i1 + 1]) - y0, az = Float.intBitsToFloat(v[i1 + 2]) - z0;
            float bx = Float.intBitsToFloat(v[i2]) - x0, by = Float.intBitsToFloat(v[i2 + 1]) - y0, bz = Float.intBitsToFloat(v[i2 + 2]) - z0;
            double cx = ay * bz - az * by, cy = az * bx - ax * bz, cz = ax * by - ay * bx;
            area += 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
            projected += 0.5 * Math.abs(cy);
        }
        return new double[]{area, projected};
    }

    private static int[] fallbackColors(BlockState state) {
        int col;
        try {
            col = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
        } catch (RuntimeException e) {
            col = 0x808080;
        }
        int c = pack((col >> 16) & 255, (col >> 8) & 255, col & 255, 1f);
        return new int[]{c, c, c, c, c, c};
    }

    private static int pack(float r, float g, float b, float a) {
        int ri = Math.min(255, Math.max(0, Math.round(r)));
        int gi = Math.min(255, Math.max(0, Math.round(g)));
        int bi = Math.min(255, Math.max(0, Math.round(b)));
        int ai = Math.min(255, Math.max(0, Math.round(a * 255f)));
        return ri | (gi << 8) | (bi << 16) | (ai << 24);
    }
}
