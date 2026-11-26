package org.perazimchurch.app.presentation.ui;

import org.perazimchurch.app.ui.theme.AppDialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.ai.GuideSendGate;
import org.perazimchurch.app.ai.appcontext.AppContextComposer;
import org.perazimchurch.app.ai.appcontext.AppContextReader;
import org.perazimchurch.app.ai.appcontext.AppContextRouter;
import org.perazimchurch.app.ai.local.ModelDownloadService;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.perazimchurch.app.ai.remote.AiPreferences;
import org.perazimchurch.app.ai.remote.AiRelayClient;
import org.perazimchurch.app.ai.remote.ConversationHistory;
import org.perazimchurch.app.ai.local.OfflineKnowledgeStore;
import org.perazimchurch.app.ai.local.OfflineScriptureLookup;
import org.perazimchurch.app.ai.local.OnDeviceAi;
import org.perazimchurch.app.ai.local.GuideTurnRouter;
import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.R;
import org.perazimchurch.app.ui.theme.GlassKit;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Chatbot Interface for the Perazim Breakthrough Guide.
 * Provides built-in app help and explicit unavailability notices for unverified generative AI modes,
 * daily client/server rate limiting (3 questions/day), plain-words consent, and instant offline fail-soft.
 */
public class AiBreakthroughGuideDialog extends Dialog {

    public interface AiActionListener {
        void onNavigateTab(int tabIndex);
        void onOpenGiving();
        void onOpenLiveSermons();
        void onOpenBible(@Nullable String book);
        void onOpenGames();
        void onOpenProfile();
        void onLaunchRiddles();
        void onLaunchJokes();
        void onStartReflections();
    }

    /** Guide skills whose questions keep their existing safe path and never get an app-context answer. */
    private static final java.util.Set<String> SAFETY_SKILLS = new java.util.HashSet<>(java.util.Arrays.asList(
            "crisis", "pastoral", "doctrine", "privacy", "human-decision", "church-info", "refusal"));
    private static final int COLOR_PRIMARY_PURPLE = Color.parseColor("#681A7D");
    private static int COLOR_PURPLE_DARK() { return PerazimTheme.COLOR_TEXT_PRIMARY; }
    private static final int COLOR_PURPLE_SHADOW  = Color.parseColor("#450E53");
    private static final int COLOR_PURPLE_TINT    = Color.parseColor("#F5ECF7");
    private static int COLOR_BORDER_GREY() { return PerazimTheme.COLOR_STROKE; }
    private static final int COLOR_ACCENT_ORANGE  = Color.parseColor("#E17D2F");
    private static final int COLOR_ORANGE_SHADOW  = Color.parseColor("#98450B");
    private static final int COLOR_WHITE          = Color.parseColor("#FFFFFF");
    private static int COLOR_TEXT_DARK() { return PerazimTheme.COLOR_TEXT_PRIMARY; }
    private static int COLOR_TEXT_MUTED() { return PerazimTheme.COLOR_TEXT_SECONDARY; }
    private static int COLOR_BG_NEUTRAL() { return PerazimTheme.COLOR_BG_DEEP; }

    private final Activity activity;
    private final boolean light;
    private final GuideDictation dictation;
    private LinearLayout welcomePanel;
    private View chatPage, voicePanel;
    private TextView listeningTitle, listeningText;
    private AssistantMark.Orb listeningOrb;
    private final PerazimAiEngine aiEngine;
    private final AiActionListener actionListener;

    private ScrollView chatScrollView;
    private LinearLayout messagesContainer;
    private LinearLayout chipsContainer;
    private EditText etInput;
    private android.widget.ImageButton btnSend;
    private TextView tvSub;
    private boolean requestInFlight;
    private long requestStartedAt;
    private final ConversationHistory localHistory = new ConversationHistory();
    private Button btnMode;
    private boolean localRequest;
    private int knowledgeStatusGeneration;
    private String guideMode() {
        return activity.getSharedPreferences("perazim_guide_mode", Context.MODE_PRIVATE).getString("mode", "guide");
    }
    private void setGuideMode(String mode) {
        activity.getSharedPreferences("perazim_guide_mode", Context.MODE_PRIVATE).edit().putString("mode", mode).apply();
        updateSubtitle();
    }

