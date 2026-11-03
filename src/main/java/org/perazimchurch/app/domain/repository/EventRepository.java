package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.Event;
import java.util.List;

/**
 * Repository interface defining operations for Church Events, Calendars, and Services.
 */
public interface EventRepository {
    List<Event> getUpcomingEvents();
    List<Event> getEventsByCampus(String campusId);
    Event getEventById(String id);
}
