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

    @Override
    public void show() {
        super.show();
        org.perazimchurch.app.ui.theme.GlassPolish.install(this);
    }

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
    private final PerazimAiEngine aiEngine;
    private final AiActionListener actionListener;

    private ScrollView chatScrollView;
    private LinearLayout messagesContainer;
    private LinearLayout chipsContainer;
    private EditText etInput;
    private Button btnSend;
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

        // Welcome is shown once and is never sent as model conversation history.
        String welcome = BuildConfig.OFFLINE_QWEN_ENABLED
                ? "Perazim AI’s on-phone Qwen is enabled in this validation build. Answers use verified local passages and cannot access live information. Online AI is unavailable in this build."
                : "Generative AI is unavailable in this build. Perazim Guide can still help you use the app and retrieve local KJV passages.";
        displayAiMessage(AiResponse.plainText(
                "What would you like to explore? Ask about using the app or find a built-in shortcut.\n\n" + welcome,
                java.util.Arrays.asList("Open the Bible", "Find my groups", "Start a reflection")), "Perazim Guide");
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
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PerazimTheme.isLight() ? Color.parseColor("#B32A0F33") : Color.parseColor("#99000000")); // Scrim
        root.setGravity(Gravity.BOTTOM);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Sheet container
        LinearLayout sheet = new LinearLayout(activity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(PerazimTheme.COLOR_SURFACE);
        sheetBg.setStroke(dp(1), PerazimTheme.COLOR_STROKE);
        sheetBg.setCornerRadii(new float[]{dp(20), dp(20), dp(20), dp(20), 0, 0, 0, 0});
        sheet.setBackground(sheetBg);
        sheet.setPadding(dp(16), dp(12), dp(16), dp(16));

        LinearLayout.LayoutParams lpSheet = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.66));
        sheet.setLayoutParams(lpSheet);

        // Drag Handle
        View handle = new View(activity);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(PerazimTheme.COLOR_STROKE);
        handleBg.setCornerRadius(dp(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams lpH = new LinearLayout.LayoutParams(dp(44), dp(5));
        lpH.gravity = Gravity.CENTER_HORIZONTAL;
        lpH.setMargins(0, 0, 0, dp(10));
        handle.setLayoutParams(lpH);
        sheet.addView(handle);

        // Header
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.ic_perazim_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(GlassKit.glassControl(activity, 999));
        logo.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpLogo.setMargins(0, 0, dp(12), 0);
        logo.setLayoutParams(lpLogo);
        header.addView(logo);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleCol.setLayoutParams(lpCol);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Perazim Breakthrough Guide");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        titleCol.addView(tvTitle);

        tvSub = new TextView(activity);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvSub);

        header.addView(titleCol);

        Button btnClose = new Button(activity);
        btnClose.setText("✕");
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnClose.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        btnClose.setBackground(null);
        btnClose.setPadding(dp(6), dp(6), dp(6), dp(6));
        btnClose.setOnClickListener(v -> dismiss());
        header.addView(btnClose);

        sheet.addView(header);
        btnMode = new Button(activity);
        btnMode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnMode.setAllCaps(false);
        btnMode.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnMode.setTextColor(GlassKit.text());
        btnMode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnMode.setBackground(GlassKit.glassControl(activity, 999));
        btnMode.setPadding(dp(14), dp(6), dp(14), dp(6));
        btnMode.setMinHeight(0);
        btnMode.setMinimumHeight(0);
        LinearLayout.LayoutParams lpMode = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
        lpMode.gravity = Gravity.START;
        lpMode.setMargins(0, 0, 0, dp(8));
        btnMode.setLayoutParams(lpMode);
        btnMode.setOnClickListener(v -> {
            if (localRequest) { OnDeviceAi.cancel(); return; }
            if (!requestInFlight) showModeOptions();
        });
        sheet.addView(btnMode);

        // Chat messages scroll container
        chatScrollView = new ScrollView(activity);
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        chatScrollView.setLayoutParams(lpScroll);
        chatScrollView.setFillViewport(true);

        messagesContainer = new LinearLayout(activity);
        messagesContainer.setOrientation(LinearLayout.VERTICAL);
        messagesContainer.setPadding(0, dp(4), 0, dp(12));
        chatScrollView.addView(messagesContainer);

        sheet.addView(chatScrollView);

        // Quick Suggestion Chips Scroll
        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams lpHsv = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHsv.setMargins(0, dp(4), 0, dp(8));
        hsv.setLayoutParams(lpHsv);

        chipsContainer = new LinearLayout(activity);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(chipsContainer);
        sheet.addView(hsv);

        // Input Bar
        LinearLayout inputBar = new LinearLayout(activity);
        inputBar.setOrientation(LinearLayout.HORIZONTAL);
        inputBar.setGravity(Gravity.CENTER_VERTICAL);
        inputBar.setBackground(GlassKit.glassControl(activity, 999));
        inputBar.setPadding(dp(16), dp(4), dp(6), dp(4));

        etInput = new EditText(activity);
        etInput.setHint("Ask about Scripture, breakthrough, sermons...");
        etInput.setHintTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        etInput.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etInput.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        etInput.setBackground(null);
        etInput.setSingleLine(true);
        etInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                submitQuery(etInput.getText().toString());
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etInput.setLayoutParams(lpEt);
        inputBar.addView(etInput);

        btnSend = new Button(activity);
        btnSend.setText("");
        styleOrangePill(btnSend, 13);
        android.graphics.drawable.GradientDrawable sendBg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{GlassKit.ORANGE_TOP, GlassKit.ORANGE_BOTTOM});
        sendBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        btnSend.setBackground(sendBg);
        android.graphics.drawable.Drawable sendIcon = activity.getResources()
                .getDrawable(android.R.drawable.ic_menu_send).mutate();
        sendIcon.setTint(COLOR_WHITE);
        btnSend.setCompoundDrawablesWithIntrinsicBounds(sendIcon, null, null, null);
        btnSend.setContentDescription("Send");
        btnSend.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams lpSend = new LinearLayout.LayoutParams(dp(44), dp(44));
        btnSend.setLayoutParams(lpSend);
        btnSend.setOnClickListener(v -> submitQuery(etInput.getText().toString()));
        inputBar.addView(btnSend);

        sheet.addView(inputBar);
        root.addView(sheet);

        return root;
    }

    private void updateSubtitle() {
        if (tvSub == null) return;
        int statusGeneration = ++knowledgeStatusGeneration;
        String mode = guideMode();
        if (btnMode != null) btnMode.setText("local".equals(mode)
                ? "On-phone AI · Change mode"
                : ("online".equals(mode) ? "Online AI · Change mode" : "Offline app guide · Change mode"));
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
            tvSub.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
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
            tvSub.setTextColor(COLOR_TEXT_MUTED());
            tvSub.setOnClickListener(null);
            return;
        }
        tvSub.setText("Online AI · Church service");
        tvSub.setTextColor(COLOR_ACCENT_ORANGE);
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
        // routes (GuideTurnRouter). The evaluation uses the same order, so the phone and the evaluation cannot drift apart.
        GuideTurnRouter.Turn turn = GuideTurnRouter.route(activity, aiEngine, query);
        if (turn.response != null) {
            if (turn.clearHistory) localHistory.clear();
            displayAiMessage(turn.response, turn.badge);
            return;
        }

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
            if (btnMode != null) btnMode.setText("Stop response");
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
        if (btnMode != null) btnMode.setText("Change mode");
    }

    private View showTypingIndicator() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), dp(40), dp(4));

        TextView tv = new TextView(activity);
        tv.setText("🤖 Pondering with Scripture...");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tv.setTextColor(GlassKit.muted());
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        tv.setBackground(GlassKit.glass(activity, 18));

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

    private void displayUserMessage(String text) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.setPadding(dp(48), dp(4), 0, dp(8));

        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(COLOR_WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        tv.setLineSpacing(dp(2), 1.1f);
        tv.setPadding(dp(16), dp(10), dp(16), dp(10));
        tv.setBackground(GlassKit.orange(activity, 18));

        row.addView(tv);
        messagesContainer.addView(row);
        scrollToBottom();
    }

    private void displayAiMessage(AiResponse response, @Nullable String badgeText) {
        LinearLayout bubbleCol = new LinearLayout(activity);
        bubbleCol.setOrientation(LinearLayout.VERTICAL);
        bubbleCol.setPadding(0, dp(4), dp(24), dp(10));

        // Badge pill
        if (badgeText != null && !badgeText.isEmpty()) {
            TextView tvBadge = new TextView(activity);
            tvBadge.setText(badgeText);
            tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvBadge.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            tvBadge.setTextColor(badgeText.contains("Offline") ? GlassKit.muted() : GlassKit.accent());
            tvBadge.setPadding(dp(4), 0, 0, dp(6));
            bubbleCol.addView(tvBadge);
        }

        // Message text
        TextView tv = new TextView(activity);
        tv.setText(response.getMessageText());
        tv.setTextColor(GlassKit.text());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        tv.setLineSpacing(dp(2), 1.15f);
        tv.setPadding(dp(16), dp(14), dp(16), dp(14));
        tv.setBackground(GlassKit.glass(activity, 18));
        bubbleCol.addView(tv);

        // Interactive Action Button
        if (response.hasAction()) {
            Button btnAction = new Button(activity);
            btnAction.setText(response.getActionButtonLabel());
            styleOrangePill(btnAction, 13);
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
            chip.setTextColor(GlassKit.text());
            chip.setBackground(GlassKit.glass(activity, 999));
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setMinHeight(0);
            chip.setMinimumHeight(0);
            chip.setMinWidth(0);
            chip.setMinimumWidth(0);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> submitQuery(chipText));
            chipsContainer.addView(chip);
        }
    }

    private void scrollToBottom() {
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    /** Orange gradient pill with the Bible tab's glow; used for Send and for action buttons. */
    private void styleOrangePill(Button b, float textSp) {
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setTextColor(COLOR_WHITE);
        b.setBackground(GlassKit.orange(activity, 999));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        GlassKit.glow(b, GlassKit.ORANGE, 8);
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
