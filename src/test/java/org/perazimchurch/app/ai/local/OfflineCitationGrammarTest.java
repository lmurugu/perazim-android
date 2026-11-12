package org.perazimchurch.app.ai.local;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class OfflineCitationGrammarTest {
    @Test public void grammarRequiresAnAnswerAndAnExactRetrievedCitation() {
        assertEquals("root ::= answer \"\\n\" citation\n"
                        + "answer ::= [^\\n]{1,120}\n"
                        + "citation ::= \"[synthetic-device-source@1: Synthetic location]\"",
                CitationGrammar.build(Collections.singletonList(new CitationGrammar.Citation(
                        "synthetic-device-source", "1", "Synthetic location"))));
    }

    @Test public void grammarRejectsEmptyEvidence() {
        assertThrows(IllegalArgumentException.class,
                () -> CitationGrammar.build(Collections.<CitationGrammar.Citation>emptyList()));
    }

    @Test public void substantiveAnswersReceiveAnExplicitlyBoundedExpandedBudget() {
        String grammar = CitationGrammar.build(Collections.singletonList(new CitationGrammar.Citation(
                "Bible", "KJV", "John 3:16")), 280);
        assertEquals("root ::= answer \"\\n\" citation\n"
                        + "answer ::= [^\\n]{1,280}\n"
                        + "citation ::= \"[Bible@KJV: John 3:16]\"",
                grammar);
        assertThrows(IllegalArgumentException.class,
                () -> CitationGrammar.build(Collections.singletonList(new CitationGrammar.Citation(
                        "Bible", "KJV", "John 3:16")), 0));
    }

}
