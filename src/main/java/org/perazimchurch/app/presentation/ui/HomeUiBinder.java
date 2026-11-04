package org.perazimchurch.app.presentation.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.model.Announcement;
import org.perazimchurch.app.navigation.ChurchIntentManager;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.Event;
import org.perazimchurch.app.domain.model.Reflection;
import org.perazimchurch.app.domain.model.Sermon;
import org.perazimchurch.app.domain.model.User;
import org.perazimchurch.app.presentation.viewmodel.BibleViewModel;
import org.perazimchurch.app.presentation.viewmodel.HomeData;
import org.perazimchurch.app.presentation.viewmodel.HomeViewModel;
import org.perazimchurch.app.presentation.viewmodel.ViewModelFactory;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * UI Binder responsible for constructing, styling, and populating the Perazim Home screen.
 * Connects directly to {@link HomeViewModel} and delegates scripture passage reading
 * to {@link BibleReaderDialog}.
 */
public class HomeUiBinder {

    // --- CANONICAL THEME COLORS ---
    public static final int COLOR_PRIMARY_PURPLE = PerazimTheme.COLOR_PRIMARY_PURPLE; // #681A7D
    public static final int COLOR_PURPLE_DARK    = PerazimTheme.COLOR_PURPLE_DEEP;     // #2D0938
    public static final int COLOR_PURPLE_DEEPEST = PerazimTheme.COLOR_OBSIDIAN;        // #140319
    public static final int COLOR_PURPLE_SHADOW  = PerazimTheme.COLOR_PURPLE_SHADOW;   // #450E53
    public static final int COLOR_PURPLE_TINT    = PerazimTheme.COLOR_PURPLE_TINT;     // #F5ECF7
    public static final int COLOR_PURPLE_SOFT    = PerazimTheme.COLOR_PURPLE_SOFT;     // #C896D8
    public static final int COLOR_BORDER_GREY    = PerazimTheme.COLOR_BORDER_GREY;     // #E7D5EC
    public static final int COLOR_BG_NEUTRAL     = PerazimTheme.COLOR_BG_NEUTRAL;      // #FAF7FB
    public static final int COLOR_WHITE          = PerazimTheme.COLOR_WHITE;           // #FFFFFF
    public static final int COLOR_ACCENT_ORANGE  = PerazimTheme.COLOR_ACCENT_ORANGE;   // #E17D2F
    public static final int COLOR_ORANGE_DARK    = PerazimTheme.COLOR_ORANGE_DARK;     // #78350A
    public static final int COLOR_ORANGE_SHADOW  = PerazimTheme.COLOR_ORANGE_SHADOW;   // #98450B
    public static final int COLOR_ORANGE_BORDER  = PerazimTheme.COLOR_ORANGE_BORDER;   // #F9DFCC
    public static final int COLOR_ORANGE_TINT    = PerazimTheme.COLOR_ORANGE_TINT;     // #FDF5EF
    public static final int COLOR_TEXT_DARK      = Color.parseColor("#1A1225");
    public static final int COLOR_TEXT_MUTED     = Color.parseColor("#725B78");

    public interface HomeUiListener {
        void onStreakUpdated(int streak);
        void onXpAwarded(int xpAwarded, int totalXp);
        void onPlaySermon(@NonNull Sermon sermon);
        void onOpenScripture(@NonNull String bookId, int chapter);
        default void onNavigateToTab(int tabIndex) {}
    }

    private final Context context;
    private final HomeViewModel homeViewModel;
    private final BibleViewModel bibleViewModel;
    private final HomeUiListener listener;

    // View References
    private ScrollView rootScrollView;
    private LinearLayout contentContainer;

    // Routine State
    private static final String KEY_ROUTINE_STEP = "routine_step";

    private int routineStep = 1; // 1 = Read, 2 = Reflect, 3 = Pray, 4 = Done
    private TextView stepIndicatorRead;
    private TextView stepIndicatorReflect;
    private TextView stepIndicatorPray;
    private TextView tvRoutineStepTitle;
    private TextView tvRoutineStepContent;
    private Button btnRoutineNext;
    private LinearLayout routineInlineContainer;
    private Button btnBeginRoutine;

    // Progress State
    private int streakCount = 0;
    private int xpCount = 0;
    private int progressPercent = 0;
    private TextView tvProgressSubtitle;
    private ProgressBar progressBarDaily;

    // Dynamic Section Containers
    private LinearLayout sermonsCarouselContent;
    private LinearLayout announcementsContainer;
    private LinearLayout eventsContainer;

    // Cached Data
    private HomeData cachedHomeData;

    // Daily Cornerstone Scripture Views
    private TextView tvCornerstoneHeader;
    private TextView tvCornerstoneBody;
    private Button btnCornerstoneShare;
    private Button btnCornerstoneReadPassage;
    private String currentCornerstoneRef = "2 Samuel 5:20";
    private String currentCornerstoneText = "And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters.";

    public HomeUiBinder(@NonNull Context context, @NonNull HomeViewModel homeViewModel) {
        this(context, homeViewModel, null, null);
    }

    public HomeUiBinder(@NonNull Context context,
                        @NonNull HomeViewModel homeViewModel,
                        @Nullable BibleViewModel bibleViewModel) {
        this(context, homeViewModel, bibleViewModel, null);
    }

