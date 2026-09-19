package org.perazimchurch.app.data.mapper;

import android.content.Context;

import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.domain.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapper for converting between Room {@link UserEntity} and domain {@link User}.
 * Handles name &lt;-&gt; fullName, phone &lt;-&gt; phoneNumber, and gamification metrics backed
 * by in-memory cache and persistent {@link GamificationStore}.
 */
public final class UserMapper {

    private static volatile Context appContext;
    private static final Map<String, GamificationStats> STATS_CACHE = new ConcurrentHashMap<>();

    private static class GamificationStats {
        int streakCount;
        int spiritualXp;
        int gracePoints;
        boolean streakFrozen;

        GamificationStats(int streakCount, int spiritualXp, int gracePoints, boolean streakFrozen) {
            this.streakCount = streakCount;
            this.spiritualXp = spiritualXp;
            this.gracePoints = gracePoints;
            this.streakFrozen = streakFrozen;
        }
    }

    private UserMapper() {
        // Utility class
    }

    /**
     * Initializes the application context for persistent gamification storage.
     */
    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    /**
     * Returns the initialized application context, or null if not yet initialized.
     */
    @Nullable
    public static Context getAppContext() {
        return appContext;
    }

    /**
     * Maps a {@link UserEntity} to a domain {@link User}.
     * Handles name -&gt; fullName, phone -&gt; phoneNumber, and restores gamification stats
     * from STATS_CACHE or GamificationStore before falling back to defaults.
     */
    @Nullable
    public static User toDomain(@Nullable UserEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = new User();
        user.setId(entity.getId());
        user.setFullName(entity.getName());
        user.setEmail(entity.getEmail());
        user.setPhoneNumber(entity.getPhone());
        user.setCampusId(entity.getCampusId());
        user.setRole(entity.getRole());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());

        if (appContext != null) {
            String uid = entity.getId();
            int streak = GamificationStore.getStreak(appContext, uid, entity.getStreakCount() > 0 ? entity.getStreakCount() : 7);
            boolean frozen = GamificationStore.isStreakFrozen(appContext, uid, entity.isStreakFrozen());
            int xp = GamificationStore.getSpiritualXp(appContext, uid, entity.getSpiritualXp() > 0 ? entity.getSpiritualXp() : 100);
            int grace = GamificationStore.getGracePoints(appContext, uid, entity.getGracePoints() > 0 ? entity.getGracePoints() : 50);
            user.setStreakCount(streak);
            user.setSpiritualXp(xp);
            user.setGracePoints(grace);
            user.setStreakFrozen(frozen);
            STATS_CACHE.put(uid, new GamificationStats(streak, xp, grace, frozen));
        } else {
            GamificationStats stats = STATS_CACHE.get(entity.getId());
            if (stats != null) {
                user.setStreakCount(stats.streakCount);
                user.setSpiritualXp(stats.spiritualXp);
                user.setGracePoints(stats.gracePoints);
                user.setStreakFrozen(stats.streakFrozen);
            } else {
                // Default gamification values from entity or fallback
                user.setStreakCount(entity.getStreakCount() > 0 ? entity.getStreakCount() : 7);
                user.setSpiritualXp(entity.getSpiritualXp() > 0 ? entity.getSpiritualXp() : 100);
                user.setGracePoints(entity.getGracePoints() > 0 ? entity.getGracePoints() : 50);
                user.setStreakFrozen(entity.isStreakFrozen());
            }
        }

        return user;
    }

    /**
     * Maps a domain {@link User} to a Room {@link UserEntity}.
     * Handles fullName -&gt; name, phoneNumber -&gt; phone, updates cached gamification stats,
     * and persists them to GamificationStore.
     */
    @Nullable
    public static UserEntity toEntity(@Nullable User domain) {
        if (domain == null) {
            return null;
        }

        String id = domain.getId() != null ? domain.getId() : UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long createdAt = domain.getCreatedAt() > 0 ? domain.getCreatedAt() : now;
        long updatedAt = domain.getUpdatedAt() > 0 ? domain.getUpdatedAt() : now;

        // Cache stats for domain user
        STATS_CACHE.put(id, new GamificationStats(
                domain.getStreakCount(),
                domain.getSpiritualXp(),
                domain.getGracePoints(),
                domain.isStreakFrozen()
        ));

        // Persist to SharedPreferences store if context is available
        if (appContext != null) {
            GamificationStore.saveStreak(appContext, id, domain.getStreakCount(), domain.isStreakFrozen());
            GamificationStore.saveSpiritualXp(appContext, id, domain.getSpiritualXp());
            GamificationStore.saveGracePoints(appContext, id, domain.getGracePoints());
        }

        return new UserEntity(
                id,
                domain.getFullName(),
                domain.getEmail(),
                domain.getPhoneNumber(),
                domain.getRole() != null ? domain.getRole() : "MEMBER",
                domain.getCampusId() != null ? domain.getCampusId() : "campus_central",
                "",
                createdAt,
                updatedAt,
                true,
                domain.getStreakCount(),
                domain.getSpiritualXp(),
                domain.getGracePoints(),
                domain.isStreakFrozen()
        );
    }

    /**
     * Updates gamification stats in cache and GamificationStore for a given user id.
     */
    public static void updateGamificationStats(String userId, int streakCount, int spiritualXp, int gracePoints, boolean streakFrozen) {
        if (userId != null) {
            STATS_CACHE.put(userId, new GamificationStats(streakCount, spiritualXp, gracePoints, streakFrozen));
            if (appContext != null) {
                GamificationStore.saveStreak(appContext, userId, streakCount, streakFrozen);
                GamificationStore.saveSpiritualXp(appContext, userId, spiritualXp);
                GamificationStore.saveGracePoints(appContext, userId, gracePoints);
            }
        }
    }

    /**
     * Maps a list of {@link UserEntity} to a list of domain {@link User}.
     */
    public static List<User> toDomainList(@Nullable List<UserEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> list = new ArrayList<>(entities.size());
        for (UserEntity entity : entities) {
            User user = toDomain(entity);
            if (user != null) {
                list.add(user);
            }
        }
        return list;
    }

    /**
     * Maps a list of domain {@link User} to a list of {@link UserEntity}.
     */
    public static List<UserEntity> toEntityList(@Nullable List<User> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserEntity> list = new ArrayList<>(domains.size());
        for (User domain : domains) {
            UserEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
