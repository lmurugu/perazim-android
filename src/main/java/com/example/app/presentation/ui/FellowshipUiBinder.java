package com.example.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.data.local.entity.JokeEntity;
import com.example.app.data.local.entity.RiddleEntity;
import com.example.app.domain.model.Prayer;
import com.example.app.presentation.state.UiState;
import com.example.app.presentation.viewmodel.FellowshipViewModel;
import com.example.app.ui.theme.PerazimTheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UI Binder responsible for orchestrating the Fellowship tab.
 * <p>
 * Binds directly to {@link FellowshipViewModel} and manages:
 * <ul>
 *     <li>Community Prayer Wall with reactive listing and offline persistence.</li>
 *     <li>Interactive "Amen" counters with immediate local feedback and database sync.</li>
 *     <li>Submit Prayer dialog with anonymous toggle, saving to Room and enqueuing to {@code SyncQueueManager}.</li>
 *     <li>Devotional riddles with reveal toggle and sequential progression.</li>
 *     <li>Christian humor viewer with punchline reveal and next-joke navigation.</li>
 * </ul>
 */
public class FellowshipUiBinder {

    private final Context context;
    private final Activity activity;
    private final FellowshipViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Prayer Wall UI state
    private final List<Prayer> displayedPrayers = new ArrayList<>();
    private final Set<String> amenedPrayerIds = new HashSet<>();
    private LinearLayout prayersContainer;
    private ProgressBar prayersLoadingSpinner;
    private TextView tvPrayersEmptyState;

    // Riddles UI state
    private final List<RiddleEntity> riddlesList = new ArrayList<>();
    private int currentRiddleIndex = 0;
    private TextView tvRiddleQuestion;
    private TextView tvRiddleScriptureRef;
    private TextView tvRiddleAnswer;
    private Button btnRiddleReveal;

    // Jokes UI state
    private final List<JokeEntity> jokesList = new ArrayList<>();
    private int currentJokeIndex = 0;
    private TextView tvJokeText;
    private TextView tvJokePunchline;
    private Button btnJokeReveal;

    public FellowshipUiBinder(@NonNull Activity activity, @NonNull FellowshipViewModel viewModel) {
        this.activity = activity;
        this.context = activity;
        this.viewModel = viewModel;
    }

    public FellowshipUiBinder(@NonNull Context context, @NonNull FellowshipViewModel viewModel) {
        this.context = context;
        this.activity = context instanceof Activity ? (Activity) context : null;
        this.viewModel = viewModel;
    }

    /**
     * Builds and returns the complete ScrollView representing the Fellowship Screen.
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
     * Builds and binds the complete Fellowship Tab view.
     */
    @NonNull
    public ScrollView bind() {
        return buildView();
    }

    /**
     * Binds the Fellowship UI components into the provided root container.
     */
    public void bind(@NonNull ViewGroup rootContainer) {
        // 1. Fellowship Photo Banner
        View banner = createBannerView();
        if (banner != null) {
            rootContainer.addView(banner);
        }

        // 2. Community Prayer Wall Section Header & Submit Action
        View prayerHeaderSection = createPrayerWallHeaderSection();
        rootContainer.addView(prayerHeaderSection);

        // 3. Prayer Wall List Container
        prayersContainer = new LinearLayout(context);
        prayersContainer.setOrientation(LinearLayout.VERTICAL);
        prayersContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        prayersLoadingSpinner = new ProgressBar(context);
        prayersLoadingSpinner.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpSpinner = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpSpinner.gravity = Gravity.CENTER_HORIZONTAL;
        lpSpinner.setMargins(0, dp(12), 0, dp(12));
        prayersLoadingSpinner.setLayoutParams(lpSpinner);
        prayersContainer.addView(prayersLoadingSpinner);

        tvPrayersEmptyState = new TextView(context);
        tvPrayersEmptyState.setText("No prayer petitions yet. Be the first to lift a petition on the Wall!");
        tvPrayersEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvPrayersEmptyState.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvPrayersEmptyState.setGravity(Gravity.CENTER);
        tvPrayersEmptyState.setPadding(dp(16), dp(20), dp(16), dp(20));
        tvPrayersEmptyState.setVisibility(View.GONE);
        prayersContainer.addView(tvPrayersEmptyState);

        rootContainer.addView(prayersContainer);

        // 4. Fellowship Devotional Riddles Card
        View riddlesCard = createRiddlesCard();
        rootContainer.addView(riddlesCard);

        // 5. Christian Joy & Humor Card
        View humorCard = createHumorCard();
        rootContainer.addView(humorCard);

        // 6. Foundation & Identity Card
        View identityCard = createIdentityCard();
        rootContainer.addView(identityCard);

        // Initial Data Load
        refreshPrayers();
        loadRiddles();
        loadJokes();
    }

