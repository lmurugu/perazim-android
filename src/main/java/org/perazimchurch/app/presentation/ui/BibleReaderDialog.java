package org.perazimchurch.app.presentation.ui;

import org.perazimchurch.app.ui.theme.AppDialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.presentation.viewmodel.BibleViewModel;
import org.perazimchurch.app.presentation.viewmodel.ViewModelFactory;
import org.perazimchurch.app.ui.theme.GlassKit;
import org.perazimchurch.app.ui.theme.PerazimTheme;

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

    @Override
    public void show() {
        super.show();
        org.perazimchurch.app.ui.theme.GlassPolish.install(this);
    }

    // --- CANONICAL THEME COLORS ---
    private static int COLOR_PRIMARY_PURPLE() { return PerazimTheme.COLOR_PRIMARY_PURPLE; } // #681A7D
    private static int COLOR_PURPLE_DARK() { return PerazimTheme.COLOR_PURPLE_DEEP; }     // #2D0938
    private static int COLOR_PURPLE_SHADOW() { return PerazimTheme.COLOR_PURPLE_SHADOW; }   // #450E53
    private static int COLOR_PURPLE_TINT() { return PerazimTheme.COLOR_PURPLE_TINT; }     // #F5ECF7
    private static int COLOR_ACCENT_ORANGE() { return PerazimTheme.COLOR_ACCENT_ORANGE; }   // #E17D2F
    private static int COLOR_ORANGE_SHADOW() { return PerazimTheme.COLOR_ORANGE_SHADOW; }   // #98450B
    private static int COLOR_ORANGE_TINT() { return PerazimTheme.COLOR_ORANGE_TINT; }     // #FDF5EF
    private static int COLOR_ORANGE_BORDER() { return PerazimTheme.COLOR_ORANGE_BORDER; }   // #F9DFCC
    private static int COLOR_BORDER_GREY() { return PerazimTheme.COLOR_BORDER_GREY; }     // #E7D5EC
    private static int COLOR_BG_NEUTRAL() { return PerazimTheme.COLOR_BG_NEUTRAL; }      // #FAF7FB
    private static int COLOR_WHITE() { return PerazimTheme.COLOR_WHITE; }           // #FFFFFF
    private static int COLOR_TEXT_DARK() { return PerazimTheme.COLOR_TEXT_PRIMARY; }
    private static int COLOR_TEXT_MUTED() { return PerazimTheme.COLOR_TEXT_SECONDARY; }

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
    private String currentTranslation = "KJV";
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
    private TextView btnBookChapterTitle;
    private View btnToggleSearch;
    private View btnCloseDialog;
    private LinearLayout searchBarLayout;
    private EditText etSearchQuery;
    private Button btnExecuteSearch;
    private Button btnClearSearch;
    private LinearLayout chapterNavBar;
    private TextView btnPrevChapter;
    private TextView tvChapterProgress;
    private TextView btnNextChapter;
    private ScrollView versesScrollView;
    private LinearLayout versesContainer;
    private ProgressBar loadingProgress;
    private TextView tvEmptyState;

    public BibleReaderDialog(@NonNull Context context) {
        this(context, ViewModelFactory.getInstance(context).createBibleViewModel());
    }

    public BibleReaderDialog(@NonNull Context context, @NonNull BibleViewModel bibleViewModel) {
        super(context, org.perazimchurch.app.ui.theme.AppDialogs.fullscreenTheme());
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
            getWindow().setBackgroundDrawable(PerazimTheme.createBackgroundGradient());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(PerazimTheme.COLOR_BG_DEEP);
                getWindow().setNavigationBarColor(PerazimTheme.COLOR_BG_DEEP);
            }
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
        // Mockup board "Bible reader": glass header pills, one glass card of serif verses, Previous / Next.
        FrameLayout frame = new FrameLayout(getContext());
        frame.setBackground(PerazimTheme.createBackgroundGradient());

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(createHeaderBar());

        searchBarLayout = createSearchBarLayout();
        searchBarLayout.setVisibility(View.GONE);
        root.addView(searchBarLayout);

        versesScrollView = new ScrollView(getContext());
        versesScrollView.setFillViewport(true);
        versesScrollView.setClipToPadding(false);
        versesScrollView.setPadding(0, 0, 0, dp(96));
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        versesScrollView.setLayoutParams(lpScroll);

        LinearLayout scrollColumn = new LinearLayout(getContext());
        scrollColumn.setOrientation(LinearLayout.VERTICAL);
        scrollColumn.setPadding(dp(16), dp(14), dp(16), dp(16));

        versesContainer = GlassKit.card(getContext(), 28, 0);
        versesContainer.setPadding(dp(20), dp(22), dp(20), dp(22));

        loadingProgress = new ProgressBar(getContext());
        loadingProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpProgress = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpProgress.gravity = Gravity.CENTER_HORIZONTAL;
        lpProgress.setMargins(0, dp(24), 0, dp(24));
        loadingProgress.setLayoutParams(lpProgress);
        versesContainer.addView(loadingProgress);

        tvEmptyState = GlassKit.text(getContext(), "", 13, true, GlassKit.text());
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setPadding(dp(8), dp(16), dp(8), dp(16));
        tvEmptyState.setLineSpacing(0f, 1.3f);
        tvEmptyState.setVisibility(View.GONE);
        versesContainer.addView(tvEmptyState);

        scrollColumn.addView(versesContainer, new LinearLayout.LayoutParams(-1, -2));
        versesScrollView.addView(scrollColumn);
        root.addView(versesScrollView);
        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));

        chapterNavBar = createChapterNavBar();
        FrameLayout.LayoutParams lpNav = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lpNav.setMargins(dp(16), 0, dp(16), dp(24));
        frame.addView(chapterNavBar, lpNav);
        return frame;
    }

    private View createHeaderBar() {
        // Back circle, chapter pill (opens the book picker), KJV pill, search circle
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), 0);

        FrameLayout back = GlassKit.circleIcon(getContext(), org.perazimchurch.app.R.drawable.gl_back, 44, 20, "Back");
        back.setOnClickListener(v -> dismiss());
        btnCloseDialog = back;
        row.addView(back);

        btnBookChapterTitle = GlassKit.text(getContext(), "", 13, true, GlassKit.light() ? Color.parseColor("#2A0F33") : Color.WHITE);
        btnBookChapterTitle.setGravity(Gravity.CENTER);
        btnBookChapterTitle.setMaxLines(1);
        btnBookChapterTitle.setEllipsize(TextUtils.TruncateAt.END);
        btnBookChapterTitle.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(22));
        pill.setColor(GlassKit.light() ? Color.parseColor("#CCFFFFFF") : Color.parseColor("#1FFFFFFF"));
        pill.setStroke(Math.max(1, dp(1)), GlassKit.light() ? Color.parseColor("#F2FFFFFF") : Color.parseColor("#40FFFFFF"));
        btnBookChapterTitle.setBackground(pill);
        btnBookChapterTitle.setClickable(true);
        btnBookChapterTitle.setOnClickListener(v -> showBookSelectorDialog());
        updateHeaderTitle();
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(0, dp(44), 1f);
        lpTitle.setMargins(dp(8), 0, dp(8), 0);
        row.addView(btnBookChapterTitle, lpTitle);

        tvTranslationBadge = GlassKit.glassButton(getContext(), "", 12);
        tvTranslationBadge.setPadding(dp(14), 0, dp(14), 0);
        updateTranslationBadge();
        tvTranslationBadge.setOnClickListener(v -> showTranslationSelectorDialog());
        row.addView(tvTranslationBadge, new LinearLayout.LayoutParams(-2, dp(44)));

        FrameLayout search = GlassKit.circleIcon(getContext(), org.perazimchurch.app.R.drawable.gl_search, 44, 18, "Search the Bible");
        search.setOnClickListener(v -> toggleSearchMode());
        ((LinearLayout.LayoutParams) search.getLayoutParams()).leftMargin = dp(8);
        btnToggleSearch = search;
        row.addView(search);
        return GlassKit.keep(row);
    }

    private LinearLayout createSearchBarLayout() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(dp(16), 0, dp(6), 0);
        layout.setBackground(GlassKit.glass(getContext(), 24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.setMargins(dp(16), dp(12), dp(16), 0);
        layout.setLayoutParams(lp);
        GlassKit.keep(layout);

        etSearchQuery = new EditText(getContext());
        etSearchQuery.setHint("Search the Bible, e.g. Baal-perazim, faith");
        etSearchQuery.setHintTextColor(GlassKit.muted());
        etSearchQuery.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etSearchQuery.setTextColor(GlassKit.text());
        etSearchQuery.setBackground(null);
        etSearchQuery.setSingleLine(true);
        etSearchQuery.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        etSearchQuery.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchQuery.getText().toString().trim());
                hideKeyboard(etSearchQuery);
                return true;
            }
            return false;
        });
        layout.addView(etSearchQuery);

        TextView go = GlassKit.orangeButton(getContext(), "Search", 12);
        go.setPadding(dp(14), 0, dp(14), 0);
        go.setOnClickListener(v -> {
            performSearch(etSearchQuery.getText().toString().trim());
            hideKeyboard(etSearchQuery);
        });
        LinearLayout.LayoutParams lpGo = new LinearLayout.LayoutParams(-2, dp(36));
        lpGo.leftMargin = dp(6);
        layout.addView(go, lpGo);

        TextView clear = GlassKit.text(getContext(), "Close", 12, true, GlassKit.muted());
        clear.setPadding(dp(10), dp(8), dp(10), dp(8));
        clear.setClickable(true);
        clear.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etSearchQuery.getText())) {
                etSearchQuery.setText("");
            } else {
                toggleSearchMode();
            }
        });
        layout.addView(clear);
        return layout;
    }

    private LinearLayout createChapterNavBar() {
        // Previous (glass) · "Chapter 5 of 24" with A− A+ · Next (orange)
        LinearLayout nav = new LinearLayout(getContext());
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setClipChildren(false);
        nav.setClipToPadding(false);
        GlassKit.keep(nav);

        btnPrevChapter = GlassKit.glassButton(getContext(), "Previous", 13);
        btnPrevChapter.setPadding(dp(22), 0, dp(22), 0);
        btnPrevChapter.setOnClickListener(v -> navigateChapter(-1));
        nav.addView(btnPrevChapter, new LinearLayout.LayoutParams(-2, dp(48)));

        LinearLayout middle = new LinearLayout(getContext());
        middle.setOrientation(LinearLayout.VERTICAL);
        middle.setGravity(Gravity.CENTER);
        tvChapterProgress = GlassKit.text(getContext(), "Chapter " + currentChapter + " of " + currentChapterCount, 11, false, GlassKit.muted());
        tvChapterProgress.setGravity(Gravity.CENTER);
        middle.addView(tvChapterProgress);
        LinearLayout sizes = new LinearLayout(getContext());
        sizes.setOrientation(LinearLayout.HORIZONTAL);
        sizes.setGravity(Gravity.CENTER);
        TextView smaller = GlassKit.text(getContext(), "A−", 12, true, GlassKit.muted());
        smaller.setPadding(dp(8), dp(4), dp(8), dp(4));
        smaller.setContentDescription("Smaller text");
        smaller.setClickable(true);
        smaller.setOnClickListener(v -> changeVerseSize(-2f));
        TextView larger = GlassKit.text(getContext(), "A+", 12, true, GlassKit.muted());
        larger.setPadding(dp(8), dp(4), dp(8), dp(4));
        larger.setContentDescription("Larger text");
        larger.setClickable(true);
        larger.setOnClickListener(v -> changeVerseSize(2f));
        sizes.addView(smaller);
        sizes.addView(larger);
        middle.addView(sizes);
        nav.addView(GlassKit.keep(middle), new LinearLayout.LayoutParams(0, -2, 1f));

        btnNextChapter = GlassKit.orangeButton(getContext(), "Next", 13);
        btnNextChapter.setPadding(dp(22), 0, dp(22), 0);
        btnNextChapter.setOnClickListener(v -> navigateChapter(1));
        nav.addView(btnNextChapter, new LinearLayout.LayoutParams(-2, dp(48)));
        return nav;
    }

    private float verseTextSp = 18f;

    private void changeVerseSize(float delta) {
        verseTextSp = Math.max(14f, Math.min(28f, verseTextSp + delta));
        renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
    }

    private void updateHeaderTitle() {
        String title = currentBookName + " " + currentChapter;
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

    private void updateTranslationBadge() {
        if (tvTranslationBadge != null) {
            tvTranslationBadge.setText(currentTranslation);
        }
    }

    private void showTranslationSelectorDialog() {
        final String[] translationIds = {"KJV", "SUV", "NIV", "ESV", "NKJV"};
        final String[] translationLabels = {
                "King James Version (KJV)",
                "Swahili Union Version (SUV)",
                "New International Version (NIV)",
                "English Standard Version (ESV)",
                "New King James Version (NKJV)"
        };

        int selectedIndex = 0;
        for (int i = 0; i < translationIds.length; i++) {
            if (translationIds[i].equalsIgnoreCase(currentTranslation)) {
                selectedIndex = i;
                break;
            }
        }

        AppDialogs.builder(getContext())
                .setTitle("Select Translation / Chagua Tafsiri")
                .setSingleChoiceItems(translationLabels, selectedIndex, (dialog, which) -> {
                    String chosen = translationIds[which];
                    dialog.dismiss();
                    if (!chosen.equalsIgnoreCase(currentTranslation)) {
                        currentTranslation = chosen;
                        updateTranslationBadge();
                        loadChapterVerses(currentBookId, currentChapter);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        bibleViewModel.loadVerses(currentTranslation, bookId, chapter, verses -> {
            loadingProgress.setVisibility(View.GONE);
            if (verses != null && !verses.isEmpty()) {
                currentVerses.clear();
                currentVerses.addAll(verses);
                renderVerseList(currentVerses, false);
            } else {
                currentVerses.clear();
                tvEmptyState.setText("No " + currentTranslation + " text is downloaded for "
                        + currentBookName + " " + currentChapter
                        + ".\n\nKJV starter passages are bundled for offline reading. Other translations require an authorized scripture source.");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
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

        bibleViewModel.searchScripture(currentTranslation, query, results -> {
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
            TextView searchSummary = GlassKit.text(getContext(), "Found " + verses.size() + " verses", 11, true, GlassKit.accent());
            searchSummary.setLetterSpacing(0.06f);
            searchSummary.setPadding(0, 0, 0, dp(12));
            versesContainer.addView(searchSummary);
        }
        for (final BibleVerse verse : verses) {
            versesContainer.addView(createVerseRow(verse, isSearchResult));
        }
        versesScrollView.post(() -> versesScrollView.scrollTo(0, 0));
    }

    private View createVerseRow(final BibleVerse verse, final boolean isSearchResult) {
        // Serif verse, line height 1.7, with the verse number as a small bold orange label (mockup)
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        boolean isCornerstone = "2SA".equals(verse.getBookId()) && verse.getChapterNumber() == 5 && verse.getVerseNumber() == 20;
        if (verse.isFavorite()) {
            GradientDrawable mark = new GradientDrawable();
            mark.setCornerRadius(dp(12));
            mark.setColor(Color.parseColor("#26E17D2F"));
            row.setBackground(mark);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
        }
        GlassKit.keep(row);

        String label = isSearchResult ? formatCitation(verse) : String.valueOf(verse.getVerseNumber());
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        sb.append(label);
        int end = sb.length();
        float numberScale = 11f / verseTextSp;
        sb.setSpan(new android.text.style.RelativeSizeSpan(numberScale), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor(GlassKit.light() ? "#C06119" : "#F8A95A")),
                0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new android.text.style.TypefaceSpan("sans-serif"), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(isSearchResult ? "  " : " ");
        int textStart = sb.length();
        sb.append(verse.getText() != null ? verse.getText() : "");
        if (isCornerstone) {
            sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), textStart, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        TextView tvVerseText = new TextView(getContext());
        tvVerseText.setText(sb);
        tvVerseText.setTextSize(TypedValue.COMPLEX_UNIT_SP, verseTextSp);
        tvVerseText.setTypeface(Typeface.SERIF);
        tvVerseText.setTextColor(GlassKit.text());
        tvVerseText.setLineSpacing(0f, 1.45f);
        GlassKit.keep(tvVerseText);
        row.addView(tvVerseText);

        if (!TextUtils.isEmpty(verse.getNote())) {
            TextView tvNoteContent = GlassKit.text(getContext(), "Note: " + verse.getNote(), 12, false, GlassKit.muted());
            tvNoteContent.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            row.addView(tvNoteContent, GlassKit.matchWrap(getContext(), 6));
        }

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow.setMargins(0, 0, 0, dp(14));
        row.setLayoutParams(lpRow);
        row.setClickable(true);
        row.setContentDescription("Verse " + verse.getVerseNumber() + ". Tap to bookmark, add a note or copy.");
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
        AlertDialog.Builder builder = AppDialogs.builder(getContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundColor(PerazimTheme.COLOR_SURFACE_SOLID);

        // Title
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("📖 " + citation);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_PURPLE_DARK());
        container.addView(tvTitle);

        // Verse Text Snippet
        TextView tvSnippet = new TextView(getContext());
        tvSnippet.setText("“" + verse.getText() + "”");
        tvSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvSnippet.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvSnippet.setTextColor(COLOR_TEXT_MUTED());
        tvSnippet.setLineSpacing(dp(2), 1.15f);
        tvSnippet.setPadding(0, dp(6), 0, dp(14));
        container.addView(tvSnippet);

        builder.setView(container);
        final AlertDialog dialog = builder.create();

        // Action 1: Bookmark / Highlight Toggle
        String bookmarkLabel = verse.isFavorite() ? "⭐ Remove Bookmark" : "☆ Bookmark / Highlight";
        Button btnBookmark = createOptionButton(bookmarkLabel, COLOR_ACCENT_ORANGE(), COLOR_WHITE());
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
        Button btnNote = createOptionButton(noteLabel, COLOR_PRIMARY_PURPLE(), COLOR_WHITE());
        btnNote.setOnClickListener(v -> {
            dialog.dismiss();
            showStudyNoteEditor(verse);
        });
        container.addView(btnNote);

        // Action 3: Copy to Clipboard
        Button btnCopy = createOptionButton("📋 Copy to Clipboard", COLOR_PURPLE_DARK(), COLOR_WHITE());
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Holy Scripture", citation + "\n\"" + verse.getText() + "\" (" + verse.getTranslationId() + ")");
            if (cm != null) {
                cm.setPrimaryClip(clip);
                Toast.makeText(getContext(), "✓ Copied " + citation + " to clipboard!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        container.addView(btnCopy);

        // Action 4: In Search mode, jump to this chapter
        if (isSearchMode) {
            Button btnGoToChapter = createOptionButton("➔ Open Chapter in Reader", COLOR_PRIMARY_PURPLE(), COLOR_WHITE());
            btnGoToChapter.setOnClickListener(v -> {
                dialog.dismiss();
                loadChapterVerses(verse.getBookId(), verse.getChapterNumber());
            });
            container.addView(btnGoToChapter);
        }

        Button btnCancel = createOptionButton("Close", COLOR_BG_NEUTRAL(), COLOR_TEXT_MUTED());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.show();
    }

    private void showStudyNoteEditor(final BibleVerse verse) {
        final String citation = formatCitation(verse);
        AlertDialog.Builder builder = AppDialogs.builder(getContext());
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
        AlertDialog.Builder builder = AppDialogs.builder(getContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        container.setBackgroundColor(PerazimTheme.COLOR_SURFACE_SOLID);

        // Header Title
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("📖 Select Book of the Bible");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        container.addView(tvTitle);

        TextView tvSub = new TextView(getContext());
        tvSub.setText("66-book catalog · KJV starter passages available offline");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
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
        btnTabOt.setTextColor(COLOR_WHITE());
        btnTabOt.setBackground(createPillBg(COLOR_PRIMARY_PURPLE(), COLOR_PRIMARY_PURPLE()));
        LinearLayout.LayoutParams lpOt = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
        lpOt.setMargins(0, 0, dp(4), 0);
        btnTabOt.setLayoutParams(lpOt);
        tabsRow.addView(btnTabOt);

        final Button btnTabNt = new Button(getContext());
        btnTabNt.setText("New Testament (27)");
        btnTabNt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnTabNt.setTypeface(Typeface.DEFAULT_BOLD);
        btnTabNt.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        btnTabNt.setBackground(createPillBg(PerazimTheme.COLOR_SURFACE_2, PerazimTheme.COLOR_STROKE));
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
            btnTabOt.setTextColor(COLOR_WHITE());
            btnTabOt.setBackground(createPillBg(COLOR_PRIMARY_PURPLE(), COLOR_PRIMARY_PURPLE()));
            btnTabNt.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            btnTabNt.setBackground(createPillBg(PerazimTheme.COLOR_SURFACE_2, PerazimTheme.COLOR_STROKE));
            renderBooksOt.run();
        });

        btnTabNt.setOnClickListener(v -> {
            btnTabNt.setTextColor(COLOR_WHITE());
            btnTabNt.setBackground(createPillBg(COLOR_PRIMARY_PURPLE(), COLOR_PRIMARY_PURPLE()));
            btnTabOt.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            btnTabOt.setBackground(createPillBg(PerazimTheme.COLOR_SURFACE_2, PerazimTheme.COLOR_STROKE));
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
            bg.setColor(isCurrent ? PerazimTheme.COLOR_SURFACE_2 : PerazimTheme.COLOR_SURFACE);
            bg.setStroke(dp(1), isCurrent ? COLOR_PRIMARY_PURPLE() : PerazimTheme.COLOR_STROKE);
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
            tvBookName.setTextColor(isCurrent ? COLOR_ACCENT_ORANGE() : PerazimTheme.COLOR_TEXT_PRIMARY);
            LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tvBookName.setLayoutParams(lpName);
            itemRow.addView(tvBookName);

            TextView tvChapters = new TextView(getContext());
            tvChapters.setText(book.getChapterCount() + " ch");
            tvChapters.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvChapters.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            itemRow.addView(tvChapters);

            itemRow.setOnClickListener(v -> {
                parentDialog.dismiss();
                showChapterSelectorGrid(book);
            });

            container.addView(itemRow);
        }
    }

    private void showChapterSelectorGrid(final BibleBook book) {
        AlertDialog.Builder builder = AppDialogs.builder(getContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        container.setBackgroundColor(PerazimTheme.COLOR_SURFACE_SOLID);

        // Header Title
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("📖 " + book.getName() + " · Select Chapter");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        container.addView(tvTitle);

        TextView tvSub = new TextView(getContext());
        tvSub.setText("Select from 1 to " + book.getChapterCount() + " chapters");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
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
            btnCh.setTextColor(COLOR_WHITE());

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(isCurrent ? COLOR_PRIMARY_PURPLE() : PerazimTheme.COLOR_SURFACE_2);
            btnBg.setStroke(dp(1), isCurrent ? COLOR_PRIMARY_PURPLE() : PerazimTheme.COLOR_STROKE);
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

    @Nullable
    private Bitmap loadAssetBitmap(String name, int maxDim) {
        try {
            java.io.InputStream is = getContext().getAssets().open(name);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int sample = 1;
            int max = Math.max(opts.outWidth, opts.outHeight);
            while (max / sample > maxDim) sample *= 2;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            is = getContext().getAssets().open(name);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
