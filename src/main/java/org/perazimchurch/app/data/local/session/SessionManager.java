package org.perazimchurch.app.data.local.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.UserDao;
import org.perazimchurch.app.data.local.entity.UserEntity;

import java.util.List;

/**
 * Singleton managing active user context, authentication state, and session isolation.
 * Compliant with Phase 1.7 Account Isolation specification.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREFS_NAME = "perazim_session_prefs";
    private static final String KEY_ACTIVE_USER_ID = "active_user_id";

    private static volatile SessionManager INSTANCE;

    private final Context context;
    private final UserDao userDao;
    private final SharedPreferences prefs;

    private volatile String activeUserId;
    private volatile UserEntity currentUserCache;

    /**
     * Retrieves or creates the singleton instance of {@link SessionManager}.
     *
     * @param context Application or activity context.
     * @return The singleton {@link SessionManager}.
     */
    public static SessionManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    PerazimDatabase db = PerazimDatabase.getInstance(appContext);
                    INSTANCE = new SessionManager(appContext, db.userDao());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Retrieves or creates the singleton instance with a specified {@link UserDao}.
     *
     * @param context Context instance (may be null in unit test environments).
     * @param userDao Room UserDao instance.
     * @return The singleton {@link SessionManager}.
     */
    public static SessionManager getInstance(@Nullable Context context, @NonNull UserDao userDao) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    Context appContext = context != null ? context.getApplicationContext() : null;
                    INSTANCE = new SessionManager(appContext, userDao);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Resets the singleton instance (primarily for testing and clean teardown).
     */
    public static synchronized void resetInstance() {
        INSTANCE = null;
    }

    /**
     * Constructs a {@link SessionManager} with the given context and {@link UserDao}.
     *
     * @param context Application context or null.
     * @param userDao Room UserDao instance.
     */
    public SessionManager(@Nullable Context context, @NonNull UserDao userDao) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.userDao = userDao;
        if (this.context != null) {
            this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            this.activeUserId = this.prefs.getString(KEY_ACTIVE_USER_ID, null);
        } else {
            this.prefs = null;
            this.activeUserId = null;
        }
    }

    /**
     * Returns the currently active user ID from memory cache, SharedPreferences,
     * or active database record.
     *
     * @return Active user ID string or null if unauthenticated.
     */
    @Nullable
    public synchronized String getActiveUserId() {
        if (activeUserId != null) {
            return activeUserId;
        }
        if (prefs != null) {
            activeUserId = prefs.getString(KEY_ACTIVE_USER_ID, null);
        }
        if (activeUserId == null && userDao != null) {
            UserEntity activeUser = userDao.getActiveUser();
            if (activeUser != null) {
                activeUserId = activeUser.getId();
                if (prefs != null) {
                    prefs.edit().putString(KEY_ACTIVE_USER_ID, activeUserId).apply();
                }
            }
        }
        return activeUserId;
    }

    /**
     * Explicitly sets the active user ID, synchronizing SharedPreferences, memory cache,
     * and database active state flags.
     *
     * @param id The user ID to activate, or null to clear active user context.
     */
    public synchronized void setActiveUserId(@Nullable String id) {
        long now = System.currentTimeMillis();
        if (userDao != null) {
            List<UserEntity> allUsers = userDao.getAllUsers();
            if (allUsers != null) {
                for (UserEntity u : allUsers) {
                    if (id != null && id.equals(u.getId())) {
                        if (!u.isActive()) {
                            u.setActive(true);
                            u.setUpdatedAt(now);
                            userDao.update(u);
                        }
                    } else if (u.isActive()) {
                        u.setActive(false);
                        u.setUpdatedAt(now);
                        userDao.update(u);
                    }
                }
            }
        }

        this.activeUserId = id;
        this.currentUserCache = (id != null && userDao != null) ? userDao.getUserById(id) : null;

        if (prefs != null) {
            if (id != null) {
                prefs.edit().putString(KEY_ACTIVE_USER_ID, id).apply();
            } else {
                prefs.edit().remove(KEY_ACTIVE_USER_ID).apply();
            }
        }
    }

    /**
     * Logs in a user, inserting a new record or updating/activating an existing record
     * in {@link UserDao}, and updating SharedPreferences and memory cache.
     *
     * @param userId   Unique identifier for the user.
     * @param name     Display/Full name.
     * @param email    Email address.
     * @param campusId Selected campus ID.
     * @param role     User role (e.g. DISCIPLE, PARTNER, LEADER, MEMBER).
     */
    public synchronized void login(@NonNull String userId,
                                   @Nullable String name,
                                   @Nullable String email,
                                   @Nullable String campusId,
                                   @Nullable String role) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }

        long now = System.currentTimeMillis();

        if (userDao != null) {
            // Deactivate any currently active user in DB
            List<UserEntity> allUsers = userDao.getAllUsers();
            if (allUsers != null) {
                for (UserEntity u : allUsers) {
                    if (!userId.equals(u.getId()) && u.isActive()) {
                        u.setActive(false);
                        u.setUpdatedAt(now);
                        userDao.update(u);
                    }
                }
            }

            // Insert or activate target user
            UserEntity user = userDao.getUserById(userId);
            if (user == null) {
                user = new UserEntity(
                        userId,
                        name != null ? name : "",
                        email != null ? email : "",
                        "", // phone
                        role != null ? role : "MEMBER",
                        campusId != null ? campusId : "campus_central",
                        "", // avatarUrl
                        now,
                        now,
                        true
                );
                userDao.insert(user);
            } else {
                if (name != null) user.setName(name);
                if (email != null) user.setEmail(email);
                if (campusId != null) user.setCampusId(campusId);
                if (role != null) user.setRole(role);
                user.setActive(true);
                user.setUpdatedAt(now);
                userDao.update(user);
            }
            this.currentUserCache = user;
        }

        this.activeUserId = userId;
        if (prefs != null) {
            prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply();
        }
        Log.d(TAG, "User logged in successfully: " + userId);
    }

    /**
     * Convenience method to login using an existing {@link UserEntity}.
     *
     * @param user The user entity to log in.
     */
    public synchronized void login(@NonNull UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        login(user.getId(), user.getName(), user.getEmail(), user.getCampusId(), user.getRole());
    }

    /**
     * Logs out the current user, deactivating the user record in {@link UserDao},
     * clearing SharedPreferences, and clearing memory cache.
     */
    public synchronized void logout() {
        long now = System.currentTimeMillis();
        if (userDao != null) {
            List<UserEntity> allUsers = userDao.getAllUsers();
            if (allUsers != null) {
                for (UserEntity u : allUsers) {
                    if (u.isActive()) {
                        u.setActive(false);
                        u.setUpdatedAt(now);
                        userDao.update(u);
                    }
                }
            }
        }
        this.activeUserId = null;
        this.currentUserCache = null;
        if (prefs != null) {
            prefs.edit().remove(KEY_ACTIVE_USER_ID).apply();
        }
        Log.d(TAG, "Active user logged out. Context cleared.");
    }

    /**
     * Returns the currently authenticated {@link UserEntity}.
     *
     * @return Currently active UserEntity, or null if unauthenticated.
     */
    @Nullable
    public synchronized UserEntity getCurrentUser() {
        if (currentUserCache != null && currentUserCache.isActive()) {
            return currentUserCache;
        }
        String currentId = getActiveUserId();
        if (currentId != null && userDao != null) {
            currentUserCache = userDao.getUserById(currentId);
            return currentUserCache;
        }
        if (userDao != null) {
            currentUserCache = userDao.getActiveUser();
            if (currentUserCache != null) {
                this.activeUserId = currentUserCache.getId();
                if (prefs != null) {
                    prefs.edit().putString(KEY_ACTIVE_USER_ID, this.activeUserId).apply();
                }
            }
        }
        return currentUserCache;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if an active user ID exists, false otherwise.
     */
    public synchronized boolean isLoggedIn() {
        return getActiveUserId() != null;
    }

    /**
     * Clears in-memory cache and reloads user entity from database.
     *
     * @return Refreshed UserEntity or null.
     */
    @Nullable
    public synchronized UserEntity refreshCurrentUser() {
        this.currentUserCache = null;
        return getCurrentUser();
    }
}
