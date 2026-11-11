package org.perazimchurch.app.sync;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BackgroundSyncSchedulerTest {

    @Test
    public void oneTimeSyncWorkUsesConnectedNetworkAndExponentialBackoff() {
        OneTimeWorkRequest request = BackgroundSyncScheduler.createRequest(5000L);
        Constraints constraints = request.getWorkSpec().constraints;
        assertEquals(NetworkType.CONNECTED, constraints.getRequiredNetworkType());
        assertEquals(BackoffPolicy.EXPONENTIAL, request.getWorkSpec().backoffPolicy);
        assertEquals(WorkRequest.MIN_BACKOFF_MILLIS, request.getWorkSpec().backoffDelayDuration);
        assertEquals(5000L, request.getWorkSpec().initialDelay);
    }

    @Test
    public void enqueuePersistsOneUniqueBackgroundSyncRequest() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        WorkManager manager = WorkManager.getInstance(context);
        manager.cancelUniqueWork(BackgroundSyncScheduler.UNIQUE_WORK_NAME).getResult().get(10, TimeUnit.SECONDS);

        BackgroundSyncScheduler.enqueue(context);

        List<WorkInfo> work = manager.getWorkInfosForUniqueWork(BackgroundSyncScheduler.UNIQUE_WORK_NAME)
                .get(10, TimeUnit.SECONDS);
        assertEquals(1, work.size());
        assertTrue(work.get(0).getState() == WorkInfo.State.ENQUEUED
                || work.get(0).getState() == WorkInfo.State.RUNNING);
        assertTrue(work.get(0).getTags().contains(BackgroundSyncScheduler.UNIQUE_WORK_NAME));
        manager.cancelUniqueWork(BackgroundSyncScheduler.UNIQUE_WORK_NAME).getResult().get(10, TimeUnit.SECONDS);
    }
}