    public AiBreakthroughGuideDialog(@NonNull Activity activity, @NonNull AiActionListener actionListener) {
        super(activity, org.perazimchurch.app.ui.theme.AppDialogs.windowTheme());
        this.activity = activity;
        light = PerazimTheme.isLight();
        dictation = new GuideDictation(activity, new GuideDictation.View() {
            @Override public void showListening(String status) {
                if (voicePanel.getVisibility() != View.VISIBLE) {
                    listeningText.setText("Speak your message");
                    android.view.inputmethod.InputMethodManager keyboard = activity.getSystemService(
                            android.view.inputmethod.InputMethodManager.class);
                    if (keyboard != null) keyboard.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
                    chatPage.setVisibility(View.INVISIBLE);
                    voicePanel.setVisibility(View.VISIBLE);
                }
                listeningTitle.setText(status);
                listeningOrb.animateListening("Listening…".equals(status));
            }

            @Override public void setLevel(float rms) { listeningOrb.setLevel(rms); }
            @Override public void showPartial(String text) {
                if (!text.isEmpty()) listeningText.setText(text);
            }

            @Override public void finishListening(String text, String notice) {
                listeningOrb.animateListening(false);
                voicePanel.setVisibility(View.GONE);
                chatPage.setVisibility(View.VISIBLE);
                if (!text.isEmpty()) {
                    String draft = etInput.getText().toString();
                    etInput.setText(draft.isEmpty() ? text : draft + " " + text);
                    etInput.setSelection(etInput.length());
                    etInput.announceForAccessibility("Dictation added to your draft");
                }
                if (!notice.isEmpty()) android.widget.Toast.makeText(activity, notice, android.widget.Toast.LENGTH_LONG).show();
            }
        });
        this.actionListener = actionListener;
        this.aiEngine = new PerazimAiEngine(activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(false);

        setContentView(buildRootLayout());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        updateSuggestionChips(java.util.Arrays.asList("Open the Bible", "Find my groups", "Start a reflection"));
        updateSubtitle();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        updateSubtitle();
    }

    private View buildRootLayout() {
        FrameLayout layers = new FrameLayout(activity);
        layers.setBackgroundColor(pageColor());
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(8), dp(16), dp(10));
        page.setFocusableInTouchMode(true);
        layers.addView(page, new FrameLayout.LayoutParams(-1, -1));
        chatPage = page;

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = control("‹", false);
        back.setContentDescription("Close Perazim Assistant");
        back.setTextSize(27);
        back.setOnClickListener(v -> dismiss());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout titles = new LinearLayout(activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, dp(6), 0);
        titles.addView(label("Perazim Assistant", 19, true));
        tvSub = label("", 12, false);
        tvSub.setTextColor(mutedColor());
        tvSub.setMaxLines(2);
        titles.addView(tvSub);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        btnMode = control("⋯", false);
        btnMode.setTextSize(22);
        btnMode.setContentDescription("Assistant settings and modes");
        btnMode.setOnClickListener(v -> {
            if (localRequest) OnDeviceAi.cancel();
            else if (!requestInFlight) showModeOptions();
        });
        header.addView(btnMode, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(header, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout conversation = new FrameLayout(activity);
        page.addView(conversation, new LinearLayout.LayoutParams(-1, 0, 1f));
        chatScrollView = new ScrollView(activity);
        chatScrollView.setFillViewport(true);
        chatScrollView.setClipToPadding(false);
        messagesContainer = new LinearLayout(activity);
        messagesContainer.setOrientation(LinearLayout.VERTICAL);
        messagesContainer.setPadding(0, dp(12), 0, dp(8));
        chatScrollView.addView(messagesContainer, new ScrollView.LayoutParams(-1, -2));
        conversation.addView(chatScrollView, new FrameLayout.LayoutParams(-1, -1));

        welcomePanel = new LinearLayout(activity);
        welcomePanel.setOrientation(LinearLayout.VERTICAL);
        welcomePanel.setGravity(Gravity.CENTER_HORIZONTAL);
        welcomePanel.setPadding(dp(12), 0, dp(12), 0);
        ImageView emblem = AssistantMark.image(activity, !light);
        welcomePanel.addView(emblem, new LinearLayout.LayoutParams(dp(107), dp(80)));
        TextView heading = label("What would you like to explore?", 27, true);
        heading.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        heading.setGravity(Gravity.CENTER);
        welcomePanel.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        TextView caption = label("Ask about Scripture or find your way around Perazim.", 14, false);
        caption.setTextColor(mutedColor());
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, dp(8), 0, 0);
        welcomePanel.addView(caption);
        conversation.addView(welcomePanel, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        chatScrollView.setVisibility(View.GONE);

        HorizontalScrollView suggestions = new HorizontalScrollView(activity);
        suggestions.setHorizontalScrollBarEnabled(false);
        chipsContainer = new LinearLayout(activity);
        suggestions.addView(chipsContainer);
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-1, -2);
        chipParams.bottomMargin = dp(8);
        page.addView(suggestions, chipParams);

        LinearLayout composer = new LinearLayout(activity);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(12), dp(5), dp(5), dp(5));
        composer.setBackground(panel(surfaceColor(), lineColor(), 22));
        etInput = new EditText(activity);
        etInput.setHint("Ask the Assistant");
        etInput.setHintTextColor(mutedColor());
        etInput.setTextColor(inkColor());
        etInput.setTextSize(16);
        etInput.setMinHeight(dp(48));
        etInput.setMaxLines(4);
        etInput.setBackground(null);
        etInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        etInput.setOnEditorActionListener((v, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_SEND) return false;
            submitQuery(etInput.getText().toString());
            return true;
        });
        composer.addView(etInput, new LinearLayout.LayoutParams(0, -2, 1f));

        android.widget.ImageButton mic = new android.widget.ImageButton(activity);
        mic.setImageResource(R.drawable.ic_assistant_wave);
        mic.setColorFilter(inkColor());
        mic.setBackground(panel(surfaceColor(), Color.TRANSPARENT, 22));
        mic.setContentDescription("Dictate a message");
        mic.setOnClickListener(v -> dictation.request());
        composer.addView(mic, new LinearLayout.LayoutParams(dp(48), dp(48)));
        btnSend = new android.widget.ImageButton(activity);
        btnSend.setImageResource(R.drawable.ic_assistant_send);
        btnSend.setColorFilter(pageColor());
        btnSend.setBackground(panel(inkColor(), lineColor(), 22));
        btnSend.setContentDescription("Send");
        btnSend.setOnClickListener(v -> submitQuery(etInput.getText().toString()));
        composer.addView(btnSend, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(composer, new LinearLayout.LayoutParams(-1, -2));

        voicePanel = buildListeningPanel();
        layers.addView(voicePanel, new FrameLayout.LayoutParams(-1, -1));
        voicePanel.setVisibility(View.GONE);
        return layers;
    }

