package org.perazimchurch.app.ai.local;

import android.content.Context;

import androidx.annotation.Nullable;

import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves explicit lookup requests against the same local KJV data used by the Bible reader. */
public final class OfflineScriptureLookup {
    private static final String TRANSLATION = "KJV";
    private static final int MAX_VERSES = 20;
    private static final Pattern REQUEST = Pattern.compile(
            "(?:(?:please\\s+)?(?:read|show(?:\\s+me)?|quote|look\\s+up)\\s+)?"
                    + "((?:[1-3]\\s*)?[a-z]+(?:\\s+[a-z]+){0,3})\\s+"
                    + "([1-9]\\d{0,2}):([1-9]\\d{0,2})(?:\\s*[-–]\\s*([1-9]\\d{0,2}))?"
                    + "(?:\\s*(?:\\(kjv\\)|kjv))?[.?!]?",
            Pattern.CASE_INSENSITIVE);

    private OfflineScriptureLookup() {}

    public static boolean matchesRequest(String question) {
        return REQUEST.matcher(question.trim()).matches();
    }

    /** Call off the main thread. A null result leaves the normal guide routing in charge. */
    @Nullable
    public static AiResponse answer(Context context, String question) {
        if (!matchesRequest(question)) return null;
        try {
            return answer(PerazimDatabase.getInstance(context).bibleDao(), question);
        } catch (android.database.sqlite.SQLiteException | IllegalStateException failure) {
            return unavailable();
        }
    }

    @Nullable
    static AiResponse answer(BibleDao dao, String question) {
        Matcher request = REQUEST.matcher(question.trim());
        if (!request.matches()) return null;
        String requestedBook = request.group(1).replaceAll("\\s+", "");
        int chapter = Integer.parseInt(request.group(2));
        int first = Integer.parseInt(request.group(3));
        int last = request.group(4) == null ? first : Integer.parseInt(request.group(4));

        for (BibleBookEntity book : dao.getBooksByTranslation(TRANSLATION)) {
            String name = book.getName();
            if (name == null) continue;
            String compactName = name.replaceAll("\\s+", "");
            if (!compactName.equalsIgnoreCase(requestedBook)
                    && !(compactName.equalsIgnoreCase("Psalms") && requestedBook.equalsIgnoreCase("Psalm"))) {
                continue;
            }

            if (last < first || last - first >= MAX_VERSES) {
                return AiResponse.plainText("Choose an ascending range of up to 20 verses in one chapter.", null);
            }
            if (chapter > book.getChapterCount()) return unavailable();
            List<BibleVerseEntity> verses = dao.getVerses(TRANSLATION, book.getBookNumber(), chapter);
            StringBuilder text = new StringBuilder(name).append(' ').append(chapter)
                    .append(':').append(first);
            if (first != last) text.append('–').append(last);
            text.append(" (").append(TRANSLATION).append(')');

            int next = first;
            for (BibleVerseEntity verse : verses) {
                if (verse.getVerseNumber() < first) continue;
                if (verse.getVerseNumber() != next || verse.getText() == null
                        || verse.getText().trim().isEmpty()) return unavailable();
                text.append("\n\n").append(next).append(' ').append(verse.getText());
                if (next == last) return AiResponse.plainText(text.toString(), null);
                next++;
            }
            return unavailable();
        }
        return null;
    }

    private static AiResponse unavailable() {
        return AiResponse.plainText("I couldn’t find that complete passage in the local KJV Bible. Check the reference or look it up in the Bible reader.", null);
    }
}