    // =========================================================================
    // 1. HEADER & BANNER
    // =========================================================================

    @Nullable
    private View createBannerView() {
        Bitmap bitmap = loadAssetBitmap("fellowship.jpg", 600);
        if (bitmap != null) {
            ImageView iv = new ImageView(context);
            iv.setImageBitmap(getRoundedCornerBitmap(bitmap, dp(PerazimTheme.RADIUS_LG)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(140));
            lp.setMargins(0, 0, 0, dp(14));
            iv.setLayoutParams(lp);
            return iv;
        }

        // Fallback decorative gradient banner if asset not present
        LinearLayout banner = new LinearLayout(context);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setGravity(Gravity.CENTER);
        banner.setPadding(dp(16), dp(24), dp(16), dp(24));
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{PerazimTheme.COLOR_PRIMARY_PURPLE, PerazimTheme.COLOR_ACCENT_ORANGE}
        );
        g.setCornerRadius(dp(PerazimTheme.RADIUS_LG));
        banner.setBackground(g);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("PERAZIM FELLOWSHIP WALL");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_TITLE);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_WHITE);
        tvTitle.setGravity(Gravity.CENTER);
        banner.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("“Bear ye one another's burdens, and so fulfil the law of Christ.” — Galatians 6:2");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvSub.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvSub.setTextColor(PerazimTheme.COLOR_WARM_CREAM);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, dp(4), 0, 0);
        banner.addView(tvSub);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        banner.setLayoutParams(lp);
        return banner;
    }

    private View createPrayerWallHeaderSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvSection = new TextView(context);
        tvSection.setText("COMMUNITY PRAYER WALL");
        tvSection.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvSection.setTypeface(Typeface.DEFAULT_BOLD);
        tvSection.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvSection.setPadding(0, 0, 0, dp(6));
        section.addView(tvSection);

        Button btnSubmit = new Button(context);
        btnSubmit.setText("✍️ Submit Prayer Request to Wall (+15 XP)");
        btnSubmit.setTextColor(PerazimTheme.COLOR_WHITE);
        btnSubmit.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        btnSubmit.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmit.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_PRIMARY_PURPLE,
                PerazimTheme.COLOR_PURPLE_SHADOW,
                PerazimTheme.RADIUS_MD,
                3
        ));
        btnSubmit.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtn.setMargins(0, 0, 0, dp(12));
        btnSubmit.setLayoutParams(lpBtn);
        btnSubmit.setOnClickListener(v -> showSubmitPrayerDialog());
        section.addView(btnSubmit);

        return section;
    }

    // =========================================================================
    // 2. COMMUNITY PRAYER WALL & AMEN INTERACTION
    // =========================================================================

    /**
     * Loads public prayers from {@link FellowshipViewModel}.
     */
    public void refreshPrayers() {
        if (prayersLoadingSpinner != null) {
            prayersLoadingSpinner.setVisibility(View.VISIBLE);
        }
        viewModel.loadPublicPrayers(prayers -> {
            postToMain(() -> {
                if (prayersLoadingSpinner != null) {
                    prayersLoadingSpinner.setVisibility(View.GONE);
                }
                updatePrayerWall(prayers);
            });
        });
    }

    private void updatePrayerWall(@Nullable List<Prayer> prayers) {
        if (prayersContainer == null) return;

        // Remove previous cards except spinner and empty state view
        for (int i = prayersContainer.getChildCount() - 1; i >= 0; i--) {
            View child = prayersContainer.getChildAt(i);
            if (child != prayersLoadingSpinner && child != tvPrayersEmptyState) {
                prayersContainer.removeViewAt(i);
            }
        }

        displayedPrayers.clear();

        if (prayers == null || prayers.isEmpty()) {
            // Populate fallback prayers if empty so wall feels active
            List<Prayer> fallbacks = getFallbackPrayers();
            displayedPrayers.addAll(fallbacks);
        } else {
            displayedPrayers.addAll(prayers);
        }

        if (displayedPrayers.isEmpty()) {
            if (tvPrayersEmptyState != null) tvPrayersEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        if (tvPrayersEmptyState != null) tvPrayersEmptyState.setVisibility(View.GONE);

        for (Prayer prayer : displayedPrayers) {
            View card = createPrayerCard(prayer);
            prayersContainer.addView(card);
        }
    }

    @NonNull
    private View createPrayerCard(@NonNull Prayer prayer) {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lpCard);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("🙏 " + (prayer.getTitle() != null ? prayer.getTitle() : "Prayer Petition"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        card.addView(tvTitle);

        TextView tvAuthor = new TextView(context);
        String author = prayer.getAuthorName() != null ? prayer.getAuthorName() : "Fellow Saint";
        tvAuthor.setText("Submitted by: " + author + " · Embu Community");
        tvAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvAuthor.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvAuthor.setPadding(0, dp(2), 0, dp(6));
        card.addView(tvAuthor);

        TextView tvBody = new TextView(context);
        tvBody.setText(prayer.getBody() != null ? prayer.getBody() : "");
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvBody.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        tvBody.setLineSpacing(dp(2), 1.15f);
        tvBody.setPadding(0, 0, 0, dp(10));
        card.addView(tvBody);

        // Amen Interactive Button
        final int[] amenCount = {prayer.getAmenCount()};
        final boolean[] hasAmened = {amenedPrayerIds.contains(prayer.getId()) || prayer.isUserHasAmened()};

        Button btnAmen = new Button(context);
        updateAmenButton(btnAmen, amenCount[0], hasAmened[0]);

        btnAmen.setOnClickListener(v -> {
            if (!hasAmened[0]) {
                hasAmened[0] = true;
                amenCount[0]++;
                amenedPrayerIds.add(prayer.getId());
                prayer.setAmenCount(amenCount[0]);
                prayer.setUserHasAmened(true);

                updateAmenButton(btnAmen, amenCount[0], true);

                // Call ViewModel to persist Amen to Room and Sync queue
                viewModel.amenPrayer(prayer.getId(), "active_user", null);

                Toast.makeText(
                        context,
                        "🙏 Amen! You joined in prayer for " + (prayer.getTitle() != null ? prayer.getTitle() : "this petition") + "!",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(context, "You have already joined in prayer for this petition! 🙏", Toast.LENGTH_SHORT).show();
            }
        });

        card.addView(btnAmen);
        return card;
    }

    private void updateAmenButton(Button btn, int count, boolean hasAmened) {
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(PerazimTheme.COLOR_WHITE);
        btn.setPadding(dp(12), dp(8), dp(12), dp(8));

        if (hasAmened) {
            btn.setText("✓ Amen (" + count + ")");
            btn.setBackground(create3dButtonDrawable(
                    PerazimTheme.COLOR_PRIMARY_PURPLE,
                    PerazimTheme.COLOR_PURPLE_SHADOW,
                    PerazimTheme.RADIUS_MD,
                    3
            ));
        } else {
            btn.setText("🙏 Amen (" + count + ")");
            btn.setBackground(create3dButtonDrawable(
                    PerazimTheme.COLOR_ACCENT_ORANGE,
                    PerazimTheme.COLOR_ORANGE_SHADOW,
                    PerazimTheme.RADIUS_MD,
                    3
            ));
        }
    }

    // =========================================================================
    // 3. SUBMIT PRAYER DIALOG
    // =========================================================================

    /**
     * Displays a dialog allowing the member to submit a prayer petition to Room & SyncQueue.
     */
    public void showSubmitPrayerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🙏 Submit Prayer Petition");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(14), dp(20), dp(10));

        TextView tvPrompt = new TextView(context);
        tvPrompt.setText("Your petition will be submitted to the Perazim Prayer Wall and our Intercessory Team.");
        tvPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvPrompt.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvPrompt.setPadding(0, 0, 0, dp(12));
        layout.addView(tvPrompt);

        // Title Input
        final EditText etTitle = new EditText(context);
        etTitle.setHint("Prayer Title (e.g., Healing, Family, Exams)");
        etTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        etTitle.setSingleLine(true);
        etTitle.setBackground(createPillDrawable(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.COLOR_BORDER_GREY));
        etTitle.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTitle.setMargins(0, 0, 0, dp(10));
        etTitle.setLayoutParams(lpTitle);
        layout.addView(etTitle);

        // Content Input
        final EditText etContent = new EditText(context);
        etContent.setHint("Describe your petition or praise report in detail...");
        etContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        etContent.setMinLines(4);
        etContent.setGravity(Gravity.TOP | Gravity.START);
        etContent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etContent.setBackground(createCardBackground(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.RADIUS_MD, PerazimTheme.COLOR_BORDER_GREY, 1));
        etContent.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpContent = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpContent.setMargins(0, 0, 0, dp(10));
        etContent.setLayoutParams(lpContent);
        layout.addView(etContent);

        // Anonymous Toggle
        final CheckBox cbAnonymous = new CheckBox(context);
        cbAnonymous.setText("Post Anonymously (Hide my name on the Wall)");
        cbAnonymous.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        cbAnonymous.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        layout.addView(cbAnonymous);

        builder.setView(layout);

        builder.setPositiveButton("Submit (+15 XP)", (dialog, which) -> {
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

            // Submit prayer via ViewModel (Room + SyncQueueManager PENDING)
            viewModel.submitPrayer(
                    title,
                    content,
                    isAnonymous,
                    "user_active",
                    isAnonymous ? "Anonymous Saint" : "Perazim Saint",
                    () -> postToMain(() -> {
                        Toast.makeText(context, "🙏 Prayer petition submitted to the Wall! (+15 XP)", Toast.LENGTH_LONG).show();
                        refreshPrayers();
                    })
            );
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // =========================================================================
    // 4. DEVOTIONAL RIDDLES CARD
    // =========================================================================

    private View createRiddlesCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, dp(12), 0, 0);
        card.setLayoutParams(lpCard);

        TextView tvTag = new TextView(context);
        tvTag.setText("🧩 DEVOTIONAL RIDDLES FOR FELLOWSHIP");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        card.addView(tvTag);

        tvRiddleScriptureRef = new TextView(context);
        tvRiddleScriptureRef.setText("Category: Prophets");
        tvRiddleScriptureRef.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvRiddleScriptureRef.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvRiddleScriptureRef.setPadding(0, dp(2), 0, dp(4));
        card.addView(tvRiddleScriptureRef);

        tvRiddleQuestion = new TextView(context);
        tvRiddleQuestion.setText("Loading riddle...");
        tvRiddleQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        tvRiddleQuestion.setTypeface(Typeface.DEFAULT_BOLD);
        tvRiddleQuestion.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        tvRiddleQuestion.setPadding(0, dp(4), 0, dp(8));
        card.addView(tvRiddleQuestion);

        tvRiddleAnswer = new TextView(context);
        tvRiddleAnswer.setText("");
        tvRiddleAnswer.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvRiddleAnswer.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvRiddleAnswer.setTypeface(Typeface.DEFAULT_BOLD);
        tvRiddleAnswer.setVisibility(View.GONE);
        tvRiddleAnswer.setPadding(0, dp(4), 0, dp(8));
        card.addView(tvRiddleAnswer);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        btnRiddleReveal = new Button(context);
        btnRiddleReveal.setText("👆 Tap to Reveal Answer");
        btnRiddleReveal.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btnRiddleReveal.setTypeface(Typeface.DEFAULT_BOLD);
        btnRiddleReveal.setTextColor(PerazimTheme.COLOR_WHITE);
        btnRiddleReveal.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_PRIMARY_PURPLE,
                PerazimTheme.COLOR_PURPLE_SHADOW,
                PerazimTheme.RADIUS_MD,
                2
        ));
        LinearLayout.LayoutParams lpReveal = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpReveal.setMargins(0, 0, dp(6), 0);
        btnRiddleReveal.setLayoutParams(lpReveal);
        btnRiddleReveal.setOnClickListener(v -> toggleRiddleAnswer());
        btnRow.addView(btnRiddleReveal);

        Button btnNextRiddle = new Button(context);
        btnNextRiddle.setText("Next Riddle ➔");
        btnNextRiddle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btnNextRiddle.setTypeface(Typeface.DEFAULT_BOLD);
        btnNextRiddle.setTextColor(PerazimTheme.COLOR_WHITE);
        btnNextRiddle.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_ACCENT_ORANGE,
                PerazimTheme.COLOR_ORANGE_SHADOW,
                PerazimTheme.RADIUS_MD,
                2
        ));
        LinearLayout.LayoutParams lpNext = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnNextRiddle.setLayoutParams(lpNext);
        btnNextRiddle.setOnClickListener(v -> nextRiddle());
        btnRow.addView(btnNextRiddle);

        card.addView(btnRow);
        return card;
    }

    private void loadRiddles() {
        viewModel.loadRiddles(riddles -> {
            postToMain(() -> {
                riddlesList.clear();
                if (riddles != null && !riddles.isEmpty()) {
                    riddlesList.addAll(riddles);
                } else {
                    riddlesList.addAll(getFallbackRiddles());
                }
                currentRiddleIndex = 0;
                displayCurrentRiddle();
            });
        });
    }

    private void displayCurrentRiddle() {
        if (riddlesList.isEmpty()) return;
        RiddleEntity riddle = riddlesList.get(currentRiddleIndex % riddlesList.size());
        if (tvRiddleQuestion != null) {
            tvRiddleQuestion.setText(riddle.getQuestion());
        }
        if (tvRiddleScriptureRef != null) {
            String ref = riddle.getScriptureRef() != null ? riddle.getScriptureRef() : "";
            String diff = riddle.getDifficulty() != null ? " • " + riddle.getDifficulty() : "";
            tvRiddleScriptureRef.setText(ref + diff);
        }
        if (tvRiddleAnswer != null) {
            String explanation = riddle.getExplanation() != null ? " — " + riddle.getExplanation() : "";
            tvRiddleAnswer.setText("Answer: " + riddle.getAnswer() + explanation);
            tvRiddleAnswer.setVisibility(View.GONE);
        }
        if (btnRiddleReveal != null) {
            btnRiddleReveal.setText("👆 Tap to Reveal Answer");
        }
    }

    private void toggleRiddleAnswer() {
        if (tvRiddleAnswer == null) return;
        if (tvRiddleAnswer.getVisibility() == View.GONE) {
            tvRiddleAnswer.setVisibility(View.VISIBLE);
            if (btnRiddleReveal != null) btnRiddleReveal.setText("🙈 Hide Answer");
        } else {
            tvRiddleAnswer.setVisibility(View.GONE);
            if (btnRiddleReveal != null) btnRiddleReveal.setText("👆 Tap to Reveal Answer");
        }
    }

    public void nextRiddle() {
        if (riddlesList.isEmpty()) return;
        currentRiddleIndex = (currentRiddleIndex + 1) % riddlesList.size();
        displayCurrentRiddle();
    }

    // =========================================================================
    // 5. CHRISTIAN JOY & HUMOR CARD
    // =========================================================================

    private View createHumorCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, dp(12), 0, 0);
        card.setLayoutParams(lpCard);

        TextView tvTag = new TextView(context);
        tvTag.setText("😄 CHRISTIAN JOY & BIBLICAL HUMOR");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        card.addView(tvTag);

        tvJokeText = new TextView(context);
        tvJokeText.setText("Loading humor...");
        tvJokeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        tvJokeText.setTypeface(Typeface.DEFAULT_BOLD);
        tvJokeText.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        tvJokeText.setPadding(0, dp(6), 0, dp(4));
        card.addView(tvJokeText);

        tvJokePunchline = new TextView(context);
        tvJokePunchline.setText("");
        tvJokePunchline.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvJokePunchline.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvJokePunchline.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvJokePunchline.setVisibility(View.GONE);
        tvJokePunchline.setPadding(0, dp(4), 0, dp(8));
        card.addView(tvJokePunchline);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        btnJokeReveal = new Button(context);
        btnJokeReveal.setText("👆 Tap to Reveal Punchline 😄");
        btnJokeReveal.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btnJokeReveal.setTypeface(Typeface.DEFAULT_BOLD);
        btnJokeReveal.setTextColor(PerazimTheme.COLOR_WHITE);
        btnJokeReveal.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_PRIMARY_PURPLE,
                PerazimTheme.COLOR_PURPLE_SHADOW,
                PerazimTheme.RADIUS_MD,
                2
        ));
        LinearLayout.LayoutParams lpReveal = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpReveal.setMargins(0, 0, dp(6), 0);
        btnJokeReveal.setLayoutParams(lpReveal);
        btnJokeReveal.setOnClickListener(v -> toggleJokePunchline());
        btnRow.addView(btnJokeReveal);

        Button btnNextJoke = new Button(context);
        btnNextJoke.setText("Next Joke ➔");
        btnNextJoke.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btnNextJoke.setTypeface(Typeface.DEFAULT_BOLD);
        btnNextJoke.setTextColor(PerazimTheme.COLOR_WHITE);
        btnNextJoke.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_ACCENT_ORANGE,
                PerazimTheme.COLOR_ORANGE_SHADOW,
                PerazimTheme.RADIUS_MD,
                2
        ));
        LinearLayout.LayoutParams lpNext = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnNextJoke.setLayoutParams(lpNext);
        btnNextJoke.setOnClickListener(v -> nextJoke());
        btnRow.addView(btnNextJoke);

        card.addView(btnRow);
        return card;
    }

    private void loadJokes() {
        viewModel.loadJokes(jokes -> {
            postToMain(() -> {
                jokesList.clear();
                if (jokes != null && !jokes.isEmpty()) {
                    jokesList.addAll(jokes);
                } else {
                    jokesList.addAll(getFallbackJokes());
                }
                currentJokeIndex = 0;
                displayCurrentJoke();
            });
        });
    }

    private void displayCurrentJoke() {
        if (jokesList.isEmpty()) return;
        JokeEntity joke = jokesList.get(currentJokeIndex % jokesList.size());
        String fullText = joke.getText() != null ? joke.getText() : "";

        // Parse Question and Answer if formatted with Q: and A:
        if (fullText.contains("A:") || fullText.contains("Answer:")) {
            int splitIdx = fullText.contains("A:") ? fullText.indexOf("A:") : fullText.indexOf("Answer:");
            String q = fullText.substring(0, splitIdx).trim();
            String a = fullText.substring(splitIdx).trim();
            if (tvJokeText != null) tvJokeText.setText(q);
            if (tvJokePunchline != null) {
                tvJokePunchline.setText(a);
                tvJokePunchline.setVisibility(View.GONE);
            }
        } else {
            if (tvJokeText != null) tvJokeText.setText(fullText);
            if (tvJokePunchline != null) {
                tvJokePunchline.setText("“A merry heart doeth good like a medicine!” (Proverbs 17:22) 😄");
                tvJokePunchline.setVisibility(View.GONE);
            }
        }

        if (btnJokeReveal != null) {
            btnJokeReveal.setText("👆 Tap to Reveal Punchline 😄");
        }
    }

    private void toggleJokePunchline() {
        if (tvJokePunchline == null) return;
        if (tvJokePunchline.getVisibility() == View.GONE) {
            tvJokePunchline.setVisibility(View.VISIBLE);
            if (btnJokeReveal != null) btnJokeReveal.setText("😄 Hide Punchline");
        } else {
            tvJokePunchline.setVisibility(View.GONE);
            if (btnJokeReveal != null) btnJokeReveal.setText("👆 Tap to Reveal Punchline 😄");
        }
    }

    public void nextJoke() {
        if (jokesList.isEmpty()) return;
        currentJokeIndex = (currentJokeIndex + 1) % jokesList.size();
        displayCurrentJoke();
    }

    // =========================================================================
    // 6. FOUNDATION & IDENTITY CARD
    // =========================================================================

    private View createIdentityCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_PRIMARY_PURPLE, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(14), 0, dp(6));
        card.setLayoutParams(lp);

        TextView tvTag = new TextView(context);
        tvTag.setText("🏛️ CHURCH FOUNDATION & IDENTITY");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        card.addView(tvTag);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("PERAZIM MISSION CHURCH");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_TITLE);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        tvTitle.setPadding(0, dp(4), 0, dp(2));
        card.addView(tvTitle);

        TextView tvMotto = new TextView(context);
        tvMotto.setText("“The Place of Great Breakthrough” (2 Samuel 5:20)");
        tvMotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvMotto.setTypeface(Typeface.DEFAULT_BOLD);
        tvMotto.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvMotto.setPadding(0, 0, 0, dp(4));
        card.addView(tvMotto);

        TextView tvSlogan = new TextView(context);
        tvSlogan.setText("“A place where everybody is somebody, and nobody is a nobody”");
        tvSlogan.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvSlogan.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        card.addView(tvSlogan);

        return card;
    }

    // =========================================================================
    // FALLBACK DATA GENERATORS
    // =========================================================================

    private List<Prayer> getFallbackPrayers() {
        List<Prayer> list = new ArrayList<>();
        list.add(new Prayer(
                "prayer_1",
                "Mama Sarah (Embu General Hospital)",
                "Praying for full recovery, divine strength, and quick healing following hip replacement surgery.",
                "user_elder",
                "Elder Gitonga",
                Prayer.Visibility.PUBLIC,
                24,
                System.currentTimeMillis(),
                false,
                false
        ));
        list.add(new Prayer(
                "prayer_2",
                "KCSE Candidates at Kangaru School",
                "Interceding for focus, clarity of mind, and divine wisdom for all 120 Form 4 exam candidates.",
                "user_youth",
                "Youth Ministry",
                Prayer.Visibility.PUBLIC,
                38,
                System.currentTimeMillis(),
                false,
                false
        ));
        list.add(new Prayer(
                "prayer_3",
                "Central Sanctuary Roof Completion",
                "Thanksgiving for milestones achieved and prayer for safety of engineering team completing the roof.",
                "user_deacon",
                "Deacon Board",
                Prayer.Visibility.PUBLIC,
                52,
                System.currentTimeMillis(),
                false,
                false
        ));
        list.add(new Prayer(
                "prayer_4",
                "Family Agribusiness in Mwea",
                "Trusting God for supernatural breakthrough, market favor, and open doors for church rice farmers.",
                "user_mercy",
                "Sister Mercy",
                Prayer.Visibility.PUBLIC,
                19,
                System.currentTimeMillis(),
                false,
                false
        ));
        return list;
    }

    private List<RiddleEntity> getFallbackRiddles() {
        List<RiddleEntity> list = new ArrayList<>();
        list.add(new RiddleEntity("r1", "Who was swallowed by a great fish when running from God's assignment?", "Jonah", "Swallowed for 3 days and 3 nights", "Jonah 1:17", "Easy"));
        list.add(new RiddleEntity("r2", "What food fell from heaven daily to feed Israel in the wilderness?", "Manna", "Flakes like coriander seed that tasted like wafers with honey", "Exodus 16:31", "Easy"));
        list.add(new RiddleEntity("r3", "Who was the youngest king in Judah, beginning his reign at age seven?", "Joash", "Protected by priest Jehoiada in the temple", "2 Kings 11:21", "Medium"));
        list.add(new RiddleEntity("r4", "Where did Elijah contest against the 450 prophets of Baal?", "Mount Carmel", "God answered Elijah with fire consuming the altar", "1 Kings 18:19", "Medium"));
        return list;
    }

    private List<JokeEntity> getFallbackJokes() {
        List<JokeEntity> list = new ArrayList<>();
        list.add(new JokeEntity("j1", "Q: Who was the greatest financier in the Bible?\nA: Noah! He was floating his stock while everyone else was in liquidation! 🌊🚢", "G", "Finance"));
        list.add(new JokeEntity("j2", "Q: Who was the greatest babysitter in Scripture?\nA: David, because he rocked Goliath to sleep! 😴", "G", "Characters"));
        list.add(new JokeEntity("j3", "Q: At what time of day was Adam created?\nA: A little before Eve! ⏰", "G", "Creation"));
        list.add(new JokeEntity("j4", "Q: Which animal on the ark had the least faith?\nA: The cheetah! 🐆", "G", "Animals"));
        return list;
    }

    // =========================================================================
    // GRAPHICS & VIEW BUILDERS
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

    private void postToMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    public List<Prayer> getDisplayedPrayers() {
        return displayedPrayers;
    }
}
