package com.example.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.app.domain.model.Hymn;
import com.example.app.presentation.viewmodel.WorshipViewModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * WorshipUiBinder binds the Worship Tab UI to {@link WorshipViewModel}.
 * Features:
 * - Category filter chips: "All", "Worship", "Faith", "Grace", "Praise", "Favorites".
 * - Search bar to search hymns by number or lyric keywords.
 * - Hymn Card List: Hymn number badge, title, category, favorite heart icon toggle calling worshipViewModel.toggleFavorite.
 * - Hymn Detail Sheet / Dialog:
 *   - Header: Number, title, key signature, time signature.
 *   - Chords & Lyrics Viewer: Displays full lyrics with monospace guitar/piano chords.
 *   - Favorite toggle button.
 */
public class WorshipUiBinder {

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

    public static final int COLOR_HEART_RED      = Color.parseColor("#DC2626");
    public static final int COLOR_HEART_TINT     = Color.parseColor("#FEE2E2");

    public static final String[] CATEGORY_CHIPS = {
            "All", "Worship", "Faith", "Grace", "Praise", "Favorites"
    };

    private final Activity activity;
    private final WorshipViewModel viewModel;

    // UI Root & Containers
    private ScrollView rootScrollView;
    private LinearLayout contentLayout;
    private LinearLayout chipsContainer;
    private LinearLayout cardsContainer;
    private ProgressBar loadingBar;
    private TextView tvEmptyState;
    private EditText etSearch;

    // Data State
    private String selectedCategory = "All";
    private final List<Hymn> currentHymns = new ArrayList<>();
    private final List<Button> chipButtons = new ArrayList<>();

    public WorshipUiBinder(@NonNull Activity activity, @NonNull WorshipViewModel viewModel) {
        this.activity = activity;
        this.viewModel = viewModel;
    }

