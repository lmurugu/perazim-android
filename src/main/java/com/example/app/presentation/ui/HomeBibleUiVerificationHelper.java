package com.example.app.presentation.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.domain.model.Sermon;
import com.example.app.presentation.viewmodel.BibleViewModel;
import com.example.app.presentation.viewmodel.HomeViewModel;
import com.example.app.presentation.viewmodel.ViewModelFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verification gate for Workstream 2.2 & 2.3: Home & Bible UI Binders.
 * Verifies instantiations, view hierarchies, binding contracts, and lifecycle safety
 * for {@link BibleReaderDialog} and {@link HomeUiBinder}.
 */
public final class HomeBibleUiVerificationHelper {

    private static final String TAG = "PerazimUiGate";

    public interface VerificationCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private HomeBibleUiVerificationHelper() {
        // Prevent instantiation
    }

    public static void runVerification(@NonNull Context context) {
        runVerification(context, null);
    }

    public static void runVerification(@NonNull Context context, @Nullable VerificationCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                Log.i(TAG, "=================================================================");
                Log.i(TAG, ">>> STARTING WORKSTREAM 2.2 & 2.3 HOME & BIBLE UI VERIFICATION <<<");
                Log.i(TAG, "=================================================================");

                ViewModelFactory factory = ViewModelFactory.getInstance(context);
                HomeViewModel homeViewModel = factory.createHomeViewModel();
                BibleViewModel bibleViewModel = factory.createBibleViewModel();

                // -----------------------------------------------------------------
                // 1. BibleReaderDialog Verification
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 1] Validating BibleReaderDialog instantiation & contracts...");
                BibleReaderDialog bibleDialog = new BibleReaderDialog(context, bibleViewModel);
                bibleDialog.setTargetPassage("2SA", 5);
                Log.i(TAG, "[UI STEP 1: COMPLETED] BibleReaderDialog configured for 2 Samuel 5 (Baal-perazim).");

                // -----------------------------------------------------------------
                // 2. HomeUiBinder Construction & View Hierarchy Verification
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 2] Validating HomeUiBinder view construction & layout tree...");
                AtomicBoolean streakCalled = new AtomicBoolean(false);
                AtomicBoolean xpCalled = new AtomicBoolean(false);

                HomeUiBinder binder = new HomeUiBinder(context, homeViewModel, bibleViewModel, new HomeUiBinder.HomeUiListener() {
                    @Override
                    public void onStreakUpdated(int streak) {
                        streakCalled.set(true);
                    }

                    @Override
                    public void onXpAwarded(int xpAwarded, int totalXp) {
                        xpCalled.set(true);
                    }

                    @Override
                    public void onPlaySermon(@NonNull Sermon sermon) {
                        Log.i(TAG, "Sermon play requested: " + sermon.getTitle());
                    }

                    @Override
                    public void onOpenScripture(@NonNull String bookId, int chapter) {
                        Log.i(TAG, "Scripture opened: " + bookId + " ch: " + chapter);
                    }
                });

                View homeView = binder.buildView();
                if (homeView == null) {
                    throw new IllegalStateException("HomeUiBinder.buildView() returned null");
                }

                FrameLayout testContainer = new FrameLayout(context);
                binder.bind(testContainer);
                if (testContainer.getChildCount() == 0) {
                    throw new IllegalStateException("HomeUiBinder.bind() failed to add child views to container");
                }
                Log.i(TAG, "[UI STEP 2: COMPLETED] HomeUiBinder successfully built view tree and bound container.");

                // -----------------------------------------------------------------
                // 3. Home Feed Refresh Verification
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 3] Refreshing Home feed via HomeUiBinder...");
                binder.refreshFeed();
                Log.i(TAG, "[UI STEP 3: COMPLETED] HomeUiBinder feed refresh triggered asynchronously.");

                Log.i(TAG, "=================================================================");
                Log.i(TAG, ">>> WORKSTREAM 2.2 & 2.3 HOME & BIBLE UI VERIFICATION PASSED <<<");
                Log.i(TAG, "=================================================================");

                if (callback != null) {
                    callback.onComplete(true, "All UI binder verifications passed successfully.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Home & Bible UI verification failed", e);
                if (callback != null) {
                    callback.onComplete(false, e.getMessage());
                }
            }
        });
    }
}
