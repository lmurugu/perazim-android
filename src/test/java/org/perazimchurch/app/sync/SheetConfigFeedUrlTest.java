package org.perazimchurch.app.sync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The default tab URLs point at the shared church content feed. */
public class SheetConfigFeedUrlTest {

    @Test public void buildsCsvUrlForEachTab() {
        assertEquals("https://example.org/data/csv/hymns.csv",
                SheetConfig.defaultUrlFor("https://example.org/data", "hymns"));
        assertEquals("https://example.org/data/csv/config.csv",
                SheetConfig.defaultUrlFor("https://example.org/data/", "config"));
    }

    @Test public void blankBaseMeansNoFeed() {
        assertEquals("", SheetConfig.defaultUrlFor("", "hymns"));
        assertEquals("", SheetConfig.defaultUrlFor("   ", "hymns"));
        assertEquals("", SheetConfig.defaultUrlFor(null, "hymns"));
    }

    @Test public void feedNamesMatchTheSharedFeedTabs() {
        String[] expected = {
            "announcements", "events", "hymns", "riddles", "jokes", "sermons", "verses", "config"
        };
        SheetConfig.Tab[] tabs = SheetConfig.Tab.values();
        assertEquals(expected.length, tabs.length);
        for (int i = 0; i < tabs.length; i++) {
            assertEquals(expected[i], tabs[i].getFeedName());
        }
    }
}
