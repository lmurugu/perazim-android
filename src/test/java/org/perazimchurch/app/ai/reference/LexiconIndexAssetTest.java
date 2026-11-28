package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
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
    public void hebrewStrongsNumberReturnsApprovedLexiconCard() {
        Context context = ApplicationProvider.getApplicationContext();
        index = LexiconIndex.open(context);
        assertNotNull("Packaged lexicon did not open from assets", index);
        ReferenceApprovals approvals = ReferenceGuide.approvals(context);
        List<LexiconKeys.Key> keys = LexiconKeys.parse("Meaning of H1?");
        assertFalse(keys.isEmpty());
        List<ReferenceHit> hits = index.lookup(keys.get(0), approvals, 5);
        assertFalse("No lexicon hits for H1", hits.isEmpty());
    }
}
