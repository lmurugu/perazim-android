package org.perazimchurch.app.data.search;

import android.content.Context;

import androidx.annotation.Nullable;

import org.perazimchurch.app.ai.search.SearchOverviewBuilder;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.data.interlinear.GlossMatch;
import org.perazimchurch.app.data.interlinear.GreekInterlinearStore;
import org.perazimchurch.app.data.interlinear.HebrewInterlinearStore;
import org.perazimchurch.app.data.interlinear.StrongsStore;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.InterlinearWord;
import org.perazimchurch.app.domain.model.StrongsEntry;
import org.perazimchurch.app.domain.repository.BibleRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the bundled Bible text, interlinear and Strong's lexicon for {@link SearchOverviewBuilder}. Verse text is
 * always the KJV, as the AI quotes it verbatim.
 */
public final class BibleOverviewSources implements SearchOverviewBuilder.Sources {

    static final String TRANSLATION = "KJV";
    /** Old Testament books are 1 to 39; the Hebrew interlinear covers them, the Greek covers the rest. */
    static final int LAST_OLD_TESTAMENT_BOOK = 39;

    private final Context context;
    private final BibleRepository bibleRepo;

    public BibleOverviewSources(Context context, BibleRepository bibleRepo) {
        this.context = context;
        this.bibleRepo = bibleRepo;
    }

    @Override
    public List<SearchOverviewBuilder.VerseHit> keywordVerses(String word) {
        List<SearchOverviewBuilder.VerseHit> out = new ArrayList<>();
        List<BibleVerse> found = bibleRepo.searchScripture(TRANSLATION, word);
        if (found == null) {
            return out;
        }
        for (BibleVerse verse : found) {
            int book = BibleMapper.resolveBookNumber(verse.getBookId());
            out.add(new SearchOverviewBuilder.VerseHit(book, verse.getChapterNumber(), verse.getVerseNumber(),
                    verse.getText()));
        }
        return out;
    }

    @Override
    public List<SearchOverviewBuilder.GlossHit> glossWords(String word) {
        List<SearchOverviewBuilder.GlossHit> out = new ArrayList<>();
        addGlossHits(out, GreekInterlinearStore.findByGloss(context, word));
        addGlossHits(out, HebrewInterlinearStore.findByGloss(context, word));
        return out;
    }

    private static void addGlossHits(List<SearchOverviewBuilder.GlossHit> out, List<GlossMatch> matches) {
        for (GlossMatch match : matches) {
            InterlinearWord word = match.word;
            if (word.getStrongsNumber() <= 0) {
                continue;
            }
            out.add(new SearchOverviewBuilder.GlossHit(match.bookNumber, match.chapter, match.verse,
                    word.getStrongsNumber(), word.getOriginal(), word.getTransliteration()));
        }
    }

    @Override
    @Nullable
    public String kjvVerse(int bookNumber, int chapter, int verse) {
        BibleVerse found = bibleRepo.getVerse(TRANSLATION, BibleMapper.getBookIdFromNumber(bookNumber), chapter, verse);
        return found != null ? found.getText() : null;
    }

    @Override
    @Nullable
    public SearchOverviewBuilder.LexiconEntry lexicon(int strongsNumber, boolean hebrew) {
        StrongsEntry entry = StrongsStore.getEntry(context, (hebrew ? "H" : "G") + strongsNumber);
        if (entry == null) {
            return null;
        }
        return new SearchOverviewBuilder.LexiconEntry(entry.getOriginal(), entry.getTransliteration(),
                entry.getDefinition());
    }
}
