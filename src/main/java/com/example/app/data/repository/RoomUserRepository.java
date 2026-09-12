package com.example.app.data.repository;

import androidx.annotation.NonNull;

import com.example.app.data.local.dao.CampusDao;
import com.example.app.data.local.dao.UserDao;
import com.example.app.data.local.entity.UserEntity;
import com.example.app.data.mapper.UserMapper;
import com.example.app.domain.model.User;
import com.example.app.domain.repository.UserRepository;

import java.util.List;

/**
 * Room implementation of {@link UserRepository}.
 * Manages user profile persistence, active user resolution, streak tracking,
 * spiritual XP, grace points, and campus switching.
 */
public class RoomUserRepository implements UserRepository {

    private final UserDao userDao;
    private final CampusDao campusDao;

    public RoomUserRepository(@NonNull UserDao userDao, @NonNull CampusDao campusDao) {
        this.userDao = userDao;
        this.campusDao = campusDao;
    }

    @Override
    public User getCurrentUser() {
        UserEntity activeEntity = userDao.getActiveUser();
        if (activeEntity != null) {
            return UserMapper.toDomain(activeEntity);
        }

        List<UserEntity> allUsers = userDao.getAllUsers();
        if (allUsers != null && !allUsers.isEmpty()) {
            UserEntity firstUser = allUsers.get(0);
            firstUser.setActive(true);
            userDao.update(firstUser);
            return UserMapper.toDomain(firstUser);
        }

        // Fallback to default guest user
        User guestUser = new User(
                "guest_user",
                "Guest Pilgrim",
                "guest@perazim.org",
                "+254700000000",
                "campus_central",
                "MEMBER",
                7,
                100,
                50,
                false,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        UserEntity guestEntity = UserMapper.toEntity(guestUser);
        if (guestEntity != null) {
            guestEntity.setActive(true);
            userDao.insert(guestEntity);
        }

        return guestUser;
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
        }
    }

    @Override
    public void addSpiritualXp(int xpPoints) {
        User activeUser = getCurrentUser();
        if (activeUser != null) {
            activeUser.setSpiritualXp(activeUser.getSpiritualXp() + xpPoints);
            activeUser.setUpdatedAt(System.currentTimeMillis());
            updateUser(activeUser);
        }
    }

    @Override
    public void updateGracePoints(int gracePoints) {
        User activeUser = getCurrentUser();
        if (activeUser != null) {
            activeUser.setGracePoints(gracePoints);
            activeUser.setUpdatedAt(System.currentTimeMillis());
            updateUser(activeUser);
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
