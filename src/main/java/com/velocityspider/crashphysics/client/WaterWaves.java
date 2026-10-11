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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 * Waves on water surfaces: splashes send out rings, ships push up bow waves and drag V-shaped wakes behind them, and
 * anything swimming or floating leaves ripples.
 * <p>
 * The surface is a height field on a half-block grid, kept only in tiles of 8 × 8 blocks where something disturbed the
 * water, and stepped with the wave equation (waves travel at about 2 m/s, so anything faster drags a V-shaped wake;
 * they reflect off shores and die out over a few seconds). It is drawn as a lit, water-textured skin over the vanilla water: crests catch the light and foam, troughs
 * darken.
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

    private static final float SPEED = 2.0f;
    private static final float DAMPING = 0.45f;
    /**
     * Pulls the surface back to level, as gravity does: water pushed down or piled up doesn't stay that way [1/s²]
     */
    private static final float RESTORE = 3.0f;
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

    private static final Long2ObjectOpenHashMap<Tile> TILES = new Long2ObjectOpenHashMap<>();
    @Nullable
    private static ClientLevel trackedLevel;
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();

    // Light falling on the waves, from high up and to one side
    private static final float LIGHT_X = 0.45f;
    private static final float LIGHT_Y = 0.80f;
    private static final float LIGHT_Z = 0.40f;

    static final class Tile {
        final int tx;
        final int tz;
        final float[] h = new float[N * N];
        final float[] v = new float[N * N];
        final float[] last = new float[N * N];
        /**
         * Height of the water surface in each cell, or NaN where there is no open water
         */
        final float[] base = new float[N * N];
        final int color;
        boolean anyWet;
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
     * Pushes the water down (or up, if negative) in a round patch: a splash.
     *
     * @param depth how far the middle is pushed down [m]
     */
    public static void displace(final double x, final double y, final double z, final float radius, final float depth) {
        final float scaled = depth * (float) CrashClientConfig.WAVE_STRENGTH.getAsDouble();
        apply(x, y, z, radius, scaled, false);
    }

    /**
     * Gives the water in a round patch a downward push: something moving through it, applied once per tick.
     *
     * @param speed downward speed given to the middle of the patch [m/s]
     */
    public static void push(final double x, final double y, final double z, final float radius, final float speed) {
        final float scaled = speed * (float) CrashClientConfig.WAVE_STRENGTH.getAsDouble();
        apply(x, y, z, radius, scaled, true);
    }

    private static void apply(final double x, final double y, final double z, final float radius, final float amount, final boolean velocity) {
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
                        // Pushed down in the middle, the water it moved piled up in a ring around it: nothing added or lost
                        final float weight = (float) (Math.exp(-d2 * inverse) - 0.25 * Math.exp(-d2 * inverse * 0.25));
                        if (velocity) {
                            tile.v[index] -= amount * weight;
                        } else {
                            tile.h[index] = Mth.clamp(tile.h[index] - amount * weight, -MAX_HEIGHT, MAX_HEIGHT);
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

        CrashFxClient.rippleEntities(level);
        if (TILES.isEmpty()) {
            return;
        }

        for (final Tile tile : TILES.values()) {
            System.arraycopy(tile.h, 0, tile.last, 0, N * N);
        }
        for (int step = 0; step < SUBSTEPS; step++) {
            for (final Tile tile : TILES.values()) {
                accelerate(tile);
            }
            for (final Tile tile : TILES.values()) {
                for (int index = 0; index < N * N; index++) {
                    if (tile.wet(index)) {
                        tile.h[index] = Mth.clamp(tile.h[index] + tile.v[index] * DT, -MAX_HEIGHT, MAX_HEIGHT);
                    }
                }
            }
        }
        spreadAndForget(level);
    }

    /**
     * One step of the wave equation for the velocities: ∂v/∂t = c²∇²h, with damping. Shores reflect.
     */
    private static void accelerate(final Tile tile) {
        final float k = SPEED * SPEED / (CELL * CELL) * DT;
        final float keep = 1.0f - DAMPING * DT;
        final float restore = RESTORE * DT;
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N; i++) {
                final int index = j * N + i;
                if (!tile.wet(index)) {
                    continue;
                }
                final float c = tile.h[index];
                final float laplacian = sample(tile, i - 1, j, c) + sample(tile, i + 1, j, c) + sample(tile, i, j - 1, c) + sample(tile, i, j + 1, c) - 4.0f * c;
                tile.v[index] = (tile.v[index] + k * laplacian - restore * c) * keep;
            }
        }
    }

    /**
     * The height of a cell, looking into the neighbouring tiles past the edges. Dry cells and missing tiles mirror the
     * cell asked from, so waves bounce off shores.
     */
    private static float sample(final Tile tile, final int i, final int j, final float fallback) {
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
        return source.wet(index) ? source.h[index] : fallback;
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
                peak = Math.max(peak, Math.abs(tile.h[index]) + Math.abs(tile.v[index]) * 0.2f);
            }
            tile.quiet = peak < QUIET ? tile.quiet + 1 : 0;

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
                        if (tile.wet(index) && Math.abs(tile.h[index]) + Math.abs(tile.v[index]) * 0.2f > strongest) {
                            strongest = Math.abs(tile.h[index]) + Math.abs(tile.v[index]) * 0.2f;
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

        final float[] corners = new float[(N + 1) * (N + 1)];
        final float[] bases = new float[(N + 1) * (N + 1)];
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

            // Heights at the cell corners, shared with the neighbouring cells, between the last two ticks
            for (int cj = 0; cj <= N; cj++) {
                for (int ci = 0; ci <= N; ci++) {
                    float sum = 0.0f;
                    float baseSum = 0.0f;
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
                        sum += Mth.lerp(partial, source.last[index], source.h[index]);
                        baseSum += source.base[index];
                        count++;
                    }
                    corners[cj * (N + 1) + ci] = count > 0 ? sum / count : 0.0f;
                    bases[cj * (N + 1) + ci] = count > 0 ? baseSum / count : Float.NaN;
                }
            }

            final float r = ((tile.color >> 16) & 0xFF) / 255.0f;
            final float g = ((tile.color >> 8) & 0xFF) / 255.0f;
            final float b = (tile.color & 0xFF) / 255.0f;
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    if (!tile.wet(j * N + i)) {
                        continue;
                    }
                    final int c00 = j * (N + 1) + i;
                    final int c10 = c00 + 1;
                    final int c01 = c00 + N + 1;
                    final int c11 = c01 + 1;
                    final float h00 = corners[c00], h10 = corners[c10], h01 = corners[c01], h11 = corners[c11];
                    final float slopeX = Math.abs(h10 - h00) + Math.abs(h11 - h01);
                    final float slopeZ = Math.abs(h01 - h00) + Math.abs(h11 - h10);
                    final float activity = Math.max(Math.max(Math.abs(h00), Math.abs(h10)), Math.max(Math.abs(h01), Math.abs(h11))) + slopeX + slopeZ;
                    if (activity < 0.004f) {
                        continue;
                    }

                    final int blockX = tile.tx * TILE_BLOCKS + (i >> 1);
                    final int blockZ = tile.tz * TILE_BLOCKS + (j >> 1);
                    CURSOR.set(blockX, Mth.floor(tile.base[j * N + i]) + 1, blockZ);
                    final int light = LevelRenderer.getLightColor(level, CURSOR);
                    final float u0 = (i & 1) * 0.5f;
                    final float v0 = (j & 1) * 0.5f;
                    final double x0 = tileX + i * CELL - camera.x;
                    final double z0 = tileZ + j * CELL - camera.z;

                    // Up-facing quad, wound the way Minecraft draws block tops
                    vertex(consumer, pose, sprite, corners, bases, tile, c00, i, j, x0, z0, camera, u0, v0, light, r, g, b);
                    vertex(consumer, pose, sprite, corners, bases, tile, c01, i, j + 1, x0, z0 + CELL, camera, u0, v0 + 0.5f, light, r, g, b);
                    vertex(consumer, pose, sprite, corners, bases, tile, c11, i + 1, j + 1, x0 + CELL, z0 + CELL, camera, u0 + 0.5f, v0 + 0.5f, light, r, g, b);
                    vertex(consumer, pose, sprite, corners, bases, tile, c10, i + 1, j, x0 + CELL, z0, camera, u0 + 0.5f, v0, light, r, g, b);
                }
            }
        }
        buffers.endBatch(type);
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

    private static void vertex(final VertexConsumer consumer, final Matrix4f pose, final TextureAtlasSprite sprite,
                               final float[] corners, final float[] bases, final Tile tile, final int corner, final int ci, final int cj,
                               final double x, final double z, final Vec3 camera, final float u, final float v, final int light,
                               final float r, final float g, final float b) {
        final float h = corners[corner];
        float base = bases[corner];
        if (Float.isNaN(base)) {
            base = tile.base[Math.min(cj, N - 1) * N + Math.min(ci, N - 1)];
        }

        // Surface slope at this corner, from the corners either side
        final float left = ci > 0 ? corners[corner - 1] : h;
        final float right = ci < N ? corners[corner + 1] : h;
        final float up = cj > 0 ? corners[corner - (N + 1)] : h;
        final float down = cj < N ? corners[corner + (N + 1)] : h;
        final float gx = (right - left) / (2.0f * CELL);
        final float gz = (down - up) / (2.0f * CELL);

        // Lit by the sky: faces tilted towards the light are brighter, troughs and faces turned away are darker
        final float length = Mth.sqrt(gx * gx + 1.0f + gz * gz);
        final float facing = (-gx * LIGHT_X + LIGHT_Y - gz * LIGHT_Z) / length;
        final float shade = Mth.clamp(0.35f + 0.75f * facing + h * 1.2f, 0.3f, 1.5f);

        // Steep, high crests break into foam
        final float steepness = Mth.sqrt(gx * gx + gz * gz);
        final float foam = Mth.clamp((h - 0.12f) * 5.0f, 0.0f, 1.0f) * 0.7f + Mth.clamp((steepness - 0.5f) * 1.2f, 0.0f, 0.6f);

        final float red = Mth.clamp(Mth.lerp(foam, r * shade, 0.95f), 0.0f, 1.0f);
        final float green = Mth.clamp(Mth.lerp(foam, g * shade, 0.97f), 0.0f, 1.0f);
        final float blue = Mth.clamp(Mth.lerp(foam, b * shade, 1.0f), 0.0f, 1.0f);
        // Flat water is left to vanilla: only slopes, and crests and troughs, show
        final float alpha = Mth.clamp(steepness * 3.0f + Math.abs(h) * 2.0f + foam * 0.4f - 0.02f, 0.0f, 0.85f);

        // Troughs can't be drawn below the vanilla water surface, which hides them; their shading shows them instead
        final float y = base + Math.max(h, 0.0f) + 0.012f;
        consumer.addVertex(pose, (float) x, (float) (y - camera.y), (float) z)
                .setColor(red, green, blue, alpha)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setLight(light)
                .setNormal(-gx / length, 1.0f / length, -gz / length);
    }

    /**
     * @return the water surface height near a position, or NaN if there is no open water there
     */
    static float surface(final ClientLevel level, final double x, final double y, final double z) {
        return surfaceAt(level, Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }
}
