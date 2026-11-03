package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import androidx.annotation.Nullable;

import org.perazimchurch.app.domain.model.Hymn;
import org.perazimchurch.app.presentation.viewmodel.WorshipViewModel;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WorshipUiBinder binds the Worship Tab UI to {@link WorshipViewModel}.
 * Adheres strictly to the quiet dark contemplative theme:
 * - Canvas: #2A0F33 (COLOR_BG_DEEP)
 * - Search bar on #4F1E5C
 * - Filter chips: All | Favorites | Worship | Praise. Outline pill; active is solid #681A7D
 * - Hymn rows on surface #3D1749:
 *   - Number in #A67BB3, 12sp bold
 *   - Title in #F5F0F7, 15sp
 *   - Category / key in #B9A8C0, 12sp
 *   - Quiet outline heart for favorite toggle
 *   - Clean chevron right vector drawable (no emojis)
 * - Hymn detail view on #2A0F33:
 *   - Full lyrics in #F5F0F7
 *   - Chords in soft orange #E8A86A
 *   - Quiet controls: Back, Favorite, Font size (A- / A+), Transpose (- / +)
 */
public class WorshipUiBinder {

    public static final String[] CATEGORY_CHIPS = {
            "All", "Favorites", "Worship", "Faith", "Grace", "Praise"
    };

