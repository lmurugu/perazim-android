package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Welcoming first-launch Onboarding Dialog for Perazim Mission Church.
 * <p>
 * Introduces the church vision ("The Place of Great Breakthrough", 2 Samuel 5:20)
 * and explains the offline-first spiritual architecture (Bible, Hymns, Devotionals,
 * and Sermons accessible anytime, anywhere with zero internet).
 * <p>
 * Clicking "Get Started" dismisses the dialog and persists completion in {@link SharedPreferences}.
 */
public class OnboardingDialog extends Dialog {

    public static final String PREFS_NAME = "perazim_onboarding_prefs";
    public static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private final Runnable onGetStartedListener;

    public OnboardingDialog(@NonNull Context context) {
        this(context, null);
    }

    public OnboardingDialog(@NonNull Context context, @Nullable Runnable onGetStartedListener) {
        super(context);
        this.onGetStartedListener = onGetStartedListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        View contentView = buildDialogView();
        setContentView(contentView);
    }

    /**
     * Constructs the onboarding card dialog layout.
     */
    private View buildDialogView() {
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Outer Card Container with rounded corners and brand border
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(22), dp(20), dp(22));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(PerazimTheme.COLOR_WHITE);
        cardBg.setCornerRadius(dp(PerazimTheme.RADIUS_XL));
        cardBg.setStroke(dp(2), PerazimTheme.COLOR_BORDER_GREY);
        card.setBackground(cardBg);

        // 1. HEADER BRAND BADGE
        LinearLayout headerBox = new LinearLayout(getContext());
        headerBox.setOrientation(LinearLayout.VERTICAL);
        headerBox.setGravity(Gravity.CENTER_HORIZONTAL);
        headerBox.setPadding(0, 0, 0, dp(14));

