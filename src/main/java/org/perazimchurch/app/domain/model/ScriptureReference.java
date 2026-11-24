package org.perazimchurch.app.domain.model;

import androidx.annotation.Nullable;

import org.perazimchurch.app.data.mapper.BibleMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed Bible reference such as "John 3:16", "1 Cor 13:4-7" or "Psalm 23".
 *
 * <p>Used by the reader's search box so a typed reference opens the passage offline,
 * without a full-text search.
 */
public final class ScriptureReference {

    private static final Pattern REFERENCE = Pattern.compile(
            "^((?:[1-3] *)?[A-Za-z][A-Za-z.]*(?: [A-Za-z][A-Za-z.]*)*)\\s+(\\d{1,3})(?:\\s*:\\s*(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?)?$");

    public final int bookNumber;
    public final int chapter;
    /** First verse named, or 0 when the reference names only a chapter. */
    public final int verseStart;
    /** Last verse named, equal to verseStart for a single verse, or 0 when none is named. */
    public final int verseEnd;

    private ScriptureReference(int bookNumber, int chapter, int verseStart, int verseEnd) {
        this.bookNumber = bookNumber;
        this.chapter = chapter;
        this.verseStart = verseStart;
        this.verseEnd = verseEnd;
    }

    /**
     * Parses a reference, or returns null when the text is not one (for example a plain keyword).
     */
    @Nullable
    public static ScriptureReference parse(@Nullable String input) {
        if (input == null) {
            return null;
        }
        String text = input.trim();
        Matcher matcher = REFERENCE.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        int book = BibleMapper.findBookNumber(matcher.group(1));
        if (book < 1) {
            return null;
        }
        int chapter = Integer.parseInt(matcher.group(2));
        if (chapter < 1) {
            return null;
        }
        int verseStart = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        int verseEnd = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : verseStart;
        if (verseStart == 0 && verseEnd != 0) {
            return null;
        }
        if (verseEnd != 0 && verseEnd < verseStart) {
            return null;
        }
        return new ScriptureReference(book, chapter, verseStart, verseEnd);
    }

    public boolean namesVerse() {
        return verseStart > 0;
    }
}
