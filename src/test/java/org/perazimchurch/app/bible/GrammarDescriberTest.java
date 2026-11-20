package org.perazimchurch.app.bible;

import org.junit.Assert;
import org.junit.Test;
import org.perazimchurch.app.data.interlinear.GrammarDescriber;
import org.perazimchurch.app.data.interlinear.GreekInterlinearStore;
import org.perazimchurch.app.domain.model.InterlinearWord;

import java.util.List;

public class GrammarDescriberTest {

    @Test
    public void describesGreekNounCase() {
        Assert.assertEquals("noun · nominative · singular · feminine", GrammarDescriber.describeGreek("N-NSF"));
    }

    @Test
    public void describesGreekAoristVerb() {
        Assert.assertEquals("verb · aorist · active · indicative · 3rd person · singular",
                GrammarDescriber.describeGreek("V-AAI-3S"));
    }

    @Test
    public void describesSecondAorist() {
        Assert.assertTrue(GrammarDescriber.describeGreek("V-2ADI-3S").startsWith("verb · second · aorist"));
    }

    @Test
    public void describesGreekIndeclinable() {
        Assert.assertEquals("conjunction", GrammarDescriber.describeGreek("CONJ"));
    }

    @Test
    public void leavesUnknownGreekCodeUnchanged() {
        Assert.assertEquals("Q-XYZ", GrammarDescriber.describeGreek("Q-XYZ"));
    }

    @Test
    public void describesHebrewPrefixesAndNoun() {
        String parsing = GrammarDescriber.describeHebrew("HC/Td/Ncbsa");
        Assert.assertTrue(parsing, parsing.startsWith("conjunction"));
        Assert.assertTrue(parsing, parsing.contains("definite article"));
        Assert.assertTrue(parsing, parsing.contains("noun · masculine or feminine · singular · absolute state"));
    }

    @Test
    public void describesHebrewQalPerfectVerb() {
        Assert.assertEquals("verb · Qal · perfect · 3rd person · masculine · singular",
                GrammarDescriber.describeHebrew("Vqp3ms"));
    }

    @Test
    public void readsHebrewCellsWithTransliterationAndGloss() {
        List<InterlinearWord> words = GreekInterlinearStore.parseWords("βιβλος|976|N-NSF|biblos|book");
        Assert.assertEquals("biblos", words.get(0).getTransliteration());
        Assert.assertEquals("book", words.get(0).getGloss());
        Assert.assertEquals("noun · nominative · singular · feminine", words.get(0).getParsing());
    }
}
