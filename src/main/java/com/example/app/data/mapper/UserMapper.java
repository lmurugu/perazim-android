package com.example.app.data.mapper;

import androidx.annotation.Nullable;

import com.example.app.data.local.entity.UserEntity;
import com.example.app.domain.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapper for converting between Room {@link UserEntity} and domain {@link User}.
 * Handles name &lt;-&gt; fullName, phone &lt;-&gt; phoneNumber, and gamification metrics.
 */
public final class UserMapper {

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
     * Maps a {@link UserEntity} to a domain {@link User}.
     * Handles name -&gt; fullName, phone -&gt; phoneNumber, and restores gamification stats.
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

        GamificationStats stats = STATS_CACHE.get(entity.getId());
        if (stats != null) {
            user.setStreakCount(stats.streakCount);
            user.setSpiritualXp(stats.spiritualXp);
            user.setGracePoints(stats.gracePoints);
            user.setStreakFrozen(stats.streakFrozen);
        } else {
            // Default gamification values
            user.setStreakCount(7);
            user.setSpiritualXp(100);
            user.setGracePoints(50);
            user.setStreakFrozen(false);
        }

        return user;
    }

    /**
     * Maps a domain {@link User} to a Room {@link UserEntity}.
     * Handles fullName -&gt; name, phoneNumber -&gt; phone, and updates cached gamification stats.
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
                true
        );
    }

    /**
     * Updates gamification stats in cache for a given user id.
     */
    public static void updateGamificationStats(String userId, int streakCount, int spiritualXp, int gracePoints, boolean streakFrozen) {
        if (userId != null) {
            STATS_CACHE.put(userId, new GamificationStats(streakCount, spiritualXp, gracePoints, streakFrozen));
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
