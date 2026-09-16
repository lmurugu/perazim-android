package com.example.app.presentation.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.domain.model.BibleBook;
import com.example.app.domain.model.BibleVerse;
import com.example.app.presentation.viewmodel.BibleViewModel;
import com.example.app.presentation.viewmodel.ViewModelFactory;
import com.example.app.ui.theme.PerazimTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive full-featured Holy Scripture reader dialog powered by {@link BibleViewModel}.
 * Provides canonical 66 book selection (39 Old Testament / 27 New Testament),
 * dynamic chapter selector grid, full-text scripture search, rich verse typography,
 * bookmark toggling, study notes persistence, and clipboard export.
 */
public class BibleReaderDialog extends Dialog {

    // --- CANONICAL THEME COLORS ---
    private static final int COLOR_PRIMARY_PURPLE = PerazimTheme.COLOR_PRIMARY_PURPLE; // #681A7D
    private static final int COLOR_PURPLE_DARK    = PerazimTheme.COLOR_PURPLE_DEEP;     // #2D0938
    private static final int COLOR_PURPLE_SHADOW  = PerazimTheme.COLOR_PURPLE_SHADOW;   // #450E53
    private static final int COLOR_PURPLE_TINT    = PerazimTheme.COLOR_PURPLE_TINT;     // #F5ECF7
    private static final int COLOR_ACCENT_ORANGE  = PerazimTheme.COLOR_ACCENT_ORANGE;   // #E17D2F
    private static final int COLOR_ORANGE_SHADOW  = PerazimTheme.COLOR_ORANGE_SHADOW;   // #98450B
    private static final int COLOR_ORANGE_TINT    = PerazimTheme.COLOR_ORANGE_TINT;     // #FDF5EF
    private static final int COLOR_ORANGE_BORDER  = PerazimTheme.COLOR_ORANGE_BORDER;   // #F9DFCC
    private static final int COLOR_BORDER_GREY    = PerazimTheme.COLOR_BORDER_GREY;     // #E7D5EC
    private static final int COLOR_BG_NEUTRAL     = PerazimTheme.COLOR_BG_NEUTRAL;      // #FAF7FB
    private static final int COLOR_WHITE          = PerazimTheme.COLOR_WHITE;           // #FFFFFF
    private static final int COLOR_TEXT_DARK      = Color.parseColor("#1A1225");
    private static final int COLOR_TEXT_MUTED     = Color.parseColor("#725B78");

    // Canonical Fallback 66 Books
    private static final Object[][] CANONICAL_BOOKS = {
            {"GEN", "Genesis", "OLD", 50}, {"EXO", "Exodus", "OLD", 40}, {"LEV", "Leviticus", "OLD", 27},
            {"NUM", "Numbers", "OLD", 36}, {"DEU", "Deuteronomy", "OLD", 34}, {"JOS", "Joshua", "OLD", 24},
            {"JDG", "Judges", "OLD", 21}, {"RUT", "Ruth", "OLD", 4}, {"1SA", "1 Samuel", "OLD", 31},
            {"2SA", "2 Samuel", "OLD", 24}, {"1KI", "1 Kings", "OLD", 22}, {"2KI", "2 Kings", "OLD", 25},
            {"1CH", "1 Chronicles", "OLD", 29}, {"2CH", "2 Chronicles", "OLD", 36}, {"EZR", "Ezra", "OLD", 10},
            {"NEH", "Nehemiah", "OLD", 13}, {"EST", "Esther", "OLD", 10}, {"JOB", "Job", "OLD", 42},
            {"PSA", "Psalms", "OLD", 150}, {"PRO", "Proverbs", "OLD", 31}, {"ECC", "Ecclesiastes", "OLD", 12},
            {"SNG", "Song of Solomon", "OLD", 8}, {"ISA", "Isaiah", "OLD", 66}, {"JER", "Jeremiah", "OLD", 52},
            {"LAM", "Lamentations", "OLD", 5}, {"EZK", "Ezekiel", "OLD", 48}, {"DAN", "Daniel", "OLD", 12},
            {"HOS", "Hosea", "OLD", 14}, {"JOL", "Joel", "OLD", 3}, {"AMO", "Amos", "OLD", 9},
            {"OBA", "Obadiah", "OLD", 1}, {"JON", "Jonah", "OLD", 4}, {"MIC", "Micah", "OLD", 7},
            {"NAM", "Nahum", "OLD", 3}, {"HAB", "Habakkuk", "OLD", 3}, {"ZEP", "Zephaniah", "OLD", 3},
            {"HAG", "Haggai", "OLD", 2}, {"ZEC", "Zechariah", "OLD", 14}, {"MAL", "Malachi", "OLD", 4},
            {"MAT", "Matthew", "NEW", 28}, {"MRK", "Mark", "NEW", 16}, {"LUK", "Luke", "NEW", 24},
            {"JHN", "John", "NEW", 21}, {"ACT", "Acts", "NEW", 28}, {"ROM", "Romans", "NEW", 16},
            {"1CO", "1 Corinthians", "NEW", 16}, {"2CO", "2 Corinthians", "NEW", 13}, {"GAL", "Galatians", "NEW", 6},
            {"EPH", "Ephesians", "NEW", 6}, {"PHP", "Philippians", "NEW", 4}, {"COL", "Colossians", "NEW", 4},
            {"1TH", "1 Thessalonians", "NEW", 5}, {"2TH", "2 Thessalonians", "NEW", 3}, {"1TI", "1 Timothy", "NEW", 6},
            {"2TI", "2 Timothy", "NEW", 4}, {"TIT", "Titus", "NEW", 3}, {"PHM", "Philemon", "NEW", 1},
            {"HEB", "Hebrews", "NEW", 13}, {"JAS", "James", "NEW", 5}, {"1PE", "1 Peter", "NEW", 5},
            {"2PE", "2 Peter", "NEW", 3}, {"1JN", "1 John", "NEW", 5}, {"2JN", "2 John", "NEW", 1},
            {"3JN", "3 John", "NEW", 1}, {"JUD", "Jude", "NEW", 1}, {"REV", "Revelation", "NEW", 22}
    };

