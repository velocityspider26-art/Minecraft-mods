package com.velocityspider.crashphysics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashClientConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Waves on water surfaces: splashes send out rings, ships push up bow waves and drag wakes with foam behind them,
 * anything swimming or falling in leaves ripples, and rain dimples the water.
 * <p>
 * The surface is two height fields on a half-block grid, kept only in tiles of 8 × 8 blocks where something disturbed
 * the water, and stepped with the wave equation: a long, fast swell and a short, slower chop. Real water waves are
 * dispersive, long ones outrunning short ones, and the two together give the same look: a splash sends out several
 * rings, a ship's wake fans out feathered. Waves reflect off shores and die away over a few seconds. Foam is whipped
 * up where crests get steep and where things churn the water, lingers, spreads and breaks up.
 * <p>
 * Where the water is moving, the surface is drawn in its place: lifted slightly so troughs can dip below the calm
 * level, reflecting the sky more at grazing angles (Fresnel), glinting where a wave face catches the sun, lighter in
 * thin crests, white with foam. It fades back into the vanilla water at the edges.
 */
@EventBusSubscriber(modid = CrashPhysics.MODID, value = Dist.CLIENT)
public final class WaterWaves {

    /**
     * Grid spacing [m]
     */
    static final float CELL = 0.5f;
    /**
     * Cells along each side of a tile
     */
    static final int N = 16;
    static final int TILE_BLOCKS = 8;

    // The two wave bands: speed [m/s], damping [1/s], pull back to level [1/s²]
    private static final float SWELL_SPEED = 2.6f;
    private static final float SWELL_DAMPING = 0.3f;
    private static final float SWELL_RESTORE = 1.5f;
    private static final float CHOP_SPEED = 1.1f;
    private static final float CHOP_DAMPING = 0.9f;
    private static final float CHOP_RESTORE = 4.0f;

    private static final int SUBSTEPS = 2;
    private static final float DT = 0.05f / SUBSTEPS;
    private static final int MAX_TILES = 512;
    private static final float QUIET = 0.0015f;
    private static final int QUIET_TICKS = 60;
    /**
     * A wave this high at the edge of a tile makes the water next to it start moving too [m]
     */
    private static final float SPREAD = 0.003f;
    private static final float MAX_HEIGHT = 1.5f;

    /**
     * How far moving water is lifted above the calm surface, so its troughs can be drawn below it [m]
     */
    private static final float LIFT = 0.12f;

    /**
     * Foam decay per tick, and how much of it spreads to the neighbouring cells
     */
    private static final float FOAM_KEEP = 0.975f;
    private static final float FOAM_SPREAD = 0.06f;

    private static final Long2ObjectOpenHashMap<Tile> TILES = new Long2ObjectOpenHashMap<>();
    @Nullable
    private static ClientLevel trackedLevel;
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();
    private static final RandomSource RANDOM = RandomSource.create();
    private static long ticks;

    static final class Tile {
        final int tx;
        final int tz;
        final float[] swell = new float[N * N];
        final float[] swellSpeed = new float[N * N];
        final float[] swellLast = new float[N * N];
        final float[] chop = new float[N * N];
        final float[] chopSpeed = new float[N * N];
        final float[] chopLast = new float[N * N];
        final float[] foam = new float[N * N];
        final float[] foamLast = new float[N * N];
        final float[] scratch = new float[N * N];
        /**
         * Height of the water surface in each cell, or NaN where there is no open water
         */
        final float[] base = new float[N * N];
        final int color;
        boolean anyWet;
        boolean raining;
        int quiet;
        @Nullable
        Tile west, east, north, south;

        Tile(final int tx, final int tz, final int color) {
            this.tx = tx;
            this.tz = tz;
            this.color = color;
        }

        boolean wet(final int index) {
            return !Float.isNaN(this.base[index]);
        }
    }

    private WaterWaves() {
    }

    static boolean enabled() {
        return CrashClientConfig.WATER_WAVES.getAsBoolean();
    }

    /**
     * Pushes the water down in a round patch, and whips up foam there: a splash.
     *
     * @param depth how far the middle is pushed down [m]
     */
    public static void displace(final double x, final double y, final double z, final float radius, final float depth) {
        final float scaled = depth * strength();
        apply(x, y, z, radius * 1.5f, scaled * 0.6f, Band.SWELL_HEIGHT);
        apply(x, y, z, radius, scaled * 0.6f, Band.CHOP_HEIGHT);
        apply(x, y, z, radius, Math.min(0.8f, depth * 1.2f), Band.FOAM);
    }

