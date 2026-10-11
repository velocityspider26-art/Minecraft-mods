package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.CrashPhysics;

/**
 * Where crash physics spends its time, for profiling. Only measured when the {@code crashphysics.profile} system
 * property is set, so normal play pays nothing for it.
 */
public final class CrashTimings {

    public static final boolean ENABLED = Boolean.getBoolean("crashphysics.profile");

    static long callbackNanos;
    static long callbacks;
    static long preNanos;
    static long sweepNanos;
    static long stepNanos;
    static long postNanos;
    static long endTickNanos;

    static long stepStart;

    /**
     * Named parts of a tick, for finding slow ticks
     */
    public enum Section {
        SWEEP, SWEEP_SHAPE, SWEEP_FIND, SWEEP_RESOLVE, SWEEP_BREAKING, RESOLVE, RESOLVE_BREAKING, CREW, STRUCTURE, DESTRUCTION, COLLAPSE, SOIL, EFFECTS
    }

    private static final long[] TICK_SECTIONS = new long[Section.values().length];

    public static long begin() {
        return ENABLED ? System.nanoTime() : 0L;
    }

    public static void end(final Section section, final long start) {
        if (ENABLED) {
            TICK_SECTIONS[section.ordinal()] += System.nanoTime() - start;
        }
    }

    /**
     * Logs where the time went if this tick was slow, and starts the next one.
     */
    static void endTick(final long tickNanos, final long tick) {
        if (tickNanos > 5_000_000L) {
            final StringBuilder parts = new StringBuilder();
            for (final Section section : Section.values()) {
                final long nanos = TICK_SECTIONS[section.ordinal()];
                if (nanos > 100_000L) {
                    parts.append(' ').append(section.name().toLowerCase()).append('=').append(ms(nanos));
                }
            }
            CrashPhysics.LOGGER.info("[timings] slow tick {}: {} ms:{}", tick, ms(tickNanos), parts);
        }
        java.util.Arrays.fill(TICK_SECTIONS, 0L);
    }

    private CrashTimings() {
    }

    public static void reset() {
        callbackNanos = 0;
        callbacks = 0;
        preNanos = 0;
        sweepNanos = 0;
        stepNanos = 0;
        postNanos = 0;
        endTickNanos = 0;
    }

    /**
     * Logs the average cost per server tick since the last reset, all dimensions together.
     */
    public static void report(final String label, final int ticks) {
        final double n = Math.max(ticks, 1);
        CrashPhysics.LOGGER.info("[timings] {}: {} ticks, per tick: physics step {} ms (of which crash callbacks {} ms, {} calls), "
                        + "before step {} ms (sweep {} ms), after step {} ms, end of tick {} ms",
                label, ticks, ms(stepNanos / n), ms(callbackNanos / n), Math.round(callbacks / n), ms(preNanos / n), ms(sweepNanos / n),
                ms(postNanos / n), ms(endTickNanos / n));
    }

    /**
     * @return everything crash physics has spent so far, callbacks included [ns]
     */
    public static long total() {
        return callbackNanos + preNanos + postNanos + endTickNanos;
    }

    private static String ms(final double nanos) {
        return String.format("%.3f", nanos / 1.0e6);
    }
}
