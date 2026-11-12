package org.perazimchurch.app.ai.local;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class OfflineScriptureDeviceTest {
    @Test public void returnsExactLocalVersesAndRejectsAnIncompleteRange() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = "ai-scripture-fixture-" + UUID.randomUUID();
        PerazimDatabase database = Room.databaseBuilder(context, PerazimDatabase.class, databaseName)
                .createFromAsset("database/perazim_database.db")
                .addMigrations(PerazimDatabase.MIGRATION_5_6)
                .build();
        try {
            BibleDao bible = database.bibleDao();
            String first = bible.getVerse("KJV", 43, 3, 16).getText();
            String second = bible.getVerse("KJV", 43, 3, 17).getText();
            assertEquals("John 3:16–17 (KJV)\n\n16 " + first + "\n\n17 " + second,
                    OfflineScriptureLookup.answer(bible, "John 3:16-17").getMessageText());
            assertTrue(OfflineScriptureLookup.answer(bible, "John 3:35-39")
                    .getMessageText().contains("couldn’t find that complete passage"));
            assertNull(OfflineScriptureLookup.answer(bible, "Explain John 3:16"));
        } finally {
            database.close();
            context.deleteDatabase(databaseName);
        }
    }

    @Test public void scriptureRetrieverReturnsLicensedKjvEvidenceWithExactReference() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = "ai-scripture-retrieval-" + UUID.randomUUID();
        PerazimDatabase database = Room.databaseBuilder(context, PerazimDatabase.class, databaseName)
                .createFromAsset("database/perazim_database.db")
                .addMigrations(PerazimDatabase.MIGRATION_5_6)
                .build();
        try {
            org.json.JSONArray evidence = LocalScriptureRetriever.retrieve(database.bibleDao(),
                    "For God so loved the world");
            assertTrue("KJV retrieval should return locally stored evidence", evidence.length() > 0);
            org.json.JSONObject first = evidence.getJSONObject(0);
            assertEquals("Bible", first.getString("sourceId"));
            assertEquals("KJV", first.getString("version"));
            assertEquals("John 3:16", first.getString("location"));
            assertTrue(first.getString("text").contains("loved the world"));
            assertTrue("A named Bible reference should classify as a Scripture question",
                    LocalScriptureRetriever.isScriptureRelated("Explain John 3:16"));
            org.json.JSONArray referenced = LocalScriptureRetriever.retrieve(database.bibleDao(),
                    "Explain John 3:16");
            assertEquals("Named-reference retrieval should include the adjacent KJV context", 3, referenced.length());
            assertEquals("John 3:15", referenced.getJSONObject(0).getString("location"));
            assertEquals("John 3:16", referenced.getJSONObject(1).getString("location"));
            assertEquals("John 3:17", referenced.getJSONObject(2).getString("location"));
            assertTrue("Only the requested verse should be citation-eligible",
                    referenced.getJSONObject(1).getBoolean("citationEligible"));
            assertTrue("Adjacent context must not be cited in place of the requested verse",
                    !referenced.getJSONObject(0).getBoolean("citationEligible")
                            && !referenced.getJSONObject(2).getBoolean("citationEligible"));
            String grammar = CitationGrammar.build(referenced, 280);
            assertTrue(grammar.contains("[Bible@KJV: John 3:16]"));
            assertTrue(!grammar.contains("John 3:15") && !grammar.contains("John 3:17"));
            String prompt = LocalPrompt.build("Explain John 3:16",
                    new org.perazimchurch.app.ai.remote.ConversationHistory().snapshot(), referenced);
            assertTrue(prompt.contains("Bible@KJV: John 3:15; context only, do not cite"));
            org.json.JSONArray numberedBook = LocalScriptureRetriever.retrieve(database.bibleDao(),
                    "Explain 1 John 4:7");
            boolean foundFirstJohn = false;
            for (int i = 0; i < numberedBook.length(); i++) {
                foundFirstJohn |= "1 John 4:7".equals(numberedBook.getJSONObject(i).getString("location"));
            }
            assertTrue("A numbered Bible book must not resolve to the unnumbered book with a matching suffix",
                    foundFirstJohn);
            org.json.JSONArray range = LocalScriptureRetriever.retrieve(database.bibleDao(),
                    "Compare John 3:16-17");
            assertEquals(2, range.length());
            assertEquals("John 3:16", range.getJSONObject(0).getString("location"));
            assertEquals("John 3:17", range.getJSONObject(1).getString("location"));
            assertEquals("Over-limit reference ranges must not be silently truncated", 0,
                    LocalScriptureRetriever.retrieve(database.bibleDao(), "John 3:16-20").length());
            assertEquals("Church information must not fall back to unrelated Scripture", 0,
                    LocalScriptureRetriever.retrieve(database.bibleDao(),
                            "Where is the church headquarters?").length());
        } finally {
            database.close();
            context.deleteDatabase(databaseName);
        }
    }
}
