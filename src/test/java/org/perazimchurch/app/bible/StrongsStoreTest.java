package org.perazimchurch.app.bible;

import org.junit.Assert;
import org.junit.Test;
import org.perazimchurch.app.data.interlinear.StrongsStore;
import org.perazimchurch.app.domain.model.StrongsEntry;

public class StrongsStoreTest {

    @Test
    public void parsesEntryLine() {
        StrongsEntry entry = StrongsStore.parseLine("H430\tאֱלֹהִים\tʼĕlôhîym\tgods");
        Assert.assertNotNull(entry);
        Assert.assertEquals("H430", entry.getCode());
        Assert.assertEquals("gods", entry.getDefinition());
    }

    @Test
    public void rejectsMalformedLines() {
        Assert.assertNull(StrongsStore.parseLine(null));
        Assert.assertNull(StrongsStore.parseLine("G1\tonly-two"));
        Assert.assertNull(StrongsStore.parseLine("\t\tx\ty"));
    }
}
