package org.perazimchurch.app.sync;

import android.content.Context;

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
        ApiClient.setBaseUrl(context, "http://10.0.2.2:18080");

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
    }
}
