package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.entity.JokeEntity;
import org.perazimchurch.app.data.local.entity.RiddleEntity;
import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.repository.RepositoryProvider;
import org.perazimchurch.app.domain.model.Prayer;
import org.perazimchurch.app.navigation.ChurchIntentManager;
import org.perazimchurch.app.presentation.viewmodel.FellowshipViewModel;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UI Binder responsible for orchestrating the Prayer Screen (formerly Fellowship).
 * <p>
 * Follows the quiet dark contemplative aesthetic:
 * <ul>
 *     <li>Canvas: {@link PerazimTheme#COLOR_BG_DEEP} (#2A0F33).</li>
 *     <li>Title: "Prayer" with quiet scripture subtitle.</li>
 *     <li>Offline banner when disconnected.</li>
 *     <li>Segmented control: Wall | Mine.</li>
 *     <li>Quiet cards on {@link PerazimTheme#COLOR_SURFACE} with stroke {@link PerazimTheme#COLOR_STROKE}.</li>
 *     <li>Quiet outline Amen button with prayer count (NO XP awarded/advertised).</li>
 *     <li>Single solid orange CTA: "+ Share a prayer" opening calm modal dialog.</li>
 * </ul>
 */
public class FellowshipUiBinder {

    public enum FellowshipSubTab {
        PRAYER_WALL,
        PRAISE_REPORTS,
        MINE
    }

    private final Context context;
    private final Activity activity;
    private final FellowshipViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String activeUserId = "user_active";
    private String activeUserName = "Fellow Saint";

    // Segmented tab state
    private boolean isMineTab = false;
    private Button btnTabWall;
    private Button btnTabMine;

    // Containers
    private LinearLayout prayersContainer;
    private ProgressBar prayersLoadingSpinner;
    private TextView tvPrayersEmptyState;
    private View offlineBannerView;

    private final List<Prayer> displayedPrayers = new ArrayList<>();
    private final List<Prayer> userPrayers = new ArrayList<>();
    private final List<Prayer> displayedAnsweredPrayers = new ArrayList<>();
    private final Set<String> amenedPrayerIds = new HashSet<>();

    public FellowshipUiBinder(@NonNull Activity activity, @NonNull FellowshipViewModel viewModel) {
        this.activity = activity;
        this.context = activity;
        this.viewModel = viewModel;
        resolveActiveUser();
    }

    public FellowshipUiBinder(@NonNull Context context, @NonNull FellowshipViewModel viewModel) {
        this.context = context;
        this.activity = context instanceof Activity ? (Activity) context : null;
        this.viewModel = viewModel;
        resolveActiveUser();
    }

    private void resolveActiveUser() {
        try {
            UserEntity user = SessionManager.getInstance(context).getCurrentUser();
            if (user != null) {
                if (user.getId() != null && !user.getId().trim().isEmpty()) {
                    activeUserId = user.getId().trim();
                }
                if (user.getName() != null && !user.getName().trim().isEmpty()) {
                    activeUserName = user.getName().trim();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @NonNull
    public ScrollView buildView() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PerazimTheme.COLOR_BG_DEEP);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(36));

        bind(content);

        scrollView.addView(content);
        return scrollView;
    }

    @NonNull
    public ScrollView bind() {
        return buildView();
    }

    public void bind(@NonNull ViewGroup rootContainer) {
        rootContainer.removeAllViews();

        // 1. Title Bar
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.VERTICAL);
        headerRow.setPadding(0, dp(4), 0, dp(14));

        TextView tvTitle = new TextView(context);
        tvTitle.setText("Prayer");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        headerRow.addView(tvTitle);

        TextView tvSubtitle = new TextView(context);
        tvSubtitle.setText("“Bear ye one another's burdens, and so fulfil the law of Christ.” — Galatians 6:2");
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSubtitle.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvSubtitle.setPadding(0, dp(2), 0, 0);
        headerRow.addView(tvSubtitle);

        rootContainer.addView(headerRow);

        // 2. Offline Banner (shown if disconnected)
        offlineBannerView = createOfflineBanner();
        rootContainer.addView(offlineBannerView);
        updateOfflineBannerVisibility();

        // 3. Segmented Control: Wall | Mine
        View segmentedControl = createSegmentedControl();
        rootContainer.addView(segmentedControl);

        // 4. Single Primary Orange CTA: "+ Share a prayer"
        Button btnSharePrayer = new Button(context);
        btnSharePrayer.setText("+ Share a prayer");
        btnSharePrayer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnSharePrayer.setTypeface(Typeface.DEFAULT_BOLD);
        btnSharePrayer.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnSharePrayer.setBackground(createSolidPillDrawable(PerazimTheme.COLOR_ORANGE_BRAND, 12));
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        lpBtn.setMargins(0, 0, 0, dp(16));
        btnSharePrayer.setLayoutParams(lpBtn);
        btnSharePrayer.setOnClickListener(v -> showSubmitPrayerDialog());
        rootContainer.addView(btnSharePrayer);

        // 5. Prayers Cards Container
        prayersContainer = new LinearLayout(context);
        prayersContainer.setOrientation(LinearLayout.VERTICAL);
        prayersContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        prayersLoadingSpinner = new ProgressBar(context);
        prayersLoadingSpinner.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpSpinner = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpSpinner.gravity = Gravity.CENTER_HORIZONTAL;
        lpSpinner.setMargins(0, dp(16), 0, dp(16));
        prayersLoadingSpinner.setLayoutParams(lpSpinner);
        prayersContainer.addView(prayersLoadingSpinner);

        tvPrayersEmptyState = new TextView(context);
        tvPrayersEmptyState.setText("No prayer petitions yet. Be the first to share a petition.");
        tvPrayersEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPrayersEmptyState.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvPrayersEmptyState.setGravity(Gravity.CENTER);
        tvPrayersEmptyState.setPadding(dp(16), dp(24), dp(16), dp(24));
        tvPrayersEmptyState.setVisibility(View.GONE);
        prayersContainer.addView(tvPrayersEmptyState);

        rootContainer.addView(prayersContainer);

        // Initial Data Load
        refreshPrayers();
        loadUserPrayers();
    }

    private View createOfflineBanner() {
        LinearLayout banner = new LinearLayout(context);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(12), dp(10), dp(12), dp(10));
        banner.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 10, PerazimTheme.COLOR_STROKE, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        banner.setLayoutParams(lp);

        TextView tv = new TextView(context);
        tv.setText("Showing cached prayers. New prayers will sync when connected.");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        banner.addView(tv);

        return banner;
    }

    private void updateOfflineBannerVisibility() {
        if (offlineBannerView == null) return;
        boolean isOnline = isNetworkAvailable();
        offlineBannerView.setVisibility(isOnline ? View.GONE : View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private View createSegmentedControl() {
        LinearLayout segmented = new LinearLayout(context);
        segmented.setOrientation(LinearLayout.HORIZONTAL);
        segmented.setPadding(dp(4), dp(4), dp(4), dp(4));
        segmented.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 12, PerazimTheme.COLOR_STROKE, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        segmented.setLayoutParams(lp);

        btnTabWall = new Button(context);
        btnTabWall.setText("Prayer Wall");
        btnTabWall.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnTabWall.setTypeface(Typeface.DEFAULT_BOLD);
        btnTabWall.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lpWall = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnTabWall.setLayoutParams(lpWall);
        btnTabWall.setOnClickListener(v -> switchSubTab(false));

        btnTabMine = new Button(context);
        btnTabMine.setText("My Prayers");
        btnTabMine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnTabMine.setTypeface(Typeface.DEFAULT_BOLD);
        btnTabMine.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lpMine = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnTabMine.setLayoutParams(lpMine);
        btnTabMine.setOnClickListener(v -> switchSubTab(true));

        segmented.addView(btnTabWall);
        segmented.addView(btnTabMine);

        updateSegmentedStyles();
        return segmented;
    }

    private void switchSubTab(boolean mine) {
        if (isMineTab == mine) return;
        isMineTab = mine;
        updateSegmentedStyles();
        if (isMineTab) {
            renderUserPrayers();
        } else {
            renderWallPrayers();
        }
    }

    private void updateSegmentedStyles() {
        if (btnTabWall == null || btnTabMine == null) return;
        if (!isMineTab) {
            btnTabWall.setBackground(createSolidPillDrawable(PerazimTheme.COLOR_PURPLE_BRAND, 8));
            btnTabWall.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);

            btnTabMine.setBackgroundColor(Color.TRANSPARENT);
            btnTabMine.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        } else {
            btnTabMine.setBackground(createSolidPillDrawable(PerazimTheme.COLOR_PURPLE_BRAND, 8));
            btnTabMine.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);

            btnTabWall.setBackgroundColor(Color.TRANSPARENT);
            btnTabWall.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        }
    }

    public void refreshPrayers() {
        if (prayersLoadingSpinner != null) {
            prayersLoadingSpinner.setVisibility(View.VISIBLE);
        }
        updateOfflineBannerVisibility();
        viewModel.loadPublicPrayers(prayers -> postToMain(() -> {
            if (prayersLoadingSpinner != null) {
                prayersLoadingSpinner.setVisibility(View.GONE);
            }
            displayedPrayers.clear();
            if (prayers != null && !prayers.isEmpty()) {
                displayedPrayers.addAll(prayers);
            } else {
                displayedPrayers.addAll(getFallbackPrayers());
            }
            if (!isMineTab) {
                renderWallPrayers();
            }
        }));
    }

    private void loadUserPrayers() {
        viewModel.loadUserPrayers(activeUserId, prayers -> postToMain(() -> {
            userPrayers.clear();
            if (prayers != null) {
                userPrayers.addAll(prayers);
            }
            if (isMineTab) {
                renderUserPrayers();
            }
        }));
    }

    private void renderWallPrayers() {
        if (prayersContainer == null) return;
        clearCards();

        if (displayedPrayers.isEmpty()) {
            if (tvPrayersEmptyState != null) {
                tvPrayersEmptyState.setText("No prayer petitions yet. Be the first to share a petition.");
                tvPrayersEmptyState.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (tvPrayersEmptyState != null) tvPrayersEmptyState.setVisibility(View.GONE);

        for (Prayer prayer : displayedPrayers) {
            View card = createWallPrayerCard(prayer);
            prayersContainer.addView(card);
        }
    }

    private void renderUserPrayers() {
        if (prayersContainer == null) return;
        clearCards();

        if (userPrayers.isEmpty()) {
            if (tvPrayersEmptyState != null) {
                tvPrayersEmptyState.setText("You haven't submitted any prayer requests yet.\nTap \"+ Share a prayer\" above.");
                tvPrayersEmptyState.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (tvPrayersEmptyState != null) tvPrayersEmptyState.setVisibility(View.GONE);

        for (Prayer prayer : userPrayers) {
            View card = createMinePrayerCard(prayer);
            prayersContainer.addView(card);
        }
    }

    private void clearCards() {
        for (int i = prayersContainer.getChildCount() - 1; i >= 0; i--) {
            View child = prayersContainer.getChildAt(i);
            if (child != prayersLoadingSpinner && child != tvPrayersEmptyState) {
                prayersContainer.removeViewAt(i);
            }
        }
    }

    @NonNull
    private View createWallPrayerCard(@NonNull Prayer prayer) {
        LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, 14, PerazimTheme.COLOR_STROKE, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lpCard);

        // Header: Author & Time
        LinearLayout metaRow = new LinearLayout(context);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(0, 0, 0, dp(6));

        String author = prayer.getAuthorName() != null && !prayer.getAuthorName().trim().isEmpty()
                ? prayer.getAuthorName().trim()
                : "Anonymous";
        TextView tvAuthor = new TextView(context);
        tvAuthor.setText(author);
        tvAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvAuthor.setTypeface(Typeface.DEFAULT_BOLD);
        tvAuthor.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        LinearLayout.LayoutParams lpAuthor = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvAuthor.setLayoutParams(lpAuthor);
        metaRow.addView(tvAuthor);

        TextView tvTime = new TextView(context);
        tvTime.setText(formatRelativeTime(prayer.getCreatedAt()));
        tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTime.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        metaRow.addView(tvTime);

        card.addView(metaRow);

        // Title
        if (prayer.getTitle() != null && !prayer.getTitle().trim().isEmpty()) {
            TextView tvTitle = new TextView(context);
            tvTitle.setText(prayer.getTitle().trim());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            tvTitle.setPadding(0, 0, 0, dp(4));
            card.addView(tvTitle);
        }

        // Body (Max 4 lines with toggle)
        TextView tvBody = new TextView(context);
        tvBody.setText(prayer.getBody() != null ? prayer.getBody() : "");
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvBody.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvBody.setMaxLines(4);
        tvBody.setEllipsize(TextUtils.TruncateAt.END);
        tvBody.setLineSpacing(dp(2), 1.15f);
        tvBody.setPadding(0, 0, 0, dp(10));
        card.addView(tvBody);

        // Toggle expansion if multiline
        tvBody.setOnClickListener(v -> {
            if (tvBody.getMaxLines() == 4) {
                tvBody.setMaxLines(Integer.MAX_VALUE);
            } else {
                tvBody.setMaxLines(4);
            }
        });

        // Bottom Action Row: Amen quiet outline button
        final int[] count = {prayer.getAmenCount()};
        final boolean[] hasAmened = {amenedPrayerIds.contains(prayer.getId()) || prayer.isUserHasAmened()};

        Button btnAmen = new Button(context);
        btnAmen.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAmen.setTypeface(Typeface.DEFAULT_BOLD);
        btnAmen.setPadding(dp(14), dp(6), dp(14), dp(6));
        updateAmenStyle(btnAmen, count[0], hasAmened[0]);

        btnAmen.setOnClickListener(v -> {
            if (!hasAmened[0]) {
                hasAmened[0] = true;
                count[0]++;
                amenedPrayerIds.add(prayer.getId());
                prayer.setAmenCount(count[0]);
                prayer.setUserHasAmened(true);
                updateAmenStyle(btnAmen, count[0], true);
                viewModel.amenPrayer(prayer.getId(), activeUserId, null);
                Toast.makeText(context, "Amen! Joined in prayer.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "You are already standing in prayer for this petition.", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout actionRow = new LinearLayout(context);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.addView(btnAmen);
        card.addView(actionRow);

        return card;
    }

    private void updateAmenStyle(Button btn, int count, boolean hasAmened) {
        String label = (hasAmened ? "✓ Joined in Prayer (" : "🙏 Amen (") + count + ")";
        btn.setText(label);
        if (hasAmened) {
            btn.setBackground(createCardBackground(PerazimTheme.COLOR_PURPLE_GHOST, 16, PerazimTheme.COLOR_PURPLE_SOFT, 1));
            btn.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        } else {
            btn.setBackground(createCardBackground(Color.TRANSPARENT, 16, PerazimTheme.COLOR_STROKE, 1));
            btn.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        }
    }

    @NonNull
    private View createMinePrayerCard(@NonNull Prayer prayer) {
        LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, 14, PerazimTheme.COLOR_STROKE, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lpCard);

        // Header: Status pill + Time
        LinearLayout metaRow = new LinearLayout(context);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(0, 0, 0, dp(6));

        TextView tvStatus = new TextView(context);
        boolean isAnswered = prayer.isAnswered();
        tvStatus.setText(isAnswered ? "Answered" : "Active");
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
        tvStatus.setPadding(dp(8), dp(2), dp(8), dp(2));
        if (isAnswered) {
            tvStatus.setBackground(createCardBackground(PerazimTheme.COLOR_ORANGE_GHOST, 10, PerazimTheme.COLOR_ORANGE_SOFT, 1));
            tvStatus.setTextColor(PerazimTheme.COLOR_ORANGE_SOFT);
        } else {
            tvStatus.setBackground(createCardBackground(PerazimTheme.COLOR_PURPLE_GHOST, 10, PerazimTheme.COLOR_PURPLE_SOFT, 1));
            tvStatus.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        }
        metaRow.addView(tvStatus);

        View spacer = new View(context);
        LinearLayout.LayoutParams lpSpacer = new LinearLayout.LayoutParams(0, 0, 1.0f);
        spacer.setLayoutParams(lpSpacer);
        metaRow.addView(spacer);

        TextView tvTime = new TextView(context);
        tvTime.setText(formatRelativeTime(prayer.getCreatedAt()));
        tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTime.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        metaRow.addView(tvTime);

        card.addView(metaRow);

        // Title
        if (prayer.getTitle() != null && !prayer.getTitle().trim().isEmpty()) {
            TextView tvTitle = new TextView(context);
            tvTitle.setText(prayer.getTitle().trim());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            tvTitle.setPadding(0, 0, 0, dp(4));
            card.addView(tvTitle);
        }

        // Body
        TextView tvBody = new TextView(context);
        tvBody.setText(prayer.getBody() != null ? prayer.getBody() : "");
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvBody.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvBody.setMaxLines(4);
        tvBody.setEllipsize(TextUtils.TruncateAt.END);
        tvBody.setLineSpacing(dp(2), 1.15f);
        tvBody.setPadding(0, 0, 0, dp(10));
        card.addView(tvBody);

        // Actions: if active, show "Mark as Answered" button
        if (!isAnswered) {
            Button btnAnswered = new Button(context);
            btnAnswered.setText("Mark as Answered");
            btnAnswered.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnAnswered.setTypeface(Typeface.DEFAULT_BOLD);
            btnAnswered.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            btnAnswered.setBackground(createCardBackground(Color.TRANSPARENT, 12, PerazimTheme.COLOR_STROKE, 1));
            btnAnswered.setPadding(dp(12), dp(6), dp(12), dp(6));
            btnAnswered.setOnClickListener(v -> promptMarkAnswered(prayer));
            card.addView(btnAnswered);
        }

        return card;
    }

    private void promptMarkAnswered(@NonNull Prayer prayer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(18), dp(20), dp(18));
        layout.setBackgroundColor(PerazimTheme.COLOR_SURFACE);

        TextView tvHeading = new TextView(context);
        tvHeading.setText("Mark as Answered");
        tvHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvHeading.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeading.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvHeading.setPadding(0, 0, 0, dp(6));
        layout.addView(tvHeading);

        TextView tvPrompt = new TextView(context);
        tvPrompt.setText("Give praise for God's faithfulness in \"" + (prayer.getTitle() != null ? prayer.getTitle() : "your petition") + "\". Share a brief note of testimony:");
        tvPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPrompt.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvPrompt.setPadding(0, 0, 0, dp(12));
        layout.addView(tvPrompt);

        final EditText etTestimony = new EditText(context);
        etTestimony.setHint("Write testimony here...");
        etTestimony.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etTestimony.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etTestimony.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etTestimony.setMinLines(3);
        etTestimony.setGravity(Gravity.TOP | Gravity.START);
        etTestimony.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 10, PerazimTheme.COLOR_STROKE, 1));
        etTestimony.setPadding(dp(12), dp(10), dp(12), dp(10));
        layout.addView(etTestimony);

        builder.setView(layout);
        builder.setPositiveButton("Submit Praise", (d, which) -> {
            String testimony = etTestimony.getText().toString().trim();
            viewModel.markAnsweredWithTestimony(prayer.getId(), testimony, () -> postToMain(() -> {
                Toast.makeText(context, "Prayer marked as answered! Praise God.", Toast.LENGTH_SHORT).show();
                prayer.setAnswered(true);
                loadUserPrayers();
                refreshPrayers();
            }));
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(PerazimTheme.COLOR_SURFACE));
        }
        dialog.show();
    }

    public void showSubmitPrayerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(18), dp(20), dp(14));
        layout.setBackgroundColor(PerazimTheme.COLOR_SURFACE);

        TextView tvDialogTitle = new TextView(context);
        tvDialogTitle.setText("Share a Prayer");
        tvDialogTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvDialogTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvDialogTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvDialogTitle.setPadding(0, 0, 0, dp(4));
        layout.addView(tvDialogTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Your church community will stand in intercession with you.");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSub.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvSub.setPadding(0, 0, 0, dp(12));
        layout.addView(tvSub);

        final EditText etTitle = new EditText(context);
        etTitle.setHint("Prayer Title (e.g. Healing, Family, Peace)");
        etTitle.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etTitle.setSingleLine(true);
        etTitle.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 10, PerazimTheme.COLOR_STROKE, 1));
        etTitle.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTitle.setMargins(0, 0, 0, dp(10));
        etTitle.setLayoutParams(lpTitle);
        layout.addView(etTitle);

        final EditText etContent = new EditText(context);
        etContent.setHint("Describe your petition...");
        etContent.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etContent.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etContent.setMinLines(4);
        etContent.setGravity(Gravity.TOP | Gravity.START);
        etContent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etContent.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 10, PerazimTheme.COLOR_STROKE, 1));
        etContent.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpContent = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpContent.setMargins(0, 0, 0, dp(10));
        etContent.setLayoutParams(lpContent);
        layout.addView(etContent);

        final CheckBox cbAnonymous = new CheckBox(context);
        cbAnonymous.setText("Share anonymously");
        cbAnonymous.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        cbAnonymous.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        cbAnonymous.setButtonTintList(ColorStateList.valueOf(PerazimTheme.COLOR_ORANGE_BRAND));
        layout.addView(cbAnonymous);

        builder.setView(layout);

        builder.setPositiveButton("Post Prayer", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            boolean isAnonymous = cbAnonymous.isChecked();

            if (title.isEmpty()) {
                Toast.makeText(context, "Please enter a prayer title", Toast.LENGTH_SHORT).show();
                return;
            }
            if (content.isEmpty()) {
                Toast.makeText(context, "Please enter your petition details", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.submitPrayer(
                    title,
                    content,
                    isAnonymous,
                    activeUserId,
                    isAnonymous ? "Anonymous Saint" : activeUserName,
                    () -> postToMain(() -> {
                        Toast.makeText(context, "Prayer submitted to the Wall.", Toast.LENGTH_SHORT).show();
                        refreshPrayers();
                        loadUserPrayers();
                    })
            );
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(PerazimTheme.COLOR_SURFACE));
        }
        dialog.show();
    }

    public void refreshAnsweredPrayers() {
        viewModel.loadAnsweredPrayers(answered -> postToMain(() -> {
            displayedAnsweredPrayers.clear();
            if (answered != null) {
                displayedAnsweredPrayers.addAll(answered);
            }
        }));
    }

    public void updateNotificationBadge() {
        // Quiet state stub
    }

    public void openQrConnectionDialog() {
        QrConnectionDialog dialog = new QrConnectionDialog(
                context,
                viewModel.getConnectionRepo() != null
                        ? viewModel.getConnectionRepo()
                        : RepositoryProvider.getInstance(context).getConnectionRepository(),
                activeUserId,
                activeUserName,
                "Perazim Central"
        );
        dialog.show();
    }

    public void openChatDialog() {
        ChatDialog dialog = new ChatDialog(context, "elder_gitonga", "Elder Gitonga", "Online");
        dialog.show();
    }

    public void openNotificationDialog() {
        NotificationDialog dialog = new NotificationDialog(
                context,
                viewModel.getNotificationRepo() != null
                        ? viewModel.getNotificationRepo()
                        : RepositoryProvider.getInstance(context).getNotificationRepository(),
                activeUserId,
                this::updateNotificationBadge
        );
        dialog.show();
    }

    public void nextRiddle() {
        // Handled in Me -> Fellowship Extras
    }

    public List<Prayer> getDisplayedPrayers() {
        return displayedPrayers;
    }

    public List<Prayer> getDisplayedAnsweredPrayers() {
        return displayedAnsweredPrayers;
    }

    private String formatRelativeTime(long timestamp) {
        if (timestamp <= 0) return "Recently";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60_000L) {
            return "Just now";
        } else if (diff < 3600_000L) {
            long mins = Math.max(1, diff / 60_000L);
            return mins + (mins == 1 ? " min ago" : " mins ago");
        } else if (diff < 86400_000L) {
            long hours = diff / 3600_000L;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < 172800_000L) {
            return "Yesterday";
        } else {
            long days = diff / 86400_000L;
            return days + " days ago";
        }
    }

    private List<Prayer> getFallbackPrayers() {
        List<Prayer> list = new ArrayList<>();
        list.add(new Prayer(
                "p1",
                "Prayers for University Exams",
                "Please stand with me as I sit my final semester engineering papers this week. Trusting the Lord for wisdom and clarity.",
                "user_faith",
                "Faith Wanjiru",
                Prayer.Visibility.PUBLIC,
                14,
                System.currentTimeMillis() - 7200000L,
                false,
                false
        ));
        list.add(new Prayer(
                "p2",
                "Healing for My Mother",
                "Asking the church for prayers over my mother who is undergoing minor surgery tomorrow in Nairobi. We declare divine health and peace.",
                "user_john",
                "Brother John",
                Prayer.Visibility.PUBLIC,
                29,
                System.currentTimeMillis() - 86400000L,
                false,
                false
        ));
        list.add(new Prayer(
                "p3",
                "Breakthrough in Job Interview",
                "Standing in faith for open doors in an upcoming accounting interview. Believing God for supernatural favour.",
                "user_anonymous",
                "Anonymous",
                Prayer.Visibility.PUBLIC,
                8,
                System.currentTimeMillis() - 172800000L,
                false,
                false
        ));
        return list;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    private LinearLayout createCard(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        LinearLayout card = new LinearLayout(context);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(createCardBackground(bgColor, cornerRadiusDp, borderColor, borderWidthDp));
        return card;
    }

    private Drawable createCardBackground(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        return drawable;
    }

    private Drawable createSolidPillDrawable(int bgColor, int cornerRadiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        return drawable;
    }

    private void postToMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
}