    private final BibleViewModel bibleViewModel;

    // Active Reader State
    private String currentBookId = "2SA";
    private String currentBookName = "2 Samuel";
    private int currentChapter = 5;
    private int currentChapterCount = 24;
    private final List<BibleBook> booksList = new ArrayList<>();
    private final Map<String, BibleBook> bookMap = new HashMap<>();
    private final List<BibleVerse> currentVerses = new ArrayList<>();

    // Search State
    private boolean isSearchMode = false;
    private final List<BibleVerse> searchResults = new ArrayList<>();

    // UI View References
    private TextView tvTranslationBadge;
    private Button btnBookChapterTitle;
    private Button btnToggleSearch;
    private Button btnCloseDialog;
    private LinearLayout searchBarLayout;
    private EditText etSearchQuery;
    private Button btnExecuteSearch;
    private Button btnClearSearch;
    private LinearLayout chapterNavBar;
    private Button btnPrevChapter;
    private TextView tvChapterProgress;
    private Button btnNextChapter;
    private ScrollView versesScrollView;
    private LinearLayout versesContainer;
    private ProgressBar loadingProgress;
    private TextView tvEmptyState;

    public BibleReaderDialog(@NonNull Context context) {
        this(context, ViewModelFactory.getInstance(context).createBibleViewModel());
    }

    public BibleReaderDialog(@NonNull Context context, @NonNull BibleViewModel bibleViewModel) {
        super(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        this.bibleViewModel = bibleViewModel;
        initFallbackBooks();
    }

    public static BibleReaderDialog show(@NonNull Context context) {
        BibleReaderDialog dialog = new BibleReaderDialog(context);
        dialog.show();
        return dialog;
    }

    public static BibleReaderDialog show(@NonNull Context context, @NonNull BibleViewModel bibleViewModel) {
        BibleReaderDialog dialog = new BibleReaderDialog(context, bibleViewModel);
        dialog.show();
        return dialog;
    }

    public static BibleReaderDialog showPassage(@NonNull Context context, @NonNull BibleViewModel bibleViewModel,
                                               @NonNull String bookId, int chapter) {
        BibleReaderDialog dialog = new BibleReaderDialog(context, bibleViewModel);
        dialog.setTargetPassage(bookId, chapter);
        dialog.show();
        return dialog;
    }

    public void setTargetPassage(@NonNull String bookId, int chapter) {
        this.currentBookId = bookId;
        this.currentChapter = Math.max(1, chapter);
        BibleBook b = bookMap.get(bookId);
        if (b != null) {
            this.currentBookName = b.getName();
            this.currentChapterCount = b.getChapterCount();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View rootView = buildReaderLayout();
        setContentView(rootView);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(COLOR_BG_NEUTRAL));
        }

        loadCanonicalBooks();
        loadChapterVerses(currentBookId, currentChapter);
    }

    private void initFallbackBooks() {
        booksList.clear();
        bookMap.clear();
        for (int i = 0; i < CANONICAL_BOOKS.length; i++) {
            String id = (String) CANONICAL_BOOKS[i][0];
            String name = (String) CANONICAL_BOOKS[i][1];
            String testament = (String) CANONICAL_BOOKS[i][2];
            int chCount = (Integer) CANONICAL_BOOKS[i][3];
            BibleBook b = new BibleBook(id, name, i + 1, testament, chCount);
            booksList.add(b);
            bookMap.put(id, b);
        }
    }

    // =========================================================================
    // UI LAYOUT CONSTRUCTION
    // =========================================================================

    private View buildReaderLayout() {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG_NEUTRAL);

        // 1. Top Header Bar
        View topHeader = createHeaderBar();
        root.addView(topHeader);

        // 2. Search Mode Bar (Collapsible)
        searchBarLayout = createSearchBarLayout();
        searchBarLayout.setVisibility(View.GONE);
        root.addView(searchBarLayout);

        // 3. Quick Chapter Navigation Bar
        chapterNavBar = createChapterNavBar();
        root.addView(chapterNavBar);

        // 4. Verses Content Container inside ScrollView
        versesScrollView = new ScrollView(getContext());
        versesScrollView.setFillViewport(true);
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        versesScrollView.setLayoutParams(lpScroll);

        versesContainer = new LinearLayout(getContext());
        versesContainer.setOrientation(LinearLayout.VERTICAL);
        versesContainer.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Loading ProgressBar
        loadingProgress = new ProgressBar(getContext());
        loadingProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpProgress = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpProgress.gravity = Gravity.CENTER_HORIZONTAL;
        lpProgress.setMargins(0, dp(40), 0, dp(20));
        loadingProgress.setLayoutParams(lpProgress);
        versesContainer.addView(loadingProgress);

