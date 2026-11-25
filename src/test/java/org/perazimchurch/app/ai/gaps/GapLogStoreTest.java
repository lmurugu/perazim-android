package org.perazimchurch.app.ai.gaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.junit.Test;
import org.perazimchurch.app.ai.guide.BlobCipher;
import org.perazimchurch.app.ai.guide.GuideVault;

public class GapLogStoreTest {

    /** Stores bytes unchanged. The real app uses AndroidGuideCipher. */
    private static final BlobCipher PLAIN = new BlobCipher() {
        @Override public byte[] seal(byte[] plain) { return plain.clone(); }
        @Override public byte[] open(byte[] sealed) { return sealed.clone(); }
    };

    private static GuideVault vault() throws Exception {
        return new GuideVault(Files.createTempDirectory("gap-log").toFile(), PLAIN);
    }

    @Test
    public void repeatedQuestionIsCountedInOneEntry() throws Exception {
        GapLogStore store = GapLogStore.open(vault());
        assertTrue(store.record("Is baptism needed?", "no-verified-source", "r", "1 (1)", 100L));
        assertTrue(store.record("is BAPTISM needed", "no-verified-source", "r", "1 (1)", 200L));
        assertEquals(1, store.all().size());
        assertEquals(2, store.all().get(0).count);
        assertEquals(100L, store.all().get(0).firstSeen);
        assertEquals(200L, store.all().get(0).lastSeen);
    }

    @Test
    public void sameQuestionWithDifferentReasonIsKeptSeparately() throws Exception {
        GapLogStore store = GapLogStore.open(vault());
        store.record("Is baptism needed?", "no-verified-source", "r", "1 (1)", 1L);
        store.record("Is baptism needed?", "member-not-helpful", "r", "1 (1)", 2L);
        assertEquals(2, store.all().size());
    }

    @Test
    public void listKeepsOnlyTheNewestEntriesUpToTheCap() throws Exception {
        GapLogStore store = GapLogStore.open(vault());
        for (int i = 0; i < GapLogStore.CAP + 5; i++) {
            store.record("Question about grace number " + i + " in church", "no-verified-source", "r", "1 (1)", i);
        }
        assertEquals(GapLogStore.CAP, store.all().size());
        assertEquals(GapLogStore.CAP + 4, store.all().get(0).lastSeen);
    }

    @Test
    public void entriesSurviveReopeningAndClearEmptiesThem() throws Exception {
        GuideVault v = vault();
        GapLogStore store = GapLogStore.open(v);
        store.record("Is baptism needed?", "no-verified-source", "r", "1 (1)", 1L);
        assertEquals(1, GapLogStore.open(v).all().size());
        assertTrue(store.clear());
        assertEquals(0, GapLogStore.open(v).all().size());
    }

    @Test
    public void exportHasOnlyTheReviewFields() throws Exception {
        GapLogStore store = GapLogStore.open(vault());
        store.record("Is baptism needed?", "scripture-only", "routed:no-go:x", "1.0 (24)", 5L);
        String lines = GapLogStore.toJsonLines(store.all());
        assertTrue(lines, lines.contains("\"schema\":\"perazim-gap-v1\""));
        assertTrue(lines, lines.contains("\"question\":\"Is baptism needed?\""));
        assertTrue(lines, lines.contains("\"reason\":\"scripture-only\""));
        assertFalse(lines, lines.contains("answer"));
        assertFalse(lines, lines.contains("device"));
        assertFalse(lines, lines.contains("uid"));
        assertEquals(1, lines.split("\n").length);
    }
}
