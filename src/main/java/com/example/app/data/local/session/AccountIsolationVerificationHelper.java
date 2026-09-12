package com.example.app.data.local.session;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.entity.PrayerEntity;
import com.example.app.data.local.entity.ReflectionEntity;
import com.example.app.data.local.entity.UserEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Self-contained verification helper for Phase 1.7 Account Isolation Gate.
 * Runs a strict multi-user account isolation verification on {@link PerazimDatabase}:
 * <ol>
 *   <li>Login as User A (<code>user_alpha</code>, "Alpha Disciple").</li>
 *   <li>Insert private prayer under User A ("Alpha Private Prayer").</li>
 *   <li>Insert private devotional reflection under User A ("Alpha Quiet Time").</li>
 *   <li>Switch / Login as User B (<code>user_bravo</code>, "Bravo Covenant Partner").</li>
 *   <li>Query <code>prayerDao().getPrayersByUser("user_bravo")</code> -> ASSERT EMPTY (0 prayers).</li>
 *   <li>Query <code>reflectionDao().getReflectionsByUser("user_bravo")</code> -> ASSERT EMPTY (0 reflections).</li>
 *   <li>Verify that User B CANNOT see User A's private prayers or reflections.</li>
 *   <li>Switch / Login back as User A (<code>user_alpha</code>).</li>
 *   <li>Query <code>prayerDao().getPrayersByUser("user_alpha")</code> -> ASSERT returns User A's private prayer.</li>
 *   <li>Query <code>reflectionDao().getReflectionsByUser("user_alpha")</code> -> ASSERT returns User A's private reflection.</li>
 *   <li>Clean up test records and restore previous active user.</li>
 *   <li>Log: <code>android.util.Log.i("PerazimAccountIsolation", "[PERAZIM-ACCOUNT-ISOLATION-GATE-1.7: SUCCESS]");</code></li>
 * </ol>
 */
public class AccountIsolationVerificationHelper {

