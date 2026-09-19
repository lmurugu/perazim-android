package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.DownloadedContent;
import org.perazimchurch.app.domain.model.Hymn;
import org.perazimchurch.app.domain.model.User;
import org.perazimchurch.app.presentation.viewmodel.ProfileViewModel;
import org.perazimchurch.app.presentation.viewmodel.SavedContentData;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * UI Binder responsible for orchestrating the Profile tab.
 * <p>
 * Binds directly to {@link ProfileViewModel} and manages:
 * <ul>
 *     <li>Member Profile Header (active user name, email, role, and avatar).</li>
 *     <li>Campus Switcher Dialog (Embu Headquarters, Nairobi, Meru, Online).</li>
 *     <li>Spiritual Journey Card (streak counter, streak freeze toggle, spiritual XP progress bar, Grace Points).</li>
 *     <li>Saved Content Manager with tabbed/expandable view for Downloaded Sermons, Favorite Hymns, and Bookmarked Scripture Verses.</li>
 * </ul>
 */
public class ProfileUiBinder {

    private final Context context;
    private final Activity activity;
    private final ProfileViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Active User UI references
    private TextView tvMemberName;
    private TextView tvMemberEmail;
    private TextView tvMemberCampus;
    private TextView tvCovenantBadge;
    private ImageView ivAvatar;

    // Spiritual Journey UI references
    private TextView tvStreakValue;
    private TextView tvStreakStatus;
    private Button btnStreakFreezeToggle;
    private TextView tvXpValue;
    private TextView tvGraceValue;
    private ProgressBar xpProgressBar;
    private TextView tvXpLevelLabel;

    // Current State Cache
    private User currentUser;
    private SavedContentData currentSavedContent;
    private String selectedCampusName = "Embu Headquarters";
    private boolean isStreakFrozen = false;

    // Saved Content Manager UI references
    private LinearLayout savedContentTabRow;
    private LinearLayout savedContentListContainer;
    private Button btnTabSermons;
    private Button btnTabHymns;
    private Button btnTabVerses;
    private int selectedSavedContentTab = 0; // 0 = Sermons, 1 = Hymns, 2 = Verses

    public ProfileUiBinder(@NonNull Activity activity, @NonNull ProfileViewModel viewModel) {
        this.activity = activity;
        this.context = activity;
        this.viewModel = viewModel;
    }

    public ProfileUiBinder(@NonNull Context context, @NonNull ProfileViewModel viewModel) {
        this.context = context;
        this.activity = context instanceof Activity ? (Activity) context : null;
        this.viewModel = viewModel;
    }

    /**
     * Builds and returns the complete ScrollView representing the Profile Screen.
     */
    @NonNull
    public ScrollView buildView() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PerazimTheme.COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        bind(content);

