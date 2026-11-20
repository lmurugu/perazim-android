package org.perazimchurch.app.ai;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Host-side harness for the offline assistant's scope gate. Runs on the laptop with no emulator or phone.
 * Synthetic prompts only; no member data. Off-topic prompts must be refused by the deterministic gate.
 * In-scope prompts must never be refused by it. Anything the gate does not catch is still refused at
 * runtime, because the on-device path requires verified approved-source evidence before generation.
 */
public class OfflineScopeHarnessTest {

    /** Off-topic prompts the deterministic gate must refuse. */
    private static final String[] MUST_REFUSE = {
            "Which stock should I buy this week?",
            "Should I invest in bitcoin?",
            "Help me choose a retirement portfolio",
            "What medicine should I take for a headache?",
            "Give me a dose of ibuprofen for my child",
            "Give me legal advice about my lease",
            "Should I sue my employer?",
            "Draft a contract for my freelance work",
            "Who won the football match last night?",
            "Write me a Python script to sort a list",
            "Explain this JavaScript code snippet",
            "Solve this equation: 3x + 5 = 20",
            "What is 45 x 12?",
            "Calculate the integral of x squared",
            "Give me a recipe for chapati",
            "How do I cook rice without a pot?",
            "What is the weather today in Nairobi?",
            "Weather forecast for tomorrow please",
            "Book flights to Mombasa for Friday",
            "Find cheap hotels in Kisumu",
            "Help me with a visa application",
            "Recommend a good movie tonight",
            "Which Netflix series should I watch?",
            "Who is the most popular celebrity this year?",
    };

    /** Church, faith and app prompts that the gate must never refuse. */
    private static final String[] MUST_PASS = {
            "What does Scripture teach about stewardship?",
            "What does the Bible say about the song of Moses?",
            "Write a prayer for my cell group",
            "Explain Romans 8 in simple terms",
            "How do I give my tithe through M-Pesa?",
            "Where is the paybill for offerings?",
            "Which hymn is number 12 in the hymnal?",
            "How do I watch the livestream on Sunday?",
            "What is the daily verse today?",
            "Show me the chapter of Genesis 1",
            "How do I join a cell group?",
            "How do I post a prayer request on the wall?",
            "How many XP do I need to level up?",
            "What is my streak in the app?",
            "Explain the meaning of grace in Ephesians 2",
            "Who is the bishop of the church?",
            "How do I recover my account in the app?",
            "What does the weather of the spirit in Ezekiel mean?",
            "Explain the movement of the Holy Spirit in Acts 2",
    };

    @Test
    public void offTopicPromptsAreRefusedByDeterministicGate() {
        List<String> misses = new ArrayList<>();
        for (String prompt : MUST_REFUSE) {
            if (!PerazimAiEngine.isClearlyOutOfScopeQuery(prompt)) misses.add(prompt);
        }
        if (!misses.isEmpty()) {
            fail("Off-topic prompts not refused by the gate: " + misses);
        }
    }

    @Test
    public void inScopePromptsAreNeverRefusedByDeterministicGate() {
        List<String> wrongRefusals = new ArrayList<>();
        for (String prompt : MUST_PASS) {
            if (PerazimAiEngine.isClearlyOutOfScopeQuery(prompt)) wrongRefusals.add(prompt);
        }
        if (!wrongRefusals.isEmpty()) {
            fail("In-scope prompts wrongly refused: " + wrongRefusals);
        }
    }

    @Test
    public void harnessCorpusIsNonEmpty() {
        assertTrue(MUST_REFUSE.length >= 20);
        assertTrue(MUST_PASS.length >= 15);
    }
}
