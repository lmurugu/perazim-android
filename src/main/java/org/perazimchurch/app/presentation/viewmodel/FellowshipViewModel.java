package org.perazimchurch.app.presentation.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.perazimchurch.app.data.local.dao.JokeDao;
import org.perazimchurch.app.data.local.dao.RiddleDao;
import org.perazimchurch.app.data.local.entity.JokeEntity;
import org.perazimchurch.app.data.local.entity.RiddleEntity;
import org.perazimchurch.app.domain.model.Connection;
import org.perazimchurch.app.domain.model.Notification;
import org.perazimchurch.app.domain.model.Prayer;
import org.perazimchurch.app.domain.repository.ConnectionRepository;
import org.perazimchurch.app.domain.repository.NotificationRepository;
import org.perazimchurch.app.domain.repository.PrayerRepository;
import org.perazimchurch.app.domain.repository.UserRepository;
import org.perazimchurch.app.presentation.state.UiState;
import org.perazimchurch.app.sync.SyncOperationType;
import org.perazimchurch.app.sync.SyncQueueManager;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel managing community prayers, amen interactions, praise reports/testimonies,
 * fellowship connections, in-app notifications, offline mutation queuing via
 * {@link SyncQueueManager}, and fellowship content (Christian riddles and jokes).
 */
public class FellowshipViewModel extends ViewModel {

    private static final String TAG = "FellowshipViewModel";

    private final PrayerRepository prayerRepo;
    private final ConnectionRepository connectionRepo;
    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final SyncQueueManager syncQueueManager;
    private final RiddleDao riddleDao;
    private final JokeDao jokeDao;
    private final Executor executor;

    private final MutableLiveData<UiState<List<Prayer>>> prayersState = new MutableLiveData<>();

    public FellowshipViewModel(@NonNull PrayerRepository prayerRepo,
                               @NonNull SyncQueueManager syncQueueManager,
                               @NonNull RiddleDao riddleDao,
                               @NonNull JokeDao jokeDao) {
        this(prayerRepo, null, null, null, syncQueueManager, riddleDao, jokeDao, Executors.newSingleThreadExecutor());
    }

    public FellowshipViewModel(@NonNull PrayerRepository prayerRepo,
                               @NonNull SyncQueueManager syncQueueManager,
                               @NonNull RiddleDao riddleDao,
                               @NonNull JokeDao jokeDao,
                               @NonNull Executor executor) {
        this(prayerRepo, null, null, null, syncQueueManager, riddleDao, jokeDao, executor);
    }

    public FellowshipViewModel(@NonNull PrayerRepository prayerRepo,
                               @Nullable ConnectionRepository connectionRepo,
                               @Nullable NotificationRepository notificationRepo,
                               @NonNull SyncQueueManager syncQueueManager,
                               @NonNull RiddleDao riddleDao,
                               @NonNull JokeDao jokeDao) {
        this(prayerRepo, connectionRepo, notificationRepo, null, syncQueueManager, riddleDao, jokeDao, Executors.newSingleThreadExecutor());
    }

    public FellowshipViewModel(@NonNull PrayerRepository prayerRepo,
                               @Nullable ConnectionRepository connectionRepo,
                               @Nullable NotificationRepository notificationRepo,
                               @Nullable UserRepository userRepo,
                               @NonNull SyncQueueManager syncQueueManager,
                               @NonNull RiddleDao riddleDao,
                               @NonNull JokeDao jokeDao) {
        this(prayerRepo, connectionRepo, notificationRepo, userRepo, syncQueueManager, riddleDao, jokeDao, Executors.newSingleThreadExecutor());
    }

    public FellowshipViewModel(@NonNull PrayerRepository prayerRepo,
                               @Nullable ConnectionRepository connectionRepo,
                               @Nullable NotificationRepository notificationRepo,
                               @Nullable UserRepository userRepo,
                               @NonNull SyncQueueManager syncQueueManager,
                               @NonNull RiddleDao riddleDao,
                               @NonNull JokeDao jokeDao,
                               @NonNull Executor executor) {
        this.prayerRepo = prayerRepo;
        this.connectionRepo = connectionRepo;
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.syncQueueManager = syncQueueManager;
        this.riddleDao = riddleDao;
        this.jokeDao = jokeDao;
        this.executor = executor;
    }

