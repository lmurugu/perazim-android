package org.perazimchurch.app.domain.games.catalog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BookOfTest {

    @Test
    public void bookNameIsTakenFromTheReference() {
        assertEquals("Genesis", HintRules.bookOf("Genesis 1:3"));
        assertEquals("1 Samuel", HintRules.bookOf("1 Samuel 3:5"));
        assertEquals("John", HintRules.bookOf("John 4:15"));
    }
}
