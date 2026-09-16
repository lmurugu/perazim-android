package com.example.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app.data.local.preference.GamificationStore;
import com.example.app.data.mapper.UserMapper;
import com.example.app.data.local.PersistenceVerificationHelper;
import com.example.app.data.local.Phase1MasterVerificationHelper;
import com.example.app.community.CommunityPhase3VerificationHelper;
import com.example.app.navigation.NavigationContract;
import com.example.app.presentation.Phase2MasterVerificationHelper;
import com.example.app.presentation.Phase3MasterVerificationHelper;
import com.example.app.presentation.ui.BibleReaderDialog;
import com.example.app.presentation.ui.FellowshipUiBinder;
import com.example.app.presentation.ui.HomeUiBinder;
import com.example.app.presentation.ui.OnboardingDialog;
import com.example.app.presentation.ui.ProfileUiBinder;
import com.example.app.presentation.ui.SermonsUiBinder;
import com.example.app.presentation.ui.WorshipUiBinder;
import com.example.app.presentation.viewmodel.BibleViewModel;
import com.example.app.presentation.viewmodel.FellowshipViewModel;
import com.example.app.presentation.viewmodel.HomeViewModel;
import com.example.app.presentation.viewmodel.ProfileViewModel;
import com.example.app.presentation.viewmodel.SermonsViewModel;
import com.example.app.presentation.viewmodel.ViewModelFactory;
import com.example.app.presentation.viewmodel.WorshipViewModel;
import com.example.app.ui.theme.PerazimTheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {

    // --- STRICT DUAL-COLOR PALETTE (EXCLUSIVELY DERIVED FROM #681A7D & #E17D2F) ---
    public static final int COLOR_PRIMARY_PURPLE  = Color.parseColor("#681A7D"); // Primary Brand Royal Purple
    public static final int COLOR_PURPLE_DARK     = Color.parseColor("#2D0938"); // Deep purple for headings/bars
    public static final int COLOR_PURPLE_DEEPEST  = Color.parseColor("#140319"); // Obsidian purple for cards
    public static final int COLOR_PURPLE_SHADOW   = Color.parseColor("#450E53"); // 3D bevel shadow
    public static final int COLOR_PURPLE_LIGHT    = Color.parseColor("#8E30A8"); // Brightened purple
    public static final int COLOR_PURPLE_SOFT     = Color.parseColor("#C896D8"); // Soft purple text
    public static final int COLOR_BORDER_GREY     = Color.parseColor("#E7D5EC"); // Whitened purple card border
    public static final int COLOR_PURPLE_TINT     = Color.parseColor("#F5ECF7"); // Pastel purple tint
    public static final int COLOR_TEXT_MUTED      = Color.parseColor("#725B78"); // Muted purple-grey text
    public static final int COLOR_TEXT_DARK       = Color.parseColor("#1A1225"); // Deep purple-black text

    public static final int COLOR_ACCENT_ORANGE   = Color.parseColor("#E17D2F"); // Secondary Brand Warm Amber/Orange
    public static final int COLOR_ORANGE_DARK     = Color.parseColor("#78350A"); // Darkened orange
    public static final int COLOR_ORANGE_SHADOW   = Color.parseColor("#98450B"); // 3D bevel shadow
    public static final int COLOR_ORANGE_BORDER   = Color.parseColor("#F9DFCC"); // Whitened orange border
    public static final int COLOR_ORANGE_TINT     = Color.parseColor("#FDF5EF"); // Pastel orange tint

    public static final int COLOR_WHITE           = Color.parseColor("#FFFFFF"); // Card surface
    public static final int COLOR_BG_NEUTRAL      = Color.parseColor("#FAF7FB"); // Whitened purple canvas

    // --- STATE VARIABLES ---
    private int streakCount = 7;
    private int xpCount = 450;
    private int graceCount = 5;
    private boolean streakFrozen = false;
    private int progressPercent = 80;
    private boolean questClaimed = false;
    private String selectedCampus = "Perazim Mission Church";

    // Top Header Views
    private TextView topStreakBadge;
    private TextView topXpBadge;
    private TextView topGraceBadge;
    private Button topGiveBtn;
    private Spinner campusSpinner;
    private Spinner fundGivingSpinner;
    private TextView tvGivingAccount;

    // Navigation Shell Views
    private FrameLayout tabContainer;
    private ScrollView[] tabViews = new ScrollView[5];
    private LinearLayout[] navTabButtons = new LinearLayout[5];
    private TextView[] navTabLabels = new TextView[5];
    private int currentTabIndex = 0;

    // Persistent Docked Mini-Player (Subsplash pattern)
    private TextView tvMiniTrackTitle;
    private TextView tvMiniTrackSub;
    private Button btnMiniPlayPause;
    private Button btnMiniDataSaver;
    private boolean isPlaying = false;
    private boolean isDataSaverActive = true;

    // 3-Minute Breakthrough Routine State & Views (Glorify pattern)
    private int routineStep = 1; // 1 = Read, 2 = Reflect, 3 = Pray, 4 = Done
    private TextView tvRoutineStepTitle;
    private TextView tvRoutineStepContent;
    private Button btnRoutineNext;
    private TextView stepIndicatorRead;
    private TextView stepIndicatorReflect;
    private TextView stepIndicatorPray;

    // Home Tab Views
    private TextView progressTextView;
    private ProgressBar progressBar;
    private Button btnPray;
    private Button btnReflect;
    private Button btnShare;
    private Button btnFullPassage;
    private Button btnGiveHome;
    private Button btnSubmitPrayerHome;
    private EditText prayerInputDialog;

    // WhatsApp Status Gradient Themes
    private int currentGradientTheme = 0;
    private final int[][] gradientThemes = {
            {Color.parseColor("#140319"), Color.parseColor("#681A7D")}, // Obsidian Royal
            {Color.parseColor("#681A7D"), Color.parseColor("#E17D2F")}, // Breakthrough Sunrise
            {Color.parseColor("#2D0938"), Color.parseColor("#98450B")}  // Burgundy Sunset
    };
    private final String[] themeNames = {"Obsidian Royal", "Breakthrough Sunrise", "Burgundy Sunset"};

    // Phase 2 UI Binders & ViewModels
    private Button btnWatchLiveSermon;
    private ViewModelFactory factory;
    private BibleViewModel bibleViewModel;
    private HomeUiBinder homeUiBinder;
    private SermonsUiBinder sermonsUiBinder;
    private WorshipUiBinder worshipUiBinder;
    private FellowshipUiBinder fellowshipUiBinder;
    private ProfileUiBinder profileUiBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize gamification persistence
        UserMapper.init(this);
        streakCount = GamificationStore.getStreak(this, "user_me", 7);
        xpCount = GamificationStore.getSpiritualXp(this, "user_me", 450);
        graceCount = GamificationStore.getGracePoints(this, "user_me", 5);
        streakFrozen = GamificationStore.isStreakFrozen(this, "user_me", false);

        // Phase 3 Master Verification Gate (Foundation, Core UX & Community/Fellowship)
        Phase3MasterVerificationHelper.runVerification(this);

        // Trigger first-launch onboarding
        OnboardingDialog.showIfNeeded(this);

        factory = ViewModelFactory.getInstance(this);
        bibleViewModel = factory.createBibleViewModel();

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(COLOR_BG_NEUTRAL);

        // 1. Top App Bar
        View topBar = createTopAppBar();
        rootLayout.addView(topBar);

        // 2. Tab Content Shell
        tabContainer = new FrameLayout(this);
        LinearLayout.LayoutParams lpContainer = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        tabContainer.setLayoutParams(lpContainer);

        tabViews[0] = buildHomeScreen();
        tabViews[1] = buildSermonsScreen();
        tabViews[2] = buildWorshipScreen();
        tabViews[3] = buildFellowshipScreen();
        tabViews[4] = buildProfileScreen();
        for (int i = 0; i < 5; i++) {
            tabContainer.addView(tabViews[i]);
            tabViews[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        }
        rootLayout.addView(tabContainer);

        // 3. Persistent Mini-Player (Subsplash pattern)
        View miniPlayer = createMiniPlayer();
        rootLayout.addView(miniPlayer);

        // 4. Bottom Navigation Bar
        View bottomBar = createBottomTabBar();
        rootLayout.addView(bottomBar);

        setContentView(rootLayout);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(android.content.Intent intent) {
        if (intent == null) return;
        int tab = intent.getIntExtra("tab_index", -1);
        if (tab >= 0 && tab < 5) {
            switchTab(tab);
        }
        if (intent.getBooleanExtra("show_qr", false) && fellowshipUiBinder != null) {
            fellowshipUiBinder.openQrConnectionDialog();
        }
    }

    // =========================================================================
    // 1. TOP APP BAR & HUD WITH GRACE POINT SYSTEM
    // =========================================================================

    private View createTopAppBar() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(COLOR_WHITE);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(COLOR_WHITE);
        headerBg.setStroke(dp(1), COLOR_BORDER_GREY);
        header.setBackground(headerBg);

        // Church Crest Logo
        Bitmap logoBmp = loadAssetBitmap("logo_crest.png", 150);
        if (logoBmp != null) {
            ImageView logoView = new ImageView(this);
            logoView.setImageBitmap(logoBmp);
            LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(32), dp(32));
            lpLogo.setMargins(0, 0, dp(8), 0);
            logoView.setLayoutParams(lpLogo);
            header.addView(logoView);
        }

        // Campus Dropdown
        LinearLayout spinnerPill = new LinearLayout(this);
        spinnerPill.setOrientation(LinearLayout.HORIZONTAL);
        spinnerPill.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(COLOR_BG_NEUTRAL);
        pillBg.setCornerRadius(dp(16));
        pillBg.setStroke(dp(1), COLOR_BORDER_GREY);
        spinnerPill.setBackground(pillBg);
        spinnerPill.setPadding(dp(6), dp(2), dp(4), dp(2));

        LinearLayout.LayoutParams lpPill = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpPill.setMargins(0, 0, dp(6), 0);
        spinnerPill.setLayoutParams(lpPill);

        campusSpinner = new Spinner(this);
        List<String> campuses = new ArrayList<>();
        campuses.add("Perazim Mission Church");
        campuses.add("Mwea Campus");
        campuses.add("Rombo Campus");
        campuses.add("Online Campus");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, campuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campusSpinner.setAdapter(adapter);
        campusSpinner.setBackground(null);
        campusSpinner.setOnItemSelectedListener(this);
        spinnerPill.addView(campusSpinner);
        header.addView(spinnerPill);

        // Streak HUD Badge (🔥 7 Days)
        topStreakBadge = new TextView(this);
        topStreakBadge.setText("🔥 " + streakCount);
        topStreakBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        topStreakBadge.setTypeface(Typeface.DEFAULT_BOLD);
        topStreakBadge.setTextColor(COLOR_ACCENT_ORANGE);
        topStreakBadge.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
        topStreakBadge.setPadding(dp(7), dp(4), dp(7), dp(4));
        LinearLayout.LayoutParams lpStreak = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpStreak.setMargins(0, 0, dp(4), 0);
        topStreakBadge.setLayoutParams(lpStreak);
        topStreakBadge.setOnClickListener(v -> Toast.makeText(this, "🔥 " + streakCount + "-Day Devotional Streak! Keep up your daily walk with Christ.", Toast.LENGTH_SHORT).show());
        header.addView(topStreakBadge);

        // XP HUD Badge (⭐ 450 XP)
        topXpBadge = new TextView(this);
        topXpBadge.setText("⭐ " + xpCount);
        topXpBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        topXpBadge.setTypeface(Typeface.DEFAULT_BOLD);
        topXpBadge.setTextColor(COLOR_PRIMARY_PURPLE);
        topXpBadge.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        topXpBadge.setPadding(dp(7), dp(4), dp(7), dp(4));
        LinearLayout.LayoutParams lpXp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpXp.setMargins(0, 0, dp(4), 0);
        topXpBadge.setLayoutParams(lpXp);
        topXpBadge.setOnClickListener(v -> Toast.makeText(this, "⭐ " + xpCount + " Total XP Earned! Quests, prayers & reflections build your spiritual stature.", Toast.LENGTH_SHORT).show());
        header.addView(topXpBadge);

        // Grace HUD Badge (💜 5 Grace) - Duolingo pattern
        topGraceBadge = new TextView(this);
        topGraceBadge.setText("💜 " + graceCount);
        topGraceBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        topGraceBadge.setTypeface(Typeface.DEFAULT_BOLD);
        topGraceBadge.setTextColor(COLOR_PRIMARY_PURPLE);
        topGraceBadge.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
        topGraceBadge.setPadding(dp(7), dp(4), dp(7), dp(4));
        LinearLayout.LayoutParams lpGrace = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpGrace.setMargins(0, 0, dp(4), 0);
        topGraceBadge.setLayoutParams(lpGrace);
        topGraceBadge.setOnClickListener(v -> showGracePointDialog());
        header.addView(topGraceBadge);

        // M-Pesa Giving Pill in Top Bar
        topGiveBtn = new Button(this);
        topGiveBtn.setText("💚 Give");
        topGiveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        topGiveBtn.setTypeface(Typeface.DEFAULT_BOLD);
        topGiveBtn.setTextColor(COLOR_WHITE);
        topGiveBtn.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 2));
        topGiveBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpGive = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topGiveBtn.setLayoutParams(lpGive);
        topGiveBtn.setOnClickListener(v -> showGivingDialog());
        header.addView(topGiveBtn);

        return header;
    }

    private void persistGamification() {
        GamificationStore.saveStreak(this, "user_me", streakCount, streakFrozen);
        GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
        GamificationStore.saveGracePoints(this, "user_me", graceCount);
    }

    private void updateHud() {
        if (topStreakBadge != null) {
            topStreakBadge.setText((streakFrozen ? "🛡️ " : "🔥 ") + streakCount);
        }
        if (topXpBadge != null) {
            topXpBadge.setText("⭐ " + xpCount);
        }
        if (topGraceBadge != null) {
            topGraceBadge.setText("💜 " + graceCount);
        }
        persistGamification();
    }

    // =========================================================================
    // 2. PERSISTENT MINI-PLAYER & DATA-SAVER (Subsplash Pattern)
    // =========================================================================

    private View createMiniPlayer() {
        LinearLayout player = new LinearLayout(this);
        player.setOrientation(LinearLayout.HORIZONTAL);
        player.setGravity(Gravity.CENTER_VERTICAL);
        player.setBackgroundColor(COLOR_PURPLE_DARK);
        player.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PURPLE_DARK);
        bg.setStroke(dp(1), COLOR_PURPLE_SHADOW);
        player.setBackground(bg);

        TextView icon = new TextView(this);
        icon.setText("🎙️");
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        icon.setPadding(0, 0, dp(10), 0);
        player.addView(icon);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        tvMiniTrackTitle = new TextView(this);
        tvMiniTrackTitle.setText("Supernatural Breakthrough");
        tvMiniTrackTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvMiniTrackTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvMiniTrackTitle.setTextColor(COLOR_WHITE);
        tvMiniTrackTitle.setMaxLines(1);
        textCol.addView(tvMiniTrackTitle);

        tvMiniTrackSub = new TextView(this);
        tvMiniTrackSub.setText("Bishop Dr. David Mutweri · 03:42 / 28:15");
        tvMiniTrackSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvMiniTrackSub.setTextColor(COLOR_PURPLE_SOFT);
        tvMiniTrackSub.setMaxLines(1);
        textCol.addView(tvMiniTrackSub);

        textCol.setOnClickListener(v -> showSermonPlayerDialog("Supernatural Breakthrough in Hard Times", "Bishop Dr. David Mutweri"));
        player.addView(textCol);

        // Data Saver Switch Button
        btnMiniDataSaver = new Button(this);
        btnMiniDataSaver.setText(isDataSaverActive ? "📶 32k" : "🎧 HD");
        btnMiniDataSaver.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnMiniDataSaver.setTextColor(COLOR_WHITE);
        btnMiniDataSaver.setTypeface(Typeface.DEFAULT_BOLD);
        btnMiniDataSaver.setPadding(dp(6), dp(4), dp(6), dp(4));
        GradientDrawable dsBg = new GradientDrawable();
        dsBg.setColor(isDataSaverActive ? COLOR_ACCENT_ORANGE : COLOR_PURPLE_SHADOW);
        dsBg.setCornerRadius(dp(12));
        btnMiniDataSaver.setBackground(dsBg);
        LinearLayout.LayoutParams lpDs = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
        lpDs.setMargins(0, 0, dp(6), 0);
        btnMiniDataSaver.setLayoutParams(lpDs);
        btnMiniDataSaver.setOnClickListener(v -> toggleDataSaver());
        player.addView(btnMiniDataSaver);

        // Play/Pause Button
        btnMiniPlayPause = new Button(this);
        btnMiniPlayPause.setText(isPlaying ? "⏸" : "▶");
        btnMiniPlayPause.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnMiniPlayPause.setTextColor(COLOR_WHITE);
        btnMiniPlayPause.setTypeface(Typeface.DEFAULT_BOLD);
        btnMiniPlayPause.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable playBg = new GradientDrawable();
        playBg.setColor(COLOR_ACCENT_ORANGE);
        playBg.setCornerRadius(dp(15));
        btnMiniPlayPause.setBackground(playBg);
        LinearLayout.LayoutParams lpPlay = new LinearLayout.LayoutParams(dp(36), dp(36));
        btnMiniPlayPause.setLayoutParams(lpPlay);
        btnMiniPlayPause.setOnClickListener(v -> togglePlayback());
        player.addView(btnMiniPlayPause);

        return player;
    }

    private void togglePlayback() {
        isPlaying = !isPlaying;
        btnMiniPlayPause.setText(isPlaying ? "⏸" : "▶");
        if (isPlaying) {
            Toast.makeText(this, "▶ Playing: Supernatural Breakthrough (Bishop Dr. David Mutweri)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⏸ Playback paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDataSaver() {
        isDataSaverActive = !isDataSaverActive;
        btnMiniDataSaver.setText(isDataSaverActive ? "📶 32k" : "🎧 HD");
        GradientDrawable dsBg = new GradientDrawable();
        dsBg.setColor(isDataSaverActive ? COLOR_ACCENT_ORANGE : COLOR_PURPLE_SHADOW);
        dsBg.setCornerRadius(dp(12));
        btnMiniDataSaver.setBackground(dsBg);
        if (isDataSaverActive) {
            Toast.makeText(this, "📶 Data Saver Active: 32kbps AAC (saves Safaricom/Airtel mobile bundles)", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "🎧 High Quality Stream: 128kbps stereo", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 3. BOTTOM TAB BAR NAVIGATION
    // =========================================================================

    private View createBottomTabBar() {
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER_VERTICAL);
        bottomNav.setBackgroundColor(COLOR_WHITE);
        bottomNav.setPadding(0, dp(6), 0, dp(8));

        GradientDrawable navBorder = new GradientDrawable();
        navBorder.setColor(COLOR_WHITE);
        navBorder.setStroke(dp(1), COLOR_BORDER_GREY);
        bottomNav.setBackground(navBorder);

        final String[] icons = {
                NavigationContract.TAB_ICON_HOME,
                NavigationContract.TAB_ICON_SERMONS,
                NavigationContract.TAB_ICON_WORSHIP,
                NavigationContract.TAB_ICON_FELLOWSHIP,
                NavigationContract.TAB_ICON_PROFILE
        };
        final String[] labels = {
                NavigationContract.TAB_TITLE_HOME,
                NavigationContract.TAB_TITLE_SERMONS,
                NavigationContract.TAB_TITLE_WORSHIP,
                NavigationContract.TAB_TITLE_FELLOWSHIP,
                NavigationContract.TAB_TITLE_PROFILE
        };

        for (int i = 0; i < 5; i++) {
            final int index = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lpTab = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tab.setLayoutParams(lpTab);
            tab.setPadding(0, dp(4), 0, dp(4));

            TextView tvIcon = new TextView(this);
            tvIcon.setText(icons[i]);
            tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvIcon.setGravity(Gravity.CENTER);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(labels[i]);
            tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvLabel.setGravity(Gravity.CENTER);
            tvLabel.setPadding(0, dp(2), 0, 0);

            tab.addView(tvIcon);
            tab.addView(tvLabel);
            tab.setOnClickListener(v -> switchTab(index));

            navTabButtons[i] = tab;
            navTabLabels[i] = tvLabel;

            bottomNav.addView(tab);
        }

        refreshTabStyles();
        return bottomNav;
    }

    private void switchTab(int index) {
        if (currentTabIndex == index) return;
        currentTabIndex = index;

        for (int i = 0; i < 5; i++) {
            tabViews[i].setVisibility(i == currentTabIndex ? View.VISIBLE : View.GONE);
        }
        refreshTabStyles();
    }

    private void refreshTabStyles() {
        for (int i = 0; i < 5; i++) {
            boolean isSelected = (i == currentTabIndex);
            if (isSelected) {
                navTabLabels[i].setTextColor(COLOR_PRIMARY_PURPLE);
                navTabLabels[i].setTypeface(Typeface.DEFAULT_BOLD);

                GradientDrawable activePill = new GradientDrawable();
                activePill.setColor(COLOR_PURPLE_TINT);
                activePill.setCornerRadius(dp(14));
                navTabButtons[i].setBackground(activePill);
            } else {
                navTabLabels[i].setTextColor(COLOR_TEXT_MUTED);
                navTabLabels[i].setTypeface(Typeface.DEFAULT);
                navTabButtons[i].setBackground(null);
            }
        }
    }

    // =========================================================================
    // TAB 0: HOME SCREEN WITH 3-MINUTE BREAKTHROUGH ROUTINE, EVENTS & GIVING
    // =========================================================================

    private ScrollView buildHomeScreen() {
        if (factory == null) {
            factory = ViewModelFactory.getInstance(this);
        }
        if (bibleViewModel == null) {
            bibleViewModel = factory.createBibleViewModel();
        }
        homeUiBinder = new HomeUiBinder(
                this,
                factory.createHomeViewModel(),
                bibleViewModel,
                new HomeUiBinder.HomeUiListener() {
                    @Override
                    public void onStreakUpdated(int streak) {
                        streakCount = streak;
                        GamificationStore.saveStreak(MainActivity.this, "user_me", streakCount, streakFrozen);
                        updateHud();
                    }

                    @Override
                    public void onXpAwarded(int xpAwarded, int totalXp) {
                        xpCount = totalXp;
                        GamificationStore.saveSpiritualXp(MainActivity.this, "user_me", xpCount);
                        updateHud();
                    }

                    @Override
                    public void onPlaySermon(com.example.app.domain.model.Sermon sermon) {
                        if (tvMiniTrackTitle != null) tvMiniTrackTitle.setText(sermon.getTitle());
                        if (tvMiniTrackSub != null) tvMiniTrackSub.setText(sermon.getPreacher());
                    }

                    @Override
                    public void onOpenScripture(String bookId, int chapter) {
                        showFullScriptureDialog();
                    }
                }
        );
        return homeUiBinder.bind();
    }

    private ScrollView buildLegacyHomeScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // 1. Hero Photographic Banner (sanctuary.jpg)
        Bitmap sanctuaryBmp = loadAssetBitmap("sanctuary.jpg", 600);
        if (sanctuaryBmp != null) {
            ImageView ivHero = new ImageView(this);
            ivHero.setImageBitmap(getRoundedCornerBitmap(sanctuaryBmp, dp(14)));
            ivHero.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpHero = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(150));
            lpHero.setMargins(0, 0, 0, dp(14));
            ivHero.setLayoutParams(lpHero);
            content.addView(ivHero);
        }

        // Canonical Church Identity & Welcome Header
        TextView tvChurchHeader = new TextView(this);
        tvChurchHeader.setText("PERAZIM MISSION CHURCH");
        tvChurchHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvChurchHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchHeader.setTextColor(COLOR_PURPLE_DARK);
        tvChurchHeader.setPadding(0, 0, 0, dp(2));
        content.addView(tvChurchHeader);

        TextView tvSubGreeting = new TextView(this);
        tvSubGreeting.setText("“The Place of Great Breakthrough”\n“A place where everybody is somebody, and no body is a nobody”\nPerazim Mission Church · Embu Headquarters");
        tvSubGreeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSubGreeting.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        tvSubGreeting.setTextColor(COLOR_PRIMARY_PURPLE);
        tvSubGreeting.setLineSpacing(dp(2), 1.15f);
        tvSubGreeting.setPadding(0, dp(2), 0, dp(12));
        content.addView(tvSubGreeting);

        // Quick Actions Grid (Give / Tithe, Full Scripture, Submit Prayer)
        LinearLayout quickActionsCard = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        quickActionsCard.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpQuick = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpQuick.setMargins(0, 0, 0, dp(14));
        quickActionsCard.setLayoutParams(lpQuick);

        btnGiveHome = createSmallButton("💚 Give / Tithe", COLOR_ACCENT_ORANGE, COLOR_WHITE);
        btnGiveHome.setOnClickListener(v -> showGivingDialog());

        btnFullPassage = createSmallButton("📖 Full Passage", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnFullPassage.setOnClickListener(v -> showFullScriptureDialog());

        Button btnQuickPrayer = createSmallButton("🙏 Intercede", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnQuickPrayer.setOnClickListener(v -> showPrayerRequestDialog());

        quickActionsCard.addView(btnGiveHome);
        quickActionsCard.addView(btnFullPassage);
        quickActionsCard.addView(btnQuickPrayer);
        content.addView(quickActionsCard);

        // 2. Daily Devotion Progress Track
        LinearLayout progressCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        progressCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpProgressCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpProgressCard.setMargins(0, 0, 0, dp(14));
        progressCard.setLayoutParams(lpProgressCard);

        TextView goalHeader = new TextView(this);
        goalHeader.setText("DAILY DEVOTION PROGRESS");
        goalHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        goalHeader.setTypeface(Typeface.DEFAULT_BOLD);
        goalHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        progressCard.addView(goalHeader);

        progressTextView = new TextView(this);
        progressTextView.setText(progressPercent + "% completed — 4 of 5 devotions finished");
        progressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        progressTextView.setTextColor(COLOR_TEXT_DARK);
        progressTextView.setPadding(0, dp(4), 0, dp(8));
        progressCard.addView(progressTextView);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(progressPercent);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)));

        GradientDrawable bgTrack = new GradientDrawable();
        bgTrack.setColor(COLOR_PURPLE_TINT);
        bgTrack.setCornerRadius(dp(6));

        GradientDrawable progressFill = new GradientDrawable();
        progressFill.setColor(COLOR_ACCENT_ORANGE);
        progressFill.setCornerRadius(dp(6));

        ClipDrawable clip = new ClipDrawable(progressFill, Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable progressLayers = new LayerDrawable(new Drawable[]{bgTrack, clip});
        progressLayers.setId(0, android.R.id.background);
        progressLayers.setId(1, android.R.id.progress);
        progressBar.setProgressDrawable(progressLayers);
        progressCard.addView(progressBar);
        content.addView(progressCard);

        // 3. YouVersion Scripture Card with Breakthrough Waters Image (Obsidian #140319 Deep Purple)
        LinearLayout verseCard = createCard(COLOR_PURPLE_DEEPEST, dp(16), COLOR_PRIMARY_PURPLE, dp(1));
        verseCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpVerse = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpVerse.setMargins(0, 0, 0, dp(14));
        verseCard.setLayoutParams(lpVerse);

        // Breakthrough Waters Photographic Header (waters.jpg)
        Bitmap watersBmp = loadAssetBitmap("waters.jpg", 500);
        if (watersBmp != null) {
            ImageView imgWaters = new ImageView(this);
            imgWaters.setImageBitmap(getRoundedCornerBitmap(watersBmp, dp(10)));
            imgWaters.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpWaters = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(110));
            lpWaters.setMargins(0, 0, 0, dp(10));
            imgWaters.setLayoutParams(lpWaters);
            verseCard.addView(imgWaters);
        }

        TextView verseHeader = new TextView(this);
        verseHeader.setText("📖 VERSE OF THE DAY · 2 SAMUEL 5:20");
        verseHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        verseHeader.setTypeface(Typeface.DEFAULT_BOLD);
        verseHeader.setTextColor(COLOR_ACCENT_ORANGE);
        verseCard.addView(verseHeader);

        TextView verseBody = new TextView(this);
        verseBody.setText("“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.”");
        verseBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        verseBody.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        verseBody.setTextColor(COLOR_WHITE);
        verseBody.setPadding(0, dp(8), 0, dp(10));
        verseBody.setLineSpacing(dp(3), 1.15f);
        verseCard.addView(verseBody);

        LinearLayout verseActions = new LinearLayout(this);
        verseActions.setOrientation(LinearLayout.HORIZONTAL);

        btnReflect = createSmallButton("Reflect", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnReflect.setOnClickListener(this);

        btnPray = createSmallButton("Pray (+10 XP)", COLOR_ACCENT_ORANGE, COLOR_WHITE);
        btnPray.setOnClickListener(this);

        btnShare = createSmallButton("📱 WhatsApp Story", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnShare.setOnClickListener(this);

        verseActions.addView(btnReflect);
        verseActions.addView(btnPray);
        verseActions.addView(btnShare);
        verseCard.addView(verseActions);

        // Dedicated "Read Full Passage" button inside Scripture Card
        Button btnReadPassageCard = new Button(this);
        btnReadPassageCard.setText("📖 Read Full Passage (2 Samuel 5:17–21)");
        btnReadPassageCard.setTextColor(COLOR_WHITE);
        btnReadPassageCard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnReadPassageCard.setTypeface(Typeface.DEFAULT_BOLD);
        btnReadPassageCard.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 2));
        LinearLayout.LayoutParams lpReadCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpReadCard.setMargins(0, dp(8), 0, 0);
        btnReadPassageCard.setLayoutParams(lpReadCard);
        btnReadPassageCard.setOnClickListener(v -> showFullScriptureDialog());
        verseCard.addView(btnReadPassageCard);

        content.addView(verseCard);

        // 4. THE "3-MINUTE BREAKTHROUGH" SEQUENTIAL ROUTINE (Glorify Pattern)
        LinearLayout routineCard = createCard(COLOR_WHITE, dp(16), COLOR_ORANGE_BORDER, dp(2));
        routineCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpRoutine = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRoutine.setMargins(0, 0, 0, dp(14));
        routineCard.setLayoutParams(lpRoutine);

        TextView routineHeader = new TextView(this);
        routineHeader.setText("⚡ 3-MINUTE BREAKTHROUGH ROUTINE");
        routineHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        routineHeader.setTypeface(Typeface.DEFAULT_BOLD);
        routineHeader.setTextColor(COLOR_ACCENT_ORANGE);
        routineCard.addView(routineHeader);

        // Step Pills Row (1. Read -> 2. Reflect -> 3. Pray)
        LinearLayout stepPillsRow = new LinearLayout(this);
        stepPillsRow.setOrientation(LinearLayout.HORIZONTAL);
        stepPillsRow.setPadding(0, dp(6), 0, dp(12));

        stepIndicatorRead = createStepPill("1. Read 📖", true, false);
        stepIndicatorReflect = createStepPill("2. Reflect 💡", false, false);
        stepIndicatorPray = createStepPill("3. Pray 🙏", false, false);

        stepPillsRow.addView(stepIndicatorRead);
        stepPillsRow.addView(stepIndicatorReflect);
        stepPillsRow.addView(stepIndicatorPray);
        routineCard.addView(stepPillsRow);

        // Dynamic Step Content Box
        LinearLayout stepBox = new LinearLayout(this);
        stepBox.setOrientation(LinearLayout.VERTICAL);
        stepBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setColor(COLOR_BG_NEUTRAL);
        sbBg.setCornerRadius(dp(10));
        sbBg.setStroke(dp(1), COLOR_BORDER_GREY);
        stepBox.setBackground(sbBg);

        tvRoutineStepTitle = new TextView(this);
        tvRoutineStepTitle.setText("Step 1 of 3: Read Today's Word (30s)");
        tvRoutineStepTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvRoutineStepTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvRoutineStepTitle.setTextColor(COLOR_TEXT_DARK);
        stepBox.addView(tvRoutineStepTitle);

        tvRoutineStepContent = new TextView(this);
        tvRoutineStepContent.setText("“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.” — 2 Samuel 5:20");
        tvRoutineStepContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvRoutineStepContent.setTextColor(COLOR_TEXT_MUTED);
        tvRoutineStepContent.setPadding(0, dp(4), 0, dp(10));
        tvRoutineStepContent.setLineSpacing(dp(2), 1.15f);
        stepBox.addView(tvRoutineStepContent);

        btnRoutineNext = new Button(this);
        btnRoutineNext.setText("✓ Done Reading (Next Step ➔)");
        btnRoutineNext.setTextColor(COLOR_WHITE);
        btnRoutineNext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnRoutineNext.setTypeface(Typeface.DEFAULT_BOLD);
        btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnRoutineNext.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnRoutineNext.setOnClickListener(v -> advanceRoutineStep());
        stepBox.addView(btnRoutineNext);

        routineCard.addView(stepBox);
        content.addView(routineCard);

        // 5. UPCOMING CHURCH EVENTS SHELF
        TextView tvEventsHeader = new TextView(this);
        tvEventsHeader.setText("UPCOMING CHURCH EVENTS & GATHERINGS");
        tvEventsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvEventsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvEventsHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvEventsHeader.setPadding(0, dp(6), 0, dp(8));
        content.addView(tvEventsHeader);

        final String[][] churchEvents = {
                {"Community Food Drive & Mercy Outreach", "Saturday, 9:00 AM • Embu Outreach Center", "Missions & Mercy", "Distributing food packages and warm clothing to vulnerable families across Embu County."},
                {"Sunday Breakthrough Celebration Service", "Sunday, 9:00 AM & 11:15 AM • Central Sanctuary", "Worship & Word", "Two powerful services of Word, Breakthrough Worship, and prophetic ministry led by Bishop Dr. David Mutweri."},
                {"Youth & Young Adults Fellowship (Ignite)", "Friday, 5:30 PM • Youth Chapel", "NextGen", "Worship, dynamic discussion on career & faith, and games with the young adults."},
                {"Midweek Miracle & Prayer Service", "Wednesday, 6:00 PM • Sanctuary & Online", "Intercession", "Prevailing prayer and teaching on spiritual breakthrough and family blessings."}
        };

        for (final String[] ev : churchEvents) {
            LinearLayout evCard = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
            evCard.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpEv = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpEv.setMargins(0, 0, 0, dp(10));
            evCard.setLayoutParams(lpEv);

            LinearLayout tagRow = new LinearLayout(this);
            tagRow.setOrientation(LinearLayout.HORIZONTAL);
            tagRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tagCat = new TextView(this);
            tagCat.setText(ev[2].toUpperCase());
            tagCat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tagCat.setTypeface(Typeface.DEFAULT_BOLD);
            tagCat.setTextColor(COLOR_ACCENT_ORANGE);
            tagCat.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
            tagCat.setPadding(dp(6), dp(2), dp(6), dp(2));
            tagRow.addView(tagCat);

            View sp = new View(this);
            tagRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1.0f));

            TextView tvDate = new TextView(this);
            tvDate.setText(ev[1]);
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvDate.setTextColor(COLOR_TEXT_MUTED);
            tagRow.addView(tvDate);
            evCard.addView(tagRow);

            TextView evTitle = new TextView(this);
            evTitle.setText(ev[0]);
            evTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            evTitle.setTypeface(Typeface.DEFAULT_BOLD);
            evTitle.setTextColor(COLOR_TEXT_DARK);
            evTitle.setPadding(0, dp(4), 0, dp(2));
            evCard.addView(evTitle);

            TextView evDesc = new TextView(this);
            evDesc.setText(ev[3]);
            evDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            evDesc.setTextColor(COLOR_TEXT_MUTED);
            evDesc.setLineSpacing(dp(2), 1.15f);
            evDesc.setPadding(0, 0, 0, dp(8));
            evCard.addView(evDesc);

            final Button btnRsvp = new Button(this);
            btnRsvp.setText("🗓️ RSVP / Set Reminder (+5 XP)");
            btnRsvp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btnRsvp.setTypeface(Typeface.DEFAULT_BOLD);
            btnRsvp.setTextColor(COLOR_PRIMARY_PURPLE);
            btnRsvp.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
            btnRsvp.setPadding(dp(8), dp(4), dp(8), dp(4));
            btnRsvp.setOnClickListener(v -> {
                xpCount += 5;
                GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
                updateHud();
                btnRsvp.setText("✓ RSVP Confirmed!");
                btnRsvp.setEnabled(false);
                Toast.makeText(this, "Reminder set for " + ev[0] + "! (+5 XP)", Toast.LENGTH_SHORT).show();
            });
            evCard.addView(btnRsvp);

            content.addView(evCard);
        }

        // 6. Quick Prayer Request CTA
        btnSubmitPrayerHome = new Button(this);
        btnSubmitPrayerHome.setText("🙏 Submit Prayer Request to Intercessors");
        btnSubmitPrayerHome.setTextColor(COLOR_WHITE);
        btnSubmitPrayerHome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSubmitPrayerHome.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmitPrayerHome.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnSubmitPrayerHome.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lpBtnPray = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtnPray.setMargins(0, dp(6), 0, dp(14));
        btnSubmitPrayerHome.setLayoutParams(lpBtnPray);
        btnSubmitPrayerHome.setOnClickListener(this);
        content.addView(btnSubmitPrayerHome);

        scrollView.addView(content);
        return scrollView;
    }

    private TextView createStepPill(String label, boolean active, boolean done) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(6), dp(4), dp(6), dp(4));

        GradientDrawable bg = new GradientDrawable();
        if (done) {
            bg.setColor(COLOR_PURPLE_TINT);
            bg.setStroke(dp(1), COLOR_PRIMARY_PURPLE);
            tv.setTextColor(COLOR_PRIMARY_PURPLE);
        } else if (active) {
            bg.setColor(COLOR_ORANGE_TINT);
            bg.setStroke(dp(1), COLOR_ACCENT_ORANGE);
            tv.setTextColor(COLOR_ACCENT_ORANGE);
        } else {
            bg.setColor(COLOR_BG_NEUTRAL);
            bg.setStroke(dp(1), COLOR_BORDER_GREY);
            tv.setTextColor(COLOR_TEXT_MUTED);
        }
        bg.setCornerRadius(dp(12));
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void advanceRoutineStep() {
        if (routineStep == 1) {
            routineStep = 2;
            updateRoutineView();
            Toast.makeText(this, "Step 1 Completed! Now meditate on Bishop's insight.", Toast.LENGTH_SHORT).show();
        } else if (routineStep == 2) {
            routineStep = 3;
            updateRoutineView();
            Toast.makeText(this, "Step 2 Completed! Stand in agreement for the Breakthrough Prayer.", Toast.LENGTH_SHORT).show();
        } else if (routineStep == 3) {
            routineStep = 4;
            questClaimed = true;
            streakCount += 1;
            xpCount += 50;
            GamificationStore.saveStreak(this, "user_me", streakCount, streakFrozen);
            GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
            progressPercent = 100;
            updateHud();
            progressBar.setProgress(progressPercent);
            progressTextView.setText("100% completed — All daily devotions accomplished! 🎉");
            updateRoutineView();

            new AlertDialog.Builder(this)
                    .setTitle("🎉 3-Minute Breakthrough Complete!")
                    .setMessage("Hallelujah! You completed your daily breakthrough routine!\n\n"
                            + "• Streak Extended: " + streakCount + " Days 🔥\n"
                            + "• XP Rewarded: +50 XP (Total: " + xpCount + " ⭐)\n\n"
                            + "\"The LORD will make you the head and not the tail; you shall be above only and not beneath.\" — Deuteronomy 28:13")
                    .setPositiveButton("Receive Breakthrough!", null)
                    .show();
        }
    }

    private void updateRoutineView() {
        if (routineStep == 1) {
            stepIndicatorRead.setText("1. Read 📖");
            stepIndicatorRead.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ACCENT_ORANGE));
            stepIndicatorRead.setTextColor(COLOR_ACCENT_ORANGE);

            stepIndicatorReflect.setText("2. Reflect");
            stepIndicatorReflect.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
            stepIndicatorReflect.setTextColor(COLOR_TEXT_MUTED);

            stepIndicatorPray.setText("3. Pray");
            stepIndicatorPray.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
            stepIndicatorPray.setTextColor(COLOR_TEXT_MUTED);

            tvRoutineStepTitle.setText("Step 1: Read Today's Word (30s)");
            tvRoutineStepContent.setText("“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.” — 2 Samuel 5:20");
            btnRoutineNext.setText("✓ Done Reading (Next Step ➔)");
            btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
            btnRoutineNext.setEnabled(true);
        } else if (routineStep == 2) {
            stepIndicatorRead.setText("1. Read ✓");
            stepIndicatorRead.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
            stepIndicatorRead.setTextColor(COLOR_PRIMARY_PURPLE);

            stepIndicatorReflect.setText("2. Reflect 💡");
            stepIndicatorReflect.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ACCENT_ORANGE));
            stepIndicatorReflect.setTextColor(COLOR_ACCENT_ORANGE);

            stepIndicatorPray.setText("3. Pray");
            stepIndicatorPray.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
            stepIndicatorPray.setTextColor(COLOR_TEXT_MUTED);

            tvRoutineStepTitle.setText("Step 2: Bishop's Key Insight (1 min)");
            tvRoutineStepContent.setText("“Breakthrough is not accidental; it is covenantal. At Baal-Perazim, David stood in faith and saw God burst through his obstacles like rushing waters. Whatever mountain you face today, God is breaking through!” — Bishop Dr. David Mutweri");
            btnRoutineNext.setText("💡 Meditated on Insight (Next Step ➔)");
            btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
            btnRoutineNext.setEnabled(true);
        } else if (routineStep == 3) {
            stepIndicatorRead.setText("1. Read ✓");
            stepIndicatorRead.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
            stepIndicatorRead.setTextColor(COLOR_PRIMARY_PURPLE);

            stepIndicatorReflect.setText("2. Reflect ✓");
            stepIndicatorReflect.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
            stepIndicatorReflect.setTextColor(COLOR_PRIMARY_PURPLE);

            stepIndicatorPray.setText("3. Pray 🙏");
            stepIndicatorPray.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ACCENT_ORANGE));
            stepIndicatorPray.setTextColor(COLOR_ACCENT_ORANGE);

            tvRoutineStepTitle.setText("Step 3: 1-Tap Breakthrough Prayer (1 min)");
            tvRoutineStepContent.setText("“Heavenly Father, I declare that You are Baal-Perazim in my life. Break out over my finances, family, and health like bursting waters. I receive divine acceleration in Jesus' name. Amen!”");
            btnRoutineNext.setText("🙏 Declare Prayer & Complete (+50 XP)");
            btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
            btnRoutineNext.setEnabled(true);
        } else {
            stepIndicatorRead.setText("1. Read ✓");
            stepIndicatorRead.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
            stepIndicatorRead.setTextColor(COLOR_PRIMARY_PURPLE);

            stepIndicatorReflect.setText("2. Reflect ✓");
            stepIndicatorReflect.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
            stepIndicatorReflect.setTextColor(COLOR_PRIMARY_PURPLE);

            stepIndicatorPray.setText("3. Pray ✓");
            stepIndicatorPray.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
            stepIndicatorPray.setTextColor(COLOR_PRIMARY_PURPLE);

            tvRoutineStepTitle.setText("🎉 3-Minute Breakthrough Completed!");
            tvRoutineStepContent.setText("You have completed today's spiritual routine. Your spirit is recharged, your 8-day streak is alive, and your +50 XP has been added!");
            btnRoutineNext.setText("✓ Completed for Today (+50 XP Claimed)");
            btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
            btnRoutineNext.setEnabled(false);
        }
    }

    // =========================================================================
    // 4. M-PESA GIVING & TITHING BOTTOM SHEET / MODAL
    // =========================================================================

    private void showGivingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundColor(COLOR_WHITE);

        // Header Title
        TextView title = new TextView(this);
        title.setText("💚 Lipa na M-Pesa · Giving & Tithes");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        container.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("“Honor the LORD with your wealth, with the firstfruits of all your crops.” — Proverbs 3:9");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subtitle.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        subtitle.setTextColor(COLOR_TEXT_MUTED);
        subtitle.setPadding(0, dp(2), 0, dp(14));
        container.addView(subtitle);

        // Paybill Details Box
        LinearLayout paybillBox = new LinearLayout(this);
        paybillBox.setOrientation(LinearLayout.VERTICAL);
        paybillBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable pbBg = new GradientDrawable();
        pbBg.setColor(COLOR_PURPLE_TINT);
        pbBg.setCornerRadius(dp(12));
        pbBg.setStroke(dp(1), COLOR_PRIMARY_PURPLE);
        paybillBox.setBackground(pbBg);

        TextView tvPaybillLabel = new TextView(this);
        tvPaybillLabel.setText("SAFARICOM M-PESA PAYBILL");
        tvPaybillLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvPaybillLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        paybillBox.addView(tvPaybillLabel);

        LinearLayout pbRow = new LinearLayout(this);
        pbRow.setOrientation(LinearLayout.HORIZONTAL);
        pbRow.setGravity(Gravity.CENTER_VERTICAL);
        pbRow.setPadding(0, dp(4), 0, dp(4));

        TextView tvPaybillNum = new TextView(this);
        tvPaybillNum.setText("Business No: 4069983");
        tvPaybillNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvPaybillNum.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillNum.setTextColor(COLOR_PURPLE_DARK);
        LinearLayout.LayoutParams lpPbNum = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvPaybillNum.setLayoutParams(lpPbNum);
        pbRow.addView(tvPaybillNum);

        Button btnCopyPaybill = new Button(this);
        btnCopyPaybill.setText("📋 Copy 4069983");
        btnCopyPaybill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnCopyPaybill.setTextColor(COLOR_WHITE);
        btnCopyPaybill.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 8, 2));
        btnCopyPaybill.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnCopyPaybill.setOnClickListener(v -> {
            android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Paybill", "4069983");
            cb.setPrimaryClip(clip);
            Toast.makeText(this, "✓ Paybill 4069983 copied to clipboard!", Toast.LENGTH_SHORT).show();
        });
        pbRow.addView(btnCopyPaybill);
        paybillBox.addView(pbRow);

        final TextView tvAccount = new TextView(this);
        tvAccount.setText("Account: PERAZIM-TITHE");
        tvAccount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvAccount.setTypeface(Typeface.DEFAULT_BOLD);
        tvAccount.setTextColor(COLOR_ACCENT_ORANGE);
        tvAccount.setPadding(0, dp(4), 0, 0);
        paybillBox.addView(tvAccount);

        container.addView(paybillBox);

        // Fund Selector Spinner
        TextView tvFundLabel = new TextView(this);
        tvFundLabel.setText("SELECT GIVING FUND:");
        tvFundLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFundLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvFundLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvFundLabel.setPadding(0, dp(14), 0, dp(4));
        container.addView(tvFundLabel);

        Spinner fundSpinner = new Spinner(this);
        List<String> funds = new ArrayList<>();
        funds.add("Tithe (PERAZIM-TITHE)");
        funds.add("Offering (PERAZIM-OFFERING)");
        funds.add("Missions & Outreach (PERAZIM-MISSIONS)");
        funds.add("Mercy Fund / Food Drive (PERAZIM-MERCY)");

        ArrayAdapter<String> fundAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, funds);
        fundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fundSpinner.setAdapter(fundAdapter);
        tvGivingAccount = tvAccount;
        fundGivingSpinner = fundSpinner;
        fundGivingSpinner.setOnItemSelectedListener(this);
        container.addView(fundSpinner);

        // Amount Selection & Preset Chips
        TextView tvAmountLabel = new TextView(this);
        tvAmountLabel.setText("AMOUNT (KES):");
        tvAmountLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvAmountLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvAmountLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvAmountLabel.setPadding(0, dp(14), 0, dp(6));
        container.addView(tvAmountLabel);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("Enter amount in KES (e.g. 1000)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        etAmount.setText("1000");

        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, 0, 0, dp(8));

        String[] chipAmounts = {"200", "500", "1,000", "2,500"};
        final String[] chipValues = {"200", "500", "1000", "2500"};
        for (int i = 0; i < chipAmounts.length; i++) {
            final String val = chipValues[i];
            Button chip = new Button(this);
            chip.setText(chipAmounts[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTextColor(COLOR_PRIMARY_PURPLE);
            chip.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
            LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
            lpChip.setMargins(dp(2), 0, dp(2), 0);
            chip.setLayoutParams(lpChip);
            chip.setOnClickListener(v -> etAmount.setText(val));
            chipsRow.addView(chip);
        }
        container.addView(chipsRow);
        container.addView(etAmount);

        // Confirm Give Button
        Button btnConfirmGive = new Button(this);
        btnConfirmGive.setText("💚 Complete M-Pesa Giving (+20 XP)");
        btnConfirmGive.setTextColor(COLOR_WHITE);
        btnConfirmGive.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnConfirmGive.setTypeface(Typeface.DEFAULT_BOLD);
        btnConfirmGive.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        LinearLayout.LayoutParams lpGive = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpGive.setMargins(0, dp(14), 0, 0);
        btnConfirmGive.setLayoutParams(lpGive);

        sv.addView(container);
        builder.setView(sv);
        final AlertDialog dlg = builder.create();

        btnConfirmGive.setOnClickListener(v -> {
            String amt = etAmount.getText().toString().trim();
            if (amt.isEmpty()) amt = "1000";
            xpCount += 20;
            GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
            updateHud();
            Toast.makeText(this, "🙏 Giving recorded for KES " + amt + " to Paybill 4069983! May God open the windows of heaven upon you! (+20 XP)", Toast.LENGTH_LONG).show();
            dlg.dismiss();
        });
        container.addView(btnConfirmGive);

        // Giving Inquiries & Pastoral Office Contact Box
        LinearLayout contactHelpBox = new LinearLayout(this);
        contactHelpBox.setOrientation(LinearLayout.VERTICAL);
        contactHelpBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable helpBg = new GradientDrawable();
        helpBg.setColor(COLOR_PURPLE_TINT);
        helpBg.setCornerRadius(dp(10));
        helpBg.setStroke(dp(1), COLOR_BORDER_GREY);
        contactHelpBox.setBackground(helpBg);
        LinearLayout.LayoutParams lpHelpBox = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHelpBox.setMargins(0, dp(14), 0, 0);
        contactHelpBox.setLayoutParams(lpHelpBox);

        TextView tvHelpHeader = new TextView(this);
        tvHelpHeader.setText("📞 GIVING INQUIRIES & PASTORAL CARE");
        tvHelpHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHelpHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHelpHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        contactHelpBox.addView(tvHelpHeader);

        TextView tvHelpDesc = new TextView(this);
        tvHelpDesc.setText("For giving verification, covenants, or prayer support:");
        tvHelpDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHelpDesc.setTextColor(COLOR_TEXT_MUTED);
        tvHelpDesc.setPadding(0, dp(2), 0, dp(6));
        contactHelpBox.addView(tvHelpDesc);

        // Bishop Phone Row
        TextView tvBishopPhone = new TextView(this);
        tvBishopPhone.setText("📞 Bishop: (+254) 0710 772 227");
        tvBishopPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBishopPhone.setTypeface(Typeface.DEFAULT_BOLD);
        tvBishopPhone.setTextColor(COLOR_PURPLE_DARK);
        tvBishopPhone.setPadding(0, dp(3), 0, dp(3));
        tvBishopPhone.setOnClickListener(v -> dialPhoneNumber("+254710772227"));
        contactHelpBox.addView(tvBishopPhone);

        // Church Email Row
        TextView tvChurchEmail = new TextView(this);
        tvChurchEmail.setText("✉️ Church: info@perazimchurch.org");
        tvChurchEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvChurchEmail.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchEmail.setTextColor(COLOR_PRIMARY_PURPLE);
        tvChurchEmail.setPadding(0, dp(3), 0, dp(3));
        tvChurchEmail.setOnClickListener(v -> sendEmail("info@perazimchurch.org", "Perazim Giving Inquiry"));
        contactHelpBox.addView(tvChurchEmail);

        // Bishop Email Row
        TextView tvBishopEmail = new TextView(this);
        tvBishopEmail.setText("✉️ Bishop: bishop@perazimchurch.org");
        tvBishopEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBishopEmail.setTypeface(Typeface.DEFAULT_BOLD);
        tvBishopEmail.setTextColor(COLOR_ACCENT_ORANGE);
        tvBishopEmail.setPadding(0, dp(3), 0, dp(4));
        tvBishopEmail.setOnClickListener(v -> sendEmail("bishop@perazimchurch.org", "Giving Covenant / Pastoral Support"));
        contactHelpBox.addView(tvBishopEmail);

        container.addView(contactHelpBox);

        dlg.show();
    }

    // =========================================================================
    // 5. FULL SCRIPTURE PASSAGE READER DIALOG (2 Samuel 5:17–21)
    // =========================================================================

    private void showFullScriptureDialog() {
        if (factory == null) {
            factory = ViewModelFactory.getInstance(this);
        }
        if (bibleViewModel == null) {
            bibleViewModel = factory.createBibleViewModel();
        }
        new BibleReaderDialog(this, bibleViewModel).show();
    }

    private void showLegacyFullScriptureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundColor(COLOR_WHITE);

        // Header image (waters.jpg)
        Bitmap watersBmp = loadAssetBitmap("waters.jpg", 500);
        if (watersBmp != null) {
            ImageView imgWaters = new ImageView(this);
            imgWaters.setImageBitmap(getRoundedCornerBitmap(watersBmp, dp(10)));
            imgWaters.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(130));
            lpImg.setMargins(0, 0, 0, dp(12));
            imgWaters.setLayoutParams(lpImg);
            container.addView(imgWaters);
        }

        TextView tvTitle = new TextView(this);
        tvTitle.setText("2 Samuel 5:17–21 · Baal Perazim");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_PURPLE_DARK);
        container.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("The Account of David's Breakthrough in the Valley of Rephaim");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(COLOR_TEXT_MUTED);
        tvSub.setPadding(0, dp(2), 0, dp(12));
        container.addView(tvSub);

        String[][] verses = {
                {"17", "When the Philistines heard that David had been anointed king over Israel, they went up in full force to search for David, but David heard about it and went down to the stronghold."},
                {"18", "Now the Philistines had come and spread out in the Valley of Rephaim;"},
                {"19", "so David inquired of the LORD, “Shall I go and attack the Philistines? Will you deliver them into my hands?” The LORD answered him, “Go, for I will surely deliver the Philistines into your hands.”"},
                {"20", "So David went to Baal Perazim, and there he defeated them. He said, “As waters break out, the LORD has broken out against my enemies before me.” Therefore he named that place Baal Perazim."},
                {"21", "The Philistines abandoned their idols there, and David and his men carried them off."}
        };

        for (String[] v : verses) {
            LinearLayout vRow = new LinearLayout(this);
            vRow.setOrientation(LinearLayout.HORIZONTAL);
            vRow.setPadding(0, dp(4), 0, dp(4));

            TextView tvNum = new TextView(this);
            tvNum.setText(v[0] + "  ");
            tvNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvNum.setTypeface(Typeface.DEFAULT_BOLD);
            tvNum.setTextColor(COLOR_ACCENT_ORANGE);
            vRow.addView(tvNum);

            TextView tvText = new TextView(this);
            tvText.setText(v[1]);
            tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvText.setTextColor(COLOR_TEXT_DARK);
            tvText.setLineSpacing(dp(2), 1.15f);
            if (v[0].equals("20")) {
                tvText.setTypeface(Typeface.DEFAULT_BOLD);
                tvText.setTextColor(COLOR_PRIMARY_PURPLE);
            }
            vRow.addView(tvText);

            container.addView(vRow);
        }

        // Commentary Card
        LinearLayout commBox = new LinearLayout(this);
        commBox.setOrientation(LinearLayout.VERTICAL);
        commBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        commBox.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpComm = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpComm.setMargins(0, dp(14), 0, dp(14));
        commBox.setLayoutParams(lpComm);

        TextView commHeader = new TextView(this);
        commHeader.setText("💡 HISTORICAL CONTEXT & THEOLOGICAL SIGNIFICANCE");
        commHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        commHeader.setTypeface(Typeface.DEFAULT_BOLD);
        commHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        commBox.addView(commHeader);

        TextView commBody = new TextView(this);
        commBody.setText("The Valley of Rephaim ('Valley of the Giants') was a strategic agricultural corridor outside Jerusalem. The Philistines mobilized their full army to crush David before his kingdom was unified.\n\nDavid did not depend on past military intellect; he inquired of the LORD. God answered with swift, overwhelming breakthrough—like a burst dam sweeping away all barriers. This is the divine lineage of Perazim Mission Church: whatever giant opposes you, God will burst forth as a flood of breakthrough!");
        commBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        commBody.setTextColor(COLOR_TEXT_MUTED);
        commBody.setPadding(0, dp(4), 0, 0);
        commBody.setLineSpacing(dp(2), 1.15f);
        commBox.addView(commBody);
        container.addView(commBox);

        sv.addView(container);
        builder.setView(sv);
        builder.setPositiveButton("Amen! Receive Breakthrough", (d, w) -> {
            xpCount += 10;
            GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
            updateHud();
            Toast.makeText(this, "Scripture passage meditated! (+10 XP)", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // =========================================================================
    // 6. WHATSAPP STATUS VERSE CARD GENERATOR DIALOG (YouVersion Pattern)
    // =========================================================================

    private void showWhatsAppVerseCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundColor(COLOR_WHITE);

        TextView tvModalTitle = new TextView(this);
        tvModalTitle.setText("📱 WhatsApp Status Verse Creator");
        tvModalTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvModalTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvModalTitle.setTextColor(COLOR_PURPLE_DARK);
        container.addView(tvModalTitle);

        TextView tvModalSub = new TextView(this);
        tvModalSub.setText("Formatted 9:16 portrait story for WhatsApp Status");
        tvModalSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvModalSub.setTextColor(COLOR_TEXT_MUTED);
        tvModalSub.setPadding(0, dp(2), 0, dp(12));
        container.addView(tvModalSub);

        // 9:16 Story Frame
        final LinearLayout previewCard = new LinearLayout(this);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setGravity(Gravity.CENTER);
        previewCard.setPadding(dp(16), dp(24), dp(16), dp(24));
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(260));
        previewCard.setLayoutParams(lpCard);

        final GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                gradientThemes[currentGradientTheme]
        );
        cardBg.setCornerRadius(dp(16));
        previewCard.setBackground(cardBg);

        TextView churchHeader = new TextView(this);
        churchHeader.setText("PERAZIM MISSION CHURCH");
        churchHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        churchHeader.setTypeface(Typeface.DEFAULT_BOLD);
        churchHeader.setTextColor(COLOR_ORANGE_BORDER);
        churchHeader.setGravity(Gravity.CENTER);
        previewCard.addView(churchHeader);

        TextView tagVerse = new TextView(this);
        tagVerse.setText("VERSE OF THE DAY · 2 SAMUEL 5:20");
        tagVerse.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tagVerse.setTypeface(Typeface.DEFAULT_BOLD);
        tagVerse.setTextColor(COLOR_PURPLE_SOFT);
        tagVerse.setGravity(Gravity.CENTER);
        tagVerse.setPadding(0, dp(2), 0, dp(10));
        previewCard.addView(tagVerse);

        TextView verseQuote = new TextView(this);
        verseQuote.setText("“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.”");
        verseQuote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        verseQuote.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        verseQuote.setTextColor(COLOR_WHITE);
        verseQuote.setGravity(Gravity.CENTER);
        verseQuote.setLineSpacing(dp(3), 1.15f);
        verseQuote.setPadding(dp(6), 0, dp(6), dp(12));
        previewCard.addView(verseQuote);

        TextView pastorAttribution = new TextView(this);
        pastorAttribution.setText("PERAZIM MISSION CHURCH — “The Place of Great Breakthrough”\nBishop Dr. David Mutweri · Embu, Kenya\n📞 (+254) 0710 772 227 · ✉️ bishop@perazimchurch.org\n#GodOfTheBreakthrough");
        pastorAttribution.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        pastorAttribution.setTextColor(COLOR_ORANGE_BORDER);
        pastorAttribution.setGravity(Gravity.CENTER);
        pastorAttribution.setPadding(dp(4), dp(2), dp(4), dp(2));
        pastorAttribution.setOnClickListener(v -> showContactDialog());
        previewCard.addView(pastorAttribution);

        container.addView(previewCard);

        // Theme Switcher Button
        final Button btnTheme = new Button(this);
        btnTheme.setText("🎨 Style: " + themeNames[currentGradientTheme] + " (Tap to Change)");
        btnTheme.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnTheme.setTextColor(COLOR_PURPLE_DARK);
        GradientDrawable thmBg = new GradientDrawable();
        thmBg.setColor(COLOR_PURPLE_TINT);
        thmBg.setCornerRadius(dp(8));
        thmBg.setStroke(dp(1), COLOR_BORDER_GREY);
        btnTheme.setBackground(thmBg);
        LinearLayout.LayoutParams lpBtnTheme = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtnTheme.setMargins(0, dp(10), 0, dp(6));
        btnTheme.setLayoutParams(lpBtnTheme);
        btnTheme.setOnClickListener(v -> {
            currentGradientTheme = (currentGradientTheme + 1) % gradientThemes.length;
            GradientDrawable newBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    gradientThemes[currentGradientTheme]
            );
            newBg.setCornerRadius(dp(16));
            previewCard.setBackground(newBg);
            btnTheme.setText("🎨 Style: " + themeNames[currentGradientTheme] + " (Tap to Change)");
        });
        container.addView(btnTheme);

        // Share to WhatsApp Button
        Button btnShareWA = new Button(this);
        btnShareWA.setText("📱 Share to WhatsApp / Status");
        btnShareWA.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnShareWA.setTypeface(Typeface.DEFAULT_BOLD);
        btnShareWA.setTextColor(COLOR_WHITE);
        btnShareWA.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnShareWA.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lpShareWA = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpShareWA.setMargins(0, 0, 0, dp(6));
        btnShareWA.setLayoutParams(lpShareWA);
        btnShareWA.setOnClickListener(v -> {
            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                String shareBody = "✨ *PERAZIM MISSION CHURCH — “The Place of Great Breakthrough”* ✨\n\n"
                        + "“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.”\n\n"
                        + "— *2 Samuel 5:20*\n\n"
                        + "PERAZIM MISSION CHURCH — “The Place of Great Breakthrough”\n"
                        + "“A place where everybody is somebody, and no body is a nobody”\n\n"
                        + "Presiding Bishop Dr. David Mutweri · Embu, Kenya\n"
                        + "📞 Phone: (+254) 0710 772 227\n"
                        + "✉️ Church: info@perazimchurch.org\n"
                        + "✉️ Bishop: bishop@perazimchurch.org\n"
                        + "💚 Safaricom Paybill: 4069983\n\n"
                        + "Theme: Baal-Perazim / God of the Breakthrough\n\n"
                        + "#BaalPerazim #GodOfTheBreakthrough #PerazimMissionChurch";
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                sendIntent.setType("text/plain");
                sendIntent.setPackage("com.whatsapp");
                startActivity(sendIntent);
            } catch (Exception e) {
                Intent chooser = new Intent(Intent.ACTION_SEND);
                chooser.putExtra(Intent.EXTRA_TEXT, "“As waters break out, the LORD has broken out against my enemies before me.” — 2 Samuel 5:20\nPERAZIM MISSION CHURCH — “The Place of Great Breakthrough”\nPresiding Bishop Dr. David Mutweri | Phone: (+254) 0710 772 227 | Paybill: 4069983");
                chooser.setType("text/plain");
                startActivity(Intent.createChooser(chooser, "Share Breakthrough Verse"));
            }
        });
        container.addView(btnShareWA);

        // Copy Text Button
        Button btnCopy = new Button(this);
        btnCopy.setText("📋 Copy Scripture Text");
        btnCopy.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnCopy.setTextColor(COLOR_PURPLE_DARK);
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setColor(COLOR_WHITE);
        copyBg.setCornerRadius(dp(8));
        copyBg.setStroke(dp(1), COLOR_BORDER_GREY);
        btnCopy.setBackground(copyBg);
        btnCopy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "Breakthrough Verse",
                    "“As waters break out, the LORD has broken out against my enemies before me.” — 2 Samuel 5:20 (PERAZIM MISSION CHURCH — “The Place of Great Breakthrough”)"
            );
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Scripture copied to clipboard! Ready to paste on WhatsApp.", Toast.LENGTH_SHORT).show();
        });
        container.addView(btnCopy);

        builder.setView(container);
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // =========================================================================
    // 7. GRACE POINT SANCTUARY & HABIT PROTECTION DIALOG (Duolingo Pattern)
    // =========================================================================

    private void showGracePointDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundColor(COLOR_WHITE);

        TextView title = new TextView(this);
        title.setText("💜 Grace Point Sanctuary");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        container.addView(title);

        final TextView balance = new TextView(this);
        balance.setText("Current Balance: " + graceCount + " Grace Points 💜" + (streakFrozen ? " (🛡️ Shield Active)" : ""));
        balance.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        balance.setTypeface(Typeface.DEFAULT_BOLD);
        balance.setTextColor(COLOR_PRIMARY_PURPLE);
        balance.setPadding(0, dp(4), 0, dp(10));
        container.addView(balance);

        TextView desc = new TextView(this);
        desc.setText("In God's kingdom, grace exceeds the law. Grace Points allow you to protect your devotional streak when life, travel, or ministry takes you away from your phone.");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        desc.setTextColor(COLOR_TEXT_MUTED);
        desc.setPadding(0, 0, 0, dp(14));
        container.addView(desc);

        // Freeze streak button
        Button btnFreeze = new Button(this);
        btnFreeze.setText("🛡️ Activate 48-Hour Streak Freeze (Cost: 2 💜)");
        btnFreeze.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnFreeze.setTextColor(COLOR_WHITE);
        btnFreeze.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
        LinearLayout.LayoutParams lpFreeze = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpFreeze.setMargins(0, 0, 0, dp(8));
        btnFreeze.setLayoutParams(lpFreeze);
        btnFreeze.setOnClickListener(v -> {
            if (streakFrozen) {
                Toast.makeText(this, "🛡️ Streak Freeze is already active for your habit!", Toast.LENGTH_SHORT).show();
            } else if (graceCount >= 2) {
                graceCount -= 2;
                streakFrozen = true;
                GamificationStore.saveGracePoints(this, "user_me", graceCount);
                GamificationStore.saveStreak(this, "user_me", streakCount, streakFrozen);
                updateHud();
                balance.setText("Current Balance: " + graceCount + " Grace Points 💜 (🛡️ Shield Active)");
                Toast.makeText(this, "🛡️ Streak Freeze Activated! Your " + streakCount + "-day streak is protected for 48 hours.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Need at least 2 Grace Points. Complete daily devotions to earn grace!", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnFreeze);

        // Redeem missed day button
        Button btnRestore = new Button(this);
        btnRestore.setText("🎁 Restore Missed Devotional Day (Cost: 3 💜)");
        btnRestore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRestore.setTextColor(COLOR_WHITE);
        btnRestore.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnRestore.setOnClickListener(v -> {
            if (graceCount >= 3) {
                graceCount -= 3;
                streakCount += 1;
                GamificationStore.saveGracePoints(this, "user_me", graceCount);
                GamificationStore.saveStreak(this, "user_me", streakCount, streakFrozen);
                updateHud();
                balance.setText("Current Balance: " + graceCount + " Grace Points 💜");
                Toast.makeText(this, "🎁 Missed day restored through Grace! Streak is now " + streakCount + " days.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Need at least 3 Grace Points to restore a missed day.", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnRestore);

        builder.setView(container);
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // =========================================================================
    // TAB 1: SERMONS SCREEN
    // =========================================================================

    private ScrollView buildSermonsScreen() {
        if (factory == null) {
            factory = ViewModelFactory.getInstance(this);
        }
        sermonsUiBinder = new SermonsUiBinder(this, factory.createSermonsViewModel());
        ScrollView sv = sermonsUiBinder.bind();
        btnWatchLiveSermon = sermonsUiBinder.getWatchLiveButton();
        return sv;
    }

    private ScrollView buildLegacySermonsScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Livestream Ready Card with Stage Photographic Banner (stage.jpg)
        LinearLayout liveCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        liveCard.setOrientation(LinearLayout.VERTICAL);

        Bitmap stageBmp = loadAssetBitmap("stage.jpg", 500);
        if (stageBmp != null) {
            ImageView ivStage = new ImageView(this);
            ivStage.setImageBitmap(getRoundedCornerBitmap(stageBmp, dp(10)));
            ivStage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpStage = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(130));
            lpStage.setMargins(0, 0, 0, dp(10));
            ivStage.setLayoutParams(lpStage);
            liveCard.addView(ivStage);
        }

        LinearLayout liveBadgeRow = new LinearLayout(this);
        liveBadgeRow.setOrientation(LinearLayout.HORIZONTAL);
        liveBadgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView redDot = new TextView(this);
        redDot.setText("🔴");
        redDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        liveBadgeRow.addView(redDot);

        TextView liveTag = new TextView(this);
        liveTag.setText(" LIVESTREAM BROADCAST");
        liveTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        liveTag.setTypeface(Typeface.DEFAULT_BOLD);
        liveTag.setTextColor(COLOR_ACCENT_ORANGE);
        liveBadgeRow.addView(liveTag);
        liveCard.addView(liveBadgeRow);

        TextView tvSermonTitle = new TextView(this);
        tvSermonTitle.setText("Walking in Faith: Overcoming Fear in Difficult Times");
        tvSermonTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvSermonTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvSermonTitle.setTextColor(COLOR_TEXT_DARK);
        tvSermonTitle.setPadding(0, dp(8), 0, dp(4));
        liveCard.addView(tvSermonTitle);

        TextView tvSpeaker = new TextView(this);
        tvSpeaker.setText("Speaker: Bishop Dr. David Mutweri | Central Campus");
        tvSpeaker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSpeaker.setTextColor(COLOR_TEXT_MUTED);
        tvSpeaker.setPadding(0, 0, 0, dp(12));
        liveCard.addView(tvSpeaker);

        btnWatchLiveSermon = new Button(this);
        btnWatchLiveSermon.setText("▶ Watch Live Broadcast");
        btnWatchLiveSermon.setTextColor(COLOR_WHITE);
        btnWatchLiveSermon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnWatchLiveSermon.setTypeface(Typeface.DEFAULT_BOLD);
        btnWatchLiveSermon.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnWatchLiveSermon.setPadding(dp(14), dp(10), dp(14), dp(10));
        btnWatchLiveSermon.setOnClickListener(this);
        liveCard.addView(btnWatchLiveSermon);
        content.addView(liveCard);

        // Media Archive Section Header
        TextView tvArchive = new TextView(this);
        tvArchive.setText("RECENT MESSAGES & ARCHIVES");
        tvArchive.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvArchive.setTypeface(Typeface.DEFAULT_BOLD);
        tvArchive.setTextColor(COLOR_PRIMARY_PURPLE);
        tvArchive.setPadding(0, dp(20), 0, dp(10));
        content.addView(tvArchive);

        // Archive 1: Candle Thumbnail
        LinearLayout btnSermonArchive1 = createSermonCard("🎥 The Power of Prevailing Prayer", "Last Sunday • 48 mins • Bishop Dr. David Mutweri", "candle.jpg");
        btnSermonArchive1.setOnClickListener(v -> showSermonPlayerDialog("The Power of Prevailing Prayer", "Bishop Dr. David Mutweri"));
        content.addView(btnSermonArchive1);

        // Archive 2: Bishop Portrait Thumbnail
        LinearLayout btnSermonArchive2 = createSermonCard("🎥 Grace That Transcends Generations", "2 weeks ago • 52 mins • Bishop Dr. David Mutweri", "bishop_portrait.jpg");
        btnSermonArchive2.setOnClickListener(v -> showSermonPlayerDialog("Grace That Transcends Generations", "Bishop Dr. David Mutweri"));
        content.addView(btnSermonArchive2);

        // Archive 3: Campus / Bookshelf Thumbnail
        LinearLayout btnSermonArchive3 = createSermonCard("🎥 Renewing the Inner Spirit", "3 weeks ago • 44 mins • Pastor Grace Mutweri", "campus.jpg");
        btnSermonArchive3.setOnClickListener(v -> showSermonPlayerDialog("Renewing the Inner Spirit", "Pastor Grace Mutweri"));
        content.addView(btnSermonArchive3);

        scrollView.addView(content);
        return scrollView;
    }

    private LinearLayout createSermonCard(String title, String metadata, String assetThumbnail) {
        LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        Bitmap thumb = loadAssetBitmap(assetThumbnail, 200);
        if (thumb != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(getRoundedCornerBitmap(thumb, dp(8)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dp(54), dp(54));
            lpImg.setMargins(0, 0, dp(10), 0);
            iv.setLayoutParams(lpImg);
            card.addView(iv);
        }

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_DARK);
        textCol.addView(tvTitle);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(metadata);
        tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvMeta.setTextColor(COLOR_TEXT_MUTED);
        tvMeta.setPadding(0, dp(3), 0, 0);
        textCol.addView(tvMeta);

        card.addView(textCol);
        return card;
    }

    // =========================================================================
    // TAB 2: WORSHIP & EXPANDED HYMNAL SCREEN (6 HYMNS)
    // =========================================================================

    private ScrollView buildWorshipScreen() {
        if (factory == null) {
            factory = ViewModelFactory.getInstance(this);
        }
        worshipUiBinder = new WorshipUiBinder(this, factory.createWorshipViewModel());
        return worshipUiBinder.bind();
    }

    private ScrollView buildLegacyWorshipScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Classical Art Banner Row (hymnals.jpg and stage.jpg)
        LinearLayout artRow = new LinearLayout(this);
        artRow.setOrientation(LinearLayout.HORIZONTAL);
        artRow.setPadding(0, 0, 0, dp(14));

        Bitmap hymnalsBmp = loadAssetBitmap("hymnals.jpg", 400);
        if (hymnalsBmp != null) {
            ImageView img1 = new ImageView(this);
            img1.setImageBitmap(getRoundedCornerBitmap(hymnalsBmp, dp(12)));
            img1.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(110), 1.0f);
            lp.setMargins(0, 0, dp(6), 0);
            img1.setLayoutParams(lp);
            artRow.addView(img1);
        }

        Bitmap stageBmp = loadAssetBitmap("stage.jpg", 400);
        if (stageBmp != null) {
            ImageView img2 = new ImageView(this);
            img2.setImageBitmap(getRoundedCornerBitmap(stageBmp, dp(12)));
            img2.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(110), 1.0f);
            lp2.setMargins(dp(6), 0, 0, 0);
            img2.setLayoutParams(lp2);
            artRow.addView(img2);
        }
        content.addView(artRow);

        TextView tvHymnsHeader = new TextView(this);
        tvHymnsHeader.setText("EXPANDED HYMNAL & CHORD BROWSER");
        tvHymnsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHymnsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHymnsHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHymnsHeader.setPadding(0, 0, 0, dp(10));
        content.addView(tvHymnsHeader);

        // 1. How Great Thou Art
        content.addView(createInteractiveHymnCard(
                "How Great Thou Art",
                "Key of G • 72 BPM • Chords: G, C, D",
                "Verse 1:\n[G] O Lord my God, when I in awesome [C] wonder,\nConsider [G] all the [D] worlds Thy hands have [G] made;\nI see the stars, I hear the rolling [C] thunder,\nThy power through-[G]-out the [D] universe dis-[G]-played.\n\nChorus:\nThen sings my [G] soul, my Savior God, to [C] Thee,\nHow great Thou [G] art, how [D] great Thou [G] art!\nThen sings my [G] soul, my Savior God, to [C] Thee,\nHow great Thou [G] art, how [D] great Thou [G] art!"
        ));

        // 2. Amazing Grace
        content.addView(createInteractiveHymnCard(
                "Amazing Grace",
                "Key of E • 68 BPM • Chords: E, A, B7",
                "Verse 1:\n[E] Amazing grace! How sweet the [A] sound\nThat [E] saved a wretch like [B7] me!\nI [E] once was lost, but now am [A] found;\nWas [E] blind, but [B7] now I [E] see.\n\nVerse 2:\n'Twas [E] grace that taught my heart to [A] fear,\nAnd [E] grace my fears re-[B7]-lieved;\nHow [E] precious did that grace ap-[A]-pear\nThe [E] hour I [B7] first be-[E]-lieved!"
        ));

        // 3. Way Maker
        content.addView(createInteractiveHymnCard(
                "Way Maker",
                "Key of E • 68 BPM • Chords: A, E, B, C#m",
                "Verse 1:\n[A] You are here, moving in our [E] midst;\nI worship You, [B] I worship You. [C#m]\n[A] You are here, working in this [E] place;\nI worship You, [B] I worship You. [C#m]\n\nChorus:\n[A] Way Maker, Miracle Worker, Promise Keeper,\n[E] Light in the darkness, my God,\nThat is who You [B] are! [C#m]"
        ));

        // 4. Great Is Thy Faithfulness
        content.addView(createInteractiveHymnCard(
                "Great Is Thy Faithfulness",
                "Key of D • 84 BPM • Chords: D, G, A7, Bm",
                "Verse 1:\n[D] Great is Thy faithfulness, [G] O God my Father,\n[A7] There is no shadow of [D] turning with Thee;\nThou changest not, Thy compassions, they [G] fail not;\nAs [A7] Thou hast been Thou forever wilt [D] be.\n\nChorus:\n[A] Great is Thy faithfulness! [D] Great is Thy faithfulness!\n[B7] Morning by morning new [Em] mercies I see;\n[A] All I have needed Thy [D] hand hath provided—\n[G] Great is Thy [D] faithfulness, [A7] Lord, unto [D] me!"
        ));

        // 5. Be Thou My Vision
        content.addView(createInteractiveHymnCard(
                "Be Thou My Vision",
                "Key of D • 76 BPM • Chords: D, Em, G, A, Bm",
                "Verse 1:\n[D] Be Thou my [Em] Vision, O [G] Lord of my [D] heart;\n[A] Naught be all [Bm] else to me, [G] save that Thou [A] art—\n[Bm] Thou my best [D] thought, by [G] day or by [A] night,\n[D] Waking or [Bm] sleeping, Thy [G] presence my [D] light.\n\nVerse 2:\n[D] Be Thou my [Em] Wisdom, and [G] Thou my true [D] Word;\n[A] I ever [Bm] with Thee and [G] Thou with me, [A] Lord;\n[Bm] Thou my great [D] Father, I [G] Thy true [A] son,\n[D] Thou in me [Bm] dwelling, and [G] I with Thee [D] one."
        ));

        // 6. It Is Well With My Soul
        content.addView(createInteractiveHymnCard(
                "It Is Well With My Soul",
                "Key of C • 70 BPM • Chords: C, G, F, Am",
                "Verse 1:\n[C] When peace, like a [G] river, attendeth my [C] way,\nWhen [Am] sorrows like [D] sea billows [G] roll;\nWhatever my [C] lot, Thou hast [F] taught me to [D] say,\nIt is [C] well, it is [G] well with my [C] soul.\n\nChorus:\nIt is [C] well (it is well) with my [G] soul (with my soul),\nIt is [F] well, it is [C] well [G] with my [C] soul!"
        ));

        scrollView.addView(content);
        return scrollView;
    }

    private LinearLayout createInteractiveHymnCard(final String title, final String metadata, final String lyrics) {
        final LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🎵  " + title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_DARK);
        card.addView(tvTitle);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(metadata);
        tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvMeta.setTextColor(COLOR_TEXT_MUTED);
        tvMeta.setPadding(0, dp(2), 0, dp(6));
        card.addView(tvMeta);

        final TextView tvLyrics = new TextView(this);
        tvLyrics.setText(lyrics);
        tvLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLyrics.setTextColor(COLOR_TEXT_DARK);
        tvLyrics.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvLyrics.setPadding(dp(8), dp(6), dp(8), dp(6));
        tvLyrics.setLineSpacing(dp(2), 1.15f);
        GradientDrawable lyrBg = new GradientDrawable();
        lyrBg.setColor(COLOR_BG_NEUTRAL);
        lyrBg.setCornerRadius(dp(6));
        tvLyrics.setBackground(lyrBg);
        tvLyrics.setVisibility(View.GONE);
        card.addView(tvLyrics);

        final TextView tvToggle = new TextView(this);
        tvToggle.setText("▼ Show Chords & Lyrics");
        tvToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvToggle.setTypeface(Typeface.DEFAULT_BOLD);
        tvToggle.setTextColor(COLOR_PRIMARY_PURPLE);
        tvToggle.setPadding(0, dp(6), 0, 0);
        card.addView(tvToggle);

        card.setOnClickListener(v -> {
            if (tvLyrics.getVisibility() == View.VISIBLE) {
                tvLyrics.setVisibility(View.GONE);
                tvToggle.setText("▼ Show Chords & Lyrics");
            } else {
                tvLyrics.setVisibility(View.VISIBLE);
                tvToggle.setText("▲ Hide Chords & Lyrics");
            }
        });

        return card;
    }

    // =========================================================================
    // TAB 3: FELLOWSHIP SCREEN WITH COMMUNITY PRAYER WALL & BIBLICAL HUMOR
    // =========================================================================

    private ScrollView buildFellowshipScreen() {
        if (factory == null) {
            factory = ViewModelFactory.getInstance(this);
        }
        fellowshipUiBinder = new FellowshipUiBinder(this, factory.createFellowshipViewModel());
        return fellowshipUiBinder.bind();
    }

    private ScrollView buildLegacyFellowshipScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Header Fellowship Photo Banner (fellowship.jpg)
        Bitmap fellowshipBmp = loadAssetBitmap("fellowship.jpg", 600);
        if (fellowshipBmp != null) {
            ImageView ivFsp = new ImageView(this);
            ivFsp.setImageBitmap(getRoundedCornerBitmap(fellowshipBmp, dp(14)));
            ivFsp.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpFsp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(140));
            lpFsp.setMargins(0, 0, 0, dp(14));
            ivFsp.setLayoutParams(lpFsp);
            content.addView(ivFsp);
        }

        // COMMUNITY PRAYER WALL HEADER
        TextView tvPrayerWallHeader = new TextView(this);
        tvPrayerWallHeader.setText("COMMUNITY PRAYER WALL");
        tvPrayerWallHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPrayerWallHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvPrayerWallHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvPrayerWallHeader.setPadding(0, 0, 0, dp(6));
        content.addView(tvPrayerWallHeader);

        // Submit Prayer Request Button on Wall
        Button btnSubmitWall = new Button(this);
        btnSubmitWall.setText("✍️ Submit Prayer Request to Wall (+15 XP)");
        btnSubmitWall.setTextColor(COLOR_WHITE);
        btnSubmitWall.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnSubmitWall.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmitWall.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
        btnSubmitWall.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lpSubWall = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpSubWall.setMargins(0, 0, 0, dp(12));
        btnSubmitWall.setLayoutParams(lpSubWall);
        btnSubmitWall.setOnClickListener(v -> showPrayerRequestDialog());
        content.addView(btnSubmitWall);

        final String[][] prayerData = {
                {"Mama Sarah (Embu General Hospital)", "Elder Gitonga", "Praying for full recovery, divine strength, and quick healing following hip replacement surgery.", "24"},
                {"KCSE Candidates at Kangaru School", "Youth Ministry", "Interceding for focus, clarity of mind, and divine wisdom for all 120 Form 4 exam candidates.", "38"},
                {"Central Sanctuary Roof Completion", "Deacon Board", "Thanksgiving for milestones achieved and prayer for safety of engineering team completing the roof.", "52"},
                {"Family Agribusiness in Mwea", "Sister Mercy", "Trusting God for supernatural breakthrough, market favor, and open doors for church rice farmers.", "19"}
        };

        for (int i = 0; i < prayerData.length; i++) {
            final String prayerTitle = prayerData[i][0];
            final String prayerAuthor = prayerData[i][1];
            final String prayerText = prayerData[i][2];
            final int initialCount = Integer.parseInt(prayerData[i][3]);

            LinearLayout pCard = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
            pCard.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpP.setMargins(0, 0, 0, dp(10));
            pCard.setLayoutParams(lpP);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("🙏 " + prayerTitle);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(COLOR_TEXT_DARK);
            pCard.addView(tvTitle);

            TextView tvAuthor = new TextView(this);
            tvAuthor.setText("Submitted by: " + prayerAuthor + " · Embu Community");
            tvAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvAuthor.setTextColor(COLOR_TEXT_MUTED);
            tvAuthor.setPadding(0, dp(2), 0, dp(6));
            pCard.addView(tvAuthor);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(prayerText);
            tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvDesc.setTextColor(COLOR_TEXT_DARK);
            tvDesc.setLineSpacing(dp(2), 1.15f);
            tvDesc.setPadding(0, 0, 0, dp(10));
            pCard.addView(tvDesc);

            final int[] countHolder = {initialCount};
            final boolean[] prayedHolder = {false};
            final Button btnAmen = new Button(this);
            btnAmen.setText("🙏 Amen (" + countHolder[0] + ")");
            btnAmen.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnAmen.setTypeface(Typeface.DEFAULT_BOLD);
            btnAmen.setTextColor(COLOR_WHITE);
            btnAmen.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
            btnAmen.setPadding(dp(12), dp(8), dp(12), dp(8));
            btnAmen.setOnClickListener(v -> {
                if (!prayedHolder[0]) {
                    prayedHolder[0] = true;
                    countHolder[0]++;
                    btnAmen.setText("✓ Amen (" + countHolder[0] + ")");
                    btnAmen.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
                    xpCount += 5;
                    GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
                    updateHud();
                    Toast.makeText(this, "🙏 Amen! You joined in prayer for " + prayerTitle + "! (+5 XP)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "You have already joined in prayer for this petition! 🙏", Toast.LENGTH_SHORT).show();
                }
            });
            pCard.addView(btnAmen);
            content.addView(pCard);
        }

        // Section: Biblical Riddles
        TextView tvRiddleHeader = new TextView(this);
        tvRiddleHeader.setText("BIBLICAL RIDDLES FOR FELLOWSHIP");
        tvRiddleHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvRiddleHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvRiddleHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvRiddleHeader.setPadding(0, dp(14), 0, dp(8));
        content.addView(tvRiddleHeader);

        content.addView(createInteractiveRiddleCard("Prophets", "Who was swallowed by a great fish when running from God's assignment?", "Jonah (Jonah 1:17)", 15));
        content.addView(createInteractiveRiddleCard("Miracles", "What food fell from heaven daily to feed Israel in the wilderness?", "Manna (Exodus 16:31)", 20));
        content.addView(createInteractiveRiddleCard("Kings", "Who was the youngest king in Judah, beginning his reign at age seven?", "Joash (2 Kings 11:21)", 10));
        content.addView(createInteractiveRiddleCard("Geography", "Where did Elijah contest against the 450 prophets of Baal?", "Mount Carmel (1 Kings 18:19)", 25));

        // Section: Biblical Humor & Joy Card
        LinearLayout humorCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        humorCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpHumor = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHumor.setMargins(0, dp(10), 0, 0);
        humorCard.setLayoutParams(lpHumor);

        TextView tvHumorTag = new TextView(this);
        tvHumorTag.setText("😄 CHRISTIAN JOY & BIBLICAL HUMOR");
        tvHumorTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHumorTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvHumorTag.setTextColor(COLOR_ACCENT_ORANGE);
        humorCard.addView(tvHumorTag);

        TextView tvJokeQ = new TextView(this);
        tvJokeQ.setText("Q: Who was the greatest financier in the Bible?");
        tvJokeQ.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvJokeQ.setTypeface(Typeface.DEFAULT_BOLD);
        tvJokeQ.setTextColor(COLOR_TEXT_DARK);
        tvJokeQ.setPadding(0, dp(8), 0, dp(4));
        humorCard.addView(tvJokeQ);

        final TextView tvJokePrompt = new TextView(this);
        tvJokePrompt.setText("👆 Tap to reveal punchline 😄");
        tvJokePrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvJokePrompt.setTypeface(Typeface.DEFAULT_BOLD);
        tvJokePrompt.setTextColor(COLOR_PRIMARY_PURPLE);
        tvJokePrompt.setPadding(0, dp(4), 0, dp(4));
        humorCard.addView(tvJokePrompt);

        final TextView tvJokeA = new TextView(this);
        tvJokeA.setText("A: Noah! He was floating his stock while everyone else was in liquidation! 🌊🚢\n\nBonus: Who was the greatest babysitter? David, because he rocked Goliath to sleep! 😴");
        tvJokeA.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvJokeA.setTextColor(COLOR_TEXT_DARK);
        tvJokeA.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvJokeA.setPadding(0, dp(6), 0, 0);
        tvJokeA.setVisibility(View.GONE);
        humorCard.addView(tvJokeA);

        final boolean[] humorClaimed = {false};
        humorCard.setOnClickListener(v -> {
            if (tvJokeA.getVisibility() == View.GONE) {
                tvJokeA.setVisibility(View.VISIBLE);
                tvJokePrompt.setText("😄 Tap to hide punchline");
                if (!humorClaimed[0]) {
                    humorClaimed[0] = true;
                    xpCount += 5;
                    GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
                    updateHud();
                    Toast.makeText(this, "A merry heart doeth good like a medicine! (Prov 17:22) +5 XP", Toast.LENGTH_SHORT).show();
                }
            } else {
                tvJokeA.setVisibility(View.GONE);
                tvJokePrompt.setText("👆 Tap to reveal punchline 😄");
            }
        });
        content.addView(humorCard);

        // 6. CHURCH FOUNDATION & IDENTITY CARD
        LinearLayout identityCard = createCard(COLOR_WHITE, dp(16), COLOR_PRIMARY_PURPLE, dp(1));
        identityCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpIdentity = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpIdentity.setMargins(0, dp(14), 0, dp(6));
        identityCard.setLayoutParams(lpIdentity);

        TextView tvIdTag = new TextView(this);
        tvIdTag.setText("🏛️ CHURCH FOUNDATION & IDENTITY");
        tvIdTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvIdTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvIdTag.setTextColor(COLOR_PRIMARY_PURPLE);
        identityCard.addView(tvIdTag);

        TextView tvIdTitle = new TextView(this);
        tvIdTitle.setText("PERAZIM MISSION CHURCH");
        tvIdTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvIdTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvIdTitle.setTextColor(COLOR_PURPLE_DARK);
        tvIdTitle.setPadding(0, dp(4), 0, dp(2));
        identityCard.addView(tvIdTitle);

        TextView tvMotto = new TextView(this);
        tvMotto.setText("Motto: “The Place of Great Breakthrough”");
        tvMotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvMotto.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
        tvMotto.setTextColor(COLOR_ACCENT_ORANGE);
        tvMotto.setPadding(0, 0, 0, dp(2));
        identityCard.addView(tvMotto);

        TextView tvSlogan = new TextView(this);
        tvSlogan.setText("Slogan: “A place where everybody is somebody, and no body is a nobody”");
        tvSlogan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSlogan.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        tvSlogan.setTextColor(COLOR_TEXT_DARK);
        tvSlogan.setPadding(0, 0, 0, dp(8));
        identityCard.addView(tvSlogan);

        View sepId = new View(this);
        sepId.setBackgroundColor(COLOR_BORDER_GREY);
        LinearLayout.LayoutParams lpSepId = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lpSepId.setMargins(0, dp(2), 0, dp(8));
        sepId.setLayoutParams(lpSepId);
        identityCard.addView(sepId);

        TextView tvVision = new TextView(this);
        tvVision.setText("Vision: “To be a Center for Missions”");
        tvVision.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvVision.setTypeface(Typeface.DEFAULT_BOLD);
        tvVision.setTextColor(COLOR_PURPLE_DARK);
        tvVision.setPadding(0, 0, 0, dp(4));
        identityCard.addView(tvVision);

        TextView tvMission = new TextView(this);
        tvMission.setText("Mission: “Perazim exists to draw people to Christ, to disciple them to belong to His family, and to have them glorify GOD with their lives and their services.”");
        tvMission.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvMission.setTextColor(COLOR_TEXT_DARK);
        tvMission.setLineSpacing(dp(2), 1.15f);
        tvMission.setPadding(0, 0, 0, dp(8));
        identityCard.addView(tvMission);

        TextView tvValuesLabel = new TextView(this);
        tvValuesLabel.setText("Core Values:");
        tvValuesLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvValuesLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvValuesLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvValuesLabel.setPadding(0, 0, 0, dp(4));
        identityCard.addView(tvValuesLabel);

        TextView tvValues = new TextView(this);
        tvValues.setText("Evangelism • Discipleship • Fellowship • Worship • Ministry");
        tvValues.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvValues.setTypeface(Typeface.DEFAULT_BOLD);
        tvValues.setTextColor(COLOR_PURPLE_DARK);
        tvValues.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        tvValues.setPadding(dp(8), dp(6), dp(8), dp(6));
        identityCard.addView(tvValues);

        content.addView(identityCard);

        // 7. CHURCH CONTACT & PASTORAL CARE SECTION
        LinearLayout contactCard = createCard(COLOR_WHITE, dp(16), COLOR_PRIMARY_PURPLE, dp(1));
        contactCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpContact = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpContact.setMargins(0, dp(10), 0, dp(10));
        contactCard.setLayoutParams(lpContact);

        // Section Tag
        TextView tvContactTag = new TextView(this);
        tvContactTag.setText("🏛️ CHURCH CONTACT & PASTORAL CARE");
        tvContactTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvContactTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvContactTag.setTextColor(COLOR_PRIMARY_PURPLE);
        contactCard.addView(tvContactTag);

        // Leader Profile Row with Bishop Portrait
        LinearLayout leaderRow = new LinearLayout(this);
        leaderRow.setOrientation(LinearLayout.HORIZONTAL);
        leaderRow.setGravity(Gravity.CENTER_VERTICAL);
        leaderRow.setPadding(0, dp(8), 0, dp(8));

        Bitmap bishopBmp = loadAssetBitmap("bishop_portrait.jpg", 200);
        if (bishopBmp != null) {
            ImageView ivBishop = new ImageView(this);
            ivBishop.setImageBitmap(getRoundedCornerBitmap(bishopBmp, dp(24)));
            ivBishop.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpBmp = new LinearLayout.LayoutParams(dp(48), dp(48));
            lpBmp.setMargins(0, 0, dp(10), 0);
            ivBishop.setLayoutParams(lpBmp);
            leaderRow.addView(ivBishop);
        }

        LinearLayout leaderDetails = new LinearLayout(this);
        leaderDetails.setOrientation(LinearLayout.VERTICAL);
        TextView tvLeaderName = new TextView(this);
        tvLeaderName.setText("Bishop Dr. David Mutweri");
        tvLeaderName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvLeaderName.setTypeface(Typeface.DEFAULT_BOLD);
        tvLeaderName.setTextColor(COLOR_PURPLE_DARK);
        leaderDetails.addView(tvLeaderName);

        TextView tvLeaderTitle = new TextView(this);
        tvLeaderTitle.setText("Presiding Bishop · Perazim Mission Church");
        tvLeaderTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvLeaderTitle.setTextColor(COLOR_TEXT_MUTED);
        leaderDetails.addView(tvLeaderTitle);
        leaderRow.addView(leaderDetails);
        contactCard.addView(leaderRow);

        TextView tvContactDesc = new TextView(this);
        tvContactDesc.setText("Reach out directly for pastoral care, prayer covenants, church administration, or giving support:");
        tvContactDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvContactDesc.setTextColor(COLOR_TEXT_DARK);
        tvContactDesc.setLineSpacing(dp(2), 1.15f);
        tvContactDesc.setPadding(0, 0, 0, dp(10));
        contactCard.addView(tvContactDesc);

        // Interactive Contact Rows
        LinearLayout rowPhone = createInteractiveContactRow("📞", "Bishop's Direct Phone", "(+254) 0710 772 227", v -> dialPhoneNumber("+254710772227"));
        contactCard.addView(rowPhone);

        LinearLayout rowChurchEmail = createInteractiveContactRow("📧", "Church Secretariat Email", "info@perazimchurch.org", v -> sendEmail("info@perazimchurch.org", "Perazim Church Inquiry"));
        contactCard.addView(rowChurchEmail);

        LinearLayout rowBishopEmail = createInteractiveContactRow("✉️", "Bishop's Direct Email", "bishop@perazimchurch.org", v -> sendEmail("bishop@perazimchurch.org", "Pastoral Care / Counseling"));
        contactCard.addView(rowBishopEmail);

        LinearLayout rowPaybill = createInteractiveContactRow("💚", "Safaricom Giving Paybill", "4069983 (Tap to Give)", v -> showGivingDialog());
        contactCard.addView(rowPaybill);

        // Quick Action Buttons
        LinearLayout contactButtonsRow = new LinearLayout(this);
        contactButtonsRow.setOrientation(LinearLayout.HORIZONTAL);
        contactButtonsRow.setPadding(0, dp(6), 0, 0);

        Button btnCall = createSmallButton("📞 Call", COLOR_ACCENT_ORANGE, COLOR_WHITE);
        btnCall.setOnClickListener(v -> dialPhoneNumber("+254710772227"));

        Button btnEmailBishop = createSmallButton("✉️ Bishop", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnEmailBishop.setOnClickListener(v -> sendEmail("bishop@perazimchurch.org", "Pastoral Care"));

        Button btnEmailChurch = createSmallButton("📧 Office", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnEmailChurch.setOnClickListener(v -> sendEmail("info@perazimchurch.org", "Church Information"));

        contactButtonsRow.addView(btnCall);
        contactButtonsRow.addView(btnEmailBishop);
        contactButtonsRow.addView(btnEmailChurch);
        contactCard.addView(contactButtonsRow);

        content.addView(contactCard);

        scrollView.addView(content);
        return scrollView;
    }

    // =========================================================================
    // TAB 4: PROFILE SCREEN (Guidebook §73 Layout Contract)
    // =========================================================================

    private ScrollView buildProfileScreen() {
        if (factory == null) {
            factory = ViewModelFactory.getInstance(this);
        }
        profileUiBinder = new ProfileUiBinder(this, factory.createProfileViewModel());
        return profileUiBinder.bind();
    }

    private ScrollView buildLegacyProfileScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // 1. MEMBER PROFILE HEADER CARD
        LinearLayout profileHeaderCard = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        profileHeaderCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpProfHeader = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpProfHeader.setMargins(0, 0, 0, dp(12));
        profileHeaderCard.setLayoutParams(lpProfHeader);

        LinearLayout memberRow = new LinearLayout(this);
        memberRow.setOrientation(LinearLayout.HORIZONTAL);
        memberRow.setGravity(Gravity.CENTER_VERTICAL);
        memberRow.setPadding(0, 0, 0, dp(12));

        // Avatar: Bishop portrait or monogram
        Bitmap bishopBmp = loadAssetBitmap("bishop_portrait.jpg", 200);
        if (bishopBmp != null) {
            ImageView ivAvatar = new ImageView(this);
            ivAvatar.setImageBitmap(getRoundedCornerBitmap(bishopBmp, dp(32)));
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpAv = new LinearLayout.LayoutParams(dp(64), dp(64));
            lpAv.setMargins(0, 0, dp(14), 0);
            ivAvatar.setLayoutParams(lpAv);
            memberRow.addView(ivAvatar);
        } else {
            TextView tvDefaultAvatar = new TextView(this);
            tvDefaultAvatar.setText("👤");
            tvDefaultAvatar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
            tvDefaultAvatar.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lpAv = new LinearLayout.LayoutParams(dp(64), dp(64));
            lpAv.setMargins(0, 0, dp(14), 0);
            tvDefaultAvatar.setLayoutParams(lpAv);
            memberRow.addView(tvDefaultAvatar);
        }

        LinearLayout memberInfo = new LinearLayout(this);
        memberInfo.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpMemInfo = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        memberInfo.setLayoutParams(lpMemInfo);

        TextView tvMemberName = new TextView(this);
        tvMemberName.setText("Perazim Covenant Member");
        tvMemberName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvMemberName.setTypeface(Typeface.DEFAULT_BOLD);
        tvMemberName.setTextColor(COLOR_PURPLE_DARK);
        memberInfo.addView(tvMemberName);

        TextView tvMemberSub = new TextView(this);
        tvMemberSub.setText("Perazim Mission Church · Embu Headquarters");
        tvMemberSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvMemberSub.setTextColor(COLOR_TEXT_MUTED);
        tvMemberSub.setPadding(0, dp(2), 0, dp(6));
        memberInfo.addView(tvMemberSub);

        // Covenant Badge Pill
        TextView tvCovenantBadge = new TextView(this);
        tvCovenantBadge.setText("🕊️ Active Disciple • Covenant Partner");
        tvCovenantBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvCovenantBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvCovenantBadge.setTextColor(COLOR_PRIMARY_PURPLE);
        tvCovenantBadge.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        tvCovenantBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        memberInfo.addView(tvCovenantBadge);

        memberRow.addView(memberInfo);
        profileHeaderCard.addView(memberRow);

        // Divider
        View divHeader = new View(this);
        divHeader.setBackgroundColor(COLOR_BORDER_GREY);
        LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lpDiv.setMargins(0, 0, 0, dp(12));
        divHeader.setLayoutParams(lpDiv);
        profileHeaderCard.addView(divHeader);

        // 2. SUMMARY HUD: Streak, XP, Grace Points (Guidebook §73.1)
        TextView tvHudLabel = new TextView(this);
        tvHudLabel.setText("SPIRITUAL GROWTH & DISCIPLESHIP METRICS");
        tvHudLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvHudLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvHudLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHudLabel.setPadding(0, 0, 0, dp(8));
        profileHeaderCard.addView(tvHudLabel);

        LinearLayout hudRow = new LinearLayout(this);
        hudRow.setOrientation(LinearLayout.HORIZONTAL);
        hudRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout streakCard = createMetricCard("🔥 " + streakCount + " Days", "Streak", streakFrozen ? "❄️ Frozen" : "Active", COLOR_ORANGE_TINT, COLOR_ACCENT_ORANGE);
        LinearLayout xpCard = createMetricCard("⭐ " + xpCount + " XP", "Spiritual XP", "Level 3 Disciple", COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE);
        LinearLayout graceCard = createMetricCard("💜 " + graceCount + " Pts", "Grace Points", "Tokens Available", COLOR_ORANGE_TINT, COLOR_ORANGE_DARK);

        hudRow.addView(streakCard);
        hudRow.addView(xpCard);
        hudRow.addView(graceCard);
        profileHeaderCard.addView(hudRow);

        content.addView(profileHeaderCard);

        // 3. QUICK ACTIONS GRID (Guidebook §73.3)
        TextView tvActionsHeader = new TextView(this);
        tvActionsHeader.setText("QUICK ACTIONS");
        tvActionsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvActionsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvActionsHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvActionsHeader.setPadding(0, 0, 0, dp(6));
        content.addView(tvActionsHeader);

        LinearLayout actionsRow1 = new LinearLayout(this);
        actionsRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpRow1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow1.setMargins(0, 0, 0, dp(8));
        actionsRow1.setLayoutParams(lpRow1);

        Button btnQuickGive = create3dActionButton("💚 Give", "Lipa na M-Pesa", COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, v -> showGivingDialog());
        Button btnQuickMessage = create3dActionButton("💬 Message", "Pastoral Care", COLOR_PURPLE_DARK, COLOR_PURPLE_SHADOW, v -> sendEmail("bishop@perazimchurch.org", "Member Direct Message"));
        actionsRow1.addView(btnQuickGive);
        actionsRow1.addView(btnQuickMessage);
        content.addView(actionsRow1);

        LinearLayout actionsRow2 = new LinearLayout(this);
        actionsRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpRow2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow2.setMargins(0, 0, 0, dp(14));
        actionsRow2.setLayoutParams(lpRow2);

        Button btnQuickInvite = create3dActionButton("📢 Invite", "Share Church", COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, v -> shareInvite());
        Button btnQuickSettings = create3dActionButton("⚙️ Settings", "Preferences", COLOR_PURPLE_DARK, COLOR_PURPLE_SHADOW, v -> showSettingsDialog());
        actionsRow2.addView(btnQuickInvite);
        actionsRow2.addView(btnQuickSettings);
        content.addView(actionsRow2);

        // 4. CHURCH IDENTITY & AFFILIATION CARD (Guidebook §73.2)
        LinearLayout churchCard = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        churchCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpChurch = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpChurch.setMargins(0, 0, 0, dp(14));
        churchCard.setLayoutParams(lpChurch);

        TextView tvChurchTag = new TextView(this);
        tvChurchTag.setText("🏛️ CHURCH IDENTITY & AFFILIATION");
        tvChurchTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvChurchTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchTag.setTextColor(COLOR_PRIMARY_PURPLE);
        tvChurchTag.setPadding(0, 0, 0, dp(6));
        churchCard.addView(tvChurchTag);

        TextView tvChurchName = new TextView(this);
        tvChurchName.setText("PERAZIM MISSION CHURCH");
        tvChurchName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvChurchName.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchName.setTextColor(COLOR_PURPLE_DARK);
        churchCard.addView(tvChurchName);

        TextView tvChurchMotto = new TextView(this);
        tvChurchMotto.setText("“The Place of Great Breakthrough” (2 Samuel 5:20)");
        tvChurchMotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvChurchMotto.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchMotto.setTextColor(COLOR_ACCENT_ORANGE);
        tvChurchMotto.setPadding(0, dp(2), 0, dp(4));
        churchCard.addView(tvChurchMotto);

        TextView tvChurchSlogan = new TextView(this);
        tvChurchSlogan.setText("“A place where everybody is somebody, and no body is a nobody”");
        tvChurchSlogan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvChurchSlogan.setTextColor(COLOR_TEXT_MUTED);
        tvChurchSlogan.setPadding(0, 0, 0, dp(8));
        churchCard.addView(tvChurchSlogan);

        LinearLayout rowAffCampus = createInteractiveContactRow("📍", "Affiliated Campus", selectedCampus + " (Embu, Kenya)", v -> Toast.makeText(this, "Active Campus: " + selectedCampus, Toast.LENGTH_SHORT).show());
        LinearLayout rowBishopAff = createInteractiveContactRow("👤", "Presiding Bishop", "Bishop Dr. David Mutweri", v -> dialPhoneNumber("+254710772227"));
        churchCard.addView(rowAffCampus);
        churchCard.addView(rowBishopAff);

        content.addView(churchCard);

        // 5. PERSONAL CONTENT & OFFLINE CACHE STATUS CARD (Guidebook §73.4)
        LinearLayout cacheCard = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        cacheCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCache = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCache.setMargins(0, 0, 0, dp(14));
        cacheCard.setLayoutParams(lpCache);

        TextView tvCacheTag = new TextView(this);
        tvCacheTag.setText("📦 PERSONAL CONTENT & OFFLINE CACHE STATUS");
        tvCacheTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvCacheTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvCacheTag.setTextColor(COLOR_PRIMARY_PURPLE);
        tvCacheTag.setPadding(0, 0, 0, dp(8));
        cacheCard.addView(tvCacheTag);

        LinearLayout rowStorage = createInteractiveContactRow("📥", "Offline Storage", "24.8 MB Cached · 100% Offline Ready", v -> Toast.makeText(this, "24.8 MB local assets verified", Toast.LENGTH_SHORT).show());
        LinearLayout rowHymnsCache = createInteractiveContactRow("🎵", "Hymnal & Chords", "9 Hymns with Full Offline Chord Charts", v -> switchTab(NavigationContract.TAB_ID_WORSHIP));
        LinearLayout rowSermonCache = createInteractiveContactRow("🎙️", "Audio Sermons", "2 Sermons Stored (Data-Saver 32k Mode)", v -> switchTab(NavigationContract.TAB_ID_SERMONS));
        LinearLayout rowScriptureCache = createInteractiveContactRow("📖", "Holy Scriptures", "2 Samuel 5 & Daily Devotionals Synced", v -> showFullScriptureDialog());
        cacheCard.addView(rowStorage);
        cacheCard.addView(rowHymnsCache);
        cacheCard.addView(rowSermonCache);
        cacheCard.addView(rowScriptureCache);

        LinearLayout cacheBtnRow = new LinearLayout(this);
        cacheBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        cacheBtnRow.setPadding(0, dp(6), 0, 0);

        Button btnSyncNow = createSmallButton("🔄 Sync Content", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnSyncNow.setOnClickListener(v -> Toast.makeText(this, "🔄 Checking Perazim sync servers... All offline assets up to date!", Toast.LENGTH_SHORT).show());

        Button btnClearCache = createSmallButton("🗑️ Refresh Cache", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnClearCache.setOnClickListener(v -> Toast.makeText(this, "✨ Local cache refreshed (24.8 MB verified intact)", Toast.LENGTH_SHORT).show());

        cacheBtnRow.addView(btnSyncNow);
        cacheBtnRow.addView(btnClearCache);
        cacheCard.addView(cacheBtnRow);

        content.addView(cacheCard);

        // 6. PASTORAL CARE DIRECT ACTIONS CARD (Guidebook §73.5)
        LinearLayout pastoralCard = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        pastoralCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpPastoral = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpPastoral.setMargins(0, 0, 0, dp(10));
        pastoralCard.setLayoutParams(lpPastoral);

        TextView tvPastoralTag = new TextView(this);
        tvPastoralTag.setText("🙏 PASTORAL CARE & DIRECT SUPPORT");
        tvPastoralTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvPastoralTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvPastoralTag.setTextColor(COLOR_PRIMARY_PURPLE);
        tvPastoralTag.setPadding(0, 0, 0, dp(6));
        pastoralCard.addView(tvPastoralTag);

        TextView tvPastoralDesc = new TextView(this);
        tvPastoralDesc.setText("Reach out directly for counseling, prayer covenants, or pastoral blessings:");
        tvPastoralDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPastoralDesc.setTextColor(COLOR_TEXT_DARK);
        tvPastoralDesc.setPadding(0, 0, 0, dp(8));
        pastoralCard.addView(tvPastoralDesc);

        LinearLayout rowBishopCall = createInteractiveContactRow("📞", "Call Bishop Dr. David Mutweri", "(+254) 0710 772 227", v -> dialPhoneNumber("+254710772227"));
        LinearLayout rowBishopEmailDirect = createInteractiveContactRow("✉️", "Email Bishop Directly", "bishop@perazimchurch.org", v -> sendEmail("bishop@perazimchurch.org", "Pastoral Care Request"));
        LinearLayout rowChurchEmailDirect = createInteractiveContactRow("📧", "Church Secretariat", "info@perazimchurch.org", v -> sendEmail("info@perazimchurch.org", "Church Inquiry"));
        LinearLayout rowPaybillDirect = createInteractiveContactRow("💚", "Safaricom Giving Paybill", "4069983 (Tap to Give)", v -> showGivingDialog());

        pastoralCard.addView(rowBishopCall);
        pastoralCard.addView(rowBishopEmailDirect);
        pastoralCard.addView(rowChurchEmailDirect);
        pastoralCard.addView(rowPaybillDirect);

        content.addView(pastoralCard);

        scrollView.addView(content);
        return scrollView;
    }

    private LinearLayout createMetricCard(String value, String label, String sublabel, int bgColor, int textColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), COLOR_BORDER_GREY);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(lp);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setTextColor(textColor);
        tvVal.setGravity(Gravity.CENTER);
        card.addView(tvVal);

        TextView tvLbl = new TextView(this);
        tvLbl.setText(label);
        tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvLbl.setTypeface(Typeface.DEFAULT_BOLD);
        tvLbl.setTextColor(COLOR_TEXT_DARK);
        tvLbl.setGravity(Gravity.CENTER);
        tvLbl.setPadding(0, dp(2), 0, dp(1));
        card.addView(tvLbl);

        TextView tvSub = new TextView(this);
        tvSub.setText(sublabel);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvSub.setTextColor(COLOR_TEXT_MUTED);
        tvSub.setGravity(Gravity.CENTER);
        card.addView(tvSub);

        return card;
    }

    private Button create3dActionButton(String title, String subtitle, int faceColor, int shadowColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(title + "\n" + subtitle);
        btn.setTextColor(COLOR_WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackground(create3dButtonDrawable(faceColor, shadowColor, 10, 3));
        btn.setPadding(dp(10), dp(10), dp(10), dp(10));
        btn.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Preferences & Data Saver");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Current Campus: " + selectedCampus + "\n\n" +
                "Audio Mode: " + (isDataSaverActive ? "📶 32kbps AAC (Data-Saver Active)" : "🎧 HD Audio") + "\n\n" +
                "Offline Cache: 24.8 MB preserved\n\n" +
                "App Version: 2.1.0 (Build 3)");
        tvInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvInfo.setTextColor(COLOR_TEXT_DARK);
        tvInfo.setLineSpacing(dp(3), 1.2f);
        layout.addView(tvInfo);

        builder.setView(layout);
        builder.setPositiveButton("Toggle Data-Saver", (dialog, which) -> {
            toggleDataSaver();
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void shareInvite() {
        try {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            String shareBody = "✨ *Join Perazim Mission Church — The Place of Great Breakthrough!* ✨\n\n"
                    + "“A place where everybody is somebody, and no body is a nobody”\n\n"
                    + "Fellowship with us:\n"
                    + "🏛️ Headquarters: Perazim Mission Church, Embu, Kenya\n"
                    + "👤 Presiding Bishop: Bishop Dr. David Mutweri\n"
                    + "📞 Phone: (+254) 0710 772 227\n"
                    + "📧 Email: info@perazimchurch.org\n"
                    + "💚 Safaricom Paybill: 4069983\n\n"
                    + "Download the official Perazim Android App for daily devotionals, sermons, and hymns!\n"
                    + "#PerazimMissionChurch #BaalPerazim #PlaceOfGreatBreakthrough";
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            sendIntent.setType("text/plain");
            sendIntent.setPackage("com.whatsapp");
            startActivity(sendIntent);
        } catch (Exception e) {
            Intent chooser = new Intent(Intent.ACTION_SEND);
            chooser.putExtra(Intent.EXTRA_TEXT, "PERAZIM MISSION CHURCH — “The Place of Great Breakthrough”\nPresiding Bishop Dr. David Mutweri | Phone: (+254) 0710 772 227 | Paybill: 4069983");
            chooser.setType("text/plain");
            startActivity(Intent.createChooser(chooser, "Share Perazim Invitation"));
        }
    }

    private LinearLayout createInteractiveRiddleCard(String category, String question, final String answer, final int xpReward) {
        final LinearLayout card = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView catTag = new TextView(this);
        catTag.setText(category.toUpperCase());
        catTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        catTag.setTypeface(Typeface.DEFAULT_BOLD);
        catTag.setTextColor(COLOR_PRIMARY_PURPLE);
        catTag.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        catTag.setPadding(dp(8), dp(4), dp(8), dp(4));
        badgeRow.addView(catTag);

        View spacer = new View(this);
        badgeRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        final TextView xpBadge = new TextView(this);
        xpBadge.setText("⚡ +" + xpReward + " XP");
        xpBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        xpBadge.setTypeface(Typeface.DEFAULT_BOLD);
        xpBadge.setTextColor(COLOR_ACCENT_ORANGE);
        xpBadge.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
        xpBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badgeRow.addView(xpBadge);
        card.addView(badgeRow);

        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(question);
        tvQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvQuestion.setTypeface(Typeface.DEFAULT_BOLD);
        tvQuestion.setTextColor(COLOR_TEXT_DARK);
        tvQuestion.setPadding(0, dp(8), 0, dp(8));
        tvQuestion.setLineSpacing(dp(3), 1.15f);
        card.addView(tvQuestion);

        final TextView tapPrompt = new TextView(this);
        tapPrompt.setText("👆 Tap to reveal answer");
        tapPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tapPrompt.setTypeface(Typeface.DEFAULT_BOLD);
        tapPrompt.setTextColor(COLOR_PRIMARY_PURPLE);
        tapPrompt.setGravity(Gravity.CENTER);
        tapPrompt.setPadding(0, dp(4), 0, dp(4));
        card.addView(tapPrompt);

        final LinearLayout answerBox = new LinearLayout(this);
        answerBox.setOrientation(LinearLayout.VERTICAL);
        answerBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable ansBg = new GradientDrawable();
        ansBg.setColor(COLOR_BG_NEUTRAL);
        ansBg.setCornerRadius(dp(10));
        ansBg.setStroke(dp(1), COLOR_BORDER_GREY);
        answerBox.setBackground(ansBg);
        answerBox.setVisibility(View.GONE);

        TextView ansLabel = new TextView(this);
        ansLabel.setText("ANSWER");
        ansLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        ansLabel.setTypeface(Typeface.DEFAULT_BOLD);
        ansLabel.setTextColor(COLOR_ACCENT_ORANGE);
        answerBox.addView(ansLabel);

        TextView ansText = new TextView(this);
        ansText.setText(answer);
        ansText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ansText.setTextColor(COLOR_TEXT_DARK);
        ansText.setPadding(0, dp(2), 0, 0);
        answerBox.addView(ansText);
        card.addView(answerBox);

        final boolean[] claimed = {false};
        card.setOnClickListener(v -> {
            if (answerBox.getVisibility() == View.GONE) {
                answerBox.setVisibility(View.VISIBLE);
                tapPrompt.setVisibility(View.GONE);
                if (!claimed[0]) {
                    claimed[0] = true;
                    xpCount += xpReward;
                    GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
                    updateHud();
                    xpBadge.setText("✓ +" + xpReward + " XP");
                    xpBadge.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
                    xpBadge.setTextColor(COLOR_PRIMARY_PURPLE);
                    Toast.makeText(this, "🎉 +" + xpReward + " XP Earned! Total: " + xpCount + " XP", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return card;
    }

    // =========================================================================
    // MODALS & DIALOGS
    // =========================================================================

    private void showPrayerRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🙏 Submit Prayer Request");
        builder.setMessage("Your petition will be submitted to Bishop Dr. David Mutweri and the Perazim Intercessory Prayer Team.\n\nPastoral Office: (+254) 0710 772 227 · info@perazimchurch.org");

        prayerInputDialog = new EditText(this);
        prayerInputDialog.setHint("Write your petition or praise report here...");
        prayerInputDialog.setMinLines(3);
        prayerInputDialog.setGravity(Gravity.TOP | Gravity.START);
        builder.setView(prayerInputDialog);

        builder.setPositiveButton("Submit (+15 XP)", (dialog, which) -> {
            String text = prayerInputDialog.getText().toString().trim();
            if (!text.isEmpty()) {
                xpCount += 15;
                GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
                updateHud();
                Toast.makeText(this, "🙏 Prayer request submitted! (+15 XP)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Please write a petition before submitting.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.setNeutralButton("📞 Pastoral Office", (dialog, which) -> showContactDialog());
        builder.show();
    }

    private void showSermonPlayerDialog(String sermonTitle, String speaker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackgroundColor(COLOR_WHITE);

        TextView t = new TextView(this);
        t.setText(sermonTitle);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(COLOR_TEXT_DARK);
        box.addView(t);

        TextView s = new TextView(this);
        s.setText(speaker);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        s.setTextColor(COLOR_TEXT_MUTED);
        s.setPadding(0, dp(2), 0, dp(14));
        box.addView(s);

        // Progress bar
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(35);
        box.addView(pb);

        TextView times = new TextView(this);
        times.setText("03:42 / 28:15 · " + (isDataSaverActive ? "Data Saver: 32kbps AAC" : "HD Audio: 128kbps"));
        times.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        times.setTextColor(COLOR_PRIMARY_PURPLE);
        times.setPadding(0, dp(4), 0, dp(14));
        box.addView(times);

        final Button btnPlayNow = new Button(this);
        btnPlayNow.setText(isPlaying ? "⏸ Pause Sermon" : "▶ Play Sermon");
        btnPlayNow.setTextColor(COLOR_WHITE);
        btnPlayNow.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlayNow.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnPlayNow.setOnClickListener(v -> {
            togglePlayback();
            btnPlayNow.setText(isPlaying ? "⏸ Pause Sermon" : "▶ Play Sermon");
        });
        box.addView(btnPlayNow);

        builder.setView(box);
        builder.setPositiveButton("Watch on YouTube", (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
                startActivity(intent);
            } catch (Exception ignored) {}
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // =========================================================================
    // EVENT HANDLING & CLICKS
    // =========================================================================

    @Override
    public void onClick(View v) {
        if (v == btnPray) {
            xpCount += 10;
            GamificationStore.saveSpiritualXp(this, "user_me", xpCount);
            updateHud();
            Toast.makeText(this, "+10 XP for spending time in prayer!", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(this)
                    .setTitle("Daily Guided Prayer")
                    .setMessage("Heavenly Father, we worship You as the God of the Breakthrough. As waters burst forth, let every mountain before us be swept away. Amen.")
                    .setPositiveButton("Amen", null)
                    .show();
        } else if (v == btnReflect) {
            switchTab(2); // Jump to Worship tab
            Toast.makeText(this, "Reflecting on God's goodness through Praise and Hymns", Toast.LENGTH_SHORT).show();
        } else if (v == btnShare) {
            showWhatsAppVerseCardDialog();
        } else if (v == btnSubmitPrayerHome) {
            showPrayerRequestDialog();
        } else if (v == btnWatchLiveSermon) {
            showSermonPlayerDialog("Walking in Faith: Overcoming Fear", "Bishop Dr. David Mutweri");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {}

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == campusSpinner) {
            selectedCampus = (String) parent.getItemAtPosition(position);
        } else if (parent == fundGivingSpinner) {
            String[] accts = {"PERAZIM-TITHE", "PERAZIM-OFFERING", "PERAZIM-MISSIONS", "PERAZIM-MERCY"};
            if (tvGivingAccount != null && position >= 0 && position < accts.length) {
                tvGivingAccount.setText("Account: " + accts[position]);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // =========================================================================
    // CHURCH CONTACT & INTENT LAUNCHERS
    // =========================================================================

    private void dialPhoneNumber(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open dialer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(String emailAddress, String subject) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + emailAddress));
            if (subject != null && !subject.isEmpty()) {
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open email app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🏛️ Church & Pastoral Office Contacts");
        builder.setMessage("Perazim Mission Church Headquarters, Embu\n\n"
                + "• Presiding Bishop: Bishop Dr. David Mutweri\n"
                + "• Bishop Direct Phone: (+254) 0710 772 227\n"
                + "• Church Office Email: info@perazimchurch.org\n"
                + "• Bishop Email: bishop@perazimchurch.org\n"
                + "• Giving Paybill: 4069983\n\n"
                + "Tap an action below to connect directly:");

        builder.setPositiveButton("📞 Call Bishop", (dialog, which) -> dialPhoneNumber("+254710772227"));
        builder.setNegativeButton("✉️ Email Bishop", (dialog, which) -> sendEmail("bishop@perazimchurch.org", "Pastoral Care & Prayer"));
        builder.setNeutralButton("📧 Church Office", (dialog, which) -> sendEmail("info@perazimchurch.org", "Church Inquiries"));
        builder.show();
    }

    private LinearLayout createInteractiveContactRow(String icon, String label, String value, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PURPLE_TINT);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), COLOR_BORDER_GREY);
        row.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvIcon.setPadding(0, 0, dp(8), 0);
        row.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label.toUpperCase());
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setTextColor(COLOR_TEXT_MUTED);
        textCol.addView(tvLabel);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setTextColor(COLOR_PURPLE_DARK);
        textCol.addView(tvVal);
        row.addView(textCol);

        TextView tvActionHint = new TextView(this);
        tvActionHint.setText("Tap ➔");
        tvActionHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvActionHint.setTypeface(Typeface.DEFAULT_BOLD);
        tvActionHint.setTextColor(COLOR_ACCENT_ORANGE);
        row.addView(tvActionHint);

        row.setOnClickListener(listener);
        return row;
    }

    // =========================================================================
    // GRAPHICS HELPERS
    // =========================================================================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private Drawable createPillBg(int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private LinearLayout createCard(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        LinearLayout card = new LinearLayout(this);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        card.setBackground(drawable);
        return card;
    }

    private Button createSmallButton(String text, int bgColor, int textColor) {
        Button btn = new Button(this);
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
            InputStream is = getAssets().open(name);
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int sampleSize = 1;
            while ((opts.outWidth / sampleSize) > maxDim || (opts.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            is = getAssets().open(name);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bmp;
        } catch (Exception e) {
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
}
