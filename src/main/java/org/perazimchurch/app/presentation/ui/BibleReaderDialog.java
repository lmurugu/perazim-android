package org.perazimchurch.app.presentation.ui;

import org.perazimchurch.app.ui.theme.AppDialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.ScriptureReference;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.data.local.seeder.BibleDataSeeder;
import org.perazimchurch.app.presentation.bible.HighlightColors;
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

    /**
     * Android Back closes the reader. In search mode it first leaves search, as the in-app Back control does, and
     * hides the keyboard. Without this the key was not handled, and the reader stayed open.
     */
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            if (actionBarView != null) {
                dismissActionBar();
            } else if (isSearchMode) {
                hideKeyboard(etSearchQuery);
                toggleSearchMode();
            } else {
                dismiss();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    private FrameLayout readerFrame;
    private View actionBarView;
    private BibleVerse actionVerse;
    private int focusedVerseNumber = 0;

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
        readerFrame = frame;

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
        if (!GlassKit.light()) {
            GradientDrawable deepPurple = new GradientDrawable();
            deepPurple.setCornerRadius(dp(28));
            deepPurple.setColor(Color.parseColor("#22082F"));
            versesContainer.setBackground(deepPurple);
        }
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
        etSearchQuery.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                performSearch(etSearchQuery.getText().toString().trim());
                hideKeyboard(etSearchQuery);
                return true;
            }
            return false;
        });
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

        TextView notesButton = GlassKit.text(getContext(), "Notes", 12, true, GlassKit.accent());
        notesButton.setPadding(dp(10), dp(8), dp(10), dp(8));
        notesButton.setClickable(true);
        notesButton.setOnClickListener(v -> showNotesDialog());
        layout.addView(notesButton);

        TextView topics = GlassKit.text(getContext(), "Topics", 12, true, GlassKit.accent());
        topics.setPadding(dp(10), dp(8), dp(10), dp(8));
        topics.setClickable(true);
        topics.setOnClickListener(v -> showTopicsDialog());
        layout.addView(topics);

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

        btnNextChapter = GlassKit.glassButton(getContext(), "Next", 13);
        btnNextChapter.setPadding(dp(22), 0, dp(22), 0);
        attachGlow(btnNextChapter);
        attachGlow(btnPrevChapter);
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
        bibleViewModel.loadReadableTranslations(translations -> {
            if (translations == null || translations.isEmpty()) {
                Toast.makeText(getContext(), "No translation text is available offline.", Toast.LENGTH_SHORT).show();
                return;
            }
            rememberTranslationNames(translations);
            final List<String> ids = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int selectedIndex = 0;
            for (BibleTranslation t : translations) {
                if (t.getId() == null) {
                    continue;
                }
                if (t.getId().equalsIgnoreCase(currentTranslation)) {
                    selectedIndex = ids.size();
                }
                ids.add(t.getId());
                labels.add(t.getId() + " · " + (t.getName() != null ? t.getName() : t.getId()));
            }
            AppDialogs.builder(getContext())
                    .setTitle("Select Translation / Chagua Tafsiri")
                    .setSingleChoiceItems(labels.toArray(new String[0]), selectedIndex, (dialog, which) -> {
                        String chosen = ids.get(which);
                        dialog.dismiss();
                        if (!chosen.equalsIgnoreCase(currentTranslation)) {
                            currentTranslation = chosen;
                            updateTranslationBadge();
                            loadChapterVerses(currentBookId, currentChapter);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private final Map<String, String> translationNames = new HashMap<>();

    private void rememberTranslationNames(List<BibleTranslation> translations) {
        for (BibleTranslation t : translations) {
            if (t.getId() != null && t.getName() != null) {
                translationNames.put(t.getId(), t.getName());
            }
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
        dismissActionBar();
        freshRender = true;
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
        dismissActionBar();
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

    private String lastQuery = "";

    private void performSearch(String query) {
        lastQuery = query;
        freshRender = true;
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(getContext(), "Please enter a keyword to search", Toast.LENGTH_SHORT).show();
            return;
        }

        // A typed reference such as "John 3:16" opens the passage directly, offline.
        ScriptureReference reference = ScriptureReference.parse(query);
        if (reference != null) {
            pendingScrollVerse = reference.namesVerse() ? reference.verseStart : 0;
            loadChapterVerses(BibleMapper.getBookIdFromNumber(reference.bookNumber), reference.chapter);
            hideKeyboard(etSearchQuery);
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

    private final Map<Integer, View> verseRows = new HashMap<>();
    private int pendingScrollVerse = 0;

    /** Pressed state: the button glows orange only while it is held down. */
    private void attachGlow(final TextView button) {
        final android.graphics.drawable.Drawable rest = button.getBackground();
        final int restText = button.getCurrentTextColor();
        button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    GradientDrawable glow = new GradientDrawable();
                    glow.setCornerRadius(dp(14));
                    glow.setColor(COLOR_ACCENT_ORANGE());
                    button.setBackground(glow);
                    button.setTextColor(COLOR_WHITE());
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    button.setBackground(rest);
                    button.setTextColor(restText);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private void clearVerseViews() {
        verseRows.clear();
        for (int i = versesContainer.getChildCount() - 1; i >= 0; i--) {
            View child = versesContainer.getChildAt(i);
            if (child != loadingProgress && child != tvEmptyState) {
                versesContainer.removeViewAt(i);
            }
        }
    }

    /** True when the next render shows new content (chapter, search or topic), not an in-place update. */
    private boolean freshRender = false;

    private void renderVerseList(@NonNull List<BibleVerse> verses, boolean isSearchResult) {
        boolean fresh = freshRender;
        freshRender = false;
        final int keepScrollY = versesScrollView != null ? versesScrollView.getScrollY() : 0;
        clearVerseViews();
        if (isSearchResult) {
            TextView searchSummary = GlassKit.text(getContext(), "Found " + verses.size() + " verses", 11, true, GlassKit.accent());
            searchSummary.setLetterSpacing(0.06f);
            searchSummary.setPadding(0, 0, 0, dp(12));
            versesContainer.addView(searchSummary);
        }
        if (isSearchResult && !verses.isEmpty()) {
            TextView saveAll = GlassKit.orangeButton(getContext(), "Save all to a topic", 12);
            saveAll.setPadding(dp(14), 0, dp(14), 0);
            saveAll.setOnClickListener(v -> showSaveAllToTopic(new ArrayList<>(verses)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(36));
            lp.setMargins(0, 0, 0, dp(12));
            versesContainer.addView(saveAll, lp);
        }
        for (final BibleVerse verse : verses) {
            versesContainer.addView(createVerseRow(verse, isSearchResult));
        }
        if (!fresh) {
            // In-place update (highlight, bookmark, focus): keep the reader where it was, no animation.
            versesScrollView.post(() -> versesScrollView.scrollTo(0, keepScrollY));
            return;
        }
        for (int i = 0; i < versesContainer.getChildCount(); i++) {
            View child = versesContainer.getChildAt(i);
            if (child == loadingProgress || child == tvEmptyState) {
                continue;
            }
            child.setTranslationY(-dp(18));
            child.animate().translationY(0f).setStartDelay(Math.min(i, 14) * 25L).setDuration(320).start();
        }
        final View target = (!isSearchResult && pendingScrollVerse > 0) ? verseRows.get(pendingScrollVerse) : null;
        pendingScrollVerse = 0;
        if (target != null) {
            // Wait for layout so the verse's position is known before scrolling to it.
            versesScrollView.postDelayed(() -> versesScrollView.scrollTo(0, offsetWithin(target, versesScrollView)), 120);
        } else {
            versesScrollView.post(() -> versesScrollView.scrollTo(0, 0));
        }
    }


    private View createVerseRow(final BibleVerse verse, final boolean isSearchResult) {
        // Serif verse, line height 1.7, with the verse number as a small bold orange label (mockup)
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        boolean isCornerstone = "2SA".equals(verse.getBookId()) && verse.getChapterNumber() == 5 && verse.getVerseNumber() == 20;
        HighlightColors.Colour highlight = HighlightColors.byName(verse.getHighlightColor());
        if (highlight != null) {
            GradientDrawable mark = new GradientDrawable();
            mark.setCornerRadius(dp(12));
            mark.setColor(Color.parseColor(highlight.tint));
            row.setBackground(mark);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
        } else if (verse.isFavorite()) {
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
        row.setContentDescription("Verse " + verse.getVerseNumber() + ". Tap to highlight, bookmark, add a note, copy, share or compare.");
        if (!isSearchResult) {
            verseRows.put(verse.getVerseNumber(), row);
            if (focusedVerseNumber > 0 && verse.getVerseNumber() != focusedVerseNumber) {
                row.setAlpha(0.35f);
            }
        }
        row.setOnClickListener(v -> showVerseActionBar(verse));
        return row;
    }

    /** Vertical position of a view inside an ancestor scroll view. */
    private static int offsetWithin(View target, ViewGroup ancestor) {
        int y = 0;
        View current = target;
        while (current != null && current.getParent() != null && current.getParent() != ancestor) {
            y += current.getTop();
            current = (View) current.getParent();
        }
        if (current != null) {
            y += current.getTop();
        }
        return Math.max(0, y - ancestor.getPaddingTop());
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

    /**
     * Floating action bar over the text: no card, icons float at the bottom. Swipe up to reveal
     * the second row; every button works on its own.
     */
    private void showVerseActionBar(final BibleVerse verse) {
        dismissActionBar();
        actionVerse = verse;
        focusedVerseNumber = verse.getVerseNumber();
        renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
        if (readerFrame == null) {
            return;
        }
        final String citation = formatCitation(verse);
        final boolean newTestament = BibleMapper.resolveBookNumber(verse.getBookId()) >= 40;

        final SwipeUpBar bar = new SwipeUpBar(getContext());
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setGravity(Gravity.CENTER_HORIZONTAL);

        // Colour palette: major colours, muted versions, spectrum and clear.
        final LinearLayout palette = new LinearLayout(getContext());
        palette.setOrientation(LinearLayout.VERTICAL);
        palette.setGravity(Gravity.CENTER_HORIZONTAL);
        palette.setVisibility(View.GONE);
        LinearLayout majorRow = new LinearLayout(getContext());
        majorRow.setGravity(Gravity.CENTER);
        for (HighlightColors.Colour colour : HighlightColors.ALL) {
            majorRow.addView(swatchView(colour.swatch, colour.name.equals(verse.getHighlightColor()),
                    v -> applyHighlight(verse, colour.name)));
        }
        LinearLayout mutedRow = new LinearLayout(getContext());
        mutedRow.setGravity(Gravity.CENTER);
        for (HighlightColors.Colour colour : HighlightColors.MUTED) {
            mutedRow.addView(swatchView(colour.swatch, colour.name.equals(verse.getHighlightColor()),
                    v -> applyHighlight(verse, colour.name)));
        }
        mutedRow.addView(barIcon("\u2715", "Remove highlight", v -> applyHighlight(verse, null)));
        palette.addView(majorRow);
        palette.addView(mutedRow);
        palette.addView(spectrumView(verse));
        bar.addView(palette, GlassKit.matchWrap(getContext(), 6));

        LinearLayout primary = barRow();
        primary.addView(barIcon("\u25cf", "Highlight", v -> palette.setVisibility(palette.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)));
        primary.addView(barIcon(verse.isFavorite() ? "\u2605" : "\u2606", "Bookmark", v -> {
            boolean nextState = !verse.isFavorite();
            verse.setFavorite(nextState);
            bibleViewModel.toggleBookmark(verse.getId(), nextState, () ->
                    Toast.makeText(getContext(), nextState ? "Bookmarked" : "Bookmark removed", Toast.LENGTH_SHORT).show());
            renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
        }));
        primary.addView(barIcon("\u270e", "Note", v -> {
            dismissActionBar();
            showStudyNoteEditor(verse);
        }));
        primary.addView(barIcon("\u2398", "Copy", v -> copyVerse(verse, citation)));
        primary.addView(barIcon("\u2197", "Share", v -> shareVerse(verse, citation)));
        bar.addView(primary);

        final LinearLayout secondary = barRow();
        secondary.setVisibility(View.GONE);
        secondary.addView(barIcon("\u21c4", "Compare", v -> showCompareDialog(verse, citation)));
        if (newTestament) {
            secondary.addView(barIcon("\u0391\u03b2", "Interlinear", v -> showInterlinearDialog(verse, citation)));
        }
        secondary.addView(barIcon("#", "Topic", v -> showTopicPicker(verse)));
        if (isSearchMode) {
            secondary.addView(barIcon("\u27a4", "Open chapter", v -> {
                dismissActionBar();
                loadChapterVerses(verse.getBookId(), verse.getChapterNumber());
            }));
        }
        secondary.addView(barIcon("\u2715", "Close", v -> dismissActionBar()));
        bar.addView(secondary);

        bar.setOnSwipeUp(() -> secondary.setVisibility(View.VISIBLE));
        bar.setOnSwipeDown(() -> secondary.setVisibility(View.GONE));
        TextView hint = GlassKit.text(getContext(), "swipe up for more", 10, false, COLOR_TEXT_MUTED());
        hint.setGravity(Gravity.CENTER);
        bar.addView(hint);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.setMargins(dp(12), 0, dp(12), dp(84));
        readerFrame.addView(bar, lp);
        actionBarView = bar;
    }

    private void dismissActionBar() {
        if (actionBarView != null && readerFrame != null) {
            readerFrame.removeView(actionBarView);
        }
        actionBarView = null;
        if (focusedVerseNumber != 0) {
            focusedVerseNumber = 0;
            renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
        }
    }

    private LinearLayout barRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    /** Circular floating icon with a label for screen readers. */
    private TextView barIcon(String glyph, String label, View.OnClickListener onClick) {
        TextView icon = new TextView(getContext());
        icon.setText(glyph);
        icon.setGravity(Gravity.CENTER);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        icon.setTextColor(GlassKit.text());
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(GlassKit.light() ? Color.parseColor("#F2FFFFFF") : Color.parseColor("#D9201830"));
        circle.setStroke(dp(1), Color.parseColor("#33000000"));
        icon.setBackground(circle);
        icon.setContentDescription(label);
        icon.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        lp.setMargins(dp(5), dp(6), dp(5), dp(6));
        icon.setLayoutParams(lp);
        return icon;
    }

    private View swatchView(String hex, boolean active, View.OnClickListener onClick) {
        View swatch = new View(getContext());
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(Color.parseColor(hex));
        dot.setStroke(active ? dp(3) : dp(1), active ? COLOR_PURPLE_DARK() : Color.parseColor("#33000000"));
        swatch.setBackground(dot);
        swatch.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        swatch.setLayoutParams(lp);
        return swatch;
    }

    /** RGB spectrum: drag or tap across the hue bar to pick any colour. */
    private View spectrumView(final BibleVerse verse) {
        final View spectrum = new View(getContext());
        GradientDrawable hues = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{
                0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000});
        hues.setCornerRadius(dp(10));
        spectrum.setBackground(hues);
        spectrum.setContentDescription("Pick any colour from the spectrum");
        spectrum.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_MOVE
                    || action == android.view.MotionEvent.ACTION_UP) {
                float frac = Math.max(0f, Math.min(1f, event.getX() / Math.max(1, v.getWidth())));
                int rgb = Color.HSVToColor(new float[]{frac * 359f, 0.55f, 0.98f});
                String hex = String.format(java.util.Locale.US, "#%06X", rgb & 0xFFFFFF);
                if (action == android.view.MotionEvent.ACTION_UP) {
                    applyHighlight(verse, hex);
                }
            }
            return true;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(260), dp(20));
        lp.setMargins(0, dp(8), 0, dp(4));
        spectrum.setLayoutParams(lp);
        return spectrum;
    }

    /** Stays in front of the verses; swipe up reveals more actions, swipe down hides them. */
    private static final class SwipeUpBar extends LinearLayout {
        private float downY;
        private boolean swiping;
        private Runnable onUp;
        private Runnable onDown;

        SwipeUpBar(android.content.Context context) {
            super(context);
        }

        void setOnSwipeUp(Runnable r) { onUp = r; }
        void setOnSwipeDown(Runnable r) { onDown = r; }

        @Override
        public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
            float slop = getResources().getDisplayMetrics().density * 24;
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downY = ev.getY();
                    swiping = false;
                    return false;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (Math.abs(ev.getY() - downY) > slop) {
                        swiping = true;
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent ev) {
            if (!swiping) {
                return false;
            }
            if (ev.getActionMasked() == android.view.MotionEvent.ACTION_UP
                    || ev.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                if (ev.getY() < downY && onUp != null) {
                    onUp.run();
                } else if (ev.getY() > downY && onDown != null) {
                    onDown.run();
                }
                swiping = false;
            }
            return true;
        }
    }

    private void copyVerse(BibleVerse verse, String citation) {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Holy Scripture", verseShareText(verse, citation));
        if (cm != null) {
            cm.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Copied " + citation, Toast.LENGTH_SHORT).show();
        }
    }

    /** Sets or clears a verse highlight, updates the loaded verse lists and redraws. */
    private void applyHighlight(final BibleVerse verse, @Nullable final String colour) {
        final String previous = verse.getHighlightColor();
        verse.setHighlightColor(colour);
        showUndoPill(verse, previous, colour);
        if (actionVerse != null) {
            dismissActionBar();
        }
        for (BibleVerse loaded : currentVerses) {
            if (loaded.getVerseNumber() == verse.getVerseNumber() && loaded.getChapterNumber() == verse.getChapterNumber()) {
                loaded.setHighlightColor(colour);
            }
        }
        bibleViewModel.setHighlight(verse.getBookId(), verse.getChapterNumber(), verse.getVerseNumber(), colour, () ->
                Toast.makeText(getContext(), colour == null ? "Highlight removed" : "Highlighted " + colour, Toast.LENGTH_SHORT).show());
        renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
    }

    /** Brief "Undo" pill above the controls; restores the verse's previous highlight. */
    private void showUndoPill(final BibleVerse verse, @Nullable final String previous, @Nullable final String applied) {
        if (readerFrame == null || java.util.Objects.equals(previous, applied)) {
            return;
        }
        if (undoPillView != null) {
            readerFrame.removeView(undoPillView);
        }
        TextView pill = GlassKit.text(getContext(), "Undo highlight", 12, true, GlassKit.text());
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(dp(20));
        glass.setColor(GlassKit.light() ? Color.parseColor("#F2FFFFFF") : Color.parseColor("#E6221431"));
        glass.setStroke(dp(1), Color.parseColor("#40FFFFFF"));
        pill.setBackground(glass);
        pill.setOnClickListener(v -> {
            readerFrame.removeView(pill);
            undoPillView = null;
            applyHighlight(verse, previous);
        });
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.setMargins(0, 0, 0, dp(140));
        readerFrame.addView(pill, lp);
        undoPillView = pill;
        pill.postDelayed(() -> {
            if (undoPillView == pill) {
                readerFrame.removeView(pill);
                undoPillView = null;
            }
        }, 5000);
    }

    private View undoPillView;

    /** The text copied or shared for a verse: reference, verse text and translation id. */
    private String verseShareText(BibleVerse verse, String citation) {
        return citation + " (" + verse.getTranslationId() + ")\n\u201c" + verse.getText() + "\u201d";
    }

    private void shareVerse(BibleVerse verse, String citation) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, citation);
        send.putExtra(Intent.EXTRA_TEXT, verseShareText(verse, citation));
        try {
            getContext().startActivity(Intent.createChooser(send, "Share verse"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "No app available to share this verse.", Toast.LENGTH_SHORT).show();
        }
    }

    /** Word-by-word Greek with Strong's numbers and morphology for a New Testament verse. */
    private void showInterlinearDialog(final BibleVerse verse, final String citation) {
        bibleViewModel.loadGreekInterlinear(verse.getBookId(), verse.getChapterNumber(), verse.getVerseNumber(), words -> {
            if (words == null || words.isEmpty()) {
                Toast.makeText(getContext(), "No interlinear is available for this verse.", Toast.LENGTH_SHORT).show();
                return;
            }
            LinearLayout box = new LinearLayout(getContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(18), dp(8), dp(18), dp(12));
            for (org.perazimchurch.app.domain.model.InterlinearWord word : words) {
                LinearLayout cell = new LinearLayout(getContext());
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setPadding(0, dp(6), 0, dp(6));
                if (word.getStrongsNumber() > 0) {
                    final int strongs = word.getStrongsNumber();
                    cell.setClickable(true);
                    cell.setContentDescription("Strong's G" + strongs + " for " + word.getOriginal());
                    cell.setOnClickListener(v -> showStrongsDialog("G" + strongs, strongs));
                }
                TextView greek = new TextView(getContext());
                greek.setText(word.getOriginal());
                greek.setTypeface(Typeface.SERIF);
                greek.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                greek.setTextColor(GlassKit.text());
                cell.addView(greek);
                TextView meta = new TextView(getContext());
                String strong = word.getStrongsNumber() > 0 ? "G" + word.getStrongsNumber() : "";
                meta.setText((strong + (strong.isEmpty() || word.getMorphology().isEmpty() ? "" : " \u00b7 ") + word.getMorphology()).trim());
                meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                meta.setTextColor(COLOR_TEXT_MUTED());
                cell.addView(meta);
                box.addView(cell);
            }
            TextView note = new TextView(getContext());
            note.setText("Robinson-Pierpont style parsing from the Antoniades 1904/1912 text (public domain). Strong's numbers identify the lemma.");
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            note.setTextColor(COLOR_TEXT_MUTED());
            note.setPadding(0, dp(10), 0, 0);
            box.addView(note);
            ScrollView scroll = new ScrollView(getContext());
            scroll.addView(box);
            AppDialogs.builder(getContext())
                    .setTitle(citation + " · Greek Interlinear")
                    .setView(scroll)
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    /** Strong's dictionary entry and the New Testament occurrences of the same number. */
    private void showStrongsDialog(final String code, final int strongsNumber) {
        bibleViewModel.loadStrongsDetail(code, strongsNumber, detail -> {
            LinearLayout box = new LinearLayout(getContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(18), dp(8), dp(18), dp(12));
            TextView def = new TextView(getContext());
            if (detail.entry != null) {
                def.setText((detail.entry.getOriginal().isEmpty() ? "" : detail.entry.getOriginal() + "  ")
                        + detail.entry.getTransliteration() + "\n\n" + detail.entry.getDefinition());
            } else {
                def.setText("No dictionary entry for " + code + " in the bundled Strong's dictionary.");
            }
            def.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            def.setTextColor(GlassKit.text());
            def.setLineSpacing(0f, 1.3f);
            box.addView(def);
            TextView head = new TextView(getContext());
            head.setText("NEW TESTAMENT OCCURRENCES (" + detail.occurrences.size() + ")");
            head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            head.setLetterSpacing(0.08f);
            head.setTypeface(Typeface.DEFAULT_BOLD);
            head.setTextColor(COLOR_PRIMARY_PURPLE());
            head.setPadding(0, dp(16), 0, dp(4));
            box.addView(head);
            StringBuilder refs = new StringBuilder();
            for (org.perazimchurch.app.domain.model.VerseLocation loc : detail.occurrences) {
                if (refs.length() > 0) refs.append(", ");
                refs.append(BibleDataSeeder.canonicalBookName(loc.bookNumber)).append(' ').append(loc.chapter).append(':').append(loc.verse);
            }
            TextView list = new TextView(getContext());
            list.setText(refs.length() == 0 ? "None" : refs.toString());
            list.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            list.setTextColor(COLOR_TEXT_MUTED());
            list.setLineSpacing(0f, 1.3f);
            box.addView(list);
            ScrollView scroll = new ScrollView(getContext());
            scroll.addView(box);
            AppDialogs.builder(getContext())
                    .setTitle("Strong's " + code)
                    .setView(scroll)
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    /** Study notes: list, open to edit, and tap a linked verse to jump to it. */
    private void showNotesDialog() {
        bibleViewModel.loadNotes(notes -> {
            LinearLayout box = new LinearLayout(getContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(18), dp(8), dp(18), dp(12));
            TextView newNote = GlassKit.orangeButton(getContext(), "New note", 12);
            newNote.setOnClickListener(v -> showNoteEditor(null));
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-2, dp(36));
            nlp.setMargins(0, 0, 0, dp(12));
            box.addView(newNote, nlp);
            if (notes.isEmpty()) {
                TextView empty = GlassKit.text(getContext(), "No notes yet. Write a note and link the verses it talks about.", 13, false, COLOR_TEXT_MUTED());
                box.addView(empty);
            }
            for (final org.perazimchurch.app.domain.model.BibleNote note : notes) {
                TextView body = GlassKit.text(getContext(), note.getBody().length() > 140
                        ? note.getBody().substring(0, 140) + "..." : note.getBody(), 14, false, GlassKit.text());
                body.setOnClickListener(v -> showNoteEditor(note));
                body.setPadding(0, dp(10), 0, dp(4));
                box.addView(body);
                LinearLayout links = new LinearLayout(getContext());
                links.setOrientation(LinearLayout.HORIZONTAL);
                for (final org.perazimchurch.app.domain.model.VerseLocation loc : note.getLinks()) {
                    TextView chip = GlassKit.text(getContext(), BibleDataSeeder.canonicalBookName(loc.bookNumber)
                            + " " + loc.chapter + ":" + loc.verse, 12, true, GlassKit.accent());
                    chip.setPadding(dp(8), dp(4), dp(8), dp(4));
                    chip.setClickable(true);
                    chip.setOnClickListener(v -> jumpToVerse(loc));
                    links.addView(chip);
                }
                box.addView(links);
            }
            ScrollView scroll = new ScrollView(getContext());
            scroll.addView(box);
            AppDialogs.builder(getContext())
                    .setTitle("Notes")
                    .setView(scroll)
                    .setNegativeButton("Close", null)
                    .show();
        });
    }

    /** Opens a verse from a note link: loads its chapter and scrolls to the verse. */
    private void jumpToVerse(org.perazimchurch.app.domain.model.VerseLocation loc) {
        pendingScrollVerse = loc.verse;
        loadChapterVerses(BibleMapper.getBookIdFromNumber(loc.bookNumber), loc.chapter);
    }

    /** Editor for one note: text, linked verses (added by reference, e.g. "John 3:16") and save or delete. */
    private void showNoteEditor(@Nullable final org.perazimchurch.app.domain.model.BibleNote existing) {
        final List<org.perazimchurch.app.domain.model.VerseLocation> links = new ArrayList<>();
        if (existing != null) {
            links.addAll(existing.getLinks());
        }
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(8));
        final EditText body = new EditText(getContext());
        body.setHint("Write your note");
        body.setMinLines(4);
        body.setGravity(Gravity.TOP);
        if (existing != null) {
            body.setText(existing.getBody());
        }
        box.addView(body);
        final LinearLayout linkBox = new LinearLayout(getContext());
        linkBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(linkBox);
        final Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            linkBox.removeAllViews();
            for (final org.perazimchurch.app.domain.model.VerseLocation loc : links) {
                TextView row = GlassKit.text(getContext(), "Linked: " + BibleDataSeeder.canonicalBookName(loc.bookNumber)
                        + " " + loc.chapter + ":" + loc.verse + "   (tap to remove)", 12, false, GlassKit.accent());
                row.setPadding(0, dp(4), 0, dp(4));
                row.setOnClickListener(v -> {
                    links.remove(loc);
                    redraw[0].run();
                });
                linkBox.addView(row);
            }
        };
        redraw[0].run();
        final EditText reference = new EditText(getContext());
        reference.setHint("Link a verse, e.g. John 3:16");
        reference.setSingleLine(true);
        box.addView(reference);
        AppDialogs.builder(getContext())
                .setTitle(existing == null ? "New note" : "Edit note")
                .setView(box)
                .setPositiveButton("Save", (dialog, which) -> {
                    String ref = reference.getText().toString().trim();
                    ScriptureReference parsed = ScriptureReference.parse(ref);
                    if (!ref.isEmpty() && parsed != null && parsed.namesVerse()) {
                        links.add(new org.perazimchurch.app.domain.model.VerseLocation(parsed.bookNumber, parsed.chapter, parsed.verseStart));
                    }
                    bibleViewModel.saveNote(existing == null ? 0L : existing.getId(), body.getText().toString(), links,
                            id -> Toast.makeText(getContext(), "Note saved", Toast.LENGTH_SHORT).show());
                })
                .setNeutralButton(existing == null ? null : "Delete", existing == null ? null : (dialog, which) ->
                        bibleViewModel.deleteNote(existing.getId(), () -> Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Lists topics; choosing one shows its saved verses in the search list. */
    private void showTopicsDialog() {
        bibleViewModel.loadTopics(topics -> {
            if (topics == null || topics.isEmpty()) {
                Toast.makeText(getContext(), "No topics yet. Save verses to a topic from a verse or from search results.", Toast.LENGTH_LONG).show();
                return;
            }
            AppDialogs.builder(getContext())
                    .setTitle("Topics")
                    .setItems(topics.toArray(new String[0]), (dialog, which) -> showTopicVerses(topics.get(which)))
                    .setNegativeButton("Close", null)
                    .show();
        });
    }

    private void showTopicVerses(final String topic) {
        freshRender = true;
        bibleViewModel.loadTopicVerses(topic, verses -> {
            searchResults.clear();
            searchResults.addAll(verses);
            isSearchMode = true;
            if (searchBarLayout != null) {
                searchBarLayout.setVisibility(View.VISIBLE);
            }
            if (chapterNavBar != null) {
                chapterNavBar.setVisibility(View.GONE);
            }
            if (etSearchQuery != null) {
                etSearchQuery.setText(topic);
            }
            renderVerseList(searchResults, true);
            if (verses.isEmpty()) {
                Toast.makeText(getContext(), "No verses saved under " + topic + " yet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Checkbox list of topics for one verse, plus a box for a new topic. */
    private void showTopicPicker(final BibleVerse verse) {
        final String citation = formatCitation(verse);
        bibleViewModel.loadTopics(allTopics -> bibleViewModel.loadTopicsForVerse(verse.getBookId(),
                verse.getChapterNumber(), verse.getVerseNumber(), memberOf -> {
                    final List<String> existing = new ArrayList<>(allTopics);
                    final List<String> originallyIn = new ArrayList<>(memberOf);
                    final List<android.widget.CheckBox> boxes = new ArrayList<>();
                    LinearLayout box = new LinearLayout(getContext());
                    box.setOrientation(LinearLayout.VERTICAL);
                    box.setPadding(dp(18), dp(8), dp(18), dp(8));
                    for (String topic : existing) {
                        android.widget.CheckBox cb = new android.widget.CheckBox(getContext());
                        cb.setText(topic);
                        cb.setTextColor(GlassKit.text());
                        cb.setChecked(originallyIn.contains(topic));
                        cb.setTag(topic);
                        boxes.add(cb);
                        box.addView(cb);
                    }
                    final EditText newTopic = new EditText(getContext());
                    newTopic.setHint("New topic, for example healing");
                    newTopic.setSingleLine(true);
                    box.addView(newTopic);
                    ScrollView scroll = new ScrollView(getContext());
                    scroll.addView(box);
                    AppDialogs.builder(getContext())
                            .setTitle("Topics for " + citation)
                            .setView(scroll)
                            .setPositiveButton("Save", (dialog, which) -> {
                                for (android.widget.CheckBox cb : boxes) {
                                    String topic = (String) cb.getTag();
                                    boolean nowIn = cb.isChecked();
                                    if (nowIn != originallyIn.contains(topic)) {
                                        bibleViewModel.setVerseTopic(verse.getBookId(), verse.getChapterNumber(),
                                                verse.getVerseNumber(), topic, nowIn, null);
                                    }
                                }
                                String name = newTopic.getText().toString().trim();
                                if (!name.isEmpty()) {
                                    bibleViewModel.setVerseTopic(verse.getBookId(), verse.getChapterNumber(),
                                            verse.getVerseNumber(), name, true, () ->
                                                    Toast.makeText(getContext(), "Saved under " + name, Toast.LENGTH_SHORT).show());
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }));
    }

    /** Saves every verse of the current search results under one topic label. */
    private void showSaveAllToTopic(final List<BibleVerse> verses) {
        final EditText name = new EditText(getContext());
        name.setSingleLine(true);
        name.setText(lastQuery);
        name.setHint("Topic name, for example healing");
        AppDialogs.builder(getContext())
                .setTitle("Save " + verses.size() + " verses to a topic")
                .setView(name)
                .setPositiveButton("Save", (dialog, which) -> {
                    final String topic = name.getText().toString().trim();
                    if (topic.isEmpty()) {
                        return;
                    }
                    for (BibleVerse verse : verses) {
                        bibleViewModel.setVerseTopic(verse.getBookId(), verse.getChapterNumber(),
                                verse.getVerseNumber(), topic, true, null);
                    }
                    Toast.makeText(getContext(), "Saved " + verses.size() + " verses under " + topic, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Shows the same verse in every readable translation, one under the other. */
    private void showCompareDialog(final BibleVerse verse, final String citation) {
        bibleViewModel.loadReadableTranslations(translations -> {
            if (translations != null) {
                rememberTranslationNames(translations);
            }
            bibleViewModel.loadVerseInAllTranslations(verse.getBookId(), verse.getChapterNumber(), verse.getVerseNumber(), versions -> {
                if (versions == null || versions.isEmpty()) {
                    Toast.makeText(getContext(), "No other translations are available for this verse.", Toast.LENGTH_SHORT).show();
                    return;
                }
                LinearLayout box = new LinearLayout(getContext());
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(dp(18), dp(8), dp(18), dp(12));
                for (BibleVerse version : versions) {
                    String name = translationNames.get(version.getTranslationId());
                    TextView head = new TextView(getContext());
                    head.setText(version.getTranslationId() + (name != null ? " · " + name : ""));
                    head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                    head.setLetterSpacing(0.08f);
                    head.setTypeface(Typeface.DEFAULT_BOLD);
                    head.setTextColor(COLOR_PRIMARY_PURPLE());
                    head.setPadding(0, dp(12), 0, dp(4));
                    box.addView(head);
                    TextView body = new TextView(getContext());
                    body.setText(version.getText());
                    body.setTypeface(Typeface.SERIF);
                    body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    body.setTextColor(GlassKit.text());
                    body.setLineSpacing(0f, 1.35f);
                    box.addView(body);
                }
                ScrollView scroll = new ScrollView(getContext());
                scroll.addView(box);
                AppDialogs.builder(getContext())
                        .setTitle(citation + " · Compare")
                        .setView(scroll)
                        .setPositiveButton("Close", null)
                        .show();
            });
        });
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
