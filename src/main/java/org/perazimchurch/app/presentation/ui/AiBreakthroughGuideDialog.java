package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.ai.PerazimAiEngine;

import java.util.List;

/**
 * Interactive Chatbot Interface for the Perazim Breakthrough Guide.
 * Provides instant on-device natural language navigation, gamification status checks,
 * quick suggestion chips, and direct action triggers.
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

        // Display initial greeting
        displayAiMessage(aiEngine.getWelcomeResponse());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
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
        sheetBg.setColor(COLOR_WHITE);
        sheetBg.setCornerRadii(new float[]{dp(20), dp(20), dp(20), dp(20), 0, 0, 0, 0});
        sheet.setBackground(sheetBg);
        sheet.setPadding(dp(16), dp(12), dp(16), dp(16));

        LinearLayout.LayoutParams lpSheet = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.82));
        sheet.setLayoutParams(lpSheet);

        // Drag Handle
        View handle = new View(activity);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(COLOR_BORDER_GREY);
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
        tvTitle.setTextColor(COLOR_PURPLE_DARK);
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("⚡ Tiny Edge AI • 100% Offline • Perazim Guide");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(COLOR_ACCENT_ORANGE);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvSub);

        header.addView(titleCol);

        Button btnClose = new Button(activity);
        btnClose.setText("✕");
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnClose.setTextColor(COLOR_TEXT_MUTED);
        btnClose.setBackground(null);
        btnClose.setPadding(dp(6), dp(6), dp(6), dp(6));
        btnClose.setOnClickListener(v -> dismiss());
        header.addView(btnClose);

        sheet.addView(header);

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
        inputBar.setBackground(createPillBg(COLOR_BG_NEUTRAL, COLOR_BORDER_GREY));
        inputBar.setPadding(dp(12), dp(2), dp(6), dp(2));

        etInput = new EditText(activity);
        etInput.setHint("Ask about XP, Bible, sermons, tithe...");
        etInput.setHintTextColor(COLOR_TEXT_MUTED);
        etInput.setTextColor(COLOR_TEXT_DARK);
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

    private void submitQuery(String text) {
        if (text == null || text.trim().isEmpty()) return;
        etInput.setText("");

        // 1. Render User Message Bubble
        displayUserMessage(text.trim());

        // 2. Process query via on-device AI Engine
        AiResponse response = aiEngine.processQuery(text.trim());

        // 3. Render AI Response Bubble with interactive buttons
        displayAiMessage(response);
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

    private void displayAiMessage(AiResponse response) {
        LinearLayout bubbleCol = new LinearLayout(activity);
        bubbleCol.setOrientation(LinearLayout.VERTICAL);
        bubbleCol.setPadding(0, dp(4), dp(40), dp(8));

        // Message text
        TextView tv = new TextView(activity);
        tv.setText(response.getMessageText());
        tv.setTextColor(COLOR_TEXT_DARK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PURPLE_TINT);
        bg.setStroke(dp(1), COLOR_BORDER_GREY);
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
            chip.setTextColor(COLOR_PRIMARY_PURPLE);
            chip.setBackground(createPillBg(COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
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
