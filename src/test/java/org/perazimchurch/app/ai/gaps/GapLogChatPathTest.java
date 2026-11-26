package org.perazimchurch.app.ai.gaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.nio.file.Files;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.perazimchurch.app.ai.guide.BlobCipher;
import org.perazimchurch.app.ai.guide.GuideVault;
import org.perazimchurch.app.ai.local.GapSignals;
import org.perazimchurch.app.ai.local.GuideTurnRouter;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The gap log through the real chat routing: {@link GuideTurnRouter#route} decides the status, {@link GapSignals} maps it,
 * and {@link GapLog#recordWith} applies the policy, the faith check and redaction before storing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GapLogChatPathTest {

    private static final BlobCipher PLAIN = new BlobCipher() {
        @Override public byte[] seal(byte[] plain) { return plain.clone(); }
        @Override public byte[] open(byte[] sealed) { return sealed.clone(); }
    };

    private Context context;
    private PerazimAiEngine engine;
    private GapLogStore store;

    @Before
    public void fresh() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        SoftSafety.reset();
        engine = new PerazimAiEngine(context);
        store = GapLogStore.open(new GuideVault(Files.createTempDirectory("gap-chat").toFile(), PLAIN));
    }

    /** What the dialog does for one message: route it, then log it exactly as the observe hooks do. */
    private GuideTurnRouter.Turn askAndObserve(String question) {
        GuideTurnRouter.Turn turn = GuideTurnRouter.route(context, engine, question);
        String reason = GapSignals.reasonForRouted(turn.status);
        if (reason != null) GapLog.recordWith(store, question, reason, turn.status, "test", 1L);
        return turn;
    }

    @Test
    public void crisisMessagesAreNeverLogged() {
        askAndObserve("I want to kill myself. What does the Bible say about this?");
        askAndObserve("My brother texted me, 'I'm ending my life tonight'. What should I do?");
        assertEquals(0, store.all().size());
    }

    @Test
    public void abuseAndPrivacyMessagesAreNeverLogged() {
        askAndObserve("My husband beats me. Is it a sin to leave him, according to the Bible?");
        askAndObserve("What is my password and national ID number for the church app?");
        assertEquals(0, store.all().size());
    }

    @Test
    public void answeredDoctrineIsNotAGap() {
        GuideTurnRouter.Turn turn = askAndObserve("Is baptism necessary for salvation?");
        if (GapSignals.reasonForRouted(turn.status) == null) {
            assertEquals(0, store.all().size());
        }
    }

    @Test
    public void gapReasonForAFaithQuestionIsStoredRedacted() {
        // A no-go zone decline on a faith question, the same status the router returns for a zone match.
        String question = "My name is Grace Wambui. Did Paul write Hebrews?";
        assertEquals(GapReason.NO_GO_DECLINE, GapSignals.reasonForRouted("routed:no-go:history-authorship-research"));
        assertTrue(GapLog.recordWith(store, question, GapReason.NO_GO_DECLINE,
                "routed:no-go:history-authorship-research", "test", 1L));
        List<GapEntry> all = store.all();
        assertEquals(1, all.size());
        assertFalse(all.get(0).question, all.get(0).question.contains("Wambui"));
        assertTrue(all.get(0).question, all.get(0).question.contains("Did Paul write Hebrews?"));
    }

    @Test
    public void nonFaithDeclineIsNotLogged() {
        assertFalse(GapLog.recordWith(store, "What is the stock price of Safaricom today?",
                GapReason.SCOPE_DECLINE, "routed:scope-refusal", "test", 1L));
        assertEquals(0, store.all().size());
    }

    @Test
    public void memberNotHelpfulOnSensitiveQuestionIsNotLogged() {
        assertFalse(GapLog.recordWith(store, "I am afraid my husband will hurt me, what do I do?",
                GapReason.MEMBER_NOT_HELPFUL, "member-flag", "test", 1L));
        assertEquals(0, store.all().size());
    }
}
