package org.perazimchurch.app.presentation.ui;

import org.perazimchurch.app.ui.theme.AppDialogs;

import org.perazimchurch.app.config.ChurchConfig;

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
import org.perazimchurch.app.data.local.entity.UserEntity;
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
import org.perazimchurch.app.R;
import org.perazimchurch.app.ui.theme.FrostPane;
import org.perazimchurch.app.ui.theme.GlassKit;
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
    public static int COLOR_PRIMARY_PURPLE() { return PerazimTheme.COLOR_PRIMARY_PURPLE; } // #681A7D
    public static int COLOR_PURPLE_DARK() { return PerazimTheme.COLOR_PURPLE_DEEP; }     // #2D0938
    public static int COLOR_PURPLE_DEEPEST() { return PerazimTheme.COLOR_OBSIDIAN; }        // #140319
    public static int COLOR_PURPLE_SHADOW() { return PerazimTheme.COLOR_PURPLE_SHADOW; }   // #450E53
    public static int COLOR_PURPLE_TINT() { return PerazimTheme.COLOR_PURPLE_TINT; }     // #F5ECF7
    public static int COLOR_PURPLE_SOFT() { return PerazimTheme.COLOR_PURPLE_SOFT; }     // #C896D8
    public static int COLOR_BORDER_GREY() { return PerazimTheme.COLOR_BORDER_GREY; }     // #E7D5EC
    public static int COLOR_BG_NEUTRAL() { return PerazimTheme.COLOR_BG_NEUTRAL; }      // #FAF7FB
    public static int COLOR_WHITE() { return PerazimTheme.COLOR_WHITE; }           // #FFFFFF
    public static int COLOR_ACCENT_ORANGE() { return PerazimTheme.COLOR_ACCENT_ORANGE; }   // #E17D2F
    public static int COLOR_ORANGE_DARK() { return PerazimTheme.COLOR_ORANGE_DARK; }     // #78350A
    public static int COLOR_ORANGE_SHADOW() { return PerazimTheme.COLOR_ORANGE_SHADOW; }   // #98450B
    public static int COLOR_ORANGE_BORDER() { return PerazimTheme.COLOR_ORANGE_BORDER; }   // #F9DFCC
    public static int COLOR_ORANGE_TINT() { return PerazimTheme.COLOR_ORANGE_TINT; }     // #FDF5EF
    public static final int COLOR_TEXT_DARK      = Color.parseColor("#1A1225");
    public static final int COLOR_TEXT_MUTED     = Color.parseColor("#725B78");

    public interface HomeUiListener {
        void onStreakUpdated(int streak);
        void onXpAwarded(int xpAwarded, int totalXp);
        void onPlaySermon(@NonNull Sermon sermon);
        void onOpenScripture(@NonNull String bookId, int chapter);
        default void onNavigateToTab(int tabIndex) {}
        /** Opens the Perazim Guide (on-phone assistant). Only offered when the build enables it. */
        default void onOpenAiGuide() {}
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
    private TextView btnRoutineNext;
    private LinearLayout routineInlineContainer;
    private TextView btnBeginRoutine;
    private TextView tvGreeting;
    private TextView tvContinueStep;

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
        contentContainer.setPadding(dp(16), dp(8), dp(16), dp(24));

        // 1. Greeting, as in the original build: a short line, then the church name under it
        LinearLayout greeting = new LinearLayout(context);
        greeting.setOrientation(LinearLayout.VERTICAL);
        tvGreeting = GlassKit.text(context, buildGreeting(), 20, true, GlassKit.text());
        greeting.addView(tvGreeting);
        TextView church = GlassKit.text(context, "Perazim Mission Church · Embu Headquarters", 12, false, GlassKit.muted());
        greeting.addView(church, GlassKit.matchWrap(context, 2));
        contentContainer.addView(GlassKit.keep(greeting));
        refreshGreetingNameAsync();

        // 2. Header photo, 140dp as in the original build
        contentContainer.addView(createHeaderBanner(), GlassKit.matchHeight(context, 140, 14));

        // 3. Continue today's reading
        contentContainer.addView(createTodayCard(), GlassKit.matchWrap(context, 16));

        // 4. Sanctuary doors (2x2) and the Bible games row
        contentContainer.addView(createDoorsGrid(), GlassKit.matchWrap(context, 16));

        // 5. Announcements, below the fold
        announcementsContainer = new LinearLayout(context);
        announcementsContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.addView(announcementsContainer, GlassKit.matchWrap(context, 14));

        rootScrollView.addView(contentContainer);
    }

    /** The original header: the dawn-cross photo, the church logo and name along the bottom, 14dp corners. */
    private View createHeaderBanner() {
        FrameLayout header = new FrameLayout(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(16));
        shape.setColor(PerazimTheme.COLOR_SURFACE_SOLID);
        header.setBackground(shape);
        header.setClipToOutline(true);

        Bitmap bg = loadAssetBitmap("backgrounds/bg_home_dawn_cross.png", 720);
        if (bg != null) {
            ImageView iv = new ImageView(context);
            iv.setImageBitmap(bg);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            header.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            View scrim = new View(context);
            scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#2A0F33")}));
            header.addView(scrim, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        LinearLayout banner = new LinearLayout(context);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(14), dp(14), dp(14), dp(14));

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.ic_perazim_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(44), dp(44));
        lpLogo.setMargins(0, 0, dp(12), 0);
        banner.addView(logo, lpLogo);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.addView(GlassKit.text(context, "PERAZIM MISSION CHURCH", 17, true, Color.WHITE));
        titleCol.addView(GlassKit.text(context, "The Place of Great Breakthrough", 12, false,
                Color.parseColor("#E6D8EE")), GlassKit.matchWrap(context, 2));
        banner.addView(titleCol);

        header.addView(GlassKit.keep(banner), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return GlassKit.keep(header);
    }

    private String buildGreeting() {
        return greetingText(null);
    }

    private String greetingText(String firstName) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String part = hour < 12 ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");
        return firstName == null ? part : part + ", " + firstName;
    }

    /** Reads the member's first name from the database off the main thread, then updates the greeting. */
    private void refreshGreetingNameAsync() {
        new Thread(() -> {
            String firstName = null;
            try {
                UserEntity user = SessionManager.getInstance(context).getCurrentUser();
                if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                    firstName = user.getName().trim().split("\\s+")[0];
                }
            } catch (RuntimeException ignored) {
                // Keep the plain greeting if the name cannot be read.
            }
            final String resolved = firstName;
            if (tvGreeting != null) {
                tvGreeting.post(() -> tvGreeting.setText(greetingText(resolved)));
            }
        }, "home-greeting").start();
    }

    private View createTodayCard() {
        LinearLayout card = GlassKit.card(context, 16, 16);

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = GlassKit.label(context, "CONTINUE TODAY'S READING");
        top.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvContinueStep = GlassKit.text(context, "", 11, true, GlassKit.accent());
        top.addView(tvContinueStep);
        card.addView(GlassKit.keep(top));

        tvCornerstoneHeader = GlassKit.text(context, currentCornerstoneRef, 17, true, GlassKit.text());
        card.addView(tvCornerstoneHeader, GlassKit.matchWrap(context, 8));

        tvCornerstoneBody = GlassKit.text(context, "“" + currentCornerstoneText + "”", 12, false, GlassKit.body());
        tvCornerstoneBody.setLineSpacing(0f, 1.3f);
        tvCornerstoneBody.setMaxLines(4);
        tvCornerstoneBody.setEllipsize(TextUtils.TruncateAt.END);
        tvCornerstoneBody.setOnClickListener(v -> parseAndOpenScripture(currentCornerstoneRef));
        card.addView(tvCornerstoneBody, GlassKit.matchWrap(context, 6));

        homeStepBar = GlassKit.stepBar(context, 3, 0);
        card.addView(homeStepBar, GlassKit.matchWrap(context, 12));

        btnBeginRoutine = GlassKit.orangeButton(context, "PICK UP WHERE YOU LEFT OFF", 13);
        btnBeginRoutine.setOnClickListener(v -> toggleOrAdvanceRoutine());
        card.addView(btnBeginRoutine, GlassKit.matchHeight(context, 46, 14));
        if (org.perazimchurch.app.BuildConfig.OFFLINE_QWEN_ENABLED) {
            TextView btnAskGuide = GlassKit.glassButton(context, "ASK PERAZIM GUIDE", 13);
            btnAskGuide.setOnClickListener(v -> {
                if (listener != null) listener.onOpenAiGuide();
            });
            card.addView(btnAskGuide, GlassKit.matchHeight(context, 46, 10));
        }

        syncRoutineStateWithGamificationStore();
        updateBeginRoutineButtonText();
        return card;
    }

    /**
     * Home's lower section, laid out like the original build: a "Sanctuary doors" label, a 2x2 grid of compact
     * door cards, then a full-width row for the free Bible games.
     */
    private View createDoorsGrid() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        container.addView(GlassKit.keep(GlassKit.label(context, "SANCTUARY DOORS")), GlassKit.matchWrap(context, 0));

        container.addView(doorRow(
                createDoorCard(R.drawable.ic_christian_bible, "Bible", "Read scripture",
                        v -> parseAndOpenScripture(currentCornerstoneRef)),
                createDoorCard(R.drawable.ic_christian_candle, "Sermons", "Recent messages",
                        v -> { if (listener != null) listener.onNavigateToTab(1); })),
                GlassKit.matchWrap(context, 8));
        container.addView(doorRow(
                createDoorCard(R.drawable.ic_christian_lyre, "Worship", "Hymns and praise",
                        v -> { if (listener != null) listener.onNavigateToTab(2); }),
                createDoorCard(R.drawable.ic_christian_praying_hands, "Prayer", "Share and intercede",
                        v -> { if (listener != null) listener.onNavigateToTab(3); })),
                GlassKit.matchWrap(context, 8));

        container.addView(createGamesRow(), GlassKit.matchWrap(context, 8));
        return container;
    }

    /** Two equal cards side by side with an 8dp gap. */
    private LinearLayout doorRow(View left, View right) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(new View(context), new LinearLayout.LayoutParams(dp(8), 0));
        row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    /** One compact door: a 36dp icon square, a 13sp title and an 11sp subtitle on one line. */
    private View createDoorCard(int iconRes, String title, String subtitle, View.OnClickListener onClick) {
        LinearLayout card = GlassKit.card(context, 14, 12);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(GlassKit.iconBadge(context, iconRes, GlassKit.iconSquare(context, 10), 36, 18),
                new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(10), 0, 0, 0);
        TextView tvTitle = GlassKit.text(context, title, 13, true, GlassKit.text());
        textCol.addView(tvTitle);
        TextView tvSub = GlassKit.text(context, subtitle, 11, false, GlassKit.muted());
        tvSub.setMaxLines(1);
        tvSub.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(tvSub);
        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        card.setClickable(true);
        card.setOnClickListener(onClick);
        return card;
    }

    /** The free Bible games, as a full-width row under the doors. */
    private View createGamesRow() {
        LinearLayout row = GlassKit.card(context, 14, 12);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(GlassKit.iconBadge(context, R.drawable.gl_star, GlassKit.iconSquare(context, 10), 36, 18),
                new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView tv = GlassKit.text(context, "Bible games: free and offline", 13, true, GlassKit.text());
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpText.leftMargin = dp(10);
        row.addView(tv, lpText);
        row.setClickable(true);
        row.setOnClickListener(v -> {
            if (context instanceof android.app.Activity) {
                org.perazimchurch.app.presentation.ui.games.GamesHubDialog.show((android.app.Activity) context);
            }
        });
        return row;
    }

    private void toggleOrAdvanceRoutine() {
        showDailyReading();
    }

    private static final String[] STEP_NAMES = {
            "Read Today's Scripture · 30s",
            "Meditate and Reflect · 60s",
            "Agreement Prayer · 90s"
    };

    private void updateBeginRoutineButtonText() {
        int step = Math.max(routineStep, 1);
        if (tvContinueStep != null) {
            tvContinueStep.setText(step >= 4 ? "Done today" : "Step " + step + " of 3");
        }
        if (homeStepBar != null) {
            GlassKit.setSteps(homeStepBar, Math.min(step, 3));
        }
        if (btnBeginRoutine != null) {
            if (step >= 4) {
                btnBeginRoutine.setText("READ TODAY'S PASSAGE AGAIN");
            } else if (step == 1) {
                btnBeginRoutine.setText("START TODAY'S READING");
            } else {
                btnBeginRoutine.setText("PICK UP WHERE YOU LEFT OFF");
            }
        }
    }

    // =========================================================================
    // DAILY READING SCREEN (mockup board "Daily reading")
    // =========================================================================

    private android.app.Dialog readingDialog;
    private LinearLayout readingStepBar;
    private LinearLayout homeStepBar;
    private TextView tvReadingRef;
    private TextView tvReadingPassage;
    private TextView tvReadingReflection;
    private TextView tvReadingPrayer;

    private void showDailyReading() {
        if (readingDialog != null && readingDialog.isShowing()) {
            return;
        }
        android.app.Dialog dialog = new android.app.Dialog(context, org.perazimchurch.app.ui.theme.AppDialogs.windowTheme()) {
            @Override
            public void show() {
                super.show();
                org.perazimchurch.app.ui.theme.GlassPolish.install(this);
            }
        };
        FrameLayout root = new FrameLayout(context);
        root.setBackground(PerazimTheme.createBackgroundGradient());

        ScrollView scroll = new ScrollView(context);
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, dp(96));

        // Top bar: back circle, centred title, balancing space
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(16), dp(20), 0);
        FrameLayout back = GlassKit.circleIcon(context, R.drawable.gl_back, 40, 20, "Back");
        back.setOnClickListener(v -> dialog.dismiss());
        bar.addView(back);
        TextView title = GlassKit.text(context, "Daily reading", 15, true, GlassKit.text());
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(new View(context), new LinearLayout.LayoutParams(dp(40), dp(40)));
        col.addView(GlassKit.keep(bar));

        LinearLayout intro = new LinearLayout(context);
        intro.setOrientation(LinearLayout.VERTICAL);
        intro.setPadding(dp(20), dp(14), dp(20), 0);
        intro.addView(GlassKit.label(context, "CONTINUE TODAY'S READING"));
        tvReadingRef = GlassKit.text(context, currentCornerstoneRef, 24, true, GlassKit.text());
        intro.addView(tvReadingRef, GlassKit.matchWrap(context, 4));
        readingStepBar = GlassKit.stepBar(context, 3, 0);
        intro.addView(readingStepBar, GlassKit.matchWrap(context, 10));
        tvRoutineStepTitle = GlassKit.text(context, "", 12, true, GlassKit.accent());
        intro.addView(tvRoutineStepTitle, GlassKit.matchWrap(context, 6));
        col.addView(GlassKit.keep(intro));

        LinearLayout card = GlassKit.card(context, 28, 16);
        tvReadingPassage = addReadingSection(card, "SCRIPTURE PASSAGE", true);
        tvReadingReflection = addReadingSection(card, "MORNING REFLECTION", false);
        tvReadingPrayer = addReadingSection(card, "PRAYER POINTS", false);
        TextView action = addReadingSection(card, "ACTION STEPS", false);
        action.setText("Identify one intimidating challenge in your schedule today. Declare aloud Micah 2:13 over it three times before stepping out into your day.");
        LinearLayout.LayoutParams lpCard = GlassKit.matchWrap(context, 14);
        lpCard.leftMargin = dp(16);
        lpCard.rightMargin = dp(16);
        col.addView(card, lpCard);

        scroll.addView(col);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView next = GlassKit.orangeButton(context, "PROCEED", 13);
        next.setOnClickListener(v -> {
            if (routineStep >= 4) {
                dialog.dismiss();
            } else {
                advanceReflectionStep();
            }
        });
        btnRoutineNext = next;
        FrameLayout.LayoutParams lpNext = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50), Gravity.BOTTOM);
        lpNext.setMargins(dp(16), 0, dp(16), dp(24));
        root.addView(next, lpNext);

        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            readingDialog = null;
            updateBeginRoutineButtonText();
        });
        readingDialog = dialog;
        updateRoutineView();
        dialog.show();
    }

    private TextView addReadingSection(LinearLayout card, String heading, boolean first) {
        TextView h = GlassKit.text(context, heading, 10, true, GlassKit.accent());
        h.setLetterSpacing(0.1f);
        card.addView(h, GlassKit.matchWrap(context, first ? 0 : 14));
        TextView body = GlassKit.text(context, "", 13, false, GlassKit.text());
        body.setLineSpacing(0f, 1.35f);
        card.addView(body, GlassKit.matchWrap(context, 4));
        return body;
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
            btnRoutineNext.setText("SAVING…");

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
                btnRoutineNext.setEnabled(true);
                updateRoutineView();
                if (isNewDay) {
                    org.perazimchurch.app.ui.components.FeedbackManager fm =
                            org.perazimchurch.app.ui.components.FeedbackManager.get(context);
                    if (readingDialog != null && readingDialog.getWindow() != null) {
                        fm.celebrateIn(readingDialog.getWindow());
                        if (context instanceof android.app.Activity) {
                            fm.reward((android.app.Activity) context);
                        }
                    } else if (context instanceof android.app.Activity) {
                        fm.milestone((android.app.Activity) context);
                    }
                    Toast.makeText(context, "Daily reflection completed. +20 XP awarded.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Routine already completed today.", Toast.LENGTH_SHORT).show();
                }
            }, error -> {
                btnRoutineNext.setEnabled(true);
                btnRoutineNext.setText("COMPLETE ROUTINE");
                Toast.makeText(context, "Could not complete reflection: " + (error != null ? error.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateRoutineView() {
        syncRoutineStateWithGamificationStore();
        updateBeginRoutineButtonText();

        Reflection refl = (cachedHomeData != null) ? cachedHomeData.getTodayReflection() : null;
        String ref = (refl != null && !TextUtils.isEmpty(refl.getScriptureReference()))
                ? refl.getScriptureReference() : currentCornerstoneRef;
        String passage = (refl != null && !TextUtils.isEmpty(refl.getPassageText()))
                ? refl.getPassageText()
                : "“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.” — 2 Samuel 5:20";
        String reflection = (refl != null && !TextUtils.isEmpty(refl.getCommentary()))
                ? refl.getCommentary()
                : "When David faced the Philistine host in the Valley of Rephaim, he inquired of the Lord. God burst forth like waters through a shattered dam. Whatever wall confronts you today, declare that the Lord of the Breakthrough is rising on your behalf.";
        String prayer = (refl != null && !TextUtils.isEmpty(refl.getPrayerPrompt()))
                ? refl.getPrayerPrompt()
                : "Heavenly Father, Lord of Baal-Perazim, thank You that You are the God who breaks forth. Break forth upon every obstacle standing in my path. I receive Your breakthrough today. In Jesus' name, Amen.";

        if (tvReadingRef != null) tvReadingRef.setText(ref);
        if (tvReadingPassage != null) tvReadingPassage.setText(passage);
        if (tvReadingReflection != null) tvReadingReflection.setText(reflection);
        if (tvReadingPrayer != null) tvReadingPrayer.setText(prayer);

        int step = Math.max(routineStep, 1);
        if (readingStepBar != null) {
            GlassKit.setSteps(readingStepBar, Math.min(step, 3));
        }
        if (tvRoutineStepTitle != null) {
            tvRoutineStepTitle.setText(step >= 4
                    ? "Complete · " + streakCount + "-day streak"
                    : "Step " + step + " of 3: " + STEP_NAMES[step - 1]);
        }
        if (btnRoutineNext != null) {
            if (step == 1) {
                btnRoutineNext.setText("PROCEED TO REFLECTION");
            } else if (step == 2) {
                btnRoutineNext.setText("PROCEED TO PRAYER");
            } else if (step == 3) {
                btnRoutineNext.setText("COMPLETE ROUTINE");
            } else {
                btnRoutineNext.setText("DONE FOR TODAY");
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
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE());
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
            LinearLayout card = createCard(COLOR_WHITE(), dp(14), COLOR_BORDER_GREY(), dp(1));
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(12), 0);
            card.setLayoutParams(lp);

            // Series Badge
            TextView tvSeries = new TextView(context);
            tvSeries.setText(sermon.getSeriesName() != null ? sermon.getSeriesName().toUpperCase() : "BREAKTHROUGH SERIES");
            tvSeries.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvSeries.setTypeface(Typeface.DEFAULT_BOLD);
            tvSeries.setTextColor(COLOR_ACCENT_ORANGE());
            tvSeries.setBackground(createPillBg(COLOR_ORANGE_TINT(), COLOR_ORANGE_BORDER()));
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

            Button btnPlay = createSmallButton("▶ Play Sermon", COLOR_PRIMARY_PURPLE(), COLOR_WHITE());
            btnPlay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaySermon(sermon);
                }
                showSermonPlayerModal(sermon);
            });

            Button btnDetails = createSmallButton("ℹ Details", COLOR_PURPLE_TINT(), COLOR_PRIMARY_PURPLE());
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
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE());
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
        LinearLayout card = GlassKit.card(context, 24, 14);
        card.addView(GlassKit.label(context, formatTimestamp(ann.getTimestamp()).toUpperCase(Locale.ROOT) + " · BULLETIN"));
        TextView tvTitle = GlassKit.text(context, ann.getTitle(), 16, true, GlassKit.text());
        card.addView(tvTitle, GlassKit.matchWrap(context, 4));
        TextView tvBody = GlassKit.text(context, ann.getBody(), 12, false, GlassKit.body());
        tvBody.setMaxLines(2);
        tvBody.setEllipsize(TextUtils.TruncateAt.END);
        tvBody.setLineSpacing(0f, 1.3f);
        card.addView(tvBody, GlassKit.matchWrap(context, 4));
        card.setClickable(true);

        card.setOnClickListener(v -> {
            AppDialogs.builder(context)
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
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE());
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
            LinearLayout card = createCard(COLOR_WHITE(), dp(14), COLOR_BORDER_GREY(), dp(1));
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
            tagCat.setTextColor(COLOR_ACCENT_ORANGE());
            tagCat.setBackground(createPillBg(COLOR_ORANGE_TINT(), COLOR_ORANGE_BORDER()));
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
            btnRsvp.setTextColor(COLOR_PRIMARY_PURPLE());
            btnRsvp.setBackground(createPillBg(COLOR_PURPLE_TINT(), COLOR_BORDER_GREY()));
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
        AlertDialog.Builder builder = AppDialogs.builder(context);
        builder.setTitle("🙏 Submit Prayer Request");
        builder.setMessage("Your petition will be submitted to Bishop Dr. David Mutweri and the Perazim Intercessory Prayer Team.\n\nPastoral Care: " + ChurchConfig.PASTORAL_PHONE_DISPLAY + " · info@perazimchurch.org");

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
        builder.setNeutralButton("📞 Pastoral Office", (dialog, which) -> dialPhoneNumber("" + ChurchConfig.PASTORAL_PHONE_E164 + ""));
        builder.show();
    }

    private void showSermonPlayerModal(@NonNull Sermon sermon) {
        AlertDialog.Builder builder = AppDialogs.builder(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackgroundColor(COLOR_WHITE());

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
        tvTimes.setTextColor(COLOR_PRIMARY_PURPLE());
        tvTimes.setPadding(0, dp(4), 0, dp(14));
        box.addView(tvTimes);

        Button btnClose = new Button(context);
        btnClose.setText("Close Player");
        btnClose.setBackground(createPillBg(COLOR_PURPLE_TINT(), COLOR_BORDER_GREY()));
        btnClose.setTextColor(COLOR_PRIMARY_PURPLE());
        box.addView(btnClose);

        builder.setView(box);
        final AlertDialog dlg = builder.create();
        btnClose.setOnClickListener(v -> dlg.dismiss());
        dlg.show();
    }

    private void showSermonDetailsModal(@NonNull Sermon sermon) {
        AlertDialog.Builder builder = AppDialogs.builder(context);
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
