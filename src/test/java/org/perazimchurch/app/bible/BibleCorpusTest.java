package org.perazimchurch.app.bible;

import org.junit.Assert;
import org.junit.Test;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleTranslationEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleVerse;

public class BibleCorpusTest {

    @Test
    public void testCanonicalBookNumberResolution() {
        Assert.assertEquals(1, BibleMapper.resolveBookNumber("GEN"));
        Assert.assertEquals(1, BibleMapper.resolveBookNumber("Genesis"));
        Assert.assertEquals(10, BibleMapper.resolveBookNumber("2SA"));
        Assert.assertEquals(10, BibleMapper.resolveBookNumber("2 Samuel"));
        Assert.assertEquals(19, BibleMapper.resolveBookNumber("PSA"));
        Assert.assertEquals(19, BibleMapper.resolveBookNumber("Psalms"));
        Assert.assertEquals(40, BibleMapper.resolveBookNumber("MAT"));
        Assert.assertEquals(40, BibleMapper.resolveBookNumber("Matthew"));
        Assert.assertEquals(66, BibleMapper.resolveBookNumber("REV"));
        Assert.assertEquals(66, BibleMapper.resolveBookNumber("Revelation"));
    }

    @Test
    public void testBookIdFromNumber() {
        Assert.assertEquals("GEN", BibleMapper.getBookIdFromNumber(1));
        Assert.assertEquals("2SA", BibleMapper.getBookIdFromNumber(10));
        Assert.assertEquals("PSA", BibleMapper.getBookIdFromNumber(19));
        Assert.assertEquals("MAT", BibleMapper.getBookIdFromNumber(40));
        Assert.assertEquals("REV", BibleMapper.getBookIdFromNumber(66));
    }

    @Test
    public void testTranslationEntityMapping() {
        BibleTranslationEntity entity = new BibleTranslationEntity(
                "SUV", "SUV", "Swahili Union Version", "sw", "Union Version 1952", false, "Public Domain"
        );
        BibleTranslation domain = BibleMapper.toDomain(entity);
        Assert.assertNotNull(domain);
        Assert.assertEquals("SUV", domain.getId());
        Assert.assertEquals("Swahili Union Version", domain.getName());
        Assert.assertEquals("sw", domain.getLanguage());
    }

    @Test
    public void testVerseEntityMappingWithTranslations() {
        String[] translations = {"KJV", "SUV", "NIV", "ESV", "NKJV"};
        for (String tId : translations) {
            BibleVerseEntity entity = new BibleVerseEntity(
                    tId + ".2SA.5.20",
                    tId,
                    "2SA.5",
                    10,
                    5,
                    20,
                    "David came to Baal-perazim...",
                    false,
                    null
            );

            BibleVerse domain = BibleMapper.toDomain(entity, tId, "2SA");
            Assert.assertNotNull(domain);
            Assert.assertEquals(tId, domain.getTranslationId());
            Assert.assertEquals("2SA", domain.getBookId());
            Assert.assertEquals(5, domain.getChapterNumber());
            Assert.assertEquals(20, domain.getVerseNumber());
            Assert.assertTrue(domain.getText().contains("Baal-perazim"));
        }
    }
}
