package org.perazimchurch.app.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.concurrent.TimeUnit;

/** Schedules durable, serialized background work for the offline mutation queue. */
public final class BackgroundSyncScheduler {
    public static final String UNIQUE_WORK_NAME = "perazim-background-sync";

    private BackgroundSyncScheduler() {}

    public static void enqueue(@NonNull Context context) {
        enqueue(context, 0L);
    }

    static void enqueue(@NonNull Context context, long initialDelayMillis) {
        OneTimeWorkRequest request = createRequest(initialDelayMillis);
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }

    static OneTimeWorkRequest createRequest(long initialDelayMillis) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        return new OneTimeWorkRequest.Builder(BackgroundSyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .setInitialDelay(Math.max(0L, initialDelayMillis), TimeUnit.MILLISECONDS)
                .addTag(UNIQUE_WORK_NAME)
                .build();
    }
}
