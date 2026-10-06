package com.vantage.debug;

import com.vantage.Vantage;
import com.vantage.gen.WorldGenSandbox;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.List;

/**
 * Development aid ({@code -Dvantage.debug.sandboxBench=true} on a server): times
 * {@link WorldGenSandbox} with nothing else running, over a spread of places. With
 * {@code -Dvantage.debug.sandboxVerify=N} it also has the server generate the first N batches'
 * chunks for real and compares the top block of every column.
 */
public final class SandboxBench {
    private SandboxBench() {
    }

    public static void start(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Thread t = new Thread(() -> run(level), "Vantage sandbox bench");
        t.setDaemon(true);
        t.start();
    }

    private static void run(ServerLevel level) {
        WorldGenSandbox sandbox = new WorldGenSandbox(level);
        int batches = Integer.getInteger("vantage.debug.sandboxBatches", 12);
        int verify = Integer.getInteger("vantage.debug.sandboxVerify", 0);
        long[] tally = new long[4];
        for (int i = 0; i < batches; i++) {
            long t0 = System.nanoTime();
            // Spread over a wide area, for a mix of terrain.
            List<WorldGenSandbox.Chunk> chunks = sandbox.generate(Math.floorMod(i * 7919, 4096) - 2048, Math.floorMod(i * 6151 + 1000, 4096) - 2048);
            Vantage.LOGGER.info("[bench] batch {}: {} ms", i, (System.nanoTime() - t0) / 1_000_000);
            if (i < verify) {
                for (WorldGenSandbox.Chunk c : chunks) {
                    compare(c, level.getChunk(c.pos().x, c.pos().z, ChunkStatus.FULL, true), tally);
                }
            }
        }
        Vantage.LOGGER.info("[bench] {}", sandbox.stats());
        long n = Math.max(1, tally[0] + tally[1] + tally[2] + tally[3]);
        if (verify > 0) {
            MISMATCHES.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(40)
                    .forEach(e -> Vantage.LOGGER.info("[bench] mismatch {} x{}", e.getKey(), e.getValue()));
            Vantage.LOGGER.info("[bench] top block vs real chunks: same {}%, same height other block {}%, height off by 1-2 {}%, more {}%",
                    100 * tally[0] / n, 100 * tally[1] / n, 100 * tally[2] / n, 100 * tally[3] / n);
        }
    }

    private static void compare(WorldGenSandbox.Chunk c, ChunkAccess real, long[] tally) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int bx = c.pos().getMinBlockX() + x;
                int bz = c.pos().getMinBlockZ() + z;
                int ya = top(c.chunk(), pos, bx, bz);
                int yb = top(real, pos, bx, bz);
                BlockState a = ya == Integer.MIN_VALUE ? null : c.chunk().getBlockState(pos.set(bx, ya, bz));
                BlockState b = yb == Integer.MIN_VALUE ? null : real.getBlockState(pos.set(bx, yb, bz));
                if (ya == yb) {
                    tally[a == b ? 0 : 1]++;
                } else {
                    tally[Math.abs(ya - yb) <= 2 ? 2 : 3]++;
                }
                if (ya != yb || a != b) {
                    String key = name(a) + " @" + (ya - yb) / 4 * 4 + " vs real " + name(b);
                    MISMATCHES.merge(key, 1, Integer::sum);
                }
            }
        }
    }

    private static final java.util.Map<String, Integer> MISMATCHES = new java.util.concurrent.ConcurrentHashMap<>();

    private static String name(BlockState s) {
        return s == null ? "none" : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    }

    private static int top(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int z) {
        for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
            if (!chunk.getBlockState(pos.set(x, y, z)).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
