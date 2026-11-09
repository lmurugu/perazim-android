package org.perazimchurch.app.data.local.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.UserDao;
import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.remote.ApiClient;
import org.perazimchurch.app.data.remote.AuthRemoteSource;

import java.util.List;

/**
 * Singleton managing active user context, authentication state, and session isolation.
 * Compliant with Phase 1.7 Account Isolation specification.
 */
public class SessionManager {

    private static final java.util.concurrent.ExecutorService MIGRATION_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();

    private static final String TAG = "SessionManager";
    private static final String PREFS_NAME = "perazim_session_prefs";
    private static final String KEY_ACTIVE_USER_ID = "active_user_id";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_GUEST_USER_ID = "guest_user_id";

    private static volatile SessionManager INSTANCE;

    private final Context context;
    private final UserDao userDao;
    private final SharedPreferences prefs;
    private final SessionTokenStore tokenStore;

    private volatile String activeUserId;
    private volatile String authToken;
    private volatile String authTokenAccount;
    private volatile String authTokenOrigin;
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
            this.tokenStore = new SessionTokenStore(this.context);
            this.activeUserId = this.prefs.getString(KEY_ACTIVE_USER_ID, null);
        } else {
            this.prefs = null;
            this.tokenStore = null;
            this.activeUserId = null;
        }
    }

    /**
     * Returns the currently active user ID from memory cache, SharedPreferences,
     * or active database record. Defaults to "guest" to ensure offline resilience.
     *
     * @return Active user ID string ("guest" if unauthenticated).
     */
    @NonNull
    public synchronized String getActiveUserId() {
        if (activeUserId != null) {
            return activeUserId;
        }
        if (prefs != null) {
            activeUserId = prefs.getString(KEY_ACTIVE_USER_ID, null);
        }
        if (activeUserId == null && userDao != null && Looper.myLooper() != Looper.getMainLooper()) {
            UserEntity activeUser = userDao.getActiveUser();
            if (activeUser != null) {
                activeUserId = activeUser.getId();
                if (prefs != null) {
                    prefs.edit().putString(KEY_ACTIVE_USER_ID, activeUserId).apply();
                }
            }
        }
        if (activeUserId == null || activeUserId.trim().isEmpty() || "guest".equals(activeUserId)) {
            activeUserId = getGuestUserId();
        }
        return activeUserId;
    }

    @Nullable
    public synchronized String getAuthToken() {
        if (context == null || tokenStore == null) return null;
        String accountId = getActiveUserId();
        if (accountId.startsWith("guest")) return null;
        String origin;
        try {
            origin = SessionTokenStore.normalizeOrigin(ApiClient.getBaseUrl(context));
        } catch (IllegalArgumentException e) {
            clearTokenCache();
            return null;
        }
        if (authToken != null) {
            if (accountId.equals(authTokenAccount) && origin.equals(authTokenOrigin)) return authToken;
            clearTokenCache();
        }

        String token = tokenStore.readOrMigrateLegacy(prefs, KEY_AUTH_TOKEN, accountId, origin);
        if (token != null) {
            cacheToken(token, accountId, origin);
            return token;
        }
        return null;
    }

    public synchronized void setAuthToken(@Nullable String token) {
        clearTokenCache();
        if (token == null) {
            if (tokenStore != null) tokenStore.clear();
            if (prefs != null) prefs.edit().remove(KEY_AUTH_TOKEN).commit();
            return;
        }
        if (context == null || tokenStore == null || prefs == null) return;
        String accountId = getActiveUserId();
        if (accountId.startsWith("guest") || !tokenStore.blockLegacyMigration()) return;
        String origin;
        try {
            origin = SessionTokenStore.normalizeOrigin(ApiClient.getBaseUrl(context));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!tokenStore.write(token, accountId, origin)) return;
        if (!prefs.edit().remove(KEY_AUTH_TOKEN).commit()) return;
        if (!tokenStore.unblockLegacyMigration()) return;
        cacheToken(token, accountId, origin);
    }

    private void cacheToken(String token, String accountId, String origin) {
        this.authToken = token;
        this.authTokenAccount = accountId;
        this.authTokenOrigin = origin;
    }

    private void clearTokenCache() {
        this.authToken = null;
        this.authTokenAccount = null;
        this.authTokenOrigin = null;
    }

    public synchronized boolean isGuest() {
        String token = getAuthToken();
        String uid = getActiveUserId();
        return token == null || token.trim().isEmpty() || uid == null || uid.startsWith("guest");
    }

    @NonNull
    public synchronized String getGuestUserId() {
        if (prefs == null) return "guest_default";
        String guestId = prefs.getString(KEY_GUEST_USER_ID, null);
        if (guestId == null || guestId.trim().isEmpty() || "guest".equals(guestId)) {
            guestId = "guest_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            prefs.edit().putString(KEY_GUEST_USER_ID, guestId).apply();
        }
        return guestId;
    }

    /**
     * Explicitly sets the active user ID, synchronizing SharedPreferences, memory cache,
     * and database active state flags.
     *
     * @param id The user ID to activate, or null to clear active user context.
     */
    public synchronized void setActiveUserId(@Nullable String id) {
        long now = System.currentTimeMillis();
        String priorId = getActiveUserId();
        if (id == null || !id.equals(priorId)) {
            clearTokenCache();
            if (tokenStore != null) tokenStore.blockLegacyMigration();
        }
        this.activeUserId = id;

        if (prefs != null) {
            if (id != null) {
                prefs.edit().putString(KEY_ACTIVE_USER_ID, id).apply();
            } else {
                prefs.edit().remove(KEY_ACTIVE_USER_ID).apply();
            }
        }

        if (userDao != null) {
            Runnable updateDb = () -> {
                try {
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
                    currentUserCache = (id != null) ? userDao.getUserById(id) : null;
                } catch (Exception e) {
                    Log.w(TAG, "Error updating active user in DB", e);
                }
            };
            if (Looper.myLooper() != Looper.getMainLooper()) {
                updateDb.run();
            } else {
                new Thread(updateDb).start();
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

        this.currentUserCache = null;
        if (!userId.equals(getActiveUserId())) {
            clearTokenCache();
            if (tokenStore != null) tokenStore.blockLegacyMigration();
        }
        this.activeUserId = userId;
        if (prefs != null) {
            prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply();
        }

        if (userDao != null) {
            Runnable updateDb = () -> {
                try {
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
                                email != null ? email : "", // phone
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
                        if (email != null) {
                            user.setEmail(email);
                            user.setPhone(email);
                        }
                        if (campusId != null) user.setCampusId(campusId);
                        if (role != null) user.setRole(role);
                        user.setActive(true);
                        user.setUpdatedAt(now);
                        userDao.update(user);
                    }
                    currentUserCache = user;
                } catch (Exception e) {
                    Log.w(TAG, "Error persisting login in DB", e);
                }
            };
            if (Looper.myLooper() != Looper.getMainLooper()) {
                updateDb.run();
            } else {
                new Thread(updateDb).start();
            }
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
        String oldToken = getAuthToken();
        if (context != null && oldToken != null) {
            AuthRemoteSource.logout(context, oldToken);
        }

        // Generate a new pristine guest context so future guest activity does not collide with previous member
        String freshGuestId = "guest_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        this.activeUserId = freshGuestId;
        this.currentUserCache = null;
        setAuthToken(null);
        if (prefs != null) {
            prefs.edit()
                    .putString(KEY_GUEST_USER_ID, freshGuestId)
                    .putString(KEY_ACTIVE_USER_ID, freshGuestId)
                    .remove(KEY_AUTH_TOKEN)
                    .apply();
        }
        if (userDao != null) {
            new Thread(() -> {
                try {
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
                } catch (Exception e) {
                    Log.w(TAG, "Error deactivating users on logout", e);
                }
            }).start();
        }
        Log.d(TAG, "Active user logged out. Reverted to fresh isolated guest session: " + freshGuestId);
    }

    /**
     * Returns the currently authenticated {@link UserEntity}.
     *
     * @return Currently active UserEntity, or null if unauthenticated.
     */
    @Nullable
    public synchronized UserEntity getCurrentUser() {
        if (currentUserCache != null && currentUserCache.isActive()
                && currentUserCache.getId().equals(getActiveUserId())) {
            return currentUserCache;
        }
        String currentId = getActiveUserId();
        if (Looper.myLooper() != Looper.getMainLooper()) {
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
        }
        return currentUserCache;
    }

    /**
     * Checks if a user is currently logged in with a real authenticated account.
     *
     * @return true if an active non-guest user token exists, false otherwise.
     */
    public synchronized boolean isLoggedIn() {
        return !isGuest();
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

    /**
     * Migrates local prayers, reflections, and gamification progress from a temporary/guest ID
     * to the authenticated member ID upon login or registration.
     */
    public void migrateLocalData(@NonNull String fromUserId, @NonNull String toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.trim().isEmpty() || toUserId.trim().isEmpty() || fromUserId.equals(toUserId)) {
            return;
        }
        if (context == null) return;
        MIGRATION_EXECUTOR.execute(() -> {
            try {
                PerazimDatabase db = PerazimDatabase.getInstance(context);
                String migrationKey = "migrated_guest_" + fromUserId;
                if (prefs != null) {
                    if (prefs.getBoolean(migrationKey, false)) return;
                    String ownerKey = "guest_migration_owner_" + fromUserId;
                    String claimedOwner = prefs.getString(ownerKey, null);
                    if (claimedOwner != null && !toUserId.equals(claimedOwner)) return;
                    // Persist the sole destination before copying anything; interrupted work can retry only for it.
                    if (claimedOwner == null && !prefs.edit().putString(ownerKey, toUserId).commit()) {
                        throw new IllegalStateException("Could not claim guest migration");
                    }
                }
                db.runInTransaction(() -> {
                    db.prayerDao().migrateUserPrayers(fromUserId, toUserId);
                    db.reflectionDao().migrateUserReflections(fromUserId, toUserId);
                    java.util.List<org.perazimchurch.app.data.local.entity.SyncQueueEntity> drafts = new java.util.ArrayList<>(db.syncQueueDao().getPendingItems());
                    drafts.addAll(db.syncQueueDao().getFailedItems());
                    drafts.addAll(db.syncQueueDao().getSyncingItems());
                    for (org.perazimchurch.app.data.local.entity.SyncQueueEntity item : drafts) {
                        if (fromUserId.equals(org.perazimchurch.app.sync.SyncQueueManager.ownerOf(item))) {
                            try {
                                JSONObject payload = new JSONObject(item.getPayloadJson());
                                payload.put("_sessionOwnerId", toUserId);
                                item.setPayloadJson(payload.toString());
                                db.syncQueueDao().update(item);
                            } catch (org.json.JSONException e) { throw new IllegalStateException(e); }
                        }
                    }
                });
                // Migrate gamification state
                int guestXp = GamificationStore.getSpiritualXp(context, fromUserId, 0);
                if (guestXp > 0) {
                    int existingXp = GamificationStore.getSpiritualXp(context, toUserId, 0);
                    GamificationStore.saveSpiritualXp(context, toUserId, Math.max(guestXp, existingXp));
                }
                int guestStreak = GamificationStore.getStreak(context, fromUserId, 0);
                if (guestStreak > 0) {
                    int existingStreak = GamificationStore.getStreak(context, toUserId, 0);
                    GamificationStore.saveStreak(context, toUserId, Math.max(guestStreak, existingStreak), false);
                }
                // Complete before clearing source metrics; retries use the same claimed destination.
                if (prefs != null && !prefs.edit().putBoolean(migrationKey, true).commit()) {
                    throw new IllegalStateException("Could not persist guest migration claim");
                }
                GamificationStore.saveSpiritualXp(context, fromUserId, 0);
                GamificationStore.saveStreak(context, fromUserId, 0, false);
                Log.i(TAG, "Successfully migrated local data from " + fromUserId + " to " + toUserId);
            } catch (Exception e) {
                Log.e(TAG, "Error migrating local data: " + e.getMessage(), e);
            }
        });
    }

    public interface SessionAuthCallback {
        void onSuccess(@NonNull UserEntity user);
        void onError(@NonNull String errorMessage);
    }

    /**
     * Authenticates with phone number and 4-digit PIN against church server.
     */
    public void loginWithPhoneAndPin(@NonNull String phone,
                                     @NonNull String pin,
                                     @NonNull SessionAuthCallback callback) {
        if (context == null) {
            callback.onError("Context unavailable for network login");
            return;
        }
        final String currentGuest = getGuestUserId();
        AuthRemoteSource.login(context, phone, pin, new AuthRemoteSource.AuthCallback() {
            @Override
            public void onSuccess(@NonNull String token, @NonNull JSONObject userJson) {
                try {
                    String userId = userJson.getString("id");
                    String name = userJson.optString("name", "Member");
                    String campusId = userJson.optString("campus_id", "campus_embu");
                    String role = userJson.optString("role", "MEMBER");
                    int xp = userJson.optInt("xp", 0);
                    int streak = userJson.optInt("streak", 0);
                    if (userJson.has("grace_points")) {
                        GamificationStore.saveGracePoints(context, userId, userJson.getInt("grace_points"));
                    }

                    synchronized (SessionManager.this) {
                        login(userId, name, phone, campusId, role);
                        setAuthToken(token);
                        if (!token.equals(getAuthToken())) {
                            callback.onError("Secure session storage is unavailable. Please try again.");
                            return;
                        }
                    }

                    if (xp > 0) {
                        GamificationStore.saveSpiritualXp(context, userId, Math.max(xp, GamificationStore.getSpiritualXp(context, userId, 0)));
                    }
                    if (streak > 0) {
                        GamificationStore.saveStreak(context, userId, Math.max(streak, GamificationStore.getStreak(context, userId, 0)), false);
                    }

                    migrateLocalData(currentGuest, userId);
                    migrateLocalData("guest", userId);
                    migrateLocalData("user_me", userId);
                    migrateLocalData("guest_user", userId);

                    UserEntity user = getCurrentUser();
                    callback.onSuccess(user != null ? user : new UserEntity(userId, name, phone, phone, role, campusId, "", System.currentTimeMillis(), System.currentTimeMillis(), true));
                } catch (Exception e) {
                    callback.onError("Error parsing user profile: " + e.getMessage());
                }
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Registers a new account with phone number, 4-digit PIN, full name, and campus.
     */
    public void registerWithPhoneAndPin(@NonNull String phone,
                                        @NonNull String pin,
                                        @NonNull String name,
                                        @Nullable String campusId,
                                        @NonNull String privacyNoticeVersion,
                                        @NonNull String privacyNoticeHash,
                                        long privacyNoticePresentedAt,
                                        @NonNull SessionAuthCallback callback) {
        if (context == null) {
            callback.onError("Context unavailable for registration");
            return;
        }
        final String currentGuest = getGuestUserId();
        AuthRemoteSource.register(context, phone, pin, name, campusId, privacyNoticeVersion, privacyNoticeHash, privacyNoticePresentedAt, new AuthRemoteSource.AuthCallback() {
            @Override
            public void onSuccess(@NonNull String token, @NonNull JSONObject userJson) {
                try {
                    String userId = userJson.getString("id");
                    String userName = userJson.optString("name", name);
                    String userCampus = userJson.optString("campus_id", campusId != null ? campusId : "campus_embu");
                    String role = userJson.optString("role", "MEMBER");

                    if (userJson.has("grace_points")) {
                        GamificationStore.saveGracePoints(context, userId, userJson.getInt("grace_points"));
                    }

                    synchronized (SessionManager.this) {
                        login(userId, userName, phone, userCampus, role);
                        setAuthToken(token);
                        if (!token.equals(getAuthToken())) {
                            callback.onError("Secure session storage is unavailable. Please try again.");
                            return;
                        }
                    }

                    migrateLocalData(currentGuest, userId);
                    migrateLocalData("guest", userId);
                    migrateLocalData("user_me", userId);
                    migrateLocalData("guest_user", userId);

                    UserEntity user = getCurrentUser();
                    callback.onSuccess(user != null ? user : new UserEntity(userId, userName, phone, phone, role, userCampus, "", System.currentTimeMillis(), System.currentTimeMillis(), true));
                } catch (Exception e) {
                    callback.onError("Error saving new account: " + e.getMessage());
                }
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}