    private static final String TAG = "PerazimAccountIsolation";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onVerificationComplete(boolean success, String message);
    }

    /**
     * Executes the Phase 1.7 Account Isolation verification asynchronously on a background thread.
     *
     * @param context Application context.
     */
    public static void runVerification(@NonNull Context context) {
        runVerification(context, (VerificationCallback) null);
    }

    public static void runVerification(@NonNull Context context, @NonNull PerazimDatabase db) {
        SessionManager sessionManager = SessionManager.getInstance(context, db.userDao());
        verifyInternal(context, db, sessionManager);
    }

    /**
     * Executes the Phase 1.7 Account Isolation verification asynchronously with callback.
     *
     * @param context  Application context.
     * @param callback Callback notifying pass/fail status and message.
     */
    public static void runVerification(@NonNull Context context, @Nullable VerificationCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                Log.d(TAG, "Starting Phase 1.7 Account Isolation Verification Gate...");
                PerazimDatabase db = PerazimDatabase.getInstance(context);
                SessionManager sessionManager = SessionManager.getInstance(context, db.userDao());

                verifyInternal(context, db, sessionManager);

                if (callback != null) {
                    callback.onVerificationComplete(true, "Phase 1.7 Account Isolation verified successfully.");
                }
            } catch (Exception e) {
                Log.e(TAG, "[PERAZIM-ACCOUNT-ISOLATION-GATE-1.7: FAILED] " + e.getMessage(), e);
                if (callback != null) {
                    callback.onVerificationComplete(false, e.getMessage());
                }
            }
        });
    }

    /**
     * Convenience method to run verification synchronously with database and session manager instances.
     *
     * @param db             The PerazimDatabase instance under test.
     * @param sessionManager The SessionManager instance under test.
     * @return true if all isolation assertions pass.
     */
    public static boolean verifyInternal(@NonNull PerazimDatabase db, @NonNull SessionManager sessionManager) {
        return verifyInternal(null, db, sessionManager);
    }

    /**
     * Executes the strict multi-user account isolation verification suite synchronously.
     *
     * @param context        Android context (optional).
     * @param db             The PerazimDatabase instance under test.
     * @param sessionManager The SessionManager instance under test.
     * @return true if all isolation assertions pass.
     * @throws IllegalStateException if any assertion or cleanup check fails.
     */
    public static boolean verifyInternal(@Nullable Context context,
                                         @NonNull PerazimDatabase db,
                                         @NonNull SessionManager sessionManager) {
        long timestamp = System.currentTimeMillis();
        String testSuffix = "_" + timestamp;

        String userAId = "user_alpha";
        String userBId = "user_bravo";
        String prayerAId = "prayer_alpha" + testSuffix;
        String reflectionAId = "reflection_alpha" + testSuffix;

        // Capture previous active user context to restore at Step K
        String previousActiveUserId = sessionManager.getActiveUserId();
        UserEntity previousActiveUser = (previousActiveUserId != null && db.userDao() != null)
                ? db.userDao().getUserById(previousActiveUserId)
                : null;

        try {
            // Pre-clean in case of prior interrupted execution
            db.prayerDao().deleteById(prayerAId);
            db.reflectionDao().deleteById(reflectionAId);
            db.userDao().deleteById(userAId);
            db.userDao().deleteById(userBId);

            // -----------------------------------------------------------------
            // a) Login as User A ("user_alpha", "Alpha Disciple")
            // -----------------------------------------------------------------
            sessionManager.login(
                    userAId,
                    "Alpha Disciple",
                    "alpha.disciple@perazim.org",
                    "campus_central",
                    "DISCIPLE"
            );

            if (!userAId.equals(sessionManager.getActiveUserId())) {
                throw new IllegalStateException("Step A failed: Expected active user ID " + userAId
                        + ", got " + sessionManager.getActiveUserId());
            }

            UserEntity userA = sessionManager.getCurrentUser();
            if (userA == null || !userAId.equals(userA.getId())) {
                throw new IllegalStateException("Step A failed: User A entity not found or ID mismatch");
            }
            if (!"Alpha Disciple".equals(userA.getName())) {
                throw new IllegalStateException("Step A failed: Expected name 'Alpha Disciple', got " + userA.getName());
            }
            if (!userA.isActive()) {
                throw new IllegalStateException("Step A failed: User A entity is not marked active in database");
            }
            Log.d(TAG, "Step A passed: Logged in as User A (" + userAId + ").");

            // -----------------------------------------------------------------
            // b) Insert private prayer under User A ("Alpha Private Prayer")
            // -----------------------------------------------------------------
            PrayerEntity prayerA = new PrayerEntity(
                    prayerAId,
                    userAId,
                    "Alpha Disciple",
                    "Alpha Private Prayer",
                    "Lord, grant divine wisdom, protection, and breakthrough in our discipleship journey.",
                    false, // isAnonymous
                    false, // isAnswered
                    0,     // prayerCount
                    timestamp
            );
            db.prayerDao().insert(prayerA);

            PrayerEntity insertedPrayer = db.prayerDao().getPrayerById(prayerAId);
            if (insertedPrayer == null) {
                throw new IllegalStateException("Step B failed: Could not persist private prayer for User A.");
            }
            Log.d(TAG, "Step B passed: Inserted private prayer for User A (id=" + prayerAId + ").");

            // -----------------------------------------------------------------
            // c) Insert private devotional reflection under User A ("Alpha Quiet Time")
            // -----------------------------------------------------------------
            ReflectionEntity reflectionA = new ReflectionEntity(
                    reflectionAId,
                    userAId,
                    "Alpha Quiet Time",
                    "Quiet time devotional meditation on covenant promises and God's supernatural peace.",
                    "Proverbs 3:5-6",
                    timestamp,
                    timestamp,
                    true // isPrivate
            );
            db.reflectionDao().insert(reflectionA);

            ReflectionEntity insertedReflection = db.reflectionDao().getReflectionById(reflectionAId);
            if (insertedReflection == null) {
                throw new IllegalStateException("Step C failed: Could not persist private reflection for User A.");
            }
            Log.d(TAG, "Step C passed: Inserted private reflection for User A (id=" + reflectionAId + ").");

            // -----------------------------------------------------------------
            // d) Switch / Login as User B ("user_bravo", "Bravo Covenant Partner")
            // -----------------------------------------------------------------
            sessionManager.login(
                    userBId,
                    "Bravo Covenant Partner",
                    "bravo.partner@perazim.org",
                    "campus_north",
                    "PARTNER"
            );

            if (!userBId.equals(sessionManager.getActiveUserId())) {
                throw new IllegalStateException("Step D failed: Expected active user ID " + userBId
                        + ", got " + sessionManager.getActiveUserId());
            }

            UserEntity userB = sessionManager.getCurrentUser();
            if (userB == null || !userBId.equals(userB.getId())) {
                throw new IllegalStateException("Step D failed: User B entity not found or ID mismatch");
            }
            if (!"Bravo Covenant Partner".equals(userB.getName())) {
                throw new IllegalStateException("Step D failed: Expected name 'Bravo Covenant Partner', got " + userB.getName());
            }
            if (!userB.isActive()) {
                throw new IllegalStateException("Step D failed: User B entity is not marked active in database");
            }

            // Verify User A was deactivated on switch
            UserEntity checkUserA = db.userDao().getUserById(userAId);
            if (checkUserA != null && checkUserA.isActive()) {
                throw new IllegalStateException("Step D failed: User A remained active after User B login");
            }
            Log.d(TAG, "Step D passed: Switched/Logged in as User B (" + userBId + ").");

            // -----------------------------------------------------------------
            // e) Query prayerDao().getPrayersByUser("user_bravo") -> ASSERT EMPTY (0 prayers)
            // -----------------------------------------------------------------
            List<PrayerEntity> prayersBravo = db.prayerDao().getPrayersByUser(userBId);
            if (prayersBravo == null || !prayersBravo.isEmpty()) {
                throw new IllegalStateException("Step E failed: Expected 0 prayers for User B (" + userBId
                        + "), found " + (prayersBravo == null ? "null" : prayersBravo.size()));
            }
            Log.d(TAG, "Step E passed: User B has 0 prayers as expected.");

            // -----------------------------------------------------------------
            // f) Query reflectionDao().getReflectionsByUser("user_bravo") -> ASSERT EMPTY (0 reflections)
            // -----------------------------------------------------------------
            List<ReflectionEntity> reflectionsBravo = db.reflectionDao().getReflectionsByUser(userBId);
            if (reflectionsBravo == null || !reflectionsBravo.isEmpty()) {
                throw new IllegalStateException("Step F failed: Expected 0 reflections for User B (" + userBId
                        + "), found " + (reflectionsBravo == null ? "null" : reflectionsBravo.size()));
            }
            Log.d(TAG, "Step F passed: User B has 0 reflections as expected.");

            // -----------------------------------------------------------------
            // g) Verify that User B CANNOT see User A's private prayers or reflections
            // -----------------------------------------------------------------
            // Check 1: Scoped queries via active session user ID return nothing from User A
            String activeUserId = sessionManager.getActiveUserId();
            List<PrayerEntity> activeSessionPrayers = db.prayerDao().getPrayersByUser(activeUserId);
            for (PrayerEntity p : activeSessionPrayers) {
                if (userAId.equals(p.getUserId()) || prayerAId.equals(p.getId())) {
                    throw new IllegalStateException("Step G failed: Active session (User B) exposed User A's prayer " + p.getId());
                }
            }

            List<ReflectionEntity> activeSessionReflections = db.reflectionDao().getReflectionsByUser(activeUserId);
            for (ReflectionEntity r : activeSessionReflections) {
                if (userAId.equals(r.getUserId()) || reflectionAId.equals(r.getId())) {
                    throw new IllegalStateException("Step G failed: Active session (User B) exposed User A's reflection " + r.getId());
                }
            }

            // Check 2: Direct query by User B's identifier contains no leaks of User A's private content
            for (PrayerEntity p : prayersBravo) {
                if (userAId.equals(p.getUserId()) || prayerAId.equals(p.getId())) {
                    throw new IllegalStateException("Step G failed: Isolation breach: User B query returned User A's prayer " + p.getId());
                }
            }

            for (ReflectionEntity r : reflectionsBravo) {
                if (userAId.equals(r.getUserId()) || reflectionAId.equals(r.getId())) {
                    throw new IllegalStateException("Step G failed: Isolation breach: User B query returned User A's reflection " + r.getId());
                }
            }
            Log.d(TAG, "Step G passed: Verified User B CANNOT see User A's private prayers or reflections.");

            // -----------------------------------------------------------------
            // h) Switch / Login back as User A ("user_alpha")
            // -----------------------------------------------------------------
            sessionManager.login(
                    userAId,
                    "Alpha Disciple",
                    "alpha.disciple@perazim.org",
                    "campus_central",
                    "DISCIPLE"
            );

            if (!userAId.equals(sessionManager.getActiveUserId())) {
                throw new IllegalStateException("Step H failed: Expected active user ID " + userAId
                        + ", got " + sessionManager.getActiveUserId());
            }

            // Verify User B was deactivated on switch
            UserEntity checkUserB = db.userDao().getUserById(userBId);
            if (checkUserB != null && checkUserB.isActive()) {
                throw new IllegalStateException("Step H failed: User B remained active after User A login");
            }
            Log.d(TAG, "Step H passed: Switched/Logged in back as User A (" + userAId + ").");

            // -----------------------------------------------------------------
            // i) Query prayerDao().getPrayersByUser("user_alpha") -> ASSERT returns User A's private prayer
            // -----------------------------------------------------------------
            List<PrayerEntity> prayersAlpha = db.prayerDao().getPrayersByUser(userAId);
            if (prayersAlpha == null || prayersAlpha.isEmpty()) {
                throw new IllegalStateException("Step I failed: Expected User A's prayers, but got empty list or null");
            }

            boolean foundPrayerA = false;
            for (PrayerEntity p : prayersAlpha) {
                if (prayerAId.equals(p.getId())) {
                    if (!"Alpha Private Prayer".equals(p.getTitle())) {
                        throw new IllegalStateException("Step I failed: Prayer title mismatch. Expected 'Alpha Private Prayer', got '" + p.getTitle() + "'");
                    }
                    if (!userAId.equals(p.getUserId())) {
                        throw new IllegalStateException("Step I failed: Prayer owner mismatch. Expected '" + userAId + "', got '" + p.getUserId() + "'");
                    }
                    foundPrayerA = true;
                    break;
                }
            }
            if (!foundPrayerA) {
                throw new IllegalStateException("Step I failed: Did not find User A's private prayer with id " + prayerAId);
            }
            Log.d(TAG, "Step I passed: User A's private prayer successfully retrieved.");

            // -----------------------------------------------------------------
            // j) Query reflectionDao().getReflectionsByUser("user_alpha") -> ASSERT returns User A's private reflection
            // -----------------------------------------------------------------
            List<ReflectionEntity> reflectionsAlpha = db.reflectionDao().getReflectionsByUser(userAId);
            if (reflectionsAlpha == null || reflectionsAlpha.isEmpty()) {
                throw new IllegalStateException("Step J failed: Expected User A's reflections, but got empty list or null");
            }

            boolean foundReflectionA = false;
            for (ReflectionEntity r : reflectionsAlpha) {
                if (reflectionAId.equals(r.getId())) {
                    if (!"Alpha Quiet Time".equals(r.getTitle())) {
                        throw new IllegalStateException("Step J failed: Reflection title mismatch. Expected 'Alpha Quiet Time', got '" + r.getTitle() + "'");
                    }
                    if (!userAId.equals(r.getUserId())) {
                        throw new IllegalStateException("Step J failed: Reflection owner mismatch. Expected '" + userAId + "', got '" + r.getUserId() + "'");
                    }
                    if (!r.isPrivate()) {
                        throw new IllegalStateException("Step J failed: Expected reflection to be marked private");
                    }
                    foundReflectionA = true;
                    break;
                }
            }
            if (!foundReflectionA) {
                throw new IllegalStateException("Step J failed: Did not find User A's private reflection with id " + reflectionAId);
            }
            Log.d(TAG, "Step J passed: User A's private reflection successfully retrieved.");

            // -----------------------------------------------------------------
            // k) Clean up test records and restore previous active user
            // -----------------------------------------------------------------
            db.prayerDao().deleteById(prayerAId);
            db.reflectionDao().deleteById(reflectionAId);
            db.userDao().deleteById(userAId);
            db.userDao().deleteById(userBId);

            // Verify cleanup
            if (db.prayerDao().getPrayerById(prayerAId) != null) {
                throw new IllegalStateException("Step K cleanup failed: Test prayer still exists in database");
            }
            if (db.reflectionDao().getReflectionById(reflectionAId) != null) {
                throw new IllegalStateException("Step K cleanup failed: Test reflection still exists in database");
            }
            if (db.userDao().getUserById(userAId) != null) {
                throw new IllegalStateException("Step K cleanup failed: Test user_alpha still exists in database");
            }
            if (db.userDao().getUserById(userBId) != null) {
                throw new IllegalStateException("Step K cleanup failed: Test user_bravo still exists in database");
            }

            // Restore previous active user context
            if (previousActiveUserId != null) {
                sessionManager.setActiveUserId(previousActiveUserId);
                if (previousActiveUser != null) {
                    previousActiveUser.setActive(true);
                    db.userDao().update(previousActiveUser);
                }
            } else {
                sessionManager.logout();
            }
            Log.d(TAG, "Step K passed: Test records cleaned up and previous active user restored.");

            // -----------------------------------------------------------------
            // l) Log: android.util.Log.i("PerazimAccountIsolation", "[PERAZIM-ACCOUNT-ISOLATION-GATE-1.7: SUCCESS]");
            // -----------------------------------------------------------------
            Log.i("PerazimAccountIsolation", "[PERAZIM-ACCOUNT-ISOLATION-GATE-1.7: SUCCESS]");
            return true;

        } catch (Exception e) {
            // Emergency cleanup on exception
            try {
                db.prayerDao().deleteById(prayerAId);
                db.reflectionDao().deleteById(reflectionAId);
                db.userDao().deleteById(userAId);
                db.userDao().deleteById(userBId);
                if (previousActiveUserId != null) {
                    sessionManager.setActiveUserId(previousActiveUserId);
                    if (previousActiveUser != null) {
                        previousActiveUser.setActive(true);
                        db.userDao().update(previousActiveUser);
                    }
                } else {
                    sessionManager.logout();
                }
            } catch (Exception ignored) {
                // Suppress secondary cleanup error to expose root cause
            }
            throw e;
        }
    }
}
