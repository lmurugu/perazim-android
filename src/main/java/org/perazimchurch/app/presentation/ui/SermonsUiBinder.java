package org.perazimchurch.app.presentation.ui;

import org.perazimchurch.app.ui.theme.AppDialogs;

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
import org.perazimchurch.app.ui.theme.GlassKit;
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
    private FrameLayout btnPlayPause;
    private ImageView playPauseIcon;
    private SeekBar audioSeekBar;
    private TextView tvCurrentPos;
    private TextView tvTotalDuration;
    private TextView btnSpeedToggle;

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
        contentLayout.setPadding(dp(16), dp(8), dp(16), dp(16));

        // 1. Title row (mockup: "Sermons", 24sp bold), with the phone's Bible audio as a glass pill
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(4), 0, 0, dp(14));
        TextView tvTitle = GlassKit.text(activity, "Sermons", 22, true, GlassKit.text());
        headerRow.addView(tvTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView localBibleAudio = GlassKit.glassButton(activity, "Bible audio", 12);
        localBibleAudio.setContentDescription("Bible audio on this phone");
        localBibleAudio.setPadding(dp(16), 0, dp(16), 0);
        localBibleAudio.setOnClickListener(v -> activity.startActivity(new Intent(activity, LocalBibleAudioActivity.class)));
        headerRow.addView(localBibleAudio, new LinearLayout.LayoutParams(-2, dp(36)));
        GlassKit.keep(headerRow);

        contentLayout.addView(headerRow);

        // 2. Livestream Card (Quiet)
        View liveBanner = buildLiveStreamBanner();
        liveBanner.setVisibility(View.GONE); // shown only once a live stream is configured
        contentLayout.addView(liveBanner);

        // 3. Audio Player Controller
        playerCard = buildAudioPlayerCard();
        contentLayout.addView(playerCard);

        // 4. Filter chips (All | Series | Downloaded | Video), then search
        View chipsRow = buildSeriesFilterChips();
        contentLayout.addView(chipsRow);
        View searchBar = buildSearchBar();
        contentLayout.addView(searchBar);

        // 6. Section Header
        TextView tvHeader = GlassKit.label(activity, "MESSAGES");
        tvHeader.setPadding(dp(4), dp(4), 0, dp(10));
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
            // Rounded tile so the illustration's dark square sits softly on the glow.
            android.graphics.drawable.GradientDrawable emptyShape = new android.graphics.drawable.GradientDrawable();
            emptyShape.setCornerRadius(dp(24));
            emptyShape.setColor(android.graphics.Color.TRANSPARENT);
            ivEmpty.setBackground(emptyShape);
            ivEmpty.setClipToOutline(true);
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

        // Bible audio on this phone, as a full-width photo card so the page is not left half empty
        Bitmap bibleAudioPhoto = loadAssetBitmap("backgrounds/bg_sermons_lamp.png", 720);
        LinearLayout.LayoutParams bibleAudioLp = new LinearLayout.LayoutParams(-1, dp(112));
        bibleAudioLp.topMargin = dp(14);
        contentLayout.addView(GlassKit.photoCard(activity, bibleAudioPhoto, "Bible audio",
                "Listen offline to the Bible on this phone", 112, 22,
                v -> activity.startActivity(new Intent(activity, LocalBibleAudioActivity.class))), bibleAudioLp);

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
        // Hidden until a live stream URL is configured; avoids showing a broadcast that is not running.
        liveCard.setVisibility(View.GONE);
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
        // Mockup board "Sermons player": one glass card with artwork, title, scrubber and round controls.
        LinearLayout card = GlassKit.card(activity, 28, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 0);
        card.setLayoutParams(lp);

        // Artwork, 170dp, 22dp corners, with speed and stop as small frosted pills
        FrameLayout art = new FrameLayout(activity);
        GradientDrawable artShape = new GradientDrawable();
        artShape.setCornerRadius(dp(22));
        artShape.setColor(PerazimTheme.COLOR_SURFACE_SOLID);
        art.setBackground(artShape);
        art.setClipToOutline(true);
        GlassKit.keep(art);
        Bitmap artBmp = loadAssetBitmap("backgrounds/church/sermons_pastor.jpg", 900);
        if (artBmp != null) {
            ImageView iv = new ImageView(activity);
            iv.setImageBitmap(artBmp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        }
        View artRim = new View(activity);
        GradientDrawable artRimBg = new GradientDrawable();
        artRimBg.setCornerRadius(dp(22));
        artRimBg.setStroke(Math.max(1, dp(1)), Color.parseColor("#40FFFFFF"));
        artRim.setBackground(artRimBg);
        art.addView(GlassKit.keep(artRim), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout artPills = new LinearLayout(activity);
        artPills.setOrientation(LinearLayout.HORIZONTAL);
        btnSpeedToggle = artPill("1.0×");
        btnSpeedToggle.setOnClickListener(v -> cycleSpeed());
        artPills.addView(btnSpeedToggle, new LinearLayout.LayoutParams(-2, dp(30)));
        TextView stop = artPill("Stop");
        stop.setOnClickListener(v -> stopPlayback());
        LinearLayout.LayoutParams lpStopPill = new LinearLayout.LayoutParams(-2, dp(30));
        lpStopPill.leftMargin = dp(6);
        artPills.addView(stop, lpStopPill);
        FrameLayout.LayoutParams lpPills = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        lpPills.setMargins(0, dp(10), dp(10), 0);
        art.addView(GlassKit.keep(artPills), lpPills);
        art.setElevation(dp(8));
        if (Build.VERSION.SDK_INT >= 28) {
            art.setOutlineSpotShadowColor(Color.parseColor("#8B32AA"));
            art.setOutlineAmbientShadowColor(Color.parseColor("#8B32AA"));
        }
        card.addView(art, new LinearLayout.LayoutParams(-1, dp(170)));

        tvPlayerTitle = GlassKit.text(activity, "Supernatural Breakthrough", 18, true, GlassKit.text());
        card.addView(tvPlayerTitle, GlassKit.matchWrap(activity, 14));
        tvPlayerSpeaker = GlassKit.text(activity, "Bishop Dr. David Mutweri · 2 Samuel 5:17-21", 13, false, GlassKit.muted());
        card.addView(tvPlayerSpeaker, GlassKit.matchWrap(activity, 3));

        // Scrubber: 5dp track at 0.18 white, glowing orange progress, white 14dp knob
        audioSeekBar = new SeekBar(activity);
        audioSeekBar.setMax(totalDurationSeconds);
        audioSeekBar.setProgress(currentPositionSeconds);
        audioSeekBar.setPadding(dp(7), 0, dp(7), 0);
        GradientDrawable track = new GradientDrawable();
        track.setCornerRadius(dp(3));
        track.setColor(GlassKit.light() ? Color.parseColor("#263D1749") : Color.parseColor("#2EFFFFFF"));
        GradientDrawable fill = new GradientDrawable();
        fill.setCornerRadius(dp(3));
        fill.setColor(GlassKit.ORANGE);
        android.graphics.drawable.LayerDrawable progress = new android.graphics.drawable.LayerDrawable(new Drawable[]{
                track, new android.graphics.drawable.ClipDrawable(fill, Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL)});
        progress.setId(0, android.R.id.background);
        progress.setId(1, android.R.id.progress);
        audioSeekBar.setProgressDrawable(progress);
        GradientDrawable knob = new GradientDrawable();
        knob.setShape(GradientDrawable.OVAL);
        knob.setColor(Color.WHITE);
        knob.setSize(dp(14), dp(14));
        audioSeekBar.setThumb(knob);
        audioSeekBar.setThumbOffset(dp(7));
        audioSeekBar.setMinimumHeight(dp(5));
        if (Build.VERSION.SDK_INT >= 29) {
            audioSeekBar.setMinHeight(dp(5));
            audioSeekBar.setMaxHeight(dp(5));
        }
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (fromUser) {
                    currentPositionSeconds = progressValue;
                    tvCurrentPos.setText(formatDuration(currentPositionSeconds));
                    if (mediaPlayer != null) {
                        try {
                            mediaPlayer.seekTo(progressValue * 1000);
                        } catch (Exception ignored) {}
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        GlassKit.keep(audioSeekBar);
        LinearLayout.LayoutParams lpSeek = new LinearLayout.LayoutParams(-1, dp(18));
        lpSeek.topMargin = dp(10);
        lpSeek.leftMargin = -dp(7);
        lpSeek.rightMargin = -dp(7);
        card.addView(audioSeekBar, lpSeek);

        LinearLayout timeRow = new LinearLayout(activity);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        tvCurrentPos = GlassKit.text(activity, formatDuration(currentPositionSeconds), 11, false, GlassKit.muted());
        timeRow.addView(tvCurrentPos, new LinearLayout.LayoutParams(0, -2, 1f));
        tvTotalDuration = GlassKit.text(activity, formatDuration(totalDurationSeconds), 11, false, GlassKit.muted());
        timeRow.addView(tvTotalDuration);
        card.addView(GlassKit.keep(timeRow), GlassKit.matchWrap(activity, 4));

        // Controls: −15s glass pill, 64dp orange play orb, +30s glass pill
        LinearLayout ctrlRow = new LinearLayout(activity);
        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
        ctrlRow.setGravity(Gravity.CENTER);
        ctrlRow.setClipChildren(false);
        ctrlRow.setClipToPadding(false);
        ctrlRow.setPadding(0, dp(4), 0, dp(6));

        TextView btnRewind = GlassKit.glassButton(activity, "−15s", 12);
        btnRewind.setContentDescription("Back 15 seconds");
        btnRewind.setPadding(dp(12), 0, dp(12), 0);
        btnRewind.setMinWidth(dp(48));
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
        ctrlRow.addView(btnRewind, new LinearLayout.LayoutParams(-2, dp(48)));

        btnPlayPause = new FrameLayout(activity);
        GradientDrawable orb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{GlassKit.ORANGE_TOP, GlassKit.ORANGE_BOTTOM});
        orb.setShape(GradientDrawable.OVAL);
        orb.setStroke(Math.max(1, dp(1)), Color.parseColor("#66FFFFFF"));
        btnPlayPause.setBackground(orb);
        playPauseIcon = new ImageView(activity);
        btnPlayPause.addView(playPauseIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        btnPlayPause.setClickable(true);
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        GlassKit.glow(btnPlayPause, GlassKit.ORANGE, 12);
        GlassKit.keep(btnPlayPause);
        setPlayButton(false);
        LinearLayout.LayoutParams lpPlay = new LinearLayout.LayoutParams(dp(64), dp(64));
        lpPlay.setMargins(dp(22), 0, dp(22), 0);
        ctrlRow.addView(btnPlayPause, lpPlay);

        TextView btnForward = GlassKit.glassButton(activity, "+30s", 12);
        btnForward.setContentDescription("Forward 30 seconds");
        btnForward.setPadding(dp(12), 0, dp(12), 0);
        btnForward.setMinWidth(dp(48));
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
        ctrlRow.addView(btnForward, new LinearLayout.LayoutParams(-2, dp(48)));

        card.addView(GlassKit.keep(ctrlRow), GlassKit.matchWrap(activity, 8));
        return card;
    }

    private TextView artPill(String text) {
        TextView pill = GlassKit.text(activity, text, 11, true, Color.WHITE);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(15));
        bg.setColor(Color.parseColor("#732A0F33"));
        bg.setStroke(Math.max(1, dp(1)), Color.parseColor("#40FFFFFF"));
        pill.setBackground(bg);
        pill.setClickable(true);
        return pill;
    }

    private void setPlayButton(boolean playing) {
        if (playPauseIcon == null) return;
        playPauseIcon.setImageResource(playing ? org.perazimchurch.app.R.drawable.gl_pause : org.perazimchurch.app.R.drawable.gl_play);
        playPauseIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        btnPlayPause.setContentDescription(playing ? "Pause" : "Play");
    }

    // =========================================================================
    // 3. SEARCH BAR (QUIET SURFACE #4F1E5C)
    // =========================================================================

    private View buildSearchBar() {
        LinearLayout searchLayout = new LinearLayout(activity);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchLayout.setGravity(Gravity.CENTER_VERTICAL);
        searchLayout.setBackground(GlassKit.glassControl(activity, 999));
        searchLayout.setPadding(dp(16), 0, dp(8), 0);
        GlassKit.keep(searchLayout);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        lp.setMargins(0, 0, 0, dp(14));
        searchLayout.setLayoutParams(lp);

        ImageView ivSearch = new ImageView(activity);
        ivSearch.setImageResource(org.perazimchurch.app.R.drawable.gl_search);
        ivSearch.setImageTintList(android.content.res.ColorStateList.valueOf(GlassKit.muted()));
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(18), dp(18));
        lpIcon.setMargins(0, 0, dp(10), 0);
        ivSearch.setLayoutParams(lpIcon);
        searchLayout.addView(ivSearch);

        etSearch = new EditText(activity);
        etSearch.setHint("Search title, preacher, passage…");
        etSearch.setHintTextColor(GlassKit.muted());
        etSearch.setTextColor(GlassKit.text());
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
        btnClear.setTextColor(GlassKit.muted());
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
        lpHsv.setMargins(0, dp(16), 0, dp(12));
        hsv.setLayoutParams(lpHsv);

        chipsContainer = new LinearLayout(activity);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipsContainer.setPadding(0, dp(2), 0, dp(4));

        chipButtons.clear();
        for (String chip : SERIES_CHIPS) {
            Button btn = new Button(activity);
            btn.setText(chip);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setAllCaps(false);
            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setMinHeight(0);
            btn.setMinimumHeight(0);
            btn.setStateListAnimator(null);
            btn.setPadding(dp(16), 0, dp(16), 0);
            GlassKit.keep(btn);

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

        loadAllSermons(sermons -> activity.runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            renderSermonCards(sermons);
        }));
    }

    private void updateChipStyles() {
        for (Button btn : chipButtons) {
            boolean isSelected = btn.getText().toString().equalsIgnoreCase(selectedChip);
            btn.setTextColor(isSelected ? Color.WHITE : GlassKit.text());
            btn.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            btn.setBackground(isSelected ? GlassKit.purple(activity, 999) : GlassKit.glass(activity, 999));
        }
    }

    // =========================================================================
    // 5. DATA LOADING & SERMON CARDS RENDERING
    // =========================================================================

    public void loadInitialData() {
        loadingBar.setVisibility(View.VISIBLE);
        loadAllSermons(sermons -> activity.runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            renderSermonCards(sermons);
            if (sermons != null && !sermons.isEmpty() && activeSermon == null && sermons.get(0).getAudioUrl() != null && !sermons.get(0).getAudioUrl().isEmpty()) {
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
        // Mockup row: glass, 22dp corners, title and one muted meta line, purple play circle on the right.
        final LinearLayout card = GlassKit.card(activity, 22, 0);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView tvTitle = GlassKit.text(activity,
                sermon.getTitle() != null ? sermon.getTitle() : "Untitled Sermon", 14, true, GlassKit.text());
        titleCol.addView(tvTitle);

        long durationSec = sermon.getDurationSeconds() > 0 ? sermon.getDurationSeconds() : 2700;
        boolean isSample = (sermon.getLocalAudioPath() == null || sermon.getLocalAudioPath().isEmpty())
                && (sermon.getAudioUrl() == null || sermon.getAudioUrl().isEmpty());
        boolean watchOnly = sermon.getVideoUrl() != null && !sermon.getVideoUrl().isEmpty();
        StringBuilder meta = new StringBuilder(sermon.getPreacher() != null ? sermon.getPreacher() : "Perazim Mission Church");
        if (sermon.getPassage() != null && !sermon.getPassage().isEmpty()) {
            meta.append(" · ").append(sermon.getPassage());
        }
        meta.append(" · ").append(durationSec / 60).append(" min");
        if (sermon.getSeries() != null && !sermon.getSeries().isEmpty()) {
            meta.append(" · ").append(sermon.getSeries());
        }
        if (isSample) {
            meta.append(watchOnly ? " · Watch on YouTube" : " · Audio coming soon");
        }
        TextView tvMeta = GlassKit.text(activity, meta.toString(), 12, false, GlassKit.muted());
        tvMeta.setLineSpacing(0f, 1.2f);
        titleCol.addView(tvMeta, GlassKit.matchWrap(activity, 3));
        topRow.addView(GlassKit.keep(titleCol), new LinearLayout.LayoutParams(0, -2, 1f));

        FrameLayout share = GlassKit.circleIcon(activity, org.perazimchurch.app.R.drawable.gl_share, 36, 16, "Share");
        share.setOnClickListener(v -> YouTubeFeedRepository.shareSermonToWhatsApp(activity, sermon));
        ((LinearLayout.LayoutParams) share.getLayoutParams()).leftMargin = dp(10);
        topRow.addView(share);

        FrameLayout play = GlassKit.iconBadge(activity, org.perazimchurch.app.R.drawable.gl_play,
                GlassKit.purple(activity, 20), 40, 18);
        play.setContentDescription("Play " + (sermon.getTitle() != null ? sermon.getTitle() : "sermon"));
        play.setClickable(true);
        play.setOnClickListener(v -> {
            if (sermon.hasVideo() && (sermon.getAudioUrl() == null || sermon.getAudioUrl().isEmpty())) {
                showInAppVideo(sermon.getVideoUrl(), sermon.getTitle());
            } else {
                startPlayback(sermon);
            }
        });
        LinearLayout.LayoutParams lpPlay = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpPlay.leftMargin = dp(8);
        topRow.addView(play, lpPlay);
        card.addView(GlassKit.keep(topRow));

        // Download and study notes, only when the message has them
        boolean isLocalAudio = sermon.getAudioUrl() != null && !sermon.getAudioUrl().isEmpty() && !sermon.getId().startsWith("yt_");
        if (isLocalAudio || sermon.hasPdf()) {
            LinearLayout actionsRow = new LinearLayout(activity);
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);
            actionsRow.setGravity(Gravity.CENTER_VERTICAL);
            if (isLocalAudio) {
                final Button btnDownload = new Button(activity);
                String initialStatus = downloadStatusMap.containsKey(sermon.getId())
                        ? downloadStatusMap.get(sermon.getId())
                        : (sermon.isDownloaded() ? "DOWNLOADED" : "STREAMING");
                updateDownloadActionButton(btnDownload, initialStatus);
                btnDownload.setOnClickListener(v -> handleDownloadAction(sermon, btnDownload));
                actionsRow.addView(btnDownload, new LinearLayout.LayoutParams(-2, dp(32)));
            }
            if (sermon.hasPdf()) {
                TextView btnNotes = GlassKit.glassButton(activity, "Study notes", 11);
                btnNotes.setPadding(dp(14), 0, dp(14), 0);
                btnNotes.setOnClickListener(v -> openPdfNotes(activity, sermon.getPdfUrl()));
                LinearLayout.LayoutParams lpNotes = new LinearLayout.LayoutParams(-2, dp(32));
                lpNotes.leftMargin = isLocalAudio ? dp(8) : 0;
                actionsRow.addView(btnNotes, lpNotes);
            }
            card.addView(GlassKit.keep(actionsRow), GlassKit.matchWrap(activity, 10));
        }

        card.setClickable(true);
        card.setOnClickListener(v -> {
            if (sermon.hasVideo() && !isLocalAudio) {
                showInAppVideo(sermon.getVideoUrl(), sermon.getTitle());
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
        GlassKit.keep(btn);
        btn.setAllCaps(false);
        btn.setStateListAnimator(null);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        if ("DOWNLOADED".equals(status)) {
            btn.setText("Downloaded");
            btn.setTextColor(Color.WHITE);
            btn.setBackground(GlassKit.purple(activity, 999));
        } else if ("DOWNLOADING".equals(status)) {
            btn.setText("Downloading…");
            btn.setTextColor(GlassKit.accent());
            btn.setBackground(GlassKit.glassControl(activity, 999));
        } else {
            btn.setText("Download");
            btn.setTextColor(GlassKit.text());
            btn.setBackground(GlassKit.glassControl(activity, 999));
        }
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(14), 0, dp(14), 0);
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
            AppDialogs.builder(activity)
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
        boolean hasAudio = sermon.getAudioUrl() != null && !sermon.getAudioUrl().isEmpty();
        String watch = sermon.getVideoUrl();
        if (!hasAudio && watch != null && watch.startsWith("https://")) {
            // Church YouTube uploads have no separate audio file: play the video inside the app.
            showInAppVideo(watch, sermon.getTitle());
            return;
        }
        loadSermonIntoPlayer(sermon, true);
    }

    /** Plays a YouTube video in an in-app player so the member stays in the app. */
    private void showInAppVideo(String watchUrl, String title) {
        String videoId = android.net.Uri.parse(watchUrl).getQueryParameter("v");
        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{6,20}")) {
            Toast.makeText(activity, "This video cannot be played in the app", Toast.LENGTH_SHORT).show();
            return;
        }
        final String embedPrefix = "https://www.youtube-nocookie.com/embed/";

        android.webkit.WebView player = new android.webkit.WebView(activity);
        android.webkit.WebSettings settings = player.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        player.setWebChromeClient(new android.webkit.WebChromeClient());
        player.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(android.webkit.WebView view, android.webkit.WebResourceRequest request) {
                // Only the embedded player may load; anything else stays out of the app.
                return !request.getUrl().toString().startsWith(embedPrefix);
            }
        });

        // Full-screen dark "Now Playing" screen (no stock dialog frame or footer).
        android.widget.LinearLayout root = new android.widget.LinearLayout(activity);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackground(PerazimTheme.createBackgroundGradient());

        android.widget.LinearLayout topBar = new android.widget.LinearLayout(activity);
        topBar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8), dp(12), dp(8), dp(8));

        android.widget.Button closeBtn = new android.widget.Button(activity);
        closeBtn.setText("\u2304");
        closeBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22);
        closeBtn.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        closeBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        closeBtn.setContentDescription("Close player");
        topBar.addView(closeBtn, new android.widget.LinearLayout.LayoutParams(dp(56), dp(48)));

        android.widget.TextView topTitle = new android.widget.TextView(activity);
        topTitle.setText("Now Playing");
        topTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        topTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        topTitle.setTypeface(Typeface.DEFAULT_BOLD);
        topTitle.setGravity(android.view.Gravity.CENTER);
        topBar.addView(topTitle, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        View topSpacer = new View(activity);
        topBar.addView(topSpacer, new android.widget.LinearLayout.LayoutParams(dp(56), dp(48)));
        root.addView(topBar);

        root.addView(player, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        android.app.Dialog dialog = new android.app.Dialog(activity, org.perazimchurch.app.R.style.PerazimPlayerDialog);
        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            // Full-screen player: hide the status bar so no activity background shows above the controls.
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setStatusBarColor(PerazimTheme.COLOR_BG_DEEP);
            dialog.getWindow().setNavigationBarColor(PerazimTheme.COLOR_BG_DEEP);
            dialog.getWindow().setBackgroundDrawable(PerazimTheme.createBackgroundGradient());
        }
        final int previousStatusColor = activity.getWindow().getStatusBarColor();
        activity.getWindow().setStatusBarColor(PerazimTheme.COLOR_BG_DEEP);
        dialog.setOnDismissListener(d -> {
            activity.getWindow().setStatusBarColor(previousStatusColor);
            player.stopLoading();
            player.loadUrl("about:blank");
            player.destroy();
        });
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        // Church player page (own controls around YouTube's embed). The page origin is the
        // church domain because YouTube refuses embeds with no referring site (error 153).
        String html;
        try (java.io.InputStream in = activity.getAssets().open("player/church_video_player.html")) {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
            html = buf.toString("UTF-8")
                    .replace("__VIDEO_ID__", videoId)
                    .replace("__TITLE__", android.text.TextUtils.htmlEncode(title == null ? "Church video" : title));
        } catch (java.io.IOException e) {
            Toast.makeText(activity, "Player could not be loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        player.loadDataWithBaseURL("https://perazim.org/", html, "text/html", "UTF-8", null);
    }

    /**
     * Loads the church's YouTube uploads from the pilot server. Calls back with null when the
     * feed is unavailable, so callers can fall back to the bundled list.
     */
    private void loadChurchFeed(java.util.function.Consumer<List<Sermon>> callback) {
        org.perazimchurch.app.data.remote.ApiClient.get(activity, "/api/media/church-feed", null,
                new org.perazimchurch.app.data.remote.ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(int statusCode, org.json.JSONObject json) {
                        List<Sermon> out = new ArrayList<>();
                        org.json.JSONArray items = json.optJSONArray("items");
                        if (items != null) {
                            for (int i = 0; i < items.length(); i++) {
                                org.json.JSONObject it = items.optJSONObject(i);
                                if (it == null) continue;
                                Sermon s = new Sermon();
                                s.setId("yt-" + it.optString("videoId"));
                                s.setTitle(it.optString("title"));
                                s.setPreacher("Perazim Mission Church");
                                s.setVideoUrl(it.optString("watchUrl"));
                                s.setThumbnailUrl(it.optString("thumbnailUrl"));
                                s.setDatePreached(it.optString("publishedAt"));
                                out.add(s);
                            }
                        }
                        callback.accept(out);
                    }

                    @Override
                    public void onError(int statusCode, String errorMessage) {
                        callback.accept(null);
                    }
                });
    }

    /**
     * Church YouTube uploads from the pilot server first. If the server feed is not configured
     * or unreachable, the public channel feed is used. The bundled Room list is the last resort.
     */
    private void loadAllSermons(java.util.function.Consumer<List<Sermon>> callback) {
        loadChurchFeed(feed -> {
            if (feed != null && !feed.isEmpty()) {
                callback.accept(feed);
                return;
            }
            YouTubeFeedRepository.fetchRecentVideos(videos -> {
                if (videos != null && !videos.isEmpty()) {
                    callback.accept(videos);
                } else {
                    viewModel.loadSermons(callback::accept);
                }
            });
        });
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
        setPlayButton(true);

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

                String remoteUrl = activeSermon != null ? activeSermon.getAudioUrl() : null;
                if (!sourceSet && remoteUrl != null && remoteUrl.startsWith("https://")) {
                    try {
                        mediaPlayer.setDataSource(remoteUrl);
                        sourceSet = true;
                    } catch (Exception e) {
                        Log.w("SermonsUiBinder", "Failed to set remote audio source", e);
                    }
                }

                boolean isSampleSermon = activeSermon == null || "sample-sermon-1".equals(activeSermon.getId());
                if (!sourceSet && isSampleSermon) {
                    try {
                        AssetFileDescriptor afd = activity.getAssets().openFd("audio/sample_sermon.mp3");
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        sourceSet = true;
                    } catch (Exception e) {
                        Log.w("SermonsUiBinder", "Failed to load audio/sample_sermon.mp3 asset", e);
                    }
                }

                if (!sourceSet) {
                    // No audio file for this message yet: do not play unrelated sample audio.
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isPlaying = false;
                    setPlayButton(false);
                    Toast.makeText(activity, "Audio for this message is not available yet", Toast.LENGTH_LONG).show();
                    return;
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
                        setPlayButton(false);
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
        setPlayButton(false);
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
            setPlayButton(false);
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
            return GlassKit.purple(activity, cornerRadiusDp);
        } else if (bgColor == PerazimTheme.COLOR_ORANGE_BRAND) {
            return GlassKit.orange(activity, cornerRadiusDp);
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