    public HomeUiBinder(@NonNull Context context,
                        @NonNull HomeViewModel homeViewModel,
                        @Nullable BibleViewModel bibleViewModel,
                        @Nullable HomeUiListener listener) {
        this.context = context;
        this.homeViewModel = homeViewModel;
        this.bibleViewModel = bibleViewModel != null
                ? bibleViewModel
                : ViewModelFactory.getInstance(context).createBibleViewModel();
        this.listener = listener;
        if (this.context != null) {
            String uid = resolveUserId();
            this.streakCount = GamificationStore.getStreak(this.context, uid, 0);
            this.xpCount = GamificationStore.getSpiritualXp(this.context, uid, 0);
            try {
                this.routineStep = this.context.getSharedPreferences("perazim_routine", Context.MODE_PRIVATE).getInt(KEY_ROUTINE_STEP, 1);
            } catch (Exception ignored) {}
            syncRoutineStateWithGamificationStore();
        }
    }

    private String resolveUserId() {
        if (context == null) return "guest";
        try {
            SessionManager sm = SessionManager.getInstance(context);
            if (sm != null && sm.getActiveUserId() != null && !sm.getActiveUserId().trim().isEmpty()) {
                return sm.getActiveUserId();
            }
        } catch (Exception ignored) {}
        return "guest";
    }

    private void syncRoutineStateWithGamificationStore() {
        if (context == null) return;
        String uid = resolveUserId();
        boolean completedToday = GamificationStore.isRoutineCompletedToday(context, uid);
        if (completedToday) {
            routineStep = 4;
            progressPercent = 100;
        } else if (routineStep >= 4) {
            routineStep = 1;
            progressPercent = 0;
        }
    }

    public static View createView(@NonNull Context context,
                                  @NonNull HomeViewModel homeViewModel,
                                  @Nullable BibleViewModel bibleViewModel,
                                  @Nullable HomeUiListener listener) {
        HomeUiBinder binder = new HomeUiBinder(context, homeViewModel, bibleViewModel, listener);
        return binder.buildView();
    }

    public static HomeUiBinder bind(@NonNull ViewGroup rootContainer,
                                    @NonNull HomeViewModel homeViewModel,
                                    @Nullable BibleViewModel bibleViewModel,
                                    @Nullable HomeUiListener listener) {
        HomeUiBinder binder = new HomeUiBinder(rootContainer.getContext(), homeViewModel, bibleViewModel, listener);
        binder.bind(rootContainer);
        return binder;
    }

    public View buildView() {
        if (rootScrollView == null) {
            initUi();
        }
        refreshFeed();
        return rootScrollView;
    }

    public ScrollView bind() {
        return (ScrollView) buildView();
    }

    public void bind(@NonNull ViewGroup container) {
        View view = buildView();
        container.removeAllViews();
        container.addView(view);
    }

    public View getRootView() {
        return rootScrollView;
    }

    // =========================================================================
    // UI INITIALIZATION & LAYOUT BUILDING
    // =========================================================================

    private void initUi() {
        rootScrollView = new ScrollView(context);
        rootScrollView.setFillViewport(true);
        rootScrollView.setBackground(PerazimTheme.createBackgroundGradient());

        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(dp(16), dp(16), dp(16), dp(32));

        // 1. Quiet Sanctuary Welcome Wordmark with Official Logo & Illustrated Quiet-Dark Header
        FrameLayout headerContainer = new FrameLayout(context);
        LinearLayout.LayoutParams lpHeader = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(140));
        lpHeader.setMargins(0, 0, 0, dp(14));
        headerContainer.setLayoutParams(lpHeader);

