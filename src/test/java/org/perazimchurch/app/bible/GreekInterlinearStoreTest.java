package org.perazimchurch.app.bible;

import org.junit.Assert;
import org.junit.Test;
import org.perazimchurch.app.data.interlinear.GreekInterlinearStore;
import org.perazimchurch.app.domain.model.InterlinearWord;

import java.util.List;

public class GreekInterlinearStoreTest {

    @Test
    public void parsesWordCells() {
        List<InterlinearWord> words = GreekInterlinearStore.parseWords("αρχη|746|N-DSF ην|1510|V-IAI-3S");
        Assert.assertEquals(2, words.size());
        Assert.assertEquals("αρχη", words.get(0).getOriginal());
        Assert.assertEquals(746, words.get(0).getStrongsNumber());
        Assert.assertEquals("N-DSF", words.get(0).getMorphology());
    }

    @Test
    public void toleratesMissingStrongsAndSkipsBrokenCells() {
        List<InterlinearWord> words = GreekInterlinearStore.parseWords("και||CONJ broken ||X");
        Assert.assertEquals(1, words.size());
        Assert.assertEquals(0, words.get(0).getStrongsNumber());
        Assert.assertEquals("CONJ", words.get(0).getMorphology());
    }
}
