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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {

    // --- BRAND DESIGN SYSTEM & DESIGN TOKENS ---
    public static final int COLOR_PRIMARY_PURPLE  = Color.parseColor("#581665"); // Brand Deep Royal Purple
    public static final int COLOR_PURPLE_DARK     = Color.parseColor("#2D0938"); // Deep purple for headings
    public static final int COLOR_PURPLE_DEEPEST  = Color.parseColor("#140319"); // Near-black purple for YouVersion card
    public static final int COLOR_PURPLE_SHADOW   = Color.parseColor("#450E53"); // 3D bevel button shadow
    public static final int COLOR_PURPLE_LIGHT    = Color.parseColor("#8E30A8"); // Brightened purple
    public static final int COLOR_PURPLE_SOFT     = Color.parseColor("#C896D8"); // Soft purple text
    public static final int COLOR_BORDER_GREY     = Color.parseColor("#E2E8F0"); // Slate 200 Card Border
    public static final int COLOR_PURPLE_TINT     = Color.parseColor("#F5ECF7"); // Pastel purple tint
    public static final int COLOR_TEXT_MUTED      = Color.parseColor("#64748B"); // Slate 500 Muted text
    public static final int COLOR_TEXT_DARK       = Color.parseColor("#1E293B"); // Slate 800 Dark text

    public static final int COLOR_ACCENT_ORANGE   = Color.parseColor("#DE6F26"); // Secondary Brand Warm Amber/Orange
    public static final int COLOR_ORANGE_DARK     = Color.parseColor("#78350A"); // Darkened orange
    public static final int COLOR_ORANGE_SHADOW   = Color.parseColor("#98450B"); // 3D bevel shadow for orange buttons
    public static final int COLOR_ORANGE_BORDER   = Color.parseColor("#F9DFCC"); // Whitened orange border
    public static final int COLOR_ORANGE_TINT     = Color.parseColor("#FDF5EF"); // Pastel orange tint

    public static final int COLOR_WHITE           = Color.parseColor("#FFFFFF"); // Card surface
    public static final int COLOR_BG_NEUTRAL      = Color.parseColor("#F8FAFC"); // Neutral Canvas (Slate 50)

    // --- GLOBAL PRESENTATION STATE ---
    private int streakCount = 7;
    private int xpCount = 450;
    private int progressPercent = 80;
    private boolean questClaimed = false;
    private String selectedCampus = "Central Campus (Embu)";

    // Top Header Views
    private TextView topXpBadge;
    private Spinner campusSpinner;

    // Navigation Shell Views
    private FrameLayout tabContainer;
    private ScrollView[] tabViews = new ScrollView[4];
    private LinearLayout[] navTabButtons = new LinearLayout[4];
    private TextView[] navTabLabels = new TextView[4];
    private int currentTabIndex = 0;

    // Home Tab Specific Views
    private TextView progressTextView;
    private ProgressBar progressBar;
    private Button questButton;
    private Button btnPray;
    private Button btnReflect;
    private Button btnShare;
    private Button btnSubmitPrayerHome;
    private EditText prayerInputDialog;

    // Sermons Tab Views
    private Button btnWatchLiveSermon;
    private LinearLayout btnSermonArchive1;
    private LinearLayout btnSermonArchive2;
    private LinearLayout btnSermonArchive3;

    // Worship Tab Views
    private LinearLayout cardHymn1;
    private LinearLayout cardHymn2;
    private LinearLayout cardHymn3;
    private LinearLayout cardHymn4;
    private TextView lyricsHymn1;
    private TextView lyricsHymn2;
    private TextView lyricsHymn3;
    private TextView lyricsHymn4;
    private TextView toggleHymn1;
    private TextView toggleHymn2;
    private TextView toggleHymn3;
    private TextView toggleHymn4;

    // Fellowship Riddle Views
    private LinearLayout cardRiddle1;
    private LinearLayout cardRiddle2;
    private LinearLayout cardRiddle3;
    private LinearLayout cardRiddle4;
    private LinearLayout ansBoxRiddle1;
    private LinearLayout ansBoxRiddle2;
    private LinearLayout ansBoxRiddle3;
    private LinearLayout ansBoxRiddle4;
    private TextView promptRiddle1;
    private TextView promptRiddle2;
    private TextView promptRiddle3;
    private TextView promptRiddle4;
    private TextView xpBadgeRiddle1;
    private TextView xpBadgeRiddle2;
    private TextView xpBadgeRiddle3;
    private TextView xpBadgeRiddle4;
    private boolean claimedRiddle1 = false;
    private boolean claimedRiddle2 = false;
    private boolean claimedRiddle3 = false;
    private boolean claimedRiddle4 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate root layout from XML
        setContentView(R.layout.activity_main);

        // Add top app bar programmatically to root LinearLayout
        LinearLayout rootLayout = findViewById(R.id.root_linear);
        View topBar = createTopAppBar();
        rootLayout.addView(topBar, 0);

        // Initialize tab container and build tabs
        tabContainer = findViewById(R.id.tab_container);
        tabViews[0] = buildHomeScreen();
        tabViews[1] = buildSermonsScreen();
        tabViews[2] = buildWorshipScreen();
        tabViews[3] = buildFellowshipScreen();
        for (int i = 0; i < 4; i++) {
            tabContainer.addView(tabViews[i]);
            tabViews[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        }

        // Setup BottomNavigationView
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    switchTab(0);
                    return true;
                case R.id.nav_sermons:
                    switchTab(1);
                    return true;
                case R.id.nav_worship:
                    switchTab(2);
                    return true;
                case R.id.nav_fellowship:
                    switchTab(3);
                    return true;
                default:
                    return false;
            }
        });
    }

    // =========================================================================
    // 1. TOP APP BAR STANDARDIZATION
    // =========================================================================

    private View createTopAppBar() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(COLOR_WHITE);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));

        // Bottom border line for header
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(COLOR_WHITE);
        headerBg.setStroke(dp(1), COLOR_BORDER_GREY);
        header.setBackground(headerBg);

        // Church Brand Logo
        Bitmap logoBmp = loadAssetBitmap("logo_crest.png", 150);
        if (logoBmp != null) {
            ImageView logoView = new ImageView(this);
            logoView.setImageBitmap(logoBmp);
            LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(36), dp(36));
            lpLogo.setMargins(0, 0, dp(10), 0);
            logoView.setLayoutParams(lpLogo);
            header.addView(logoView);
        }

        // Campus Selector Dropdown (Pill container)
        LinearLayout spinnerPill = new LinearLayout(this);
        spinnerPill.setOrientation(LinearLayout.HORIZONTAL);
        spinnerPill.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(COLOR_BG_NEUTRAL);
        pillBg.setCornerRadius(dp(20));
        pillBg.setStroke(dp(1), COLOR_BORDER_GREY);
        spinnerPill.setBackground(pillBg);
        spinnerPill.setPadding(dp(10), dp(3), dp(8), dp(3));

        LinearLayout.LayoutParams lpPill = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpPill.setMargins(0, 0, dp(10), 0);
        spinnerPill.setLayoutParams(lpPill);

        campusSpinner = new Spinner(this);
        List<String> campuses = new ArrayList<>();
        campuses.add("Central Campus (Embu)");
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

        // Persistent XP Counter Pill (#DE6F26 Badge)
        topXpBadge = new TextView(this);
        topXpBadge.setText("⚡ " + xpCount + " XP");
        topXpBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        topXpBadge.setTypeface(Typeface.DEFAULT_BOLD);
        topXpBadge.setTextColor(COLOR_ACCENT_ORANGE);

        GradientDrawable xpBg = new GradientDrawable();
        xpBg.setColor(COLOR_ORANGE_TINT);
        xpBg.setCornerRadius(dp(20));
        xpBg.setStroke(dp(1), COLOR_ACCENT_ORANGE);
        topXpBadge.setBackground(xpBg);
        topXpBadge.setPadding(dp(10), dp(6), dp(10), dp(6));
        topXpBadge.setOnClickListener(this);
        header.addView(topXpBadge);

        return header;
    }

    private void updateXpDisplay() {
        if (topXpBadge != null) {
            topXpBadge.setText("⚡ " + xpCount + " XP");
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == campusSpinner) {
            selectedCampus = (String) parent.getItemAtPosition(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // =========================================================================
    // 2. BOTTOM NAVIGATION BAR (Material 3 Style, 4 Persistent Destinations)
    // =========================================================================

    private View createBottomNavigationBar() {
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER_VERTICAL);
        bottomNav.setBackgroundColor(COLOR_WHITE);
        bottomNav.setPadding(0, dp(6), 0, dp(8));

        GradientDrawable navBorder = new GradientDrawable();
        navBorder.setColor(COLOR_WHITE);
        navBorder.setStroke(dp(1), COLOR_BORDER_GREY);
        bottomNav.setBackground(navBorder);

        final String[] icons = {"🏠", "🎥", "🎵", "👥"};
        final String[] labels = {"Home", "Sermons", "Worship", "Fellowship"};

        for (int i = 0; i < 4; i++) {
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
            tab.setOnClickListener(this);

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

        // Toggle visibility to preserve scroll and runtime state without recreating views
        for (int i = 0; i < 4; i++) {
            tabViews[i].setVisibility(i == currentTabIndex ? View.VISIBLE : View.GONE);
        }
        refreshTabStyles();
    }

    private void refreshTabStyles() {
        for (int i = 0; i < 4; i++) {
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
    // TAB 0: HOME SCREEN
    // =========================================================================

    private ScrollView buildHomeScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Greeting
        TextView tvWelcome = new TextView(this);
        tvWelcome.setText("Welcome back, Disciple");
        tvWelcome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvWelcome.setTypeface(Typeface.DEFAULT_BOLD);
        tvWelcome.setTextColor(COLOR_TEXT_DARK);
        content.addView(tvWelcome);

        TextView tvSubGreeting = new TextView(this);
        tvSubGreeting.setText("Faithful walk with Perazim Mission Church");
        tvSubGreeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvSubGreeting.setTextColor(COLOR_TEXT_MUTED);
        tvSubGreeting.setPadding(0, dp(2), 0, dp(14));
        content.addView(tvSubGreeting);

        // 7-Day Streak Gradient Card
        LinearLayout streakCard = new LinearLayout(this);
        streakCard.setOrientation(LinearLayout.HORIZONTAL);
        streakCard.setGravity(Gravity.CENTER_VERTICAL);
        streakCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable streakGrad = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{COLOR_PRIMARY_PURPLE, Color.parseColor("#7A208C")}
        );
        streakGrad.setCornerRadius(dp(16));
        streakCard.setBackground(streakGrad);

        TextView fireIcon = new TextView(this);
        fireIcon.setText("🔥");
        fireIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        fireIcon.setPadding(0, 0, dp(14), 0);
        streakCard.addView(fireIcon);

        LinearLayout streakTextBox = new LinearLayout(this);
        streakTextBox.setOrientation(LinearLayout.VERTICAL);

        TextView tvStreakTitle = new TextView(this);
        tvStreakTitle.setText(streakCount + "-Day Devotional Streak!");
        tvStreakTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvStreakTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvStreakTitle.setTextColor(COLOR_WHITE);
        streakTextBox.addView(tvStreakTitle);

        TextView tvStreakSub = new TextView(this);
        tvStreakSub.setText("Daily prayer & scripture engagement grants bonus XP.");
        tvStreakSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvStreakSub.setTextColor(Color.parseColor("#E0D0E5"));
        streakTextBox.addView(tvStreakSub);

        streakCard.addView(streakTextBox);
        content.addView(streakCard);

        // Daily Devotion Progress Card
        LinearLayout progressCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        progressCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpProgressCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpProgressCard.setMargins(0, dp(14), 0, 0);
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

        // YouVersion Scripture Card (Obsidian #140319 Deep Purple)
        LinearLayout verseCard = createCard(COLOR_PURPLE_DEEPEST, dp(16), COLOR_PRIMARY_PURPLE, dp(1));
        verseCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpVerse = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpVerse.setMargins(0, dp(14), 0, 0);
        verseCard.setLayoutParams(lpVerse);

        TextView verseHeader = new TextView(this);
        verseHeader.setText("📖 VERSE OF THE DAY · 2 SAMUEL 5:20");
        verseHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        verseHeader.setTypeface(Typeface.DEFAULT_BOLD);
        verseHeader.setTextColor(COLOR_ACCENT_ORANGE);
        verseCard.addView(verseHeader);

        TextView verseBody = new TextView(this);
        verseBody.setText("“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.”");
        verseBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        verseBody.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        verseBody.setTextColor(COLOR_WHITE);
        verseBody.setPadding(0, dp(10), 0, dp(10));
        verseBody.setLineSpacing(dp(4), 1.15f);
        verseCard.addView(verseBody);

        LinearLayout verseActions = new LinearLayout(this);
        verseActions.setOrientation(LinearLayout.HORIZONTAL);

        btnReflect = createSmallButton("Reflect", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnReflect.setOnClickListener(this);

        btnPray = createSmallButton("Pray (+10 XP)", COLOR_ACCENT_ORANGE, COLOR_WHITE);
        btnPray.setOnClickListener(this);

        btnShare = createSmallButton("Share", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnShare.setOnClickListener(this);

        verseActions.addView(btnReflect);
        verseActions.addView(btnPray);
        verseActions.addView(btnShare);
        verseCard.addView(verseActions);
        content.addView(verseCard);

        // Daily Quest Card
        LinearLayout questCard = createCard(COLOR_WHITE, dp(16), COLOR_ORANGE_BORDER, dp(2));
        questCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpQuest = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpQuest.setMargins(0, dp(14), 0, 0);
        questCard.setLayoutParams(lpQuest);

        TextView questTitle = new TextView(this);
        questTitle.setText("⚡ TODAY'S BREAKTHROUGH QUEST");
        questTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        questTitle.setTypeface(Typeface.DEFAULT_BOLD);
        questTitle.setTextColor(COLOR_ACCENT_ORANGE);
        questCard.addView(questTitle);

        TextView questDesc = new TextView(this);
        questDesc.setText("Read today's scripture and meditate on the God of the Breakthrough to claim your blessing!");
        questDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        questDesc.setTextColor(COLOR_TEXT_DARK);
        questDesc.setPadding(0, dp(4), 0, dp(12));
        questCard.addView(questDesc);

        questButton = new Button(this);
        questButton.setText("Complete Quest (+50 XP)");
        questButton.setTextColor(COLOR_WHITE);
        questButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        questButton.setTypeface(Typeface.DEFAULT_BOLD);
        questButton.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 12, 4));
        questButton.setPadding(dp(14), dp(12), dp(14), dp(12));
        questButton.setOnClickListener(this);
        questCard.addView(questButton);
        content.addView(questCard);

        // Quick Prayer Request CTA
        btnSubmitPrayerHome = new Button(this);
        btnSubmitPrayerHome.setText("🙏 Submit Prayer Request to Intercessors");
        btnSubmitPrayerHome.setTextColor(COLOR_WHITE);
        btnSubmitPrayerHome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSubmitPrayerHome.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmitPrayerHome.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnSubmitPrayerHome.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lpBtnPray = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtnPray.setMargins(0, dp(14), 0, 0);
        btnSubmitPrayerHome.setLayoutParams(lpBtnPray);
        btnSubmitPrayerHome.setOnClickListener(this);
        content.addView(btnSubmitPrayerHome);

        scrollView.addView(content);
        return scrollView;
    }

    // =========================================================================
    // TAB 1: SERMONS SCREEN
    // =========================================================================

    private ScrollView buildSermonsScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Livestream Ready Card
        LinearLayout liveCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        liveCard.setOrientation(LinearLayout.VERTICAL);

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
        liveTag.setTextColor(Color.RED);
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

        // Archive 1
        btnSermonArchive1 = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        btnSermonArchive1.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpItem1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpItem1.setMargins(0, 0, 0, dp(10));
        btnSermonArchive1.setLayoutParams(lpItem1);
        TextView title1 = new TextView(this);
        title1.setText("🎥  The Power of Prevailing Prayer");
        title1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title1.setTypeface(Typeface.DEFAULT_BOLD);
        title1.setTextColor(COLOR_TEXT_DARK);
        btnSermonArchive1.addView(title1);
        TextView date1 = new TextView(this);
        date1.setText("Last Sunday • 48 mins");
        date1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        date1.setTextColor(COLOR_TEXT_MUTED);
        date1.setPadding(0, dp(4), 0, 0);
        btnSermonArchive1.addView(date1);
        btnSermonArchive1.setOnClickListener(this);
        content.addView(btnSermonArchive1);

        // Archive 2
        btnSermonArchive2 = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        btnSermonArchive2.setOrientation(LinearLayout.VERTICAL);
        btnSermonArchive2.setLayoutParams(lpItem1);
        TextView title2 = new TextView(this);
        title2.setText("🎥  Grace That Transcends Generations");
        title2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title2.setTypeface(Typeface.DEFAULT_BOLD);
        title2.setTextColor(COLOR_TEXT_DARK);
        btnSermonArchive2.addView(title2);
        TextView date2 = new TextView(this);
        date2.setText("2 weeks ago • 52 mins");
        date2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        date2.setTextColor(COLOR_TEXT_MUTED);
        date2.setPadding(0, dp(4), 0, 0);
        btnSermonArchive2.addView(date2);
        btnSermonArchive2.setOnClickListener(this);
        content.addView(btnSermonArchive2);

        // Archive 3
        btnSermonArchive3 = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        btnSermonArchive3.setOrientation(LinearLayout.VERTICAL);
        btnSermonArchive3.setLayoutParams(lpItem1);
        TextView title3 = new TextView(this);
        title3.setText("🎥  Renewing the Inner Spirit");
        title3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title3.setTypeface(Typeface.DEFAULT_BOLD);
        title3.setTextColor(COLOR_TEXT_DARK);
        btnSermonArchive3.addView(title3);
        TextView date3 = new TextView(this);
        date3.setText("3 weeks ago • 44 mins");
        date3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        date3.setTextColor(COLOR_TEXT_MUTED);
        date3.setPadding(0, dp(4), 0, 0);
        btnSermonArchive3.addView(date3);
        btnSermonArchive3.setOnClickListener(this);
        content.addView(btnSermonArchive3);

        scrollView.addView(content);
        return scrollView;
    }

    // =========================================================================
    // TAB 2: WORSHIP & HYMNS SCREEN
    // =========================================================================

    private ScrollView buildWorshipScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Classical Art Banner
        LinearLayout artRow = new LinearLayout(this);
        artRow.setOrientation(LinearLayout.HORIZONTAL);
        artRow.setPadding(0, 0, 0, dp(14));

        Bitmap hymnBmp = loadAssetBitmap("art_hymn.jpg", 400);
        if (hymnBmp != null) {
            Bitmap roundHymn = getRoundedCornerBitmap(hymnBmp, dp(12));
            ImageView img1 = new ImageView(this);
            img1.setImageBitmap(roundHymn);
            img1.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(110), 1.0f);
            lp.setMargins(0, 0, dp(6), 0);
            img1.setLayoutParams(lp);
            artRow.addView(img1);
        }

        Bitmap worshipBmp = loadAssetBitmap("art_worship_classic.jpg", 400);
        if (worshipBmp != null) {
            Bitmap roundWorship = getRoundedCornerBitmap(worshipBmp, dp(12));
            ImageView img2 = new ImageView(this);
            img2.setImageBitmap(roundWorship);
            img2.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(110), 1.0f);
            lp2.setMargins(dp(6), 0, 0, 0);
            img2.setLayoutParams(lp2);
            artRow.addView(img2);
        }
        content.addView(artRow);

        TextView tvHymnsHeader = new TextView(this);
        tvHymnsHeader.setText("HYMNS & CHORD BROWSER");
        tvHymnsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHymnsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHymnsHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHymnsHeader.setPadding(0, 0, 0, dp(10));
        content.addView(tvHymnsHeader);

        // Hymn 1
        cardHymn1 = createHymnCard("How Great Thou Art", "Key of G • 72 BPM",
                "O Lord my God, when I in awesome wonder, consider all the worlds Thy hands have made...", 1);
        content.addView(cardHymn1);

        // Hymn 2
        cardHymn2 = createHymnCard("Amazing Grace (My Chains Are Gone)", "Key of D • 68 BPM",
                "Amazing grace! How sweet the sound that saved a wretch like me! I once was lost, but now am found...", 2);
        content.addView(cardHymn2);

        // Hymn 3
        cardHymn3 = createHymnCard("Way Maker", "Key of E • 66 BPM",
                "You are here, moving in our midst; I worship You, I worship You. Way maker, miracle worker, promise keeper...", 3);
        content.addView(cardHymn3);

        // Hymn 4
        cardHymn4 = createHymnCard("Great Is Thy Faithfulness", "Key of C • 80 BPM",
                "Great is Thy faithfulness, O God my Father, there is no shadow of turning with Thee...", 4);
        content.addView(cardHymn4);

        scrollView.addView(content);
        return scrollView;
    }

    private LinearLayout createHymnCard(String title, String meta, String lyrics, int hymnNum) {
        LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🎵  " + title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_DARK);
        card.addView(tvTitle);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(meta);
        tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvMeta.setTextColor(COLOR_TEXT_MUTED);
        tvMeta.setPadding(0, dp(2), 0, dp(8));
        card.addView(tvMeta);

        TextView tvLyrics = new TextView(this);
        tvLyrics.setText(lyrics);
        tvLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvLyrics.setTextColor(COLOR_TEXT_DARK);
        tvLyrics.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvLyrics.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable lyricsBg = new GradientDrawable();
        lyricsBg.setColor(COLOR_BG_NEUTRAL);
        lyricsBg.setCornerRadius(dp(8));
        lyricsBg.setStroke(dp(1), COLOR_BORDER_GREY);
        tvLyrics.setBackground(lyricsBg);
        tvLyrics.setVisibility(View.GONE);
        card.addView(tvLyrics);

        TextView tvToggle = new TextView(this);
        tvToggle.setText("▼ Tap to view lyrics & chords");
        tvToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvToggle.setTypeface(Typeface.DEFAULT_BOLD);
        tvToggle.setTextColor(COLOR_PRIMARY_PURPLE);
        tvToggle.setPadding(0, dp(6), 0, 0);
        card.addView(tvToggle);

        if (hymnNum == 1) {
            lyricsHymn1 = tvLyrics;
            toggleHymn1 = tvToggle;
        } else if (hymnNum == 2) {
            lyricsHymn2 = tvLyrics;
            toggleHymn2 = tvToggle;
        } else if (hymnNum == 3) {
            lyricsHymn3 = tvLyrics;
            toggleHymn3 = tvToggle;
        } else if (hymnNum == 4) {
            lyricsHymn4 = tvLyrics;
            toggleHymn4 = tvToggle;
        }

        card.setOnClickListener(this);
        return card;
    }

    // =========================================================================
    // TAB 3: FELLOWSHIP SCREEN (INTERACTIVE RIDDLE CARDS + HUMOR + ANNOUNCEMENTS)
    // =========================================================================

    private ScrollView buildFellowshipScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Header for Riddles
        TextView tvRiddleHeader = new TextView(this);
        tvRiddleHeader.setText("DAILY BIBLICAL RIDDLES (INTERACTIVE)");
        tvRiddleHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvRiddleHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvRiddleHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvRiddleHeader.setPadding(0, 0, 0, dp(10));
        content.addView(tvRiddleHeader);

        // Riddle 1
        cardRiddle1 = createRiddleCard(
                "OLD TESTAMENT",
                "I was not born, yet I had a wife. I never had a mother, yet I walked in the garden of life. Who am I?",
                "Adam (Genesis 2:7, 2:22)",
                15, 1
        );
        content.addView(cardRiddle1);

        // Riddle 2
        cardRiddle2 = createRiddleCard(
                "PROPHETS",
                "I ran away on a ship to escape God's calling, only to end up in a three-day dark submarine ride. Who am I?",
                "Jonah (Book of Jonah 1:17)",
                20, 2
        );
        content.addView(cardRiddle2);

        // Riddle 3
        cardRiddle3 = createRiddleCard(
                "GOSPELS",
                "I climbed up a sycamore tree just to catch a glimpse of Jesus passing by. Who was I?",
                "Zacchaeus (Luke 19:1-10)",
                10, 3
        );
        content.addView(cardRiddle3);

        // Riddle 4
        cardRiddle4 = createRiddleCard(
                "JUDGES",
                "Out of the eater came something to eat, and out of the strong came something sweet. What was it?",
                "Honey in the lion's carcass (Judges 14:14)",
                25, 4
        );
        content.addView(cardRiddle4);

        // Christian Joy & Humor Card
        LinearLayout humorCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        humorCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpHumor = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHumor.setMargins(0, dp(16), 0, 0);
        humorCard.setLayoutParams(lpHumor);

        TextView tvHumorTag = new TextView(this);
        tvHumorTag.setText("😄 CHRISTIAN HUMOR OF THE WEEK");
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

        TextView tvJokeA = new TextView(this);
        tvJokeA.setText("A: Noah! He was floating his stock while everyone else was in liquidation.");
        tvJokeA.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvJokeA.setTextColor(COLOR_TEXT_MUTED);
        tvJokeA.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        humorCard.addView(tvJokeA);

        content.addView(humorCard);

        // Community Announcements Card
        LinearLayout annCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        annCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpAnn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpAnn.setMargins(0, dp(16), 0, 0);
        annCard.setLayoutParams(lpAnn);

        TextView tvAnnTitle = new TextView(this);
        tvAnnTitle.setText("Community Announcements");
        tvAnnTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvAnnTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvAnnTitle.setTextColor(COLOR_TEXT_DARK);
        annCard.addView(tvAnnTitle);

        TextView ann1 = new TextView(this);
        ann1.setText("• Youth Fellowship: This Friday at 7:00 PM (Main Hall)");
        ann1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ann1.setTextColor(COLOR_TEXT_MUTED);
        ann1.setPadding(0, dp(6), 0, dp(4));
        annCard.addView(ann1);

        TextView ann2 = new TextView(this);
        ann2.setText("• Community Food Drive: Saturday 10:00 AM (Central Campus)");
        ann2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ann2.setTextColor(COLOR_TEXT_MUTED);
        annCard.addView(ann2);

        content.addView(annCard);

        scrollView.addView(content);
        return scrollView;
    }

    private LinearLayout createRiddleCard(String category, String question, String answer, int xpReward, int riddleNum) {
        LinearLayout card = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        // Category Tag & XP Badge Row
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        // Category Tag Pill
        TextView catTag = new TextView(this);
        catTag.setText(category.toUpperCase());
        catTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        catTag.setTypeface(Typeface.DEFAULT_BOLD);
        catTag.setTextColor(COLOR_PRIMARY_PURPLE);
        GradientDrawable catBg = new GradientDrawable();
        catBg.setColor(COLOR_PURPLE_TINT);
        catBg.setCornerRadius(dp(6));
        catTag.setBackground(catBg);
        catTag.setPadding(dp(8), dp(4), dp(8), dp(4));
        badgeRow.addView(catTag);

        View spacer = new View(this);
        badgeRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        // XP Reward Badge Pill
        TextView xpBadge = new TextView(this);
        xpBadge.setText("⚡ +" + xpReward + " XP");
        xpBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        xpBadge.setTypeface(Typeface.DEFAULT_BOLD);
        xpBadge.setTextColor(COLOR_ACCENT_ORANGE);
        GradientDrawable xpBadgeBg = new GradientDrawable();
        xpBadgeBg.setColor(Color.TRANSPARENT);
        xpBadgeBg.setCornerRadius(dp(12));
        xpBadgeBg.setStroke(dp(1), COLOR_ACCENT_ORANGE);
        xpBadge.setBackground(xpBadgeBg);
        xpBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badgeRow.addView(xpBadge);

        card.addView(badgeRow);

        // Question Body
        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(question);
        tvQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvQuestion.setTypeface(Typeface.DEFAULT_BOLD);
        tvQuestion.setTextColor(COLOR_TEXT_DARK);
        tvQuestion.setPadding(0, dp(10), 0, dp(10));
        tvQuestion.setLineSpacing(dp(3), 1.15f);
        card.addView(tvQuestion);

        // Interactive Reveal Area
        TextView tapPrompt = new TextView(this);
        tapPrompt.setText("👆 Tap to reveal answer");
        tapPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tapPrompt.setTypeface(Typeface.DEFAULT_BOLD);
        tapPrompt.setTextColor(COLOR_PRIMARY_PURPLE);
        tapPrompt.setGravity(Gravity.CENTER);
        tapPrompt.setPadding(0, dp(4), 0, dp(4));
        card.addView(tapPrompt);

        // Answer Container (initially hidden)
        LinearLayout answerBox = new LinearLayout(this);
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

        if (riddleNum == 1) {
            ansBoxRiddle1 = answerBox;
            promptRiddle1 = tapPrompt;
            xpBadgeRiddle1 = xpBadge;
        } else if (riddleNum == 2) {
            ansBoxRiddle2 = answerBox;
            promptRiddle2 = tapPrompt;
            xpBadgeRiddle2 = xpBadge;
        } else if (riddleNum == 3) {
            ansBoxRiddle3 = answerBox;
            promptRiddle3 = tapPrompt;
            xpBadgeRiddle3 = xpBadge;
        } else if (riddleNum == 4) {
            ansBoxRiddle4 = answerBox;
            promptRiddle4 = tapPrompt;
            xpBadgeRiddle4 = xpBadge;
        }

        card.setOnClickListener(this);
        return card;
    }

    private void handleRiddleClick(int num, int xpReward) {
        LinearLayout box = null;
        TextView prompt = null;
        TextView badge = null;
        boolean isClaimed = false;

        if (num == 1) {
            box = ansBoxRiddle1; prompt = promptRiddle1; badge = xpBadgeRiddle1; isClaimed = claimedRiddle1;
        } else if (num == 2) {
            box = ansBoxRiddle2; prompt = promptRiddle2; badge = xpBadgeRiddle2; isClaimed = claimedRiddle2;
        } else if (num == 3) {
            box = ansBoxRiddle3; prompt = promptRiddle3; badge = xpBadgeRiddle3; isClaimed = claimedRiddle3;
        } else if (num == 4) {
            box = ansBoxRiddle4; prompt = promptRiddle4; badge = xpBadgeRiddle4; isClaimed = claimedRiddle4;
        }

        if (box == null) return;

        if (box.getVisibility() == View.VISIBLE) {
            box.setVisibility(View.GONE);
            prompt.setVisibility(View.VISIBLE);
        } else {
            box.setVisibility(View.VISIBLE);
            prompt.setVisibility(View.GONE);

            if (!isClaimed) {
                if (num == 1) claimedRiddle1 = true;
                if (num == 2) claimedRiddle2 = true;
                if (num == 3) claimedRiddle3 = true;
                if (num == 4) claimedRiddle4 = true;

                xpCount += xpReward;
                updateXpDisplay();

                badge.setText("✓ +" + xpReward + " XP");
                GradientDrawable g = new GradientDrawable();
                g.setColor(COLOR_ORANGE_TINT);
                g.setCornerRadius(dp(12));
                g.setStroke(dp(1), COLOR_ACCENT_ORANGE);
                badge.setBackground(g);

                Toast.makeText(this, "🎉 +" + xpReward + " XP Claimed! Total: " + xpCount + " XP", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleHymnClick(int num) {
        TextView lyrics = null;
        TextView toggle = null;

        if (num == 1) { lyrics = lyricsHymn1; toggle = toggleHymn1; }
        else if (num == 2) { lyrics = lyricsHymn2; toggle = toggleHymn2; }
        else if (num == 3) { lyrics = lyricsHymn3; toggle = toggleHymn3; }
        else if (num == 4) { lyrics = lyricsHymn4; toggle = toggleHymn4; }

        if (lyrics == null) return;

        if (lyrics.getVisibility() == View.VISIBLE) {
            lyrics.setVisibility(View.GONE);
            toggle.setText("▼ Tap to view lyrics & chords");
        } else {
            lyrics.setVisibility(View.VISIBLE);
            toggle.setText("▲ Hide lyrics & chords");
        }
    }

    // =========================================================================
    // 3. SUB-PAGE NAVIGATION & ACTIONS
    // =========================================================================

    private void showSermonPlayerDialog(String sermonTitle, String speaker) {
        new AlertDialog.Builder(this)
                .setTitle("▶ " + sermonTitle)
                .setMessage("Speaker: " + speaker + "\n\nLive service recording from Central Campus auditorium.\n\nNotes:\n1. Fear is natural, but faith is supernatural.\n2. In all things acknowledge Him and He will make paths straight.")
                .setPositiveButton("Watch on YouTube", this)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showPrayerRequestDialog() {
        prayerInputDialog = new EditText(this);
        prayerInputDialog.setHint("Type your prayer request here...");
        prayerInputDialog.setPadding(dp(14), dp(14), dp(14), dp(14));

        new AlertDialog.Builder(this)
                .setTitle("Submit Prayer Request")
                .setMessage("Our pastoral and intercessory prayer teams stand with you in faith. Confidences are strictly protected.")
                .setView(prayerInputDialog)
                .setPositiveButton("Submit (+15 XP)", this)
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (prayerInputDialog != null) {
                String req = prayerInputDialog.getText().toString().trim();
                prayerInputDialog = null;
                if (req.length() > 0) {
                    xpCount += 15;
                    updateXpDisplay();
                    Toast.makeText(this, "🙏 Prayer request submitted! (+15 XP)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Please enter your prayer request.", Toast.LENGTH_SHORT).show();
                }
            } else {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Opening video stream...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        // Bottom Navigation Tabs
        if (v == navTabButtons[0]) {
            switchTab(0);
        } else if (v == navTabButtons[1]) {
            switchTab(1);
        } else if (v == navTabButtons[2]) {
            switchTab(2);
        } else if (v == navTabButtons[3]) {
            switchTab(3);
        }
        // Top Header
        else if (v == topXpBadge) {
            Toast.makeText(this, "⭐ " + xpCount + " Total XP Earned! Keep solving riddles and quests!", Toast.LENGTH_SHORT).show();
        }
        // Home Actions
        else if (v == questButton) {
            claimQuest();
        } else if (v == btnPray) {
            xpCount += 10;
            updateXpDisplay();
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
            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "“As waters break out, the LORD has broken out against my enemies before me.” — 2 Samuel 5:20 (Perazim Mission Church)");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share Verse"));
            } catch (Exception e) {
                Toast.makeText(this, "Verse shared!", Toast.LENGTH_SHORT).show();
            }
        } else if (v == btnSubmitPrayerHome) {
            showPrayerRequestDialog();
        }
        // Sermons Actions
        else if (v == btnWatchLiveSermon) {
            showSermonPlayerDialog("Walking in Faith: Overcoming Fear", "Bishop Dr. David Mutweri");
        } else if (v == btnSermonArchive1) {
            showSermonPlayerDialog("The Power of Prevailing Prayer", "Perazim Media Archive");
        } else if (v == btnSermonArchive2) {
            showSermonPlayerDialog("Grace That Transcends Generations", "Perazim Media Archive");
        } else if (v == btnSermonArchive3) {
            showSermonPlayerDialog("Renewing the Inner Spirit", "Perazim Media Archive");
        }
        // Worship Hymn Cards
        else if (v == cardHymn1) {
            handleHymnClick(1);
        } else if (v == cardHymn2) {
            handleHymnClick(2);
        } else if (v == cardHymn3) {
            handleHymnClick(3);
        } else if (v == cardHymn4) {
            handleHymnClick(4);
        }
        // Fellowship Riddle Cards
        else if (v == cardRiddle1) {
            handleRiddleClick(1, 15);
        } else if (v == cardRiddle2) {
            handleRiddleClick(2, 20);
        } else if (v == cardRiddle3) {
            handleRiddleClick(3, 10);
        } else if (v == cardRiddle4) {
            handleRiddleClick(4, 25);
        }
    }

    private void claimQuest() {
        if (questClaimed) return;
        questClaimed = true;
        streakCount += 1;
        xpCount += 50;
        progressPercent = 100;
        updateXpDisplay();

        progressBar.setProgress(progressPercent);
        progressTextView.setText("100% completed — All daily devotions accomplished! 🎉");

        questButton.setText("✅ Quest Completed! (+50 XP Claimed)");
        questButton.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        questButton.setEnabled(false);

        new AlertDialog.Builder(this)
                .setTitle("🎉 Breakthrough Quest Completed!")
                .setMessage("Glory to God! You earned +50 XP and extended your streak to " + streakCount + " days!\n\n\"The LORD will make you the head and not the tail.\" — Deuteronomy 28:13")
                .setPositiveButton("Hallelujah!", null)
                .show();
    }

    // =========================================================================
    // UI GRAPHICS HELPERS
    // =========================================================================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private LinearLayout createCard(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        LinearLayout card = new LinearLayout(this);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(cornerRadiusDp);
        drawable.setStroke(borderWidthDp, borderColor);
        card.setBackground(drawable);
        return card;
    }

    private Button createSmallButton(String text, int bgColor, int textColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(10), dp(8), dp(10), dp(8));

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
