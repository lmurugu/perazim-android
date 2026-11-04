package org.perazimchurch.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.dao.CampusDao;
import org.perazimchurch.app.data.local.dao.UserDao;
import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.mapper.UserMapper;
import org.perazimchurch.app.domain.model.User;
import org.perazimchurch.app.domain.repository.UserRepository;

import java.util.List;

/**
 * Room implementation of {@link UserRepository}.
 * Manages user profile persistence, active user resolution, streak tracking,
 * spiritual XP, grace points, and campus switching. Backed by Room and {@link GamificationStore}.
 */
public class RoomUserRepository implements UserRepository {

    private final UserDao userDao;
    private final CampusDao campusDao;
    private final Context context;

    public RoomUserRepository(@NonNull UserDao userDao, @NonNull CampusDao campusDao) {
        this(userDao, campusDao, UserMapper.getAppContext());
    }

    public RoomUserRepository(@NonNull UserDao userDao, @NonNull CampusDao campusDao, @Nullable Context context) {
        this.userDao = userDao;
        this.campusDao = campusDao;
        this.context = context != null ? context.getApplicationContext() : UserMapper.getAppContext();
    }

    private Context getEffectiveContext() {
        return context != null ? context : UserMapper.getAppContext();
    }

    @Override
    public User getCurrentUser() {
        Context ctx = getEffectiveContext();
        boolean isGuest = (ctx != null) && org.perazimchurch.app.data.local.session.SessionManager.getInstance(ctx).isGuest();
        if (!isGuest) {
            UserEntity activeEntity = userDao.getActiveUser();
            if (activeEntity != null) {
                return UserMapper.toDomain(activeEntity);
            }
        }

        // Fallback to guest user
        String guestId = (ctx != null) ? org.perazimchurch.app.data.local.session.SessionManager.getInstance(ctx).getActiveUserId() : "guest";
        if (guestId == null || guestId.trim().isEmpty()) {
            guestId = "guest";
        }
        int defaultStreak = GamificationStore.getStreak(ctx, guestId, 0);
        boolean defaultFrozen = GamificationStore.isStreakFrozen(ctx, guestId, false);
        int defaultXp = GamificationStore.getSpiritualXp(ctx, guestId, 0);
        int defaultGrace = GamificationStore.getGracePoints(ctx, guestId, 2);

        return new User(
                guestId,
                "Guest Pilgrim",
                "guest@perazim.org",
                "+254700000000",
                "campus_central",
                "GUEST",
                defaultStreak,
                defaultXp,
                defaultGrace,
                defaultFrozen,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );
    }

    @Override
    public User getUserById(String id) {
        if (id == null) {
            return null;
        }
        UserEntity entity = userDao.getUserById(id);
        return UserMapper.toDomain(entity);
    }

    @Override
    public void updateUser(User user) {
        if (user == null) {
            return;
        }
        UserEntity entity = UserMapper.toEntity(user);
        if (entity != null) {
            UserEntity existing = userDao.getUserById(entity.getId());
            if (existing != null) {
                entity.setActive(existing.isActive());
                if (entity.getAvatarUrl() == null || entity.getAvatarUrl().isEmpty()) {
                    entity.setAvatarUrl(existing.getAvatarUrl());
                }
                userDao.update(entity);
            } else {
                userDao.insert(entity);
            }
        }
    }

    @Override
    public void updateStreak(int newStreak, boolean frozen) {
        User activeUser = getCurrentUser();
        if (activeUser != null) {
            activeUser.setStreakCount(newStreak);
            activeUser.setStreakFrozen(frozen);
            activeUser.setUpdatedAt(System.currentTimeMillis());
            updateUser(activeUser);

            Context ctx = getEffectiveContext();
            if (ctx != null) {
                GamificationStore.saveStreak(ctx, activeUser.getId(), newStreak, frozen);
            }
        }
    }

    @Override
    public void addSpiritualXp(int xpPoints) {
        User activeUser = getCurrentUser();
        if (activeUser != null) {
            int newXp = activeUser.getSpiritualXp() + xpPoints;
            activeUser.setSpiritualXp(newXp);
            activeUser.setUpdatedAt(System.currentTimeMillis());
            updateUser(activeUser);

            Context ctx = getEffectiveContext();
            if (ctx != null) {
                GamificationStore.saveSpiritualXp(ctx, activeUser.getId(), newXp);
            }
        }
    }

    @Override
    public void updateGracePoints(int gracePoints) {
        User activeUser = getCurrentUser();
        if (activeUser != null) {
            activeUser.setGracePoints(gracePoints);
            activeUser.setUpdatedAt(System.currentTimeMillis());
            updateUser(activeUser);

            Context ctx = getEffectiveContext();
            if (ctx != null) {
                GamificationStore.saveGracePoints(ctx, activeUser.getId(), gracePoints);
            }
        }
    }

    @Override
    public void switchCampus(String campusId) {
        User activeUser = getCurrentUser();
        if (activeUser != null) {
            activeUser.setCampusId(campusId);
            activeUser.setUpdatedAt(System.currentTimeMillis());
            updateUser(activeUser);
        }
    }

    /**
     * Getter for campusDao if needed by callers or campus queries.
     */
    public CampusDao getCampusDao() {
        return campusDao;
    }
}
