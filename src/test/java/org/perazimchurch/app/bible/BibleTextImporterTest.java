package org.perazimchurch.app.bible;

import org.junit.Assert;
import org.junit.Test;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.seeder.BibleTextImporter;

public class BibleTextImporterTest {

    @Test
    public void parsesWellFormedLine() {
        BibleTextImporter.Row row = BibleTextImporter.parseLine("43\t3\t16\tFor God so loved the world.");
        Assert.assertNotNull(row);
        Assert.assertEquals(43, row.bookNumber);
        Assert.assertEquals(3, row.chapter);
        Assert.assertEquals(16, row.verse);
        Assert.assertEquals("For God so loved the world.", row.text);
    }

    @Test
    public void skipsMalformedLines() {
        Assert.assertNull(BibleTextImporter.parseLine(null));
        Assert.assertNull(BibleTextImporter.parseLine(""));
        Assert.assertNull(BibleTextImporter.parseLine("43\t3\t16"));
        Assert.assertNull(BibleTextImporter.parseLine("x\t3\t16\ttext"));
        Assert.assertNull(BibleTextImporter.parseLine("43\t3\t16\t   "));
    }

    @Test
    public void keepsKjvStarterIdScheme() {
        BibleTextImporter.Spec kjv = BibleTextImporter.BUNDLED.get(0);
        BibleVerseEntity entity = BibleTextImporter.toEntity(kjv, new BibleTextImporter.Row(10, 5, 20, "text"));
        Assert.assertNotNull(entity);
        Assert.assertEquals("2SA.5.20", entity.getId());
        Assert.assertEquals("KJV", entity.getTranslationId());
    }

    @Test
    public void prefixesOtherTranslationIds() {
        BibleTextImporter.Spec web = BibleTextImporter.BUNDLED.get(2);
        BibleVerseEntity entity = BibleTextImporter.toEntity(web, new BibleTextImporter.Row(1, 1, 1, "text"));
        Assert.assertNotNull(entity);
        Assert.assertEquals("WEB.GEN.1.1", entity.getId());
        Assert.assertEquals("GEN.1", entity.getChapterId());
    }

    @Test
    public void rejectsOutOfRangeBook() {
        BibleTextImporter.Spec kjv = BibleTextImporter.BUNDLED.get(0);
        Assert.assertNull(BibleTextImporter.toEntity(kjv, new BibleTextImporter.Row(67, 1, 1, "text")));
    }
}