    /**
     * Builds and binds the complete Worship Tab view.
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

    private void initViews() {
        rootScrollView = new ScrollView(activity);
        rootScrollView.setFillViewport(true);
        rootScrollView.setBackgroundColor(COLOR_BG_NEUTRAL);

        contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(16), dp(16), dp(36));

        // 1. Classical Art Banner Row (hymnals.jpg and stage.jpg)
        View artBanner = buildArtBanner();
        contentLayout.addView(artBanner);

        // 2. Search Bar
        View searchBar = buildSearchBar();
        contentLayout.addView(searchBar);

        // 3. Category Filter Chips
        View chipsRow = buildCategoryChips();
        contentLayout.addView(chipsRow);

        // 4. Section Header
        TextView tvHeader = new TextView(activity);
        tvHeader.setText("EXPANDED HYMNAL & CHORD BROWSER");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvHeader.setPadding(0, dp(14), 0, dp(10));
        contentLayout.addView(tvHeader);

        // 5. Loading Indicator
        loadingBar = new ProgressBar(activity);
        loadingBar.setIndeterminate(true);
        loadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpLoad = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLoad.gravity = Gravity.CENTER_HORIZONTAL;
        lpLoad.setMargins(0, dp(16), 0, dp(16));
        loadingBar.setLayoutParams(lpLoad);
        contentLayout.addView(loadingBar);

        // 6. Empty State View
        tvEmptyState = new TextView(activity);
        tvEmptyState.setText("No hymns found matching your query.\nTry selecting another category or clearing your search.");
        tvEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvEmptyState.setTextColor(COLOR_TEXT_MUTED);
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setPadding(dp(16), dp(24), dp(16), dp(24));
        tvEmptyState.setVisibility(View.GONE);
        contentLayout.addView(tvEmptyState);

        // 7. Hymn Cards Container
        cardsContainer = new LinearLayout(activity);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        contentLayout.addView(cardsContainer);

        rootScrollView.addView(contentLayout);
    }

    // =========================================================================
    // 1. CLASSICAL ART BANNER ROW
    // =========================================================================

    private View buildArtBanner() {
        LinearLayout artRow = new LinearLayout(activity);
        artRow.setOrientation(LinearLayout.HORIZONTAL);
        artRow.setPadding(0, 0, 0, dp(14));

        Bitmap hymnalsBmp = loadAssetBitmap("hymnals.jpg", 400);
        if (hymnalsBmp != null) {
            ImageView img1 = new ImageView(activity);
            img1.setImageBitmap(getRoundedCornerBitmap(hymnalsBmp, dp(12)));
            img1.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(110), 1.0f);
            lp.setMargins(0, 0, dp(6), 0);
            img1.setLayoutParams(lp);
            artRow.addView(img1);
        }

        Bitmap stageBmp = loadAssetBitmap("stage.jpg", 400);
        if (stageBmp != null) {
            ImageView img2 = new ImageView(activity);
            img2.setImageBitmap(getRoundedCornerBitmap(stageBmp, dp(12)));
            img2.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(110), 1.0f);
            lp2.setMargins(dp(6), 0, 0, 0);
            img2.setLayoutParams(lp2);
            artRow.addView(img2);
        }

        return artRow;
    }

    // =========================================================================
    // 2. SEARCH BAR
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
        etSearch.setHint("Search hymns by number or lyric keywords...");
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
                    onCategorySelected(selectedCategory);
                } else {
                    viewModel.searchHymns(q, hymns -> activity.runOnUiThread(() -> renderHymnCards(hymns)));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchLayout.addView(etSearch);

        Button btnClear = new Button(activity);
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
    // 3. CATEGORY FILTER CHIPS
    // =========================================================================

    private View buildCategoryChips() {
        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams lpHsv = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHsv.setMargins(0, 0, 0, dp(8));
        hsv.setLayoutParams(lpHsv);

        chipsContainer = new LinearLayout(activity);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipsContainer.setPadding(0, dp(2), 0, dp(4));

        chipButtons.clear();
        for (String cat : CATEGORY_CHIPS) {
            Button btn = new Button(activity);
            btn.setText(cat);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setPadding(dp(14), dp(6), dp(14), dp(6));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
            lp.setMargins(0, 0, dp(8), 0);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> onCategorySelected(cat));
            chipButtons.add(btn);
            chipsContainer.addView(btn);
        }

        updateChipStyles();
        hsv.addView(chipsContainer);
        return hsv;
    }

    private void onCategorySelected(String category) {
        selectedCategory = category;
        updateChipStyles();
        loadingBar.setVisibility(View.VISIBLE);
        viewModel.filterByCategory(category, hymns -> activity.runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            renderHymnCards(hymns);
        }));
    }

    private void updateChipStyles() {
        for (Button btn : chipButtons) {
            boolean isSelected = btn.getText().toString().equalsIgnoreCase(selectedCategory);
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
    // 4. DATA LOADING & HYMN CARD LIST
    // =========================================================================

    public void loadInitialData() {
        loadingBar.setVisibility(View.VISIBLE);
        viewModel.loadHymns(hymns -> activity.runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            renderHymnCards(hymns);
        }));
    }

    private void renderHymnCards(List<Hymn> hymns) {
        currentHymns.clear();
        cardsContainer.removeAllViews();

        if (hymns == null || hymns.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        currentHymns.addAll(hymns);

        for (final Hymn hymn : hymns) {
            View card = createHymnCard(hymn);
            cardsContainer.addView(card);
        }
    }

    private View createHymnCard(final Hymn hymn) {
        final LinearLayout card = createCard(COLOR_WHITE, dp(14), COLOR_BORDER_GREY, dp(1));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        // 1. Hymn Number Badge (#1, #2...)
        TextView tvNumberBadge = new TextView(activity);
        tvNumberBadge.setText("#" + hymn.getNumber());
        tvNumberBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvNumberBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvNumberBadge.setTextColor(COLOR_WHITE);
        tvNumberBadge.setBackground(createPillBg(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW));
        tvNumberBadge.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpNum = new LinearLayout.LayoutParams(dp(44), dp(32));
        lpNum.setMargins(0, 0, dp(12), 0);
        tvNumberBadge.setLayoutParams(lpNum);
        card.addView(tvNumberBadge);

        // 2. Middle Column: Title & Category & Key
        LinearLayout midCol = new LinearLayout(activity);
        midCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpMid = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        midCol.setLayoutParams(lpMid);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(hymn.getTitle());
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_DARK);
        midCol.addView(tvTitle);

        LinearLayout subRow = new LinearLayout(activity);
        subRow.setOrientation(LinearLayout.HORIZONTAL);
        subRow.setGravity(Gravity.CENTER_VERTICAL);
        subRow.setPadding(0, dp(3), 0, 0);

        // Category pill
        String cat = hymn.getCategory() != null && !hymn.getCategory().isEmpty() ? hymn.getCategory() : "Worship";
        TextView tvCat = new TextView(activity);
        tvCat.setText(cat);
        tvCat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvCat.setTypeface(Typeface.DEFAULT_BOLD);
        tvCat.setTextColor(COLOR_ACCENT_ORANGE);
        tvCat.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
        tvCat.setPadding(dp(6), dp(2), dp(6), dp(2));
        subRow.addView(tvCat);

        // Key & Time signature info
        String key = hymn.getKeySignature() != null ? hymn.getKeySignature() : "D Major";
        String time = hymn.getTimeSignature() != null ? hymn.getTimeSignature() : "4/4";
        TextView tvKeyInfo = new TextView(activity);
        tvKeyInfo.setText("  •  " + key + " (" + time + ")");
        tvKeyInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvKeyInfo.setTextColor(COLOR_TEXT_MUTED);
        subRow.addView(tvKeyInfo);

        midCol.addView(subRow);
        card.addView(midCol);

        // 3. Favorite Heart Icon Toggle
        final Button btnHeart = new Button(activity);
        updateHeartButtonState(btnHeart, hymn.isFavorite());
        LinearLayout.LayoutParams lpHeart = new LinearLayout.LayoutParams(dp(42), dp(42));
        lpHeart.setMargins(dp(6), 0, 0, 0);
        btnHeart.setLayoutParams(lpHeart);

        btnHeart.setOnClickListener(v -> {
            boolean nextFav = !hymn.isFavorite();
            hymn.setFavorite(nextFav);
            updateHeartButtonState(btnHeart, nextFav);

            viewModel.toggleFavorite(hymn.getId(), nextFav, () -> activity.runOnUiThread(() -> {
                String msg = nextFav ? "Added to Favorites: " + hymn.getTitle() : "Removed from Favorites";
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                if ("Favorites".equalsIgnoreCase(selectedCategory) && !nextFav) {
                    onCategorySelected("Favorites");
                }
            }));
        });
        card.addView(btnHeart);

        // Tap card to open Hymn Detail Sheet / Dialog
        card.setOnClickListener(v -> showHymnDetailSheet(hymn));

        return card;
    }

    private void updateHeartButtonState(Button btnHeart, boolean isFav) {
        if (isFav) {
            btnHeart.setText("❤️");
            btnHeart.setBackground(createPillBg(COLOR_HEART_TINT, COLOR_HEART_RED));
        } else {
            btnHeart.setText("♡");
            btnHeart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            btnHeart.setTextColor(COLOR_TEXT_MUTED);
            btnHeart.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        }
        btnHeart.setPadding(0, 0, 0, 0);
    }

    // =========================================================================
    // 5. HYMN DETAIL SHEET / DIALOG
    // =========================================================================

    public void showHymnDetailSheet(final Hymn hymn) {
        if (hymn == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ScrollView dialogScroll = new ScrollView(activity);
        dialogScroll.setFillViewport(true);

        LinearLayout detailBox = new LinearLayout(activity);
        detailBox.setOrientation(LinearLayout.VERTICAL);
        detailBox.setPadding(dp(20), dp(20), dp(20), dp(24));
        detailBox.setBackgroundColor(COLOR_WHITE);

        // Header: Number badge + Favorite status button
        LinearLayout topHeaderRow = new LinearLayout(activity);
        topHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        topHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvNumber = new TextView(activity);
        tvNumber.setText("HYMN #" + hymn.getNumber());
        tvNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvNumber.setTypeface(Typeface.DEFAULT_BOLD);
        tvNumber.setTextColor(COLOR_WHITE);
        tvNumber.setBackground(createPillBg(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW));
        tvNumber.setPadding(dp(10), dp(4), dp(10), dp(4));
        topHeaderRow.addView(tvNumber);

        View spacer = new View(activity);
        LinearLayout.LayoutParams lpSpacer = new LinearLayout.LayoutParams(0, 1, 1.0f);
        spacer.setLayoutParams(lpSpacer);
        topHeaderRow.addView(spacer);

        final Button btnFavToggle = new Button(activity);
        updateDialogFavButton(btnFavToggle, hymn.isFavorite());
        btnFavToggle.setOnClickListener(v -> {
            boolean nextFav = !hymn.isFavorite();
            hymn.setFavorite(nextFav);
            updateDialogFavButton(btnFavToggle, nextFav);

            viewModel.toggleFavorite(hymn.getId(), nextFav, () -> activity.runOnUiThread(() -> {
                renderHymnCards(currentHymns);
                String msg = nextFav ? "Marked as Favorite" : "Removed from Favorites";
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
            }));
        });
        topHeaderRow.addView(btnFavToggle);
        detailBox.addView(topHeaderRow);

        // Title
        TextView tvDetailTitle = new TextView(activity);
        tvDetailTitle.setText(hymn.getTitle());
        tvDetailTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvDetailTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvDetailTitle.setTextColor(COLOR_TEXT_DARK);
        tvDetailTitle.setPadding(0, dp(12), 0, dp(6));
        detailBox.addView(tvDetailTitle);

        // Metadata Pills Row (Key signature, Time signature, Category)
        LinearLayout metaPillsRow = new LinearLayout(activity);
        metaPillsRow.setOrientation(LinearLayout.HORIZONTAL);
        metaPillsRow.setGravity(Gravity.CENTER_VERTICAL);
        metaPillsRow.setPadding(0, 0, 0, dp(14));

        String keySig = hymn.getKeySignature() != null && !hymn.getKeySignature().isEmpty()
                ? hymn.getKeySignature() : (hymn.getKey() != null ? hymn.getKey() : "D Major");
        TextView tvKey = new TextView(activity);
        tvKey.setText("🎼 Key: " + keySig);
        tvKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvKey.setTypeface(Typeface.DEFAULT_BOLD);
        tvKey.setTextColor(COLOR_PRIMARY_PURPLE);
        tvKey.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        tvKey.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpK = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpK.setMargins(0, 0, dp(6), 0);
        tvKey.setLayoutParams(lpK);
        metaPillsRow.addView(tvKey);

        String timeSig = hymn.getTimeSignature() != null && !hymn.getTimeSignature().isEmpty()
                ? hymn.getTimeSignature() : "4/4";
        TextView tvTime = new TextView(activity);
        tvTime.setText("⏱ Time: " + timeSig);
        tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTime.setTypeface(Typeface.DEFAULT_BOLD);
        tvTime.setTextColor(COLOR_TEXT_MUTED);
        tvTime.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        tvTime.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpT.setMargins(0, 0, dp(6), 0);
        tvTime.setLayoutParams(lpT);
        metaPillsRow.addView(tvTime);

        String category = hymn.getCategory() != null ? hymn.getCategory() : "Worship";
        TextView tvCatPill = new TextView(activity);
        tvCatPill.setText("🏷 " + category);
        tvCatPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvCatPill.setTypeface(Typeface.DEFAULT_BOLD);
        tvCatPill.setTextColor(COLOR_ACCENT_ORANGE);
        tvCatPill.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
        tvCatPill.setPadding(dp(8), dp(4), dp(8), dp(4));
        metaPillsRow.addView(tvCatPill);

        detailBox.addView(metaPillsRow);

        // Chords Viewer Box
        String chords = hymn.getChords() != null && !hymn.getChords().isEmpty()
                ? hymn.getChords() : "D - G - A - D";
        LinearLayout chordsCard = createCard(COLOR_BG_NEUTRAL, dp(8), COLOR_BORDER_GREY, dp(1));
        chordsCard.setOrientation(LinearLayout.VERTICAL);
        chordsCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpChords = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpChords.setMargins(0, 0, 0, dp(14));
        chordsCard.setLayoutParams(lpChords);

        TextView tvChordsHeader = new TextView(activity);
        tvChordsHeader.setText("GUITAR & PIANO CHORD PROGRESSION");
        tvChordsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvChordsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvChordsHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        chordsCard.addView(tvChordsHeader);

        TextView tvChords = new TextView(activity);
        tvChords.setText(chords);
        tvChords.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvChords.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvChords.setTextColor(COLOR_ACCENT_ORANGE);
        tvChords.setPadding(0, dp(4), 0, 0);
        chordsCard.addView(tvChords);

        detailBox.addView(chordsCard);

        // Lyrics Header & Full Lyrics Viewer
        TextView tvLyricsHeader = new TextView(activity);
        tvLyricsHeader.setText("FULL HYMN LYRICS");
        tvLyricsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvLyricsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvLyricsHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        tvLyricsHeader.setPadding(0, 0, 0, dp(6));
        detailBox.addView(tvLyricsHeader);

        String lyrics = hymn.getLyrics() != null ? hymn.getLyrics() : "Lyrics not available.";
        TextView tvLyrics = new TextView(activity);
        tvLyrics.setText(lyrics);
        tvLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvLyrics.setTextColor(COLOR_TEXT_DARK);
        tvLyrics.setTypeface(Typeface.SERIF);
        tvLyrics.setLineSpacing(dp(4), 1.25f);
        tvLyrics.setPadding(dp(14), dp(12), dp(14), dp(16));

        GradientDrawable lyrBg = new GradientDrawable();
        lyrBg.setColor(COLOR_BG_NEUTRAL);
        lyrBg.setCornerRadius(dp(8));
        lyrBg.setStroke(dp(1), COLOR_BORDER_GREY);
        tvLyrics.setBackground(lyrBg);

        detailBox.addView(tvLyrics);

        dialogScroll.addView(detailBox);
        builder.setView(dialogScroll);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void updateDialogFavButton(Button btn, boolean isFav) {
        if (isFav) {
            btn.setText("❤️ Favorited");
            btn.setTextColor(COLOR_HEART_RED);
            btn.setBackground(createPillBg(COLOR_HEART_TINT, COLOR_HEART_RED));
        } else {
            btn.setText("♡ Add to Favorites");
            btn.setTextColor(COLOR_PRIMARY_PURPLE);
            btn.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        }
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(10), dp(4), dp(10), dp(4));
    }

    // =========================================================================
    // 6. GRAPHICS HELPERS
    // =========================================================================

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
