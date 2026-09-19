package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.Reflection;
import java.util.List;

/**
 * Repository interface defining operations for Daily Breakthrough Devotionals.
 */
public interface ReflectionRepository {
    Reflection getTodayReflection();
    List<Reflection> getRecentReflections();
    Reflection getReflectionById(String id);
    void markReflectionCompleted(String reflectionId);
}
