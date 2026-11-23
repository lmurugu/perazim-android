package org.perazimchurch.app.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.os.Process;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.remote.ApiClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/** Prepares synthetic queued work; CI kills this app process before restoring network. */
@RunWith(AndroidJUnit4.class)
public class BackgroundSyncProcessRecoveryTest {
    private static final String USER_ID = "synthetic_workmanager_user";
    private static final String PROBE_ID = "wm_process_restart_probe";

    @Test
    public void queuedSyntheticMutationIsPersistedForBackgroundDelivery() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApiClient.setBaseUrl(context, "http://127.0.0.1:18080");

        SessionManager session = SessionManager.getInstance(context);
        session.login(USER_ID, "Synthetic WorkManager Test", null, "campus_central", "MEMBER");
        session.setAuthToken("synthetic-workmanager-test-token");

        String mutationId = SyncQueueManager.getInstance(context).enqueue(
                "CREATE", "PRAYER", PROBE_ID,
                "{\"title\":\"Synthetic background sync probe\",\"content\":\"Synthetic test row\"}",
                PROBE_ID);
        assertNotNull(mutationId);

        List<WorkInfo> work = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(BackgroundSyncScheduler.UNIQUE_WORK_NAME)
                .get(10, TimeUnit.SECONDS);
        assertFalse("WorkManager must retain the queued one-time sync", work.isEmpty());

        // Only the CI recovery script sets this argument. It kills this process after WorkManager has
        // persisted the work, which is a real SIGKILL, not a force-stop. The script expects the crash.
        Bundle args = InstrumentationRegistry.getArguments();
        if ("true".equals(args.getString("simulateProcessDeath"))) {
            awaitPersistedState(context);
            // Diagnostic: record what JobScheduler holds for this app before the death, so logcat shows it.
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            List<JobInfo> pending = jobScheduler.getAllPendingJobs();
            Log.i("BackgroundSyncRecovery", "pending JobScheduler jobs before self-kill: " + pending.size());
            for (JobInfo job : pending) {
                Log.i("BackgroundSyncRecovery", "job id=" + job.getId() + " service=" + job.getService());
            }
            Process.killProcess(Process.myPid());
        }
    }

    private static void awaitPersistedState(Context context) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
        while (System.currentTimeMillis() < deadline) {
            List<WorkInfo> infos = WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(BackgroundSyncScheduler.UNIQUE_WORK_NAME).get(5, TimeUnit.SECONDS);
            for (WorkInfo info : infos) {
                if (info.getState() == WorkInfo.State.ENQUEUED || info.getState() == WorkInfo.State.BLOCKED) {
                    Thread.sleep(1000L);
                    return;
                }
            }
            Thread.sleep(250L);
        }
        throw new AssertionError("WorkManager did not report the queued sync as ENQUEUED or BLOCKED before process death");
    }
}
