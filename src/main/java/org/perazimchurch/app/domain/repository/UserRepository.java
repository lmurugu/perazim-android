package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.User;

/**
 * Repository interface defining operations for User Profile, Streaks, XP, and Grace Points.
 */
public interface UserRepository {
    User getCurrentUser();
    User getUserById(String id);
    void updateUser(User user);
    void updateStreak(int newStreak, boolean frozen);
    void addSpiritualXp(int xpPoints);
    void updateGracePoints(int gracePoints);
    void switchCampus(String campusId);
}