    private static final String[] SCALE = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] SCALE_FLATS = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};

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

    private void initViews() {
        rootScrollView = new ScrollView(activity);
        rootScrollView.setFillViewport(true);
        rootScrollView.setBackgroundColor(PerazimTheme.COLOR_BG_DEEP);

        contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(16), dp(16), dp(36));

        // 1. Title Header
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.VERTICAL);
        headerRow.setPadding(0, dp(4), 0, dp(12));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Worship");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        headerRow.addView(tvTitle);

        TextView tvSubtitle = new TextView(activity);
        tvSubtitle.setText("Expanded Hymnal & Chord Browser");
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSubtitle.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvSubtitle.setPadding(0, dp(2), 0, 0);
        headerRow.addView(tvSubtitle);

        contentLayout.addView(headerRow);

        // 2. Classical Art Banner Row (Quiet)
        View artBanner = buildArtBanner();
        if (artBanner != null) {
            contentLayout.addView(artBanner);
        }

        // 3. Search Bar on #4F1E5C
        View searchBar = buildSearchBar();
        contentLayout.addView(searchBar);

        // 4. Category Filter Chips (All | Favorites | ...)
        View chipsRow = buildCategoryChips();
        contentLayout.addView(chipsRow);

        // 5. Section Header
        TextView tvHeader = new TextView(activity);
        tvHeader.setText("HYMNS & SPIRITUAL SONGS");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvHeader.setPadding(0, dp(10), 0, dp(8));
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
        tvEmptyState.setText("No hymns found matching your query.\nTry selecting another category or clearing your search.");
        tvEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvEmptyState.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setPadding(dp(16), dp(24), dp(16), dp(24));
        tvEmptyState.setVisibility(View.GONE);
        contentLayout.addView(tvEmptyState);

        // 8. Hymn Cards Container
        cardsContainer = new LinearLayout(activity);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        contentLayout.addView(cardsContainer);

        rootScrollView.addView(contentLayout);
    }

    // =========================================================================
    // 1. CLASSICAL ART BANNER ROW
    // =========================================================================

    @Nullable
    private View buildArtBanner() {
        Bitmap hymnalsBmp = loadAssetBitmap("hymnals.jpg", 400);
        if (hymnalsBmp == null) return null;

        LinearLayout artRow = new LinearLayout(activity);
        artRow.setOrientation(LinearLayout.HORIZONTAL);
        artRow.setPadding(0, 0, 0, dp(12));

        ImageView img1 = new ImageView(activity);
        img1.setImageBitmap(getRoundedCornerBitmap(hymnalsBmp, dp(10)));
        img1.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(90), 1.0f);
        img1.setLayoutParams(lp);
        artRow.addView(img1);

        Bitmap stageBmp = loadAssetBitmap("stage.jpg", 400);
        if (stageBmp != null) {
            lp.setMargins(0, 0, dp(6), 0);
            ImageView img2 = new ImageView(activity);
            img2.setImageBitmap(getRoundedCornerBitmap(stageBmp, dp(10)));
            img2.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(90), 1.0f);
            lp2.setMargins(dp(6), 0, 0, 0);
            img2.setLayoutParams(lp2);
            artRow.addView(img2);
        }

        return artRow;
    }

    // =========================================================================
    // 2. SEARCH BAR (QUIET SURFACE #4F1E5C)
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
        etSearch.setHint("Search hymns by number or lyric keywords…");
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
        btnClear.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        btnClear.setBackground(null);
        btnClear.setPadding(dp(4), dp(4), dp(4), dp(4));
        btnClear.setOnClickListener(v -> etSearch.setText(""));
        searchLayout.addView(btnClear);

        return searchLayout;
    }

    // =========================================================================
    // 3. CATEGORY FILTER CHIPS (All | Favorites | ...)
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
            btn.setPadding(dp(14), dp(4), dp(14), dp(4));

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
                btn.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
                btn.setBackground(createSolidPill(PerazimTheme.COLOR_PURPLE_BRAND, 14));
            } else {
                btn.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
                btn.setBackground(createCardBackground(Color.TRANSPARENT, 14, PerazimTheme.COLOR_STROKE, 1));
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
            View card = createHymnRowCard(hymn);
            cardsContainer.addView(card);
        }
    }

    private View createHymnRowCard(final Hymn hymn) {
        final LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, 14, PerazimTheme.COLOR_STROKE, 1);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        // 1. Number Badge in #A67BB3
        TextView tvNumber = new TextView(activity);
        tvNumber.setText("#" + hymn.getNumber());
        tvNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvNumber.setTypeface(Typeface.DEFAULT_BOLD);
        tvNumber.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvNumber.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 6, PerazimTheme.COLOR_STROKE, 1));
        tvNumber.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpNum = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpNum.setMargins(0, 0, dp(12), 0);
        tvNumber.setLayoutParams(lpNum);
        card.addView(tvNumber);

        // 2. Title & Key Column
        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(hymn.getTitle());
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        textCol.addView(tvTitle);

        String category = hymn.getCategory() != null ? hymn.getCategory() : "Hymn";
        String rawKey = hymn.getKeySignature() != null && !hymn.getKeySignature().isEmpty()
                ? hymn.getKeySignature() : (hymn.getKey() != null ? hymn.getKey() : "G");
        TextView tvCategoryKey = new TextView(activity);
        tvCategoryKey.setText(category + " · Key of " + rawKey.replace("Major", "").trim());
        tvCategoryKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvCategoryKey.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvCategoryKey.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvCategoryKey);

        card.addView(textCol);

        // 3. Quiet Outline Heart Button
        final Button btnHeart = new Button(activity);
        updateHeartButtonState(btnHeart, hymn.isFavorite());
        LinearLayout.LayoutParams lpHeart = new LinearLayout.LayoutParams(dp(34), dp(34));
        lpHeart.setMargins(dp(6), 0, dp(6), 0);
        btnHeart.setLayoutParams(lpHeart);

        btnHeart.setOnClickListener(v -> {
            boolean nextFav = !hymn.isFavorite();
            hymn.setFavorite(nextFav);
            updateHeartButtonState(btnHeart, nextFav);

            viewModel.toggleFavorite(hymn.getId(), nextFav, () -> activity.runOnUiThread(() -> {
                String msg = nextFav ? "Added to Favorites" : "Removed from Favorites";
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                if ("Favorites".equalsIgnoreCase(selectedCategory) && !nextFav) {
                    onCategorySelected("Favorites");
                }
            }));
        });
        card.addView(btnHeart);

        // 4. Clean Vector Chevron Right
        ImageView ivChevron = new ImageView(activity);
        ivChevron.setImageResource(org.perazimchurch.app.R.drawable.ic_chevron_right);
        LinearLayout.LayoutParams lpChev = new LinearLayout.LayoutParams(dp(18), dp(18));
        ivChevron.setLayoutParams(lpChev);
        card.addView(ivChevron);

        card.setOnClickListener(v -> showHymnDetailSheet(hymn));

        return card;
    }

    private void updateHeartButtonState(Button btnHeart, boolean isFav) {
        if (isFav) {
            btnHeart.setText("♥");
            btnHeart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            btnHeart.setTextColor(PerazimTheme.COLOR_ORANGE_BRAND);
            btnHeart.setBackground(createCardBackground(PerazimTheme.COLOR_ORANGE_GHOST, 17, PerazimTheme.COLOR_ORANGE_SOFT, 1));
        } else {
            btnHeart.setText("♡");
            btnHeart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            btnHeart.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            btnHeart.setBackground(createCardBackground(Color.TRANSPARENT, 17, PerazimTheme.COLOR_STROKE, 1));
        }
        btnHeart.setPadding(0, 0, 0, 0);
    }

    // =========================================================================
    // 5. HYMN DETAIL SHEET / DIALOG (QUIET CONTROLS)
    // =========================================================================

    public void showHymnDetailSheet(final Hymn hymn) {
        if (hymn == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ScrollView dialogScroll = new ScrollView(activity);
        dialogScroll.setFillViewport(true);
        dialogScroll.setBackgroundColor(PerazimTheme.COLOR_BG_DEEP);

        LinearLayout detailBox = new LinearLayout(activity);
        detailBox.setOrientation(LinearLayout.VERTICAL);
        detailBox.setPadding(dp(20), dp(20), dp(20), dp(28));
        detailBox.setBackgroundColor(PerazimTheme.COLOR_BG_DEEP);

        // Top Navigation & Controls Row: Back, Favorite, Font Size, Transpose
        LinearLayout topControlsRow = new LinearLayout(activity);
        topControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        topControlsRow.setGravity(Gravity.CENTER_VERTICAL);
        topControlsRow.setPadding(0, 0, 0, dp(14));

        Button btnBack = new Button(activity);
        btnBack.setText("← Back");
        btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnBack.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnBack.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 8, PerazimTheme.COLOR_STROKE, 1));
        btnBack.setPadding(dp(10), dp(6), dp(10), dp(6));
        topControlsRow.addView(btnBack);

        View spacer = new View(activity);
        LinearLayout.LayoutParams lpSpacer = new LinearLayout.LayoutParams(0, 1, 1.0f);
        spacer.setLayoutParams(lpSpacer);
        topControlsRow.addView(spacer);

        // Font Size Controls (A- / A+)
        final float[] currentTextSizeSp = {14f};
        Button btnFontDown = new Button(activity);
        btnFontDown.setText("A-");
        btnFontDown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnFontDown.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnFontDown.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 8, PerazimTheme.COLOR_STROKE, 1));
        btnFontDown.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpFd = new LinearLayout.LayoutParams(dp(36), dp(34));
        lpFd.setMargins(0, 0, dp(4), 0);
        btnFontDown.setLayoutParams(lpFd);
        topControlsRow.addView(btnFontDown);

        Button btnFontUp = new Button(activity);
        btnFontUp.setText("A+");
        btnFontUp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnFontUp.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnFontUp.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 8, PerazimTheme.COLOR_STROKE, 1));
        btnFontUp.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpFu = new LinearLayout.LayoutParams(dp(36), dp(34));
        lpFu.setMargins(0, 0, dp(6), 0);
        btnFontUp.setLayoutParams(lpFu);
        topControlsRow.addView(btnFontUp);

        // Favorite Toggle
        final Button btnFavToggle = new Button(activity);
        updateDialogFavButton(btnFavToggle, hymn.isFavorite());
        topControlsRow.addView(btnFavToggle);

        detailBox.addView(topControlsRow);

        // Number & Title
        TextView tvNumber = new TextView(activity);
        tvNumber.setText("HYMN #" + hymn.getNumber());
        tvNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvNumber.setTypeface(Typeface.DEFAULT_BOLD);
        tvNumber.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        detailBox.addView(tvNumber);

        TextView tvDetailTitle = new TextView(activity);
        tvDetailTitle.setText(hymn.getTitle());
        tvDetailTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        tvDetailTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvDetailTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvDetailTitle.setPadding(0, dp(4), 0, dp(10));
        detailBox.addView(tvDetailTitle);

        // Transpose Controls Row
        final int[] semitoneOffset = {0};
        final String baseChords = hymn.getChords() != null && !hymn.getChords().isEmpty()
                ? hymn.getChords() : "G - C - D";

        LinearLayout transposeRow = new LinearLayout(activity);
        transposeRow.setOrientation(LinearLayout.HORIZONTAL);
        transposeRow.setGravity(Gravity.CENTER_VERTICAL);
        transposeRow.setPadding(dp(12), dp(8), dp(12), dp(8));
        transposeRow.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 10, PerazimTheme.COLOR_STROKE, 1));
        LinearLayout.LayoutParams lpTr = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTr.setMargins(0, 0, 0, dp(12));
        transposeRow.setLayoutParams(lpTr);

        TextView tvTrLabel = new TextView(activity);
        tvTrLabel.setText("Transpose: ");
        tvTrLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvTrLabel.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        transposeRow.addView(tvTrLabel);

        final TextView tvCurrentKey = new TextView(activity);
        String initialKey = hymn.getKeySignature() != null && !hymn.getKeySignature().isEmpty()
                ? hymn.getKeySignature() : (hymn.getKey() != null ? hymn.getKey() : "G");
        tvCurrentKey.setText(initialKey);
        tvCurrentKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvCurrentKey.setTypeface(Typeface.DEFAULT_BOLD);
        tvCurrentKey.setTextColor(PerazimTheme.COLOR_ORANGE_SOFT);
        LinearLayout.LayoutParams lpKey = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvCurrentKey.setLayoutParams(lpKey);
        transposeRow.addView(tvCurrentKey);

        Button btnTrDown = new Button(activity);
        btnTrDown.setText("♭ -1");
        btnTrDown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnTrDown.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnTrDown.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 6, PerazimTheme.COLOR_STROKE, 1));
        btnTrDown.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpTd = new LinearLayout.LayoutParams(dp(44), dp(32));
        lpTd.setMargins(0, 0, dp(4), 0);
        btnTrDown.setLayoutParams(lpTd);
        transposeRow.addView(btnTrDown);

        Button btnTrUp = new Button(activity);
        btnTrUp.setText("♯ +1");
        btnTrUp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnTrUp.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnTrUp.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE_2, 6, PerazimTheme.COLOR_STROKE, 1));
        btnTrUp.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lpTu = new LinearLayout.LayoutParams(dp(44), dp(32));
        btnTrUp.setLayoutParams(lpTu);
        transposeRow.addView(btnTrUp);

        detailBox.addView(transposeRow);

        // Chords Progression Box with Soft Orange (#E8A86A)
        LinearLayout chordsCard = createCard(PerazimTheme.COLOR_SURFACE, 10, PerazimTheme.COLOR_STROKE, 1);
        chordsCard.setOrientation(LinearLayout.VERTICAL);
        chordsCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpChords = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpChords.setMargins(0, 0, 0, dp(14));
        chordsCard.setLayoutParams(lpChords);

        TextView tvChordsHeader = new TextView(activity);
        tvChordsHeader.setText("CHORD PROGRESSION");
        tvChordsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvChordsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvChordsHeader.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        chordsCard.addView(tvChordsHeader);

        final TextView tvChords = new TextView(activity);
        tvChords.setText(baseChords);
        tvChords.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvChords.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvChords.setTextColor(PerazimTheme.COLOR_ORANGE_SOFT);
        tvChords.setPadding(0, dp(4), 0, 0);
        chordsCard.addView(tvChords);

        detailBox.addView(chordsCard);

        // Full Lyrics View (#F5F0F7)
        TextView tvLyricsHeader = new TextView(activity);
        tvLyricsHeader.setText("LYRICS");
        tvLyricsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvLyricsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvLyricsHeader.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvLyricsHeader.setPadding(0, 0, 0, dp(6));
        detailBox.addView(tvLyricsHeader);

        String lyrics = hymn.getLyrics() != null ? hymn.getLyrics() : "Lyrics not available.";
        final TextView tvLyrics = new TextView(activity);
        tvLyrics.setText(lyrics);
        tvLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSizeSp[0]);
        tvLyrics.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvLyrics.setTypeface(Typeface.SERIF);
        tvLyrics.setLineSpacing(dp(4), 1.35f);
        tvLyrics.setPadding(dp(14), dp(14), dp(14), dp(18));
        tvLyrics.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 10, PerazimTheme.COLOR_STROKE, 1));
        detailBox.addView(tvLyrics);

        // Action row: Copy Chords, Share
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(12), 0, 0);

        Button btnCopy = new Button(activity);
        btnCopy.setText("Copy Chords");
        btnCopy.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnCopy.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnCopy.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 8, PerazimTheme.COLOR_STROKE, 1));
        btnCopy.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Hymn Chords", hymn.getTitle() + " Chords: " + tvChords.getText()));
                Toast.makeText(activity, "✓ Chords copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        actionRow.addView(btnCopy);

        Button btnShare = new Button(activity);
        btnShare.setText("Share Hymn");
        btnShare.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnShare.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnShare.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 8, PerazimTheme.COLOR_STROKE, 1));
        btnShare.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams lpS = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpS.setMargins(dp(8), 0, 0, 0);
        btnShare.setLayoutParams(lpS);
        btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, hymn.getTitle() + " - Perazim Hymnal");
            share.putExtra(Intent.EXTRA_TEXT, hymn.getTitle() + "\nChords: " + tvChords.getText() + "\n\n" + hymn.getLyrics() + "\n\n— Perazim Mission Church");
            activity.startActivity(Intent.createChooser(share, "Share Hymn via"));
        });
        actionRow.addView(btnShare);

        detailBox.addView(actionRow);

        dialogScroll.addView(detailBox);
        builder.setView(dialogScroll);
        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(PerazimTheme.COLOR_BG_DEEP));
        }

        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Font Size Listeners
        btnFontDown.setOnClickListener(v -> {
            if (currentTextSizeSp[0] > 11f) {
                currentTextSizeSp[0] -= 1f;
                tvLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSizeSp[0]);
            }
        });
        btnFontUp.setOnClickListener(v -> {
            if (currentTextSizeSp[0] < 22f) {
                currentTextSizeSp[0] += 1f;
                tvLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSizeSp[0]);
            }
        });

        // Transpose Listeners
        btnTrDown.setOnClickListener(v -> {
            semitoneOffset[0]--;
            tvChords.setText(transposeChordLine(baseChords, semitoneOffset[0]));
            tvCurrentKey.setText("Shift: " + (semitoneOffset[0] >= 0 ? "+" : "") + semitoneOffset[0] + " st");
        });
        btnTrUp.setOnClickListener(v -> {
            semitoneOffset[0]++;
            tvChords.setText(transposeChordLine(baseChords, semitoneOffset[0]));
            tvCurrentKey.setText("Shift: " + (semitoneOffset[0] >= 0 ? "+" : "") + semitoneOffset[0] + " st");
        });

        // Favorite Listener
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

        dialog.show();
    }

    private void updateDialogFavButton(Button btn, boolean isFav) {
        if (isFav) {
            btn.setText("♥ Favorited");
            btn.setTextColor(PerazimTheme.COLOR_ORANGE_BRAND);
            btn.setBackground(createCardBackground(PerazimTheme.COLOR_ORANGE_GHOST, 8, PerazimTheme.COLOR_ORANGE_SOFT, 1));
        } else {
            btn.setText("♡ Favorite");
            btn.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            btn.setBackground(createCardBackground(PerazimTheme.COLOR_SURFACE, 8, PerazimTheme.COLOR_STROKE, 1));
        }
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setPadding(dp(10), dp(4), dp(10), dp(4));
    }

    private String transposeChordLine(String chordLine, int offset) {
        if (chordLine == null || chordLine.isEmpty() || offset == 0) return chordLine;
        Pattern pattern = Pattern.compile("\\b([A-G][b#]?)([^A-G\\s]*)");
        Matcher matcher = pattern.matcher(chordLine);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String root = matcher.group(1);
            String suffix = matcher.group(2);
            String transposedRoot = transposeRoot(root, offset);
            matcher.appendReplacement(sb, transposedRoot + suffix);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String transposeRoot(String root, int offset) {
        int index = -1;
        for (int i = 0; i < SCALE.length; i++) {
            if (SCALE[i].equalsIgnoreCase(root) || SCALE_FLATS[i].equalsIgnoreCase(root)) {
                index = i;
                break;
            }
        }
        if (index == -1) return root;
        int newIndex = (index + offset) % 12;
        if (newIndex < 0) newIndex += 12;
        return SCALE[newIndex];
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

    private Drawable createCardBackground(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        return drawable;
    }

    private Drawable createSolidPill(int bgColor, int cornerRadiusDp) {
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
