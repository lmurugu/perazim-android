package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.Announcement;
import java.util.List;

/**
 * Repository interface defining operations for Church Announcements and Bulletins.
 */
public interface AnnouncementRepository {
    List<Announcement> getActiveAnnouncements();
    Announcement getAnnouncementById(String id);
    void markAsRead(String announcementId);
}
