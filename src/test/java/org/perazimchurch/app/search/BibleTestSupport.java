package org.perazimchurch.app.search;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.seeder.BibleTextImporter;
import org.perazimchurch.app.data.local.text.BibleVerseReader;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test fixture for the Bible text. On a device the bundled KJV is imported on a background thread and every
 * text-database read runs off the main thread. Robolectric's main thread is the test thread, so the same work
 * runs on a background executor here, and the main looper is drained afterwards so posted callbacks run.
 */
final class BibleTestSupport {

    /** One background thread, in order: a task submitted later runs after the ones before it. */
    static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bible-test-background");
        t.setDaemon(true);
        return t;
    });

    private BibleTestSupport() {
    }

    /** Runs work off the main thread and returns its result. */
    static <T> T background(Callable<T> work) throws Exception {
        return BACKGROUND.submit(work).get();
    }

    /** Waits for queued background work, then runs the main looper so posted callbacks execute. */
    static void drain() throws Exception {
        for (int i = 0; i < 2; i++) {
            BACKGROUND.submit(() -> null).get();
            ShadowLooper.idleMainLooper();
        }
    }

    /**
     * Imports the bundled translations into the text database, the way the app does on first run, and checks that
     * the KJV is marked ready so reads use it.
     */
    static void importBundledText(Context context, PerazimDatabase database) throws Exception {
        background(() -> {
            BibleTextImporter.importBundledTranslations(context, database.bibleDao());
            return null;
        });
        boolean ready = background(() -> BibleVerseReader.isCurrent(context, "KJV"));
        assertTrue("the KJV must be imported and marked ready before the checks run", ready);
    }
}
