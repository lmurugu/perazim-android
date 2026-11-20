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
import org.perazimchurch.app.domain.model.WordHighlight;
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
    private boolean explicitTarget = false;
    private static final String PREFS_READER = "bible_reader";
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

    /** Restores the book, chapter and version the reader was last using. */
    private void restoreLastPosition() {
        if (explicitTarget) {
            return;
        }
        try {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_READER, Context.MODE_PRIVATE);
            currentBookId = prefs.getString("book", currentBookId);
            currentChapter = prefs.getInt("chapter", currentChapter);
            currentTranslation = prefs.getString("translation", currentTranslation);
            verseTextSp = prefs.getFloat("verse_sp", verseTextSp);
            verseWeight = prefs.getInt("verse_weight", verseWeight);
        } catch (Exception ignored) {
            // Preferences unavailable: keep the defaults.
        }
    }

    private void saveLastPosition() {
        try {
            getContext().getSharedPreferences(PREFS_READER, Context.MODE_PRIVATE).edit()
                    .putString("book", currentBookId)
                    .putInt("chapter", currentChapter)
                    .putString("translation", currentTranslation)
                    .apply();
        } catch (Exception ignored) {
            // Saving is best effort.
        }
    }

    /** Three appearance icons in a popup under the header button; System is the default. */
    private View themePopupView;

    private void toggleAppearance() {
        if (themePopupView != null) {
            dismissThemePopup();
            return;
        }
        if (readerFrame == null) {
            return;
        }
        LinearLayout row = new LinearLayout(getContext());
        GlassKit.keep(row);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable panel = new GradientDrawable();
        panel.setCornerRadius(dp(24));
        panel.setColor(GlassKit.light() ? Color.parseColor("#F7FFFFFF") : Color.parseColor("#F2241238"));
        panel.setStroke(dp(1), Color.parseColor("#33000000"));
        row.setBackground(panel);
        int current = PerazimTheme.savedChoice(getContext());
        // System (auto), Light (sun), Dark (moon)
        String[] glyphs = {"", "\u2600", "\u263E"};
        String[] descriptions = {"System", "Light", "Dark"};
        for (int choice = 0; choice < glyphs.length; choice++) {
            final int picked = choice;
            TextView icon = GlassKit.text(getContext(), glyphs[choice], 16, true,
                    choice == current ? GlassKit.accent() : GlassKit.text());
            icon.setGravity(Gravity.CENTER);
            icon.setContentDescription(descriptions[choice]);
            if (choice == 0) {
                // System: a computer (monitor) icon
                android.graphics.drawable.Drawable monitor = getContext().getDrawable(org.perazimchurch.app.R.drawable.ic_theme_system);
                if (monitor != null) {
                    monitor.setTint(choice == current ? COLOR_ACCENT_ORANGE() : GlassKit.text());
                    icon.setCompoundDrawablesWithIntrinsicBounds(null, monitor, null, null);
                    icon.setPadding(0, dp(12), 0, 0);
                }
            }
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(choice == current ? Color.parseColor("#33E17D2F")
                    : (GlassKit.light() ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#33FFFFFF")));
            circle.setStroke(dp(1), choice == current ? COLOR_ACCENT_ORANGE() : Color.parseColor("#33000000"));
            icon.setBackground(circle);
            icon.setClickable(true);
            icon.setOnClickListener(v -> applyAppearance(picked));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
            lp.setMargins(dp(4), 0, dp(4), 0);
            row.addView(icon, lp);
        }
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        flp.setMargins(0, dp(70), dp(16), 0);
        readerFrame.addView(row, flp);
        themePopupView = row;
    }

    private void dismissThemePopup() {
        if (themePopupView != null && readerFrame != null) {
            readerFrame.removeView(themePopupView);
        }
        themePopupView = null;
    }

    /** Saves the chosen appearance and redraws the reader in place, without opening another window. */
    private void applyAppearance(int choice) {
        PerazimTheme.saveChoice(getContext(), choice);
        PerazimTheme.applyPreference(getContext());
        dismissActionBar();
        dismissNotesPanel();
        dismissNoteEditor();
        dismissThemePopup();
        View rebuilt = buildReaderLayout();
        setContentView(rebuilt);
        applyWindowChrome();
        loadCanonicalBooks();
        loadChapterVerses(currentBookId, currentChapter);
    }

    public void setTargetPassage(@NonNull String bookId, int chapter) {
        this.explicitTarget = true;
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

        restoreLastPosition();
        View rootView = buildReaderLayout();
        setContentView(rootView);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        applyWindowChrome();

        loadCanonicalBooks();
        loadChapterVerses(currentBookId, currentChapter);
    }

    private FrameLayout readerFrame;
    private View actionBarView;
    private BibleVerse actionVerse;
    private int focusedVerseNumber = 0;

    /** One background gradient behind everything; status and navigation bars are transparent so it flows top to bottom. */
    private void applyWindowChrome() {
        android.view.Window window = getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(PerazimTheme.createBackgroundGradient());
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            // Let the reader draw into the camera-cutout strip at the top, so no other screen shows there.
            android.view.WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.layoutInDisplayCutoutMode = android.os.Build.VERSION.SDK_INT >= 30
                    ? android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attrs);
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            // Stop the system painting a contrast scrim over the transparent bars.
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            // Draw behind the status and navigation bars, then pad the content so nothing sits under them.
            window.setDecorFitsSystemWindows(false);
            if (readerFrame != null) {
                readerFrame.setOnApplyWindowInsetsListener((v, insets) -> {
                    android.graphics.Insets bars = insets.getInsets(
                            android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                    v.setPadding(0, bars.top, 0, bars.bottom);
                    return insets;
                });
                readerFrame.requestApplyInsets();
            }
            android.view.WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int mask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(GlassKit.light() ? mask : 0, mask);
            }
        }
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
        frame.setBackground(null);
        readerFrame = frame;

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(createHeaderBar());

        searchBarLayout = createSearchBarLayout();
        searchBarLayout.setVisibility(View.GONE);
        root.addView(searchBarLayout);

        versesScrollView = new ScrollView(getContext());
        versesScrollView.setFillViewport(true);

        LinearLayout scrollColumn = new LinearLayout(getContext());
        scrollColumn.setOrientation(LinearLayout.VERTICAL);
        scrollColumn.setPadding(dp(16), dp(14), dp(16), dp(16));

        versesContainer = GlassKit.card(getContext(), 28, 0);
        // The card itself is the fixed frosted panel below; the verses scroll inside it.
        versesContainer.setBackground(null);
        versesContainer.setPadding(dp(20), dp(22), dp(20), dp(22));

        loadingProgress = new ProgressBar(getContext());
        loadingProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpProgress = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpProgress.gravity = Gravity.CENTER_HORIZONTAL;
        lpProgress.setMargins(0, dp(24), 0, dp(24));
        loadingProgress.setLayoutParams(lpProgress);
        versesContainer.addView(loadingProgress);

        tvEmptyState = GlassKit.text(getContext(), "", 13, true, verseInk());
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setPadding(dp(8), dp(16), dp(8), dp(16));
        tvEmptyState.setLineSpacing(0f, 1.3f);
        tvEmptyState.setVisibility(View.GONE);
        versesContainer.addView(tvEmptyState);

        scrollColumn.addView(versesContainer, new LinearLayout.LayoutParams(-1, -2));
        versesScrollView.addView(scrollColumn, new FrameLayout.LayoutParams(-1, -2));

        // Fixed frosted card: its top and bottom edges never move; only the verses inside scroll.
        FrameLayout chapterCard = new FrameLayout(getContext());
        GradientDrawable cardGlass = new GradientDrawable();
        cardGlass.setCornerRadius(dp(28));
        // The card is invisible (no fill, no border) but still holds the verses in place.
        cardGlass.setColor(Color.TRANSPARENT);
        chapterCard.setBackground(cardGlass);
        chapterCardView = chapterCard;
        GlassKit.keep(chapterCard);  // stop the automatic glass pass from drawing a card here
        chapterCard.addView(versesScrollView, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(-1, 0, 1f);
        lpCard.setMargins(dp(16), dp(12), dp(16), dp(12));
        root.addView(chapterCard, lpCard);

        // Previous / Next sit below the card, so they never cover the verses.
        chapterNavBar = createChapterNavBar();
        LinearLayout.LayoutParams lpNav = new LinearLayout.LayoutParams(-1, -2);
        lpNav.setMargins(dp(16), 0, dp(16), dp(24));
        root.addView(chapterNavBar, lpNav);
        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    private View createHeaderBar() {
        // Back circle, chapter pill (opens the book picker), KJV pill, search circle
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), 0);

        // App logo: goes to the home page.
        FrameLayout back = new FrameLayout(getContext());
        GlassKit.keep(back);
        back.setBackground(null);
        ImageView logo = new ImageView(getContext());
        logo.setImageResource(org.perazimchurch.app.R.drawable.ic_perazim_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        back.addView(logo, new FrameLayout.LayoutParams(dp(44), dp(44)));
        back.setContentDescription("Home");
        back.setOnClickListener(v -> dismiss());
        btnCloseDialog = back;
        row.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

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

        FrameLayout themeToggle = GlassKit.circleIcon(getContext(),
                GlassKit.light() ? org.perazimchurch.app.R.drawable.gl_sun : org.perazimchurch.app.R.drawable.gl_moon,
                44, 18, "Light or dark mode");
        themeToggle.setOnClickListener(v -> toggleAppearance());
        row.addView(themeToggle);

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
        topics.setOnClickListener(v -> showTopicsCard(null));
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
        // One compact row: A- A+ (size) and B- B+ (weight), kept small so the bottom bar stays minimal.
        LinearLayout sizes = new LinearLayout(getContext());
        sizes.setOrientation(LinearLayout.HORIZONTAL);
        sizes.setGravity(Gravity.CENTER);
        TextView smaller = sizeButton("A\u2212", "Smaller text", false);
        smaller.setOnClickListener(v -> changeVerseSize(-2f));
        TextView larger = sizeButton("A+", "Larger text", true);
        larger.setOnClickListener(v -> changeVerseSize(2f));
        TextView lighter = sizeButton("B\u2212", "Less bold text", false);
        lighter.setOnClickListener(v -> changeVerseWeight(-100));
        TextView bolder = sizeButton("B+", "Bolder text", true);
        bolder.setOnClickListener(v -> changeVerseWeight(100));
        sizes.addView(smaller);
        sizes.addView(larger);
        sizes.addView(lighter);
        sizes.addView(bolder);
        TextView highlightTools = sizeButton("Highlight", "Highlight tools: swipe colouring and select verses", false);
        highlightTools.setOnClickListener(v -> showHighlightTools());
        sizes.addView(highlightTools);
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
    /** Stroke weight of the verse text: 300 is light, 400 is the normal default, 700 is bold, 800 is heavy. */
    private int verseWeight = 400;

    /** One of the four text controls under the chapter line: bigger tap target, spaced apart. */
    private TextView sizeButton(String label, String description, boolean bold) {
        TextView b = GlassKit.text(getContext(), label, 13, bold, GlassKit.muted());
        b.setPadding(dp(9), dp(4), dp(9), dp(4));
        b.setContentDescription(description);
        b.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void changeVerseSize(float delta) {
        verseTextSp = Math.max(14f, Math.min(28f, verseTextSp + delta));
        saveVerseStyle();
        renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
    }

    /** B- removes weight (lighter text), B+ adds weight (bolder text). Size and weight are independent. */
    private void changeVerseWeight(int delta) {
        verseWeight = Math.max(300, Math.min(800, verseWeight + delta));
        saveVerseStyle();
        renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
    }

    private android.graphics.Typeface verseTypeface() {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            return Typeface.create(Typeface.SERIF, verseWeight, false);
        }
        return verseWeight >= 600 ? Typeface.create(Typeface.SERIF, Typeface.BOLD) : Typeface.create(Typeface.SERIF, Typeface.NORMAL);
    }

    private void saveVerseStyle() {
        try {
            getContext().getSharedPreferences(PREFS_READER, Context.MODE_PRIVATE).edit()
                    .putFloat("verse_sp", verseTextSp)
                    .putInt("verse_weight", verseWeight)
                    .apply();
        } catch (Exception ignored) {
            // Preferences unavailable: the size and weight still apply for this session.
        }
    }

    private void updateHeaderTitle() {
        String title = currentBookName + " " + currentChapter;
        title += " ▾";
        if (btnBookChapterTitle != null) {
            btnBookChapterTitle.setText(title);
        }
        if (tvChapterProgress != null) {
            tvChapterProgress.setText("Chapter " + currentChapter + " of " + currentChapterCount);
        }
    }

    private void updateTranslationBadge() {
        saveLastPosition();
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
            final LinearLayout list = new LinearLayout(getContext());
            list.setOrientation(LinearLayout.VERTICAL);
            final android.app.Dialog[] holder = new android.app.Dialog[1];
            for (BibleTranslation t : translations) {
                if (t.getId() == null) {
                    continue;
                }
                final String chosen = t.getId();
                boolean selected = chosen.equalsIgnoreCase(currentTranslation);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(14), dp(11), dp(14), dp(11));
                row.setBackground(cardTile(selected));
                row.setClickable(true);
                LinearLayout.LayoutParams lp = GlassKit.matchWrap(getContext(), 0);
                lp.bottomMargin = dp(6);
                row.setLayoutParams(lp);
                TextView code = GlassKit.text(getContext(), chosen, 14, true, selected ? Color.WHITE : notesInk());
                row.addView(code, new LinearLayout.LayoutParams(dp(56), -2));
                String name = t.getName() != null ? t.getName() : chosen;
                TextView label = GlassKit.text(getContext(), name, 13, false, selected ? Color.WHITE : notesMuted());
                row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
                row.setOnClickListener(v -> {
                    if (holder[0] != null) {
                        holder[0].dismiss();
                    }
                    if (!chosen.equalsIgnoreCase(currentTranslation)) {
                        // Stay on the verse the reader is on, rather than jumping back to verse 1.
                        pendingScrollVerse = topVisibleVerse();
                        currentTranslation = chosen;
                        updateTranslationBadge();
                        loadChapterVerses(currentBookId, currentChapter);
                    }
                });
                list.addView(row);
            }
            holder[0] = openCardDialog("Select translation", "Chagua tafsiri", list, null, null);
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
        exitSelectMode();
        freshRender = true;
        this.currentBookId = bookId;
        saveLastPosition();
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
                if (pendingVersePicker) {
                    pendingVersePicker = false;
                    showVersePicker();
                }
            } else {
                currentVerses.clear();
                tvEmptyState.setText("No " + currentTranslation + " text is downloaded for "
                        + currentBookName + " " + currentChapter
                        + ".\n\nKJV starter passages are bundled for offline reading. Other translations require an authorized scripture source.");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Select mode: drag a finger across verses (or tap them) to choose several, then pick a colour for all of them. */
    private boolean selectMode = false;
    private final java.util.TreeSet<Integer> selectedVerses = new java.util.TreeSet<>();
    private int dragVerse = -1;
    private boolean dragAdds = true;

    private void enterSelectMode(@Nullable BibleVerse verse) {
        dismissActionBar();
        openSelectCard();
        selectMode = true;
        selectedVerses.clear();
        if (verse != null) {
            selectedVerses.add(verse.getVerseNumber());
        }
        renderVerseList(currentVerses, false);
    }

    private void exitSelectMode() {
        selectMode = false;
        selectedVerses.clear();
        dragVerse = -1;
    }

    /** Touch handling while selecting: the verse under the finger is added, or removed if the drag began on a chosen verse. */
    private boolean selectTouch(android.view.MotionEvent ev) {
        if (!selectMode) {
            return false;
        }
        int action = ev.getActionMasked();
        if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_MOVE) {
            int hit = verseAtRawY(ev.getRawY());
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                dragAdds = hit <= 0 || !selectedVerses.contains(hit);
                dragVerse = -1;
            }
            if (hit > 0 && hit != dragVerse) {
                dragVerse = hit;
                if (dragAdds) {
                    selectedVerses.add(hit);
                } else {
                    selectedVerses.remove(hit);
                }
                restyleVerseRows();
            }
            if (versesScrollView != null) {
                versesScrollView.requestDisallowInterceptTouchEvent(true);
            }
        } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
            dragVerse = -1;
        }
        return true;
    }

    /** The verse whose row is under a screen Y position, or 0. */
    private int verseAtRawY(float rawY) {
        int[] loc = new int[2];
        for (Map.Entry<Integer, View> e : verseRows.entrySet()) {
            View row = e.getValue();
            row.getLocationOnScreen(loc);
            if (rawY >= loc[1] && rawY < loc[1] + row.getHeight()) {
                return e.getKey();
            }
        }
        return 0;
    }

    /** The colour panel for select mode: stays open so one colour can be chosen, then another portion selected. */
    private void openSelectCard() {
        LinearLayout list = openFloatingCard("select", "Select verses", null, null);
        // No backdrop card: strip the panel to its content, so only the picker and the Done icon show.
        if (notesPanelView instanceof LinearLayout) {
            LinearLayout panel = (LinearLayout) notesPanelView;
            panel.setBackground(null);
            panel.setPadding(dp(16), 0, dp(16), 0);
            View content = list;
            while (content.getParent() instanceof View && content.getParent() != panel) {
                content = (View) content.getParent();
            }
            for (int i = panel.getChildCount() - 1; i >= 0; i--) {
                if (panel.getChildAt(i) != content) {
                    panel.removeViewAt(i);
                }
            }
        }
        View picker = buildColourPicker(name -> applyHighlightToSelected(name), null, false, null, false);
        picker.setLayoutParams(new LinearLayout.LayoutParams(dp(300), dp(40)));
        list.addView(picker);
        ImageView done = new ImageView(getContext());
        done.setImageResource(org.perazimchurch.app.R.drawable.ic_hl_check);
        done.setContentDescription("Done");
        done.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable doneBg = new GradientDrawable();
        doneBg.setShape(GradientDrawable.OVAL);
        doneBg.setColor(COLOR_ACCENT_ORANGE());
        done.setBackground(doneBg);
        LinearLayout.LayoutParams doneLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        doneLp.setMargins(0, dp(8), 0, 0);
        done.setLayoutParams(doneLp);
        done.setOnClickListener(v -> dismissNotesPanel());
        LinearLayout doneRow = new LinearLayout(getContext());
        doneRow.setGravity(Gravity.CENTER);
        doneRow.addView(done);
        list.addView(doneRow);
    }

    /** Colours every chosen verse, then clears the choice so the next portion can be selected. */
    private void applyHighlightToSelected(final String colour) {
        recordColourUse(colour);
        if (selectedVerses.isEmpty()) {
            Toast.makeText(getContext(), "Drag across or tap the verses first", Toast.LENGTH_SHORT).show();
            return;
        }
        int count = 0;
        for (BibleVerse v : currentVerses) {
            if (selectedVerses.contains(v.getVerseNumber())) {
                v.setHighlightColor(colour);
                bibleViewModel.setHighlight(v.getBookId(), v.getChapterNumber(), v.getVerseNumber(), colour, () -> { });
                count++;
            }
        }
        selectedVerses.clear();
        renderVerseList(currentVerses, false);
        Toast.makeText(getContext(), "Highlighted " + count + (count == 1 ? " verse" : " verses"), Toast.LENGTH_SHORT).show();
    }

    /** Set when a chapter is chosen in the picker: the verse card opens once the chapter is on screen. */
    private boolean pendingVersePicker = false;

    /** One card with every verse number of the chapter, so a verse can be reached without scrolling. */
    private void showVersePicker() {
        int count = 0;
        for (BibleVerse v : currentVerses) {
            count = Math.max(count, v.getVerseNumber());
        }
        if (count <= 0) {
            return;
        }
        final LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        final android.app.Dialog[] holder = new android.app.Dialog[1];
        final int columns = 6;
        LinearLayout row = null;
        for (int n = 1; n <= count; n++) {
            if ((n - 1) % columns == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, GlassKit.matchWrap(getContext(), 0));
            }
            final int verseNumber = n;
            TextView cell = GlassKit.text(getContext(), String.valueOf(n), 14, true, notesInk());
            cell.setGravity(Gravity.CENTER);
            cell.setBackground(cardTile(false));
            cell.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            cell.setOnClickListener(v -> {
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                scrollToVerse(verseNumber);
            });
            row.addView(cell, lp);
        }
        if (row != null) {
            for (int i = row.getChildCount(); i < columns; i++) {
                View filler = new View(getContext());
                LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, dp(42), 1f);
                fp.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(filler, fp);
            }
        }
        holder[0] = openCardDialog(currentBookName + " " + currentChapter, "Choose a verse", grid,
                "\u2039 Start of chapter", () -> scrollToVerse(1));
    }

    /** Scrolls the chapter so the verse is at the top. */
    private void scrollToVerse(int verseNumber) {
        View row = verseRows.get(verseNumber);
        if (row != null && versesScrollView != null) {
            versesScrollView.post(() -> versesScrollView.smoothScrollTo(0, offsetWithin(row, versesScrollView)));
        }
    }

    /** The verse nearest the top of the screen: kept when the translation changes. */
    private int topVisibleVerse() {
        if (versesScrollView == null || verseRows.isEmpty()) {
            return 0;
        }
        int scrollY = versesScrollView.getScrollY();
        int best = 0;
        int bestY = Integer.MIN_VALUE;
        for (Map.Entry<Integer, View> e : verseRows.entrySet()) {
            int y = offsetWithin(e.getValue(), versesScrollView);
            if (y <= scrollY + dp(24) && y > bestY) {
                bestY = y;
                best = e.getKey();
            }
        }
        return best;
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
        styleVerseRow(row, verse, isSearchResult);
        GlassKit.keep(row);

        String label = isSearchResult ? formatCitation(verse) : String.valueOf(verse.getVerseNumber());
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        sb.append(label);
        int end = sb.length();
        // Search-result citations (Genesis 46:32) match the verse text size; chapter verse numbers are a little bigger than before.
        float numberScale = isSearchResult ? 1.0f : 13f / verseTextSp;
        sb.setSpan(new android.text.style.RelativeSizeSpan(numberScale), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor(GlassKit.light() ? "#3A0870" : "#FF8A2B")),
                0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new android.text.style.TypefaceSpan("sans-serif"), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(isSearchResult ? "  " : " ");
        int textStart = sb.length();
        boolean interlinearVersion = org.perazimchurch.app.data.repository.RoomBibleRepository.GREEK_INTERLINEAR.equals(verse.getTranslationId())
                || org.perazimchurch.app.data.repository.RoomBibleRepository.HEBREW_INTERLINEAR.equals(verse.getTranslationId());
        if (interlinearVersion && !isSearchResult) {
            appendInterlinearVerse(sb, verse.getText() != null ? verse.getText() : "");
        } else {
            sb.append(verse.getText() != null ? verse.getText() : "");
        }
        if (isSearchResult && !TextUtils.isEmpty(lastQuery)) {
            highlightSearchTerms(sb, textStart, verse.getText() != null ? verse.getText() : "", lastQuery);
        }
        if (!isSearchResult && !interlinearVersion) {
            applyWordHighlightSpans(sb, textStart, verse);
        }
        if (isCornerstone) {
            sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), textStart, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        TextView tvVerseText = new TextView(getContext());
        tvVerseText.setText(sb);
        tvVerseText.setTextSize(TypedValue.COMPLEX_UNIT_SP, verseTextSp);
        tvVerseText.setTypeface(verseTypeface());
        if (interlinearVersion && !isSearchResult) {
            // Selecting words opens their dictionary entry; selecting a range or the whole verse opens the verse menu.
            tvVerseText.setTextIsSelectable(true);
            tvVerseText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            tvVerseText.setCustomSelectionActionModeCallback(interlinearSelectionCallback(tvVerseText, verse));
        }
        if (!isSearchResult && !interlinearVersion) {
            // Drag across words to select a range: Highlight, Clear highlight or Select verses from the menu.
            tvVerseText.setTextIsSelectable(true);
            tvVerseText.setCustomSelectionActionModeCallback(wordSelectionCallback(tvVerseText, verse, textStart));
            tvVerseText.setOnClickListener(v -> showVerseActionBar(verse));
        }
        if (!isSearchResult) {
            // A quick sideways swipe across the verse selects the whole verse and opens the verse menu.
            final android.view.GestureDetector swipe = new android.view.GestureDetector(getContext(),
                    new android.view.GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float vx, float vy) {
                            if (e1 == null || e2 == null) {
                                return false;
                            }
                            float dx = Math.abs(e2.getX() - e1.getX());
                            float dy = Math.abs(e2.getY() - e1.getY());
                            if (dx > dp(60) && dx > 2 * dy) {
                                showVerseActionBar(verse);
                                return true;
                            }
                            return false;
                        }
                    });
            tvVerseText.setOnTouchListener((v, ev) -> {
                if (selectTouch(ev)) {
                    return true;
                }
                if (swipeColourOn && !interlinearVersion) {
                    return swipeColourTouch(tvVerseText, verse, textStart, ev);
                }
                swipe.onTouchEvent(ev);
                return false;
            });
        }
        if (!isSearchResult) {
            row.setOnTouchListener((v, ev) -> selectTouch(ev));
            row.setOnLongClickListener(v -> {
                enterSelectMode(verse);
                return true;
            });
        }
        tvVerseText.setTextColor(verseInk());
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

    /** Background and padding of a verse row: whole-verse highlight, favourite, or chosen in select mode. */
    private void styleVerseRow(LinearLayout row, BibleVerse verse, boolean isSearchResult) {
        row.setBackground(null);
        row.setPadding(0, 0, 0, 0);
        if (selectMode && !isSearchResult) {
            // Constant padding in select mode, so rows do not move while the finger drags across them.
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
        }
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
        if (selectMode && !isSearchResult && selectedVerses.contains(verse.getVerseNumber())) {
            // Chosen for colouring: a bordered panel in the highlight tint (or orange if not yet coloured).
            GradientDrawable chosen = new GradientDrawable();
            chosen.setCornerRadius(dp(12));
            chosen.setColor(highlight != null ? Color.parseColor(highlight.tint) : Color.parseColor("#33E17D2F"));
            chosen.setStroke(dp(2), Color.parseColor("#E17D2F"));
            row.setBackground(chosen);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
        }
    }

    /** Restyles the chosen verse rows in place; rebuilding them mid-drag would end the touch. */
    private void restyleVerseRows() {
        for (BibleVerse v : currentVerses) {
            View r = verseRows.get(v.getVerseNumber());
            if (r instanceof LinearLayout) {
                styleVerseRow((LinearLayout) r, v, false);
            }
        }
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

    /** Colours every occurrence of the searched words (orange, bold) inside a search result verse. */
    private void highlightSearchTerms(android.text.SpannableStringBuilder sb, int textStart, String text, String query) {
        String lower = text.toLowerCase(java.util.Locale.US);
        for (String term : query.trim().split("\\s+")) {
            if (term.length() < 2) {
                continue;
            }
            String needle = term.toLowerCase(java.util.Locale.US);
            int from = 0;
            while (true) {
                int at = lower.indexOf(needle, from);
                if (at < 0) {
                    break;
                }
                int start = textStart + at;
                int end = start + needle.length();
                // Light mode: a bold, readable purple. Dark mode: orange.
                sb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor(GlassKit.light() ? "#2E0560" : "#FF8A2B")),
                        start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (GlassKit.light()) {
                    sb.setSpan(new android.text.style.BackgroundColorSpan(Color.parseColor("#33A06CE0")),
                            start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),
                        start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                from = at + needle.length();
            }
        }
    }

    /** Parses the number from a code such as H7626 or G3588. Returns 0 when there is no code. */
    private static int parseStrongsNumber(String code) {
        if (code == null || code.length() < 2) {
            return 0;
        }
        try {
            return Integer.parseInt(code.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Interlinear version text: "{original} gloss" pairs. The English gloss is bold; the original sits inside
     * the braces in regular weight, so the reader sees the English first.
     */
    private void appendInterlinearVerse(android.text.SpannableStringBuilder sb, String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([^}]*)\\}\\s*([^{]*)").matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                sb.append(text.substring(last, m.start()));
            }
            String token = m.group(1);
            String code = "";
            int bar = token.indexOf('|');
            if (bar >= 0) {
                code = token.substring(bar + 1);
                token = token.substring(0, bar);
            }
            int wordStart = sb.length();
            // The original word: smaller, regular weight, muted and underlined. Isolated (FSI...PDI) for right-to-left text.
            int braceStart = sb.length();
            sb.append('{').append('\u2068').append(token).append('\u2069').append('}');
            int braceEnd = sb.length();
            sb.setSpan(new android.text.style.StyleSpan(Typeface.NORMAL), braceStart, braceEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.RelativeSizeSpan(0.8f), braceStart, braceEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.ForegroundColorSpan(GlassKit.muted()), braceStart, braceEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.UnderlineSpan(), braceStart, braceEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(' ');
            // The English gloss: bold and a little larger, so it is the first thing the eye reads.
            int glossStart = sb.length();
            sb.append(m.group(2).trim());
            int glossEnd = sb.length();
            sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), glossStart, glossEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.RelativeSizeSpan(1.15f), glossStart, glossEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // One span covers the whole word, so a tap on either part opens its dictionary entry.
            int number = parseStrongsNumber(code);
            if (number > 0 && glossEnd > wordStart) {
                sb.setSpan(new InterlinearWordSpan(code, number), wordStart, glossEnd,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append(' ');
            last = m.end();
        }
        if (last < text.length()) {
            sb.append(text.substring(last));
        }
    }

    /** Chooses the action from the selection: one word opens its dictionary entry, anything wider opens the verse menu. */
    private android.view.ActionMode.Callback interlinearSelectionCallback(final TextView tv, final BibleVerse verse) {
        return new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                int start = tv.getSelectionStart();
                int end = tv.getSelectionEnd();
                if (start < 0 || end <= start || !(tv.getText() instanceof android.text.Spannable)) {
                    return false;
                }
                final android.text.Spannable text = (android.text.Spannable) tv.getText();
                InterlinearWordSpan[] words = text.getSpans(start, end, InterlinearWordSpan.class);
                InterlinearWordSpan only = null;
                if (words.length == 1 && start >= text.getSpanStart(words[0]) && end <= text.getSpanEnd(words[0])) {
                    only = words[0];
                }
                final InterlinearWordSpan single = only;
                // Clear the selection first, so the card opens on a clean verse.
                tv.post(() -> {
                    android.text.Selection.removeSelection(text);
                    if (single != null) {
                        showStrongsDialog(single.code, single.number);
                    } else {
                        showVerseActionBar(verse);
                    }
                });
                return false;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
            }
        };
    }

    /** Swipe-to-colour mode: when on, dragging across a plain-text verse colours the words in bold purple. */
    private boolean swipeColourOn;
    /** Verse offset where the current swipe started, or -1 when no swipe is in progress. */
    private int swipeAnchor = -1;

    /** One small card of icon toggles for the highlight tools. On is orange, off is muted. */
    private void showHighlightTools() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        final android.app.Dialog[] holder = new android.app.Dialog[1];

        final ImageView swipe = highlightIcon(org.perazimchurch.app.R.drawable.ic_hl_swipe, swipeColourOn, "Swipe to colour words");
        swipe.setOnClickListener(v -> {
            swipeColourOn = !swipeColourOn;
            swipeAnchor = -1;
            styleHighlightIcon(swipe, swipeColourOn);
        });
        row.addView(swipe);

        ImageView select = highlightIcon(org.perazimchurch.app.R.drawable.ic_hl_select, false, "Select verses");
        select.setOnClickListener(v -> {
            if (holder[0] != null) {
                holder[0].dismiss();
            }
            enterSelectMode(null);
        });
        row.addView(select);

        // Icons only: no card, no title and no words.
        final android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(row);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0f);
        }
        holder[0] = dialog;
        dialog.show();
    }

    private ImageView highlightIcon(int drawable, boolean on, String description) {
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(drawable);
        icon.setContentDescription(description);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(46));
        lp.setMargins(dp(10), dp(4), dp(10), dp(4));
        icon.setLayoutParams(lp);
        styleHighlightIcon(icon, on);
        return icon;
    }

    private void styleHighlightIcon(ImageView icon, boolean on) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        // Translucent circle behind each tool, as on the verse action bar: dark on a dark phone, light on a light one.
        // Solid enough to read, like the verse action bar: deep purple behind light icons on dark, near-white behind dark icons on light.
        circle.setColor(on ? 0x33E17D2F : (phoneIsDark() ? 0x99301848 : 0xE6FFFFFF));
        icon.setBackground(circle);
        // Dark ink on the light theme, bright ink on the dark theme; the on state stays orange.
        icon.setColorFilter(on ? COLOR_ACCENT_ORANGE() : (phoneIsDark() ? 0xFFF1EAFB : 0xFF2A1B3D));
    }

    /**
     * Handles a touch while swipe colouring is on. A drag from the first word to the last colours
     * everything between them in purple; a tap without a drag opens the verse menu as before.
     */
    private boolean swipeColourTouch(final TextView tv, final BibleVerse verse, final int textStart,
                                     android.view.MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == android.view.MotionEvent.ACTION_DOWN) {
            if (tv.getLayout() == null) {
                return false;
            }
            tv.getParent().requestDisallowInterceptTouchEvent(true);
            swipeAnchor = tv.getOffsetForPosition(ev.getX(), ev.getY());
            return true;
        }
        if (action == android.view.MotionEvent.ACTION_MOVE) {
            return swipeAnchor >= 0;
        }
        if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
            if (swipeAnchor < 0) {
                return true;
            }
            int last = tv.getOffsetForPosition(ev.getX(), ev.getY());
            int from = Math.min(swipeAnchor, last) - textStart;
            int to = Math.max(swipeAnchor, last) - textStart;
            swipeAnchor = -1;
            int[] range = trimmedBodyRange(verse, from, to);
            if (action == android.view.MotionEvent.ACTION_UP && range != null) {
                applyWordHighlight(verse, range[0], range[1], "bold-purple");
            } else if (action == android.view.MotionEvent.ACTION_UP) {
                showVerseActionBar(verse);
            }
            return true;
        }
        return false;
    }

    /** Clamps [from, to) to the verse body and trims spaces at both ends; null when nothing is left. */
    @Nullable
    private static int[] trimmedBodyRange(BibleVerse verse, int from, int to) {
        String body = verse.getText() != null ? verse.getText() : "";
        int start = Math.max(0, from);
        int end = Math.min(body.length(), to);
        while (start < end && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(body.charAt(end - 1))) {
            end--;
        }
        return end > start ? new int[]{start, end} : null;
    }

    /** Action menu for a selected word or range in a plain-text verse. */
    private android.view.ActionMode.Callback wordSelectionCallback(final TextView tv, final BibleVerse verse, final int textStart) {
        return new android.view.ActionMode.Callback() {
            private int[] range;

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                range = bodyRangeOfSelection(tv, verse, textStart);
                if (range == null) {
                    return false;
                }
                menu.add(0, 1, 0, "Highlight");
                if (overlapsWordHighlight(verse, range[0], range[1])) {
                    menu.add(0, 2, 1, "Clear highlight");
                }
                menu.add(0, 3, 2, "Select verses");
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                if (range == null) {
                    mode.finish();
                    return true;
                }
                final int start = range[0];
                final int end = range[1];
                mode.finish();
                if (item.getItemId() == 1) {
                    pickWordColour(verse, start, end);
                } else if (item.getItemId() == 2) {
                    clearWordHighlight(verse, start, end);
                } else if (item.getItemId() == 3) {
                    enterSelectMode(verse);
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
            }
        };
    }

    /** The selected text as [start, end) offsets into the verse body, trimmed of spaces; null when nothing is selected. */
    @Nullable
    private static int[] bodyRangeOfSelection(TextView tv, BibleVerse verse, int textStart) {
        int selStart = tv.getSelectionStart();
        int selEnd = tv.getSelectionEnd();
        if (selStart < 0 || selEnd <= selStart) {
            return null;
        }
        String body = verse.getText() != null ? verse.getText() : "";
        int start = Math.max(0, selStart - textStart);
        int end = Math.min(body.length(), selEnd - textStart);
        while (start < end && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(body.charAt(end - 1))) {
            end--;
        }
        return end > start ? new int[]{start, end} : null;
    }

    private static boolean overlapsWordHighlight(BibleVerse verse, int start, int end) {
        for (WordHighlight highlight : verse.getWordHighlights()) {
            if (highlight.overlaps(start, end)) {
                return true;
            }
        }
        return false;
    }

    /** Paints each word or character range of the verse in its colour; only the covered characters change. */
    private static void applyWordHighlightSpans(android.text.SpannableStringBuilder sb, int textStart, BibleVerse verse) {
        int bodyLength = sb.length() - textStart;
        for (WordHighlight highlight : verse.getWordHighlights()) {
            HighlightColors.Colour colour = HighlightColors.byName(highlight.getColour());
            int start = Math.max(0, Math.min(highlight.getStart(), bodyLength));
            int end = Math.max(0, Math.min(highlight.getEnd(), bodyLength));
            if (colour == null || end <= start) {
                continue;
            }
            int fill = (Color.parseColor(colour.swatch) & 0x00FFFFFF) | 0x73000000;
            sb.setSpan(new android.text.style.BackgroundColorSpan(fill), textStart + start, textStart + end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** The open colour card for a word or range, closed once a colour is applied. */
    private android.app.Dialog wordColourDialog;

    /** Colour card for a chosen word or range. */
    private void pickWordColour(final BibleVerse verse, final int start, final int end) {
        wordColourDialog = openCardDialog("Highlight", null,
                buildColourPicker(name -> applyWordHighlight(verse, start, end, name), null, false, null, false), null, null);
    }

    /** Adds a word range to the verse, updates the loaded verse lists and redraws. */
    private void applyWordHighlight(final BibleVerse verse, final int start, final int end, final String colour) {
        recordColourUse(colour);
        if (wordColourDialog != null) {
            wordColourDialog.dismiss();
            wordColourDialog = null;
        }
        List<WordHighlight> ranges = new ArrayList<>(verse.getWordHighlights());
        ranges.add(new WordHighlight(start, end, colour));
        setWordHighlights(verse, ranges);
        bibleViewModel.addWordHighlight(verse.getTranslationId(), verse.getBookId(), verse.getChapterNumber(),
                verse.getVerseNumber(), start, end, colour, null);
        renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
    }

    /** Removes the word ranges that overlap the selection; other ranges in the verse stay. */
    private void clearWordHighlight(final BibleVerse verse, final int start, final int end) {
        List<WordHighlight> kept = new ArrayList<>();
        for (WordHighlight highlight : verse.getWordHighlights()) {
            if (!highlight.overlaps(start, end)) {
                kept.add(highlight);
            }
        }
        setWordHighlights(verse, kept);
        bibleViewModel.clearWordHighlights(verse.getTranslationId(), verse.getBookId(), verse.getChapterNumber(),
                verse.getVerseNumber(), start, end, null);
        renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
    }

    private void setWordHighlights(final BibleVerse verse, final List<WordHighlight> ranges) {
        verse.setWordHighlights(new ArrayList<>(ranges));
        for (BibleVerse loaded : currentVerses) {
            if (loaded.getVerseNumber() == verse.getVerseNumber() && loaded.getChapterNumber() == verse.getChapterNumber()) {
                loaded.setWordHighlights(new ArrayList<>(ranges));
            }
        }
    }

    /** Size scale for the word cards: follows the verse A- / A+ setting (18 is the default). */
    private float cardScale() {
        return verseTextSp / 18f;
    }

    /** Typeface for the word cards: follows the verse B- / B+ setting. */
    private android.graphics.Typeface cardTypeface(String family, int weight) {
        int w = Math.max(300, Math.min(900, weight));
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            return Typeface.create(Typeface.create(family, Typeface.NORMAL), w, false);
        }
        return w >= 600 ? Typeface.create(family, Typeface.BOLD) : Typeface.create(family, Typeface.NORMAL);
    }

    /** A tappable interlinear word: opens its Strong's entry when it is tapped or selected on its own. */
    private final class InterlinearWordSpan extends android.text.style.ClickableSpan {
        final String code;
        final int number;

        InterlinearWordSpan(String code, int number) {
            this.code = code;
            this.number = number;
        }

        @Override
        public void onClick(android.view.View widget) {
            showStrongsDialog(code, number);
        }

        @Override
        public void updateDrawState(android.text.TextPaint ds) {
            ds.setUnderlineText(false);
        }
    }

    /** Verse text colour: light ink on the dark frosted chapter card in light mode. */
    private int verseInk() {
        return GlassKit.text();
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
        GlassKit.keep(bar);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setGravity(Gravity.CENTER_HORIZONTAL);

        // The area above the main row shows whichever group is open: colours, save tools or study tools.
        final LinearLayout expand = new LinearLayout(getContext());
        expand.setOrientation(LinearLayout.VERTICAL);
        expand.setGravity(Gravity.CENTER_HORIZONTAL);
        bar.addView(expand, GlassKit.matchWrap(getContext(), 4));

        final ImageView highlightBtn = barIconRes(org.perazimchurch.app.R.drawable.ic_bar_highlight, "Highlight colours", null);
        final ImageView saveBtn = barIconRes(org.perazimchurch.app.R.drawable.ic_bar_saveplus, "Save and notes", null);
        final ImageView studyBtn = barIconRes(org.perazimchurch.app.R.drawable.ic_bar_study, "Study tools", null);
        final String[] openGroup = {""};
        final Runnable[] refreshGroup = new Runnable[1];
        final java.util.function.Consumer<String> toggleGroup = kind -> {
            openGroup[0] = kind.equals(openGroup[0]) ? "" : kind;
            expand.removeAllViews();
            tintBarIcon(highlightBtn, "highlight".equals(openGroup[0]));
            tintBarIcon(saveBtn, "save".equals(openGroup[0]));
            tintBarIcon(studyBtn, "study".equals(openGroup[0]));
            if ("highlight".equals(openGroup[0])) {
                expand.addView(buildPalette(verse));
            } else if ("save".equals(openGroup[0])) {
                LinearLayout row = barRow();
                final ImageView star = barIconRes(verse.isFavorite()
                        ? org.perazimchurch.app.R.drawable.ic_bar_star_filled : org.perazimchurch.app.R.drawable.ic_bar_star, "Bookmark", null);
                tintBarIcon(star, verse.isFavorite());
                star.setOnClickListener(v -> {
                    boolean nextState = !verse.isFavorite();
                    verse.setFavorite(nextState);
                    bibleViewModel.toggleBookmark(verse.getId(), nextState, () ->
                            Toast.makeText(getContext(), nextState ? "Bookmarked" : "Bookmark removed", Toast.LENGTH_SHORT).show());
                    star.setImageResource(nextState ? org.perazimchurch.app.R.drawable.ic_bar_star_filled : org.perazimchurch.app.R.drawable.ic_bar_star);
                    tintBarIcon(star, nextState);
                    renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
                });
                row.addView(star);
                row.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_topic, "Topics", v -> {
                    dismissActionBar();
                    showTopicsCard(verse);
                }));
                row.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_edit, "Note on this verse", v -> {
                    dismissActionBar();
                    showStudyNoteEditor(verse);
                }));
                row.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_notes, "All notes", v -> {
                    dismissActionBar();
                    showNotesDialog();
                }));
                expand.addView(row);
            } else if ("study".equals(openGroup[0])) {
                LinearLayout row = barRow();
                row.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_compare, "Compare translations", v -> showCompareDialog(verse, citation)));
                row.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_interlinear, "Interlinear", v -> showInterlinearDialog(verse, citation, newTestament)));
                row.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_search, "Search the Bible", v -> {
                    dismissActionBar();
                    if (!isSearchMode) {
                        toggleSearchMode();
                    }
                }));
                if (isSearchMode) {
                    row.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_study, "Open chapter", v -> {
                        dismissActionBar();
                        loadChapterVerses(verse.getBookId(), verse.getChapterNumber());
                    }));
                }
                expand.addView(row);
            }
        };
        highlightBtn.setOnClickListener(v -> toggleGroup.accept("highlight"));
        saveBtn.setOnClickListener(v -> toggleGroup.accept("save"));
        studyBtn.setOnClickListener(v -> toggleGroup.accept("study"));

        LinearLayout primary = barRow();
        primary.addView(highlightBtn);
        primary.addView(saveBtn);
        primary.addView(studyBtn);
        primary.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_copy, "Copy", v -> copyVerse(verse, citation)));
        primary.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_share, "Share", v -> shareVerse(verse, citation)));
        primary.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_close, "Close", v -> dismissActionBar()));
        bar.addView(primary);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.setMargins(dp(12), 0, dp(12), dp(104));
        readerFrame.addView(bar, lp);
        actionBarView = bar;
    }

    /** Colours used most often, so the shared picker shows them first. */
    private static final String[] DEFAULT_COLOURS = {"bold-purple", "bold-yellow", "bold-green", "bold-blue", "bold-orange", "red"};
    private static final String HIGHLIGHT_PREFS = "highlight_colour_use";

    private void recordColourUse(@Nullable String colour) {
        if (colour == null || getContext() == null) {
            return;
        }
        try {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences(HIGHLIGHT_PREFS, Context.MODE_PRIVATE);
            prefs.edit().putInt(colour, prefs.getInt(colour, 0) + 1).apply();
        } catch (RuntimeException ignored) {
            // Usage counts are a convenience; a failed write must not stop the highlight.
        }
    }

    /** Up to six colours, most used first, then the defaults to fill the row. */
    private List<String> frequentColours() {
        List<String> names = new ArrayList<>();
        try {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences(HIGHLIGHT_PREFS, Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            List<Map.Entry<String, ?>> entries = new ArrayList<>(all.entrySet());
            java.util.Collections.sort(entries, (a, b) -> Integer.compare(
                    ((Number) b.getValue()).intValue(), ((Number) a.getValue()).intValue()));
            for (Map.Entry<String, ?> e : entries) {
                if (names.size() < 6 && e.getValue() instanceof Number && HighlightColors.byName(e.getKey()) != null) {
                    names.add(e.getKey());
                }
            }
        } catch (RuntimeException ignored) {
            // Fall through to the defaults.
        }
        for (String fallback : DEFAULT_COLOURS) {
            if (names.size() >= 6) {
                break;
            }
            if (!names.contains(fallback)) {
                names.add(fallback);
            }
        }
        return names;
    }

    /**
     * The one colour picker used everywhere, as a compact panel: a hue track, a saturation track and an
     * intensity track (washed out to dark), a row of frequently used colours, and a check to apply the mix.
     * Presets apply straight away; the tracks preview until the check is pressed.
     */
    private View buildColourPicker(final java.util.function.Consumer<String> onPick, @Nullable final String current,
                                   boolean allowRemove, @Nullable final Runnable onRemove, boolean framed) {
        final String seed = current != null && HighlightColors.byName(current) != null ? current : frequentColours().get(0);
        HighlightColors.Colour seedColour = HighlightColors.byName(seed);
        int seedRgb = seedColour != null ? Color.parseColor(seedColour.swatch) : 0xFF8E24AA;
        final float[] hsv = new float[3];
        Color.colorToHSV(seedRgb, hsv);
        // State: hue in degrees, saturation 0..1, intensity position 0 (dark) .. 1 (washed out).
        final float[] st = {hsv[0], hsv[1], Math.max(0f, Math.min(1f, 0.5f * (hsv[2] - 0.35f) / 0.65f))};

        // Frosted, slightly curved card with a single row: two strips on the left, colour circles on the right.
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        if (framed) {
            card.setPadding(dp(12), dp(4), dp(12), dp(4));
            GradientDrawable frost = new GradientDrawable();
            frost.setCornerRadius(dp(12));
            // Light package on a dark phone, dark package on a light phone.
            frost.setColor(phoneIsDark() ? 0x80FFFFFF : 0xCC2D0938);
            frost.setStroke(dp(1), phoneIsDark() ? 0xD9FFFFFF : 0x33FFFFFF);
            card.setBackground(frost);
        }

        final TrackSlider[] bars = new TrackSlider[2];
        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            int colour = intensityColour(st[0], st[1], st[2]);
            int thumb = pickerThumbColour();
            bars[0].update(st[0] / 360f, hueStops(), thumb);
            bars[1].update(st[2], intensityStops(st[0], st[1]), thumb);
            bars[1].setTag(colour);
        };
        LinearLayout strips = new LinearLayout(getContext());
        strips.setOrientation(LinearLayout.VERTICAL);
        strips.setGravity(Gravity.CENTER_VERTICAL);
        bars[0] = new TrackSlider(getContext(), st[0] / 360f, (f, done) -> {
            st[0] = f * 360f;
            refresh[0].run();
        });
        bars[1] = new TrackSlider(getContext(), st[2], (f, done) -> {
            st[2] = f;
            refresh[0].run();
        });
        for (TrackSlider bar : bars) {
            strips.addView(bar, new LinearLayout.LayoutParams(-1, dp(14)));
        }
        card.addView(strips, new LinearLayout.LayoutParams(0, -2, 1f));
        refresh[0].run();

        LinearLayout dots = new LinearLayout(getContext());
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        for (final String name : frequentColours()) {
            HighlightColors.Colour colour = HighlightColors.byName(name);
            if (colour == null) {
                continue;
            }
            final boolean chosen = name.equals(current);
            View dot = swatchView(colour.swatch, false, v -> {
                recordColourUse(name);
                onPick.accept(name);
            });
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(14), dp(14));
            dotLp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(dotLp);
            // The chosen circle grows to 1.5 times its size instead of showing a ring.
            dot.setScaleX(chosen ? 1.5f : 1f);
            dot.setScaleY(chosen ? 1.5f : 1f);
            dots.addView(dot);
        }
        if (allowRemove && onRemove != null) {
            dots.addView(barIconRes(org.perazimchurch.app.R.drawable.ic_bar_close, "Remove highlight", v -> onRemove.run()));
        }
        ImageView apply = new ImageView(getContext());
        apply.setImageResource(org.perazimchurch.app.R.drawable.ic_hl_check);
        apply.setContentDescription("Apply this colour");
        apply.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable applyBg = new GradientDrawable();
        applyBg.setShape(GradientDrawable.OVAL);
        applyBg.setColor(COLOR_ACCENT_ORANGE());
        apply.setBackground(applyBg);
        LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(dp(14), dp(14));
        applyLp.setMargins(dp(4), 0, 0, 0);
        apply.setLayoutParams(applyLp);
        apply.setOnClickListener(v -> {
            int rgb = intensityColour(st[0], st[1], st[2]);
            String hex = String.format(java.util.Locale.US, "#%06X", rgb & 0xFFFFFF);
            recordColourUse(hex);
            onPick.accept(hex);
        });
        dots.addView(apply);
        card.addView(dots, new LinearLayout.LayoutParams(-2, -2));
        return card;
    }

    /**
     * True when the reader is drawn dark. The reader's background follows the app's theme (GlassKit), and the
     * verse action bar contrasts with that, so the picker and tool icons do the same.
     */
    private boolean phoneIsDark() {
        return !GlassKit.light();
    }

    /** Slider thumbs contrast with the phone: purple on a light phone, orange on a dark phone. */
    private int pickerThumbColour() {
        return phoneIsDark() ? 0xFFE17D2F : 0xFF4A1D6E;
    }

    /** A thin strip with a round thumb. The gradient is the strip; the thumb is dark purple with a glow. */
    private static final class TrackSlider extends View {
        interface Listener {
            void onMove(float value, boolean done);
        }

        private float value;
        private int[] stops = {0xFF000000, 0xFFFFFFFF};
        private int thumbFill = 0xFF4A1D6E;
        private final Listener listener;
        private final float density;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        TrackSlider(android.content.Context context, float initial, Listener listener) {
            super(context);
            this.value = initial;
            this.listener = listener;
            this.density = context.getResources().getDisplayMetrics().density;
        }

        void update(float newValue, int[] newStops, int fill) {
            value = Math.max(0f, Math.min(1f, newValue));
            stops = newStops;
            thumbFill = fill;
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            float inset = 4 * density;
            float trackH = 3 * density;
            float cy = getHeight() / 2f;
            android.graphics.RectF track = new android.graphics.RectF(inset, cy - trackH / 2, getWidth() - inset, cy + trackH / 2);
            paint.setShader(new android.graphics.LinearGradient(track.left, 0, track.right, 0, stops, null,
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRoundRect(track, trackH / 2, trackH / 2, paint);
            paint.setShader(null);
            float x = track.left + value * track.width();
            paint.setShadowLayer(4 * density, 0, 0, (thumbFill & 0x00FFFFFF) | 0xCC000000);
            paint.setColor(thumbFill);
            canvas.drawCircle(x, cy, 3.5f * density, paint);
            paint.clearShadowLayer();
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent ev) {
            int action = ev.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_MOVE
                    || action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                float inset = 4 * density;
                float span = Math.max(1f, getWidth() - 2 * inset);
                value = Math.max(0f, Math.min(1f, (ev.getX() - inset) / span));
                invalidate();
                listener.onMove(value, action == android.view.MotionEvent.ACTION_UP
                        || action == android.view.MotionEvent.ACTION_CANCEL);
                return true;
            }
            return super.onTouchEvent(ev);
        }
    }

    private static int hsvInt(float hue, float sat, float val) {
        return Color.HSVToColor(new float[]{hue, sat, val});
    }

    private static int[] hueStops() {
        return new int[]{hsvInt(0, 1, 1), hsvInt(60, 1, 1), hsvInt(120, 1, 1), hsvInt(180, 1, 1),
                hsvInt(240, 1, 1), hsvInt(300, 1, 1), hsvInt(360, 1, 1)};
    }

    /** Dark, the colour itself, then washed out. */
    private static int[] intensityStops(float hue, float sat) {
        return new int[]{hsvInt(hue, sat, 0.35f), hsvInt(hue, sat, 1f), hsvInt(hue, sat * 0.25f, 1f)};
    }

    private static int intensityColour(float hue, float sat, float position) {
        return lerpStops(intensityStops(hue, sat), position);
    }

    private static int lerpStops(int[] stops, float position) {
        float pos = Math.max(0f, Math.min(1f, position)) * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) pos);
        float f = pos - i;
        int a = stops[i];
        int b = stops[i + 1];
        return Color.rgb(Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * f),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * f),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f));
    }

    /** Colours for the verse action bar: the shared picker, with remove. */
    private View buildPalette(final BibleVerse verse) {
        return buildColourPicker(name -> applyHighlight(verse, name), verse.getHighlightColor(),
                true, () -> applyHighlight(verse, null), true);
    }

    /** Active group icons turn orange; the rest use the normal ink. */
    private void tintBarIcon(ImageView icon, boolean active) {
        icon.setColorFilter(active ? COLOR_ACCENT_ORANGE() : GlassKit.text());
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
    /** Circular floating icon drawn from a vector, for the Notes, Topics and Search buttons. */
    private ImageView barIconRes(int drawableRes, String label, View.OnClickListener onClick) {
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(drawableRes);
        icon.setColorFilter(GlassKit.text());
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int pad = dp(11);
        icon.setPadding(pad, pad, pad, pad);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(GlassKit.light() ? Color.parseColor("#F2FFFFFF") : Color.parseColor("#D9201830"));
        circle.setStroke(dp(1), Color.parseColor("#33000000"));
        icon.setBackground(circle);
        icon.setContentDescription(label);
        if (onClick != null) {
            icon.setOnClickListener(onClick);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        icon.setLayoutParams(lp);
        return icon;
    }

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
        GlassKit.keep(pill);
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

    /**
     * Word-by-word view of a verse, laid out like a printed interlinear. Each column is one original word with
     * its transliteration, English gloss, tappable Strong's number and plain-English parsing. Columns wrap onto
     * new lines, so the verse fits the card without sideways scrolling. Hebrew runs right to left.
     */
    private void showInterlinearDialog(final BibleVerse verse, final String citation, final boolean newTestament) {
        bibleViewModel.loadInterlinear(verse.getBookId(), verse.getChapterNumber(), verse.getVerseNumber(), words -> {
            if (words == null || words.isEmpty()) {
                Toast.makeText(getContext(), "No interlinear is available for this verse.", Toast.LENGTH_SHORT).show();
                return;
            }
            final boolean hebrew = !newTestament;
            WrapLayout flow = new WrapLayout(getContext(), dp(6), dp(8));
            flow.setRightToLeft(hebrew);
            for (org.perazimchurch.app.domain.model.InterlinearWord word : words) {
                flow.addView(interlinearColumn(word, hebrew));
            }
            LinearLayout box = new LinearLayout(getContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.addView(flow);
            TextView note = GlassKit.text(getContext(), hebrew
                    ? "Hebrew text and morphology: Open Scriptures Hebrew Bible (CC BY 4.0). English glosses: STEPBible.org Translators Amalgamated OT, Tyndale House Cambridge (CC BY 4.0). Strong's entries: Strong's dictionary. Tap a Strong's number for its entry."
                    : "Greek text and parsing: Antoniades 1904/1912 (public domain). English glosses: STEPBible.org Translators Amalgamated NT, Tyndale House Cambridge (CC BY 4.0). Strong's entries: Strong's dictionary (public domain). Tap a Strong's number for its entry and occurrences.",
                    11, false, notesMuted());
            note.setPadding(0, dp(12), 0, 0);
            box.addView(note);
            ScrollView scroll = new ScrollView(getContext());
            openCardDialog(citation + (hebrew ? " \u00b7 Hebrew Interlinear" : " \u00b7 Greek Interlinear"),
                    "Word by word", box, null, null);
        });
    }

    /** One interlinear column: the word, its transliteration, gloss, Strong's number and parsing. */
    private View interlinearColumn(final org.perazimchurch.app.domain.model.InterlinearWord word, boolean hebrew) {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(8), dp(8), dp(8), dp(8));
        column.setLayoutParams(new ViewGroup.LayoutParams(dp(132), ViewGroup.LayoutParams.WRAP_CONTENT));
        // No tile behind each word: the words sit directly on the one card, so there is no card inside a card.

        TextView original = new TextView(getContext());
        original.setText("{" + word.getOriginal() + "}");
        original.setTypeface(cardTypeface("serif", verseWeight));
        original.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * cardScale());
        original.setTextColor(notesInk());
        original.setGravity(Gravity.CENTER);
        original.setTextDirection(hebrew ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        column.addView(original);

        TextView translit = new TextView(getContext());
        translit.setText(word.getTransliteration());
        translit.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        translit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * cardScale());
        translit.setTextColor(notesMuted());
        translit.setGravity(Gravity.CENTER);
        column.addView(translit);

        TextView gloss = new TextView(getContext());
        gloss.setText(word.getGloss().isEmpty() ? "\u2014" : word.getGloss());
        gloss.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19 * cardScale());
        // The English is bold by default; the B- / B+ setting moves it lighter or heavier.
        gloss.setTypeface(cardTypeface("sans-serif", verseWeight + 300));
        gloss.setTextColor(notesInk());
        gloss.setGravity(Gravity.CENTER);
        gloss.setPadding(0, dp(4), 0, dp(4));
        column.addView(gloss);

        if (word.getStrongsNumber() > 0) {
            final String code = (hebrew ? "H" : "G") + word.getStrongsNumber();
            final int number = word.getStrongsNumber();
            TextView strongs = new TextView(getContext());
            strongs.setText("{" + code + "}");
            strongs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * cardScale());
            strongs.setTextColor(cardAccent());
            strongs.setPaintFlags(strongs.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            strongs.setGravity(Gravity.CENTER);
            strongs.setPadding(dp(6), dp(4), dp(6), dp(4));
            strongs.setClickable(true);
            strongs.setContentDescription("Strong's " + code + " for " + word.getOriginal());
            strongs.setOnClickListener(v -> showStrongsDialog(code, number));
            column.addView(strongs);
        }

        TextView parsing = new TextView(getContext());
        parsing.setText(word.getParsing().isEmpty() ? word.getMorphology() : word.getParsing());
        parsing.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11 * cardScale());
        parsing.setTextColor(notesMuted());
        parsing.setGravity(Gravity.CENTER);
        parsing.setPadding(0, dp(4), 0, 0);
        column.addView(parsing);
        return column;
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
            def.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14 * cardScale());
            def.setTypeface(cardTypeface("sans-serif", verseWeight));
            def.setTextColor(notesInk());
            def.setLineSpacing(0f, 1.3f);
            box.addView(def);
            TextView head = new TextView(getContext());
            head.setText("NEW TESTAMENT OCCURRENCES (" + detail.occurrences.size() + ")");
            head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            head.setLetterSpacing(0.08f);
            head.setTypeface(Typeface.DEFAULT_BOLD);
            head.setTextColor(cardAccent());
            head.setPadding(0, dp(16), 0, dp(4));
            box.addView(head);
            StringBuilder refs = new StringBuilder();
            for (org.perazimchurch.app.domain.model.VerseLocation loc : detail.occurrences) {
                if (refs.length() > 0) refs.append(", ");
                refs.append(BibleDataSeeder.canonicalBookName(loc.bookNumber)).append(' ').append(loc.chapter).append(':').append(loc.verse);
            }
            TextView list = new TextView(getContext());
            list.setText(refs.length() == 0 ? "None" : refs.toString());
            list.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13 * cardScale());
            list.setTypeface(cardTypeface("sans-serif", verseWeight));
            list.setTextColor(notesMuted());
            list.setLineSpacing(0f, 1.3f);
            box.addView(list);
            openCardDialog("Strong's " + code, "Dictionary entry and concordance", box, null, null);
        });
    }

    /** Frosted floating Notes card; drag the handle to resize it so the verses stay visible. */
    private View notesPanelView;
    private View notesScrimView;
    private View chapterCardView;
    private LinearLayout notesPanelList;

    /** Which card is open: "notes", "topics" or "topic-save"; empty when none. */
    private String cardKind = "";

    /** Notes icon: opens the Notes card, or closes it if it is already open. */
    private void showNotesDialog() {
        if (notesPanelView != null && "notes".equals(cardKind)) {
            dismissNotesPanel();
            return;
        }
        if (readerFrame == null) {
            return;
        }
        openFloatingCard("notes", "Notes", "New note", () -> showNoteEditor(null));
        refreshNotesPanel();
    }

    /**
     * Builds the frosted floating card used for Notes and Topics: drag the header to float it, drag the handle to
     * resize it. Returns the list that the caller fills.
     */
    private LinearLayout openFloatingCard(String kind, String titleText, @Nullable String actionLabel, @Nullable Runnable action) {
        dismissNotesPanel();
        cardKind = kind;
        final android.view.ScaleGestureDetector[] pinch = new android.view.ScaleGestureDetector[1];
        final boolean[] twoFingerActive = new boolean[1];
        final float[] twoFingerLast = new float[2];
        final LinearLayout panel = new LinearLayout(getContext()) {
            @Override
            public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
                // The card owns every touch that lands on it, so nothing reaches the verses behind it.
                // Two fingers on the card drag it (the middle of the two fingers follows the card),
                // and spreading or pinching them resizes it. One finger still scrolls the list inside.
                if (pinch[0] != null) {
                    pinch[0].onTouchEvent(ev);
                }
                int action = ev.getActionMasked();
                if (ev.getPointerCount() >= 2) {
                    float cx = 0f;
                    float cy = 0f;
                    for (int i = 0; i < ev.getPointerCount(); i++) {
                        cx += ev.getRawX(i);
                        cy += ev.getRawY(i);
                    }
                    cx /= ev.getPointerCount();
                    cy /= ev.getPointerCount();
                    if (!twoFingerActive[0]) {
                        // Stop the list from scrolling with the first finger before the second one lands.
                        android.view.MotionEvent cancel = android.view.MotionEvent.obtain(ev);
                        cancel.setAction(android.view.MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                        twoFingerActive[0] = true;
                        twoFingerLast[0] = cx;
                        twoFingerLast[1] = cy;
                    } else if (action == android.view.MotionEvent.ACTION_MOVE
                            && (pinch[0] == null || !pinch[0].isInProgress())) {
                        setTranslationX(getTranslationX() + cx - twoFingerLast[0]);
                        setTranslationY(getTranslationY() + cy - twoFingerLast[1]);
                    }
                    twoFingerLast[0] = cx;
                    twoFingerLast[1] = cy;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                twoFingerActive[0] = false;
                if (pinch[0] != null && pinch[0].isInProgress()) {
                    return true;
                }
                super.dispatchTouchEvent(ev);
                return true;
            }
        };
        panel.setClickable(true);
        GlassKit.keep(panel);  // keep the colours as built; the automatic glass pass would wash them out
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(10), dp(16), dp(12));
        panel.setBackground(cardGlass());
        panel.setAlpha(1f);
        // Higher than the verse card, so the verses cannot draw over the Notes card.
        panel.setElevation(dp(48));
        panel.setTranslationZ(dp(48));

        // Drag handle: drag up to enlarge, down to shrink.
        View handle = new View(getContext());
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(3));
        pill.setColor(Color.parseColor("#66808080"));
        handle.setBackground(pill);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(44), dp(5));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.setMargins(0, 0, 0, dp(8));
        panel.addView(handle, hlp);

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        final float[] floatStart = new float[2];
        final float[] floatOrigin = new float[2];
        header.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    floatStart[0] = ev.getRawX();
                    floatStart[1] = ev.getRawY();
                    floatOrigin[0] = panel.getTranslationX();
                    floatOrigin[1] = panel.getTranslationY();
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    // Float the card anywhere across the reader while the header is held.
                    panel.setTranslationX(floatOrigin[0] + ev.getRawX() - floatStart[0]);
                    panel.setTranslationY(floatOrigin[1] + ev.getRawY() - floatStart[1]);
                    return true;
                default:
                    return true;
            }
        });
        TextView title = GlassKit.text(getContext(), titleText, 16, true, notesInk());
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        if (actionLabel != null && action != null) {
            TextView add = GlassKit.orangeButton(getContext(), actionLabel, 12);
            add.setPadding(dp(14), 0, dp(14), 0);
            add.setOnClickListener(v -> action.run());
            header.addView(add, new LinearLayout.LayoutParams(-2, dp(34)));
        }
        TextView close = GlassKit.text(getContext(), "\u2715", 16, true, notesMuted());
        close.setPadding(dp(12), dp(4), dp(4), dp(4));
        close.setOnClickListener(v -> dismissNotesPanel());
        header.addView(close);
        panel.addView(header);

        notesPanelList = new LinearLayout(getContext());
        notesPanelList.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(notesPanelList);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        final int[] startHeight = new int[1];
        final float[] startY = new float[1];
        handle.setOnTouchListener((v, ev) -> {
            FrameLayout.LayoutParams plp = (FrameLayout.LayoutParams) panel.getLayoutParams();
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startHeight[0] = plp.height;
                    startY[0] = ev.getRawY();
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    int max = (int) (readerFrame.getHeight() * 0.85f);
                    plp.height = Math.max(dp(120), Math.min(max, startHeight[0] - (int) (ev.getRawY() - startY[0])));
                    panel.setLayoutParams(plp);
                    return true;
                default:
                    return true;
            }
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(330), Gravity.BOTTOM);
        lp.setMargins(dp(12), 0, dp(12), dp(104));
        // Pinch the card: spreading two fingers makes it taller and wider, pinching makes it smaller.
        pinch[0] = new android.view.ScaleGestureDetector(getContext(),
                new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(android.view.ScaleGestureDetector d) {
                        FrameLayout.LayoutParams plp = (FrameLayout.LayoutParams) panel.getLayoutParams();
                        float f = d.getScaleFactor();
                        int max = (int) (readerFrame.getHeight() * 0.9f);
                        plp.height = Math.max(dp(140), Math.min(max, (int) (panel.getHeight() * f)));
                        int side = Math.max(0, Math.min(dp(48), (int) (plp.leftMargin / Math.max(0.6f, f))));
                        plp.leftMargin = side;
                        plp.rightMargin = side;
                        panel.setLayoutParams(plp);
                        return true;
                    }
                });
        readerFrame.addView(panel, lp);
        panel.bringToFront();
        readerFrame.requestLayout();
        readerFrame.invalidate();
        notesPanelView = panel;
        return notesPanelList;
    }

    /** Dark mode: solid white card with dark text (F). Light mode: dark card with light text (G). */
    private int notesInk() {
        return GlassKit.light() ? Color.parseColor("#F5EEFF") : Color.parseColor("#2A0F33");
    }

    private int notesMuted() {
        return GlassKit.light() ? Color.parseColor("#D9C9F2") : Color.parseColor("#5B4A6E");
    }

    private void dismissNotesPanel() {
        if ("select".equals(cardKind)) {
            exitSelectMode();
            renderVerseList(isSearchMode ? searchResults : currentVerses, isSearchMode);
        }
        if (notesPanelView != null && readerFrame != null) {
            readerFrame.removeView(notesPanelView);
        }
        notesPanelView = null;
        notesPanelList = null;
        cardKind = "";
    }

    /** Reloads the notes inside the Notes card. */
    private void refreshNotesPanel() {
        bibleViewModel.loadNotes(notes -> {
            if (notesPanelList == null) {
                return;
            }
            notesPanelList.removeAllViews();
            if (notes.isEmpty()) {
                notesPanelList.addView(GlassKit.text(getContext(),
                        "No notes yet. Write a note and link the verses it talks about.", 13, false, notesMuted()));
            }
            for (final org.perazimchurch.app.domain.model.BibleNote note : notes) {
                String plain = android.text.Html.fromHtml(note.getBody(), android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim();
                TextView body = GlassKit.text(getContext(), plain.length() > 140 ? plain.substring(0, 140) + "..." : plain,
                        14, false, notesInk());
                body.setOnClickListener(v -> showNoteEditor(note));
                body.setPadding(0, dp(10), 0, dp(4));
                notesPanelList.addView(body);
                LinearLayout links = new LinearLayout(getContext());
                links.setOrientation(LinearLayout.HORIZONTAL);
                for (final org.perazimchurch.app.domain.model.VerseLocation loc : note.getLinks()) {
                    TextView chip = GlassKit.text(getContext(), BibleDataSeeder.canonicalBookName(loc.bookNumber)
                            + " " + loc.chapter + ":" + loc.verse, 12, true, GlassKit.light() ? GlassKit.accent() : Color.parseColor("#B8530F"));
                    chip.setPadding(dp(8), dp(4), dp(8), dp(4));
                    chip.setClickable(true);
                    chip.setOnClickListener(v -> {
                        dismissNotesPanel();
                        jumpToVerse(loc);
                    });
                    links.addView(chip);
                }
                notesPanelList.addView(links);
                if (!note.getTopics().isEmpty()) {
                    TextView tags = GlassKit.text(getContext(), "#" + android.text.TextUtils.join("  #", note.getTopics()),
                            11, true, COLOR_ACCENT_ORANGE());
                    notesPanelList.addView(tags);
                }
            }
        });
    }

    /** Opens a verse from a note link: loads its chapter and scrolls to the verse. */
    private void jumpToVerse(org.perazimchurch.app.domain.model.VerseLocation loc) {
        pendingScrollVerse = loc.verse;
        loadChapterVerses(BibleMapper.getBookIdFromNumber(loc.bookNumber), loc.chapter);
    }

    /** Full-screen note editor on the gradient background: rich text, linked verses and topics. */
    private View noteEditorView;

    private void showNoteEditor(@Nullable final org.perazimchurch.app.domain.model.BibleNote existing) {
        if (readerFrame == null) {
            return;
        }
        dismissNoteEditor();
        dismissNotesPanel();
        final List<org.perazimchurch.app.domain.model.VerseLocation> links = new ArrayList<>();
        if (existing != null) {
            links.addAll(existing.getLinks());
        }
        final LinearLayout editor = new LinearLayout(getContext());
        GlassKit.keep(editor);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setBackground(PerazimTheme.createBackgroundGradient());
        editor.setClickable(true);
        editor.setPadding(dp(16), dp(12), dp(16), dp(8));

        // Top bar: back, title, save
        LinearLayout top = new LinearLayout(getContext());
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView close = GlassKit.text(getContext(), "Back", 13, true, GlassKit.accent());
        close.setPadding(dp(8), dp(8), dp(8), dp(8));
        close.setOnClickListener(v -> dismissNoteEditor());
        top.addView(close);
        top.addView(GlassKit.text(getContext(), "Note", 15, true, GlassKit.text()), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView save = GlassKit.orangeButton(getContext(), "Save", 12);
        save.setPadding(dp(16), 0, dp(16), 0);
        top.addView(save, new LinearLayout.LayoutParams(-2, dp(34)));
        editor.addView(top);

        // Scrolling content: topic (H1), body, linked verses, verse reference
        ScrollView content = new ScrollView(getContext());
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        content.addView(column);
        editor.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Topic is the H1 heading at the very top of the note
        final EditText topicField = new EditText(getContext());
        topicField.setHint("Topic");
        topicField.setSingleLine(true);
        topicField.setBackground(null);
        topicField.setTypeface(Typeface.DEFAULT_BOLD);
        topicField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        topicField.setTextColor(GlassKit.text());
        topicField.setHintTextColor(GlassKit.muted());
        if (existing != null && !existing.getTopics().isEmpty()) {
            topicField.setText(existing.getTopics().get(0));
        }
        column.addView(topicField, GlassKit.matchWrap(getContext(), 8));

        // Body: first line can be the H2 subtopic; the description hint is overwritten by typing
        final EditText body = new EditText(getContext());
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setMinLines(6);
        body.setTypeface(Typeface.SERIF);
        body.setTextColor(GlassKit.text());
        body.setHint("Subtopic, then your notes. Use the toolbar to format.");
        body.setHintTextColor(GlassKit.muted());
        body.setBackground(null);
        if (existing != null && existing.getBody() != null) {
            body.setText(android.text.Html.fromHtml(existing.getBody(), android.text.Html.FROM_HTML_MODE_COMPACT));
        }
        column.addView(body, GlassKit.matchWrap(getContext(), 8));

        // Linked verses below the body
        final LinearLayout linkBox = new LinearLayout(getContext());
        linkBox.setOrientation(LinearLayout.VERTICAL);
        linkBox.setPadding(0, dp(12), 0, dp(12));
        column.addView(linkBox);
        final Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            linkBox.removeAllViews();
            for (final org.perazimchurch.app.domain.model.VerseLocation loc : links) {
                TextView row = GlassKit.text(getContext(), BibleDataSeeder.canonicalBookName(loc.bookNumber)
                        + " " + loc.chapter + ":" + loc.verse + "  ·  tap to remove", 13, false, GlassKit.accent());
                row.setPadding(0, dp(6), 0, dp(6));
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
        reference.setBackground(null);
        reference.setTextColor(GlassKit.text());
        reference.setHintTextColor(GlassKit.muted());
        column.addView(reference, GlassKit.matchWrap(getContext(), 8));

        if (existing != null) {
            TextView delete = GlassKit.text(getContext(), "Delete note", 12, true, COLOR_ACCENT_ORANGE());
            delete.setPadding(dp(8), dp(12), dp(8), dp(8));
            delete.setOnClickListener(v -> bibleViewModel.deleteNote(existing.getId(), () -> {
                Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                dismissNoteEditor();
                showNotesDialog();
            }));
            column.addView(delete);
        }

        // Formatting toolbar: docked above the keyboard while typing, or opened from the menu button.
        final LinearLayout dock = new LinearLayout(getContext());
        dock.setOrientation(LinearLayout.VERTICAL);
        final HorizontalScrollView toolbarScroll = new HorizontalScrollView(getContext());
        final LinearLayout toolbar = new LinearLayout(getContext());
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbarScroll.addView(toolbar);
        toolbarScroll.setVisibility(View.GONE);
        dock.addView(toolbarScroll, GlassKit.matchWrap(getContext(), 6));
        TextView menu = GlassKit.text(getContext(), "☰  Format", 13, true, GlassKit.accent());
        menu.setGravity(Gravity.CENTER);
        menu.setPadding(dp(14), dp(8), dp(14), dp(8));
        menu.setContentDescription("Show formatting tools");
        dock.addView(menu, new LinearLayout.LayoutParams(-2, -2));
        editor.addView(dock);

        toolbar.addView(toolButton("B", () -> applyFormat(body, new android.text.style.StyleSpan(Typeface.BOLD)), true));
        toolbar.addView(toolButton("I", () -> applyFormat(body, new android.text.style.StyleSpan(Typeface.ITALIC)), false));
        toolbar.addView(toolButton("U", () -> applyFormat(body, new android.text.style.UnderlineSpan()), false));
        toolbar.addView(toolButton("S", () -> applyFormat(body, new android.text.style.StrikethroughSpan()), false));
        toolbar.addView(toolButton("H1", () -> applyHeading(body, 2.0f), true));
        toolbar.addView(toolButton("H2", () -> applyHeading(body, 1.6f), true));
        toolbar.addView(toolButton("H3", () -> applyHeading(body, 1.3f), true));
        toolbar.addView(toolButton("H4", () -> applyHeading(body, 1.1f), true));
        toolbar.addView(toolButton("☐", () -> toggleCheckbox(body), false));
        final String[] fonts = {"serif", "sans-serif", "monospace"};
        final int[] fontIndex = {0};
        toolbar.addView(toolButton("Aa", () -> {
            fontIndex[0] = (fontIndex[0] + 1) % fonts.length;
            applyFormat(body, new android.text.style.TypefaceSpan(fonts[fontIndex[0]]));
        }, false));
        for (final HighlightColors.Colour colour : HighlightColors.ALL) {
            toolbar.addView(swatchView(colour.swatch, false, v -> applyFormat(body,
                    new android.text.style.BackgroundColorSpan(Color.parseColor(colour.tint)))));
        }
        toolbar.addView(toolButton("✕", () -> clearFormat(body), false));

        menu.setOnClickListener(v -> toolbarScroll.setVisibility(
                toolbarScroll.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        // Show the tools above the keyboard when it opens, and tuck them into the menu when it closes.
        final boolean[] keyboardUp = {false};
        editor.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect visible = new android.graphics.Rect();
            editor.getWindowVisibleDisplayFrame(visible);
            int hidden = editor.getRootView().getHeight() - visible.bottom;
            boolean up = hidden > dp(120);
            if (up != keyboardUp[0]) {
                keyboardUp[0] = up;
                toolbarScroll.setVisibility(up ? View.VISIBLE : View.GONE);
                menu.setVisibility(up ? View.GONE : View.VISIBLE);
            }
        });

        save.setOnClickListener(v -> {
            String ref = reference.getText().toString().trim();
            ScriptureReference parsed = ScriptureReference.parse(ref);
            if (!ref.isEmpty() && parsed != null && parsed.namesVerse()) {
                links.add(new org.perazimchurch.app.domain.model.VerseLocation(parsed.bookNumber, parsed.chapter, parsed.verseStart));
            }
            List<String> topics = new ArrayList<>();
            if (!topicField.getText().toString().trim().isEmpty()) {
                topics.add(topicField.getText().toString().trim());
            }
            String html = android.text.Html.toHtml(body.getText(), android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
            bibleViewModel.saveNote(existing == null ? 0L : existing.getId(), html, links, topics, id -> {
                Toast.makeText(getContext(), "Note saved", Toast.LENGTH_SHORT).show();
                dismissNoteEditor();
                showNotesDialog();
            });
        });

        readerFrame.addView(editor, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        noteEditorView = editor;
    }

    private void dismissNoteEditor() {
        if (noteEditorView != null && readerFrame != null) {
            readerFrame.removeView(noteEditorView);
        }
        noteEditorView = null;
    }

    private TextView toolButton(String label, Runnable action, boolean bold) {
        TextView button = GlassKit.text(getContext(), label, 13, bold, GlassKit.text());
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(40));
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable chip = new GradientDrawable();
        chip.setCornerRadius(dp(12));
        chip.setColor(GlassKit.light() ? Color.parseColor("#E6FFFFFF") : Color.parseColor("#CC2E1B4A"));
        button.setBackground(chip);
        button.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, 0, dp(6), 0);
        button.setLayoutParams(lp);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    /** Character range of the selection, or the line the cursor is on when nothing is selected. */
    private int[] formatRange(EditText body) {
        android.text.Editable text = body.getText();
        int start = body.getSelectionStart();
        int end = body.getSelectionEnd();
        if (start < 0 || end < 0) {
            start = end = text.length();
        }
        if (start == end) {
            int ls = start;
            while (ls > 0 && text.charAt(ls - 1) != '\n') {
                ls--;
            }
            int le = end;
            while (le < text.length() && text.charAt(le) != '\n') {
                le++;
            }
            return new int[]{ls, le};
        }
        return new int[]{Math.min(start, end), Math.max(start, end)};
    }

    private void applyFormat(EditText body, Object span) {
        int[] range = formatRange(body);
        if (range[0] < range[1]) {
            body.getText().setSpan(span, range[0], range[1], android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applyHeading(EditText body, float scale) {
        int[] range = formatRange(body);
        android.text.Editable text = body.getText();
        for (Object old : text.getSpans(range[0], range[1], android.text.style.RelativeSizeSpan.class)) {
            text.removeSpan(old);
        }
        if (range[0] < range[1]) {
            text.setSpan(new android.text.style.RelativeSizeSpan(scale), range[0], range[1], android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), range[0], range[1], android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** Puts an empty box at the start of the line, or ticks/unticks an existing one. */
    private void toggleCheckbox(EditText body) {
        android.text.Editable text = body.getText();
        int[] range = formatRange(body);
        int lineStart = range[0];
        if (lineStart < text.length() && (text.charAt(lineStart) == '☐' || text.charAt(lineStart) == '☑')) {
            text.replace(lineStart, lineStart + 1, text.charAt(lineStart) == '☐' ? "☑" : "☐");
        } else {
            text.insert(lineStart, "☐ ");
        }
    }

    private void clearFormat(EditText body) {
        int[] range = formatRange(body);
        android.text.Editable text = body.getText();
        for (Object span : text.getSpans(range[0], range[1], Object.class)) {
            if (span instanceof android.text.style.StyleSpan || span instanceof android.text.style.UnderlineSpan
                    || span instanceof android.text.style.StrikethroughSpan || span instanceof android.text.style.RelativeSizeSpan
                    || span instanceof android.text.style.BackgroundColorSpan || span instanceof android.text.style.TypefaceSpan) {
                text.removeSpan(span);
            }
        }
    }

    // =========================================================================
    // TOPICS (bookmark labels): the same frosted card as Notes
    // =========================================================================

    /**
     * Topics card. With a verse it also shows whether that verse is saved under each topic and lets the person
     * tick it; without a verse it just lists the topics.
     */
    private void showTopicsCard(@Nullable final BibleVerse verse) {
        if (notesPanelView != null && "topics".equals(cardKind) && verse == null) {
            dismissNotesPanel();
            return;
        }
        if (readerFrame == null) {
            return;
        }
        openFloatingCard("topics", verse == null ? "Topics" : "Topics for " + formatCitation(verse), null, null);
        refreshTopicsCard(verse, null);
    }

    private TextView cardText(String text, float sp, boolean bold, boolean muted) {
        return GlassKit.text(getContext(), text, sp, bold, muted ? notesMuted() : notesInk());
    }

    private void refreshTopicsCard(@Nullable final BibleVerse verse, @Nullable final String openTopic) {
        if (notesPanelList == null) {
            return;
        }
        if (openTopic != null) {
            showTopicDetail(verse, openTopic);
            return;
        }
        bibleViewModel.loadTopics(topics -> {
            if (notesPanelList == null || !"topics".equals(cardKind)) {
                return;
            }
            final Runnable[] reload = new Runnable[1];
            reload[0] = () -> refreshTopicsCard(verse, null);
            if (verse == null) {
                bibleViewModel.loadTopicsForVerse("", 0, 0, none -> fillTopics(topics, new ArrayList<>(), null));
            } else {
                bibleViewModel.loadTopicsForVerse(verse.getBookId(), verse.getChapterNumber(), verse.getVerseNumber(),
                        memberOf -> fillTopics(topics, memberOf, verse));
            }
        });
    }

    private void fillTopics(List<String> topics, final List<String> memberOf, @Nullable final BibleVerse verse) {
        if (notesPanelList == null) {
            return;
        }
        notesPanelList.removeAllViews();
        if (topics.isEmpty()) {
            notesPanelList.addView(cardText("No topics yet. Name one below, or save search results to a topic.", 13, false, true));
        }
        for (final String topic : topics) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            if (verse != null) {
                final boolean in = memberOf.contains(topic);
                TextView box = cardText(in ? "☑" : "☐", 20, true, false);
                box.setPadding(0, 0, dp(10), 0);
                box.setContentDescription((in ? "Remove from " : "Save to ") + topic);
                box.setOnClickListener(v -> bibleViewModel.setVerseTopic(verse.getBookId(), verse.getChapterNumber(),
                        verse.getVerseNumber(), topic, !in, () -> refreshTopicsCard(verse, null)));
                row.addView(box);
            }
            TextView name = cardText(topic, 15, true, false);
            name.setOnClickListener(v -> refreshTopicsCard(verse, topic));
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView open = GlassKit.text(getContext(), "View ›", 12, true, COLOR_ACCENT_ORANGE());
            open.setPadding(dp(10), dp(4), 0, dp(4));
            open.setOnClickListener(v -> refreshTopicsCard(verse, topic));
            row.addView(open);
            notesPanelList.addView(row);
        }
        // New topic: type a name and add; with a verse selected the verse is saved under it straight away.
        LinearLayout add = new LinearLayout(getContext());
        add.setOrientation(LinearLayout.HORIZONTAL);
        add.setGravity(Gravity.CENTER_VERTICAL);
        add.setPadding(0, dp(12), 0, 0);
        final EditText field = new EditText(getContext());
        field.setHint(verse == null ? "New topic, e.g. healing" : "Save this verse under a new topic");
        field.setSingleLine(true);
        field.setTextColor(notesInk());
        field.setHintTextColor(notesMuted());
        add.addView(field, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView go = GlassKit.orangeButton(getContext(), verse == null ? "Add" : "Save", 12);
        go.setPadding(dp(14), 0, dp(14), 0);
        go.setOnClickListener(v -> {
            String name = field.getText().toString().trim();
            if (name.isEmpty()) {
                return;
            }
            if (verse == null) {
                Toast.makeText(getContext(), "Topic “" + name + "” is created when you save a verse to it.", Toast.LENGTH_SHORT).show();
                return;
            }
            bibleViewModel.setVerseTopic(verse.getBookId(), verse.getChapterNumber(), verse.getVerseNumber(), name, true,
                    () -> refreshTopicsCard(verse, null));
        });
        add.addView(go, new LinearLayout.LayoutParams(-2, dp(36)));
        notesPanelList.addView(add);
    }

    /** One topic's saved verses inside the card. Tap a verse to jump straight to its place in the Bible. */
    private void showTopicDetail(@Nullable final BibleVerse verse, final String topic) {
        notesPanelList.removeAllViews();
        TextView back = GlassKit.text(getContext(), "‹ Topics   ·   " + topic, 14, true, COLOR_ACCENT_ORANGE());
        back.setPadding(0, dp(4), 0, dp(10));
        back.setOnClickListener(v -> refreshTopicsCard(verse, null));
        notesPanelList.addView(back);
        bibleViewModel.loadTopicVerses(topic, verses -> {
            if (notesPanelList == null || !"topics".equals(cardKind)) {
                return;
            }
            if (verses.isEmpty()) {
                notesPanelList.addView(cardText("No verses saved under this topic yet.", 13, false, true));
            }
            for (final BibleVerse saved : verses) {
                TextView ref = cardText(formatCitation(saved), 13, true, false);
                ref.setTextColor(COLOR_ACCENT_ORANGE());
                ref.setPadding(0, dp(10), 0, dp(2));
                notesPanelList.addView(ref);
                String text = saved.getText() == null ? "" : saved.getText();
                TextView body = cardText(text.length() > 160 ? text.substring(0, 160) + "..." : text, 14, false, false);
                View.OnClickListener jump = v -> {
                    dismissNotesPanel();
                    pendingScrollVerse = saved.getVerseNumber();
                    loadChapterVerses(saved.getBookId(), saved.getChapterNumber());
                };
                ref.setOnClickListener(jump);
                body.setOnClickListener(jump);
                notesPanelList.addView(body);
            }
        });
    }

    /** Saves every verse of the current search results under one topic: same card, with the topics as quick picks. */
    private void showSaveAllToTopic(final List<BibleVerse> verses) {
        if (readerFrame == null) {
            return;
        }
        LinearLayout list = openFloatingCard("topic-save", "Save " + verses.size() + " verses to a topic", null, null);
        final EditText name = new EditText(getContext());
        name.setSingleLine(true);
        name.setText(lastQuery);
        name.setTextColor(notesInk());
        name.setHintTextColor(notesMuted());
        name.setHint("Topic name, for example healing");
        list.addView(name);
        final LinearLayout chips = new LinearLayout(getContext());
        chips.setOrientation(LinearLayout.VERTICAL);
        list.addView(chips);
        bibleViewModel.loadTopics(topics -> {
            if (!"topic-save".equals(cardKind) || topics.isEmpty()) {
                return;
            }
            chips.addView(cardText("Or pick one you already have", 12, false, true));
            for (final String topic : topics) {
                TextView chip = GlassKit.text(getContext(), topic, 14, true, COLOR_ACCENT_ORANGE());
                chip.setPadding(0, dp(8), 0, dp(8));
                chip.setOnClickListener(v -> name.setText(topic));
                chips.addView(chip);
            }
        });
        TextView save = GlassKit.orangeButton(getContext(), "Save " + verses.size() + " verses", 13);
        save.setPadding(dp(18), 0, dp(18), 0);
        save.setOnClickListener(v -> {
            final String topic = name.getText().toString().trim();
            if (topic.isEmpty()) {
                return;
            }
            for (BibleVerse verse : verses) {
                bibleViewModel.setVerseTopic(verse.getBookId(), verse.getChapterNumber(),
                        verse.getVerseNumber(), topic, true, null);
            }
            Toast.makeText(getContext(), "Saved " + verses.size() + " verses under " + topic, Toast.LENGTH_SHORT).show();
            showTopicsCard(null);
            refreshTopicsCard(null, topic);
        });
        LinearLayout.LayoutParams lpSave = new LinearLayout.LayoutParams(-2, dp(40));
        lpSave.topMargin = dp(12);
        list.addView(save, lpSave);
    }

    /** Shows the same verse in every readable translation, one under the other. */
    /**
     * The frosted card used by the book picker, the chapter picker and Compare. It matches the Notes card:
     * the same glass, the same fonts and the same colours in light and dark mode.
     */
    private android.app.Dialog openCardDialog(String titleText, @Nullable String sub, View content,
                                              @Nullable String backLabel, @Nullable Runnable onBack) {
        final android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout card = new LinearLayout(getContext());
        GlassKit.keep(card);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(cardGlass());

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(GlassKit.text(getContext(), titleText, 16, true, notesInk()), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView close = GlassKit.text(getContext(), "✕", 16, true, notesMuted());
        close.setPadding(dp(12), dp(4), dp(4), dp(4));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close);
        card.addView(header);
        if (sub != null) {
            TextView subtitle = GlassKit.text(getContext(), sub, 12, false, notesMuted());
            subtitle.setPadding(0, dp(2), 0, dp(10));
            card.addView(subtitle);
        } else {
            card.addView(new View(getContext()), new LinearLayout.LayoutParams(-1, dp(8)));
        }
        // The list scrolls inside the card, and the card never grows taller than about two thirds of the screen.
        ScrollView scroll = new ScrollView(getContext()) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                int cap = (int) (getResources().getDisplayMetrics().heightPixels * 0.62f);
                super.onMeasure(widthSpec, View.MeasureSpec.makeMeasureSpec(cap, View.MeasureSpec.AT_MOST));
            }
        };
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(content);
        card.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        if (backLabel != null && onBack != null) {
            TextView back = GlassKit.text(getContext(), backLabel, 13, true, cardAccent());
            back.setPadding(0, dp(12), 0, dp(2));
            back.setOnClickListener(v -> {
                dialog.dismiss();
                onBack.run();
            });
            card.addView(back);
        }
        dialog.setContentView(card);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
            dialog.getWindow().setLayout((int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
        return dialog;
    }

    /** Gradient, rounded card surface shared by the word, picker and compare cards. Dark purple frosted in light mode, white in dark mode. */
    private GradientDrawable cardGlass() {
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                GlassKit.light()
                        ? new int[]{Color.parseColor("#F21C0E2A"), Color.parseColor("#E6140B20")}
                        : new int[]{Color.parseColor("#FFFFFF"), Color.parseColor("#F4EEF8")});
        glass.setCornerRadius(dp(24));
        glass.setStroke(dp(1), GlassKit.light() ? Color.parseColor("#40FFFFFF") : Color.parseColor("#40241238"));
        return glass;
    }

    /** Accent for text on the Notes-style card: bright orange on the dark card, deep orange on the white card. */
    private int cardAccent() {
        return GlassKit.light() ? Color.parseColor("#FF8A2B") : Color.parseColor("#B8530F");
    }

    private GradientDrawable cardTile(boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(14));
        if (selected) {
            d.setColor(Color.parseColor("#FF8A2B"));
        } else {
            d.setColor(GlassKit.light() ? Color.parseColor("#26FFFFFF") : Color.parseColor("#14000000"));
        }
        return d;
    }

    private void showBookSelectorDialog() {
        final LinearLayout body = new LinearLayout(getContext());
        body.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout tabs = new LinearLayout(getContext());
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        final TextView tabOt = GlassKit.text(getContext(), "Old Testament", 13, true, notesInk());
        final TextView tabNt = GlassKit.text(getContext(), "New Testament", 13, true, notesInk());
        for (TextView t : new TextView[]{tabOt, tabNt}) {
            t.setGravity(Gravity.CENTER);
            t.setClickable(true);
        }
        LinearLayout.LayoutParams lpOt = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpOt.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams lpNt = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpNt.setMargins(dp(4), 0, 0, 0);
        tabs.addView(tabOt, lpOt);
        tabs.addView(tabNt, lpNt);
        body.addView(tabs, GlassKit.matchWrap(getContext(), 0));

        final LinearLayout list = new LinearLayout(getContext());
        list.setOrientation(LinearLayout.VERTICAL);
        body.addView(list, GlassKit.matchWrap(getContext(), 10));

        final android.app.Dialog dialog = openCardDialog("Choose a book", "66 books, all available offline", body, null, null);
        final Runnable showOt = () -> {
            tabOt.setBackground(cardTile(true));
            tabOt.setTextColor(Color.WHITE);
            tabNt.setBackground(cardTile(false));
            tabNt.setTextColor(notesInk());
            populateBooksInContainer(list, "OLD", dialog);
        };
        final Runnable showNt = () -> {
            tabNt.setBackground(cardTile(true));
            tabNt.setTextColor(Color.WHITE);
            tabOt.setBackground(cardTile(false));
            tabOt.setTextColor(notesInk());
            populateBooksInContainer(list, "NEW", dialog);
        };
        tabOt.setOnClickListener(v -> showOt.run());
        tabNt.setOnClickListener(v -> showNt.run());
        BibleBook current = bookMap.get(currentBookId);
        if (current != null && "NEW".equalsIgnoreCase(current.getTestament())) {
            showNt.run();
        } else {
            showOt.run();
        }
    }

    private void populateBooksInContainer(LinearLayout container, String testament, final android.app.Dialog parentDialog) {
        container.removeAllViews();
        for (final BibleBook book : booksList) {
            if (!testament.equalsIgnoreCase(book.getTestament())) {
                continue;
            }
            boolean isCurrent = book.getId().equals(currentBookId);
            LinearLayout itemRow = new LinearLayout(getContext());
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding(dp(14), dp(11), dp(14), dp(11));
            itemRow.setBackground(cardTile(false));
            itemRow.setClickable(true);
            LinearLayout.LayoutParams lp = GlassKit.matchWrap(getContext(), 0);
            lp.bottomMargin = dp(6);
            itemRow.setLayoutParams(lp);

            TextView name = GlassKit.text(getContext(), book.getName(), 14, true, isCurrent ? cardAccent() : notesInk());
            itemRow.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
            itemRow.addView(GlassKit.text(getContext(), book.getChapterCount() + " ch", 12, false, notesMuted()));
            itemRow.setOnClickListener(v -> {
                parentDialog.dismiss();
                showChapterSelectorGrid(book);
            });
            container.addView(itemRow);
        }
    }

    private void showChapterSelectorGrid(final BibleBook book) {
        final LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        final android.app.Dialog[] holder = new android.app.Dialog[1];
        int columns = 5;
        int total = book.getChapterCount();
        LinearLayout currentRow = null;
        for (int ch = 1; ch <= total; ch++) {
            if ((ch - 1) % columns == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lpRow = GlassKit.matchWrap(getContext(), 0);
                lpRow.bottomMargin = dp(8);
                grid.addView(currentRow, lpRow);
            }
            final int chNum = ch;
            boolean isCurrent = book.getId().equals(currentBookId) && chNum == currentChapter;
            TextView cell = GlassKit.text(getContext(), String.valueOf(chNum), 14, true, isCurrent ? Color.WHITE : notesInk());
            cell.setGravity(Gravity.CENTER);
            cell.setBackground(cardTile(isCurrent));
            cell.setClickable(true);
            LinearLayout.LayoutParams lpCell = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lpCell.setMargins(dp(3), 0, dp(3), 0);
            cell.setOnClickListener(v -> {
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                pendingVersePicker = true;
                loadChapterVerses(book.getId(), chNum);
            });
            currentRow.addView(cell, lpCell);
        }
        if (currentRow != null) {
            for (int i = currentRow.getChildCount(); i < columns; i++) {
                View filler = new View(getContext());
                LinearLayout.LayoutParams lpHolder = new LinearLayout.LayoutParams(0, dp(46), 1f);
                lpHolder.setMargins(dp(3), 0, dp(3), 0);
                currentRow.addView(filler, lpHolder);
            }
        }
        holder[0] = openCardDialog(book.getName(), "Choose a chapter, 1 to " + total, grid,
                "‹ Back to books", this::showBookSelectorDialog);
    }

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
                for (BibleVerse version : versions) {
                    String name = translationNames.get(version.getTranslationId());
                    TextView head = GlassKit.text(getContext(),
                            version.getTranslationId() + (name != null ? " · " + name : ""), 12, true, cardAccent());
                    head.setLetterSpacing(0.06f);
                    head.setPadding(0, dp(12), 0, dp(4));
                    box.addView(head);
                    TextView body = GlassKit.text(getContext(), version.getText(), 15, false, notesInk());
                    body.setTypeface(Typeface.SERIF);
                    body.setLineSpacing(0f, 1.35f);
                    box.addView(body);
                }
                openCardDialog(citation, "Compare translations", box, null, null);
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
