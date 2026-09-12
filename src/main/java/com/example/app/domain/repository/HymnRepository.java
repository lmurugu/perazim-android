package com.example.app.domain.repository;

import com.example.app.domain.model.Hymn;
import java.util.List;

/**
 * Repository interface defining data operations for Hymns, Lyrics, and Worship charts.
 * Compliant with Guidebook §21.
 */
public interface HymnRepository {
    List<Hymn> getAllHymns();
    Hymn getHymnByNumber(int number);
    Hymn getHymnById(String id);
    List<Hymn> searchHymns(String query);
    List<Hymn> getFavoriteHymns();
    void toggleFavorite(String hymnId, boolean isFavorite);
}
