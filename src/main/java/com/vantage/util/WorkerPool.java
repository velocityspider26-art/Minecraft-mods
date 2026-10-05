package com.vantage.util;

import com.vantage.Vantage;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed pool of low-priority daemon threads that always run the most urgent job first
 * (lowest {@code priority}, ties in submission order).
 */
public final class WorkerPool implements AutoCloseable {
    private record Job(double priority, long seq, Runnable task) implements Comparable<Job> {
        @Override
        public int compareTo(Job o) {
            int c = Double.compare(this.priority, o.priority);
            return c != 0 ? c : Long.compare(this.seq, o.seq);
        }
    }

    private final PriorityBlockingQueue<Job> queue = new PriorityBlockingQueue<>();
    private final AtomicLong seq = new AtomicLong();
    private final Thread[] threads;
    private volatile boolean running = true;

    public WorkerPool(String name, int threads) {
        this.threads = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(this::loop, name + " #" + (i + 1));
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY + 1);
            this.threads[i] = t;
            t.start();
        }
    }

    public void submit(double priority, Runnable task) {
        if (this.running) {
            this.queue.add(new Job(priority, this.seq.getAndIncrement(), task));
        }
    }

    public int queued() {
        return this.queue.size();
    }

    public int threadCount() {
        return this.threads.length;
    }

    private void loop() {
        while (this.running) {
            Job job;
            try {
                job = this.queue.poll(250, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            }
            if (job == null) {
                continue;
            }
            try {
                job.task.run();
            } catch (Throwable t) {
                Vantage.LOGGER.error("Vantage worker task failed", t);
            }
        }
    }

    /** Stops accepting work, drops what is queued and waits for running jobs to finish. */
    @Override
    public void close() {
        this.running = false;
        this.queue.clear();
        for (Thread t : this.threads) {
            try {
                t.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
