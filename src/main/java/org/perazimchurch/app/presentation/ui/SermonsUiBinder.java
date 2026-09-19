package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;
import java.io.File;
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
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.domain.model.Sermon;
import org.perazimchurch.app.presentation.viewmodel.SermonsViewModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SermonsUiBinder binds the Sermons Tab UI to {@link SermonsViewModel}.
 * Features:
 * - Search bar for filtering sermons by title, preacher, or keyword.
 * - Series filter chips ("All", "Breakthrough 2026", "Walking in Faith", "Deliverance").
 * - Sermon card list displaying preacher, passage, duration, series badge, download status.
 * - Audio Player Controller with play/pause, seekbar progress, and speed toggle (1.0x, 1.25x, 1.5x).
 * - Download action button handling requestDownload (STREAMING -> DOWNLOADING -> DOWNLOADED) and deleteDownload.
 */
public class SermonsUiBinder {

    // --- CANONICAL PERAZIM PALETTE ---
    public static final int COLOR_PRIMARY_PURPLE = Color.parseColor("#681A7D");
    public static final int COLOR_PURPLE_DARK    = Color.parseColor("#2D0938");
    public static final int COLOR_PURPLE_SHADOW  = Color.parseColor("#450E53");
    public static final int COLOR_PURPLE_LIGHT   = Color.parseColor("#8E30A8");
    public static final int COLOR_PURPLE_SOFT    = Color.parseColor("#C896D8");
    public static final int COLOR_BORDER_GREY    = Color.parseColor("#E7D5EC");
    public static final int COLOR_PURPLE_TINT    = Color.parseColor("#F5ECF7");
    public static final int COLOR_TEXT_MUTED     = Color.parseColor("#725B78");
    public static final int COLOR_TEXT_DARK      = Color.parseColor("#1A1225");

    public static final int COLOR_ACCENT_ORANGE  = Color.parseColor("#E17D2F");
    public static final int COLOR_ORANGE_DARK    = Color.parseColor("#78350A");
    public static final int COLOR_ORANGE_SHADOW  = Color.parseColor("#98450B");
    public static final int COLOR_ORANGE_BORDER  = Color.parseColor("#F9DFCC");
    public static final int COLOR_ORANGE_TINT    = Color.parseColor("#FDF5EF");

    public static final int COLOR_WHITE          = Color.parseColor("#FFFFFF");
    public static final int COLOR_BG_NEUTRAL     = Color.parseColor("#FAF7FB");

    public static final int COLOR_GREEN_SUCCESS  = Color.parseColor("#15803D");
    public static final int COLOR_GREEN_TINT     = Color.parseColor("#DCFCE7");
    public static final int COLOR_AMBER_TINT     = Color.parseColor("#FEF3C7");
    public static final int COLOR_AMBER_TEXT     = Color.parseColor("#B45309");

    // Predefined series filter chips
    public static final String[] SERIES_CHIPS = {
            "All", "Breakthrough 2026", "Walking in Faith", "Deliverance"
    };

    private final Activity activity;
    private final SermonsViewModel viewModel;

    // UI Root & Containers
    private ScrollView rootScrollView;
    private LinearLayout contentLayout;
    private LinearLayout chipsContainer;
    private LinearLayout cardsContainer;
    private ProgressBar loadingBar;
    private TextView tvEmptyState;
    private EditText etSearch;
    private Button btnWatchLiveBroadcast;

    // Audio Player Controller Views
    private LinearLayout playerCard;
    private TextView tvPlayerTitle;
    private TextView tvPlayerSpeaker;
    private Button btnPlayPause;
    private SeekBar audioSeekBar;
    private TextView tvCurrentPos;
    private TextView tvTotalDuration;
    private Button btnSpeedToggle;

    // Audio Player State
    private Sermon activeSermon;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private float playbackSpeed = 1.0f; // 1.0f, 1.25f, 1.5f
    private int currentPositionSeconds = 0;
    private int totalDurationSeconds = 2700; // default 45 mins
    private final Handler playerHandler = new Handler(Looper.getMainLooper());
    private Runnable playerRunnable;

    // Data State
    private String selectedSeries = "All";
    private final List<Sermon> currentSermons = new ArrayList<>();
    private final Map<String, String> downloadStatusMap = new HashMap<>(); // sermonId -> "STREAMING" | "DOWNLOADING" | "DOWNLOADED"
    private final List<Button> chipButtons = new ArrayList<>();

