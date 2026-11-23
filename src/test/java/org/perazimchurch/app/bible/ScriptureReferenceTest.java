package org.perazimchurch.app.bible;

import org.junit.Assert;
import org.junit.Test;
import org.perazimchurch.app.domain.model.ScriptureReference;

public class ScriptureReferenceTest {

    @Test
    public void parsesSingleVerse() {
        ScriptureReference ref = ScriptureReference.parse("John 3:16");
        Assert.assertNotNull(ref);
        Assert.assertEquals(43, ref.bookNumber);
        Assert.assertEquals(3, ref.chapter);
        Assert.assertEquals(16, ref.verseStart);
        Assert.assertEquals(16, ref.verseEnd);
    }

    @Test
    public void parsesVerseRangeAndNumberedBook() {
        ScriptureReference ref = ScriptureReference.parse("1 Cor 13:4-7");
        Assert.assertNotNull(ref);
        Assert.assertEquals(46, ref.bookNumber);
        Assert.assertEquals(13, ref.chapter);
        Assert.assertEquals(4, ref.verseStart);
        Assert.assertEquals(7, ref.verseEnd);
    }

    @Test
    public void parsesChapterOnly() {
        ScriptureReference ref = ScriptureReference.parse("2 Samuel 5");
        Assert.assertNotNull(ref);
        Assert.assertEquals(10, ref.bookNumber);
        Assert.assertEquals(5, ref.chapter);
        Assert.assertFalse(ref.namesVerse());
    }

    @Test
    public void rejectsKeywordsAndBadRanges() {
        Assert.assertNull(ScriptureReference.parse("Baal-perazim"));
        Assert.assertNull(ScriptureReference.parse("faith"));
        Assert.assertNull(ScriptureReference.parse("John 3:16-4"));
        Assert.assertNull(ScriptureReference.parse("Nosuchbook 1:1"));
        Assert.assertNull(ScriptureReference.parse(null));
    }
}
