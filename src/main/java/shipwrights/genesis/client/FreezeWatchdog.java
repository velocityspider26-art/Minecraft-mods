package shipwrights.genesis.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import shipwrights.genesis.GenesisMod;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Catches a client freeze in the act and writes down what every thread was
 * doing.
 *
 * <p>"There is a freeze" has cost several sessions of guessing, because a
 * freeze leaves almost no trace: the log simply has a gap, and a gap is also
 * what a quiet minute looks like. Three separate hypotheses were argued from
 * correlation — a sampler running nearby, a pool being starved, a chunk load
 * blocking — and each one cost a test cycle to disprove.</p>
 *
 * <p>So this stops guessing. A daemon thread watches a heartbeat that the
 * client tick updates. If the heartbeat goes stale, the client is not ticking,
 * which is what a freeze IS — and every thread's stack goes into the log,
 * naming whatever is actually holding it up. One reproduction then answers the
 * question outright.</p>
 *
 * <p>Costs nothing when nothing is wrong: one long write per tick, and a
 * thread that sleeps.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public final class FreezeWatchdog {
    /**
     * A gap longer than this is not lag, it is a freeze worth explaining.
     *
     * <p>Was 1500 ms, and that was too blunt. It caught the two-second grid
     * rebuild and the twenty-second world load, both of which are now dealt
     * with — and then reported nothing at all for a crossing the player could
     * still plainly feel. A stall of half a second is invisible to a threshold
     * built for the obvious ones and is still a very long time to be looking at
     * a frozen frame.</p>
     */
    private static final long STALL_THRESHOLD_MS = 400L;
    /** How often the watcher looks. Well under the threshold so it cannot miss one. */
    private static final long POLL_INTERVAL_MS = 100L;
    /**
     * Quiet period after a report. A single freeze would otherwise produce a
     * dump every poll for as long as it lasts, burying the first and most
     * useful one.
     */
    private static final long REPORT_COOLDOWN_MS = 5_000L;

    private static volatile long heartbeat = System.currentTimeMillis();
    private static volatile boolean started;
    private static long lastReportAt = Long.MIN_VALUE / 2;

    private FreezeWatchdog() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        heartbeat = System.currentTimeMillis();
        if (!started) {
            started = true;
            start();
        }
    }

    private static void start() {
        Thread watcher = new Thread(FreezeWatchdog::watch, "genesis-freeze-watchdog");
        watcher.setDaemon(true);
        // Above the threads it is watching, or it would be descheduled by the
        // very contention it exists to report on.
        watcher.setPriority(Thread.MAX_PRIORITY);
        watcher.start();
        GenesisMod.LOGGER.info("[WATCHDOG] watching for client stalls over {}ms", STALL_THRESHOLD_MS);
    }

    private static void watch() {
        while (true) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }

            long now = System.currentTimeMillis();
            long stalledFor = now - heartbeat;
            if (stalledFor < STALL_THRESHOLD_MS || now - lastReportAt < REPORT_COOLDOWN_MS) {
                continue;
            }
            lastReportAt = now;
            report(stalledFor);
        }
    }

    private static void report(long stalledFor) {
        StringBuilder out = new StringBuilder(4096);
        out.append("[WATCHDOG] client has not ticked for ").append(stalledFor)
                .append("ms — dumping every thread\n");
        try {
            ThreadMXBean threads = ManagementFactory.getThreadMXBean();
            // Include lock ownership: "who is blocked on what" is usually the
            // whole answer, and it is the part a plain stack trace omits.
            for (ThreadInfo info : threads.dumpAllThreads(true, true)) {
                if (info == null) {
                    continue;
                }
                out.append('\n').append(info.getThreadName())
                        .append(" [").append(info.getThreadState()).append(']');
                if (info.getLockName() != null) {
                    out.append(" waiting on ").append(info.getLockName());
                }
                if (info.getLockOwnerName() != null) {
                    out.append(" held by ").append(info.getLockOwnerName());
                }
                out.append('\n');
                StackTraceElement[] stack = info.getStackTrace();
                for (int i = 0; i < Math.min(stack.length, 24); i++) {
                    out.append("    at ").append(stack[i]).append('\n');
                }
            }
        } catch (Throwable error) {
            out.append("  (could not dump threads: ").append(error).append(')');
        }
        GenesisMod.LOGGER.warn(out.toString());
    }
}
