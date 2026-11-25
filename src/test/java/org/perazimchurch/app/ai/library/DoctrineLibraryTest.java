package org.perazimchurch.app.ai.library;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.BeforeClass;
import org.junit.Test;

/** The doctrine library reads Levi's approved wording and never shows drafts or unapproved statuses. */
public class DoctrineLibraryTest {
    private static DoctrineLibrary library;

    @BeforeClass
    public static void load() throws Exception {
        library = DoctrineLibrary.fromJson(
                read("src/main/assets/ai/library/doctrine/entries.json"),
                read("src/main/assets/ai/library/doctrine/positions.json"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void approvedHolySpiritEntryIsShownAsWrittenWithPerazimViewFirst() {
        String answer = library.answerFor("What is the Holy Spirit baptism and speaking in tongues?");
        assertNotNull(answer);
        assertTrue(answer.startsWith("Spirit baptism is an experience distinct from conversion."));
        assertTrue(answer.contains("Acts 2:4 (King James Version)"));
        assertTrue(answer.contains("No verdict on any person"));
    }

    @Test
    public void approvedAnswerLeadsWithTheBibleViewAndHidesOtherViewsByDefault() {
        String answer = library.answerFor("What is the Holy Spirit baptism and speaking in tongues?");
        assertNotNull(answer);
        assertFalse(answer.contains("Other Christian views"));
        assertFalse(answer.contains("Cessationist view"));
        assertFalse(answer.contains("Classical Pentecostal view"));
    }

    @Test
    public void otherViewsAreAddedOnlyWhenTheQuestionAsksForThem() {
        String answer = library.answerFor("What do other Christians believe about tongues? Do some churches disagree?");
        assertNotNull(answer);
        assertTrue(answer.startsWith("Spirit baptism is an experience distinct from conversion."));
        // Levi's rule (coordinator decision 2026-11-25): no neutral list of other views in approved answers.
        assertFalse(answer.contains("Other Christian views"));
        assertFalse(answer.contains("Cessationist view"));
        assertTrue(answer.contains("Scripture:"));
    }

    @Test
    public void plainPerazimViewQuestionDoesNotAddOtherViews() {
        String answer = library.answerFor("What is your view on the Holy Spirit and tongues?");
        assertNotNull(answer);
        assertFalse(answer.contains("Other Christian views"));
    }

    @Test
    public void approvedPolygamyEntryIsShown() {
        String answer = library.answerFor("Is polygamy allowed for a family in a polygamous marriage?");
        assertNotNull(answer);
        assertTrue(answer.startsWith("Marriage is one man and one woman."));
        assertTrue(answer.contains("pastoral care"));
    }

    @Test
    public void approvedDowryEntryIsShown() {
        String answer = library.answerFor("What about dowry?");
        assertNotNull(answer);
        assertTrue(answer.startsWith("Dowry is an acceptable custom"));
    }

    @Test
    public void developingViewIsLabelledUnderReview() {
        String answer = library.answerFor("Can someone lose their salvation?");
        assertNotNull(answer);
        assertTrue(answer.contains("under review"));
        assertTrue(answer.contains("freely and finally depart"));
    }

    @Test
    public void undecidedTopicIsExplainedWithoutAPosition() {
        String answer = library.answerFor("Is tithing required for every believer?");
        assertNotNull(answer);
        assertTrue(answer.contains("has not settled a position"));
        assertFalse(answer.contains("must"));
    }

    @Test
    public void undecidedTopicInAPersonalMessageGoesToTheModel() {
        assertNull(library.answerFor("My husband wants a divorce and I am so tired, what do I do?"));
    }

    @Test
    public void inScopeQuestionWithNoApprovedTopicReturnsNull() {
        assertNull(library.answerFor("What does Psalm 23 teach about the shepherd?"));
    }

    @Test
    public void draftOnlyTopicsAreNeverShown() {
        // Canon and the creeds are DRAFT_PENDING_LEVI: no matcher or status lets them out here.
        assertNull(library.answerFor("How many books are in the Bible?"));
    }
}
