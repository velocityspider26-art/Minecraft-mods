package com.vantage.client.gen;

import com.vantage.client.render.Planner;

/** Where real terrain near the player comes from: made here, or sent by the server. */
public interface Detail extends Planner.Detailer {
    /** Chunks of real terrain stored so far. */
    long chunks();

    /** Average milliseconds of work per chunk (making it here, or waiting for the server). */
    double averageMillis();

    /** Work waiting or underway. */
    int queued();

    /** One line for the debug screen. */
    String describe();

    /** Main thread, every client tick. */
    default void tick() {
    }

    void close();
}
