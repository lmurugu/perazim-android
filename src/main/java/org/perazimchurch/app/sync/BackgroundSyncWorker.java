package org.perazimchurch.app.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.perazimchurch.app.data.local.session.SessionManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Runs the existing queue dispatcher in WorkManager's restartable worker process. */
public final class BackgroundSyncWorker extends Worker {
    private static final long COMPLETION_TIMEOUT_MINUTES = 9L;

    public BackgroundSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        CountDownLatch completion = new CountDownLatch(1);
        AtomicBoolean syncSucceeded = new AtomicBoolean(false);
        SyncDispatcher.dispatchSync(getApplicationContext(), (success, synced, downloaded) -> {
            syncSucceeded.set(success);
            completion.countDown();
        });

        try {
            if (!completion.await(COMPLETION_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                return Result.retry();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        if (!syncSucceeded.get()) return Result.retry();

        scheduleNextDueQueueItem();
        return Result.success();
    }

    private void scheduleNextDueQueueItem() {
        SessionManager session = SessionManager.getInstance(getApplicationContext());
        if (session.isGuest()) return;
        long nextAttemptAt = SyncQueueManager.getInstance(getApplicationContext())
                .getNextPendingAttemptAt();
        if (nextAttemptAt <= 0L) return;
        BackgroundSyncScheduler.enqueue(getApplicationContext(), nextAttemptAt - System.currentTimeMillis());
    }
}