    public SermonsUiBinder(@NonNull Activity activity, @NonNull SermonsViewModel viewModel) {
        this.activity = activity;
        this.viewModel = viewModel;
    }

    /**
     * Builds and binds the complete Sermons Tab view.
     */
    public ScrollView bind() {
        if (rootScrollView == null) {
            initViews();
            loadInitialData();
        }
        return rootScrollView;
    }

    /**
     * Binds into an existing ViewGroup container.
     */
    public void bind(@NonNull ViewGroup container) {
        ScrollView view = bind();
        container.removeAllViews();
        container.addView(view);
    }

    public Button getWatchLiveButton() {
        return btnWatchLiveBroadcast;
    }

    public Sermon getActiveSermon() {
        return activeSermon;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    private void initViews() {
        rootScrollView = new ScrollView(activity);
        rootScrollView.setFillViewport(true);
        rootScrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(16), dp(16), dp(36));

        // 1. Livestream Ready Card with Stage Photographic Banner
        View liveBanner = buildLiveStreamBanner();
        contentLayout.addView(liveBanner);

        // 2. Audio Player Controller
        playerCard = buildAudioPlayerCard();
        contentLayout.addView(playerCard);

        // 3. Search Bar
        View searchBar = buildSearchBar();
        contentLayout.addView(searchBar);

        // 4. Series Filter Chips
        View chipsRow = buildSeriesFilterChips();
        contentLayout.addView(chipsRow);

        // 5. Section Header
        TextView tvHeader = new TextView(activity);
        tvHeader.setText("SERMON ARCHIVES & AUDIO MESSAGES");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHeader.setPadding(0, dp(16), 0, dp(10));
        contentLayout.addView(tvHeader);

        // 6. Loading Indicator
        loadingBar = new ProgressBar(activity);
        loadingBar.setIndeterminate(true);
        loadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpLoad = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLoad.gravity = Gravity.CENTER_HORIZONTAL;
        lpLoad.setMargins(0, dp(16), 0, dp(16));
        loadingBar.setLayoutParams(lpLoad);
        contentLayout.addView(loadingBar);

        // 7. Empty State View
        tvEmptyState = new TextView(activity);
        tvEmptyState.setText("No sermons found matching your filter.\nTry adjusting your search query or series.");
        tvEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvEmptyState.setTextColor(COLOR_TEXT_MUTED);
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setPadding(dp(16), dp(24), dp(16), dp(24));
        tvEmptyState.setVisibility(View.GONE);
        contentLayout.addView(tvEmptyState);

        // 8. Sermon Cards Container
        cardsContainer = new LinearLayout(activity);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        contentLayout.addView(cardsContainer);

        rootScrollView.addView(contentLayout);

        // Setup Player Runnable
        playerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    if (mediaPlayer != null) {
                        try {
                            if (mediaPlayer.isPlaying()) {
                                int posMs = mediaPlayer.getCurrentPosition();
                                currentPositionSeconds = posMs / 1000;
                            } else {
                                currentPositionSeconds++;
                            }
                        } catch (Exception e) {
                            currentPositionSeconds++;
                        }
                    } else {
                        currentPositionSeconds++;
                    }

                    if (currentPositionSeconds >= totalDurationSeconds) {
                        currentPositionSeconds = totalDurationSeconds;
                        if (audioSeekBar != null) audioSeekBar.setProgress(currentPositionSeconds);
                        if (tvCurrentPos != null) tvCurrentPos.setText(formatDuration(currentPositionSeconds));
                        pausePlayback();
                    } else {
                        if (audioSeekBar != null) audioSeekBar.setProgress(currentPositionSeconds);
                        if (tvCurrentPos != null) tvCurrentPos.setText(formatDuration(currentPositionSeconds));
                        long delay = (long) (1000 / playbackSpeed);
                        playerHandler.postDelayed(this, Math.max(200, delay));
                    }
                }
            }
        };
    }

    // =========================================================================
    // 1. LIVESTREAM READY CARD
    // =========================================================================

    private View buildLiveStreamBanner() {
        LinearLayout liveCard = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        liveCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        liveCard.setLayoutParams(lp);

        Bitmap stageBmp = loadAssetBitmap("stage.jpg", 500);
        if (stageBmp != null) {
            ImageView ivStage = new ImageView(activity);
            ivStage.setImageBitmap(getRoundedCornerBitmap(stageBmp, dp(10)));
            ivStage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpStage = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(130));
            lpStage.setMargins(0, 0, 0, dp(10));
            ivStage.setLayoutParams(lpStage);
            liveCard.addView(ivStage);
        }

        LinearLayout liveBadgeRow = new LinearLayout(activity);
        liveBadgeRow.setOrientation(LinearLayout.HORIZONTAL);
        liveBadgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView redDot = new TextView(activity);
        redDot.setText("🔴");
        redDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        liveBadgeRow.addView(redDot);

        TextView liveTag = new TextView(activity);
        liveTag.setText(" LIVESTREAM BROADCAST");
        liveTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        liveTag.setTypeface(Typeface.DEFAULT_BOLD);
        liveTag.setTextColor(COLOR_ACCENT_ORANGE);
        liveBadgeRow.addView(liveTag);
        liveCard.addView(liveBadgeRow);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Walking in Faith: Overcoming Fear in Difficult Times");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_DARK);
        tvTitle.setPadding(0, dp(8), 0, dp(4));
        liveCard.addView(tvTitle);

        TextView tvSpeaker = new TextView(activity);
        tvSpeaker.setText("Speaker: Bishop Dr. David Mutweri | Central Campus");
        tvSpeaker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSpeaker.setTextColor(COLOR_TEXT_MUTED);
        tvSpeaker.setPadding(0, 0, 0, dp(12));
        liveCard.addView(tvSpeaker);

        btnWatchLiveBroadcast = new Button(activity);
        btnWatchLiveBroadcast.setText("▶ Watch Live Broadcast");
        btnWatchLiveBroadcast.setTextColor(COLOR_WHITE);
        btnWatchLiveBroadcast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnWatchLiveBroadcast.setTypeface(Typeface.DEFAULT_BOLD);
        btnWatchLiveBroadcast.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnWatchLiveBroadcast.setPadding(dp(14), dp(10), dp(14), dp(10));
        btnWatchLiveBroadcast.setOnClickListener(v -> {
            if (activeSermon == null && !currentSermons.isEmpty()) {
                startPlayback(currentSermons.get(0));
            } else if (activeSermon != null) {
                togglePlayPause();
            }
            Toast.makeText(activity, "Broadcasting Live: Walking in Faith", Toast.LENGTH_SHORT).show();
        });
        liveCard.addView(btnWatchLiveBroadcast);

        return liveCard;
    }

    // =========================================================================
    // 2. AUDIO PLAYER CONTROLLER
    // =========================================================================

    private LinearLayout buildAudioPlayerCard() {
        LinearLayout card = createCard(COLOR_WHITE, dp(16), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        // Header with status indicator
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvHeader = new TextView(activity);
        tvHeader.setText("AUDIO PLAYER CONTROLLER");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        LinearLayout.LayoutParams lpH = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvHeader.setLayoutParams(lpH);
        headerRow.addView(tvHeader);

        btnSpeedToggle = new Button(activity);
        btnSpeedToggle.setText("⚡ 1.0×");
        btnSpeedToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnSpeedToggle.setTypeface(Typeface.DEFAULT_BOLD);
        btnSpeedToggle.setTextColor(COLOR_PRIMARY_PURPLE);
        btnSpeedToggle.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        btnSpeedToggle.setPadding(dp(10), dp(4), dp(10), dp(4));
        btnSpeedToggle.setOnClickListener(v -> cycleSpeed());
        headerRow.addView(btnSpeedToggle);

        card.addView(headerRow);

        // Now Playing Sermon Info
        tvPlayerTitle = new TextView(activity);
        tvPlayerTitle.setText("Supernatural Breakthrough");
        tvPlayerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvPlayerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPlayerTitle.setTextColor(COLOR_TEXT_DARK);
        tvPlayerTitle.setPadding(0, dp(8), 0, dp(2));
        card.addView(tvPlayerTitle);

        tvPlayerSpeaker = new TextView(activity);
        tvPlayerSpeaker.setText("Bishop Dr. David Mutweri • 2 Samuel 5:17-21");
        tvPlayerSpeaker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPlayerSpeaker.setTextColor(COLOR_TEXT_MUTED);
        tvPlayerSpeaker.setPadding(0, 0, 0, dp(10));
        card.addView(tvPlayerSpeaker);

        // Seekbar
        audioSeekBar = new SeekBar(activity);
        audioSeekBar.setMax(totalDurationSeconds);
        audioSeekBar.setProgress(currentPositionSeconds);
        audioSeekBar.setPadding(dp(4), dp(8), dp(4), dp(8));
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentPositionSeconds = progress;
                    tvCurrentPos.setText(formatDuration(currentPositionSeconds));
                    if (mediaPlayer != null) {
                        try {
                            mediaPlayer.seekTo(progress * 1000);
                        } catch (Exception ignored) {}
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        card.addView(audioSeekBar);

        // Duration Labels Row
        LinearLayout timeRow = new LinearLayout(activity);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setPadding(dp(4), 0, dp(4), dp(10));

        tvCurrentPos = new TextView(activity);
        tvCurrentPos.setText(formatDuration(currentPositionSeconds));
        tvCurrentPos.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvCurrentPos.setTextColor(COLOR_TEXT_MUTED);
        LinearLayout.LayoutParams lpCur = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvCurrentPos.setLayoutParams(lpCur);
        timeRow.addView(tvCurrentPos);

        tvTotalDuration = new TextView(activity);
        tvTotalDuration.setText(formatDuration(totalDurationSeconds));
        tvTotalDuration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTotalDuration.setTextColor(COLOR_TEXT_MUTED);
        tvTotalDuration.setGravity(Gravity.END);
        timeRow.addView(tvTotalDuration);

        card.addView(timeRow);

        // Control Buttons Row
        LinearLayout ctrlRow = new LinearLayout(activity);
        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
        ctrlRow.setGravity(Gravity.CENTER_VERTICAL);

        btnPlayPause = new Button(activity);
        btnPlayPause.setText("▶ Play Sermon");
        btnPlayPause.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnPlayPause.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlayPause.setTextColor(COLOR_WHITE);
        btnPlayPause.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnPlayPause.setPadding(dp(16), dp(8), dp(16), dp(8));
        LinearLayout.LayoutParams lpPlay = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpPlay.setMargins(0, 0, dp(8), 0);
        btnPlayPause.setLayoutParams(lpPlay);
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        ctrlRow.addView(btnPlayPause);

        Button btnRewind = new Button(activity);
        btnRewind.setText("⏪ -15s");
        btnRewind.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnRewind.setTypeface(Typeface.DEFAULT_BOLD);
        btnRewind.setTextColor(COLOR_PRIMARY_PURPLE);
        btnRewind.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        btnRewind.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lpRewind = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRewind.setMargins(0, 0, dp(6), 0);
        btnRewind.setLayoutParams(lpRewind);
        btnRewind.setOnClickListener(v -> {
            currentPositionSeconds = Math.max(0, currentPositionSeconds - 15);
            audioSeekBar.setProgress(currentPositionSeconds);
            tvCurrentPos.setText(formatDuration(currentPositionSeconds));
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.seekTo(currentPositionSeconds * 1000);
                } catch (Exception ignored) {}
            }
        });
        ctrlRow.addView(btnRewind);

        Button btnForward = new Button(activity);
        btnForward.setText("+30s ⏩");
        btnForward.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnForward.setTypeface(Typeface.DEFAULT_BOLD);
        btnForward.setTextColor(COLOR_PRIMARY_PURPLE);
        btnForward.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        btnForward.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnForward.setOnClickListener(v -> {
            currentPositionSeconds = Math.min(totalDurationSeconds, currentPositionSeconds + 30);
            audioSeekBar.setProgress(currentPositionSeconds);
            tvCurrentPos.setText(formatDuration(currentPositionSeconds));
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.seekTo(currentPositionSeconds * 1000);
                } catch (Exception ignored) {}
            }
        });
        ctrlRow.addView(btnForward);

        card.addView(ctrlRow);

        return card;
    }

    // =========================================================================
    // 3. SEARCH BAR
    // =========================================================================

    private View buildSearchBar() {
        LinearLayout searchLayout = new LinearLayout(activity);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchLayout.setGravity(Gravity.CENTER_VERTICAL);
        searchLayout.setBackground(createPillBg(COLOR_WHITE, COLOR_BORDER_GREY));
        searchLayout.setPadding(dp(12), dp(2), dp(12), dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        searchLayout.setLayoutParams(lp);

        TextView searchIcon = new TextView(activity);
        searchIcon.setText("🔍");
        searchIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        searchIcon.setPadding(0, 0, dp(8), 0);
        searchLayout.addView(searchIcon);

        etSearch = new EditText(activity);
        etSearch.setHint("Search by title, preacher, keyword...");
        etSearch.setHintTextColor(COLOR_TEXT_MUTED);
        etSearch.setTextColor(COLOR_TEXT_DARK);
        etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etSearch.setBackground(null);
        etSearch.setSingleLine(true);
        LinearLayout.LayoutParams lpEdit = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etSearch.setLayoutParams(lpEdit);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s != null ? s.toString().trim() : "";
                if (q.isEmpty()) {
                    onChipSelected(selectedSeries);
                } else {
                    viewModel.searchSermons(q, sermons -> activity.runOnUiThread(() -> renderSermonCards(sermons)));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchLayout.addView(etSearch);

        final Button btnClear = new Button(activity);
        btnClear.setText("✕");
        btnClear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnClear.setTextColor(COLOR_TEXT_MUTED);
        btnClear.setBackground(null);
        btnClear.setPadding(dp(4), dp(4), dp(4), dp(4));
        btnClear.setOnClickListener(v -> etSearch.setText(""));
        searchLayout.addView(btnClear);

        return searchLayout;
    }

    // =========================================================================
    // 4. SERIES FILTER CHIPS
    // =========================================================================

    private View buildSeriesFilterChips() {
        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams lpHsv = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHsv.setMargins(0, 0, 0, dp(10));
        hsv.setLayoutParams(lpHsv);

        chipsContainer = new LinearLayout(activity);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipsContainer.setPadding(0, dp(2), 0, dp(4));

        chipButtons.clear();
        for (String series : SERIES_CHIPS) {
            Button btn = new Button(activity);
            btn.setText(series);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setPadding(dp(14), dp(6), dp(14), dp(6));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
            lp.setMargins(0, 0, dp(8), 0);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> onChipSelected(series));
            chipButtons.add(btn);
            chipsContainer.addView(btn);
        }

        updateChipStyles();
        hsv.addView(chipsContainer);
        return hsv;
    }

    private void onChipSelected(String series) {
        selectedSeries = series;
        updateChipStyles();
        loadingBar.setVisibility(View.VISIBLE);
        viewModel.filterBySeries(series, sermons -> activity.runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            renderSermonCards(sermons);
        }));
    }

    private void updateChipStyles() {
        for (Button btn : chipButtons) {
            boolean isSelected = btn.getText().toString().equalsIgnoreCase(selectedSeries);
            if (isSelected) {
                btn.setTextColor(COLOR_WHITE);
                btn.setBackground(createPillBg(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW));
            } else {
                btn.setTextColor(COLOR_TEXT_DARK);
                btn.setBackground(createPillBg(COLOR_WHITE, COLOR_BORDER_GREY));
            }
        }
    }

    // =========================================================================
    // 5. DATA LOADING & SERMON CARDS RENDERING
    // =========================================================================

    public void loadInitialData() {
        loadingBar.setVisibility(View.VISIBLE);
        viewModel.loadSermons(sermons -> activity.runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            renderSermonCards(sermons);
            if (sermons != null && !sermons.isEmpty() && activeSermon == null) {
                loadSermonIntoPlayer(sermons.get(0), false);
            }
        }));
    }

    private void renderSermonCards(List<Sermon> sermons) {
        currentSermons.clear();
        cardsContainer.removeAllViews();

        if (sermons == null || sermons.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        currentSermons.addAll(sermons);

        for (final Sermon sermon : sermons) {
            View card = createSermonCard(sermon);
            cardsContainer.addView(card);
        }
    }

    private View createSermonCard(final Sermon sermon) {
        final LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        // Top Row: Preacher avatar thumbnail + Details
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        Bitmap thumbBmp = loadAssetBitmap("bishop_portrait.jpg", 150);
        if (thumbBmp != null) {
            ImageView ivThumb = new ImageView(activity);
            ivThumb.setImageBitmap(getCircularBitmap(thumbBmp));
            LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dp(46), dp(46));
            lpImg.setMargins(0, 0, dp(10), 0);
            ivThumb.setLayoutParams(lpImg);
            topRow.addView(ivThumb);
        }

        LinearLayout infoCol = new LinearLayout(activity);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpInfo = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        infoCol.setLayoutParams(lpInfo);

        TextView tvPreacher = new TextView(activity);
        tvPreacher.setText("🎙 " + (sermon.getPreacher() != null ? sermon.getPreacher() : "Bishop Dr. David Mutweri"));
        tvPreacher.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPreacher.setTypeface(Typeface.DEFAULT_BOLD);
        tvPreacher.setTextColor(COLOR_PRIMARY_PURPLE);
        infoCol.addView(tvPreacher);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(sermon.getTitle() != null ? sermon.getTitle() : "Untitled Sermon");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_DARK);
        tvTitle.setPadding(0, dp(2), 0, dp(2));
        infoCol.addView(tvTitle);

        String passage = sermon.getPassage() != null && !sermon.getPassage().isEmpty()
                ? sermon.getPassage() : "2 Samuel 5:17-21";
        long durationSec = sermon.getDurationSeconds() > 0 ? sermon.getDurationSeconds() : 2700;
        String durationStr = (durationSec / 60) + " mins";

        TextView tvMeta = new TextView(activity);
        tvMeta.setText("📖 " + passage + "  •  ⏱ " + durationStr);
        tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvMeta.setTextColor(COLOR_TEXT_MUTED);
        infoCol.addView(tvMeta);

        topRow.addView(infoCol);
        card.addView(topRow);

        // Middle Row: Badges (Series badge + Download status badge)
        LinearLayout badgeRow = new LinearLayout(activity);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        badgeRow.setPadding(0, dp(8), 0, dp(10));

        // Series badge
        String seriesName = sermon.getSeries() != null && !sermon.getSeries().isEmpty()
                ? sermon.getSeries() : "Breakthrough Series";
        TextView tvSeriesBadge = new TextView(activity);
        tvSeriesBadge.setText("🏷 " + seriesName);
        tvSeriesBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvSeriesBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvSeriesBadge.setTextColor(COLOR_PRIMARY_PURPLE);
        tvSeriesBadge.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        tvSeriesBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams lpSeries = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpSeries.setMargins(0, 0, dp(6), 0);
        tvSeriesBadge.setLayoutParams(lpSeries);
        badgeRow.addView(tvSeriesBadge);

        // Download status badge
        final TextView tvDownloadBadge = new TextView(activity);
        String initialStatus = downloadStatusMap.containsKey(sermon.getId())
                ? downloadStatusMap.get(sermon.getId())
                : (sermon.isDownloaded() ? "DOWNLOADED" : "STREAMING");
        updateDownloadBadgeView(tvDownloadBadge, initialStatus);
        badgeRow.addView(tvDownloadBadge);

        card.addView(badgeRow);

        // Bottom Row: Play Button & Download Action Button
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);

        Button btnPlay = new Button(activity);
        btnPlay.setText("▶ Play Message");
        btnPlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnPlay.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlay.setTextColor(COLOR_WHITE);
        btnPlay.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 8, 2));
        btnPlay.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams lpBtnPlay = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpBtnPlay.setMargins(0, 0, dp(6), 0);
        btnPlay.setLayoutParams(lpBtnPlay);
        btnPlay.setOnClickListener(v -> startPlayback(sermon));
        actionRow.addView(btnPlay);

        final Button btnDownload = new Button(activity);
        updateDownloadActionButton(btnDownload, initialStatus);
        LinearLayout.LayoutParams lpBtnDl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpBtnDl.setMargins(dp(6), 0, 0, 0);
        btnDownload.setLayoutParams(lpBtnDl);

        btnDownload.setOnClickListener(v -> handleDownloadAction(sermon, tvDownloadBadge, btnDownload));
        actionRow.addView(btnDownload);

        card.addView(actionRow);

        // Tapping card loads sermon into player
        card.setOnClickListener(v -> startPlayback(sermon));

        return card;
    }

    private void updateDownloadBadgeView(TextView badge, String status) {
        if ("DOWNLOADED".equals(status)) {
            badge.setText("✅ Downloaded");
            badge.setTextColor(COLOR_GREEN_SUCCESS);
            badge.setBackground(createPillBg(COLOR_GREEN_TINT, COLOR_GREEN_SUCCESS));
        } else if ("DOWNLOADING".equals(status)) {
            badge.setText("⏳ DOWNLOADING...");
            badge.setTextColor(COLOR_AMBER_TEXT);
            badge.setBackground(createPillBg(COLOR_AMBER_TINT, COLOR_ACCENT_ORANGE));
        } else {
            badge.setText("☁️ Streaming");
            badge.setTextColor(COLOR_PRIMARY_PURPLE);
            badge.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        }
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
    }

    private void updateDownloadActionButton(Button btn, String status) {
        if ("DOWNLOADED".equals(status)) {
            btn.setText("🗑 Delete Download");
            btn.setTextColor(COLOR_TEXT_MUTED);
            btn.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        } else if ("DOWNLOADING".equals(status)) {
            btn.setText("⏳ Downloading...");
            btn.setTextColor(COLOR_AMBER_TEXT);
            btn.setBackground(createPillBg(COLOR_AMBER_TINT, COLOR_ACCENT_ORANGE));
        } else {
            btn.setText("📥 Download");
            btn.setTextColor(COLOR_WHITE);
            btn.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 8, 2));
        }
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(10), dp(6), dp(10), dp(6));
    }

    // =========================================================================
    // 6. DOWNLOAD ACTION HANDLER
    // =========================================================================

    private void handleDownloadAction(final Sermon sermon, final TextView badge, final Button btn) {
        String currentStatus = downloadStatusMap.containsKey(sermon.getId())
                ? downloadStatusMap.get(sermon.getId())
                : (sermon.isDownloaded() ? "DOWNLOADED" : "STREAMING");

        if ("STREAMING".equals(currentStatus)) {
            // Transition: STREAMING -> DOWNLOADING -> DOWNLOADED
            downloadStatusMap.put(sermon.getId(), "DOWNLOADING");
            updateDownloadBadgeView(badge, "DOWNLOADING");
            updateDownloadActionButton(btn, "DOWNLOADING");
            Toast.makeText(activity, "Offline Queue: Download staged. Remote media delivery unlocks in Phase 4.", Toast.LENGTH_SHORT).show();

            viewModel.requestDownload(sermon.getId(), success -> activity.runOnUiThread(() -> {
                if (Boolean.TRUE.equals(success)) {
                    downloadStatusMap.put(sermon.getId(), "DOWNLOADED");
                    sermon.setDownloaded(true);
                    updateDownloadBadgeView(badge, "DOWNLOADED");
                    updateDownloadActionButton(btn, "DOWNLOADED");
                    Toast.makeText(activity, "Offline Queue: Download staged. Remote media delivery unlocks in Phase 4.", Toast.LENGTH_SHORT).show();
                } else {
                    downloadStatusMap.put(sermon.getId(), "STREAMING");
                    updateDownloadBadgeView(badge, "STREAMING");
                    updateDownloadActionButton(btn, "STREAMING");
                    Toast.makeText(activity, "Download failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }));
        } else if ("DOWNLOADED".equals(currentStatus)) {
            // Options: Play Offline or Delete
            new AlertDialog.Builder(activity)
                    .setTitle("Manage Download")
                    .setMessage(sermon.getTitle() + " is saved offline. What would you like to do?")
                    .setPositiveButton("▶ Play Offline", (dialog, which) -> {
                        startPlayback(sermon);
                        Toast.makeText(activity, "Playing from local storage", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("🗑 Delete Download", (dialog, which) -> {
                        viewModel.deleteDownload(sermon.getId(), success -> activity.runOnUiThread(() -> {
                            downloadStatusMap.put(sermon.getId(), "STREAMING");
                            sermon.setDownloaded(false);
                            updateDownloadBadgeView(badge, "STREAMING");
                            updateDownloadActionButton(btn, "STREAMING");
                            Toast.makeText(activity, "Download removed.", Toast.LENGTH_SHORT).show();
                        }));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    // =========================================================================
    // 7. AUDIO CONTROLLER OPERATIONS & PLAYBACK
    // =========================================================================

    public void startPlayback(Sermon sermon) {
        if (sermon == null) return;
        loadSermonIntoPlayer(sermon, true);
    }

    private void loadSermonIntoPlayer(Sermon sermon, boolean autoPlay) {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPlaying = false;
        playerHandler.removeCallbacks(playerRunnable);

        activeSermon = sermon;
        tvPlayerTitle.setText(sermon.getTitle() != null ? sermon.getTitle() : "Untitled Sermon");
        String preacher = sermon.getPreacher() != null ? sermon.getPreacher() : "Bishop Dr. David Mutweri";
        String passage = sermon.getPassage() != null ? sermon.getPassage() : "2 Samuel 5:17-21";
        tvPlayerSpeaker.setText(preacher + " • " + passage);

        totalDurationSeconds = (int) (sermon.getDurationSeconds() > 0 ? sermon.getDurationSeconds() : 2700);
        currentPositionSeconds = 0;
        audioSeekBar.setMax(totalDurationSeconds);
        audioSeekBar.setProgress(0);
        tvCurrentPos.setText("00:00");
        tvTotalDuration.setText(formatDuration(totalDurationSeconds));

        if (autoPlay) {
            resumePlayback();
        }
    }

    public void togglePlayPause() {
        if (isPlaying) {
            pausePlayback();
        } else {
            resumePlayback();
        }
    }

    public void resumePlayback() {
        isPlaying = true;
        btnPlayPause.setText("⏸ Pause Sermon");
        btnPlayPause.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                boolean sourceSet = false;
                String localPath = activeSermon != null ? activeSermon.getLocalAudioPath() : null;
                if (localPath != null && !localPath.isEmpty()) {
                    File file = new File(localPath);
                    if (file.exists() && file.isFile()) {
                        try {
                            mediaPlayer.setDataSource(localPath);
                            sourceSet = true;
                        } catch (Exception e) {
                            Log.w("SermonsUiBinder", "Failed to set data source from local path: " + localPath, e);
                        }
                    }
                }

                if (!sourceSet) {
                    try {
                        AssetFileDescriptor afd = activity.getAssets().openFd("audio/sample_sermon.mp3");
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        sourceSet = true;
                    } catch (Exception e) {
                        Log.w("SermonsUiBinder", "Failed to load audio/sample_sermon.mp3 asset", e);
                    }
                }

                if (sourceSet) {
                    mediaPlayer.setOnCompletionListener(mp -> activity.runOnUiThread(() -> {
                        pausePlayback();
                        currentPositionSeconds = 0;
                        if (audioSeekBar != null) audioSeekBar.setProgress(0);
                        if (tvCurrentPos != null) tvCurrentPos.setText("00:00");
                    }));
                    mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                        Log.w("SermonsUiBinder", "MediaPlayer error: what=" + what + ", extra=" + extra);
                        return true;
                    });
                    mediaPlayer.prepare();
                    if (currentPositionSeconds > 0) {
                        mediaPlayer.seekTo(currentPositionSeconds * 1000);
                    }
                    mediaPlayer.start();
                    int durationMs = mediaPlayer.getDuration();
                    if (durationMs > 0) {
                        totalDurationSeconds = durationMs / 1000;
                        if (audioSeekBar != null) audioSeekBar.setMax(totalDurationSeconds);
                        if (tvTotalDuration != null) tvTotalDuration.setText(formatDuration(totalDurationSeconds));
                    }
                }
            } else {
                mediaPlayer.start();
            }

            if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
                } catch (Exception e) {
                    Log.w("SermonsUiBinder", "Could not set playback speed on MediaPlayer", e);
                }
            }
        } catch (Throwable t) {
            Log.w("SermonsUiBinder", "MediaPlayer playback initialization warning", t);
        }

        playerHandler.removeCallbacks(playerRunnable);
        playerHandler.post(playerRunnable);
    }

    public void pausePlayback() {
        isPlaying = false;
        btnPlayPause.setText("▶ Play Sermon");
        btnPlayPause.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            } catch (Exception e) {
                Log.w("SermonsUiBinder", "Error pausing MediaPlayer", e);
            }
        }
        playerHandler.removeCallbacks(playerRunnable);
    }

    public void cycleSpeed() {
        if (Math.abs(playbackSpeed - 1.0f) < 0.05f) {
            playbackSpeed = 1.25f;
        } else if (Math.abs(playbackSpeed - 1.25f) < 0.05f) {
            playbackSpeed = 1.5f;
        } else {
            playbackSpeed = 1.0f;
        }
        btnSpeedToggle.setText(String.format(Locale.US, "⚡ %.2f×", playbackSpeed).replace(".00", ".0"));
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
            } catch (Exception ignored) {}
        }
        Toast.makeText(activity, "Playback speed: " + btnSpeedToggle.getText(), Toast.LENGTH_SHORT).show();
    }

    public void release() {
        isPlaying = false;
        playerHandler.removeCallbacks(playerRunnable);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    // =========================================================================
    // 8. GRAPHICS & DRAWABLE UTILITIES
    // =========================================================================

    private String formatDuration(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", mins, secs);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
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
        LinearLayout card = new LinearLayout(activity);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        card.setBackground(drawable);
        return card;
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
            InputStream is = activity.getAssets().open(name);
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int sampleSize = 1;
            while ((opts.outWidth / sampleSize) > maxDim || (opts.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            is = activity.getAssets().open(name);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap, int cornerRadiusPx) {
        if (bitmap == null) return null;
        try {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(0xff424242);
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return output;
        } catch (Throwable t) {
            return bitmap;
        }
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, (size - bitmap.getWidth()) / 2f, (size - bitmap.getHeight()) / 2f, paint);
        return output;
    }
}