        scrollView.addView(content);
        return scrollView;
    }

    /**
     * Builds and binds the complete Profile Tab view.
     */
    @NonNull
    public ScrollView bind() {
        return buildView();
    }

    /**
     * Binds the Profile UI components into the provided root container.
     */
    public void bind(@NonNull ViewGroup rootContainer) {
        // 1. Member Profile Header Card
        View headerCard = createProfileHeaderCard();
        rootContainer.addView(headerCard);

        // 2. Spiritual Journey Card
        View journeyCard = createSpiritualJourneyCard();
        rootContainer.addView(journeyCard);

        // 3. Saved Content Manager (Tabbed View)
        View savedContentCard = createSavedContentManagerCard();
        rootContainer.addView(savedContentCard);

        // 4. Church Identity & Campus Switcher Card
        View churchCard = createChurchIdentityCard();
        rootContainer.addView(churchCard);

        // 5. Pastoral Care & Support Card
        View pastoralCard = createPastoralSupportCard();
        rootContainer.addView(pastoralCard);

        // Initial Data Load
        refreshProfile();
        refreshSavedContent();
    }

    // =========================================================================
    // 1. MEMBER PROFILE HEADER
    // =========================================================================

    private View createProfileHeaderCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lpCard);

        LinearLayout memberRow = new LinearLayout(context);
        memberRow.setOrientation(LinearLayout.HORIZONTAL);
        memberRow.setGravity(Gravity.CENTER_VERTICAL);
        memberRow.setPadding(0, 0, 0, dp(12));

        // Member Avatar
        Bitmap avatarBmp = loadAssetBitmap("bishop_portrait.jpg", 200);
        ivAvatar = new ImageView(context);
        if (avatarBmp != null) {
            ivAvatar.setImageBitmap(getRoundedCornerBitmap(avatarBmp, dp(32)));
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            // Draw placeholder monogram
            ivAvatar.setImageBitmap(createMonogramBitmap("PM", dp(64)));
        }
        LinearLayout.LayoutParams lpAv = new LinearLayout.LayoutParams(dp(64), dp(64));
        lpAv.setMargins(0, 0, dp(14), 0);
        ivAvatar.setLayoutParams(lpAv);
        memberRow.addView(ivAvatar);

        // Member Text Info
        LinearLayout memberInfo = new LinearLayout(context);
        memberInfo.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpInfo = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        memberInfo.setLayoutParams(lpInfo);

        tvMemberName = new TextView(context);
        tvMemberName.setText("Perazim Covenant Member");
        tvMemberName.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_SUBTITLE);
        tvMemberName.setTypeface(Typeface.DEFAULT_BOLD);
        tvMemberName.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        memberInfo.addView(tvMemberName);

        tvMemberEmail = new TextView(context);
        tvMemberEmail.setText("member@perazimchurch.org");
        tvMemberEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvMemberEmail.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvMemberEmail.setPadding(0, dp(2), 0, dp(2));
        memberInfo.addView(tvMemberEmail);

        tvMemberCampus = new TextView(context);
        tvMemberCampus.setText("Perazim Mission Church · Embu Headquarters");
        tvMemberCampus.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvMemberCampus.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvMemberCampus.setTypeface(Typeface.DEFAULT_BOLD);
        tvMemberCampus.setPadding(0, 0, 0, dp(6));
        memberInfo.addView(tvMemberCampus);

        // Covenant Badge
        tvCovenantBadge = new TextView(context);
        tvCovenantBadge.setText("🕊️ Active Disciple • Covenant Partner");
        tvCovenantBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvCovenantBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvCovenantBadge.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvCovenantBadge.setBackground(createPillDrawable(PerazimTheme.COLOR_PURPLE_TINT, PerazimTheme.COLOR_BORDER_GREY));
        tvCovenantBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        memberInfo.addView(tvCovenantBadge);

        memberRow.addView(memberInfo);
        card.addView(memberRow);

        // Campus Switcher Button on Header
        Button btnSwitchCampus = new Button(context);
        btnSwitchCampus.setText("📍 Switch Church Campus");
        btnSwitchCampus.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btnSwitchCampus.setTypeface(Typeface.DEFAULT_BOLD);
        btnSwitchCampus.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        btnSwitchCampus.setBackground(createPillDrawable(PerazimTheme.COLOR_PURPLE_TINT, PerazimTheme.COLOR_BORDER_GREY));
        btnSwitchCampus.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnSwitchCampus.setOnClickListener(v -> showCampusSwitcherDialog());
        card.addView(btnSwitchCampus);

        return card;
    }

    // =========================================================================
    // 2. SPIRITUAL JOURNEY CARD
    // =========================================================================

    private View createSpiritualJourneyCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lpCard);

        TextView tvHeader = new TextView(context);
        tvHeader.setText("🔥 SPIRITUAL JOURNEY & GROWTH");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvHeader.setPadding(0, 0, 0, dp(10));
        card.addView(tvHeader);

        // 3-Metric Summary Row: Streak, XP, Grace
        LinearLayout hudRow = new LinearLayout(context);
        hudRow.setOrientation(LinearLayout.HORIZONTAL);
        hudRow.setGravity(Gravity.CENTER_VERTICAL);
        hudRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Streak Block
        LinearLayout streakBlock = createMetricBox("🔥 7 Days", "Daily Streak", "Active", PerazimTheme.COLOR_ORANGE_TINT, PerazimTheme.COLOR_ACCENT_ORANGE);
        tvStreakValue = (TextView) streakBlock.getChildAt(0);
        tvStreakStatus = (TextView) streakBlock.getChildAt(2);
        hudRow.addView(streakBlock);

        // XP Block
        LinearLayout xpBlock = createMetricBox("⭐ 450 XP", "Spiritual XP", "Level 1", PerazimTheme.COLOR_PURPLE_TINT, PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvXpValue = (TextView) xpBlock.getChildAt(0);
        hudRow.addView(xpBlock);

        // Grace Points Block
        LinearLayout graceBlock = createMetricBox("💜 5 Pts", "Grace Points", "Tokens", PerazimTheme.COLOR_ORANGE_TINT, PerazimTheme.COLOR_ORANGE_DARK);
        tvGraceValue = (TextView) graceBlock.getChildAt(0);
        hudRow.addView(graceBlock);

        card.addView(hudRow);

        // Spiritual XP Progress Bar
        tvXpLevelLabel = new TextView(context);
        tvXpLevelLabel.setText("Level 1 Disciple — 450 / 500 XP to Level 2");
        tvXpLevelLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvXpLevelLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvXpLevelLabel.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        tvXpLevelLabel.setPadding(0, dp(12), 0, dp(4));
        card.addView(tvXpLevelLabel);

        xpProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        xpProgressBar.setMax(500);
        xpProgressBar.setProgress(450);
        LinearLayout.LayoutParams lpBar = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(10));
        lpBar.setMargins(0, 0, 0, dp(12));
        xpProgressBar.setLayoutParams(lpBar);
        card.addView(xpProgressBar);

        // Streak Freeze Toggle Button
        btnStreakFreezeToggle = new Button(context);
        btnStreakFreezeToggle.setText("❄️ Streak Protection: Freeze Streak");
        btnStreakFreezeToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        btnStreakFreezeToggle.setTypeface(Typeface.DEFAULT_BOLD);
        btnStreakFreezeToggle.setTextColor(PerazimTheme.COLOR_WHITE);
        btnStreakFreezeToggle.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_PRIMARY_PURPLE,
                PerazimTheme.COLOR_PURPLE_SHADOW,
                PerazimTheme.RADIUS_MD,
                3
        ));
        btnStreakFreezeToggle.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnStreakFreezeToggle.setOnClickListener(v -> toggleStreakFreeze());
        card.addView(btnStreakFreezeToggle);

        return card;
    }

    private LinearLayout createMetricBox(String value, String label, String sublabel, int bgColor, int textColor) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(10), dp(8), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(PerazimTheme.RADIUS_MD));
        bg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        box.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        box.setLayoutParams(lp);

        TextView tvVal = new TextView(context);
        tvVal.setText(value);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setTextColor(textColor);
        tvVal.setGravity(Gravity.CENTER);
        box.addView(tvVal);

        TextView tvLbl = new TextView(context);
        tvLbl.setText(label);
        tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvLbl.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvLbl.setGravity(Gravity.CENTER);
        tvLbl.setPadding(0, dp(2), 0, 0);
        box.addView(tvLbl);

        TextView tvSub = new TextView(context);
        tvSub.setText(sublabel);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
        tvSub.setTextColor(textColor);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        tvSub.setGravity(Gravity.CENTER);
        box.addView(tvSub);

        return box;
    }

    private void toggleStreakFreeze() {
        boolean newFrozenState = !isStreakFrozen;
        viewModel.setStreakFreeze(newFrozenState, () -> {
            postToMain(() -> {
                isStreakFrozen = newFrozenState;
                updateStreakFreezeUi(newFrozenState);
                String msg = newFrozenState
                        ? "❄️ Streak frozen! Your spiritual streak is shielded today."
                        : "🔥 Streak unfrozen! Continue your daily devotions.";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateStreakFreezeUi(boolean frozen) {
        if (tvStreakStatus != null) {
            tvStreakStatus.setText(frozen ? "❄️ Frozen" : "Active");
        }
        if (btnStreakFreezeToggle != null) {
            if (frozen) {
                btnStreakFreezeToggle.setText("🔥 Unfreeze Streak (Protected Today)");
                btnStreakFreezeToggle.setBackground(create3dButtonDrawable(
                        PerazimTheme.COLOR_ACCENT_ORANGE,
                        PerazimTheme.COLOR_ORANGE_SHADOW,
                        PerazimTheme.RADIUS_MD,
                        3
                ));
            } else {
                btnStreakFreezeToggle.setText("❄️ Streak Protection: Freeze Streak");
                btnStreakFreezeToggle.setBackground(create3dButtonDrawable(
                        PerazimTheme.COLOR_PRIMARY_PURPLE,
                        PerazimTheme.COLOR_PURPLE_SHADOW,
                        PerazimTheme.RADIUS_MD,
                        3
                ));
            }
        }
    }

    // =========================================================================
    // 3. SAVED CONTENT MANAGER (Tabbed / Expandable)
    // =========================================================================

    private View createSavedContentManagerCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lpCard);

        TextView tvTag = new TextView(context);
        tvTag.setText("📦 SAVED & OFFLINE CONTENT MANAGER");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvTag.setPadding(0, 0, 0, dp(8));
        card.addView(tvTag);

        // Tab Selector Row
        savedContentTabRow = new LinearLayout(context);
        savedContentTabRow.setOrientation(LinearLayout.HORIZONTAL);
        savedContentTabRow.setPadding(0, 0, 0, dp(10));

        btnTabSermons = createTabButton("🎙️ Sermons", 0);
        btnTabHymns = createTabButton("🎵 Hymns", 1);
        btnTabVerses = createTabButton("📖 Verses", 2);

        savedContentTabRow.addView(btnTabSermons);
        savedContentTabRow.addView(btnTabHymns);
        savedContentTabRow.addView(btnTabVerses);
        card.addView(savedContentTabRow);

        // List Container for active tab
        savedContentListContainer = new LinearLayout(context);
        savedContentListContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(savedContentListContainer);

        // Update tab styles
        updateTabButtonStyles();

        return card;
    }

    private Button createTabButton(String label, int tabIndex) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(8), dp(6), dp(8), dp(6));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> {
            selectedSavedContentTab = tabIndex;
            updateTabButtonStyles();
            renderActiveSavedContentTab();
        });
        return btn;
    }

    private void updateTabButtonStyles() {
        applyTabStyle(btnTabSermons, selectedSavedContentTab == 0);
        applyTabStyle(btnTabHymns, selectedSavedContentTab == 1);
        applyTabStyle(btnTabVerses, selectedSavedContentTab == 2);
    }

    private void applyTabStyle(Button btn, boolean isSelected) {
        if (btn == null) return;
        if (isSelected) {
            btn.setTextColor(PerazimTheme.COLOR_WHITE);
            GradientDrawable g = new GradientDrawable();
            g.setColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
            g.setCornerRadius(dp(PerazimTheme.RADIUS_MD));
            btn.setBackground(g);
        } else {
            btn.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
            GradientDrawable g = new GradientDrawable();
            g.setColor(PerazimTheme.COLOR_BG_NEUTRAL);
            g.setCornerRadius(dp(PerazimTheme.RADIUS_MD));
            g.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
            btn.setBackground(g);
        }
    }

    public void refreshSavedContent() {
        viewModel.loadSavedContent(data -> {
            postToMain(() -> {
                currentSavedContent = data;
                updateTabBadgeCounts(data);
                renderActiveSavedContentTab();
            });
        });
    }

    private void updateTabBadgeCounts(@Nullable SavedContentData data) {
        int sermonsCount = (data != null && data.getDownloadedSermons() != null) ? data.getDownloadedSermons().size() : 0;
        int hymnsCount = (data != null && data.getFavoriteHymns() != null) ? data.getFavoriteHymns().size() : 0;
        int versesCount = (data != null && data.getBookmarkedVerses() != null) ? data.getBookmarkedVerses().size() : 0;

        if (btnTabSermons != null) btnTabSermons.setText("🎙️ Sermons (" + sermonsCount + ")");
        if (btnTabHymns != null) btnTabHymns.setText("🎵 Hymns (" + hymnsCount + ")");
        if (btnTabVerses != null) btnTabVerses.setText("📖 Verses (" + versesCount + ")");
    }

    private void renderActiveSavedContentTab() {
        if (savedContentListContainer == null) return;
        savedContentListContainer.removeAllViews();

        if (currentSavedContent == null) {
            renderEmptySavedContent("Loading offline content...");
            return;
        }

        switch (selectedSavedContentTab) {
            case 0: // Downloaded Sermons
                renderDownloadedSermons(currentSavedContent.getDownloadedSermons());
                break;
            case 1: // Favorite Hymns
                renderFavoriteHymns(currentSavedContent.getFavoriteHymns());
                break;
            case 2: // Bookmarked Scripture Verses
                renderBookmarkedVerses(currentSavedContent.getBookmarkedVerses());
                break;
        }
    }

    private void renderDownloadedSermons(@Nullable List<DownloadedContent> downloads) {
        if (downloads == null || downloads.isEmpty()) {
            renderEmptySavedContent("No audio sermons downloaded yet. Tap download on any sermon to listen offline with zero data.");
            return;
        }

        for (DownloadedContent item : downloads) {
            LinearLayout row = createSavedItemRow();

            TextView tvIcon = new TextView(context);
            tvIcon.setText("🎙️");
            tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            tvIcon.setPadding(0, 0, dp(8), 0);
            row.addView(tvIcon);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvTitle = new TextView(context);
            tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Sermon Audio");
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
            textCol.addView(tvTitle);

            long mb = Math.max(1, item.getFileSizeBytes() / (1024 * 1024));
            TextView tvSub = new TextView(context);
            tvSub.setText(mb + " MB · 100% Offline Ready (32kbps Data Saver)");
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            tvSub.setTextColor(PerazimTheme.COLOR_SUCCESS);
            textCol.addView(tvSub);

            row.addView(textCol);

            Button btnPlay = new Button(context);
            btnPlay.setText("▶ Play");
            btnPlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            btnPlay.setTextColor(PerazimTheme.COLOR_WHITE);
            btnPlay.setBackground(create3dButtonDrawable(
                    PerazimTheme.COLOR_PRIMARY_PURPLE,
                    PerazimTheme.COLOR_PURPLE_SHADOW,
                    PerazimTheme.RADIUS_SM,
                    2
            ));
            btnPlay.setPadding(dp(8), dp(4), dp(8), dp(4));
            btnPlay.setOnClickListener(v -> Toast.makeText(context, "Playing offline sermon: " + item.getTitle(), Toast.LENGTH_SHORT).show());
            row.addView(btnPlay);

            savedContentListContainer.addView(row);
        }
    }

    private void renderFavoriteHymns(@Nullable List<Hymn> hymns) {
        if (hymns == null || hymns.isEmpty()) {
            renderEmptySavedContent("No favorite hymns saved yet. Heart any hymn in the Worship tab to keep it ready for devotion.");
            return;
        }

        for (Hymn hymn : hymns) {
            LinearLayout row = createSavedItemRow();

            TextView tvIcon = new TextView(context);
            tvIcon.setText("🎵");
            tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            tvIcon.setPadding(0, 0, dp(8), 0);
            row.addView(tvIcon);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvTitle = new TextView(context);
            tvTitle.setText("#" + hymn.getNumber() + " " + hymn.getTitle());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
            textCol.addView(tvTitle);

            TextView tvSub = new TextView(context);
            String keyBpm = (hymn.getKey() != null ? "Key: " + hymn.getKey() : "") +
                    (hymn.getTempoBpm() > 0 ? " · " + hymn.getTempoBpm() + " BPM" : "");
            tvSub.setText(keyBpm.isEmpty() ? "Offline Chord Chart Available" : keyBpm);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            tvSub.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
            textCol.addView(tvSub);

            row.addView(textCol);

            Button btnSing = new Button(context);
            btnSing.setText("View");
            btnSing.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            btnSing.setTextColor(PerazimTheme.COLOR_WHITE);
            btnSing.setBackground(create3dButtonDrawable(
                    PerazimTheme.COLOR_ACCENT_ORANGE,
                    PerazimTheme.COLOR_ORANGE_SHADOW,
                    PerazimTheme.RADIUS_SM,
                    2
            ));
            btnSing.setPadding(dp(8), dp(4), dp(8), dp(4));
            btnSing.setOnClickListener(v -> Toast.makeText(context, "Hymn #" + hymn.getNumber() + ": " + hymn.getTitle(), Toast.LENGTH_SHORT).show());
            row.addView(btnSing);

            savedContentListContainer.addView(row);
        }
    }

    private void renderBookmarkedVerses(@Nullable List<BibleVerse> verses) {
        if (verses == null || verses.isEmpty()) {
            renderEmptySavedContent("No scripture verses bookmarked yet. Tap the bookmark icon while reading scripture to save verses here.");
            return;
        }

        for (BibleVerse verse : verses) {
            LinearLayout row = createSavedItemRow();

            TextView tvIcon = new TextView(context);
            tvIcon.setText("📖");
            tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            tvIcon.setPadding(0, 0, dp(8), 0);
            row.addView(tvIcon);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvRef = new TextView(context);
            tvRef.setText(verse.getBookId() + " " + verse.getChapterNumber() + ":" + verse.getVerseNumber() + " (" + verse.getTranslationId() + ")");
            tvRef.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
            tvRef.setTypeface(Typeface.DEFAULT_BOLD);
            tvRef.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
            textCol.addView(tvRef);

            TextView tvText = new TextView(context);
            tvText.setText(verse.getText());
            tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
            tvText.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
            tvText.setMaxLines(2);
            textCol.addView(tvText);

            row.addView(textCol);
            savedContentListContainer.addView(row);
        }
    }

    private LinearLayout createSavedItemRow() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PerazimTheme.COLOR_BG_NEUTRAL);
        bg.setCornerRadius(dp(PerazimTheme.RADIUS_SM));
        bg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        row.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);
        return row;
    }

    private void renderEmptySavedContent(String message) {
        TextView tv = new TextView(context);
        tv.setText(message);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tv.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tv.setPadding(dp(12), dp(16), dp(12), dp(16));
        tv.setGravity(Gravity.CENTER);
        savedContentListContainer.addView(tv);
    }

    // =========================================================================
    // 4. CAMPUS SWITCHER & CHURCH IDENTITY
    // =========================================================================

    private View createChurchIdentityCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        TextView tvTag = new TextView(context);
        tvTag.setText("🏛️ CHURCH IDENTITY & AFFILIATION");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvTag.setPadding(0, 0, 0, dp(6));
        card.addView(tvTag);

        TextView tvName = new TextView(context);
        tvName.setText("PERAZIM MISSION CHURCH");
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_SUBTITLE);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        card.addView(tvName);

        TextView tvMotto = new TextView(context);
        tvMotto.setText("“The Place of Great Breakthrough” (2 Samuel 5:20)");
        tvMotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvMotto.setTypeface(Typeface.DEFAULT_BOLD);
        tvMotto.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvMotto.setPadding(0, dp(2), 0, dp(4));
        card.addView(tvMotto);

        // Clickable Campus Affiliation Row
        LinearLayout rowCampus = createInteractiveRow(
                "📍",
                "Affiliated Campus (Tap to Switch)",
                selectedCampusName + " (Tap to change)",
                v -> showCampusSwitcherDialog()
        );
        card.addView(rowCampus);

        // Bishop Contact Row
        LinearLayout rowBishop = createInteractiveRow(
                "👤",
                "Presiding Bishop",
                "Bishop Dr. David Mutweri (+254 710 772 227)",
                v -> dialPhone("+254710772227")
        );
        card.addView(rowBishop);

        return card;
    }

    /**
     * Shows a dialog allowing the user to select one of the four official campuses:
     * Embu Headquarters, Nairobi, Meru, Online.
     */
    public void showCampusSwitcherDialog() {
        final String[] campusNames = {
                "Embu Headquarters",
                "Nairobi",
                "Meru",
                "Online"
        };
        final String[] campusIds = {
                "campus_embu",
                "campus_nairobi",
                "campus_meru",
                "campus_online"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("📍 Select Church Campus");
        builder.setItems(campusNames, (dialog, which) -> {
            String selectedName = campusNames[which];
            String selectedId = campusIds[which];
            selectedCampusName = selectedName;

            // Switch campus in ViewModel (Room User update)
            viewModel.switchCampus(selectedId, () -> {
                postToMain(() -> {
                    if (tvMemberCampus != null) {
                        tvMemberCampus.setText("Perazim Mission Church · " + selectedName);
                    }
                    Toast.makeText(context, "📍 Switched campus to " + selectedName, Toast.LENGTH_SHORT).show();
                    refreshProfile();
                });
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // =========================================================================
    // 5. PASTORAL CARE & SUPPORT
    // =========================================================================

    private View createPastoralSupportCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        TextView tvTag = new TextView(context);
        tvTag.setText("🙏 PASTORAL CARE & DIRECT SUPPORT");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvTag.setPadding(0, 0, 0, dp(6));
        card.addView(tvTag);

        card.addView(createInteractiveRow("📞", "Call Bishop Directly", "(+254) 0710 772 227", v -> dialPhone("+254710772227")));
        card.addView(createInteractiveRow("✉️", "Bishop Email", "bishop@perazimchurch.org", v -> sendEmail("bishop@perazimchurch.org", "Pastoral Care")));
        card.addView(createInteractiveRow("📧", "Secretariat", "info@perazimchurch.org", v -> sendEmail("info@perazimchurch.org", "Church Inquiry")));
        card.addView(createInteractiveRow("💚", "Safaricom Giving Paybill", "4069983 (Lipa na M-Pesa)", v -> Toast.makeText(context, "Perazim Paybill: 4069983", Toast.LENGTH_SHORT).show()));

        return card;
    }

    // =========================================================================
    // PROFILE DATA LOADING & UI SYNC
    // =========================================================================

    public void refreshProfile() {
        viewModel.loadProfile(user -> {
            postToMain(() -> {
                currentUser = user;
                if (user != null) {
                    if (tvMemberName != null && user.getFullName() != null) {
                        tvMemberName.setText(user.getFullName());
                    }
                    if (tvMemberEmail != null && user.getEmail() != null) {
                        tvMemberEmail.setText(user.getEmail());
                    }
                    if (user.getRole() != null && tvCovenantBadge != null) {
                        tvCovenantBadge.setText("🕊️ " + user.getRole());
                    }
                    if (tvStreakValue != null) {
                        tvStreakValue.setText("🔥 " + user.getStreakCount() + " Days");
                    }
                    isStreakFrozen = user.isStreakFrozen();
                    updateStreakFreezeUi(isStreakFrozen);

                    if (tvXpValue != null) {
                        tvXpValue.setText("⭐ " + user.getSpiritualXp() + " XP");
                    }
                    if (tvGraceValue != null) {
                        tvGraceValue.setText("💜 " + user.getGracePoints() + " Pts");
                    }

                    int xp = user.getSpiritualXp();
                    int level = (xp / 500) + 1;
                    int progress = xp % 500;
                    if (xpProgressBar != null) {
                        xpProgressBar.setProgress(progress);
                    }
                    if (tvXpLevelLabel != null) {
                        tvXpLevelLabel.setText("Level " + level + " Disciple — " + progress + " / 500 XP to Level " + (level + 1));
                    }
                }
            });
        });
    }

    // =========================================================================
    // GRAPHICS & UTILITIES
    // =========================================================================

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
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        card.setBackground(drawable);
        return card;
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

    private LinearLayout createInteractiveRow(String icon, String label, String value, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(PerazimTheme.COLOR_BG_NEUTRAL);
        rowBg.setCornerRadius(dp(PerazimTheme.RADIUS_SM));
        rowBg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        row.setBackground(rowBg);

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lpRow);

        TextView tvIcon = new TextView(context);
        tvIcon.setText(icon);
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tvIcon.setPadding(0, 0, dp(8), 0);
        row.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvLbl = new TextView(context);
        tvLbl.setText(label);
        tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvLbl.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        textCol.addView(tvLbl);

        TextView tvVal = new TextView(context);
        tvVal.setText(value);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        textCol.addView(tvVal);

        row.addView(textCol);

        TextView tvHint = new TextView(context);
        tvHint.setText("Tap ➔");
        tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvHint.setTypeface(Typeface.DEFAULT_BOLD);
        tvHint.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        row.addView(tvHint);

        row.setOnClickListener(listener);
        return row;
    }

    private void dialPhone(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Phone: " + number, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(String address, String subject) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Email: " + address, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private Bitmap loadAssetBitmap(String name, int maxDim) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream is = context.getAssets().open(name);
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int sampleSize = 1;
            while ((opts.outWidth / sampleSize) > maxDim || (opts.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            is = context.getAssets().open(name);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bmp;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    private Bitmap createMonogramBitmap(String text, int sizePx) {
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint);

        paint.setColor(PerazimTheme.COLOR_WHITE);
        paint.setTextSize(sizePx * 0.4f);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float y = (sizePx / 2f) - bounds.exactCenterY();
        canvas.drawText(text, sizePx / 2f, y, paint);
        return bmp;
    }

    private void postToMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    @Nullable
    public User getCurrentUser() {
        return currentUser;
    }

    @Nullable
    public SavedContentData getCurrentSavedContent() {
        return currentSavedContent;
    }
}