        // Empty / Status TextView
        tvEmptyState = new TextView(getContext());
        tvEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvEmptyState.setTextColor(COLOR_PURPLE_DARK);
        tvEmptyState.setTypeface(Typeface.DEFAULT_BOLD);
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        tvEmptyState.setPadding(dp(16), dp(20), dp(16), dp(20));
        tvEmptyState.setLineSpacing(dp(4), 1.2f);
        tvEmptyState.setVisibility(View.GONE);
        versesContainer.addView(tvEmptyState);

        versesScrollView.addView(versesContainer);
        root.addView(versesScrollView);

        return root;
    }

    private View createHeaderBar() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(COLOR_WHITE);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(COLOR_WHITE);
        headerBg.setStroke(dp(1), COLOR_BORDER_GREY);
        header.setBackground(headerBg);

        // Top Row: Translation Badge + Close Button
        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        tvTranslationBadge = new TextView(getContext());
        tvTranslationBadge.setText("KJV (66-Book Catalog & Core Starter Verses)");
        tvTranslationBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTranslationBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvTranslationBadge.setTextColor(COLOR_PRIMARY_PURPLE);
        tvTranslationBadge.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        tvTranslationBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        tvTranslationBadge.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Bible Translation")
                    .setMessage("Active: KJV (66-Book Catalog & Core Starter Verses)\n\nCanonical 66-book catalog with core starter verses offline. The complete 31,102-verse corpus syncs in Phase 4.")
                    .setPositiveButton("OK", null)
                    .show();
        });
        topRow.addView(tvTranslationBadge);

        View space = new View(getContext());
        topRow.addView(space, new LinearLayout.LayoutParams(0, 1, 1.0f));

        btnToggleSearch = new Button(getContext());
        btnToggleSearch.setText("🔍 Search");
        btnToggleSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnToggleSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnToggleSearch.setTextColor(COLOR_PRIMARY_PURPLE);
        btnToggleSearch.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        btnToggleSearch.setPadding(dp(10), dp(4), dp(10), dp(4));
        btnToggleSearch.setOnClickListener(v -> toggleSearchMode());
        LinearLayout.LayoutParams lpSearchBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
        lpSearchBtn.setMargins(0, 0, dp(6), 0);
        btnToggleSearch.setLayoutParams(lpSearchBtn);
        topRow.addView(btnToggleSearch);

        btnCloseDialog = new Button(getContext());
        btnCloseDialog.setText("✕");
        btnCloseDialog.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnCloseDialog.setTypeface(Typeface.DEFAULT_BOLD);
        btnCloseDialog.setTextColor(COLOR_PURPLE_DARK);
        btnCloseDialog.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(dp(36), dp(32));
        btnCloseDialog.setLayoutParams(lpClose);
        btnCloseDialog.setOnClickListener(v -> dismiss());
        topRow.addView(btnCloseDialog);

        header.addView(topRow);

        // Bottom Row: Current Book & Chapter Selector Button (Default: "2 Samuel 5 · Baal-perazim")
        btnBookChapterTitle = new Button(getContext());
        updateHeaderTitle();
        btnBookChapterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnBookChapterTitle.setTypeface(Typeface.DEFAULT_BOLD);
        btnBookChapterTitle.setTextColor(COLOR_WHITE);
        btnBookChapterTitle.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 2));
        btnBookChapterTitle.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams lpTitleBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTitleBtn.setMargins(0, dp(8), 0, 0);
        btnBookChapterTitle.setLayoutParams(lpTitleBtn);
        btnBookChapterTitle.setOnClickListener(v -> showBookSelectorDialog());
        header.addView(btnBookChapterTitle);

        return header;
    }

    private LinearLayout createSearchBarLayout() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setBackgroundColor(COLOR_WHITE);
        layout.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_WHITE);
        bg.setStroke(dp(1), COLOR_ORANGE_BORDER);
        layout.setBackground(bg);

        etSearchQuery = new EditText(getContext());
        etSearchQuery.setHint("Search Holy Scripture (e.g. Baal-perazim, break, faith)...");
        etSearchQuery.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etSearchQuery.setTextColor(COLOR_TEXT_DARK);
        etSearchQuery.setSingleLine(true);
        etSearchQuery.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etSearchQuery.setLayoutParams(lpEt);
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchQuery.getText().toString().trim());
                hideKeyboard(etSearchQuery);
                return true;
            }
            return false;
        });
        layout.addView(etSearchQuery);

        btnExecuteSearch = new Button(getContext());
        btnExecuteSearch.setText("Search");
        btnExecuteSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnExecuteSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnExecuteSearch.setTextColor(COLOR_WHITE);
        btnExecuteSearch.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 8, 2));
        LinearLayout.LayoutParams lpExec = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
        lpExec.setMargins(dp(6), 0, dp(4), 0);
        btnExecuteSearch.setLayoutParams(lpExec);
        btnExecuteSearch.setOnClickListener(v -> {
            performSearch(etSearchQuery.getText().toString().trim());
            hideKeyboard(etSearchQuery);
        });
        layout.addView(btnExecuteSearch);

        btnClearSearch = new Button(getContext());
        btnClearSearch.setText("✕");
        btnClearSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnClearSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnClearSearch.setTextColor(COLOR_TEXT_MUTED);
        btnClearSearch.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpClr = new LinearLayout.LayoutParams(dp(34), dp(34));
        btnClearSearch.setLayoutParams(lpClr);
        btnClearSearch.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etSearchQuery.getText())) {
                etSearchQuery.setText("");
            } else {
                toggleSearchMode();
            }
        });
        layout.addView(btnClearSearch);

        return layout;
    }

    private LinearLayout createChapterNavBar() {
        LinearLayout nav = new LinearLayout(getContext());
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackgroundColor(COLOR_PURPLE_TINT);
        nav.setPadding(dp(12), dp(6), dp(12), dp(6));

        btnPrevChapter = new Button(getContext());
        btnPrevChapter.setText("◀ Prev Ch");
        btnPrevChapter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnPrevChapter.setTypeface(Typeface.DEFAULT_BOLD);
        btnPrevChapter.setTextColor(COLOR_PRIMARY_PURPLE);
        btnPrevChapter.setBackground(createPillBg(COLOR_WHITE, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpPrev = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
        btnPrevChapter.setLayoutParams(lpPrev);
        btnPrevChapter.setOnClickListener(v -> navigateChapter(-1));
        nav.addView(btnPrevChapter);

        tvChapterProgress = new TextView(getContext());
        tvChapterProgress.setText("Chapter " + currentChapter + " of " + currentChapterCount);
        tvChapterProgress.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvChapterProgress.setTypeface(Typeface.DEFAULT_BOLD);
        tvChapterProgress.setTextColor(COLOR_PURPLE_DARK);
        tvChapterProgress.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpProg = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvChapterProgress.setLayoutParams(lpProg);
        nav.addView(tvChapterProgress);

        btnNextChapter = new Button(getContext());
        btnNextChapter.setText("Next Ch ▶");
        btnNextChapter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnNextChapter.setTypeface(Typeface.DEFAULT_BOLD);
        btnNextChapter.setTextColor(COLOR_PRIMARY_PURPLE);
        btnNextChapter.setBackground(createPillBg(COLOR_WHITE, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpNext = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
        btnNextChapter.setLayoutParams(lpNext);
        btnNextChapter.setOnClickListener(v -> navigateChapter(1));
        nav.addView(btnNextChapter);

        return nav;
    }

    private void updateHeaderTitle() {
        String title = "📖 " + currentBookName + " " + currentChapter;
        if ("2SA".equals(currentBookId) && currentChapter == 5) {
            title += " · Baal-perazim ▾";
        } else {
            title += " ▾";
        }
        if (btnBookChapterTitle != null) {
            btnBookChapterTitle.setText(title);
        }
        if (tvChapterProgress != null) {
            tvChapterProgress.setText("Chapter " + currentChapter + " of " + currentChapterCount);
        }
    }

    // =========================================================================
    // SCRIPTURE DATA LOADING & SEARCH
    // =========================================================================

    private void loadCanonicalBooks() {
        bibleViewModel.loadBooks(books -> {
            if (books != null && !books.isEmpty()) {
                booksList.clear();
                booksList.addAll(books);
                bookMap.clear();
                for (BibleBook b : books) {
                    bookMap.put(b.getId(), b);
                }
                BibleBook current = bookMap.get(currentBookId);
                if (current != null) {
                    currentBookName = current.getName();
                    currentChapterCount = current.getChapterCount();
                    updateHeaderTitle();
                }
            }
        });
    }

    public void loadChapterVerses(@NonNull String bookId, int chapter) {
        this.currentBookId = bookId;
        this.currentChapter = chapter;
        BibleBook b = bookMap.get(bookId);
        if (b != null) {
            this.currentBookName = b.getName();
            this.currentChapterCount = b.getChapterCount();
        }
        updateHeaderTitle();

        isSearchMode = false;
        if (searchBarLayout != null) {
            searchBarLayout.setVisibility(View.GONE);
        }
        if (chapterNavBar != null) {
            chapterNavBar.setVisibility(View.VISIBLE);
        }

        loadingProgress.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        clearVerseViews();

        bibleViewModel.loadVerses(bookId, chapter, verses -> {
            loadingProgress.setVisibility(View.GONE);
            if (verses != null && !verses.isEmpty()) {
                currentVerses.clear();
                currentVerses.addAll(verses);
                renderVerseList(currentVerses, false);
            } else {
                // Safeguard: Fallback to 2 Samuel 5 canonical passage if empty
                if ("2SA".equals(bookId) && chapter == 5) {
                    load2Samuel5Fallback();
                } else {
                    tvEmptyState.setText("📖 Canonical Catalog: " + currentBookName + " Chapter " + currentChapter + "\n(Chapter text staged for offline sync. Complete 31,102-verse corpus syncs in Phase 4)");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void load2Samuel5Fallback() {
        currentVerses.clear();
        String[][] raw = {
                {"17", "When the Philistines heard that David had been anointed king over Israel, they went up in full force to search for David, but David heard about it and went down to the stronghold."},
                {"18", "Now the Philistines had come and spread out in the Valley of Rephaim;"},
                {"19", "so David inquired of the LORD, “Shall I go and attack the Philistines? Will you deliver them into my hands?” The LORD answered him, “Go, for I will surely deliver the Philistines into your hands.”"},
                {"20", "And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters. Therefore he called the name of that place Baal-perazim."},
                {"21", "The Philistines abandoned their idols there, and David and his men carried them off."}
        };
        for (String[] r : raw) {
            int vNum = Integer.parseInt(r[0]);
            BibleVerse v = new BibleVerse("2SA.5." + vNum, "KJV", "2SA", 5, vNum, r[1], false, null);
            currentVerses.add(v);
        }
        renderVerseList(currentVerses, false);
    }

    private void toggleSearchMode() {
        isSearchMode = !isSearchMode;
        if (isSearchMode) {
            searchBarLayout.setVisibility(View.VISIBLE);
            chapterNavBar.setVisibility(View.GONE);
            etSearchQuery.requestFocus();
            showKeyboard(etSearchQuery);
            if (!searchResults.isEmpty()) {
                renderVerseList(searchResults, true);
            }
        } else {
            searchBarLayout.setVisibility(View.GONE);
            chapterNavBar.setVisibility(View.VISIBLE);
            hideKeyboard(etSearchQuery);
            renderVerseList(currentVerses, false);
        }
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(getContext(), "Please enter a keyword to search", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingProgress.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        clearVerseViews();

        bibleViewModel.searchScripture(query, results -> {
            loadingProgress.setVisibility(View.GONE);
            searchResults.clear();
            if (results != null && !results.isEmpty()) {
                searchResults.addAll(results);
                renderVerseList(searchResults, true);
            } else {
                tvEmptyState.setText("No scripture verses found matching \"" + query + "\".");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void navigateChapter(int delta) {
        int next = currentChapter + delta;
        if (next >= 1 && next <= currentChapterCount) {
            loadChapterVerses(currentBookId, next);
        } else {
            Toast.makeText(getContext(),
                    next < 1 ? "First chapter reached" : "Last chapter of " + currentBookName + " reached",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // VERSE LIST RENDERING & TYPOGRAPHY
    // =========================================================================

    private void clearVerseViews() {
        for (int i = versesContainer.getChildCount() - 1; i >= 0; i--) {
            View child = versesContainer.getChildAt(i);
            if (child != loadingProgress && child != tvEmptyState) {
                versesContainer.removeViewAt(i);
            }
        }
    }

    private void renderVerseList(@NonNull List<BibleVerse> verses, boolean isSearchResult) {
        clearVerseViews();

        if (isSearchResult) {
            TextView searchSummary = new TextView(getContext());
            searchSummary.setText("Found " + verses.size() + " verses matching keyword query");
            searchSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            searchSummary.setTypeface(Typeface.DEFAULT_BOLD);
            searchSummary.setTextColor(COLOR_ACCENT_ORANGE);
            searchSummary.setBackground(createPillBg(COLOR_ORANGE_TINT, COLOR_ORANGE_BORDER));
            searchSummary.setPadding(dp(10), dp(6), dp(10), dp(6));
            LinearLayout.LayoutParams lpSumm = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpSumm.setMargins(0, 0, 0, dp(12));
            searchSummary.setLayoutParams(lpSumm);
            versesContainer.addView(searchSummary);
        } else {
            // Chapter Title Header
            TextView chapterHeader = new TextView(getContext());
            chapterHeader.setText(currentBookName.toUpperCase() + " · CHAPTER " + currentChapter);
            chapterHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chapterHeader.setTypeface(Typeface.DEFAULT_BOLD);
            chapterHeader.setTextColor(COLOR_PRIMARY_PURPLE);
            chapterHeader.setPadding(0, 0, 0, dp(10));
            versesContainer.addView(chapterHeader);
        }

        for (final BibleVerse verse : verses) {
            View verseRow = createVerseRow(verse, isSearchResult);
            versesContainer.addView(verseRow);
        }

        versesScrollView.post(() -> versesScrollView.scrollTo(0, 0));
    }

    private View createVerseRow(final BibleVerse verse, final boolean isSearchResult) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        // Highlight Cornerstone Verse (2 Samuel 5:20) or Bookmarked Verses
        boolean isCornerstone = "2SA".equals(verse.getBookId()) && verse.getChapterNumber() == 5 && verse.getVerseNumber() == 20;
        GradientDrawable rowBg = new GradientDrawable();
        if (isCornerstone) {
            rowBg.setColor(COLOR_PURPLE_TINT);
            rowBg.setStroke(dp(1), COLOR_PRIMARY_PURPLE);
            rowBg.setCornerRadius(dp(10));
        } else if (verse.isFavorite()) {
            rowBg.setColor(COLOR_ORANGE_TINT);
            rowBg.setStroke(dp(1), COLOR_ACCENT_ORANGE);
            rowBg.setCornerRadius(dp(8));
        } else {
            rowBg.setColor(COLOR_WHITE);
            rowBg.setStroke(dp(1), COLOR_BORDER_GREY);
            rowBg.setCornerRadius(dp(8));
        }
        row.setBackground(rowBg);

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lpRow);

        // Verse Header: Citation or Verse Number + Indicators
        LinearLayout topVerseInfo = new LinearLayout(getContext());
        topVerseInfo.setOrientation(LinearLayout.HORIZONTAL);
        topVerseInfo.setGravity(Gravity.CENTER_VERTICAL);

        String citation = formatCitation(verse);
        TextView tvVerseNumber = new TextView(getContext());
        if (isSearchResult) {
            tvVerseNumber.setText("📖 " + citation);
        } else {
            tvVerseNumber.setText("Verse " + verse.getVerseNumber());
        }
        tvVerseNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvVerseNumber.setTypeface(Typeface.DEFAULT_BOLD);
        tvVerseNumber.setTextColor(isCornerstone ? COLOR_PRIMARY_PURPLE : COLOR_ACCENT_ORANGE);
        topVerseInfo.addView(tvVerseNumber);

        View spring = new View(getContext());
        topVerseInfo.addView(spring, new LinearLayout.LayoutParams(0, 1, 1.0f));

        if (verse.isFavorite()) {
            TextView favBadge = new TextView(getContext());
            favBadge.setText("⭐ Saved");
            favBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            favBadge.setTextColor(COLOR_ACCENT_ORANGE);
            favBadge.setPadding(dp(4), 0, dp(4), 0);
            topVerseInfo.addView(favBadge);
        }

        if (!TextUtils.isEmpty(verse.getNote())) {
            TextView noteBadge = new TextView(getContext());
            noteBadge.setText("📝 Study Note");
            noteBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            noteBadge.setTextColor(COLOR_PRIMARY_PURPLE);
            noteBadge.setPadding(dp(4), 0, dp(4), 0);
            topVerseInfo.addView(noteBadge);
        }

        row.addView(topVerseInfo);

        // Verse Text Body
        TextView tvVerseText = new TextView(getContext());
        tvVerseText.setText(verse.getText());
        tvVerseText.setTextSize(TypedValue.COMPLEX_UNIT_SP, isCornerstone ? 15 : 14);
        tvVerseText.setTypeface(Typeface.SERIF, isCornerstone ? Typeface.BOLD : Typeface.NORMAL);
        tvVerseText.setTextColor(COLOR_TEXT_DARK);
        tvVerseText.setLineSpacing(dp(3), 1.2f);
        tvVerseText.setPadding(0, dp(6), 0, dp(4));
        row.addView(tvVerseText);

        // Study Note Preview Box if Present
        if (!TextUtils.isEmpty(verse.getNote())) {
            TextView tvNoteContent = new TextView(getContext());
            tvNoteContent.setText("“" + verse.getNote() + "”");
            tvNoteContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvNoteContent.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            tvNoteContent.setTextColor(COLOR_PURPLE_DARK);
            tvNoteContent.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
            tvNoteContent.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams lpNote = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpNote.setMargins(0, dp(4), 0, dp(2));
            tvNoteContent.setLayoutParams(lpNote);
            row.addView(tvNoteContent);
        }

        // Tap Hint
        TextView tvTapHint = new TextView(getContext());
        tvTapHint.setText("Tap for Bookmark · Study Note · Copy");
        tvTapHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvTapHint.setTextColor(COLOR_TEXT_MUTED);
        tvTapHint.setPadding(0, dp(4), 0, 0);
        row.addView(tvTapHint);

        // Interactive Verse Click: Opens Options Modal
        row.setOnClickListener(v -> showVerseOptionsModal(verse));

        return row;
    }

    private String formatCitation(BibleVerse verse) {
        String bName = currentBookName;
        BibleBook b = bookMap.get(verse.getBookId());
        if (b != null) {
            bName = b.getName();
        }
        return bName + " " + verse.getChapterNumber() + ":" + verse.getVerseNumber();
    }

    // =========================================================================
    // INTERACTIVE VERSE OPTIONS MODAL
    // =========================================================================

    private void showVerseOptionsModal(final BibleVerse verse) {
        final String citation = formatCitation(verse);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundColor(COLOR_WHITE);

        // Title
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("📖 " + citation);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_PURPLE_DARK);
        container.addView(tvTitle);

        // Verse Text Snippet
        TextView tvSnippet = new TextView(getContext());
        tvSnippet.setText("“" + verse.getText() + "”");
        tvSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvSnippet.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvSnippet.setTextColor(COLOR_TEXT_MUTED);
        tvSnippet.setLineSpacing(dp(2), 1.15f);
        tvSnippet.setPadding(0, dp(6), 0, dp(14));
        container.addView(tvSnippet);

        builder.setView(container);
        final AlertDialog dialog = builder.create();

        // Action 1: Bookmark / Highlight Toggle
        String bookmarkLabel = verse.isFavorite() ? "⭐ Remove Bookmark" : "☆ Bookmark / Highlight";
        Button btnBookmark = createOptionButton(bookmarkLabel, COLOR_ACCENT_ORANGE, COLOR_WHITE);
        btnBookmark.setOnClickListener(v -> {
            boolean nextState = !verse.isFavorite();
            verse.setFavorite(nextState);
            bibleViewModel.toggleBookmark(verse.getId(), nextState, () -> {
                Toast.makeText(getContext(), nextState ? "✓ Verse bookmarked!" : "Bookmark removed", Toast.LENGTH_SHORT).show();
            });
            dialog.dismiss();
            renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
        });
        container.addView(btnBookmark);

        // Action 2: Add / Edit Study Note
        String noteLabel = !TextUtils.isEmpty(verse.getNote()) ? "📝 Edit Study Note" : "📝 Add Study Note";
        Button btnNote = createOptionButton(noteLabel, COLOR_PRIMARY_PURPLE, COLOR_WHITE);
        btnNote.setOnClickListener(v -> {
            dialog.dismiss();
            showStudyNoteEditor(verse);
        });
        container.addView(btnNote);

        // Action 3: Copy to Clipboard
        Button btnCopy = createOptionButton("📋 Copy to Clipboard", COLOR_PURPLE_DARK, COLOR_WHITE);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Holy Scripture", citation + "\n\"" + verse.getText() + "\" (KJV)");
            if (cm != null) {
                cm.setPrimaryClip(clip);
                Toast.makeText(getContext(), "✓ Copied " + citation + " to clipboard!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        container.addView(btnCopy);

        // Action 4: In Search mode, jump to this chapter
        if (isSearchMode) {
            Button btnGoToChapter = createOptionButton("➔ Open Chapter in Reader", COLOR_PRIMARY_PURPLE, COLOR_WHITE);
            btnGoToChapter.setOnClickListener(v -> {
                dialog.dismiss();
                loadChapterVerses(verse.getBookId(), verse.getChapterNumber());
            });
            container.addView(btnGoToChapter);
        }

        Button btnCancel = createOptionButton("Close", COLOR_BG_NEUTRAL, COLOR_TEXT_MUTED);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.show();
    }

    private void showStudyNoteEditor(final BibleVerse verse) {
        final String citation = formatCitation(verse);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("📝 Personal Study Note · " + citation);

        final EditText etNote = new EditText(getContext());
        etNote.setHint("Write your personal reflection, revelation, or sermon note...");
        etNote.setText(verse.getNote() != null ? verse.getNote() : "");
        etNote.setMinLines(3);
        etNote.setGravity(Gravity.TOP | Gravity.START);
        etNote.setPadding(dp(14), dp(12), dp(14), dp(12));
        builder.setView(etNote);

        builder.setPositiveButton("Save Note", (d, which) -> {
            String noteText = etNote.getText().toString().trim();
            verse.setNote(noteText);
            bibleViewModel.saveStudyNote(verse.getId(), noteText, () -> {
                Toast.makeText(getContext(), "✓ Study note saved for " + citation, Toast.LENGTH_SHORT).show();
            });
            renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private Button createOptionButton(String text, int bgColor, int textColor) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(textColor);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        lp.setMargins(0, dp(6), 0, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    // =========================================================================
    // BOOK & CHAPTER SELECTOR DIALOGS
    // =========================================================================

    private void showBookSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        container.setBackgroundColor(COLOR_WHITE);

        // Header Title
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("📖 Select Book of the Bible");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_PURPLE_DARK);
        container.addView(tvTitle);

        TextView tvSub = new TextView(getContext());
        tvSub.setText("Canonical 66 Books · KJV (66-Book Catalog & Core Starter Verses)");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(COLOR_TEXT_MUTED);
        tvSub.setPadding(0, dp(2), 0, dp(12));
        container.addView(tvSub);

        // Testament Tabs (Old Testament 39 · New Testament 27)
        final LinearLayout tabsRow = new LinearLayout(getContext());
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabsRow.setPadding(0, 0, 0, dp(10));

        final Button btnTabOt = new Button(getContext());
        btnTabOt.setText("Old Testament (39)");
        btnTabOt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnTabOt.setTypeface(Typeface.DEFAULT_BOLD);
        btnTabOt.setTextColor(COLOR_WHITE);
        btnTabOt.setBackground(createPillBg(COLOR_PRIMARY_PURPLE, COLOR_PRIMARY_PURPLE));
        LinearLayout.LayoutParams lpOt = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
        lpOt.setMargins(0, 0, dp(4), 0);
        btnTabOt.setLayoutParams(lpOt);
        tabsRow.addView(btnTabOt);

        final Button btnTabNt = new Button(getContext());
        btnTabNt.setText("New Testament (27)");
        btnTabNt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnTabNt.setTypeface(Typeface.DEFAULT_BOLD);
        btnTabNt.setTextColor(COLOR_TEXT_MUTED);
        btnTabNt.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpNt = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
        lpNt.setMargins(dp(4), 0, 0, 0);
        btnTabNt.setLayoutParams(lpNt);
        tabsRow.addView(btnTabNt);

        container.addView(tabsRow);

        // Books List ScrollView
        ScrollView svBooks = new ScrollView(getContext());
        LinearLayout.LayoutParams lpSv = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(380));
        svBooks.setLayoutParams(lpSv);

        final LinearLayout booksContainer = new LinearLayout(getContext());
        booksContainer.setOrientation(LinearLayout.VERTICAL);
        svBooks.addView(booksContainer);
        container.addView(svBooks);

        builder.setView(container);
        builder.setNegativeButton("Cancel", null);
        final AlertDialog bookDialog = builder.create();

        // Render Books for Selected Testament
        final Runnable renderBooksOt = () -> populateBooksInContainer(booksContainer, "OLD", bookDialog);
        final Runnable renderBooksNt = () -> populateBooksInContainer(booksContainer, "NEW", bookDialog);

        btnTabOt.setOnClickListener(v -> {
            btnTabOt.setTextColor(COLOR_WHITE);
            btnTabOt.setBackground(createPillBg(COLOR_PRIMARY_PURPLE, COLOR_PRIMARY_PURPLE));
            btnTabNt.setTextColor(COLOR_TEXT_MUTED);
            btnTabNt.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
            renderBooksOt.run();
        });

        btnTabNt.setOnClickListener(v -> {
            btnTabNt.setTextColor(COLOR_WHITE);
            btnTabNt.setBackground(createPillBg(COLOR_PRIMARY_PURPLE, COLOR_PRIMARY_PURPLE));
            btnTabOt.setTextColor(COLOR_TEXT_MUTED);
            btnTabOt.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
            renderBooksNt.run();
        });

        // Default open to current book's testament
        BibleBook current = bookMap.get(currentBookId);
        boolean isCurrentNt = current != null && "NEW".equalsIgnoreCase(current.getTestament());
        if (isCurrentNt) {
            btnTabNt.performClick();
        } else {
            renderBooksOt.run();
        }

        bookDialog.show();
    }

    private void populateBooksInContainer(LinearLayout container, String testament, final AlertDialog parentDialog) {
        container.removeAllViews();
        List<BibleBook> filtered = new ArrayList<>();
        for (BibleBook b : booksList) {
            if (testament.equalsIgnoreCase(b.getTestament())) {
                filtered.add(b);
            }
        }

        for (final BibleBook book : filtered) {
            LinearLayout itemRow = new LinearLayout(getContext());
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding(dp(12), dp(10), dp(12), dp(10));

            boolean isCurrent = book.getId().equals(currentBookId);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(isCurrent ? COLOR_PURPLE_TINT : COLOR_WHITE);
            bg.setStroke(dp(1), isCurrent ? COLOR_PRIMARY_PURPLE : COLOR_BORDER_GREY);
            bg.setCornerRadius(dp(8));
            itemRow.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(6));
            itemRow.setLayoutParams(lp);

            TextView tvBookName = new TextView(getContext());
            tvBookName.setText(book.getName());
            tvBookName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvBookName.setTypeface(Typeface.DEFAULT_BOLD);
            tvBookName.setTextColor(isCurrent ? COLOR_PRIMARY_PURPLE : COLOR_TEXT_DARK);
            LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tvBookName.setLayoutParams(lpName);
            itemRow.addView(tvBookName);

            TextView tvChapters = new TextView(getContext());
            tvChapters.setText(book.getChapterCount() + " ch");
            tvChapters.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvChapters.setTextColor(COLOR_TEXT_MUTED);
            itemRow.addView(tvChapters);

            itemRow.setOnClickListener(v -> {
                parentDialog.dismiss();
                showChapterSelectorGrid(book);
            });

            container.addView(itemRow);
        }
    }

    private void showChapterSelectorGrid(final BibleBook book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        container.setBackgroundColor(COLOR_WHITE);

        // Header Title
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("📖 " + book.getName() + " · Select Chapter");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_PURPLE_DARK);
        container.addView(tvTitle);

        TextView tvSub = new TextView(getContext());
        tvSub.setText("Select from 1 to " + book.getChapterCount() + " chapters");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(COLOR_TEXT_MUTED);
        tvSub.setPadding(0, dp(2), 0, dp(12));
        container.addView(tvSub);

        // Grid of Chapters inside ScrollView
        ScrollView sv = new ScrollView(getContext());
        LinearLayout.LayoutParams lpSv = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(350));
        sv.setLayoutParams(lpSv);

        LinearLayout gridContainer = new LinearLayout(getContext());
        gridContainer.setOrientation(LinearLayout.VERTICAL);

        builder.setView(container);
        builder.setNegativeButton("Back to Books", (d, w) -> showBookSelectorDialog());
        final AlertDialog chDialog = builder.create();

        int columns = 5;
        int total = book.getChapterCount();
        LinearLayout currentRow = null;

        for (int ch = 1; ch <= total; ch++) {
            if ((ch - 1) % columns == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lpRow.setMargins(0, 0, 0, dp(8));
                currentRow.setLayoutParams(lpRow);
                gridContainer.addView(currentRow);
            }

            final int chNum = ch;
            boolean isCurrent = book.getId().equals(currentBookId) && chNum == currentChapter;

            Button btnCh = new Button(getContext());
            btnCh.setText(String.valueOf(chNum));
            btnCh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnCh.setTypeface(Typeface.DEFAULT_BOLD);
            btnCh.setTextColor(isCurrent ? COLOR_WHITE : COLOR_TEXT_DARK);

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(isCurrent ? COLOR_PRIMARY_PURPLE : COLOR_PURPLE_TINT);
            btnBg.setStroke(dp(1), isCurrent ? COLOR_PRIMARY_PURPLE : COLOR_BORDER_GREY);
            btnBg.setCornerRadius(dp(8));
            btnCh.setBackground(btnBg);

            LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
            lpBtn.setMargins(dp(3), 0, dp(3), 0);
            btnCh.setLayoutParams(lpBtn);

            btnCh.setOnClickListener(v -> {
                chDialog.dismiss();
                loadChapterVerses(book.getId(), chNum);
            });

            if (currentRow != null) {
                currentRow.addView(btnCh);
            }
        }

        // Fill remaining empty cells in last row for alignment
        if (currentRow != null && currentRow.getChildCount() < columns) {
            int remaining = columns - currentRow.getChildCount();
            for (int i = 0; i < remaining; i++) {
                View placeholder = new View(getContext());
                LinearLayout.LayoutParams lpHolder = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
                lpHolder.setMargins(dp(3), 0, dp(3), 0);
                placeholder.setLayoutParams(lpHolder);
                currentRow.addView(placeholder);
            }
        }

        sv.addView(gridContainer);
        container.addView(sv);

        chDialog.show();
    }

    // =========================================================================
    // UI GRAPHICS & DRAWABLE HELPERS
    // =========================================================================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getContext().getResources().getDisplayMetrics());
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

    private void showKeyboard(View view) {
        if (view == null) return;
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
