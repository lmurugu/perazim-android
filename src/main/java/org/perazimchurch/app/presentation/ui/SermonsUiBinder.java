package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

import org.perazimchurch.app.data.repository.YouTubeFeedRepository;
import org.perazimchurch.app.LocalBibleAudioActivity;
import org.perazimchurch.app.domain.model.Sermon;
import org.perazimchurch.app.presentation.viewmodel.SermonsViewModel;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SermonsUiBinder binds the Sermons Tab UI to {@link SermonsViewModel}.
 * Adheres strictly to the quiet dark contemplative theme:
 * - Canvas: #2A0F33 (COLOR_BG_DEEP)
 * - Search bar: quiet surface #4F1E5C with stroke #4A3554
 * - Filter chips: All | Series | Downloaded. Outline pill; active is solid #681A7D
 * - Sermon cards on surface #3D1749 with title (#F5F0F7), preacher/passage (#B9A8C0), duration/date (#8B5A9A)
 * - Play affordance: quiet circle with play arrow (purple, NOT massive orange button)
 * - Download affordance: quiet outline button
 * - Audio controller: quiet surface #3D1749 with stroke #4A3554, scrub bar #E17D2F
 */
public class SermonsUiBinder {

    // Predefined series filter chips
    public static final String[] SERIES_CHIPS = {
            "All", "Series", "Downloaded", "Video"
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
    private View emptyStateContainer;
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
    private float playbackSpeed = 1.0f;
    private int currentPositionSeconds = 0;
    private int totalDurationSeconds = 2700;
    private final Handler playerHandler = new Handler(Looper.getMainLooper());
    private Runnable playerRunnable;

    // Data State
    private String selectedChip = "All";
    private final List<Sermon> currentSermons = new ArrayList<>();
    private final Map<String, String> downloadStatusMap = new HashMap<>();
    private final List<Button> chipButtons = new ArrayList<>();

    public SermonsUiBinder(@NonNull Activity activity, @NonNull SermonsViewModel viewModel) {
        this.activity = activity;
        this.viewModel = viewModel;
    }

    public ScrollView bind() {
        if (rootScrollView == null) {
            initViews();
            loadInitialData();
        }
        return rootScrollView;
    }

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
        rootScrollView.setBackground(PerazimTheme.createBackgroundGradient());

        contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(16), dp(16), dp(36));

