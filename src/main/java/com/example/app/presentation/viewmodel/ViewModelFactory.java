package com.example.app.presentation.viewmodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.dao.JokeDao;
import com.example.app.data.local.dao.RiddleDao;
import com.example.app.data.local.session.SessionManager;
import com.example.app.data.repository.RepositoryProvider;
import com.example.app.domain.repository.DownloadedContentRepository;
import com.example.app.media.download.DownloadManager;
import com.example.app.sync.SyncQueueManager;

/**
 * Factory class connecting strictly to {@link RepositoryProvider}, {@link DownloadManager},
 * {@link SyncQueueManager}, and {@link SessionManager} to instantiate all application ViewModels.
 * Implements standard Jetpack {@link ViewModelProvider.Factory}.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private static volatile ViewModelFactory INSTANCE;

    private final RepositoryProvider repositoryProvider;
    private final DownloadManager downloadManager;
    private final SyncQueueManager syncQueueManager;
    private final SessionManager sessionManager;
    private final RiddleDao riddleDao;
    private final JokeDao jokeDao;
    private final DownloadedContentRepository downloadedContentRepository;

    public ViewModelFactory(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        PerazimDatabase db = PerazimDatabase.getInstance(appContext);
        this.repositoryProvider = RepositoryProvider.getInstance(appContext);
        this.downloadManager = new DownloadManager(appContext);
        this.syncQueueManager = SyncQueueManager.getInstance(appContext);
        this.sessionManager = SessionManager.getInstance(appContext);
        this.riddleDao = db.riddleDao();
        this.jokeDao = db.jokeDao();
        this.downloadedContentRepository = this.repositoryProvider.getDownloadedContentRepository();
    }

    public ViewModelFactory(@NonNull RepositoryProvider repositoryProvider,
                            @NonNull DownloadManager downloadManager,
                            @NonNull SyncQueueManager syncQueueManager,
                            @NonNull SessionManager sessionManager,
                            @NonNull RiddleDao riddleDao,
                            @NonNull JokeDao jokeDao,
                            @NonNull DownloadedContentRepository downloadedContentRepository) {
        this.repositoryProvider = repositoryProvider;
        this.downloadManager = downloadManager;
        this.syncQueueManager = syncQueueManager;
        this.sessionManager = sessionManager;
        this.riddleDao = riddleDao;
        this.jokeDao = jokeDao;
        this.downloadedContentRepository = downloadedContentRepository;
    }

    public static ViewModelFactory getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ViewModelFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ViewModelFactory(context);
                }
            }
        }
        return INSTANCE;
    }

    public static synchronized void resetInstance() {
        INSTANCE = null;
    }

    public HomeViewModel createHomeViewModel() {
        return new HomeViewModel(
                repositoryProvider.getSermonRepository(),
                repositoryProvider.getBibleRepository(),
                repositoryProvider.getReflectionRepository(),
                repositoryProvider.getAnnouncementRepository(),
                repositoryProvider.getEventRepository(),
                repositoryProvider.getUserRepository()
        );
    }

    public BibleViewModel createBibleViewModel() {
        return new BibleViewModel(repositoryProvider.getBibleRepository());
    }

    public SermonsViewModel createSermonsViewModel() {
        return new SermonsViewModel(
                repositoryProvider.getSermonRepository(),
                downloadManager
        );
    }

    public WorshipViewModel createWorshipViewModel() {
        return new WorshipViewModel(repositoryProvider.getHymnRepository());
    }

    public FellowshipViewModel createFellowshipViewModel() {
        return new FellowshipViewModel(
                repositoryProvider.getPrayerRepository(),
                syncQueueManager,
                riddleDao,
                jokeDao
        );
    }

    public ProfileViewModel createProfileViewModel() {
        return new ProfileViewModel(
                repositoryProvider.getUserRepository(),
                downloadedContentRepository,
                repositoryProvider.getBibleRepository(),
                repositoryProvider.getHymnRepository(),
                sessionManager
        );
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) createHomeViewModel();
        } else if (modelClass.isAssignableFrom(BibleViewModel.class)) {
            return (T) createBibleViewModel();
        } else if (modelClass.isAssignableFrom(SermonsViewModel.class)) {
            return (T) createSermonsViewModel();
        } else if (modelClass.isAssignableFrom(WorshipViewModel.class)) {
            return (T) createWorshipViewModel();
        } else if (modelClass.isAssignableFrom(FellowshipViewModel.class)) {
            return (T) createFellowshipViewModel();
        } else if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) createProfileViewModel();
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