        Bitmap homeBgBmp = loadAssetBitmap("backgrounds/bg_home_dawn_cross.png", 720);
        if (homeBgBmp != null) {
            headerContainer.setBackground(PerazimTheme.createCardGradient(dp(16), dp(1)));
            headerContainer.setClipToOutline(true);

            ImageView ivHomeBg = new ImageView(context);
            ivHomeBg.setImageBitmap(homeBgBmp);
            ivHomeBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            headerContainer.addView(ivHomeBg, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            View bottomScrim = new View(context);
            GradientDrawable scrimGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#2A0F33")});
            bottomScrim.setBackground(scrimGrad);
            headerContainer.addView(bottomScrim, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            headerContainer.setBackground(PerazimTheme.createHeroGradient(dp(16), dp(1)));
        }

        LinearLayout headerBanner = new LinearLayout(context);
        headerBanner.setOrientation(LinearLayout.HORIZONTAL);
        headerBanner.setGravity(Gravity.CENTER_VERTICAL);
        headerBanner.setPadding(dp(14), dp(14), dp(14), dp(14));
        headerBanner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        ImageView churchLogo = new ImageView(context);
        churchLogo.setImageResource(org.perazimchurch.app.R.drawable.ic_perazim_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(44), dp(44));
        lpLogo.setMargins(0, 0, dp(12), 0);
        churchLogo.setLayoutParams(lpLogo);
        headerBanner.addView(churchLogo);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvChurchHeader = new TextView(context);
        tvChurchHeader.setText("PERAZIM MISSION CHURCH");
        tvChurchHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvChurchHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchHeader.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvChurchHeader.setLetterSpacing(0.04f);
        tvChurchHeader.setPadding(0, 0, 0, dp(2));
        titleCol.addView(tvChurchHeader);

        TextView tvSubGreeting = new TextView(context);
        tvSubGreeting.setText("The Place of Great Breakthrough · Embu Headquarters");
        tvSubGreeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSubGreeting.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        titleCol.addView(tvSubGreeting);

        headerBanner.addView(titleCol);
        headerContainer.addView(headerBanner);
        contentContainer.addView(headerContainer);

        // 2. Single "Today" Card (Hero Darkened Gradient)
        View todayCard = createTodayCard();
        contentContainer.addView(todayCard);

        // 3. Four Quiet Doors (2x2 Grid on Surface Gradient)
        View doorsGrid = createFourDoorsGrid();
        contentContainer.addView(doorsGrid);

        // 4. Optional Single Announcement Line at bottom
        announcementsContainer = new LinearLayout(context);
        announcementsContainer.setOrientation(LinearLayout.VERTICAL);
        announcementsContainer.setPadding(0, dp(8), 0, 0);
        contentContainer.addView(announcementsContainer);

        rootScrollView.addView(contentContainer);
    }

    private View createTodayCard() {
        LinearLayout card = new LinearLayout(context);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(PerazimTheme.createHeroGradient(dp(16), dp(1)));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        // Header: Date + "DAILY VERSE"
        LinearLayout dateRow = new LinearLayout(context);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        dateRow.setPadding(0, 0, 0, dp(6));

        String dateStr = new SimpleDateFormat("EEEE, MMMM d", Locale.ROOT).format(new Date());
        TextView tvDate = new TextView(context);
        tvDate.setText(dateStr.toUpperCase(Locale.ROOT) + " · DAILY VERSE");
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvDate.setTypeface(Typeface.DEFAULT_BOLD);
        tvDate.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvDate.setLetterSpacing(0.04f);
        dateRow.addView(tvDate);
        card.addView(dateRow);

        // Verse Reference (e.g. 2 Samuel 5:20)
        tvCornerstoneHeader = new TextView(context);
        tvCornerstoneHeader.setText(currentCornerstoneRef);
        tvCornerstoneHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvCornerstoneHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvCornerstoneHeader.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvCornerstoneHeader.setPadding(0, 0, 0, dp(6));
        card.addView(tvCornerstoneHeader);

        // Verse Body (up to 3-4 lines with ellipsis)
        tvCornerstoneBody = new TextView(context);
        tvCornerstoneBody.setText("“" + currentCornerstoneText + "”");
        tvCornerstoneBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvCornerstoneBody.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvCornerstoneBody.setMaxLines(4);
        tvCornerstoneBody.setEllipsize(TextUtils.TruncateAt.END);
        tvCornerstoneBody.setLineSpacing(dp(2), 1.15f);
        tvCornerstoneBody.setPadding(0, 0, 0, dp(12));
        card.addView(tvCornerstoneBody);

        // Routine Inline Container (Surface 2 Gradient)
        routineInlineContainer = new LinearLayout(context);
        routineInlineContainer.setOrientation(LinearLayout.VERTICAL);
        routineInlineContainer.setPadding(dp(12), dp(10), dp(12), dp(10));
        routineInlineContainer.setBackground(PerazimTheme.createSurface2Gradient(dp(10), dp(1)));
        routineInlineContainer.setVisibility(View.GONE);

        tvRoutineStepTitle = new TextView(context);
        tvRoutineStepTitle.setText("Step 1 of 3: Read (30s)");
        tvRoutineStepTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvRoutineStepTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvRoutineStepTitle.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        routineInlineContainer.addView(tvRoutineStepTitle);

        tvRoutineStepContent = new TextView(context);
        tvRoutineStepContent.setText("“As waters break out, the LORD has broken out against my enemies before me.” — 2 Samuel 5:20");
        tvRoutineStepContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvRoutineStepContent.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvRoutineStepContent.setPadding(0, dp(4), 0, dp(10));
        tvRoutineStepContent.setLineSpacing(dp(2), 1.15f);
        routineInlineContainer.addView(tvRoutineStepContent);

        btnRoutineNext = new Button(context);
        btnRoutineNext.setText("Next Step →");
        btnRoutineNext.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnRoutineNext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRoutineNext.setTypeface(Typeface.DEFAULT_BOLD);
        btnRoutineNext.setBackground(PerazimTheme.createActivePillGradient(dp(8)));
        btnRoutineNext.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnRoutineNext.setOnClickListener(v -> advanceReflectionStep());
        routineInlineContainer.addView(btnRoutineNext);

        card.addView(routineInlineContainer);

        // ONE Primary Orange Button (Muted/Darkened Gradient)
        btnBeginRoutine = new Button(context);
        syncRoutineStateWithGamificationStore();
        updateBeginRoutineButtonText();
        btnBeginRoutine.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnBeginRoutine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnBeginRoutine.setTypeface(Typeface.DEFAULT_BOLD);
        btnBeginRoutine.setBackground(PerazimTheme.createPrimaryOrangeGradient(dp(10)));
        btnBeginRoutine.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtn.setMargins(0, dp(8), 0, 0);
        btnBeginRoutine.setLayoutParams(lpBtn);
        btnBeginRoutine.setOnClickListener(v -> toggleOrAdvanceRoutine());
        card.addView(btnBeginRoutine);

        return card;
    }

    private View createFourDoorsGrid() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        container.setLayoutParams(lp);

        TextView tvSection = new TextView(context);
        tvSection.setText("SANCTUARY DOORS");
        tvSection.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSection.setTypeface(Typeface.DEFAULT_BOLD);
        tvSection.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvSection.setLetterSpacing(0.04f);
        tvSection.setPadding(dp(2), 0, 0, dp(8));
        container.addView(tvSection);

        // Row 1: Bible & Sermons
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, 0, 0, dp(8));
        row1.addView(createDoorCard(org.perazimchurch.app.R.drawable.ic_christian_bible, "Bible", "Read scripture", v -> parseAndOpenScripture(currentCornerstoneRef)));
        View sep1 = new View(context);
        sep1.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 0));
        row1.addView(sep1);
        row1.addView(createDoorCard(org.perazimchurch.app.R.drawable.ic_christian_candle, "Sermons", "Recent messages", v -> {
            if (listener != null) listener.onNavigateToTab(1);
        }));
        container.addView(row1);

        // Row 2: Worship & Prayer
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(createDoorCard(org.perazimchurch.app.R.drawable.ic_christian_lyre, "Worship", "Hymns & praise", v -> {
            if (listener != null) listener.onNavigateToTab(2);
        }));
        View sep2 = new View(context);
        sep2.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 0));
        row2.addView(sep2);
        row2.addView(createDoorCard(org.perazimchurch.app.R.drawable.ic_christian_praying_hands, "Prayer", "Share & intercede", v -> {
            if (listener != null) listener.onNavigateToTab(3);
        }));
        container.addView(row2);

        return container;
    }

    private View createDoorCard(int iconRes, String title, String subtitle, View.OnClickListener onClickListener) {
        LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, dp(12), PerazimTheme.COLOR_STROKE, dp(1));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(10), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        card.setLayoutParams(lp);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(PerazimTheme.COLOR_PURPLE_SOFT, PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(22), dp(22));
        lpIcon.setMargins(0, 0, dp(10), 0);
        icon.setLayoutParams(lpIcon);
        card.addView(icon);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitle);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvSub.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvSub.setMaxLines(1);
        textCol.addView(tvSub);
        card.addView(textCol);

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(org.perazimchurch.app.R.drawable.ic_chevron_right);
        chevron.setColorFilter(PerazimTheme.COLOR_PURPLE_MUTED, PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams lpChev = new LinearLayout.LayoutParams(dp(14), dp(14));
        chevron.setLayoutParams(lpChev);
        card.addView(chevron);

        card.setOnClickListener(onClickListener);
        return card;
    }

    private void toggleOrAdvanceRoutine() {
        if (routineInlineContainer == null) return;
        if (routineStep >= 4) {
            Toast.makeText(context, "Routine completed for today. God's grace abounds!", Toast.LENGTH_SHORT).show();
            return;
        }
        routineInlineContainer.setVisibility(View.VISIBLE);
        updateRoutineView();
    }

    private void updateBeginRoutineButtonText() {
        if (btnBeginRoutine == null) return;
        if (routineStep >= 4) {
            btnBeginRoutine.setText("Routine completed today ✓");
            btnBeginRoutine.setEnabled(true);
        } else if (routineStep > 1) {
            btnBeginRoutine.setText("Continue routine (Step " + routineStep + " of 3)");
            btnBeginRoutine.setEnabled(true);
        } else {
            btnBeginRoutine.setText("Begin 3-minute routine");
            btnBeginRoutine.setEnabled(true);
        }
    }

    private void advanceReflectionStep() {
        if (routineStep == 1) {
            routineStep = 2;
            progressPercent = 33;
            saveRoutineState(null);
            updateRoutineView();
        } else if (routineStep == 2) {
            routineStep = 3;
            progressPercent = 66;
            saveRoutineState(null);
            updateRoutineView();
        } else if (routineStep == 3) {
            btnRoutineNext.setEnabled(false);
            btnRoutineNext.setText("Saving...");

            String reflectionId = (cachedHomeData != null && cachedHomeData.getTodayReflection() != null)
                    ? cachedHomeData.getTodayReflection().getId()
                    : "refl_day_1";

            homeViewModel.completeDailyReflection(reflectionId, () -> {
                routineStep = 4;
                saveRoutineState(null);
                String uid = resolveUserId();
                boolean isNewDay = GamificationStore.recordDailyRoutineCompleted(context, uid, streakCount + 1);
                if (isNewDay) {
                    streakCount += 1;
                    if (listener != null) {
                        listener.onStreakUpdated(streakCount);
                    }
                    awardXp(20);
                }
                progressPercent = 100;
                updateRoutineView();
                if (isNewDay) {
                    Toast.makeText(context, "Daily reflection completed. +20 XP awarded.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Routine already completed today.", Toast.LENGTH_SHORT).show();
                }
            }, error -> {
                btnRoutineNext.setEnabled(true);
                btnRoutineNext.setText("Complete Routine");
                Toast.makeText(context, "Could not complete reflection: " + (error != null ? error.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateRoutineView() {
        syncRoutineStateWithGamificationStore();
        updateBeginRoutineButtonText();

        Reflection refl = (cachedHomeData != null) ? cachedHomeData.getTodayReflection() : null;

        if (routineStep == 1) {
            if (tvRoutineStepTitle != null) tvRoutineStepTitle.setText("Step 1 of 3: Read Today's Scripture (30s)");
            String text = (refl != null && !TextUtils.isEmpty(refl.getPassageText()))
                    ? refl.getPassageText()
                    : "“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.” — 2 Samuel 5:20";
            if (tvRoutineStepContent != null) tvRoutineStepContent.setText(text);
            if (btnRoutineNext != null) {
                btnRoutineNext.setText("Proceed to Reflection →");
                btnRoutineNext.setEnabled(true);
            }
        } else if (routineStep == 2) {
            if (tvRoutineStepTitle != null) tvRoutineStepTitle.setText("Step 2 of 3: Meditate & Reflect (60s)");
            String comm = (refl != null && !TextUtils.isEmpty(refl.getCommentary()))
                    ? refl.getCommentary()
                    : "When David faced the Philistine host in the Valley of Rephaim, he inquired of the Lord. God burst forth like waters through a shattered dam. Whatever wall confronts you today, declare that the Lord of the Breakthrough is rising on your behalf.";
            if (tvRoutineStepContent != null) tvRoutineStepContent.setText(comm);
            if (btnRoutineNext != null) {
                btnRoutineNext.setText("Proceed to Prayer →");
                btnRoutineNext.setEnabled(true);
            }
        } else if (routineStep == 3) {
            if (tvRoutineStepTitle != null) tvRoutineStepTitle.setText("Step 3 of 3: Agreement Prayer (90s)");
            String prayer = (refl != null && !TextUtils.isEmpty(refl.getPrayerPrompt()))
                    ? refl.getPrayerPrompt()
                    : "Heavenly Father, Lord of Baal-Perazim, thank You that You are the God who breaks forth. Break forth upon every obstacle standing in my path. I receive Your breakthrough today. In Jesus' name, Amen.";
            if (tvRoutineStepContent != null) tvRoutineStepContent.setText(prayer);
            if (btnRoutineNext != null) {
                btnRoutineNext.setText("Complete Routine");
                btnRoutineNext.setEnabled(true);
            }
        } else if (routineStep >= 4) {
            if (tvRoutineStepTitle != null) tvRoutineStepTitle.setText("Daily Routine Complete");
            if (tvRoutineStepContent != null) tvRoutineStepContent.setText("“Thy blessing is upon thy people. Selah.” — Psalm 3:8\nYou completed today's 3-minute breakthrough routine and maintained your " + streakCount + "-day streak.");
            if (btnRoutineNext != null) {
                btnRoutineNext.setText("Completed ✓");
                btnRoutineNext.setEnabled(false);
            }
        }
    }

    // =========================================================================
    // SECTION 5: BISHOP SERMONS CAROUSEL / CARDS
    // =========================================================================

    private View createBishopSermonsShelf() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(6), 0, dp(14));

        TextView tvHeader = new TextView(context);
        tvHeader.setText("🎙️ BISHOP DR. DAVID MUTWERI · RECENT SERMONS");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHeader.setPadding(0, 0, 0, dp(8));
        container.addView(tvHeader);

        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);

        sermonsCarouselContent = new LinearLayout(context);
        sermonsCarouselContent.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(sermonsCarouselContent);
        container.addView(hsv);

        populateFallbackSermons();
        return container;
    }

    private void renderSermonCards(@NonNull List<Sermon> sermons) {
        if (sermonsCarouselContent == null) return;
        sermonsCarouselContent.removeAllViews();

        for (final Sermon sermon : sermons) {
            LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(12), 0);
            card.setLayoutParams(lp);

            // Series Badge
            TextView tvSeries = new TextView(context);
            tvSeries.setText(sermon.getSeriesName() != null ? sermon.getSeriesName().toUpperCase() : "BREAKTHROUGH SERIES");
            tvSeries.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvSeries.setTypeface(Typeface.DEFAULT_BOLD);
            tvSeries.setTextColor(COLOR_ACCENT_ORANGE);
            tvSeries.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
            tvSeries.setPadding(dp(6), dp(2), dp(6), dp(2));
            card.addView(tvSeries);

            // Title
            TextView tvTitle = new TextView(context);
            tvTitle.setText(sermon.getTitle());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(COLOR_TEXT_DARK);
            tvTitle.setPadding(0, dp(6), 0, dp(2));
            card.addView(tvTitle);

            // Preacher & Scripture
            TextView tvPreacher = new TextView(context);
            tvPreacher.setText((sermon.getPreacher() != null ? sermon.getPreacher() : "Bishop Dr. David Mutweri")
                    + (sermon.getScriptureReference() != null ? " · " + sermon.getScriptureReference() : ""));
            tvPreacher.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvPreacher.setTextColor(COLOR_TEXT_MUTED);
            tvPreacher.setPadding(0, 0, 0, dp(10));
            card.addView(tvPreacher);

            // Actions Row: Play and Details
            LinearLayout btnRow = new LinearLayout(context);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnPlay = createSmallButton("▶ Play Sermon", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
            btnPlay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaySermon(sermon);
                }
                showSermonPlayerModal(sermon);
            });

            Button btnDetails = createSmallButton("ℹ Details", COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE);
            btnDetails.setOnClickListener(v -> showSermonDetailsModal(sermon));

            btnRow.addView(btnPlay);
            btnRow.addView(btnDetails);
            card.addView(btnRow);

            sermonsCarouselContent.addView(card);
        }
    }

    private void populateFallbackSermons() {
        List<Sermon> list = new ArrayList<>();
        list.add(new Sermon("sermon_1", "The Breakthrough of Baal-perazim", "Bishop Dr. David Mutweri",
                "2 Samuel 5:17–25", "Breakthrough Living", null, null, null, 2815, "September 2026",
                "How David mobilized the army of Israel and broke through the Valley of Rephaim.", false, 1420));
        list.add(new Sermon("sermon_2", "Waters Breaking Out: The Anatomy of Victory", "Bishop Dr. David Mutweri",
                "Isaiah 43:18–19", "Supernatural Acceleration", null, null, null, 3100, "August 2026",
                "Understanding the prophetic season of Perazim and advancing past stagnation.", false, 980));
        list.add(new Sermon("sermon_3", "Inquiring of the LORD Before the Battle", "Bishop Dr. David Mutweri",
                "1 Samuel 30:8", "Divine Guidance", null, null, null, 2640, "August 2026",
                "David's secret weapon was not spear or chariot, but divine communion with the Almighty.", false, 1120));
        renderSermonCards(list);
    }

    // =========================================================================
    // SECTION 6: CHURCH BULLETINS & ANNOUNCEMENTS
    // =========================================================================

    private View createAnnouncementsShelf() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(4), 0, dp(14));

        TextView tvHeader = new TextView(context);
        tvHeader.setText("📢 CHURCH BULLETINS & ANNOUNCEMENTS");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHeader.setPadding(0, 0, 0, dp(8));
        container.addView(tvHeader);

        announcementsContainer = new LinearLayout(context);
        announcementsContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(announcementsContainer);

        populateFallbackAnnouncements();
        return container;
    }

    private void renderAnnouncementCards(@NonNull List<Announcement> announcements) {
        if (announcementsContainer == null) return;
        announcementsContainer.removeAllViews();
        if (announcements.isEmpty()) return;

        final Announcement ann = announcements.get(0);
        LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, dp(12), PerazimTheme.COLOR_STROKE, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        TextView tvDate = new TextView(context);
        tvDate.setText(formatTimestamp(ann.getTimestamp()) + " · BULLETIN");
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvDate.setTypeface(Typeface.DEFAULT_BOLD);
        tvDate.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvDate.setLetterSpacing(0.04f);
        card.addView(tvDate);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(ann.getTitle());
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvTitle.setPadding(0, dp(2), 0, dp(2));
        card.addView(tvTitle);

        TextView tvBody = new TextView(context);
        tvBody.setText(ann.getBody());
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBody.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvBody.setMaxLines(2);
        tvBody.setEllipsize(TextUtils.TruncateAt.END);
        tvBody.setLineSpacing(dp(2), 1.15f);
        card.addView(tvBody);

        card.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(ann.getTitle())
                    .setMessage((ann.getBody() != null ? ann.getBody() : "") + "\n\n— Perazim Mission Church")
                    .setPositiveButton("Close", null)
                    .show();
        });

        announcementsContainer.addView(card);
    }

    private void populateFallbackAnnouncements() {
        List<Announcement> list = new ArrayList<>();
        list.add(new Announcement("ann_1", "Grand Annual Breakthrough Convention 2026",
                "Theme: 'The Breaker Goes Before Them' (Micah 2:13). Prepare for 4 days of apostolic power and divine visitation with Bishop Dr. David Mutweri.",
                "Bishop Dr. David Mutweri", System.currentTimeMillis(), "URGENT", false));
        list.add(new Announcement("ann_2", "Mwea & Rombo Campus Joint Workers Retreat",
                "All department heads, deacons, and pastoral staff will convene this Saturday at 8:00 AM at Embu Headquarters.",
                "Pastoral Secretariat", System.currentTimeMillis() - 86400000L, "NORMAL", false));
        renderAnnouncementCards(list);
    }

    // =========================================================================
    // SECTION 7: UPCOMING FELLOWSHIP EVENTS SHELF
    // =========================================================================

    private View createUpcomingEventsShelf() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(4), 0, dp(14));

        TextView tvHeader = new TextView(context);
        tvHeader.setText("🗓️ UPCOMING CHURCH EVENTS & GATHERINGS");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHeader.setPadding(0, 0, 0, dp(8));
        container.addView(tvHeader);

        eventsContainer = new LinearLayout(context);
        eventsContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(eventsContainer);

        populateFallbackEvents();
        return container;
    }

    private void renderEventCards(@NonNull List<Event> events) {
        if (eventsContainer == null) return;
        eventsContainer.removeAllViews();

        for (final Event ev : events) {
            LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(lp);

            LinearLayout tagRow = new LinearLayout(context);
            tagRow.setOrientation(LinearLayout.HORIZONTAL);
            tagRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tagCat = new TextView(context);
            tagCat.setText(ev.getCategory() != null ? ev.getCategory().toUpperCase() : "CHURCH GATHERING");
            tagCat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tagCat.setTypeface(Typeface.DEFAULT_BOLD);
            tagCat.setTextColor(COLOR_ACCENT_ORANGE);
            tagCat.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
            tagCat.setPadding(dp(6), dp(2), dp(6), dp(2));
            tagRow.addView(tagCat);

            View sp = new View(context);
            tagRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1.0f));

            TextView tvDate = new TextView(context);
            tvDate.setText(ev.getDateText() != null ? ev.getDateText() : "Upcoming");
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvDate.setTextColor(COLOR_TEXT_MUTED);
            tagRow.addView(tvDate);
            card.addView(tagRow);

            TextView tvTitle = new TextView(context);
            tvTitle.setText(ev.getTitle());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(COLOR_TEXT_DARK);
            tvTitle.setPadding(0, dp(4), 0, dp(2));
            card.addView(tvTitle);

            TextView tvDesc = new TextView(context);
            tvDesc.setText(ev.getDescription());
            tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvDesc.setTextColor(COLOR_TEXT_MUTED);
            tvDesc.setLineSpacing(dp(2), 1.15f);
            tvDesc.setPadding(0, 0, 0, dp(8));
            card.addView(tvDesc);

            final Button btnRsvp = new Button(context);
            btnRsvp.setText("🗓️ RSVP / Set Reminder (+5 XP)");
            btnRsvp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btnRsvp.setTypeface(Typeface.DEFAULT_BOLD);
            btnRsvp.setTextColor(COLOR_PRIMARY_PURPLE);
            btnRsvp.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
            btnRsvp.setPadding(dp(8), dp(4), dp(8), dp(4));
            btnRsvp.setOnClickListener(v -> {
                awardXp(5);
                btnRsvp.setText("✓ RSVP Confirmed!");
                btnRsvp.setEnabled(false);
                Toast.makeText(context, "Reminder set for " + ev.getTitle() + "! (+5 XP)", Toast.LENGTH_SHORT).show();
            });
            card.addView(btnRsvp);

            eventsContainer.addView(card);
        }
    }

    private void populateFallbackEvents() {
        List<Event> list = new ArrayList<>();
        list.add(new Event("ev_1", "Community Food Drive & Mercy Outreach",
                "Distributing food packages and warm clothing to vulnerable families across Embu County.",
                "Saturday, 9:00 AM • Embu Outreach Center", 0, "Embu Outreach Center", "PMC_MAIN", "Missions & Mercy", null));
        list.add(new Event("ev_2", "Sunday Breakthrough Celebration Service",
                "Two powerful services of Word, Breakthrough Worship, and prophetic ministry led by Bishop Dr. David Mutweri.",
                "Sunday, 9:00 AM & 11:15 AM • Central Sanctuary", 0, "Central Sanctuary", "PMC_MAIN", "Worship & Word", null));
        list.add(new Event("ev_3", "Youth & Young Adults Fellowship (Ignite)",
                "Worship, dynamic discussion on career & faith, and games with the young adults.",
                "Friday, 5:30 PM • Youth Chapel", 0, "Youth Chapel", "PMC_MAIN", "NextGen", null));
        renderEventCards(list);
    }

    // =========================================================================
    // DATA FEED REFRESH FROM HomeViewModel
    // =========================================================================

    public void refreshFeed() {
        loadDailyVerse();
        homeViewModel.loadHomeFeed(homeData -> {
            if (homeData != null) {
                cachedHomeData = homeData;
                streakCount = homeData.getStreak();
                xpCount = homeData.getSpiritualXp();

                if (homeData.getRecentSermons() != null && !homeData.getRecentSermons().isEmpty()) {
                    renderSermonCards(homeData.getRecentSermons());
                }
                if (homeData.getActiveAnnouncements() != null && !homeData.getActiveAnnouncements().isEmpty()) {
                    renderAnnouncementCards(homeData.getActiveAnnouncements());
                }
                if (homeData.getUpcomingEvents() != null && !homeData.getUpcomingEvents().isEmpty()) {
                    renderEventCards(homeData.getUpcomingEvents());
                }
                updateRoutineView();
            }
        });
    }

    /**
     * Queries Room for today's curated daily scripture (YYYY-MM-DD).
     * If present and active, binds it to the cornerstone scripture card.
     * If absent or inactive, retains the default 2 Samuel 5:20 scripture.
     */
    private void loadDailyVerse() {
        new Thread(() -> {
            try {
                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
                PerazimDatabase db = PerazimDatabase.getInstance(context);
                DailyVerseEntity verse = db.dailyVerseDao().getVerseForDate(todayStr);
                if (verse != null && verse.isActive() && !TextUtils.isEmpty(verse.getText())) {
                    if (rootScrollView != null) {
                        rootScrollView.post(() -> applyDailyVerse(verse.getReference(), verse.getText()));
                    }
                }
            } catch (Exception e) {
                android.util.Log.w("HomeUiBinder", "Could not load daily verse from Room", e);
            }
        }).start();
    }

    private void applyDailyVerse(String ref, String text) {
        currentCornerstoneRef = !TextUtils.isEmpty(ref) ? ref : "2 Samuel 5:20";
        currentCornerstoneText = !TextUtils.isEmpty(text) ? text : "";
        if (tvCornerstoneHeader != null) {
            tvCornerstoneHeader.setText(currentCornerstoneRef);
        }
        if (tvCornerstoneBody != null) {
            tvCornerstoneBody.setText("“" + currentCornerstoneText.trim() + "”");
        }
    }

    private void parseAndOpenScripture(String ref) {
        if (TextUtils.isEmpty(ref)) {
            openScriptureReader("2SA", 5);
            return;
        }
        String lower = ref.toLowerCase(Locale.ROOT);
        if (lower.contains("2 samuel") || lower.contains("2sa")) {
            openScriptureReader("2SA", 5);
        } else if (lower.contains("john") || lower.contains("jhn")) {
            openScriptureReader("JHN", 3);
        } else if (lower.contains("psalm") || lower.contains("psa")) {
            openScriptureReader("PSA", 23);
        } else {
            openScriptureReader("2SA", 5);
        }
    }


    // =========================================================================
    // MODAL DIALOGS: GIVING, PRAYER REQUEST, SERMON DETAILS & PLAYER
    // =========================================================================

    public void showGivingModal() {
        GivingDialog.show(context);
    }

    public void showPrayerRequestModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🙏 Submit Prayer Request");
        builder.setMessage("Your petition will be submitted to Bishop Dr. David Mutweri and the Perazim Intercessory Prayer Team.\n\nPastoral Care: (+254) 0710 772 227 · info@perazimchurch.org");

        final EditText etPrayer = new EditText(context);
        etPrayer.setHint("Write your petition or praise report here...");
        etPrayer.setMinLines(3);
        etPrayer.setGravity(Gravity.TOP | Gravity.START);
        etPrayer.setPadding(dp(14), dp(12), dp(14), dp(12));
        builder.setView(etPrayer);

        builder.setPositiveButton("Submit (+15 XP)", (dialog, which) -> {
            String text = etPrayer.getText().toString().trim();
            if (!text.isEmpty()) {
                awardXp(15);
                Toast.makeText(context, "🙏 Prayer request submitted to Bishop and Intercessors! (+15 XP)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Please write a petition before submitting.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.setNeutralButton("📞 Pastoral Office", (dialog, which) -> dialPhoneNumber("+254710772227"));
        builder.show();
    }

    private void showSermonPlayerModal(@NonNull Sermon sermon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackgroundColor(COLOR_WHITE);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(sermon.getTitle());
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_DARK);
        box.addView(tvTitle);

        TextView tvSpeaker = new TextView(context);
        tvSpeaker.setText((sermon.getPreacher() != null ? sermon.getPreacher() : "Bishop Dr. David Mutweri")
                + (sermon.getScriptureReference() != null ? " · " + sermon.getScriptureReference() : ""));
        tvSpeaker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSpeaker.setTextColor(COLOR_TEXT_MUTED);
        tvSpeaker.setPadding(0, dp(2), 0, dp(14));
        box.addView(tvSpeaker);

        ProgressBar pb = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(35);
        box.addView(pb);

        TextView tvTimes = new TextView(context);
        tvTimes.setText("03:42 / 28:15 · Data Saver: 32kbps AAC");
        tvTimes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTimes.setTextColor(COLOR_PRIMARY_PURPLE);
        tvTimes.setPadding(0, dp(4), 0, dp(14));
        box.addView(tvTimes);

        Button btnClose = new Button(context);
        btnClose.setText("Close Player");
        btnClose.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        btnClose.setTextColor(COLOR_PRIMARY_PURPLE);
        box.addView(btnClose);

        builder.setView(box);
        final AlertDialog dlg = builder.create();
        btnClose.setOnClickListener(v -> dlg.dismiss());
        dlg.show();
    }

    private void showSermonDetailsModal(@NonNull Sermon sermon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🎙️ Sermon Details");

        StringBuilder sb = new StringBuilder();
        sb.append(sermon.getTitle()).append("\n\n");
        sb.append("Preacher: ").append(sermon.getPreacher() != null ? sermon.getPreacher() : "Bishop Dr. David Mutweri").append("\n");
        if (sermon.getScriptureReference() != null) {
            sb.append("Scripture: ").append(sermon.getScriptureReference()).append("\n");
        }
        if (sermon.getSeriesName() != null) {
            sb.append("Series: ").append(sermon.getSeriesName()).append("\n");
        }
        if (sermon.getDatePreached() != null) {
            sb.append("Date: ").append(sermon.getDatePreached()).append("\n");
        }
        if (sermon.getDescription() != null) {
            sb.append("\nDescription:\n").append(sermon.getDescription());
        }

        builder.setMessage(sb.toString());
        builder.setPositiveButton("Play Now", (dialog, which) -> showSermonPlayerModal(sermon));
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void openScriptureReader(@NonNull String bookId, int chapter) {
        if (listener != null) {
            listener.onOpenScripture(bookId, chapter);
        }
        BibleReaderDialog.showPassage(context, bibleViewModel, bookId, chapter);
    }

    private void scrollToRoutine() {
        if (rootScrollView != null) {
            rootScrollView.post(() -> rootScrollView.smoothScrollTo(0, dp(400)));
        }
    }

    private void awardXp(int amount) {
        xpCount += amount;
        if (listener != null) {
            listener.onXpAwarded(amount, xpCount);
        }
    }

    private void dialPhoneNumber(String phone) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Call: " + phone, Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePassageToWhatsApp(String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(Intent.createChooser(intent, "Share Scripture"));
        } catch (Exception e) {
            Toast.makeText(context, "Scripture copied for sharing", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTimestamp(long ts) {
        if (ts <= 0) return "Recent";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            return sdf.format(new Date(ts));
        } catch (Exception e) {
            return "Recent";
        }
    }

    // =========================================================================
    // UI GRAPHICS & DRAWABLE HELPERS
    // =========================================================================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private LinearLayout createCard(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        LinearLayout card = new LinearLayout(context);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable drawable;
        if (bgColor == PerazimTheme.COLOR_SURFACE) {
            drawable = PerazimTheme.createCardGradient(dp(cornerRadiusDp), dp(borderWidthDp));
        } else if (bgColor == PerazimTheme.COLOR_SURFACE_2) {
            drawable = PerazimTheme.createSurface2Gradient(dp(cornerRadiusDp), dp(borderWidthDp));
        } else {
            drawable = new GradientDrawable();
            drawable.setColor(bgColor);
            drawable.setCornerRadius(dp(cornerRadiusDp));
            if (borderWidthDp > 0) {
                drawable.setStroke(dp(borderWidthDp), borderColor);
            }
        }
        card.setBackground(drawable);
        return card;
    }

    private Button createSmallButton(String text, int bgColor, int textColor) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(8), dp(6), dp(8), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private Drawable createPillBg(int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(16));
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
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        if (bitmap == null) return null;
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

    public void saveRoutineState(android.os.Bundle outState) {
        if (outState != null) outState.putInt(KEY_ROUTINE_STEP, routineStep);
        if (context != null) {
            try {
                context.getSharedPreferences("perazim_routine", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_ROUTINE_STEP, routineStep)
                        .commit();
            } catch (Exception e) {
                android.util.Log.e("HomeUiBinder", "Failed to save routine state", e);
            }
        }
    }
    public void restoreRoutineState(android.os.Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            routineStep = savedInstanceState.getInt(KEY_ROUTINE_STEP, 1);
        } else {
            // Restore from durable SP if Bundle not present (process death, fresh start with preserved state)
            try {
                if (context != null) {
                    routineStep = context.getSharedPreferences("perazim_routine", android.content.Context.MODE_PRIVATE).getInt(KEY_ROUTINE_STEP, 1);
                }
            } catch (Exception e) { /* ignore */ }
        }
        syncRoutineStateWithGamificationStore();
    }
}