    /**
     * Gives the water in a round patch a downward push, and churns it: something moving through it, applied once per
     * tick.
     *
     * @param speed downward speed given to the middle of the patch [m/s]
     */
    public static void push(final double x, final double y, final double z, final float radius, final float speed) {
        final float scaled = speed * strength();
        apply(x, y, z, radius * 1.4f, scaled * 0.5f, Band.SWELL_SPEED);
        apply(x, y, z, radius, scaled * 0.7f, Band.CHOP_SPEED);
        apply(x, y, z, radius * 0.7f, Math.min(0.06f, speed * 0.008f), Band.FOAM);
    }

    /**
     * A tiny ring: a raindrop or a falling droplet.
     */
    public static void ripple(final double x, final double y, final double z, final float depth) {
        apply(x, y, z, 0.3f, depth * strength(), Band.CHOP_HEIGHT);
    }

    private static float strength() {
        return (float) CrashClientConfig.WAVE_STRENGTH.getAsDouble();
    }

    private enum Band {
        SWELL_HEIGHT, SWELL_SPEED, CHOP_HEIGHT, CHOP_SPEED, FOAM
    }

    private static void apply(final double x, final double y, final double z, final float radius, final float amount, final Band band) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (!enabled() || level == null || amount == 0.0f || !inRange(x, z)) {
            return;
        }
        sync(level);

