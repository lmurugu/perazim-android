package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Opens the lexicon exactly as the app does, through the packaged assets, and checks that
 * approved lexicon entries come back for a Strong's number and for a Hebrew number.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LexiconIndexAssetTest {
    /** אָב (av, father) as stored in BDB, written with escapes so the source stays ASCII. */
    private static final String HEBREW_AV = "אָב";

    private LexiconIndex index;

    @After
    public void tearDown() {
        if (index != null) index.close();
    }

    @Test
    public void packagedLexiconOpens() {
        Context context = ApplicationProvider.getApplicationContext();
        index = LexiconIndex.open(context);
        assertNotNull("Packaged lexicon did not open from assets", index);
    }

    @Test
    public void strongsNumberReturnsApprovedLexiconCard() {
        Context context = ApplicationProvider.getApplicationContext();
        index = LexiconIndex.open(context);
        assertNotNull("Packaged lexicon did not open from assets", index);
        ReferenceApprovals approvals = ReferenceGuide.approvals(context);
        List<LexiconKeys.Key> keys = LexiconKeys.parse("What does G26 mean?");
        assertFalse(keys.isEmpty());
        List<ReferenceHit> hits = index.lookup(keys.get(0), approvals, 5);
        assertFalse("No lexicon hits for G26", hits.isEmpty());
        ReferenceHit first = hits.get(0);
        assertEquals(ReferenceHit.KIND_LEXICON, first.kind);
        assertTrue("Lexicon hit has no meaning text: " + first.text, first.text.startsWith("Meaning:"));
    }

    @Test
    public void pointedHebrewWordReturnsBdbLexiconCard() {
        Context context = ApplicationProvider.getApplicationContext();
        index = LexiconIndex.open(context);
        assertNotNull("Packaged lexicon did not open from assets", index);
        ReferenceApprovals approvals = ReferenceGuide.approvals(context);
        List<LexiconKeys.Key> keys = LexiconKeys.parse("What does " + HEBREW_AV + " mean?");
        assertFalse(keys.isEmpty());
        List<ReferenceHit> hits = index.lookup(keys.get(0), approvals, 5);
        assertFalse("No BDB hits for the pointed Hebrew word", hits.isEmpty());
        assertEquals("bdb-openscriptures", hits.get(0).sourceId);
    }

    /**
     * Release composition: TBESH (Q6) is out, and BDB has no Strong's numbers in this pack,
     * so Hebrew Strong's numbers have no lexicon card. Update this test if a Strong's map ships.
     */
    @Test
    public void releasePackHasNoHebrewStrongsCards() {
        Context context = ApplicationProvider.getApplicationContext();
        index = LexiconIndex.open(context);
        assertNotNull("Packaged lexicon did not open from assets", index);
        ReferenceApprovals approvals = ReferenceGuide.approvals(context);
        List<LexiconKeys.Key> keys = LexiconKeys.parse("What does H1 mean?");
        assertFalse(keys.isEmpty());
        assertTrue("Unexpected Hebrew Strong's card in the release pack", index.lookup(keys.get(0), approvals, 5).isEmpty());
    }

    /** Release guard: TBESH (Q6) and ESV-derived name forms (Q7) must not ship until permissions are confirmed. */
    @Test
    public void releasePackageLeavesOutTbeshAndEsvNameForms() {
        Context context = ApplicationProvider.getApplicationContext();
        index = LexiconIndex.open(context);
        assertNotNull("Packaged lexicon did not open from assets", index);
        File unpacked = new File(new File(context.getFilesDir(), "lexicon"), LexiconIndex.UNPACKED_NAME);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(unpacked.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        try {
            assertEquals(0, count(db, "SELECT COUNT(*) FROM lexicon_entries WHERE source_id = 'step-tbesh'"));
            assertEquals(0, count(db, "SELECT COUNT(*) FROM sources WHERE source_id = 'step-tbesh'"));
            assertEquals(0, count(db, "SELECT COUNT(*) FROM names WHERE esv_form IS NOT NULL"));
            assertEquals(19413, count(db, "SELECT COUNT(*) FROM lexicon_entries"));
        } finally {
            db.close();
        }
        assertFalse(ReferenceGuide.approvals(context).isApproved("step-tbesh"));
    }

    private static long count(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) {
            c.moveToFirst();
            return c.getLong(0);
        }
    }
}
