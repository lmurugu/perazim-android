package org.perazimchurch.app.ai.local;

import android.content.Context;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.library.DoctrineLibrary;
import org.perazimchurch.app.ai.library.KjvVerseText;
import org.perazimchurch.app.ai.library.NoGoZones;
import org.perazimchurch.app.ai.library.ScriptureRangeTable;

/**
 * The one ordered answer path that runs before any model call. The app and the evaluation harness both use this, so
 * they cannot drift apart. Order: crisis, missing Bible reference, the pre-model route (overdose, harm to others,
 * safeguarding, sensitive, human decision, privacy and scope), approved doctrine, verbatim verse, no-go zone.
 * A null decision means the question may go on to the model, which still needs verified evidence.
 */
public final class PreModel {

    /** Where the answer came from. The status is what the evaluation reports. */
    public static final class Decision {
        public final String status;
        public final String answer;

        Decision(String status, String answer) {
            this.status = status;
            this.answer = answer;
        }
    }

    private static final Pattern BOOK_CHAPTER_VERSE = Pattern.compile("^(.+) (\\d+):(\\d+)$");
    private static final Pattern RANGE_END = Pattern.compile("\\d{1,3}\\s*[:.]\\s*\\d{1,3}\\s*[-–]\\s*(\\d{1,3})");
    static final int MAX_VERSES = 10;

    private PreModel() {}

    /** The decision for a question, or null when it may go to the model. */
    public static Decision decide(Context c, String question) {
        if (PerazimAiEngine.isCrisisQuery(question)) {
            return new Decision("routed:crisis", PerazimAiEngine.crisisResponse().getMessageText());
        }
        ScriptureRangeTable table = null;
        try {
            table = ScriptureRangeTable.fromJson(read(c, "ai/library/scripture/kjv-verse-counts.json"));
        } catch (Exception ignored) {
            table = null;
        }
        // A reference that is not in the King James Version is refused first, as production always has.
        if (table != null) {
            Decision missing = missingReference(table, question);
            if (missing != null) return missing;
        }
        String route = PerazimAiEngine.preModelRoute(question);
        if (route != null) {
            return new Decision("routed:" + route, PerazimAiEngine.preModelResponse(route, question).getMessageText());
        }
        String doctrine = DoctrineLibrary.load(c).answerFor(question);
        if (doctrine != null) return new Decision("routed:doctrine-approved", doctrine);
        if (table != null) {
            Decision verse = verseDecision(table, KjvVerseText.load(c), question);
            if (verse != null) return verse;
        }
        NoGoZones.Zone zone = NoGoZones.match(question);
        if (zone != null) return new Decision("routed:no-go:" + zone.id, zone.decline);
        return null;
    }

    /** The refusal for a confidently named reference that does not exist in the KJV, or null. */
    static Decision missingReference(ScriptureRangeTable table, String question) {
        Reference ref = reference(table, question);
        if (ref == null || ref.valid) return null;
        return new Decision("routed:missing-reference", "I can't find " + ref.label
                + " in the King James Version, so I won't quote it. Please check the reference, or ask a church leader.");
    }

    /**
     * The verbatim King James Version text for a valid reference that the bundled file holds, or a plain statement
     * when the file does not hold it. Returns null when the question names no confident, valid reference.
     */
    static Decision verseDecision(ScriptureRangeTable table, KjvVerseText verses, String question) {
        Reference ref = reference(table, question);
        if (ref == null || !ref.valid) return null;
        if (ref.last - ref.first + 1 > MAX_VERSES) {
            return new Decision("routed:verse-range-too-long", "I can quote up to " + MAX_VERSES
                    + " verses at a time. Please ask for a shorter passage, and read the rest in the Bible tab.");
        }
        StringBuilder out = new StringBuilder();
        for (int v = ref.first; v <= ref.last; v++) {
            String text = verses == null ? null : verses.verse(ref.bookNumber, ref.chapter, v);
            if (text == null) {
                return new Decision("routed:verse-not-in-offline-text", "I can't quote " + ref.label
                        + " from memory, and this offline text does not hold that verse yet. Please read it in the Bible tab, "
                        + "which has the King James Version (KJV).");
            }
            if (out.length() > 0) out.append(' ');
            out.append('\u201c').append(text).append('\u201d');
        }
        return new Decision("routed:verse-kjv", "From the King James Version (KJV), " + ref.label + ":\n\n" + out
                + "\n\nRead it in its context. The Bible tab shows the whole chapter.");
    }

    /** A parsed reference: book, chapter, first and last verse, and whether the KJV has it. */
    static final class Reference {
        final int bookNumber;
        final int chapter;
        final int first;
        final int last;
        final boolean valid;
        final String label;

        Reference(int bookNumber, int chapter, int first, int last, boolean valid, String label) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.first = first;
            this.last = last;
            this.valid = valid;
            this.label = label;
        }
    }

    /** Parses a confidently matched reference, or returns null when there is none. */
    static Reference reference(ScriptureRangeTable table, String question) {
        ScriptureReferenceNormalizer.Result ref = ScriptureReferenceNormalizer.normalize(question);
        if (ref == null || !ref.confident || ref.normalized == null) return null;
        Matcher m = BOOK_CHAPTER_VERSE.matcher(ref.normalized);
        if (!m.find()) return null;
        String book = m.group(1);
        int chapter = Integer.parseInt(m.group(2));
        int first = Integer.parseInt(m.group(3));
        int last = first;
        Matcher range = RANGE_END.matcher(question);
        if (range.find()) last = Integer.parseInt(range.group(1));
        int bookNumber = table.bookNumber(book);
        if (bookNumber <= 0) return null;
        String label = last == first ? ref.normalized : book + " " + chapter + ":" + first + "-" + last;
        boolean valid = last >= first && table.isValid(bookNumber, chapter, first) && table.isValid(bookNumber, chapter, last);
        return new Reference(bookNumber, chapter, first, last, valid, label);
    }

    private static String read(Context c, String path) throws java.io.IOException {
        try (java.io.InputStream in = c.getAssets().open(path)) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
