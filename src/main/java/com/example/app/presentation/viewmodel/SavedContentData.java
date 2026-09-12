package com.example.app.presentation.viewmodel;

import androidx.annotation.Nullable;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.model.DownloadedContent;
import com.example.app.domain.model.Hymn;

import java.util.Collections;
import java.util.List;

/**
 * Aggregated data payload for saved and offline-accessible content.
 * Encapsulates downloaded sermons, favorite hymns, and bookmarked scripture verses.
 */
public class SavedContentData {
    private final List<DownloadedContent> downloadedSermons;
    private final List<Hymn> favoriteHymns;
    private final List<BibleVerse> bookmarkedVerses;

    public SavedContentData(@Nullable List<DownloadedContent> downloadedSermons,
                            @Nullable List<Hymn> favoriteHymns,
                            @Nullable List<BibleVerse> bookmarkedVerses) {
        this.downloadedSermons = downloadedSermons != null ? downloadedSermons : Collections.emptyList();
        this.favoriteHymns = favoriteHymns != null ? favoriteHymns : Collections.emptyList();
        this.bookmarkedVerses = bookmarkedVerses != null ? bookmarkedVerses : Collections.emptyList();
    }

    public List<DownloadedContent> getDownloadedSermons() {
        return downloadedSermons;
    }

    public List<DownloadedContent> getDownloadedContent() {
        return downloadedSermons;
    }

    public List<Hymn> getFavoriteHymns() {
        return favoriteHymns;
    }

    public List<BibleVerse> getBookmarkedVerses() {
        return bookmarkedVerses;
    }
}
