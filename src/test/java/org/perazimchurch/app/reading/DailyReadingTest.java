package org.perazimchurch.app.reading;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.preference.DailyReadingStore;
import org.perazimchurch.app.domain.reading.ReadingCatalog;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class DailyReadingTest {
    private static final String TODAY = "2026-11-26";
    private static final String TOMORROW = "2026-11-27";
    private static final String OWNER = "fixture-account";
    private DailyReadingStore store;

    @Before public void prepare() {
        store = new DailyReadingStore(RuntimeEnvironment.getApplication());
    }

    @Test public void catalogProvidesDailyVariationAndFullMonthPlans() throws Exception {
        try (InputStreamReader input = new InputStreamReader(RuntimeEnvironment.getApplication()
                .getAssets().open("reading/daily-readings.json"), StandardCharsets.UTF_8)) {
            ReadingCatalog catalog = new ReadingCatalog(input);
            ReadingCatalog.Reading today = catalog.dailyReading(TODAY);
            assertEquals(today.id, catalog.dailyReading(TODAY).id);
            assertNotEquals(today.reference, catalog.dailyReading(TOMORROW).reference);
            assertNotEquals("2 Samuel 5:20", today.reference);
            assertFalse(today.scripture.isEmpty());
            assertFalse(today.reflection.isEmpty());
            assertEquals("Peace and anxiety", catalog.matches("I need help with worry").get(0).title);
            assertTrue(catalog.matches("xyzzy").isEmpty());
            for (ReadingCatalog.Topic topic : catalog.topics) {
                List<String> readings = catalog.select(topic, "", 30);
                assertEquals(30, new HashSet<>(readings).size());
                for (String id : readings) assertNotNull(catalog.find(id));
            }
            assertThrows(IllegalArgumentException.class, () -> catalog.select(catalog.topics.get(0), "", 31));
        }
    }

    @Test public void planResumesAndOnlyOpensTheNextReadingOnAnotherDay() {
        assertTrue(store.start(OWNER, "Prayer", Arrays.asList("entry-a", "entry-b")));
        DailyReadingStore.State first = load(TODAY);
        assertEquals("entry-a", first.readingId);
        assertTrue(store.advance(OWNER, TODAY, first));
        DailyReadingStore reopened = new DailyReadingStore(RuntimeEnvironment.getApplication());
        DailyReadingStore.State saved = reopened.load(OWNER, TOMORROW, "unused");
        assertEquals(2, saved.step);
        assertEquals("entry-a", saved.readingId);
        assertFalse(store.advance(OWNER, TOMORROW, first));
        assertTrue(reopened.advance(OWNER, TOMORROW, saved));
        assertTrue(reopened.advance(OWNER, TOMORROW, load(TOMORROW)));
        assertEquals(4, load(TOMORROW).step);
        assertEquals(1, load(TOMORROW).completed);
        String later = "2026-12-01";
        DailyReadingStore.State next = load(later);
        assertEquals("entry-b", next.readingId);
        assertEquals(1, next.step);
        assertEquals(2, next.day);
        assertTrue(store.advance(OWNER, later, next));
        assertTrue(store.advance(OWNER, later, load(later)));
        assertTrue(store.advance(OWNER, later, load(later)));
        assertTrue(load(later).planFinished());
        assertFalse(store.advance(OWNER, later, load(later)));
    }

    @Test public void dailyProgressIsPerAccountAndSurvivesSwitchingModes() {
        DailyReadingStore.State first = load(TODAY);
        assertTrue(store.advance(OWNER, TODAY, first));
        assertFalse(store.advance(OWNER, TODAY, first));
        assertTrue(store.start(OWNER, "Faith", Arrays.asList("entry-a")));
        assertTrue(load(TODAY).isPlan());
        assertFalse(store.load("another-account", TODAY, "daily").isPlan());
        assertTrue(store.useDaily(OWNER));
        assertEquals(2, load(TODAY).step);
        assertEquals(1, load(TOMORROW).step);
    }

    private DailyReadingStore.State load(String date) {
        return store.load(OWNER, date, "daily-" + date);
    }
}