    private View buildListeningPanel() {
        ScrollView screen = new ScrollView(activity);
        screen.setFillViewport(true);
        screen.setBackgroundColor(0xff090909);
        LinearLayout column = new LinearLayout(activity);
        column.setGravity(Gravity.CENTER);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(22), dp(18), dp(22), dp(18));
        FrameLayout emblem = new FrameLayout(activity);
        listeningOrb = new AssistantMark.Orb(activity);
        emblem.addView(listeningOrb, new FrameLayout.LayoutParams(-1, -1));
        emblem.addView(AssistantMark.image(activity, true),
                new FrameLayout.LayoutParams(dp(91), dp(91), Gravity.CENTER));
        column.addView(emblem, new LinearLayout.LayoutParams(dp(228), dp(228)));
        listeningTitle = label("Listening…", 28, true);
        listeningTitle.setTextColor(Color.WHITE);
        listeningTitle.setGravity(Gravity.CENTER);
        listeningTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        column.addView(listeningTitle);
        listeningText = label("Speak your message", 16, false);
        listeningText.setTextColor(0xffCCCCCC);
        listeningText.setGravity(Gravity.CENTER);
        listeningText.setPadding(0, dp(14), 0, dp(26));
        listeningText.setMaxLines(6);
        column.addView(listeningText, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(activity);
        actions.setGravity(Gravity.CENTER);
        Button cancel = control("Cancel", false);
        cancel.setTextColor(Color.WHITE);
        cancel.setBackground(panel(0xff202020, 0xff535353, 22));
        cancel.setOnClickListener(v -> dictation.cancel());
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button stop = control("Stop listening", true);
        stop.setTextColor(Color.BLACK);
        stop.setBackground(panel(Color.WHITE, Color.WHITE, 22));
        stop.setOnClickListener(v -> dictation.stop());
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        stopParams.leftMargin = dp(12);
        actions.addView(stop, stopParams);
        column.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        screen.addView(column);
        return screen;
    }

    private int pageColor() { return light ? 0xffFAFAFA : 0xff111111; }
    private int surfaceColor() { return light ? Color.WHITE : 0xff202020; }
    private int lineColor() { return light ? 0xffD9D9D9 : 0xff515151; }
    private int inkColor() { return light ? 0xff171717 : 0xffF5F5F5; }
    private int mutedColor() { return light ? 0xff5B5B5B : 0xffC0C0C0; }

