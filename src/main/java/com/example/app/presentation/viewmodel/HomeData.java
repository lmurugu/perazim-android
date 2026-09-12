package com.example.app.presentation.viewmodel;

import androidx.annotation.Nullable;
import com.example.app.domain.model.Announcement;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.model.Event;
import com.example.app.domain.model.Reflection;
import com.example.app.domain.model.Sermon;
import com.example.app.domain.model.User;

import java.util.Collections;
import java.util.List;

/**
 * Aggregated data payload for the Home Feed.
 * Encapsulates scripture of the day, reflection, sermons, events, announcements,
 * and user spiritual progression metrics.
 */
public class HomeData {
    private final BibleVerse scripture;
    private final Reflection todayReflection;
    private final List<Sermon> recentSermons;
    private final List<Event> upcomingEvents;
    private final List<Announcement> activeAnnouncements;
    private final User currentUser;
    private final int streak;
    private final int spiritualXp;

    public HomeData(@Nullable BibleVerse scripture,
                    @Nullable Reflection todayReflection,
                    @Nullable List<Sermon> recentSermons,
                    @Nullable List<Event> upcomingEvents,
                    @Nullable List<Announcement> activeAnnouncements,
                    @Nullable User currentUser) {
        this.scripture = scripture;
        this.todayReflection = todayReflection;
        this.recentSermons = recentSermons != null ? recentSermons : Collections.emptyList();
        this.upcomingEvents = upcomingEvents != null ? upcomingEvents : Collections.emptyList();
        this.activeAnnouncements = activeAnnouncements != null ? activeAnnouncements : Collections.emptyList();
        this.currentUser = currentUser;
        this.streak = currentUser != null ? currentUser.getStreakCount() : 0;
        this.spiritualXp = currentUser != null ? currentUser.getSpiritualXp() : 0;
    }

    @Nullable
    public BibleVerse getScripture() {
        return scripture;
    }

    @Nullable
    public BibleVerse getDailyVerse() {
        return scripture;
    }

    @Nullable
    public Reflection getTodayReflection() {
        return todayReflection;
    }

    public List<Sermon> getRecentSermons() {
        return recentSermons;
    }

    public List<Event> getUpcomingEvents() {
        return upcomingEvents;
    }

    public List<Announcement> getActiveAnnouncements() {
        return activeAnnouncements;
    }

    @Nullable
    public User getCurrentUser() {
        return currentUser;
    }

    public int getStreak() {
        return streak;
    }

    public int getSpiritualXp() {
        return spiritualXp;
    }
}