    public LiveData<UiState<List<Prayer>>> getPrayersState() {
        return prayersState;
    }

    @Nullable
    public ConnectionRepository getConnectionRepo() {
        return connectionRepo;
    }

    @Nullable
    public NotificationRepository getNotificationRepo() {
        return notificationRepo;
    }

    @Nullable
    public UserRepository getUserRepo() {
        return userRepo;
    }

    @NonNull
    public PrayerRepository getPrayerRepo() {
        return prayerRepo;
    }

    // =========================================================================
    // 1. PRAYERS & PRAISE REPORTS
    // =========================================================================

    /**
     * Loads public prayer petitions from the prayer wall.
     */
    public void loadPublicPrayers(@Nullable Consumer<List<Prayer>> callback) {
        prayersState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                List<Prayer> prayers = prayerRepo.getPublicPrayers();
                if (prayers == null || prayers.isEmpty()) {
                    prayersState.postValue(UiState.empty());
                } else {
                    prayersState.postValue(UiState.success(prayers));
                }
                if (callback != null) {
                    final List<Prayer> res = prayers != null ? prayers : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading public prayers", e);
                prayersState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load prayers"));
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Loads answered prayers, praise reports, and testimonies.
     */
    public void loadAnsweredPrayers(@Nullable Consumer<List<Prayer>> callback) {
        executor.execute(() -> {
            try {
                List<Prayer> answered = prayerRepo.getAnsweredPrayers();
                if (callback != null) {
                    final List<Prayer> res = answered != null ? answered : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading answered prayers", e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Increments amen count and records user amen interaction.
     */
    public void amenPrayer(@NonNull String prayerId, @Nullable String userId, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                prayerRepo.amenPrayer(prayerId, userId != null ? userId : "anonymous");
            } catch (Exception e) {
                Log.e(TAG, "Error amening prayer: " + prayerId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Marks a prayer as answered with a testimony and awards +50 Spiritual XP.
     */
    public void markAnsweredWithTestimony(@NonNull String prayerId,
                                         @Nullable String testimony,
                                         @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                prayerRepo.markAnsweredWithTestimony(prayerId, testimony);
                if (userRepo != null) {
                    userRepo.addSpiritualXp(50);
                }
                if (syncQueueManager != null) {
                    String payloadJson = "{\"prayerId\":\"" + escapeJson(prayerId)
                            + "\",\"testimony\":\"" + escapeJson(testimony != null ? testimony : "")
                            + "\",\"isAnswered\":true}";
                    syncQueueManager.enqueue(SyncOperationType.UPDATE, "PRAYER_TESTIMONY", prayerId, payloadJson, prayerId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking prayer as answered with testimony", e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Submits a prayer petition, saves to database, and enqueues to the sync queue for cloud replication.
     */
    public void submitPrayer(@NonNull String title,
                             @NonNull String content,
                             boolean isAnonymous,
                             @Nullable String userId,
                             @Nullable String authorName,
                             @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                String prayerId = "prayer_" + UUID.randomUUID().toString();
                String effectiveAuthorId = isAnonymous ? "anonymous" : (userId != null && !userId.trim().isEmpty() ? userId : "guest");
                String effectiveAuthorName = isAnonymous ? "Anonymous Saint" : (authorName != null && !authorName.trim().isEmpty() ? authorName : "Fellow Saint");
                long now = System.currentTimeMillis();

                Prayer prayer = new Prayer(
                        prayerId,
                        title,
                        content,
                        effectiveAuthorId,
                        effectiveAuthorName,
                        Prayer.Visibility.PUBLIC,
                        0,
                        now,
                        false,
                        false
                );
                prayerRepo.submitPrayer(prayer);

                if (syncQueueManager != null) {
                    String payloadJson = "{\"title\":\"" + escapeJson(title)
                            + "\",\"content\":\"" + escapeJson(content)
                            + "\",\"isAnonymous\":" + isAnonymous + "}";

                    syncQueueManager.enqueue(SyncOperationType.CREATE, "PRAYER", prayerId, payloadJson, prayerId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error submitting prayer", e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    // =========================================================================
    // 2. MEMBER CONNECTIONS
    // =========================================================================

    /**
     * Loads active connections for a given user.
     */
    public void loadConnections(@NonNull String userId, @Nullable Consumer<List<Connection>> callback) {
        executor.execute(() -> {
            try {
                List<Connection> connections = connectionRepo != null
                        ? connectionRepo.getConnections(userId)
                        : Collections.emptyList();
                if (callback != null) {
                    final List<Connection> res = connections != null ? connections : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading connections for user " + userId, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Loads pending connection requests for a given user.
     */
    public void loadPendingRequests(@NonNull String userId, @Nullable Consumer<List<Connection>> callback) {
        executor.execute(() -> {
            try {
                List<Connection> pending = connectionRepo != null
                        ? connectionRepo.getPendingRequests(userId)
                        : Collections.emptyList();
                if (callback != null) {
                    final List<Connection> res = pending != null ? pending : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading pending connection requests for user " + userId, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Sends a connection request to a peer member.
     */
    public void sendConnectionRequest(@NonNull String userId,
                                      @NonNull String peerId,
                                      @Nullable String peerName,
                                      @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                if (connectionRepo != null) {
                    connectionRepo.sendConnectionRequest(userId, peerId, peerName != null ? peerName : peerId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending connection request from " + userId + " to " + peerId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Accepts a pending connection request.
     */
    public void acceptConnection(@NonNull String connectionId, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                if (connectionRepo != null) {
                    connectionRepo.acceptConnection(connectionId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error accepting connection " + connectionId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    // =========================================================================
    // 3. IN-APP NOTIFICATIONS
    // =========================================================================

    /**
     * Loads all notifications for the specified user.
     */
    public void loadNotifications(@NonNull String userId, @Nullable Consumer<List<Notification>> callback) {
        executor.execute(() -> {
            try {
                List<Notification> notifications = notificationRepo != null
                        ? notificationRepo.getNotifications(userId)
                        : Collections.emptyList();
                if (callback != null) {
                    final List<Notification> res = notifications != null ? notifications : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading notifications for user " + userId, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Retrieves the unread notification count for the specified user.
     */
    public void getUnreadNotificationCount(@NonNull String userId, @Nullable Consumer<Integer> callback) {
        executor.execute(() -> {
            try {
                int count = notificationRepo != null ? notificationRepo.getUnreadCount(userId) : 0;
                if (callback != null) {
                    postCallback(() -> callback.accept(count));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading unread notification count for user " + userId, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(0));
                }
            }
        });
    }

    /**
     * Marks an individual notification as read.
     */
    public void markNotificationAsRead(@NonNull String notificationId, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                if (notificationRepo != null) {
                    notificationRepo.markAsRead(notificationId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking notification as read: " + notificationId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Marks all notifications as read for a given user.
     */
    public void markAllNotificationsAsRead(@NonNull String userId, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                if (notificationRepo != null) {
                    notificationRepo.markAllAsRead(userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking all notifications as read for " + userId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    // =========================================================================
    // 4. RIDDLES & JOKES
    // =========================================================================

    /**
     * Loads Christian riddles for fellowship engagement.
     */
    public void loadRiddles(@Nullable Consumer<List<RiddleEntity>> callback) {
        executor.execute(() -> {
            try {
                List<RiddleEntity> riddles = riddleDao.getAllRiddles();
                if (callback != null) {
                    final List<RiddleEntity> res = riddles != null ? riddles : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading riddles", e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Loads wholesome Christian jokes for youth & fellowship engagement.
     */
    public void loadJokes(@Nullable Consumer<List<JokeEntity>> callback) {
        executor.execute(() -> {
            try {
                List<JokeEntity> jokes = jokeDao.getAllJokes();
                if (callback != null) {
                    final List<JokeEntity> res = jokes != null ? jokes : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading jokes", e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void postCallback(Runnable runnable) {
        if (runnable == null) return;
        try {
            if (Looper.getMainLooper() != null && Looper.myLooper() != Looper.getMainLooper()) {
                new Handler(Looper.getMainLooper()).post(runnable);
                return;
            }
        } catch (Throwable ignored) {
        }
        runnable.run();
    }
}
