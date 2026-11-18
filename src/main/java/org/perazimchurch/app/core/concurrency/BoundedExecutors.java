package org.perazimchurch.app.core.concurrency;

import androidx.annotation.NonNull;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Bounded background executors (review fix F-04). A burst of work queues up to a fixed
 * limit and then is refused, so the app cannot create an unlimited number of threads.
 */
public final class BoundedExecutors {
    private static final int CORE_THREADS = 4;
    private static final int MAX_THREADS = 8;
    private static final int QUEUE_CAPACITY = 64;

    private static final ExecutorService IO = new ThreadPoolExecutor(
            CORE_THREADS, MAX_THREADS, 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadPoolExecutor.AbortPolicy());

    private BoundedExecutors() {}

    /** Network and file work. Throws RejectedExecutionException when the queue is full. */
    @NonNull
    public static ExecutorService io() {
        return IO;
    }
}
