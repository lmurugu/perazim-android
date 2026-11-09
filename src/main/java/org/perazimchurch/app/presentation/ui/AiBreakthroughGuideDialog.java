package org.perazimchurch.app.presentation.ui;

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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.remote.AiPreferences;
import org.perazimchurch.app.ai.remote.AiRelayClient;
import org.perazimchurch.app.ai.remote.ConversationHistory;
import org.perazimchurch.app.ai.local.OnDeviceAi;
import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.ui.theme.PerazimTheme;

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
        void onLaunchRiddles();
        void onLaunchJokes();
        void onStartReflections();
    }

    private static final int COLOR_PRIMARY_PURPLE = Color.parseColor("#681A7D");
    private static final int COLOR_PURPLE_DARK    = Color.parseColor("#2D0938");
    private static final int COLOR_PURPLE_SHADOW  = Color.parseColor("#450E53");
    private static final int COLOR_PURPLE_TINT    = Color.parseColor("#F5ECF7");
    private static final int COLOR_BORDER_GREY    = Color.parseColor("#E7D5EC");
    private static final int COLOR_ACCENT_ORANGE  = Color.parseColor("#E17D2F");
    private static final int COLOR_ORANGE_SHADOW  = Color.parseColor("#98450B");
    private static final int COLOR_WHITE          = Color.parseColor("#FFFFFF");
    private static final int COLOR_TEXT_DARK      = Color.parseColor("#1A1225");
    private static final int COLOR_TEXT_MUTED     = Color.parseColor("#725B78");
    private static final int COLOR_BG_NEUTRAL     = Color.parseColor("#FAF7FB");

    private final Activity activity;
    private final PerazimAiEngine aiEngine;
    private final AiActionListener actionListener;

    private ScrollView chatScrollView;
    private LinearLayout messagesContainer;
    private LinearLayout chipsContainer;
    private EditText etInput;
    private Button btnSend;
    private TextView tvSub;
    private final ConversationHistory onlineHistory = new ConversationHistory();
    private boolean requestInFlight;
    private final ConversationHistory localHistory = new ConversationHistory();
    private Button btnMode;
    private boolean localRequest;
    private String guideMode() {
        return activity.getSharedPreferences("perazim_guide_mode", Context.MODE_PRIVATE).getString("mode", "guide");
    }
    private void setGuideMode(String mode) {
        activity.getSharedPreferences("perazim_guide_mode", Context.MODE_PRIVATE).edit().putString("mode", mode).apply();
        updateSubtitle();
    }

    public AiBreakthroughGuideDialog(@NonNull Activity activity, @NonNull AiActionListener actionListener) {
        super(activity);
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
        displayAiMessage(AiResponse.plainText(
                "What would you like to explore? Ask about using the app or find a built-in shortcut.\n\nGenerative AI is unavailable in this build: on-device answers are not yet limited to approved sources, and an authenticated church AI service has not been verified.",
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
        root.setBackgroundColor(Color.parseColor("#99000000")); // Scrim
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
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.82));
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

        TextView tvIcon = new TextView(activity);
        tvIcon.setText("🤖");
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvIcon.setPadding(0, 0, dp(10), 0);
        header.addView(tvIcon);

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
        btnMode.setBackground(createPillBg(PerazimTheme.COLOR_SURFACE_2, PerazimTheme.COLOR_STROKE));
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
        inputBar.setBackground(createPillBg(PerazimTheme.COLOR_SURFACE_2, PerazimTheme.COLOR_STROKE));
        inputBar.setPadding(dp(12), dp(2), dp(6), dp(2));

        etInput = new EditText(activity);
        etInput.setHint("Ask about Scripture, breakthrough, sermons...");
        etInput.setHintTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        etInput.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
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
        btnSend.setText("➤ Send");
        btnSend.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnSend.setTypeface(Typeface.DEFAULT_BOLD);
        btnSend.setTextColor(COLOR_WHITE);
        btnSend.setBackground(create3dButtonDrawable(COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 10, 2));
        btnSend.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnSend.setOnClickListener(v -> submitQuery(etInput.getText().toString()));
        inputBar.addView(btnSend);

        sheet.addView(inputBar);
        root.addView(sheet);

        return root;
    }

    private void updateSubtitle() {
        if (tvSub == null) return;
        String mode = guideMode();
        if (btnMode != null) btnMode.setText("local".equals(mode)
                ? "On-phone AI unavailable · Change mode"
                : ("online".equals(mode) ? "Online AI unavailable · Change mode" : "Offline app guide · Change mode"));
        if ("local".equals(mode)) {
            tvSub.setText("Unavailable until approved-source controls are implemented");
            tvSub.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            tvSub.setOnClickListener(v -> showModeOptions());
            return;
        }
        if ("guide".equals(mode)) {
            tvSub.setText("Built-in app help · Not a generative model");
            tvSub.setOnClickListener(v -> showModeOptions());
            return;
        }
        if ("online".equals(mode) && !BuildConfig.ONLINE_AI_ENABLED) {
            tvSub.setText("Online AI unavailable in this build");
            tvSub.setTextColor(COLOR_TEXT_MUTED);
            tvSub.setOnClickListener(null);
            return;
        }
        if (!AiPreferences.hasConsented(activity)) {
            tvSub.setText("Online AI unavailable in this build");
            tvSub.setTextColor(COLOR_TEXT_MUTED);
            tvSub.setOnClickListener(null);
        } else if (!AiPreferences.isAiEnabled(activity)) {
            tvSub.setText("📱 Offline Guide • AI Resting");
            tvSub.setTextColor(COLOR_TEXT_MUTED);
            tvSub.setOnClickListener(null);
        } else {
            int remaining = AiPreferences.getRemainingQuestions(activity);
            if (remaining > 0) {
                tvSub.setText("⚡ Breakthrough AI • " + remaining + " questions left today");
                tvSub.setTextColor(COLOR_ACCENT_ORANGE);
            } else {
                tvSub.setText("📱 Offline Guide • Daily limit reached");
                tvSub.setTextColor(COLOR_TEXT_MUTED);
            }
            tvSub.setOnClickListener(null);
        }
    }

    private void showModeOptions() {
        new AlertDialog.Builder(activity).setTitle("Choose your guide")
                .setItems(new String[]{"On-phone AI — unavailable pending approved-source controls", "Model download — disabled", "Online AI — unavailable pending authenticated service", "Offline app help — built-in shortcuts", "Clear this conversation"}, (dialog, which) -> {
                    if (which == 0) setGuideMode("local");
                    else if (which == 1) showModelDownload();
                    else if (which == 2) setGuideMode("online");
                    else if (which == 3) setGuideMode("guide");
                    else {
                        localHistory.clear(); onlineHistory.clear();
                        messagesContainer.removeAllViews();
                        displayAiMessage(AiResponse.plainText("Conversation cleared. What would you like to discuss?", null), "Perazim Guide");
                    }
                }).setNegativeButton("Close", null).show();
    }

    private void showModelDownload() {
        if (!BuildConfig.OFFLINE_QWEN_ENABLED) {
            new AlertDialog.Builder(activity)
                    .setMessage("On-device AI downloads are disabled until approved-source controls are implemented and verified.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        if (OnDeviceAi.isInstalled(activity)) {
            setGuideMode("local");
            new AlertDialog.Builder(activity).setMessage("The on-phone model is installed. Questions and answers stay on this phone in this mode.").setPositiveButton("Done", null).show();
            return;
        }
        new AlertDialog.Builder(activity).setTitle("Download on-phone AI")
                .setMessage("On-device AI downloads are disabled until approved-source controls are implemented and verified.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Download", (dialog, which) -> {
                    AlertDialog progress = new AlertDialog.Builder(activity).setTitle("Downloading on-phone model")
                            .setMessage("Starting…").setCancelable(false).setNegativeButton("Pause", (d,w) -> OnDeviceAi.cancel()).create();
                    progress.show();
                    OnDeviceAi.download(activity, new OnDeviceAi.DownloadCallback() {
                        public void onProgress(int percent) { if (progress.isShowing()) progress.setMessage(percent + "% · The download can be paused and resumed."); }
                        public void onComplete() { progress.dismiss(); setGuideMode("local"); if (isShowing()) displayAiMessage(AiResponse.plainText("The model is ready. You can turn off Wi-Fi and mobile data and ask your question.", null), "On-phone AI ready"); }
                        public void onError(String error) { progress.dismiss(); if (isShowing()) new AlertDialog.Builder(activity).setMessage(error).setPositiveButton("OK", null).show(); }
                    });
                }).show();
    }

    @Override public void dismiss() {
        if (localRequest) OnDeviceAi.cancel();
        localHistory.clear(); onlineHistory.clear();
        super.dismiss();
    }

    public void showConsentDialog() {
        if (activity == null || activity.isFinishing()) return;
        if (!BuildConfig.ONLINE_AI_ENABLED) {
            new AlertDialog.Builder(activity)
                    .setMessage("Online AI is unavailable in this build while the authenticated church service is being verified.")
                    .setPositiveButton("OK", null).show();
            return;
        }

        final Dialog consentDlg = new Dialog(activity);
        consentDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        consentDlg.setCanceledOnTouchOutside(false);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_WHITE);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), COLOR_BORDER_GREY);
        layout.setBackground(bg);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(activity);
        title.setText("🤖 Google Gemini AI Guide");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        layout.addView(title);

        TextView body = new TextView(activity);
        body.setText("Google's AI generates online answers. Your question and up to 6 recent question-and-answer exchanges from this chat are sent through the church relay to Google. Offline-guide messages are not sent. This chat history is kept only while this window is open. On the free service, Google may use questions to improve its models; avoid private details.\n\nThe online service currently allows 3 questions per day.");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        body.setTextColor(COLOR_TEXT_DARK);
        body.setPadding(0, dp(12), 0, dp(10));
        body.setLineSpacing(dp(2), 1.1f);
        layout.addView(body);

        final CheckBox cbConsent = new CheckBox(activity);
        cbConsent.setText("I understand and agree");
        cbConsent.setTextColor(COLOR_PURPLE_DARK);
        cbConsent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        layout.addView(cbConsent);

        final CheckBox cbName = new CheckBox(activity);
        cbName.setText("Address me by my first name");
        cbName.setChecked(AiPreferences.isFirstNameOptedIn(activity));
        cbName.setTextColor(COLOR_TEXT_MUTED);
        cbName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        layout.addView(cbName);

        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        btnRow.setPadding(0, dp(16), 0, 0);

        Button btnDecline = new Button(activity);
        btnDecline.setText("Offline Guide");
        btnDecline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnDecline.setTextColor(COLOR_TEXT_MUTED);
        btnDecline.setBackground(null);
        btnDecline.setOnClickListener(v -> {
            AiPreferences.setConsent(activity, false);
            onlineHistory.clear();
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
            AiPreferences.setFirstNameOptedIn(activity, cbName.isChecked());
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

    private void submitQuery(String text) {
        if (requestInFlight || text == null || text.trim().isEmpty()) return;
        final String query = text.trim();
        etInput.setText("");

        // 1. Render User Message Bubble
        displayUserMessage(query);

        String mode = guideMode();
        if ("local".equals(mode)) {
            if (!BuildConfig.OFFLINE_QWEN_ENABLED) {
                displayAiMessage(AiResponse.plainText("On-device AI is unavailable until approved-source controls are implemented and verified. Built-in app help remains available.", null), "On-phone AI unavailable");
                return;
            }
            if (!OnDeviceAi.isInstalled(activity)) {
                displayAiMessage(AiResponse.plainText("Download the on-phone model to have a real AI conversation without internet. Use the mode button above to set it up. Built-in app help remains available separately.", null), "Model not installed");
                return;
            }
            final View indicator = showTypingIndicator();
            requestInFlight = true; localRequest = true; btnSend.setEnabled(false);
            if (btnMode != null) btnMode.setText("Stop response");
            OnDeviceAi.ask(activity, query, localHistory.snapshot(), new OnDeviceAi.Callback() {
                public void onSuccess(String answer) {
                    removeTypingIndicator(indicator); localRequest = false; updateSubtitle();
                    if (!isShowing()) return;
                    String result = ConversationHistory.withoutRepeatedGreeting(answer, !localHistory.isEmpty());
                    localHistory.addExchange(query, result);
                    displayAiMessage(AiResponse.plainText(result, null), "On-phone AI · Qwen · Offline");
                }
                public void onError(String error) {
                    removeTypingIndicator(indicator); localRequest = false; updateSubtitle();
                    if (isShowing()) displayAiMessage(AiResponse.plainText(error, null), "On-phone model notice");
                }
            });
            return;
        }
        if ("guide".equals(mode)) {
            displayAiMessage(aiEngine.processQuery(query), "Offline app help · Built-in rules");
            return;
        }

        if (!BuildConfig.ONLINE_AI_ENABLED) {
            displayAiMessage(AiResponse.plainText("Online AI is unavailable in this build while an authenticated church service is being verified. Built-in app help remains available.", null), "Online AI unavailable");
            return;
        }

        // 2. Check consent
        if (!AiPreferences.hasConsented(activity)) {
            // Answer immediately on device; online consent is available from the subtitle.
            AiResponse offlineResp = aiEngine.processQuery(query);
            displayAiMessage(offlineResp, "📱 Offline Guide (Consent Required for Online AI)");
            return;
        }

        // 3. Check AI kill-switch and client quota
        if (!AiPreferences.isAiEnabled(activity)) {
            AiResponse offlineResp = aiEngine.processQuery(query);
            displayAiMessage(offlineResp, "📱 Offline Guide (Cloud AI Inactive)");
            updateSubtitle();
            return;
        }

        int remaining = AiPreferences.getRemainingQuestions(activity);
        if (remaining <= 0) {
            AiResponse offlineResp = aiEngine.processQuery(query);
            displayAiMessage(offlineResp, "📱 Offline Guide (Daily Cloud Quota Reached · 3/3 Used)");
            updateSubtitle();
            return;
        }

        // 4. Show typing/pondering indicator
        final View typingIndicator = showTypingIndicator();
        requestInFlight = true;
        btnSend.setEnabled(false);

        // 5. Query Cloudflare Relay
        AiRelayClient.askQuestion(activity, query, onlineHistory.snapshot(), new AiRelayClient.RelayCallback() {
            @Override
            public void onSuccess(@NonNull String answer, int remainingQuestionsToday) {
                removeTypingIndicator(typingIndicator);
                if (!isShowing()) return;
                updateSubtitle();

                String badge = "⚡ Online Cloud AI (Gemini · " + remainingQuestionsToday + " questions left today)";
                String conversationalAnswer = ConversationHistory.withoutRepeatedGreeting(answer, !onlineHistory.isEmpty());
                onlineHistory.addExchange(query, conversationalAnswer);
                AiResponse response = AiResponse.plainText(conversationalAnswer, null);
                displayAiMessage(response, badge);
            }

            @Override
            public void onQuotaExceeded(int remainingQuestionsToday) {
                removeTypingIndicator(typingIndicator);
                if (!isShowing()) return;
                updateSubtitle();

                AiResponse offlineResp = aiEngine.processQuery(query);
                displayAiMessage(offlineResp, "📱 Offline Guide (Daily Cloud Quota Reached · 3/3 Used)");
            }

            @Override
            public void onError(@NonNull String errorMessage, boolean shouldFallbackOffline) {
                removeTypingIndicator(typingIndicator);
                if (!isShowing()) return;
                updateSubtitle();

                if (shouldFallbackOffline) {
                    AiResponse offlineResp = aiEngine.processQuery(query);
                    displayAiMessage(offlineResp, "📱 Offline Guide (Internet / Cloud Relay Unavailable)");
                } else {
                    AiResponse errResp = AiResponse.plainText("⚠️ " + errorMessage, null);
                    displayAiMessage(errResp, "⚠️ System Notice");
                }
            }
        });
    }

    private View showTypingIndicator() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), dp(40), dp(4));

        TextView tv = new TextView(activity);
        tv.setText("🤖 Pondering with Scripture...");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(COLOR_PRIMARY_PURPLE);
        tv.setPadding(dp(12), dp(6), dp(12), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PURPLE_TINT);
        bg.setCornerRadius(dp(12));
        tv.setBackground(bg);

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
        row.setPadding(dp(40), dp(4), 0, dp(6));

        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(COLOR_WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PRIMARY_PURPLE);
        bg.setCornerRadii(new float[]{dp(14), dp(14), dp(14), dp(14), dp(2), dp(2), dp(14), dp(14)});
        tv.setBackground(bg);

        row.addView(tv);
        messagesContainer.addView(row);
        scrollToBottom();
    }

    private void displayAiMessage(AiResponse response, @Nullable String badgeText) {
        LinearLayout bubbleCol = new LinearLayout(activity);
        bubbleCol.setOrientation(LinearLayout.VERTICAL);
        bubbleCol.setPadding(0, dp(4), dp(40), dp(8));

        // Badge pill
        if (badgeText != null && !badgeText.isEmpty()) {
            TextView tvBadge = new TextView(activity);
            tvBadge.setText(badgeText);
            tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
            tvBadge.setTextColor((int) (badgeText.contains("Offline") ? PerazimTheme.COLOR_PURPLE_MUTED : PerazimTheme.COLOR_ORANGE_BRAND));
            tvBadge.setPadding(dp(4), 0, 0, dp(2));
            bubbleCol.addView(tvBadge);
        }

        // Message text
        TextView tv = new TextView(activity);
        tv.setText(response.getMessageText());
        tv.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PerazimTheme.COLOR_SURFACE_2);
        bg.setStroke(dp(1), PerazimTheme.COLOR_STROKE);
        bg.setCornerRadii(new float[]{dp(14), dp(14), dp(14), dp(14), dp(14), dp(14), dp(2), dp(2)});
        tv.setBackground(bg);
        bubbleCol.addView(tv);

        // Interactive Action Button
        if (response.hasAction()) {
            Button btnAction = new Button(activity);
            btnAction.setText(response.getActionButtonLabel());
            btnAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnAction.setTypeface(Typeface.DEFAULT_BOLD);
            btnAction.setTextColor(COLOR_WHITE);
            btnAction.setBackground(create3dButtonDrawable(COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
            btnAction.setPadding(dp(14), dp(8), dp(14), dp(8));

            LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpBtn.setMargins(0, dp(6), 0, dp(4));
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
                actionListener.onNavigateTab(4);
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
        }
    }

    private void updateSuggestionChips(List<String> chips) {
        chipsContainer.removeAllViews();
        if (chips == null || chips.isEmpty()) return;

        for (final String chipText : chips) {
            Button chip = new Button(activity);
            chip.setText(chipText);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            chip.setBackground(createPillBg(PerazimTheme.COLOR_SURFACE_2, PerazimTheme.COLOR_STROKE));
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
            lp.setMargins(0, 0, dp(6), 0);
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
