package com.example.app.presentation.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.presentation.viewmodel.FellowshipViewModel;
import com.example.app.presentation.viewmodel.ProfileViewModel;
import com.example.app.presentation.viewmodel.ViewModelFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verification helper for Workstreams 2.6, 2.7 & 2.8: Fellowship, Profile & Onboarding UI.
 * <p>
 * Validates:
 * <ul>
 *     <li>{@link OnboardingDialog} preferences flags, reset, and state queries.</li>
 *     <li>{@link FellowshipUiBinder} layout synthesis, prayer wall integration, and interactive components.</li>
 *     <li>{@link ProfileUiBinder} layout synthesis, campus switcher integration, and saved content manager.</li>
 * </ul>
 */
public final class FellowshipProfileUiVerificationHelper {

    private static final String TAG = "PerazimUiGate2678";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private FellowshipProfileUiVerificationHelper() {}

    public static void runVerification(@NonNull Context context) {
        runVerification(context, null);
    }

    public static void runVerification(@NonNull Context context, @Nullable VerificationCallback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                Log.i(TAG, "=================================================================");
                Log.i(TAG, ">>> STARTING PERAZIM FELLOWSHIP, PROFILE & ONBOARDING UI GATE <<<");
                Log.i(TAG, "=================================================================");

                // -----------------------------------------------------------------
                // 1. OnboardingDialog Verification
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 1] Validating OnboardingDialog state lifecycle...");
                OnboardingDialog.reset(appContext);
                boolean shouldShowInitial = OnboardingDialog.shouldShow(appContext);
                if (!shouldShowInitial) {
                    throw new IllegalStateException("OnboardingDialog.shouldShow() should be true after reset");
                }

                OnboardingDialog.setCompleted(appContext, true);
                boolean shouldShowAfterComplete = OnboardingDialog.shouldShow(appContext);
                if (shouldShowAfterComplete) {
                    throw new IllegalStateException("OnboardingDialog.shouldShow() should be false after completion");
                }
                Log.i(TAG, "[UI STEP 1: COMPLETED] OnboardingDialog state verification passed.");

                // -----------------------------------------------------------------
                // 2. FellowshipUiBinder View & Interaction Verification
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 2] Validating FellowshipUiBinder synthesis...");
                ViewModelFactory factory = ViewModelFactory.getInstance(appContext);
                FellowshipViewModel fellowshipVM = factory.createFellowshipViewModel();

                CountDownLatch latchFellowship = new CountDownLatch(1);
                AtomicReference<ScrollView> fellowshipViewRef = new AtomicReference<>();
                AtomicReference<FellowshipUiBinder> fellowshipBinderRef = new AtomicReference<>();

                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    try {
                        FellowshipUiBinder binder = new FellowshipUiBinder(appContext, fellowshipVM);
                        fellowshipBinderRef.set(binder);
                        ScrollView sv = binder.buildView();
                        fellowshipViewRef.set(sv);
                        binder.nextRiddle();
                        binder.nextJoke();
                        binder.refreshPrayers();
                        latchFellowship.countDown();
                    } catch (Throwable t) {
                        Log.e(TAG, "Error in FellowshipUiBinder on main thread", t);
                        latchFellowship.countDown();
                    }
                });

                if (!latchFellowship.await(5, TimeUnit.SECONDS) || fellowshipViewRef.get() == null) {
                    throw new IllegalStateException("FellowshipUiBinder failed to synthesize view hierarchy");
                }
                if (fellowshipViewRef.get().getChildCount() == 0) {
                    throw new IllegalStateException("FellowshipUiBinder ScrollView has no children");
                }
                Log.i(TAG, "[UI STEP 2: COMPLETED] FellowshipUiBinder verified successfully.");

                // -----------------------------------------------------------------
                // 3. ProfileUiBinder View & Interaction Verification
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 3] Validating ProfileUiBinder synthesis...");
                ProfileViewModel profileVM = factory.createProfileViewModel();

                CountDownLatch latchProfile = new CountDownLatch(1);
                AtomicReference<ScrollView> profileViewRef = new AtomicReference<>();
                AtomicReference<ProfileUiBinder> profileBinderRef = new AtomicReference<>();

                mainHandler.post(() -> {
                    try {
                        ProfileUiBinder binder = new ProfileUiBinder(appContext, profileVM);
                        profileBinderRef.set(binder);
                        ScrollView sv = binder.buildView();
                        profileViewRef.set(sv);
                        binder.refreshProfile();
                        binder.refreshSavedContent();
                        latchProfile.countDown();
                    } catch (Throwable t) {
                        Log.e(TAG, "Error in ProfileUiBinder on main thread", t);
                        latchProfile.countDown();
                    }
                });

                if (!latchProfile.await(5, TimeUnit.SECONDS) || profileViewRef.get() == null) {
                    throw new IllegalStateException("ProfileUiBinder failed to synthesize view hierarchy");
                }
                if (profileViewRef.get().getChildCount() == 0) {
                    throw new IllegalStateException("ProfileUiBinder ScrollView has no children");
                }
                Log.i(TAG, "[UI STEP 3: COMPLETED] ProfileUiBinder verified successfully.");

                // -----------------------------------------------------------------
                // SUCCESS
                // -----------------------------------------------------------------
                Log.i(TAG, "=================================================================");
                Log.i(TAG, "[PERAZIM-UI-GATE-2.6-2.7-2.8: SUCCESS] All UI components verified.");
                Log.i(TAG, "=================================================================");

                if (callback != null) {
                    callback.onComplete(true, "Fellowship, Profile and Onboarding UI components verified successfully.");
                }

            } catch (Exception e) {
                Log.e(TAG, "[PERAZIM-UI-GATE-2.6-2.7-2.8: FAILED] " + e.getMessage(), e);
                if (callback != null) {
                    callback.onComplete(false, e.getMessage());
                }
            }
        });
    }
}