        final float reach = radius * 3.0f;
        final int x0 = Mth.floor((x - reach) / TILE_BLOCKS);
        final int x1 = Mth.floor((x + reach) / TILE_BLOCKS);
        final int z0 = Mth.floor((z - reach) / TILE_BLOCKS);
        final int z1 = Mth.floor((z + reach) / TILE_BLOCKS);
        final float inverse = 1.0f / (radius * radius);
        for (int tx = x0; tx <= x1; tx++) {
            for (int tz = z0; tz <= z1; tz++) {
                final Tile tile = tile(level, tx, tz, Mth.floor(y));
                if (tile == null || !tile.anyWet) {
                    continue;
                }
                for (int j = 0; j < N; j++) {
                    final double cz = tz * TILE_BLOCKS + (j + 0.5) * CELL - z;
                    for (int i = 0; i < N; i++) {
                        final int index = j * N + i;
                        if (!tile.wet(index)) {
                            continue;
                        }
                        final double cx = tx * TILE_BLOCKS + (i + 0.5) * CELL - x;
                        final double d2 = cx * cx + cz * cz;
                        if (d2 > reach * reach) {
                            continue;
                        }
                        final float gauss = (float) Math.exp(-d2 * inverse);
                        // Pushed down in the middle, the water it moved piled up in a ring around it: nothing added or lost
                        final float hat = gauss - 0.25f * (float) Math.exp(-d2 * inverse * 0.25);
                        switch (band) {
                            case SWELL_HEIGHT -> tile.swell[index] = Mth.clamp(tile.swell[index] - amount * hat, -MAX_HEIGHT, MAX_HEIGHT);
                            case CHOP_HEIGHT -> tile.chop[index] = Mth.clamp(tile.chop[index] - amount * hat, -MAX_HEIGHT, MAX_HEIGHT);
                            case SWELL_SPEED -> tile.swellSpeed[index] -= amount * hat;
                            case CHOP_SPEED -> tile.chopSpeed[index] -= amount * hat;
                            case FOAM -> tile.foam[index] = Math.min(1.5f, tile.foam[index] + amount * gauss);
                        }
                    }
                }
                tile.quiet = 0;
            }
        }
    }

    private static boolean inRange(final double x, final double z) {
        final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        final Vec3 position = camera.getPosition();
        final double range = CrashClientConfig.WAVE_RANGE.getAsInt();
        final double dx = x - position.x;
        final double dz = z - position.z;
        return dx * dx + dz * dz < range * range;
    }

    private static void sync(final ClientLevel level) {
        if (trackedLevel != level) {
            TILES.clear();
            trackedLevel = level;
        }
    }

    @Nullable
    private static Tile tile(final ClientLevel level, final int tx, final int tz, final int nearY) {
        final long key = ChunkPos.asLong(tx, tz);
        Tile tile = TILES.get(key);
        if (tile != null) {
            return tile;
        }
        if (TILES.size() >= MAX_TILES) {
            return null;
        }

        CURSOR.set(tx * TILE_BLOCKS + TILE_BLOCKS / 2, nearY, tz * TILE_BLOCKS + TILE_BLOCKS / 2);
        tile = new Tile(tx, tz, BiomeColors.getAverageWaterColor(level, CURSOR));
        for (int bx = 0; bx < TILE_BLOCKS; bx++) {
            for (int bz = 0; bz < TILE_BLOCKS; bz++) {
                final float surface = surfaceAt(level, tx * TILE_BLOCKS + bx, nearY, tz * TILE_BLOCKS + bz);
                tile.anyWet |= !Float.isNaN(surface);
                // Two by two cells per block
                for (int k = 0; k < 4; k++) {
                    final int i = bx * 2 + (k & 1);
                    final int j = bz * 2 + (k >> 1);
                    tile.base[j * N + i] = surface;
                }
            }
        }
        TILES.put(key, tile);
        link(tile);
        return tile;
    }

    private static void link(final Tile tile) {
        tile.west = TILES.get(ChunkPos.asLong(tile.tx - 1, tile.tz));
        tile.east = TILES.get(ChunkPos.asLong(tile.tx + 1, tile.tz));
        tile.north = TILES.get(ChunkPos.asLong(tile.tx, tile.tz - 1));
        tile.south = TILES.get(ChunkPos.asLong(tile.tx, tile.tz + 1));
        if (tile.west != null) tile.west.east = tile;
        if (tile.east != null) tile.east.west = tile;
        if (tile.north != null) tile.north.south = tile;
        if (tile.south != null) tile.south.north = tile;
    }

    private static void unlink(final Tile tile) {
        if (tile.west != null) tile.west.east = null;
        if (tile.east != null) tile.east.west = null;
        if (tile.north != null) tile.north.south = null;
        if (tile.south != null) tile.south.north = null;
    }

    /**
     * @return the height of the open water surface in a column near a height, or NaN if there is none
     */
    private static float surfaceAt(final ClientLevel level, final int x, final int nearY, final int z) {
        for (int y = nearY + 3; y >= nearY - 3; y--) {
            CURSOR.set(x, y, z);
            final FluidState fluid = level.getFluidState(CURSOR);
            if (fluid.is(FluidTags.WATER)) {
                CURSOR.set(x, y + 1, z);
                final BlockState above = level.getBlockState(CURSOR);
                if (!above.getFluidState().isEmpty() || !above.getCollisionShape(level, CURSOR).isEmpty()) {
                    // Covered, or deeper water than we looked for
                    return Float.NaN;
                }
                CURSOR.set(x, y, z);
                return y + fluid.getHeight(level, CURSOR);
            }
            final BlockState state = level.getBlockState(CURSOR);
            if (!state.isAir() && !state.getCollisionShape(level, CURSOR).isEmpty()) {
                return Float.NaN;
            }
        }
        return Float.NaN;
    }

    @SubscribeEvent
    static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || !enabled()) {
            TILES.clear();
            trackedLevel = null;
            return;
        }
        sync(level);
        if (minecraft.isPaused()) {
            return;
        }
        ticks++;

        CrashFxClient.rippleEntities(level);
        rain(level);
        if (TILES.isEmpty()) {
            return;
        }

        for (final Tile tile : TILES.values()) {
            System.arraycopy(tile.swell, 0, tile.swellLast, 0, N * N);
            System.arraycopy(tile.chop, 0, tile.chopLast, 0, N * N);
            System.arraycopy(tile.foam, 0, tile.foamLast, 0, N * N);
        }
        for (int step = 0; step < SUBSTEPS; step++) {
            for (final Tile tile : TILES.values()) {
                accelerate(tile, tile.swell, tile.swellSpeed, SWELL_SPEED, SWELL_DAMPING, SWELL_RESTORE, true);
                accelerate(tile, tile.chop, tile.chopSpeed, CHOP_SPEED, CHOP_DAMPING, CHOP_RESTORE, false);
            }
            for (final Tile tile : TILES.values()) {
                for (int index = 0; index < N * N; index++) {
                    if (tile.wet(index)) {
                        tile.swell[index] = Mth.clamp(tile.swell[index] + tile.swellSpeed[index] * DT, -MAX_HEIGHT, MAX_HEIGHT);
                        tile.chop[index] = Mth.clamp(tile.chop[index] + tile.chopSpeed[index] * DT, -MAX_HEIGHT, MAX_HEIGHT);
                    }
                }
            }
        }
        for (final Tile tile : TILES.values()) {
            whipFoam(tile);
        }
        spreadAndForget(level);
    }

    /**
     * Rain dimples the water near the player with little rings.
     */
    private static void rain(final ClientLevel level) {
        if (!level.isRaining()) {
            return;
        }
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final int radius = 12;
        if (ticks % 10 == 0) {
            // Bring the water around the player to life
            for (int tx = Mth.floor((camera.x - radius) / TILE_BLOCKS); tx <= Mth.floor((camera.x + radius) / TILE_BLOCKS); tx++) {
                for (int tz = Mth.floor((camera.z - radius) / TILE_BLOCKS); tz <= Mth.floor((camera.z + radius) / TILE_BLOCKS); tz++) {
                    final int cx = tx * TILE_BLOCKS + TILE_BLOCKS / 2;
                    final int cz = tz * TILE_BLOCKS + TILE_BLOCKS / 2;
                    final int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
                    if (!level.getFluidState(CURSOR.set(cx, top - 1, cz)).is(FluidTags.WATER)) {
                        continue;
                    }
                    final Tile tile = tile(level, tx, tz, top - 1);
                    if (tile != null) {
                        tile.raining = level.isRainingAt(CURSOR.set(cx, top, cz));
                    }
                }
            }
        }
        final float intensity = level.getRainLevel(1.0f) * (level.isThundering() ? 1.6f : 1.0f);
        for (final Tile tile : TILES.values()) {
            if (!tile.raining || !tile.anyWet) {
                continue;
            }
            final double dx = (tile.tx + 0.5) * TILE_BLOCKS - camera.x;
            final double dz = (tile.tz + 0.5) * TILE_BLOCKS - camera.z;
            if (dx * dx + dz * dz > (radius + 8) * (radius + 8)) {
                continue;
            }
            final int drops = (int) (6 * intensity) + (RANDOM.nextFloat() < intensity ? 1 : 0);
            for (int n = 0; n < drops; n++) {
                final int index = RANDOM.nextInt(N * N);
                if (tile.wet(index)) {
                    tile.chop[index] -= 0.012f * strength();
                    tile.quiet = 0;
                }
            }
        }
    }

    /**
     * One step of the wave equation for the velocities: ∂v/∂t = c²∇²h, with damping and a pull back to level. Shores
     * reflect.
     */
    private static void accelerate(final Tile tile, final float[] height, final float[] speed, final float waveSpeed, final float damping,
                                   final float restoring, final boolean swell) {
        final float k = waveSpeed * waveSpeed / (CELL * CELL) * DT;
        final float keep = 1.0f - damping * DT;
        final float restore = restoring * DT;
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N; i++) {
                final int index = j * N + i;
                if (!tile.wet(index)) {
                    continue;
                }
                final float c = height[index];
                final float laplacian = sample(tile, i - 1, j, c, swell) + sample(tile, i + 1, j, c, swell)
                        + sample(tile, i, j - 1, c, swell) + sample(tile, i, j + 1, c, swell) - 4.0f * c;
                speed[index] = (speed[index] + k * laplacian - restore * c) * keep;
            }
        }
    }

    /**
     * The height of a cell in one band, looking into the neighbouring tiles past the edges. Dry cells and missing tiles
     * mirror the cell asked from, so waves bounce off shores.
     */
    private static float sample(final Tile tile, final int i, final int j, final float fallback, final boolean swell) {
        Tile source = tile;
        int si = i;
        int sj = j;
        if (i < 0) {
            source = tile.west;
            si = N - 1;
        } else if (i >= N) {
            source = tile.east;
            si = 0;
        } else if (j < 0) {
            source = tile.north;
            sj = N - 1;
        } else if (j >= N) {
            source = tile.south;
            sj = 0;
        }
        if (source == null) {
            return fallback;
        }
        final int index = sj * N + si;
        return source.wet(index) ? (swell ? source.swell[index] : source.chop[index]) : fallback;
    }

    /**
     * Steep, high crests whip up foam; foam spreads a little and slowly breaks up.
     */
    private static void whipFoam(final Tile tile) {
        final float[] foam = tile.foam;
        final float[] next = tile.scratch;
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N; i++) {
                final int index = j * N + i;
                if (!tile.wet(index)) {
                    next[index] = 0.0f;
                    continue;
                }
                final float h = tile.swell[index] + tile.chop[index];
                final float right = i + 1 < N && tile.wet(index + 1) ? tile.swell[index + 1] + tile.chop[index + 1] : h;
                final float down = j + 1 < N && tile.wet(index + N) ? tile.swell[index + N] + tile.chop[index + N] : h;
                final float steepness = Mth.sqrt((right - h) * (right - h) + (down - h) * (down - h)) / CELL;
                final float whipped = Math.max(0.0f, steepness - 0.7f) * 0.08f + Math.max(0.0f, h - 0.25f) * 0.15f;

                float around = 0.0f;
                int count = 0;
                if (i > 0 && tile.wet(index - 1)) { around += foam[index - 1]; count++; }
                if (i + 1 < N && tile.wet(index + 1)) { around += foam[index + 1]; count++; }
                if (j > 0 && tile.wet(index - N)) { around += foam[index - N]; count++; }
                if (j + 1 < N && tile.wet(index + N)) { around += foam[index + N]; count++; }
                final float spread = count > 0 ? FOAM_SPREAD * (around / count - foam[index]) : 0.0f;
                next[index] = Mth.clamp((foam[index] + spread) * FOAM_KEEP + whipped, 0.0f, 1.5f);
            }
        }
        System.arraycopy(next, 0, foam, 0, N * N);
    }

    /**
     * Lets waves reaching the edge of a tile carry on into the water beyond it, and drops tiles that have gone calm or
     * are out of range.
     */
    private static void spreadAndForget(final ClientLevel level) {
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final double range = CrashClientConfig.WAVE_RANGE.getAsInt() + TILE_BLOCKS;

        final Tile[] snapshot = TILES.values().toArray(new Tile[0]);
        for (final Tile tile : snapshot) {
            float peak = 0.0f;
            for (int index = 0; index < N * N; index++) {
                final float motion = Math.abs(tile.swell[index]) + Math.abs(tile.chop[index])
                        + (Math.abs(tile.swellSpeed[index]) + Math.abs(tile.chopSpeed[index])) * 0.2f;
                peak = Math.max(peak, Math.max(motion, tile.foam[index] * 0.05f));
            }
            tile.quiet = peak < QUIET && !tile.raining ? tile.quiet + 1 : 0;

            if (peak >= SPREAD) {
                for (int edge = 0; edge < 4; edge++) {
                    final Tile neighbour = edge == 0 ? tile.west : edge == 1 ? tile.east : edge == 2 ? tile.north : tile.south;
                    if (neighbour != null) {
                        continue;
                    }
                    float strongest = 0.0f;
                    float surface = Float.NaN;
                    for (int n = 0; n < N; n++) {
                        final int index = edge == 0 ? n * N : edge == 1 ? n * N + N - 1 : edge == 2 ? n : (N - 1) * N + n;
                        final float motion = Math.abs(tile.swell[index]) + Math.abs(tile.chop[index])
                                + (Math.abs(tile.swellSpeed[index]) + Math.abs(tile.chopSpeed[index])) * 0.2f;
                        if (tile.wet(index) && motion > strongest) {
                            strongest = motion;
                            surface = tile.base[index];
                        }
                    }
                    if (strongest < SPREAD || Float.isNaN(surface)) {
                        continue;
                    }
                    final int tx = tile.tx + (edge == 0 ? -1 : edge == 1 ? 1 : 0);
                    final int tz = tile.tz + (edge == 2 ? -1 : edge == 3 ? 1 : 0);
                    final double cx = (tx + 0.5) * TILE_BLOCKS - camera.x;
                    final double cz = (tz + 0.5) * TILE_BLOCKS - camera.z;
                    if (cx * cx + cz * cz < range * range) {
                        tile(level, tx, tz, Mth.floor(surface));
                    }
                }
            }
        }

        for (final ObjectIterator<Tile> iterator = TILES.values().iterator(); iterator.hasNext(); ) {
            final Tile tile = iterator.next();
            final double cx = (tile.tx + 0.5) * TILE_BLOCKS - camera.x;
            final double cz = (tile.tz + 0.5) * TILE_BLOCKS - camera.z;
            if (tile.quiet > QUIET_TICKS || !tile.anyWet && tile.quiet > 5 || cx * cx + cz * cz > range * range * 1.2) {
                unlink(tile);
                iterator.remove();
            }
        }
    }

    // Per-corner scratch for drawing one tile
    private static final int CORNERS = (N + 1) * (N + 1);
    private static final float[] HEIGHT = new float[CORNERS];
    private static final float[] BASE = new float[CORNERS];
    private static final float[] FOAM = new float[CORNERS];
    private static final float[] Y = new float[CORNERS];
    private static final float[] RED = new float[CORNERS];
    private static final float[] GREEN = new float[CORNERS];
    private static final float[] BLUE = new float[CORNERS];
    private static final float[] ALPHA = new float[CORNERS];
    private static final float[] NX = new float[CORNERS];
    private static final float[] NY = new float[CORNERS];
    private static final float[] NZ = new float[CORNERS];

    @SubscribeEvent
    static void onRenderLevel(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || TILES.isEmpty() || !enabled()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || level != trackedLevel) {
            return;
        }

        final Vec3 camera = event.getCamera().getPosition();
        final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final PoseStack poseStack = event.getPoseStack();
        final Matrix4f pose = poseStack != null ? poseStack.last().pose() : new Matrix4f();
        final TextureAtlasSprite sprite = minecraft.getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final RenderType type = NeoForgeRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET.get();
        final VertexConsumer consumer = buffers.getBuffer(type);
        final double range = CrashClientConfig.WAVE_RANGE.getAsInt();

        // The sky the water reflects, and the sun that glints off it
        final Vec3 sky = level.getSkyColor(camera, partial);
        final float angle = level.getTimeOfDay(partial) * Mth.TWO_PI;
        final float sunX = -Mth.sin(angle);
        final float sunY = Mth.cos(angle);
        final float daylight = Mth.clamp(sunY * 4.0f + 0.2f, 0.0f, 1.0f) * (1.0f - level.getRainLevel(partial) * 0.85f);
        final float time = (ticks + partial) * 0.05f;

        for (final Tile tile : TILES.values()) {
            if (!tile.anyWet) {
                continue;
            }
            final double tileX = tile.tx * TILE_BLOCKS;
            final double tileZ = tile.tz * TILE_BLOCKS;
            final double dx = tileX + TILE_BLOCKS * 0.5 - camera.x;
            final double dz = tileZ + TILE_BLOCKS * 0.5 - camera.z;
            if (dx * dx + dz * dz > range * range) {
                continue;
            }
            float baseY = Float.NaN;
            for (final float b : tile.base) {
                if (!Float.isNaN(b)) {
                    baseY = b;
                    break;
                }
            }
            if (!event.getFrustum().isVisible(new AABB(tileX, baseY - 2.0, tileZ, tileX + TILE_BLOCKS, baseY + 2.0, tileZ + TILE_BLOCKS))) {
                continue;
            }
            if (!shadeTile(tile, partial, camera, (float) sky.x, (float) sky.y, (float) sky.z, sunX, sunY, daylight, time)) {
                continue;
            }

            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    if (!tile.wet(j * N + i)) {
                        continue;
                    }
                    final int c00 = j * (N + 1) + i;
                    final int c10 = c00 + 1;
                    final int c01 = c00 + N + 1;
                    final int c11 = c01 + 1;
                    if (ALPHA[c00] + ALPHA[c10] + ALPHA[c01] + ALPHA[c11] < 0.01f) {
                        continue;
                    }
                    final int blockX = tile.tx * TILE_BLOCKS + (i >> 1);
                    final int blockZ = tile.tz * TILE_BLOCKS + (j >> 1);
                    CURSOR.set(blockX, Mth.floor(tile.base[j * N + i]) + 1, blockZ);
                    final int light = LevelRenderer.getLightColor(level, CURSOR);
                    final float u0 = (i & 1) * 0.5f;
                    final float v0 = (j & 1) * 0.5f;
                    final float x0 = (float) (tileX + i * CELL - camera.x);
                    final float z0 = (float) (tileZ + j * CELL - camera.z);
                    final float cy = (float) camera.y;

                    // Up-facing quad, wound the way Minecraft draws block tops
                    vertex(consumer, pose, sprite, c00, x0, z0, cy, u0, v0, light);
                    vertex(consumer, pose, sprite, c01, x0, z0 + CELL, cy, u0, v0 + 0.5f, light);
                    vertex(consumer, pose, sprite, c11, x0 + CELL, z0 + CELL, cy, u0 + 0.5f, v0 + 0.5f, light);
                    vertex(consumer, pose, sprite, c10, x0 + CELL, z0, cy, u0 + 0.5f, v0, light);
                }
            }
        }
        buffers.endBatch(type);
    }

    /**
     * Works out the height, normal, colour and opacity of every corner of a tile for this frame.
     *
     * @return if any of it is visible
     */
    private static boolean shadeTile(final Tile tile, final float partial, final Vec3 camera, final float skyR, final float skyG, final float skyB,
                                     final float sunX, final float sunY, final float daylight, final float time) {
        // Heights and foam at the corners, shared with the neighbouring cells, between the last two ticks
        for (int cj = 0; cj <= N; cj++) {
            for (int ci = 0; ci <= N; ci++) {
                float height = 0.0f;
                float base = 0.0f;
                float foam = 0.0f;
                int count = 0;
                for (int k = 0; k < 4; k++) {
                    final int i = ci - 1 + (k & 1);
                    final int j = cj - 1 + (k >> 1);
                    final Tile source = owner(tile, i, j);
                    if (source == null) {
                        continue;
                    }
                    final int index = Math.floorMod(j, N) * N + Math.floorMod(i, N);
                    if (!source.wet(index)) {
                        continue;
                    }
                    height += Mth.lerp(partial, source.swellLast[index] + source.chopLast[index], source.swell[index] + source.chop[index]);
                    foam += Mth.lerp(partial, source.foamLast[index], source.foam[index]);
                    base += source.base[index];
                    count++;
                }
                final int corner = cj * (N + 1) + ci;
                HEIGHT[corner] = count > 0 ? height / count : 0.0f;
                FOAM[corner] = count > 0 ? foam / count : 0.0f;
                BASE[corner] = count > 0 ? base / count : Float.NaN;
            }
        }

        final float tintR = ((tile.color >> 16) & 0xFF) / 255.0f;
        final float tintG = ((tile.color >> 8) & 0xFF) / 255.0f;
        final float tintB = (tile.color & 0xFF) / 255.0f;
        boolean visible = false;
        for (int cj = 0; cj <= N; cj++) {
            for (int ci = 0; ci <= N; ci++) {
                final int corner = cj * (N + 1) + ci;
                final float h = HEIGHT[corner];
                final float base = BASE[corner];
                if (Float.isNaN(base)) {
                    ALPHA[corner] = 0.0f;
                    Y[corner] = 0.0f;
                    NX[corner] = 0.0f;
                    NY[corner] = 1.0f;
                    NZ[corner] = 0.0f;
                    continue;
                }

                final float left = ci > 0 ? HEIGHT[corner - 1] : h;
                final float right = ci < N ? HEIGHT[corner + 1] : h;
                final float up = cj > 0 ? HEIGHT[corner - (N + 1)] : h;
                final float down = cj < N ? HEIGHT[corner + (N + 1)] : h;
                final float gx = (right - left) / (2.0f * CELL);
                final float gz = (down - up) / (2.0f * CELL);
                final float steepness = Mth.sqrt(gx * gx + gz * gz);

                // How much this corner is part of the moving water: it fades back into the vanilla surface around it
                final float activity = Math.abs(h) * 6.0f + steepness * 3.0f + Math.max(0.0f, FOAM[corner] - 0.1f) * 1.5f;
                final float cover = smoothstep(0.02f, 0.25f, activity);
                if (cover <= 0.0f) {
                    ALPHA[corner] = 0.0f;
                    Y[corner] = base + 0.012f;
                    NX[corner] = 0.0f;
                    NY[corner] = 1.0f;
                    NZ[corner] = 0.0f;
                    continue;
                }
                visible = true;

                // Lifted a little where it moves, so troughs dip below the calm level instead of vanishing under it
                final float lift = LIFT * cover;
                Y[corner] = base + 0.012f + lift + Math.max(h, -lift + 0.004f);

                final float length = Mth.sqrt(gx * gx + 1.0f + gz * gz);
                final float nx = -gx / length;
                final float ny = 1.0f / length;
                final float nz = -gz / length;
                NX[corner] = nx;
                NY[corner] = ny;
                NZ[corner] = nz;

                // Looking at the water: more sky is reflected the flatter the angle
                final float worldX = (float) (tile.tx * TILE_BLOCKS + ci * CELL - camera.x);
                final float worldZ = (float) (tile.tz * TILE_BLOCKS + cj * CELL - camera.z);
                final float worldY = (float) (Y[corner] - camera.y);
                final float distance = Mth.sqrt(worldX * worldX + worldY * worldY + worldZ * worldZ) + 1.0e-4f;
                final float vx = -worldX / distance;
                final float vy = -worldY / distance;
                final float vz = -worldZ / distance;
                final float facing = Math.max(0.0f, nx * vx + ny * vy + nz * vz);
                final float grazing = 1.0f - facing;
                final float fresnel = 0.02f + 0.98f * grazing * grazing * grazing * grazing * grazing;

                // The water itself: darker in the troughs, lighter where a thin crest lets light through
                final float crest = Mth.clamp(h * 3.0f, 0.0f, 1.0f);
                final float body = 0.55f + 0.45f * crest + Math.min(0.0f, h) * 1.5f;
                float red = tintR * body * (1.0f - fresnel) + skyR * fresnel;
                float green = tintG * body * (1.0f - fresnel) + skyG * fresnel;
                float blue = tintB * body * (1.0f - fresnel) + skyB * fresnel;

                // The sun glinting off wave faces turned towards it
                final float twice = 2.0f * (nx * vx + ny * vy + nz * vz);
                final float rx = twice * nx - vx;
                final float ry = twice * ny - vy;
                final float sunDot = Math.max(0.0f, rx * sunX + ry * sunY);
                final float glint = daylight * (pow(sunDot, 180) * 2.5f + pow(sunDot, 16) * 0.12f);
                red += glint;
                green += glint;
                blue += glint * 0.95f;

                // Foam, broken up into patches and bubbles
                final float noise = hash(Mth.floor(tile.tx * TILE_BLOCKS * 3 + ci * 1.5f), Mth.floor(tile.tz * TILE_BLOCKS * 3 + cj * 1.5f), (int) (time * 0.5f));
                // Lacy: thin foam shows only as scattered patches, thick foam as white water
                final float foam = Mth.clamp((FOAM[corner] - 0.12f - noise * 0.45f) * 2.2f + Math.max(0.0f, steepness - 0.9f) * 0.5f, 0.0f, 1.0f);
                red = Mth.lerp(foam, red, 0.92f);
                green = Mth.lerp(foam, green, 0.95f);
                blue = Mth.lerp(foam, blue, 0.97f);

                // The water texture is drawn over this and darkens it a little
                RED[corner] = Mth.clamp(red * 1.3f, 0.0f, 1.0f);
                GREEN[corner] = Mth.clamp(green * 1.3f, 0.0f, 1.0f);
                BLUE[corner] = Mth.clamp(blue * 1.3f, 0.0f, 1.0f);
                ALPHA[corner] = Mth.clamp(cover * (0.82f + 0.15f * fresnel) + foam * 0.12f, 0.0f, 0.95f);
            }
        }
        return visible;
    }

    private static void vertex(final VertexConsumer consumer, final Matrix4f pose, final TextureAtlasSprite sprite, final int corner,
                               final float x, final float z, final float cameraY, final float u, final float v, final int light) {
        consumer.addVertex(pose, x, Y[corner] - cameraY, z)
                .setColor(RED[corner], GREEN[corner], BLUE[corner], ALPHA[corner])
                .setUv(sprite.getU(u), sprite.getV(v))
                .setLight(light)
                .setNormal(NX[corner], NY[corner], NZ[corner]);
    }

    @Nullable
    private static Tile owner(final Tile tile, final int i, final int j) {
        if (i < 0) {
            return j < 0 || j >= N ? null : tile.west;
        }
        if (i >= N) {
            return j < 0 || j >= N ? null : tile.east;
        }
        if (j < 0) {
            return tile.north;
        }
        if (j >= N) {
            return tile.south;
        }
        return tile;
    }

    private static float smoothstep(final float edge0, final float edge1, final float x) {
        final float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float pow(final float base, final int exponent) {
        float result = 1.0f;
        float factor = base;
        int e = exponent;
        while (e > 0) {
            if ((e & 1) != 0) {
                result *= factor;
            }
            factor *= factor;
            e >>= 1;
        }
        return result;
    }

    /**
     * Cheap repeatable noise in [0, 1) for a lattice point.
     */
    private static float hash(final int x, final int z, final int t) {
        int h = x * 374761393 + z * 668265263 + t * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /**
     * @return the water surface height near a position, or NaN if there is no open water there
     */
    static float surface(final ClientLevel level, final double x, final double y, final double z) {
        return surfaceAt(level, Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }
}
