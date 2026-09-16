package com.example.app.presentation.ui;

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

import com.example.app.domain.model.Announcement;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.model.Event;
import com.example.app.domain.model.Reflection;
import com.example.app.domain.model.Sermon;
import com.example.app.domain.model.User;
import com.example.app.presentation.viewmodel.BibleViewModel;
import com.example.app.presentation.viewmodel.HomeData;
import com.example.app.presentation.viewmodel.HomeViewModel;
import com.example.app.presentation.viewmodel.ViewModelFactory;
import com.example.app.ui.theme.PerazimTheme;

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

    // Progress State
    private int streakCount = 7;
    private int xpCount = 450;
    private int progressPercent = 80;
    private TextView tvProgressSubtitle;
    private ProgressBar progressBarDaily;

    // Dynamic Section Containers
    private LinearLayout sermonsCarouselContent;
    private LinearLayout announcementsContainer;
    private LinearLayout eventsContainer;

    // Cached Data
    private HomeData cachedHomeData;

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
        rootScrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(dp(16), dp(16), dp(16), dp(32));

        // 1. Hero Photographic Sanctuary Banner
        Bitmap sanctuaryBmp = loadAssetBitmap("sanctuary.jpg", 600);
        if (sanctuaryBmp != null) {
            ImageView ivHero = new ImageView(context);
            ivHero.setImageBitmap(getRoundedCornerBitmap(sanctuaryBmp, dp(14)));
            ivHero.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpHero = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(150));
            lpHero.setMargins(0, 0, 0, dp(14));
            ivHero.setLayoutParams(lpHero);
            contentContainer.addView(ivHero);
        }

        // 2. Canonical Church Identity & Welcome Header
        TextView tvChurchHeader = new TextView(context);
        tvChurchHeader.setText("PERAZIM MISSION CHURCH");
        tvChurchHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvChurchHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchHeader.setTextColor(COLOR_PURPLE_DARK);
        tvChurchHeader.setPadding(0, 0, 0, dp(2));
        contentContainer.addView(tvChurchHeader);

        TextView tvSubGreeting = new TextView(context);
        tvSubGreeting.setText("“The Place of Great Breakthrough”\n“A place where everybody is somebody, and no body is a nobody”\nPerazim Mission Church · Embu Headquarters");
        tvSubGreeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSubGreeting.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        tvSubGreeting.setTextColor(COLOR_PRIMARY_PURPLE);
        tvSubGreeting.setLineSpacing(dp(2), 1.15f);
        tvSubGreeting.setPadding(0, dp(2), 0, dp(12));
        contentContainer.addView(tvSubGreeting);

        // 3. Quick Action Buttons Card (Giving, Full Passage, Prayer Request, Devotion)
        View quickActions = createQuickActionsBar();
        contentContainer.addView(quickActions);

        // 4. Daily Devotion Progress Track Card
        View progressCard = createProgressCard();
        contentContainer.addView(progressCard);

        // 5. Daily Cornerstone Scripture Card: 2 Samuel 5:20 ("Baal-perazim")
        View scriptureCard = createDailyCornerstoneScriptureCard();
        contentContainer.addView(scriptureCard);

        // 6. 3-Minute Daily Reflection Routine Card (Glorify Breakthrough Pattern)
        View routineCard = create3MinuteReflectionRoutineCard();
        contentContainer.addView(routineCard);

        // 7. Bishop Sermons Carousel Shelf
        View sermonsShelf = createBishopSermonsShelf();
        contentContainer.addView(sermonsShelf);

        // 8. Church Bulletins & Announcements Shelf
        View announcementsShelf = createAnnouncementsShelf();
        contentContainer.addView(announcementsShelf);

        // 9. Upcoming Fellowship Events Shelf
        View eventsShelf = createUpcomingEventsShelf();
        contentContainer.addView(eventsShelf);

        // 10. Quick Intercession Submission CTA
        Button btnSubmitIntercession = new Button(context);
        btnSubmitIntercession.setText("🙏 Submit Prayer Request to Intercessors");
        btnSubmitIntercession.setTextColor(COLOR_WHITE);
        btnSubmitIntercession.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSubmitIntercession.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmitIntercession.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnSubmitIntercession.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lpIntercession = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpIntercession.setMargins(0, dp(8), 0, dp(14));
        btnSubmitIntercession.setLayoutParams(lpIntercession);
        btnSubmitIntercession.setOnClickListener(v -> showPrayerRequestModal());
        contentContainer.addView(btnSubmitIntercession);

        rootScrollView.addView(contentContainer);
    }

    // =========================================================================
    // SECTION 1: QUICK ACTION BUTTONS
    // =========================================================================

    private View createQuickActionsBar() {
        LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        Button btnGive = createSmallButton("💚 Give / Tithe", COLOR_ACCENT_ORANGE, COLOR_WHITE);
        btnGive.setOnClickListener(v -> showGivingModal());

        Button btnFullPassage = createSmallButton("📖 Holy Scripture", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnFullPassage.setOnClickListener(v -> openScriptureReader("2SA", 5));

        Button btnIntercede = createSmallButton("🙏 Intercede", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnIntercede.setOnClickListener(v -> showPrayerRequestModal());

        card.addView(btnGive);
        card.addView(btnFullPassage);
        card.addView(btnIntercede);
        return card;
    }

    // =========================================================================
    // SECTION 2: DAILY DEVOTION PROGRESS CARD
    // =========================================================================

    private View createProgressCard() {
        LinearLayout card = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        TextView header = new TextView(context);
        header.setText("DAILY DEVOTION PROGRESS");
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(COLOR_PRIMARY_PURPLE);
        card.addView(header);

        tvProgressSubtitle = new TextView(context);
        tvProgressSubtitle.setText(progressPercent + "% completed — 4 of 5 devotions finished");
        tvProgressSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvProgressSubtitle.setTextColor(COLOR_TEXT_DARK);
        tvProgressSubtitle.setPadding(0, dp(4), 0, dp(8));
        card.addView(tvProgressSubtitle);

        progressBarDaily = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBarDaily.setMax(100);
        progressBarDaily.setProgress(progressPercent);
        progressBarDaily.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)));

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
        progressBarDaily.setProgressDrawable(progressLayers);
        card.addView(progressBarDaily);

        return card;
    }

    // =========================================================================
    // SECTION 3: DAILY CORNERSTONE SCRIPTURE CARD (2 Samuel 5:20)
    // =========================================================================

    private View createDailyCornerstoneScriptureCard() {
        LinearLayout verseCard = createCard(COLOR_PURPLE_DEEPEST, dp(16), COLOR_PRIMARY_PURPLE, dp(1));
        verseCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        verseCard.setLayoutParams(lp);

        // Waters Header Image (waters.jpg)
        Bitmap watersBmp = loadAssetBitmap("waters.jpg", 500);
        if (watersBmp != null) {
            ImageView imgWaters = new ImageView(context);
            imgWaters.setImageBitmap(getRoundedCornerBitmap(watersBmp, dp(10)));
            imgWaters.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpWaters = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(110));
            lpWaters.setMargins(0, 0, 0, dp(10));
            imgWaters.setLayoutParams(lpWaters);
            verseCard.addView(imgWaters);
        }

        TextView tvHeader = new TextView(context);
        tvHeader.setText("📖 CORNERSTONE SCRIPTURE · 2 SAMUEL 5:20");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(COLOR_ACCENT_ORANGE);
        verseCard.addView(tvHeader);

        TextView tvBody = new TextView(context);
        tvBody.setText("“And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters. Therefore he called the name of that place Baal-perazim.”");
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvBody.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvBody.setTextColor(COLOR_WHITE);
        tvBody.setPadding(0, dp(8), 0, dp(10));
        tvBody.setLineSpacing(dp(3), 1.15f);
        verseCard.addView(tvBody);

        // Actions Row: Reflect, Pray (+10 XP), WhatsApp Story Share
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnReflect = createSmallButton("Reflect", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnReflect.setOnClickListener(v -> scrollToRoutine());

        Button btnPray = createSmallButton("Pray (+10 XP)", COLOR_ACCENT_ORANGE, COLOR_WHITE);
        btnPray.setOnClickListener(v -> {
            awardXp(10);
            Toast.makeText(context, "🙏 Prayer lifted in agreement with Baal-perazim breakthrough! (+10 XP)", Toast.LENGTH_SHORT).show();
        });

        Button btnShare = createSmallButton("📱 WhatsApp Story", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnShare.setOnClickListener(v -> sharePassageToWhatsApp("“And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters.” — 2 Samuel 5:20 (KJV)"));

        actionsRow.addView(btnReflect);
        actionsRow.addView(btnPray);
        actionsRow.addView(btnShare);
        verseCard.addView(actionsRow);

        // Dedicated "Read Passage" Button: Opens BibleReaderDialog
        Button btnReadPassage = new Button(context);
        btnReadPassage.setText("📖 Read Passage (2 Samuel 5 · Baal-perazim)");
        btnReadPassage.setTextColor(COLOR_WHITE);
        btnReadPassage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnReadPassage.setTypeface(Typeface.DEFAULT_BOLD);
        btnReadPassage.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 2));
        LinearLayout.LayoutParams lpRead = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRead.setMargins(0, dp(8), 0, 0);
        btnReadPassage.setLayoutParams(lpRead);
        btnReadPassage.setOnClickListener(v -> openScriptureReader("2SA", 5));
        verseCard.addView(btnReadPassage);

        return verseCard;
    }

    // =========================================================================
    // SECTION 4: 3-MINUTE DAILY REFLECTION ROUTINE CARD (Glorify Pattern)
    // =========================================================================

    private View create3MinuteReflectionRoutineCard() {
        LinearLayout routineCard = createCard(COLOR_WHITE, dp(16), COLOR_ORANGE_BORDER, dp(2));
        routineCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        routineCard.setLayoutParams(lp);

        TextView header = new TextView(context);
        header.setText("⚡ 3-MINUTE DAILY REFLECTION ROUTINE");
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(COLOR_ACCENT_ORANGE);
        routineCard.addView(header);

        // Step Pills (1. Read -> 2. Reflect -> 3. Pray)
        LinearLayout stepPillsRow = new LinearLayout(context);
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
        LinearLayout stepBox = new LinearLayout(context);
        stepBox.setOrientation(LinearLayout.VERTICAL);
        stepBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setColor(COLOR_BG_NEUTRAL);
        sbBg.setCornerRadius(dp(10));
        sbBg.setStroke(dp(1), COLOR_BORDER_GREY);
        stepBox.setBackground(sbBg);

        tvRoutineStepTitle = new TextView(context);
        tvRoutineStepTitle.setText("Step 1 of 3: Read Today's Word (30s)");
        tvRoutineStepTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvRoutineStepTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvRoutineStepTitle.setTextColor(COLOR_TEXT_DARK);
        stepBox.addView(tvRoutineStepTitle);

        tvRoutineStepContent = new TextView(context);
        tvRoutineStepContent.setText("“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.” — 2 Samuel 5:20");
        tvRoutineStepContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvRoutineStepContent.setTextColor(COLOR_TEXT_MUTED);
        tvRoutineStepContent.setPadding(0, dp(4), 0, dp(10));
        tvRoutineStepContent.setLineSpacing(dp(2), 1.15f);
        stepBox.addView(tvRoutineStepContent);

        btnRoutineNext = new Button(context);
        btnRoutineNext.setText("✓ Done Reading (Next Step ➔)");
        btnRoutineNext.setTextColor(COLOR_WHITE);
        btnRoutineNext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnRoutineNext.setTypeface(Typeface.DEFAULT_BOLD);
        btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnRoutineNext.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnRoutineNext.setOnClickListener(v -> advanceReflectionStep());
        stepBox.addView(btnRoutineNext);

        routineCard.addView(stepBox);
        return routineCard;
    }

    private void advanceReflectionStep() {
        if (routineStep == 1) {
            routineStep = 2;
            updateRoutineView();
            Toast.makeText(context, "Step 1 complete! Now meditate on today's breakthrough message.", Toast.LENGTH_SHORT).show();
        } else if (routineStep == 2) {
            routineStep = 3;
            updateRoutineView();
            Toast.makeText(context, "Step 2 complete! Stand in agreement in prayer.", Toast.LENGTH_SHORT).show();
        } else if (routineStep == 3) {
            // Complete Reflection Routine
            routineStep = 4;
            String reflectionId = (cachedHomeData != null && cachedHomeData.getTodayReflection() != null)
                    ? cachedHomeData.getTodayReflection().getId()
                    : "refl_day_1";

            homeViewModel.completeDailyReflection(reflectionId, () -> {
                awardXp(20);
                streakCount += 1;
                if (listener != null) {
                    listener.onStreakUpdated(streakCount);
                }
                progressPercent = 100;
                if (progressBarDaily != null) {
                    progressBarDaily.setProgress(progressPercent);
                }
                if (tvProgressSubtitle != null) {
                    tvProgressSubtitle.setText("100% completed — All daily devotions accomplished! 🎉");
                }
                updateRoutineView();

                // Celebratory Toast & Modal
                Toast.makeText(context, "🎉 3-Minute Daily Reflection Completed! +20 Spiritual XP awarded!", Toast.LENGTH_LONG).show();

                new AlertDialog.Builder(context)
                        .setTitle("🎉 Breakthrough Routine Completed!")
                        .setMessage("Hallelujah! You completed today's 3-Minute Breakthrough Reflection!\n\n"
                                + "• Spiritual XP: +20 XP (Total: " + xpCount + " ⭐)\n"
                                + "• Active Streak: " + streakCount + " Days 🔥\n\n"
                                + "“The LORD will make you the head and not the tail; you shall be above only and not beneath.” — Deuteronomy 28:13")
                        .setPositiveButton("Amen! Receive Breakthrough", null)
                        .show();
            });
        }
    }

    private void updateRoutineView() {
        Reflection refl = (cachedHomeData != null) ? cachedHomeData.getTodayReflection() : null;

        if (routineStep == 1) {
            setStepPillState(stepIndicatorRead, true, false);
            setStepPillState(stepIndicatorReflect, false, false);
            setStepPillState(stepIndicatorPray, false, false);

            tvRoutineStepTitle.setText("Step 1 of 3: Read Today's Word (30s)");
            String text = (refl != null && !TextUtils.isEmpty(refl.getPassageText()))
                    ? refl.getPassageText()
                    : "“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.” — 2 Samuel 5:20";
            tvRoutineStepContent.setText(text);
            btnRoutineNext.setText("✓ Done Reading (Next Step ➔)");
            btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
            btnRoutineNext.setEnabled(true);
        } else if (routineStep == 2) {
            setStepPillState(stepIndicatorRead, false, true);
            setStepPillState(stepIndicatorReflect, true, false);
            setStepPillState(stepIndicatorPray, false, false);

            tvRoutineStepTitle.setText("Step 2 of 3: Bishop's Breakthrough Reflection (60s)");
            String comm = (refl != null && !TextUtils.isEmpty(refl.getCommentary()))
                    ? refl.getCommentary()
                    : "When David faced the Philistine host in the Valley of Rephaim, he did not rely on past military intellect. He inquired of the Lord. God burst forth like waters through a shattered dam. Whatever wall confronts you today, declare that the Lord of the Breakthrough is rising on your behalf!";
            tvRoutineStepContent.setText(comm);
            btnRoutineNext.setText("✓ Done Reflecting (Proceed to Prayer ➔)");
            btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
            btnRoutineNext.setEnabled(true);
        } else if (routineStep == 3) {
            setStepPillState(stepIndicatorRead, false, true);
            setStepPillState(stepIndicatorReflect, false, true);
            setStepPillState(stepIndicatorPray, true, false);

            tvRoutineStepTitle.setText("Step 3 of 3: Breakthrough Agreement Prayer (90s)");
            String prayer = (refl != null && !TextUtils.isEmpty(refl.getPrayerPrompt()))
                    ? refl.getPrayerPrompt()
                    : "Heavenly Father, Lord of Baal-Perazim, I thank You that You are the God who breaks forth. Break forth upon every obstacle, every resistance, and every delay standing in my path. I receive supernatural breakthrough for my family, my health, and my destiny today. In Jesus' mighty name, Amen!";
            tvRoutineStepContent.setText(prayer);
            btnRoutineNext.setText("🙏 Complete Reflection (+20 XP)");
            btnRoutineNext.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
            btnRoutineNext.setEnabled(true);
        } else if (routineStep == 4) {
            setStepPillState(stepIndicatorRead, false, true);
            setStepPillState(stepIndicatorReflect, false, true);
            setStepPillState(stepIndicatorPray, false, true);

            tvRoutineStepTitle.setText("✓ 3-Minute Routine Completed for Today!");
            tvRoutineStepContent.setText("“Thy blessing is upon thy people. Selah.” — Psalm 3:8\nYou earned +20 XP and maintained your active " + streakCount + "-day streak!");
            btnRoutineNext.setText("✓ Reflection Completed 🎉");
            btnRoutineNext.setEnabled(false);
            btnRoutineNext.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        }
    }

    private void setStepPillState(TextView pill, boolean active, boolean done) {
        if (pill == null) return;
        if (done) {
            pill.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_PRIMARY_PURPLE));
            pill.setTextColor(COLOR_PRIMARY_PURPLE);
            if (!pill.getText().toString().contains("✓")) {
                pill.setText(pill.getText().toString().replace("📖", "✓").replace("💡", "✓").replace("🙏", "✓"));
            }
        } else if (active) {
            pill.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ACCENT_ORANGE));
            pill.setTextColor(COLOR_ACCENT_ORANGE);
        } else {
            pill.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
            pill.setTextColor(COLOR_TEXT_MUTED);
        }
    }

    private TextView createStepPill(String label, boolean active, boolean done) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(6), dp(4), dp(6), dp(4));

        setStepPillState(tv, active, done);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        tv.setLayoutParams(lp);
        return tv;
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

        for (final Announcement ann : announcements) {
            LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(lp);

            // Priority Badge + Date Header
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            boolean isUrgent = "URGENT".equalsIgnoreCase(ann.getPriority());
            TextView tvPriority = new TextView(context);
            tvPriority.setText(isUrgent ? "URGENT NOTICE" : "CHURCH BULLETIN");
            tvPriority.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvPriority.setTypeface(Typeface.DEFAULT_BOLD);
            tvPriority.setTextColor(isUrgent ? COLOR_ACCENT_ORANGE : COLOR_PRIMARY_PURPLE);
            tvPriority.setBackground(createPillBg(isUrgent ? COLOR_ORANGE_TINT : COLOR_PURPLE_TINT,
                    isUrgent ? COLOR_ORANGE_BORDER : COLOR_BORDER_GREY));
            tvPriority.setPadding(dp(6), dp(2), dp(6), dp(2));
            topRow.addView(tvPriority);

            View sp = new View(context);
            topRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1.0f));

            TextView tvDate = new TextView(context);
            tvDate.setText(formatTimestamp(ann.getTimestamp()));
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvDate.setTextColor(COLOR_TEXT_MUTED);
            topRow.addView(tvDate);
            card.addView(topRow);

            // Title
            TextView tvTitle = new TextView(context);
            tvTitle.setText(ann.getTitle());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(COLOR_TEXT_DARK);
            tvTitle.setPadding(0, dp(4), 0, dp(2));
            card.addView(tvTitle);

            // Body
            TextView tvBody = new TextView(context);
            tvBody.setText(ann.getBody());
            tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvBody.setTextColor(COLOR_TEXT_MUTED);
            tvBody.setLineSpacing(dp(2), 1.15f);
            card.addView(tvBody);

            announcementsContainer.addView(card);
        }
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

    // =========================================================================
    // MODAL DIALOGS: GIVING, PRAYER REQUEST, SERMON DETAILS & PLAYER
    // =========================================================================

    public void showGivingModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ScrollView sv = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundColor(COLOR_WHITE);

        // Header Title
        TextView title = new TextView(context);
        title.setText("💚 Lipa na M-Pesa · Giving & Tithes");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        container.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("“Honor the LORD with your wealth, with the firstfruits of all your crops.” — Proverbs 3:9");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subtitle.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        subtitle.setTextColor(COLOR_TEXT_MUTED);
        subtitle.setPadding(0, dp(2), 0, dp(14));
        container.addView(subtitle);

        // Paybill Details Box
        LinearLayout paybillBox = new LinearLayout(context);
        paybillBox.setOrientation(LinearLayout.VERTICAL);
        paybillBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable pbBg = new GradientDrawable();
        pbBg.setColor(COLOR_PURPLE_TINT);
        pbBg.setCornerRadius(dp(12));
        pbBg.setStroke(dp(1), COLOR_PRIMARY_PURPLE);
        paybillBox.setBackground(pbBg);

        TextView tvPaybillLabel = new TextView(context);
        tvPaybillLabel.setText("SAFARICOM M-PESA PAYBILL");
        tvPaybillLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvPaybillLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        paybillBox.addView(tvPaybillLabel);

        LinearLayout pbRow = new LinearLayout(context);
        pbRow.setOrientation(LinearLayout.HORIZONTAL);
        pbRow.setGravity(Gravity.CENTER_VERTICAL);
        pbRow.setPadding(0, dp(4), 0, dp(4));

        TextView tvPaybillNum = new TextView(context);
        tvPaybillNum.setText("Business No: 4069983");
        tvPaybillNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvPaybillNum.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillNum.setTextColor(COLOR_PURPLE_DARK);
        LinearLayout.LayoutParams lpPbNum = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvPaybillNum.setLayoutParams(lpPbNum);
        pbRow.addView(tvPaybillNum);

        Button btnCopyPaybill = new Button(context);
        btnCopyPaybill.setText("📋 Copy 4069983");
        btnCopyPaybill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnCopyPaybill.setTextColor(COLOR_WHITE);
        btnCopyPaybill.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 8, 2));
        btnCopyPaybill.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnCopyPaybill.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Paybill", "4069983"));
                Toast.makeText(context, "✓ Paybill 4069983 copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        pbRow.addView(btnCopyPaybill);
        paybillBox.addView(pbRow);

        final TextView tvAccount = new TextView(context);
        tvAccount.setText("Account: PERAZIM-TITHE");
        tvAccount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvAccount.setTypeface(Typeface.DEFAULT_BOLD);
        tvAccount.setTextColor(COLOR_ACCENT_ORANGE);
        tvAccount.setPadding(0, dp(4), 0, 0);
        paybillBox.addView(tvAccount);
        container.addView(paybillBox);

        // Fund Selector Spinner
        TextView tvFundLabel = new TextView(context);
        tvFundLabel.setText("SELECT GIVING FUND:");
        tvFundLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFundLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvFundLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvFundLabel.setPadding(0, dp(14), 0, dp(4));
        container.addView(tvFundLabel);

        Spinner fundSpinner = new Spinner(context);
        List<String> funds = new ArrayList<>();
        funds.add("Tithe (PERAZIM-TITHE)");
        funds.add("Offering (PERAZIM-OFFERING)");
        funds.add("Missions & Outreach (PERAZIM-MISSIONS)");
        funds.add("Mercy Fund / Food Drive (PERAZIM-MERCY)");

        ArrayAdapter<String> fundAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, funds);
        fundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fundSpinner.setAdapter(fundAdapter);
        fundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: tvAccount.setText("Account: PERAZIM-TITHE"); break;
                    case 1: tvAccount.setText("Account: PERAZIM-OFFERING"); break;
                    case 2: tvAccount.setText("Account: PERAZIM-MISSIONS"); break;
                    case 3: tvAccount.setText("Account: PERAZIM-MERCY"); break;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        container.addView(fundSpinner);

        // Amount Selection & Preset Chips
        TextView tvAmountLabel = new TextView(context);
        tvAmountLabel.setText("AMOUNT (KES):");
        tvAmountLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvAmountLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvAmountLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvAmountLabel.setPadding(0, dp(14), 0, dp(6));
        container.addView(tvAmountLabel);

        final EditText etAmount = new EditText(context);
        etAmount.setHint("Enter amount in KES (e.g. 1000)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        etAmount.setText("1000");

        LinearLayout chipsRow = new LinearLayout(context);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, 0, 0, dp(8));

        String[] chipAmounts = {"200", "500", "1,000", "2,500"};
        final String[] chipValues = {"200", "500", "1000", "2500"};
        for (int i = 0; i < chipAmounts.length; i++) {
            final String val = chipValues[i];
            Button chip = new Button(context);
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
        Button btnConfirmGive = new Button(context);
        btnConfirmGive.setText("💚 Complete M-Pesa Giving (+20 XP)");
        btnConfirmGive.setTextColor(COLOR_WHITE);
        btnConfirmGive.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnConfirmGive.setTypeface(Typeface.DEFAULT_BOLD);
        btnConfirmGive.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        LinearLayout.LayoutParams lpGive = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpGive.setMargins(0, dp(14), 0, 0);
        btnConfirmGive.setLayoutParams(lpGive);

        // Pastoral Contact Box: Bishop 0710 772 227
        LinearLayout contactBox = new LinearLayout(context);
        contactBox.setOrientation(LinearLayout.VERTICAL);
        contactBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        contactBox.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpContact = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpContact.setMargins(0, dp(14), 0, 0);
        contactBox.setLayoutParams(lpContact);

        TextView tvHelpHeader = new TextView(context);
        tvHelpHeader.setText("📞 GIVING INQUIRIES & PASTORAL CARE");
        tvHelpHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHelpHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHelpHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        contactBox.addView(tvHelpHeader);

        TextView tvBishopPhone = new TextView(context);
        tvBishopPhone.setText("📞 Bishop Dr. David Mutweri: (+254) 0710 772 227");
        tvBishopPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBishopPhone.setTypeface(Typeface.DEFAULT_BOLD);
        tvBishopPhone.setTextColor(COLOR_PURPLE_DARK);
        tvBishopPhone.setPadding(0, dp(4), 0, dp(2));
        tvBishopPhone.setOnClickListener(v -> dialPhoneNumber("+254710772227"));
        contactBox.addView(tvBishopPhone);

        TextView tvChurchEmail = new TextView(context);
        tvChurchEmail.setText("✉️ Church: info@perazimchurch.org");
        tvChurchEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvChurchEmail.setTextColor(COLOR_TEXT_MUTED);
        tvChurchEmail.setPadding(0, dp(2), 0, 0);
        contactBox.addView(tvChurchEmail);

        container.addView(btnConfirmGive);
        container.addView(contactBox);

        sv.addView(container);
        builder.setView(sv);
        final AlertDialog dlg = builder.create();

        btnConfirmGive.setOnClickListener(v -> {
            String amt = etAmount.getText().toString().trim();
            if (amt.isEmpty()) amt = "1000";
            awardXp(20);
            Toast.makeText(context, "🙏 Giving recorded for KES " + amt + " to Paybill 4069983! May God open the windows of heaven upon you! (+20 XP)", Toast.LENGTH_LONG).show();
            dlg.dismiss();
        });

        dlg.show();
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
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
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
    }
    public void restoreRoutineState(android.os.Bundle savedInstanceState) {
        if (savedInstanceState != null) routineStep = savedInstanceState.getInt(KEY_ROUTINE_STEP, 1);
    }
}