    private GradientDrawable panel(int fill, int line, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(radius));
        if (line != Color.TRANSPARENT) background.setStroke(dp(1), line);
        return background;
    }

    private TextView label(String text, int size, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(inkColor());
        view.setTextSize(size);
        view.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return view;
    }

    private Button control(String text, boolean primary) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(primary ? pageColor() : inkColor());
        button.setTextSize(14);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setBackground(panel(primary ? inkColor() : surfaceColor(), lineColor(), 22));
        return button;
    }

    public void onMicrophonePermission(boolean granted) {
        if (isShowing()) dictation.onPermissionResult(granted);
    }

    public void pauseDictation() { dictation.pause(); }

    @Override public void onBackPressed() {
        if (voicePanel != null && voicePanel.getVisibility() == View.VISIBLE) dictation.cancel();
        else super.onBackPressed();
    }

    private void updateSubtitle() {
        if (tvSub == null) return;
        int statusGeneration = ++knowledgeStatusGeneration;
        String mode = guideMode();
        if ("local".equals(mode)) {
            if (!BuildConfig.OFFLINE_QWEN_ENABLED) {
                tvSub.setText("Unavailable until approved knowledge trust is provisioned");
            } else if (!OnDeviceAi.runtimeAvailable(activity)) {
                tvSub.setText("Qwen · llama.cpp runtime unavailable on this device");
            } else if (!OnDeviceAi.isInstalled(activity)) {
                tvSub.setText("Qwen · Model download required");
            } else {
                tvSub.setText("Qwen · Verifying the installed model…");
                OnDeviceAi.checkModel(activity, modelVerified -> {
                    if (statusGeneration != knowledgeStatusGeneration || !isShowing()
                            || !"local".equals(guideMode()) || tvSub == null) return;
                    if (!modelVerified) {
                        tvSub.setText("Qwen · Model integrity check failed · download to repair");
                        return;
                    }
                    java.util.Map<String, String> trustedKeys = OfflineKnowledgeStore.trustedKeys(activity);
                    org.perazimchurch.app.core.concurrency.BoundedExecutors.io().execute(() -> {
                        String knowledge = OfflineKnowledgeStore.knowledgeStatus(activity, trustedKeys,
                                BuildConfig.VERSION_NAME, System.currentTimeMillis());
                        activity.runOnUiThread(() -> {
                            if (statusGeneration != knowledgeStatusGeneration || !isShowing()
                                    || !"local".equals(guideMode()) || tvSub == null) return;
                            tvSub.setText("Qwen · " + knowledge);
                        });
                    });
                });
            }
            tvSub.setTextColor(mutedColor());
            tvSub.setOnClickListener(v -> { if (!requestInFlight) showModeOptions(); });
            return;
        }
        if ("guide".equals(mode)) {
            tvSub.setText("Built-in app help · Not a generative model");
            tvSub.setOnClickListener(v -> { if (!requestInFlight) showModeOptions(); });
            return;
        }
        if ("online".equals(mode) && !BuildConfig.ONLINE_AI_ENABLED) {
            tvSub.setText("Online AI unavailable in this build");
            tvSub.setTextColor(mutedColor());
            tvSub.setOnClickListener(null);
            return;
        }
        tvSub.setText("Online AI · Church service");
        tvSub.setTextColor(mutedColor());
        tvSub.setOnClickListener(v -> { if (!requestInFlight) showConsentDialog(); });
    }

    private void showModeOptions() {
        boolean canUpdateKnowledge = BuildConfig.OFFLINE_QWEN_ENABLED
                && !OfflineKnowledgeStore.trustedKeys(activity).isEmpty();
        List<String> choices = new ArrayList<>();
        choices.add(BuildConfig.OFFLINE_QWEN_ENABLED ? "On-phone AI" : "On-phone AI — unavailable");
        choices.add(BuildConfig.OFFLINE_QWEN_ENABLED ? "Model download" : "Model download — disabled");
        choices.add(BuildConfig.ONLINE_AI_ENABLED ? "Online AI" : "Online AI — unavailable");
        choices.add("Offline app help");
        int updateIndex = -1;
        int rollbackIndex = -1;
        if (canUpdateKnowledge) {
            updateIndex = choices.size();
            choices.add("Update approved offline knowledge");
            if (OfflineKnowledgeStore.hasRollbackCandidate(activity)) {
                rollbackIndex = choices.size();
                choices.add("Restore previous signed knowledge package");
            }
        }
        int memoryIndex = choices.size();
        choices.add("What the guide remembers");
        choices.add("Clear this conversation");
        final int appActivityIndex = choices.size();
        choices.add(AppContextReader.isEnabled(activity)
                ? "App activity for answers: on (tap to turn off)"
                : "App activity for answers: off (tap to turn on)");
        final int selectedUpdateIndex = updateIndex;
        final int selectedRollbackIndex = rollbackIndex;
        final int selectedMemoryIndex = memoryIndex;
        AppDialogs.builder(activity).setTitle("Choose your guide")
                .setItems(choices.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) setGuideMode("local");
                    else if (which == 1) showModelDownload();
                    else if (which == 2) setGuideMode("online");
                    else if (which == 3) setGuideMode("guide");
                    else if (which == selectedUpdateIndex) showKnowledgeUpdate();
                    else if (which == selectedRollbackIndex) showKnowledgeRollback();
                    else if (which == selectedMemoryIndex) GuideMemoryDialog.show(activity);
                    else if (which == appActivityIndex) toggleAppActivity();
                    else {
                        localHistory.clear();
                        SoftSafety.reset();
                        messagesContainer.removeAllViews();
                        displayAiMessage(AiResponse.plainText("Conversation cleared. What would you like to discuss?", null), "Perazim Guide");
                    }
                }).setNegativeButton("Close", null).show();
    }

    private void toggleAppActivity() {
        boolean enable = !AppContextReader.isEnabled(activity);
        AppContextReader.setEnabled(activity, enable);
        android.widget.Toast.makeText(activity, enable
                ? "The guide can use your app activity to answer. It stays on this phone."
                : "The guide will not use your app activity.", android.widget.Toast.LENGTH_LONG).show();
    }

    private void showKnowledgeRollback() {
        java.util.Map<String, String> trustedKeys = OfflineKnowledgeStore.trustedKeys(activity);
        String sessionToken = SessionManager.getInstance(activity).getAuthToken();
        if (trustedKeys.isEmpty()) {
            AppDialogs.builder(activity).setMessage("This build has no trusted knowledge signing key.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            AppDialogs.builder(activity).setMessage("Sign in before checking for an authorized knowledge rollback.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        AlertDialog progress = AppDialogs.builder(activity)
                .setTitle("Checking authorized rollback")
                .setMessage("Verifying the signed authorization for the previous package…")
                .setCancelable(false).create();
        progress.show();
        OfflineKnowledgeStore.downloadAndInstallAuthorizedRollback(activity, sessionToken, trustedKeys,
                new OfflineKnowledgeStore.UpdateCallback() {
                    @Override public void onInstalled(OfflineKnowledgeStore.VerifiedPackage restored) {
                        progress.dismiss();
                        if (!isShowing()) return;
                        setGuideMode("local");
                        displayAiMessage(AiResponse.plainText(
                                "The authorized previous knowledge package was verified and restored.", null),
                                "Offline knowledge rollback · " + restored.version);
                    }

                    @Override public void onError(String message) {
                        progress.dismiss();
                        if (isShowing()) AppDialogs.builder(activity).setMessage(message)
                                .setPositiveButton("OK", null).show();
                    }
                });
    }

    private void showKnowledgeUpdate() {
        java.util.Map<String, String> trustedKeys = OfflineKnowledgeStore.trustedKeys(activity);
        if (trustedKeys.isEmpty()) {
            AppDialogs.builder(activity).setMessage("This build has no trusted knowledge signing key.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String sessionToken = SessionManager.getInstance(activity).getAuthToken();
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            AppDialogs.builder(activity).setMessage("Sign in before downloading approved offline knowledge.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        OfflineKnowledgeStore.UpdateStatus previous = OfflineKnowledgeStore.lastUpdateStatus(activity);
        String previousSuccess = previous.lastSuccessVersion.isEmpty()
                ? "No successful knowledge update is recorded."
                : "Last successful sync: "
                + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(previous.lastSuccessAt))
                + " (verified package version " + previous.lastSuccessVersion + ").";
        AlertDialog progress = AppDialogs.builder(activity)
                .setTitle("Updating offline knowledge")
                .setMessage(previousSuccess + "\n\nDownloading and verifying the signed package…")
                .setCancelable(false).create();
        progress.show();
        OfflineKnowledgeStore.downloadAndInstall(activity, sessionToken, trustedKeys,
                new OfflineKnowledgeStore.UpdateCallback() {
                    @Override public void onInstalled(OfflineKnowledgeStore.VerifiedPackage installed) {
                        progress.dismiss();
                        if (!isShowing()) return;
                        setGuideMode("local");
                        displayAiMessage(AiResponse.plainText(
                                "Approved offline knowledge was verified and installed. It is now available to on-phone AI.", null),
                                "Offline knowledge updated · " + installed.version);
                    }

                    @Override public void onError(String message) {
                        progress.dismiss();
                        if (isShowing()) AppDialogs.builder(activity).setMessage(message)
                                .setPositiveButton("OK", null).show();
                    }
                });
    }

    private void showModelDownload() {
        if (!BuildConfig.OFFLINE_QWEN_ENABLED) {
            AppDialogs.builder(activity)
                    .setMessage("On-device AI downloads are disabled until an approved knowledge package and trusted signing key are provisioned.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String modelMessage = "validation".equals(BuildConfig.BUILD_TYPE)
                ? "Validation only: verifies or repairs the installed model from the locally staged Qwen artifact and uses a synthetic signed knowledge passage."
                : "This checks the installed model integrity first. If it is missing or damaged, the verified model is downloaded and installed.";
        AppDialogs.builder(activity).setTitle("Download on-phone AI")
                .setMessage(modelMessage)
                .setNegativeButton("Not now", null)
                .setPositiveButton("Check / download", (dialog, which) -> showDownloadProgress(false))
                .show();
    }

    /**
     * Shows download progress without blocking the app. Back or X dismisses the card and the download keeps
     * running in the foreground service; "Cancel download" stops it. Reopening the guide shows progress again.
     */
    private void showDownloadProgress(boolean reattach) {
        if (activity == null || activity.isFinishing()) return;
        if (!reattach) ModelDownloadService.start(activity);
        final AlertDialog progress = AppDialogs.builder(activity).setTitle("Downloading on-phone model")
                .setMessage(ModelDownloadService.isRunning()
                        ? ModelDownloadService.percent() + "% · You can leave the app; the download continues."
                        : "Starting…")
                .setCancelable(true)
                .setPositiveButton("Run in background", null)
                .setNegativeButton("Cancel download", (d, w) -> ModelDownloadService.cancel(activity))
                .create();
        progress.show();
        ModelDownloadService.setListener(new ModelDownloadService.Listener() {
            public void onProgress(int percent) {
                if (progress.isShowing()) progress.setMessage(percent + "% · You can leave the app; the download continues.");
            }
            public void onFinished(String error) {
                ModelDownloadService.setListener(null);
                if (progress.isShowing()) progress.dismiss();
                if (!isShowing()) return;
                if (error == null) {
                    setGuideMode("local");
                    displayAiMessage(AiResponse.plainText("The model is ready. Ask your question again, or choose On-phone AI.", null), "Perazim Guide · on-phone");
                } else {
                    AppDialogs.builder(activity).setMessage(error).setPositiveButton("OK", null).show();
                }
            }
        });
    }

    @Override public void dismiss() {
        dictation.cancel();
        if (localRequest) OnDeviceAi.cancel();
        localHistory.clear();
        SoftSafety.reset();
        super.dismiss();
    }

    public void showConsentDialog() {
        if (activity == null || activity.isFinishing()) return;
        if (!BuildConfig.ONLINE_AI_ENABLED) {
            AppDialogs.builder(activity)
                    .setMessage("Online AI is unavailable in this build while the authenticated church service is being verified.")
                    .setPositiveButton("OK", null).show();
            return;
        }

        final Dialog consentDlg = new Dialog(activity, org.perazimchurch.app.ui.theme.AppDialogs.windowTheme());
        consentDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        consentDlg.setCanceledOnTouchOutside(false);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PerazimTheme.COLOR_SURFACE_SOLID);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), COLOR_BORDER_GREY());
        layout.setBackground(bg);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(activity);
        title.setText("🤖 Google Gemini AI Guide");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK());
        layout.addView(title);

        TextView body = new TextView(activity);
        body.setText("Online AI sends your current question to the church service. If the request is eligible, Gemini may process it with approved source excerpts. Keep personal, prayer and counselling details out of online questions.");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        body.setTextColor(COLOR_TEXT_DARK());
        body.setPadding(0, dp(12), 0, dp(10));
        body.setLineSpacing(dp(2), 1.1f);
        layout.addView(body);

        final CheckBox cbConsent = new CheckBox(activity);
        cbConsent.setText("I understand and agree");
        cbConsent.setTextColor(COLOR_PURPLE_DARK());
        cbConsent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        layout.addView(cbConsent);

        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        btnRow.setPadding(0, dp(16), 0, 0);

        Button btnDecline = new Button(activity);
        btnDecline.setText("Offline Guide");
        btnDecline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnDecline.setTextColor(COLOR_TEXT_MUTED());
        btnDecline.setBackground(null);
        btnDecline.setOnClickListener(v -> {
            AiPreferences.setConsent(activity, false);
            consentDlg.dismiss();
            updateSubtitle();
        });
        btnRow.addView(btnDecline);

        final Button btnAccept = new Button(activity);
        btnAccept.setText("Continue");
        btnAccept.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAccept.setTypeface(Typeface.DEFAULT_BOLD);
        btnAccept.setTextColor(COLOR_WHITE);
        btnAccept.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 8, 2));
        btnAccept.setEnabled(false);
        btnAccept.setAlpha(0.5f);
        btnAccept.setPadding(dp(16), dp(8), dp(16), dp(8));

        cbConsent.setOnCheckedChangeListener((btn, isChecked) -> {
            btnAccept.setEnabled(isChecked);
            btnAccept.setAlpha(isChecked ? 1.0f : 0.5f);
        });

        btnAccept.setOnClickListener(v -> {
            AiPreferences.setConsent(activity, true);
            consentDlg.dismiss();
            updateSubtitle();
        });
        btnRow.addView(btnAccept);

        layout.addView(btnRow);

        consentDlg.setContentView(layout);
        if (consentDlg.getWindow() != null) {
            consentDlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int w = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90);
            consentDlg.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        consentDlg.show();
    }

    /**
     * Answers from the member's own app activity. Reads run on a worker thread and never send anything off the phone.
     * The answer is not added to the conversation history, so personal facts stay out of later model prompts.
     */
    private void answerFromAppContext(final String query, final AppContextRouter.Route route) {
        localHistory.clear();
        if (!AppContextReader.isEnabled(activity)) {
            displayAiMessage(appContextResponse(AppContextComposer.disabled()), "App activity is off");
            return;
        }
        if (route.has(AppContextRouter.Topic.PRIVATE_BLOCKED)) {
            displayAiMessage(appContextResponse(AppContextComposer.privateRefusal()), "Perazim Guide · built-in");
            return;
        }
        final View indicator = showTypingIndicator();
        beginRequest();
        btnSend.setEnabled(false);
        final java.util.Set<AppContextRouter.Topic> topics = new java.util.HashSet<>(route.topics);
        org.perazimchurch.app.core.concurrency.BoundedExecutors.io().execute(() -> {
            final AppContextComposer.Reply reply = AppContextComposer.compose(route,
                    AppContextReader.read(activity, topics), query);
            activity.runOnUiThread(() -> {
                removeTypingIndicator(indicator);
                finishInFlight();
                if (!isShowing()) return;
                displayAiMessage(appContextResponse(reply), "From this phone · your app activity");
            });
        });
    }

    /**
     * True when a question touches crisis, pastoral, human-decision, professional or out-of-scope matters. Those never get
     * an app-context answer; they keep their existing safe path.
     */
    private boolean needsSafetyPath(String query) {
        if (PerazimAiEngine.isCrisisQuery(query)
                || PerazimAiEngine.isSensitivePastoralDisclosure(query)
                || PerazimAiEngine.isProfessionalMatterQuery(query)
                || PerazimAiEngine.isHumanDecisionQuery(query)
                || PerazimAiEngine.isOutOfScopeForModel(query)
                || org.perazimchurch.app.ai.guide.GuideSafety.isSensitive(query)) {
            return true;
        }
        // The guide's own skill router: any pastoral, doctrine, privacy, human-decision, church or refusal skill keeps
        // its existing path. Scripture lookups and the app's own help are not blocked.
        for (org.perazimchurch.app.ai.guide.GuideDocs.SkillEntry skill
                : org.perazimchurch.app.ai.guide.GuideRouter.route(query,
                org.perazimchurch.app.ai.guide.GuideRuntime.skillIndex(activity), 3)) {
            if (SAFETY_SKILLS.contains(skill.id)) return true;
        }
        return false;
    }

    private static AiResponse appContextResponse(AppContextComposer.Reply reply) {
        if (reply.hasAction()) {
            return AiResponse.withAction(reply.text, reply.actionIntent, reply.actionLabel, reply.actionPayload, reply.chips);
        }
        return AiResponse.plainText(reply.text, reply.chips);
    }

    private void submitQuery(String text) {
        if (text == null || text.trim().isEmpty()) return;
        final String query = text.trim();

        // Safety routing is mode-independent: selecting Qwen or cloud mode must not
        // bypass the deterministic immediate-support response.
        // Two-step check: a vague exhaustion phrase gets the warm check; crisis wording or a yes or plan after it gets the full response.
        AiResponse safetyFirst = PerazimAiEngine.safetyFirstResponse(query);

        // A safety message is never blocked by an earlier request still running (Levi, 2026-11-25). A stuck earlier request
        // releases after GuideSendGate.STALE_AFTER_MS. Anything else sent while a request runs gets a visible notice and the
        // words stay in the box, so nothing is silently dropped.
        if (!GuideSendGate.mayAccept(requestInFlight, requestStartedAt, System.currentTimeMillis(), safetyFirst != null)) {
            displayAiMessage(AiResponse.plainText(
                    "I'm still answering your last question. Please wait a moment, then send again.", null), "Perazim Guide · built-in");
            return;
        }
        if (requestInFlight) {
            // A request that never answered: release the lock so this message can go through.
            localRequest = false;
            finishInFlight();
        }
        etInput.setText("");

        // 1. Render User Message Bubble
        displayUserMessage(query);

        // One routing order for every message: safety, the vague-exhaustion check, built-in answers, then the approved
        // routes and the composed conversational replies (GuideTurnRouter.respond). The evaluation calls the same method.
        // The routing can run the on-phone model, so it runs off the UI thread and the reply is posted back.
        final View routing = showTypingIndicator();
        beginRequest();
        btnSend.setEnabled(false);
        org.perazimchurch.app.core.concurrency.BoundedExecutors.io().execute(() -> {
            GuideTurnRouter.Turn turn = GuideTurnRouter.respond(activity, aiEngine, query, localHistory.snapshot());
            activity.runOnUiThread(() -> {
                removeTypingIndicator(routing);
                finishInFlight();
                if (!isShowing()) return;
                if (turn.response != null) {
                    if (turn.clearHistory) localHistory.clear();
                    else if (turn.status != null && turn.status.startsWith(GuideTurnRouter.COMPOSED_PREFIX)) {
                        localHistory.addExchange(query, turn.response.getMessageText());
                    }
                    displayAiMessage(turn.response, turn.badge);
                    return;
                }
                continueAfterRoute(query);
            });
        });
    }

    /** The app-context, on-phone model, offline Bible and online paths, reached when no fixed or composed reply answered. */
    private void continueAfterRoute(String query) {
        // Questions about the member's own app use (games, notes, sermons, community, routine) are answered from
        // compact facts on this phone. Crisis and scope checks above have already run.
        AppContextRouter.Route appRoute = needsSafetyPath(query) ? null : AppContextRouter.route(query);
        if (appRoute != null) {
            answerFromAppContext(query, appRoute);
            return;
        }
        String mode = guideMode();
        if ("local".equals(mode)) {
            if (!BuildConfig.OFFLINE_QWEN_ENABLED) {
                displayAiMessage(AiResponse.plainText("On-device AI is unavailable until an approved knowledge package and trusted signing key are provisioned. Built-in app help remains available.", null), "On-phone AI unavailable");
                return;
            }
            if (!OnDeviceAi.isInstalled(activity)) {
                displayAiMessage(AiResponse.plainText("Download the on-phone model to have a real AI conversation without internet. Use the mode button above to set it up. Built-in app help remains available separately.", null), "Model not installed");
                return;
            }
            final View indicator = showTypingIndicator();
            beginRequest(); localRequest = true; btnSend.setEnabled(false);
            if (btnMode != null) {
                btnMode.setText("■");
                btnMode.setContentDescription("Stop response");
            }
            OnDeviceAi.ask(activity, query, localHistory.snapshot(), new OnDeviceAi.Callback() {
                public void onSuccess(String answer) {
                    removeTypingIndicator(indicator); localRequest = false; finishInFlight(); updateSubtitle();
                    if (!isShowing()) return;
                    String result = ConversationHistory.withoutRepeatedGreeting(answer, !localHistory.isEmpty());
                    localHistory.addExchange(query, result);
                    displayAiMessage(AiResponse.plainText(result, null), "On-phone AI · Qwen · Offline");
                    GuideFeedbackRow.attach(activity, messagesContainer, query, result);
                }
                public void onError(String error) {
                    removeTypingIndicator(indicator); localRequest = false; finishInFlight(); updateSubtitle();
                    if (isShowing()) displayAiMessage(AiResponse.plainText(error, null), "On-phone model notice");
                }
            });
            return;
        }
        if ("guide".equals(mode)) {
            if (!OfflineScriptureLookup.matchesRequest(query)) {
                displayAiMessage(aiEngine.processQuery(query), "Offline app help · Built-in rules");
                return;
            }
            final View indicator = showTypingIndicator();
            beginRequest();
            btnSend.setEnabled(false);
            org.perazimchurch.app.core.concurrency.BoundedExecutors.io().execute(() -> {
                AiResponse scripture = OfflineScriptureLookup.answer(activity, query);
                activity.runOnUiThread(() -> {
                    removeTypingIndicator(indicator);
                    finishInFlight();
                    if (!isShowing()) return;
                    if (scripture != null) {
                        displayAiMessage(scripture, "Offline Bible lookup");
                    } else {
                        displayAiMessage(aiEngine.processQuery(query), "Offline app help · Built-in rules");
                    }
                });
            });
            return;
        }

        if (!BuildConfig.ONLINE_AI_ENABLED) {
            displayAiMessage(AiResponse.plainText("Online AI is unavailable in this build while an authenticated church service is being verified. Built-in app help remains available.", null), "Online AI unavailable");
            return;
        }

        if (!AiPreferences.hasConsented(activity)) {
            displayAiMessage(AiResponse.plainText(
                    "Review the online AI disclosure before sending your question.", null), "Online AI");
            showConsentDialog();
            return;
        }

        final View typingIndicator = showTypingIndicator();
        beginRequest();
        btnSend.setEnabled(false);
        AiRelayClient.askQuestion(activity, query, new AiRelayClient.RelayCallback() {
            @Override public void onSuccess(@NonNull String answer, int remaining, boolean generated) {
                removeTypingIndicator(typingIndicator);
                finishInFlight();
                if (!isShowing()) return;
                String badge = generated ? "Online AI · Gemini" : "Church service notice";
                if (remaining >= 0) badge += " · " + remaining + " questions left";
                displayAiMessage(AiResponse.plainText(answer, null), badge);
            }

            @Override public void onError(@NonNull String message) {
                removeTypingIndicator(typingIndicator);
                finishInFlight();
                if (isShowing()) displayAiMessage(AiResponse.plainText(message, null), "Online AI");
            }
        });
    }

    /** Marks a request as running and records when it started, so a request that never answers cannot lock the chat. */
    private void beginRequest() {
        requestInFlight = true;
        requestStartedAt = System.currentTimeMillis();
    }

    private void finishInFlight() {
        requestInFlight = false;
        if (btnSend != null) btnSend.setEnabled(true);
        if (btnMode != null) {
            btnMode.setText("⋯");
            btnMode.setContentDescription("Assistant settings and modes");
        }
    }

    private View showTypingIndicator() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), dp(40), dp(4));

        TextView tv = new TextView(activity);
        tv.setText("Looking for an answer…");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tv.setTextColor(mutedColor());
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        tv.setBackground(panel(surfaceColor(), lineColor(), 16));

        row.addView(tv);
        messagesContainer.addView(row);
        scrollToBottom();
        return row;
    }

    private void removeTypingIndicator(View indicator) {
        requestInFlight = false;
        if (btnSend != null) btnSend.setEnabled(true);
        if (!isShowing()) return;
        if (indicator != null && messagesContainer != null) {
            messagesContainer.removeView(indicator);
        }
    }

    private void showConversation() {
        welcomePanel.setVisibility(View.GONE);
        chatScrollView.setVisibility(View.VISIBLE);
    }

    private void displayUserMessage(String text) {
        showConversation();
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.setPadding(dp(48), dp(4), 0, dp(8));

        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(pageColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        tv.setLineSpacing(dp(2), 1.1f);
        tv.setPadding(dp(16), dp(10), dp(16), dp(10));
        tv.setBackground(panel(inkColor(), Color.TRANSPARENT, 16));

        row.addView(tv);
        messagesContainer.addView(row);
        scrollToBottom();
    }

    private void displayAiMessage(AiResponse response, @Nullable String badgeText) {
        showConversation();
        LinearLayout bubbleCol = new LinearLayout(activity);
        bubbleCol.setOrientation(LinearLayout.VERTICAL);
        bubbleCol.setPadding(0, dp(4), dp(24), dp(10));

        // Badge pill
        if (badgeText != null && !badgeText.isEmpty()) {
            TextView tvBadge = new TextView(activity);
            tvBadge.setText(badgeText);
            tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvBadge.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            tvBadge.setTextColor(mutedColor());
            tvBadge.setPadding(dp(4), 0, 0, dp(6));
            bubbleCol.addView(tvBadge);
        }

        // Message text
        TextView tv = new TextView(activity);
        tv.setText(response.getMessageText());
        tv.setTextColor(inkColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(Typeface.DEFAULT);
        tv.setLineSpacing(dp(2), 1.15f);
        tv.setPadding(dp(16), dp(14), dp(16), dp(14));
        tv.setBackground(panel(surfaceColor(), lineColor(), 16));
        bubbleCol.addView(tv);

        // Interactive Action Button
        if (response.hasAction()) {
            Button btnAction = control(response.getActionButtonLabel(), false);
            btnAction.setPadding(dp(20), dp(10), dp(20), dp(10));

            LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpBtn.setMargins(0, dp(10), 0, dp(4));
            btnAction.setLayoutParams(lpBtn);

            btnAction.setOnClickListener(v -> {
                dismiss();
                executeAction(response.getActionIntent(), response.getActionPayload());
            });
            bubbleCol.addView(btnAction);
        }

        messagesContainer.addView(bubbleCol);
        updateSuggestionChips(response.getSuggestedChips());
        scrollToBottom();
    }

    private void executeAction(String intent, String payload) {
        if (actionListener == null) return;

        switch (intent) {
            case AiResponse.INTENT_NAVIGATE_HOME:
                actionListener.onNavigateTab(0);
                break;
            case AiResponse.INTENT_NAVIGATE_SERMONS:
            case AiResponse.INTENT_NAVIGATE_LIVE:
                actionListener.onOpenLiveSermons();
                break;
            case AiResponse.INTENT_NAVIGATE_WORSHIP:
                actionListener.onNavigateTab(2);
                break;
            case AiResponse.INTENT_NAVIGATE_FELLOWSHIP:
                actionListener.onNavigateTab(3);
                break;
            case AiResponse.INTENT_NAVIGATE_PROFILE:
                actionListener.onNavigateTab(5);
                break;
            case AiResponse.INTENT_NAVIGATE_GIVING:
                actionListener.onOpenGiving();
                break;
            case AiResponse.INTENT_OPEN_BIBLE:
                actionListener.onOpenBible(payload);
                break;
            case AiResponse.INTENT_LAUNCH_RIDDLES:
                actionListener.onLaunchRiddles();
                break;
            case AiResponse.INTENT_LAUNCH_JOKES:
                actionListener.onLaunchJokes();
                break;
            case AiResponse.INTENT_REFLECTIONS:
                actionListener.onStartReflections();
                break;
            case AiResponse.INTENT_OPEN_GAMES:
                actionListener.onOpenGames();
                break;
            case AiResponse.INTENT_OPEN_PROFILE:
                actionListener.onOpenProfile();
                break;
        }
    }

    private void updateSuggestionChips(List<String> chips) {
        chipsContainer.removeAllViews();
        if (chips == null || chips.isEmpty()) return;

        for (final String chipText : chips) {
            Button chip = new Button(activity);
            chip.setText(chipText);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setAllCaps(false);
            chip.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            chip.setTextColor(inkColor());
            chip.setBackground(panel(surfaceColor(), lineColor(), 22));
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setMinHeight(0);
            chip.setMinimumHeight(0);
            chip.setMinWidth(0);
            chip.setMinimumWidth(0);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(44));
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> submitQuery(chipText));
            chipsContainer.addView(chip);
        }
    }

    private void scrollToBottom() {
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            activity.getResources().getDisplayMetrics()
        );
    }

    private GradientDrawable createPillBg(int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(14));
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

        LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{shadow, face});
        layers.setLayerInset(0, 0, dp(shadowDepthDp), 0, 0);
        layers.setLayerInset(1, 0, 0, 0, dp(shadowDepthDp));
        return layers;
    }
}