        // 1. Title
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.VERTICAL);
        headerRow.setPadding(0, dp(4), 0, dp(12));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Sermons");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        headerRow.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("Listen to messages and live worship from Perazim Mission Church");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSub.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvSub.setPadding(0, dp(2), 0, 0);
        headerRow.addView(tvSub);

        Button localBibleAudio = new Button(activity);
        localBibleAudio.setText("Bible audio on this phone");
        localBibleAudio.setOnClickListener(v -> activity.startActivity(new Intent(activity, LocalBibleAudioActivity.class)));
        LinearLayout.LayoutParams localAudioLp = new LinearLayout.LayoutParams(-2, -2);
        localAudioLp.topMargin = dp(8);
        headerRow.addView(localBibleAudio, localAudioLp);

        contentLayout.addView(headerRow);

        // 2. Livestream Card (Quiet)
        View liveBanner = buildLiveStreamBanner();
        contentLayout.addView(liveBanner);

        // 3. Audio Player Controller
        playerCard = buildAudioPlayerCard();
        contentLayout.addView(playerCard);

        // 4. Search Bar on #4F1E5C
        View searchBar = buildSearchBar();
        contentLayout.addView(searchBar);

        // 5. Filter Chips: All | Series | Downloaded
        View chipsRow = buildSeriesFilterChips();
        contentLayout.addView(chipsRow);

        // 6. Section Header
        TextView tvHeader = new TextView(activity);
        tvHeader.setText("AUDIO MESSAGES & ARCHIVES");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvHeader.setPadding(0, dp(10), 0, dp(8));
        contentLayout.addView(tvHeader);

        // 7. Loading Indicator
        loadingBar = new ProgressBar(activity);
        loadingBar.setIndeterminate(true);
        loadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpLoad = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLoad.gravity = Gravity.CENTER_HORIZONTAL;
        lpLoad.setMargins(0, dp(16), 0, dp(16));
        loadingBar.setLayoutParams(lpLoad);
        contentLayout.addView(loadingBar);

        // 8. Empty State View with Illustrated Vignette
        LinearLayout emptyBox = new LinearLayout(activity);
        emptyBox.setOrientation(LinearLayout.VERTICAL);
        emptyBox.setGravity(Gravity.CENTER);
        emptyBox.setPadding(dp(16), dp(24), dp(16), dp(24));
        emptyBox.setVisibility(View.GONE);

        Bitmap emptyHillBmp = loadAssetBitmap("vignettes/vignette_empty_hill.png", 300);
        if (emptyHillBmp != null) {
            ImageView ivEmpty = new ImageView(activity);
            ivEmpty.setImageBitmap(emptyHillBmp);
            ivEmpty.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivEmpty.setAlpha(0.85f);
            LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dp(110), dp(110));
            lpImg.setMargins(0, 0, 0, dp(12));
            ivEmpty.setLayoutParams(lpImg);
            emptyBox.addView(ivEmpty);
        }

        tvEmptyState = new TextView(activity);
        tvEmptyState.setText("No sermons found matching your filter.\nTry adjusting your search query or filter chip.");
        tvEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvEmptyState.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvEmptyState.setGravity(Gravity.CENTER);
        emptyBox.addView(tvEmptyState);

        emptyStateContainer = emptyBox;
        contentLayout.addView(emptyStateContainer);

        // 9. Sermon Cards Container
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
    // 1. LIVESTREAM READY CARD (QUIET)
    // =========================================================================

    private View buildLiveStreamBanner() {
        FrameLayout rootFrame = new FrameLayout(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
        lp.setMargins(0, 0, 0, dp(14));
        rootFrame.setLayoutParams(lp);
        rootFrame.setBackground(PerazimTheme.createHeroGradient(dp(14), dp(1)));
        rootFrame.setClipToOutline(true);

        Bitmap lampBmp = loadAssetBitmap("backgrounds/bg_sermons_lamp.png", 720);
        if (lampBmp != null) {
            ImageView ivBg = new ImageView(activity);
            ivBg.setImageBitmap(lampBmp);
            ivBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            rootFrame.addView(ivBg, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            View bottomScrim = new View(activity);
            GradientDrawable scrimGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#E62A0F33")});
            bottomScrim.setBackground(scrimGrad);
            rootFrame.addView(bottomScrim, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        LinearLayout liveCard = new LinearLayout(activity);
        liveCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        liveCard.setOrientation(LinearLayout.VERTICAL);
        liveCard.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout liveBadgeRow = new LinearLayout(activity);
        liveBadgeRow.setOrientation(LinearLayout.HORIZONTAL);
        liveBadgeRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView ivLogo = new ImageView(activity);
        ivLogo.setImageResource(org.perazimchurch.app.R.drawable.ic_perazim_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(16), dp(16));
        lpLogo.setMargins(0, 0, dp(6), 0);
        ivLogo.setLayoutParams(lpLogo);
        liveBadgeRow.addView(ivLogo);

        TextView liveTag = new TextView(activity);
        liveTag.setText("LIVESTREAM BROADCAST");
        liveTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        liveTag.setTypeface(Typeface.DEFAULT_BOLD);
        liveTag.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        liveTag.setBackground(PerazimTheme.createSurface2Gradient(dp(6), dp(1)));
        liveTag.setPadding(dp(8), dp(3), dp(8), dp(3));
        liveBadgeRow.addView(liveTag);
        liveCard.addView(liveBadgeRow);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Walking in Faith: Overcoming Fear in Difficult Times");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvTitle.setPadding(0, dp(6), 0, dp(2));
        liveCard.addView(tvTitle);

        TextView tvSpeaker = new TextView(activity);
        tvSpeaker.setText("Bishop Dr. David Mutweri · Central Campus");
        tvSpeaker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSpeaker.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvSpeaker.setPadding(0, 0, 0, dp(10));
        liveCard.addView(tvSpeaker);

        btnWatchLiveBroadcast = new Button(activity);
        btnWatchLiveBroadcast.setText("Watch Live Broadcast");
        btnWatchLiveBroadcast.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnWatchLiveBroadcast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnWatchLiveBroadcast.setTypeface(Typeface.DEFAULT_BOLD);
        btnWatchLiveBroadcast.setBackground(createSolidPill(PerazimTheme.COLOR_PURPLE_BRAND, 10));
        btnWatchLiveBroadcast.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnWatchLiveBroadcast.setOnClickListener(v -> {
            Toast.makeText(activity, "Connecting to Perazim Live Broadcast...", Toast.LENGTH_SHORT).show();
            YouTubeFeedRepository.openLiveBroadcast(activity);
        });
        liveCard.addView(btnWatchLiveBroadcast);

        rootFrame.addView(liveCard);
        return rootFrame;
    }

    // =========================================================================
    // 2. AUDIO PLAYER CONTROLLER
    // =========================================================================

    private LinearLayout buildAudioPlayerCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, 14, PerazimTheme.COLOR_STROKE, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        // Header with speed toggle
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvHeader = new TextView(activity);
        tvHeader.setText("AUDIO CONTROLLER · SAMPLE AUDIO PREVIEW");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        LinearLayout.LayoutParams lpH = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvHeader.setLayoutParams(lpH);
        headerRow.addView(tvHeader);

        Button btnStopHeader = new Button(activity);
        btnStopHeader.setText("⏹ Stop");
        btnStopHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnStopHeader.setTypeface(Typeface.DEFAULT_BOLD);
        btnStopHeader.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnStopHeader.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 8, PerazimTheme.COLOR_STROKE, 1));
        btnStopHeader.setPadding(dp(10), dp(4), dp(10), dp(4));
        btnStopHeader.setOnClickListener(v -> stopPlayback());
        headerRow.addView(btnStopHeader);

        btnSpeedToggle = new Button(activity);
        btnSpeedToggle.setText("1.0×");
        btnSpeedToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnSpeedToggle.setTypeface(Typeface.DEFAULT_BOLD);
        btnSpeedToggle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnSpeedToggle.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 8, PerazimTheme.COLOR_STROKE, 1));
        btnSpeedToggle.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams lpSpeed = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSpeed.setMargins(dp(6), 0, 0, 0);
        btnSpeedToggle.setLayoutParams(lpSpeed);
        btnSpeedToggle.setOnClickListener(v -> cycleSpeed());
        headerRow.addView(btnSpeedToggle);

        card.addView(headerRow);

        // Title & Speaker
        tvPlayerTitle = new TextView(activity);
        tvPlayerTitle.setText("Supernatural Breakthrough (Sample Preview)");
        tvPlayerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvPlayerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPlayerTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvPlayerTitle.setPadding(0, dp(6), 0, dp(2));
        card.addView(tvPlayerTitle);

        tvPlayerSpeaker = new TextView(activity);
        tvPlayerSpeaker.setText("Bishop Dr. David Mutweri • 2 Samuel 5:17-21");
        tvPlayerSpeaker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPlayerSpeaker.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvPlayerSpeaker.setPadding(0, 0, 0, dp(8));
        card.addView(tvPlayerSpeaker);

        // Seekbar with Orange Scrub
        audioSeekBar = new SeekBar(activity);
        audioSeekBar.setMax(totalDurationSeconds);
        audioSeekBar.setProgress(currentPositionSeconds);
        audioSeekBar.setPadding(dp(4), dp(6), dp(4), dp(6));
        try {
            audioSeekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PerazimTheme.COLOR_ORANGE_BRAND));
            audioSeekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(PerazimTheme.COLOR_ORANGE_BRAND));
        } catch (Throwable ignored) {}

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

        // Duration Labels
        LinearLayout timeRow = new LinearLayout(activity);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setPadding(dp(4), 0, dp(4), dp(8));

        tvCurrentPos = new TextView(activity);
        tvCurrentPos.setText(formatDuration(currentPositionSeconds));
        tvCurrentPos.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvCurrentPos.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        LinearLayout.LayoutParams lpCur = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvCurrentPos.setLayoutParams(lpCur);
        timeRow.addView(tvCurrentPos);

        tvTotalDuration = new TextView(activity);
        tvTotalDuration.setText(formatDuration(totalDurationSeconds));
        tvTotalDuration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTotalDuration.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvTotalDuration.setGravity(Gravity.END);
        timeRow.addView(tvTotalDuration);

        card.addView(timeRow);

        // Controls: Rewind, Play/Pause (#681A7D), Forward, Stop
        LinearLayout ctrlRow = new LinearLayout(activity);
        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
        ctrlRow.setGravity(Gravity.CENTER_VERTICAL);

        Button btnRewind = new Button(activity);
        btnRewind.setText("-15s");
        btnRewind.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnRewind.setTypeface(Typeface.DEFAULT_BOLD);
        btnRewind.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnRewind.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 8, PerazimTheme.COLOR_STROKE, 1));
        btnRewind.setPadding(dp(8), dp(6), dp(8), dp(6));
        btnRewind.setMinWidth(0);
        btnRewind.setMinimumWidth(0);
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

        btnPlayPause = new Button(activity);
        btnPlayPause.setText("Play Sermon");
        btnPlayPause.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnPlayPause.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlayPause.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnPlayPause.setBackground(createSolidPill(PerazimTheme.COLOR_PURPLE_BRAND, 10));
        btnPlayPause.setPadding(dp(4), dp(8), dp(4), dp(8));
        btnPlayPause.setAllCaps(false);
        btnPlayPause.setMinWidth(0);
        btnPlayPause.setMinimumWidth(0);
        LinearLayout.LayoutParams lpPlay = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpPlay.setMargins(dp(8), 0, dp(8), 0);
        btnPlayPause.setLayoutParams(lpPlay);
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        ctrlRow.addView(btnPlayPause);

        Button btnForward = new Button(activity);
        btnForward.setText("+30s");
        btnForward.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnForward.setTypeface(Typeface.DEFAULT_BOLD);
        btnForward.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnForward.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 8, PerazimTheme.COLOR_STROKE, 1));
        btnForward.setPadding(dp(8), dp(6), dp(8), dp(6));
        btnForward.setMinWidth(0);
        btnForward.setMinimumWidth(0);
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

        Button btnStop = new Button(activity);
        btnStop.setText("⏹ Stop");
        btnStop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnStop.setTypeface(Typeface.DEFAULT_BOLD);
        btnStop.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnStop.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 8, PerazimTheme.COLOR_STROKE, 1));
        btnStop.setPadding(dp(8), dp(6), dp(8), dp(6));
        btnStop.setMinWidth(0);
        btnStop.setMinimumWidth(0);
        btnStop.setOnClickListener(v -> stopPlayback());
        LinearLayout.LayoutParams lpStop = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpStop.setMargins(dp(6), 0, 0, 0);
        btnStop.setLayoutParams(lpStop);
        ctrlRow.addView(btnStop);

        card.addView(ctrlRow);

        return card;
    }

    // =========================================================================
    // 3. SEARCH BAR (QUIET SURFACE #4F1E5C)
    // =========================================================================

    private View buildSearchBar() {
        LinearLayout searchLayout = new LinearLayout(activity);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchLayout.setGravity(Gravity.CENTER_VERTICAL);
        searchLayout.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 12, PerazimTheme.COLOR_STROKE, 1));
        searchLayout.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        searchLayout.setLayoutParams(lp);

        ImageView ivSearch = new ImageView(activity);
        ivSearch.setImageResource(org.perazimchurch.app.R.drawable.ic_search);
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(18), dp(18));
        lpIcon.setMargins(0, 0, dp(8), 0);
        ivSearch.setLayoutParams(lpIcon);
        searchLayout.addView(ivSearch);

        etSearch = new EditText(activity);
        etSearch.setHint("Search title, preacher, passage…");
        etSearch.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etSearch.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
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
                    onChipSelected(selectedChip);
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
        btnClear.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        btnClear.setBackground(null);
        btnClear.setPadding(dp(4), dp(4), dp(4), dp(4));
        btnClear.setOnClickListener(v -> etSearch.setText(""));
        searchLayout.addView(btnClear);

        return searchLayout;
    }

    public void setSearchQuery(String query) {
        if (etSearch != null) {
            etSearch.setText(query != null ? query : "");
            if (query != null) {
                etSearch.setSelection(query.length());
            }
        }
    }

    public void startSamplePlayback() {
        Sermon sample = new Sermon();
        sample.setId("sample-sermon-1");
        sample.setTitle("Supernatural Breakthrough");
        sample.setPreacher("Bishop Dr. David Mutweri");
        sample.setPassage("2 Samuel 5:17-21");
        sample.setDurationSeconds(2700);
        startPlayback(sample);
    }

    // =========================================================================
    // 4. FILTER CHIPS (All | Series | Downloaded)
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
        for (String chip : SERIES_CHIPS) {
            Button btn = new Button(activity);
            btn.setText(chip);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setPadding(dp(14), dp(4), dp(14), dp(4));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
            lp.setMargins(0, 0, dp(8), 0);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> onChipSelected(chip));
            chipButtons.add(btn);
            chipsContainer.addView(btn);
        }

        updateChipStyles();
        hsv.addView(chipsContainer);
        return hsv;
    }

    private void onChipSelected(String chip) {
        selectedChip = chip;
        updateChipStyles();
        loadingBar.setVisibility(View.VISIBLE);

        if ("Video".equalsIgnoreCase(chip)) {
            YouTubeFeedRepository.fetchRecentVideos(videos -> activity.runOnUiThread(() -> {
                loadingBar.setVisibility(View.GONE);
                renderSermonCards(videos);
            }));
            return;
        }

        if ("Downloaded".equalsIgnoreCase(chip)) {
            viewModel.loadSermons(sermons -> activity.runOnUiThread(() -> {
                loadingBar.setVisibility(View.GONE);
                List<Sermon> downloadedOnly = new ArrayList<>();
                if (sermons != null) {
                    for (Sermon s : sermons) {
                        if (s.isDownloaded() || "DOWNLOADED".equals(downloadStatusMap.get(s.getId()))) {
                            downloadedOnly.add(s);
                        }
                    }
                }
                renderSermonCards(downloadedOnly);
            }));
            return;
        }

        viewModel.loadSermons(sermons -> activity.runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            renderSermonCards(sermons);
        }));
    }

    private void updateChipStyles() {
        for (Button btn : chipButtons) {
            boolean isSelected = btn.getText().toString().equalsIgnoreCase(selectedChip);
            if (isSelected) {
                btn.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
                btn.setBackground(PerazimTheme.createActivePillGradient(dp(14)));
            } else {
                btn.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
                btn.setBackground(PerazimTheme.createMutedPillGradient(dp(14), PerazimTheme.COLOR_STROKE, dp(1)));
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
            YouTubeFeedRepository.fetchRecentVideos(videos -> {
                Log.d("SermonsUiBinder", "Prefetched " + (videos != null ? videos.size() : 0) + " YouTube broadcasts");
            });
        }));
    }

    private void renderSermonCards(List<Sermon> sermons) {
        currentSermons.clear();
        cardsContainer.removeAllViews();

        if (sermons == null || sermons.isEmpty()) {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        currentSermons.addAll(sermons);

        for (final Sermon sermon : sermons) {
            View card = createSermonCard(sermon);
            cardsContainer.addView(card);
        }
    }

    private View createSermonCard(final Sermon sermon) {
        final LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, 14, PerazimTheme.COLOR_STROKE, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        // Top Row: Title & Play Circle Button
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleCol.setLayoutParams(lpCol);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(sermon.getTitle() != null ? sermon.getTitle() : "Untitled Sermon");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        titleCol.addView(tvTitle);

        TextView tvPreacher = new TextView(activity);
        String passage = sermon.getPassage() != null && !sermon.getPassage().isEmpty()
                ? " • " + sermon.getPassage() : "";
        String preacher = sermon.getPreacher() != null ? sermon.getPreacher() : "Bishop Dr. David Mutweri";
        tvPreacher.setText(preacher + passage);
        tvPreacher.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPreacher.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvPreacher.setPadding(0, dp(2), 0, 0);
        titleCol.addView(tvPreacher);

        topRow.addView(titleCol);

        // Quiet Purple Play Circle (NOT massive orange button!)
        Button btnPlayCircle = new Button(activity);
        btnPlayCircle.setText("▶");
        btnPlayCircle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnPlayCircle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnPlayCircle.setBackground(createSolidPill(PerazimTheme.COLOR_PURPLE_BRAND, 20));
        LinearLayout.LayoutParams lpCircle = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpCircle.setMargins(dp(8), 0, 0, 0);
        btnPlayCircle.setLayoutParams(lpCircle);
        btnPlayCircle.setOnClickListener(v -> {
            if (sermon.hasVideo() && (sermon.getAudioUrl() == null || sermon.getAudioUrl().isEmpty())) {
                YouTubeFeedRepository.openVideo(activity, sermon.getVideoUrl());
            } else {
                startPlayback(sermon);
            }
        });
        topRow.addView(btnPlayCircle);

        card.addView(topRow);

        // Metadata Row: Duration chip, date chip, series chip
        LinearLayout metaRow = new LinearLayout(activity);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(0, dp(10), 0, dp(6));

        long durationSec = sermon.getDurationSeconds() > 0 ? sermon.getDurationSeconds() : 2700;
        String durationStr = (durationSec / 60) + " min";
        TextView tvDuration = new TextView(activity);
        tvDuration.setText(durationStr);
        tvDuration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvDuration.setTextColor(PerazimTheme.COLOR_PURPLE_MUTED);
        tvDuration.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 6, PerazimTheme.COLOR_STROKE, 1));
        tvDuration.setPadding(dp(8), dp(3), dp(8), dp(3));
        metaRow.addView(tvDuration);

        TextView tvDate = new TextView(activity);
        tvDate.setText("Recorded");
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvDate.setTextColor(PerazimTheme.COLOR_PURPLE_MUTED);
        tvDate.setPadding(dp(8), 0, 0, 0);
        metaRow.addView(tvDate);

        if (sermon.getSeries() != null && !sermon.getSeries().isEmpty()) {
            TextView tvSeries = new TextView(activity);
            tvSeries.setText("· " + sermon.getSeries());
            tvSeries.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvSeries.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            tvSeries.setPadding(dp(6), 0, 0, 0);
            metaRow.addView(tvSeries);
        }

        boolean isSample = (sermon.getLocalAudioPath() == null || sermon.getLocalAudioPath().isEmpty()) && (sermon.getAudioUrl() == null || sermon.getAudioUrl().isEmpty());
        if (isSample) {
            TextView tvSample = new TextView(activity);
            tvSample.setText("Sample Audio Preview");
            tvSample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvSample.setTypeface(Typeface.DEFAULT_BOLD);
            tvSample.setTextColor(PerazimTheme.COLOR_ORANGE_BRAND);
            tvSample.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 6, PerazimTheme.COLOR_STROKE, 1));
            tvSample.setPadding(dp(6), dp(2), dp(6), dp(2));
            LinearLayout.LayoutParams lpSample = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpSample.setMargins(dp(6), 0, 0, 0);
            tvSample.setLayoutParams(lpSample);
            metaRow.addView(tvSample);
        }

        card.addView(metaRow);

        // Actions Row: Download affordance, Notes, Share
        LinearLayout actionsRow = new LinearLayout(activity);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setPadding(0, dp(6), 0, 0);

        boolean isLocalAudio = sermon.getAudioUrl() != null && !sermon.getAudioUrl().isEmpty() && !sermon.getId().startsWith("yt_");
        if (isLocalAudio) {
            final Button btnDownload = new Button(activity);
            String initialStatus = downloadStatusMap.containsKey(sermon.getId())
                    ? downloadStatusMap.get(sermon.getId())
                    : (sermon.isDownloaded() ? "DOWNLOADED" : "STREAMING");
            updateDownloadActionButton(btnDownload, initialStatus);
            btnDownload.setOnClickListener(v -> handleDownloadAction(sermon, btnDownload));
            actionsRow.addView(btnDownload);
        }

        if (sermon.hasPdf()) {
            Button btnNotes = new Button(activity);
            btnNotes.setText("Study Notes (PDF)");
            btnNotes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btnNotes.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            btnNotes.setBackground(createCardBackground(Color.TRANSPARENT, 8, PerazimTheme.COLOR_STROKE, 1));
            btnNotes.setPadding(dp(10), dp(5), dp(10), dp(5));
            LinearLayout.LayoutParams lpNotes = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpNotes.setMargins(dp(6), 0, 0, 0);
            btnNotes.setLayoutParams(lpNotes);
            btnNotes.setOnClickListener(v -> openPdfNotes(activity, sermon.getPdfUrl()));
            actionsRow.addView(btnNotes);
        }

        Button btnShare = new Button(activity);
        btnShare.setText("Share");
        btnShare.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnShare.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnShare.setBackground(createCardBackground(Color.TRANSPARENT, 8, PerazimTheme.COLOR_STROKE, 1));
        btnShare.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams lpShare = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpShare.setMargins(dp(6), 0, 0, 0);
        btnShare.setLayoutParams(lpShare);
        btnShare.setOnClickListener(v -> YouTubeFeedRepository.shareSermonToWhatsApp(activity, sermon));
        actionsRow.addView(btnShare);

        card.addView(actionsRow);

        card.setOnClickListener(v -> {
            if (sermon.hasVideo() && !isLocalAudio) {
                YouTubeFeedRepository.openVideo(activity, sermon.getVideoUrl());
            } else {
                startPlayback(sermon);
            }
        });

        return card;
    }

    public static void openPdfNotes(@NonNull Context context, @Nullable String pdfUrl) {
        if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
            Toast.makeText(context, "No PDF study notes available for this sermon.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(pdfUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            } catch (Exception ex) {
                Toast.makeText(context, "Unable to open PDF notes: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateDownloadActionButton(Button btn, String status) {
        if ("DOWNLOADED".equals(status)) {
            btn.setText("✓ Downloaded");
            btn.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            btn.setBackground(createCardBackground(PerazimTheme.COLOR_PURPLE_GHOST, 8, PerazimTheme.COLOR_PURPLE_SOFT, 1));
        } else if ("DOWNLOADING".equals(status)) {
            btn.setText("Downloading…");
            btn.setTextColor(PerazimTheme.COLOR_ORANGE_SOFT);
            btn.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 8, PerazimTheme.COLOR_STROKE, 1));
        } else {
            btn.setText("Download");
            btn.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            btn.setBackground(createCardBackground(Color.TRANSPARENT, 8, PerazimTheme.COLOR_STROKE, 1));
        }
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setPadding(dp(10), dp(5), dp(10), dp(5));
    }

    private void handleDownloadAction(final Sermon sermon, final Button btn) {
        String currentStatus = downloadStatusMap.containsKey(sermon.getId())
                ? downloadStatusMap.get(sermon.getId())
                : (sermon.isDownloaded() ? "DOWNLOADED" : "STREAMING");

        if ("STREAMING".equals(currentStatus)) {
            downloadStatusMap.put(sermon.getId(), "DOWNLOADING");
            updateDownloadActionButton(btn, "DOWNLOADING");
            Toast.makeText(activity, "Download staged.", Toast.LENGTH_SHORT).show();

            viewModel.requestDownload(sermon.getId(), success -> activity.runOnUiThread(() -> {
                if (Boolean.TRUE.equals(success)) {
                    downloadStatusMap.put(sermon.getId(), "DOWNLOADED");
                    sermon.setDownloaded(true);
                    updateDownloadActionButton(btn, "DOWNLOADED");
                    Toast.makeText(activity, "Sermon downloaded for offline listening.", Toast.LENGTH_SHORT).show();
                } else {
                    downloadStatusMap.put(sermon.getId(), "STREAMING");
                    updateDownloadActionButton(btn, "STREAMING");
                    Toast.makeText(activity, "Download failed.", Toast.LENGTH_SHORT).show();
                }
            }));
        } else if ("DOWNLOADED".equals(currentStatus)) {
            new AlertDialog.Builder(activity)
                    .setTitle("Manage Download")
                    .setMessage(sermon.getTitle() + " is saved offline.")
                    .setPositiveButton("Play Offline", (dialog, which) -> {
                        startPlayback(sermon);
                    })
                    .setNeutralButton("Delete Download", (dialog, which) -> {
                        viewModel.deleteDownload(sermon.getId(), success -> activity.runOnUiThread(() -> {
                            downloadStatusMap.put(sermon.getId(), "STREAMING");
                            sermon.setDownloaded(false);
                            updateDownloadActionButton(btn, "STREAMING");
                            Toast.makeText(activity, "Download removed.", Toast.LENGTH_SHORT).show();
                        }));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

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
        boolean isSample = (sermon.getLocalAudioPath() == null || sermon.getLocalAudioPath().isEmpty()) && (sermon.getAudioUrl() == null || sermon.getAudioUrl().isEmpty());
        String title = sermon.getTitle() != null ? sermon.getTitle() : "Untitled Sermon";
        if (isSample) {
            title += " (Sample Audio Preview)";
        }
        tvPlayerTitle.setText(title);
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
        btnPlayPause.setText("Pause Sermon");

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
                        release();
                        btnPlayPause.setText("Play Sermon");
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
        btnPlayPause.setText("Play Sermon");
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
        btnSpeedToggle.setText(String.format(Locale.US, "%.2f×", playbackSpeed).replace(".00", ".0"));
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
            } catch (Exception ignored) {}
        }
        Toast.makeText(activity, "Playback speed: " + btnSpeedToggle.getText(), Toast.LENGTH_SHORT).show();
    }

    public void stopPlayback() {
        release();
        currentPositionSeconds = 0;
        if (audioSeekBar != null) {
            audioSeekBar.setProgress(0);
        }
        if (tvCurrentPos != null) {
            tvCurrentPos.setText("00:00");
        }
        if (btnPlayPause != null) {
            btnPlayPause.setText("Play Sermon");
        }
        Toast.makeText(activity, "Sermon audio stopped", Toast.LENGTH_SHORT).show();
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

    private LinearLayout createCard(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        LinearLayout card = new LinearLayout(activity);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(createCardBackground(bgColor, cornerRadiusDp, borderColor, borderWidthDp));
        return card;
    }

    private Drawable createCardBackground(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        if (bgColor == PerazimTheme.COLOR_SURFACE) {
            return PerazimTheme.createCardGradient(dp(cornerRadiusDp), dp(borderWidthDp));
        } else if (bgColor == PerazimTheme.COLOR_SURFACE_2) {
            return PerazimTheme.createSurface2Gradient(dp(cornerRadiusDp), dp(borderWidthDp));
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        return drawable;
    }

    private Drawable createSolidPill(int bgColor, int cornerRadiusDp) {
        if (bgColor == PerazimTheme.COLOR_PURPLE_BRAND) {
            return PerazimTheme.createActivePillGradient(dp(cornerRadiusDp));
        } else if (bgColor == PerazimTheme.COLOR_ORANGE_BRAND) {
            return PerazimTheme.createPrimaryOrangeGradient(dp(cornerRadiusDp));
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        return drawable;
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
}
