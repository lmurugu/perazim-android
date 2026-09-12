package com.example.app.domain.repository;

import com.example.app.domain.model.Prayer;
import java.util.List;

/**
 * Repository interface defining operations for Community Prayer Wall petitions and personal intercessions.
 */
public interface PrayerRepository {
    List<Prayer> getPublicPrayers();
    List<Prayer> getUserPrayers(String userId);
    Prayer getPrayerById(String id);
    void submitPrayer(Prayer prayer);
    void amenPrayer(String prayerId, String userId);
    void markAnswered(String prayerId);
    void deletePrayer(String prayerId);
}