        TextView tvBadge = new TextView(getContext());
        tvBadge.setText("🏛️ PERAZIM MISSION CHURCH");
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvBadge.setBackground(createPillDrawable(PerazimTheme.COLOR_PURPLE_TINT, PerazimTheme.COLOR_BORDER_GREY));
        tvBadge.setPadding(dp(12), dp(5), dp(12), dp(5));
        tvBadge.setGravity(Gravity.CENTER);
        headerBox.addView(tvBadge);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("The Place of Great Breakthrough");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, dp(10), 0, dp(4));
        headerBox.addView(tvTitle);

        TextView tvScriptureRef = new TextView(getContext());
        tvScriptureRef.setText("“The LORD hath broken forth upon mine enemies before me, as the breach of waters.” — 2 Samuel 5:20");
        tvScriptureRef.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvScriptureRef.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvScriptureRef.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvScriptureRef.setGravity(Gravity.CENTER);
        tvScriptureRef.setLineSpacing(dp(2), 1.15f);
        tvScriptureRef.setPadding(dp(4), 0, dp(4), dp(6));
        headerBox.addView(tvScriptureRef);

        TextView tvSlogan = new TextView(getContext());
        tvSlogan.setText("“A place where everybody is somebody, and nobody is a nobody.”");
        tvSlogan.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvSlogan.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvSlogan.setGravity(Gravity.CENTER);
        headerBox.addView(tvSlogan);

        card.addView(headerBox);

        // Divider
        View divider = new View(getContext());
        divider.setBackgroundColor(PerazimTheme.COLOR_BORDER_GREY);
        LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lpDiv.setMargins(0, 0, 0, dp(16));
        divider.setLayoutParams(lpDiv);
        card.addView(divider);

        // 2. OFFLINE-FIRST PILLARS SECTION
        TextView tvSectionLabel = new TextView(getContext());
        tvSectionLabel.setText("⚡ 100% OFFLINE-FIRST SPIRITUAL FOUNDATION");
        tvSectionLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvSectionLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvSectionLabel.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvSectionLabel.setPadding(0, 0, 0, dp(10));
        card.addView(tvSectionLabel);

        TextView tvDescription = new TextView(getContext());
        tvDescription.setText("Welcome beloved! Perazim is engineered so you can nourish your spirit anywhere across Kenya — without needing an internet connection:");
        tvDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvDescription.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        tvDescription.setLineSpacing(dp(2), 1.15f);
        tvDescription.setPadding(0, 0, 0, dp(12));
        card.addView(tvDescription);

        // Feature Rows
        card.addView(createPillarItem(
                "📖",
                "Holy Scriptures (KJV & Swahili)",
                "Full 66 canonical books, chapter navigation, keyword search, and bookmarks available 100% offline."
        ));

        card.addView(createPillarItem(
                "🎵",
                "Church Hymnal & Chords",
                "Complete Perazim worship hymnal with full chord charts, lyrics, and category indexes with zero data needed."
        ));

        card.addView(createPillarItem(
                "☀️",
                "Daily Devotionals & Prayer Wall",
                "Start each morning with the 3-Minute Breakthrough routine and share petitions with the fellowship wall."
        ));

        card.addView(createPillarItem(
                "🎙️",
                "Audio Sermons & Media Cache",
                "Save teachings from Bishop Dr. David Mutweri in ultra-efficient 32kbps data-saver audio."
        ));

        // 3. GET STARTED CTA BUTTON
        Button btnGetStarted = new Button(getContext());
        btnGetStarted.setText("🚀 Enter Sanctuary & Get Started");
        btnGetStarted.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        btnGetStarted.setTypeface(Typeface.DEFAULT_BOLD);
        btnGetStarted.setTextColor(PerazimTheme.COLOR_WHITE);
        btnGetStarted.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_PRIMARY_PURPLE,
                PerazimTheme.COLOR_PURPLE_SHADOW,
                PerazimTheme.RADIUS_MD,
                4
        ));
        btnGetStarted.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpBtn.setMargins(0, dp(14), 0, 0);
        btnGetStarted.setLayoutParams(lpBtn);

        btnGetStarted.setOnClickListener(v -> completeAndDismiss());
        card.addView(btnGetStarted);

        root.addView(card);
        scrollView.addView(root);
        return scrollView;
    }

    private View createPillarItem(String icon, String title, String description) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(PerazimTheme.COLOR_BG_NEUTRAL);
        rowBg.setCornerRadius(dp(PerazimTheme.RADIUS_SM));
        rowBg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        row.setBackground(rowBg);

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpRow.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lpRow);

        TextView tvIcon = new TextView(getContext());
        tvIcon.setText(icon);
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        tvIcon.setPadding(0, 0, dp(10), 0);
        row.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvItemTitle = new TextView(getContext());
        tvItemTitle.setText(title);
        tvItemTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvItemTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvItemTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        textCol.addView(tvItemTitle);

        TextView tvItemDesc = new TextView(getContext());
        tvItemDesc.setText(description);
        tvItemDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvItemDesc.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvItemDesc.setLineSpacing(dp(1), 1.15f);
        tvItemDesc.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvItemDesc);

        row.addView(textCol);
        return row;
    }

    /**
     * Records onboarding completion in SharedPreferences and dismisses dialog.
     */
    private void completeAndDismiss() {
        setCompleted(getContext(), true);
        dismiss();
        if (onGetStartedListener != null) {
            onGetStartedListener.run();
        }
    }

    // =========================================================================
    // STATIC HELPERS & PERSISTENCE
    // =========================================================================

    /**
     * Checks if onboarding has been completed.
     */
    public static boolean shouldShow(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return !prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    /**
     * Updates the onboarding completed flag.
     */
    public static void setCompleted(@NonNull Context context, boolean completed) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    /**
     * Resets the onboarding flag (useful for testing or re-onboarding).
     */
    public static void reset(@NonNull Context context) {
        setCompleted(context, false);
    }

    /**
     * Displays the dialog if onboarding has not been completed yet.
     */
    @Nullable
    public static OnboardingDialog showIfNeeded(@NonNull Activity activity) {
        return showIfNeeded(activity, null);
    }

    /**
     * Displays the dialog if onboarding has not been completed yet, with an optional completion callback.
     */
    @Nullable
    public static OnboardingDialog showIfNeeded(@NonNull Activity activity, @Nullable Runnable onComplete) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        if (shouldShow(activity)) {
            OnboardingDialog dialog = new OnboardingDialog(activity, onComplete);
            dialog.show();
            return dialog;
        }
        return null;
    }

    /**
     * Explicitly displays the onboarding dialog.
     */
    @NonNull
    public static OnboardingDialog show(@NonNull Context context, @Nullable Runnable onComplete) {
        OnboardingDialog dialog = new OnboardingDialog(context, onComplete);
        dialog.show();
        return dialog;
    }

    // =========================================================================
    // GRAPHICAL UTILITIES
    // =========================================================================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getContext().getResources().getDisplayMetrics()
        );
    }

    private Drawable createPillDrawable(int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(PerazimTheme.RADIUS_PILL));
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private Drawable create3dButtonDrawable(int faceColor, int shadowColor, int radiusDp, int shadowDepthDp) {
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(shadowColor);
        shadow.setCornerRadius(dp(radiusDp));

        GradientDrawable face = new GradientDrawable();
        face.setColor(faceColor);
        face.setCornerRadius(dp(radiusDp));

        LayerDrawable layers = new LayerDrawable(new Drawable[]{shadow, face});
        layers.setLayerInset(0, 0, dp(shadowDepthDp), 0, 0);
        layers.setLayerInset(1, 0, 0, 0, dp(shadowDepthDp));
        return layers;
    }
}
