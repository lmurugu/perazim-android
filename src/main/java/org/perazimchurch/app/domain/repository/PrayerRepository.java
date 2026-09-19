package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.Prayer;
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
    List<Prayer> getAnsweredPrayers();
    void markAnsweredWithTestimony(String prayerId, String testimonyText);
    void deletePrayer(String prayerId);
}
