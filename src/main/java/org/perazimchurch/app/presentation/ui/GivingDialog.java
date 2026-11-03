package org.perazimchurch.app.presentation.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.navigation.ChurchIntentManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Lipa na M-Pesa Giving & Tithes Dialog.
 * Faithful implementation matching approved mockup (docs/screenshots/live_screen_giving_modal.png).
 * Integrates:
 * - Safaricom M-Pesa Paybill (4069983) with one-tap clipboard copy
 * - Dynamic Fund account indicator (Tithe, Offering, Missions, Mercy Fund)
 * - Quick amount chips (200, 500, 1,000, 2,500) and custom amount entry
 * - 3-tier Safaricom launch options: SIM Toolkit, M-Pesa App, Dial *334#
 * - Pastoral care inquiries box with WhatsApp, SMS, phone call, and email links
 */
public final class GivingDialog {

    // --- CANONICAL PERAZIM PALETTE (UNCHANGED) ---
    public static final int COLOR_PRIMARY_PURPLE = Color.parseColor("#681A7D");
    public static final int COLOR_PURPLE_DARK    = Color.parseColor("#2D0938");
    public static final int COLOR_PURPLE_SHADOW  = Color.parseColor("#450E53");
    public static final int COLOR_PURPLE_LIGHT   = Color.parseColor("#8E30A8");
    public static final int COLOR_BORDER_GREY    = Color.parseColor("#E7D5EC");
    public static final int COLOR_PURPLE_TINT    = Color.parseColor("#F5ECF7");
    public static final int COLOR_TEXT_MUTED     = Color.parseColor("#725B78");
    public static final int COLOR_TEXT_DARK      = Color.parseColor("#1A1225");

    public static final int COLOR_ACCENT_ORANGE  = Color.parseColor("#E17D2F");
    public static final int COLOR_ORANGE_SHADOW  = Color.parseColor("#98450B");

    public static final int COLOR_WHITE          = Color.parseColor("#FFFFFF");

    private GivingDialog() {
        // Prevent direct instantiation
    }

    /**
     * Constructs and displays the Lipa na M-Pesa Giving modal dialog.
     *
     * @param context Active context
     * @return The displayed AlertDialog instance
     */
    public static AlertDialog show(@NonNull final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ScrollView sv = new ScrollView(context);
        sv.setFillViewport(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));
        container.setBackgroundColor(COLOR_WHITE);

        // 1. Header Title
        TextView title = new TextView(context);
        title.setText("💚 Lipa na M-Pesa · Giving & Tithes");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        container.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("“Honor the LORD with your wealth, with the firstfruits of all your crops.” — Proverbs 3:9");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subtitle.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        subtitle.setTextColor(COLOR_TEXT_MUTED);
        subtitle.setPadding(0, dp(context, 2), 0, dp(context, 14));
        container.addView(subtitle);

        // 2. Paybill Details Box
        LinearLayout paybillBox = new LinearLayout(context);
        paybillBox.setOrientation(LinearLayout.VERTICAL);
        paybillBox.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        GradientDrawable pbBg = new GradientDrawable();
        pbBg.setColor(COLOR_PURPLE_TINT);
        pbBg.setCornerRadius(dp(context, 12));
        pbBg.setStroke(dp(context, 1), COLOR_PRIMARY_PURPLE);
        paybillBox.setBackground(pbBg);

        TextView tvPaybillLabel = new TextView(context);
        tvPaybillLabel.setText("SAFARICOM M-PESA PAYBILL");
        tvPaybillLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvPaybillLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        paybillBox.addView(tvPaybillLabel);

        LinearLayout pbRow = new LinearLayout(context);
        pbRow.setOrientation(LinearLayout.HORIZONTAL);
        pbRow.setGravity(Gravity.CENTER_VERTICAL);
        pbRow.setPadding(0, dp(context, 4), 0, dp(context, 4));

        TextView tvPaybillNum = new TextView(context);
        tvPaybillNum.setText("Business No: 4069983");
        tvPaybillNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvPaybillNum.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillNum.setTextColor(COLOR_PURPLE_DARK);
        LinearLayout.LayoutParams lpPbNum = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvPaybillNum.setLayoutParams(lpPbNum);
        pbRow.addView(tvPaybillNum);

        Button btnCopyPaybill = new Button(context);
        btnCopyPaybill.setText("📋 COPY 4069983");
        btnCopyPaybill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnCopyPaybill.setTextColor(COLOR_WHITE);
        btnCopyPaybill.setBackground(create3dButtonDrawable(context, COLOR_PRIMARY_PURPLE, COLOR_PURPLE_SHADOW, 8, 2));
        btnCopyPaybill.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
        btnCopyPaybill.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Paybill", "4069983"));
                Toast.makeText(context, "✓ Paybill 4069983 copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        pbRow.addView(btnCopyPaybill);
        paybillBox.addView(pbRow);

        final TextView tvAccount = new TextView(context);
        tvAccount.setText("Account: PERAZIM-TITHE");
        tvAccount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvAccount.setTypeface(Typeface.DEFAULT_BOLD);
        tvAccount.setTextColor(COLOR_ACCENT_ORANGE);
        tvAccount.setPadding(0, dp(context, 4), 0, 0);
        paybillBox.addView(tvAccount);

        container.addView(paybillBox);

        // 3. Fund Selector Spinner
        TextView tvFundLabel = new TextView(context);
        tvFundLabel.setText("SELECT GIVING FUND:");
        tvFundLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFundLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvFundLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvFundLabel.setPadding(0, dp(context, 14), 0, dp(context, 4));
        container.addView(tvFundLabel);

        Spinner fundSpinner = new Spinner(context);
        List<String> funds = new ArrayList<>();
        funds.add("Tithe (PERAZIM-TITHE)");
        funds.add("Offering (PERAZIM-OFFERING)");
        funds.add("Missions & Outreach (PERAZIM-MISSIONS)");
        funds.add("Mercy Fund / Food Drive (PERAZIM-MERCY)");

        ArrayAdapter<String> fundAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, funds);
        fundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fundSpinner.setAdapter(fundAdapter);

        final String[] acctCodes = {"PERAZIM-TITHE", "PERAZIM-OFFERING", "PERAZIM-MISSIONS", "PERAZIM-MERCY"};
        fundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < acctCodes.length) {
                    tvAccount.setText("Account: " + acctCodes[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        container.addView(fundSpinner);

        // 4. Amount Selection & Preset Chips
        TextView tvAmountLabel = new TextView(context);
        tvAmountLabel.setText("AMOUNT (KES):");
        tvAmountLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvAmountLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvAmountLabel.setTextColor(COLOR_PRIMARY_PURPLE);
        tvAmountLabel.setPadding(0, dp(context, 14), 0, dp(context, 6));
        container.addView(tvAmountLabel);

        final EditText etAmount = new EditText(context);
        etAmount.setHint("Enter amount in KES (e.g. 1000)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        etAmount.setText("1000");

        LinearLayout chipsRow = new LinearLayout(context);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, 0, 0, dp(context, 8));

        String[] chipAmounts = {"200", "500", "1,000", "2,500"};
        final String[] chipValues = {"200", "500", "1000", "2500"};
        for (int i = 0; i < chipAmounts.length; i++) {
            final String val = chipValues[i];
            Button chip = new Button(context);
            chip.setText(chipAmounts[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTextColor(COLOR_PRIMARY_PURPLE);
            chip.setBackground(createPillBg(context, COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
            LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(0, dp(context, 34), 1.0f);
            lpChip.setMargins(dp(context, 2), 0, dp(context, 2), 0);
            chip.setLayoutParams(lpChip);
            chip.setOnClickListener(v -> etAmount.setText(val));
            chipsRow.addView(chip);
        }
        container.addView(chipsRow);
        container.addView(etAmount);

        // 5. Confirm Give Button
        Button btnConfirmGive = new Button(context);
        btnConfirmGive.setText("💚 COMPLETE M-PESA GIVING (+20 XP)");
        btnConfirmGive.setTextColor(COLOR_WHITE);
        btnConfirmGive.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnConfirmGive.setTypeface(Typeface.DEFAULT_BOLD);
        btnConfirmGive.setBackground(create3dButtonDrawable(context, COLOR_ACCENT_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        LinearLayout.LayoutParams lpGive = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpGive.setMargins(0, dp(context, 14), 0, 0);
        btnConfirmGive.setLayoutParams(lpGive);
        container.addView(btnConfirmGive);

        sv.addView(container);
        builder.setView(sv);
        final AlertDialog dlg = builder.create();

        btnConfirmGive.setOnClickListener(v -> {
            String amt = etAmount.getText().toString().trim();
            if (amt.isEmpty()) amt = "1000";
            String account = "PERAZIM-TITHE";
            if (tvAccount.getText() != null) {
                account = tvAccount.getText().toString().replace("Account: ", "").trim();
            }
            ChurchIntentManager.launchMpesaGiving(context, "4069983", account, amt);
            try {
                String uid = "user_me";
                SessionManager sm = SessionManager.getInstance(context);
                if (sm != null && sm.getActiveUserId() != null) uid = sm.getActiveUserId();
                int currentXp = GamificationStore.getSpiritualXp(context, uid, 0);
                GamificationStore.saveSpiritualXp(context, uid, currentXp + 20);
            } catch (Exception ignored) {}
            dlg.dismiss();
        });

        // 6. Direct Action Buttons Row (STK, M-Pesa App, *334#)
        TextView tvDirectTitle = new TextView(context);
        tvDirectTitle.setText("DIRECT SAFARICOM LAUNCH OPTIONS:");
        tvDirectTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvDirectTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvDirectTitle.setTextColor(COLOR_PRIMARY_PURPLE);
        tvDirectTitle.setPadding(0, dp(context, 12), 0, dp(context, 4));
        container.addView(tvDirectTitle);

        LinearLayout rowDirectActions = new LinearLayout(context);
        rowDirectActions.setOrientation(LinearLayout.HORIZONTAL);
        rowDirectActions.setPadding(0, 0, 0, dp(context, 6));

        Button btnStk = new Button(context);
        btnStk.setText("📲 SIM TOOLKIT");
        btnStk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnStk.setTextColor(COLOR_PRIMARY_PURPLE);
        btnStk.setBackground(createPillBg(context, COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpStk = new LinearLayout.LayoutParams(0, dp(context, 38), 1.0f);
        lpStk.setMargins(0, 0, dp(context, 2), 0);
        btnStk.setLayoutParams(lpStk);
        btnStk.setOnClickListener(v -> {
            String acc = (tvAccount.getText() != null)
                    ? tvAccount.getText().toString().replace("Account: ", "").trim() : "PERAZIM-TITHE";
            ChurchIntentManager.openSimToolkit(context, "4069983", acc);
        });
        rowDirectActions.addView(btnStk);

        Button btnMpesa = new Button(context);
        btnMpesa.setText("🟢 M-PESA APP");
        btnMpesa.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnMpesa.setTextColor(COLOR_PRIMARY_PURPLE);
        btnMpesa.setBackground(createPillBg(context, COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpMpesa = new LinearLayout.LayoutParams(0, dp(context, 38), 1.0f);
        lpMpesa.setMargins(dp(context, 2), 0, dp(context, 2), 0);
        btnMpesa.setLayoutParams(lpMpesa);
        btnMpesa.setOnClickListener(v -> {
            String acc = (tvAccount.getText() != null)
                    ? tvAccount.getText().toString().replace("Account: ", "").trim() : "PERAZIM-TITHE";
            ChurchIntentManager.openMpesaApp(context, "4069983", acc);
        });
        rowDirectActions.addView(btnMpesa);

        Button btnUssd = new Button(context);
        btnUssd.setText("📞 DIAL *334#");
        btnUssd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnUssd.setTextColor(COLOR_PRIMARY_PURPLE);
        btnUssd.setBackground(createPillBg(context, COLOR_PURPLE_TINT, COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpUssd = new LinearLayout.LayoutParams(0, dp(context, 38), 1.0f);
        lpUssd.setMargins(dp(context, 2), 0, 0, 0);
        btnUssd.setLayoutParams(lpUssd);
        btnUssd.setOnClickListener(v -> ChurchIntentManager.dialUssd(context, "*334#"));
        rowDirectActions.addView(btnUssd);

        container.addView(rowDirectActions);

        // 7. Giving Inquiries & Pastoral Office Contact Box
        LinearLayout contactHelpBox = new LinearLayout(context);
        contactHelpBox.setOrientation(LinearLayout.VERTICAL);
        contactHelpBox.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        GradientDrawable helpBg = new GradientDrawable();
        helpBg.setColor(COLOR_PURPLE_TINT);
        helpBg.setCornerRadius(dp(context, 10));
        helpBg.setStroke(dp(context, 1), COLOR_BORDER_GREY);
        contactHelpBox.setBackground(helpBg);
        LinearLayout.LayoutParams lpHelpBox = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHelpBox.setMargins(0, dp(context, 14), 0, 0);
        contactHelpBox.setLayoutParams(lpHelpBox);

        TextView tvHelpHeader = new TextView(context);
        tvHelpHeader.setText("📞 GIVING INQUIRIES & PASTORAL CARE");
        tvHelpHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHelpHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHelpHeader.setTextColor(COLOR_PRIMARY_PURPLE);
        contactHelpBox.addView(tvHelpHeader);

        TextView tvHelpDesc = new TextView(context);
        tvHelpDesc.setText("For giving verification, covenants, or prayer support:");
        tvHelpDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHelpDesc.setTextColor(COLOR_TEXT_MUTED);
        tvHelpDesc.setPadding(0, dp(context, 2), 0, dp(context, 6));
        contactHelpBox.addView(tvHelpDesc);

        // WhatsApp Pastoral Care Row
        TextView tvBishopWa = new TextView(context);
        tvBishopWa.setText("💬 WhatsApp Bishop: (+254) 0710 772 227");
        tvBishopWa.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBishopWa.setTypeface(Typeface.DEFAULT_BOLD);
        tvBishopWa.setTextColor(COLOR_PRIMARY_PURPLE);
        tvBishopWa.setPadding(0, dp(context, 3), 0, dp(context, 3));
        tvBishopWa.setOnClickListener(v -> ChurchIntentManager.openWhatsAppChat(
                context, "+254710772227", "Peace be with you Bishop Dr. David Mutweri, I have an inquiry / covenant prayer request regarding Perazim giving."));
        contactHelpBox.addView(tvBishopWa);

        // SMS Pastoral Support Row
        TextView tvBishopSms = new TextView(context);
        tvBishopSms.setText("✉️ SMS Pastoral Line: (+254) 0710 772 227");
        tvBishopSms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBishopSms.setTypeface(Typeface.DEFAULT_BOLD);
        tvBishopSms.setTextColor(COLOR_PRIMARY_PURPLE);
        tvBishopSms.setPadding(0, dp(context, 3), 0, dp(context, 3));
        tvBishopSms.setOnClickListener(v -> ChurchIntentManager.sendSms(
                context, "+254710772227", "Perazim Giving / Pastoral Support Request: "));
        contactHelpBox.addView(tvBishopSms);

        // Bishop Phone Row
        TextView tvBishopPhone = new TextView(context);
        tvBishopPhone.setText("📞 Call Bishop: (+254) 0710 772 227");
        tvBishopPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBishopPhone.setTypeface(Typeface.DEFAULT_BOLD);
        tvBishopPhone.setTextColor(COLOR_PURPLE_DARK);
        tvBishopPhone.setPadding(0, dp(context, 3), 0, dp(context, 3));
        tvBishopPhone.setOnClickListener(v -> ChurchIntentManager.dialPhone(context, "+254710772227"));
        contactHelpBox.addView(tvBishopPhone);

        // Church Email Row
        TextView tvChurchEmail = new TextView(context);
        tvChurchEmail.setText("✉️ Church: info@perazimchurch.org");
        tvChurchEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvChurchEmail.setTypeface(Typeface.DEFAULT_BOLD);
        tvChurchEmail.setTextColor(COLOR_PRIMARY_PURPLE);
        tvChurchEmail.setPadding(0, dp(context, 3), 0, dp(context, 3));
        tvChurchEmail.setOnClickListener(v -> ChurchIntentManager.sendEmail(
                context, "info@perazimchurch.org", "Perazim Giving Inquiry", "Peace be with you Perazim Mission Church office,"));
        contactHelpBox.addView(tvChurchEmail);

        // Bishop Email Row
        TextView tvBishopEmail = new TextView(context);
        tvBishopEmail.setText("✉️ Bishop: bishop@perazimchurch.org");
        tvBishopEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvBishopEmail.setTypeface(Typeface.DEFAULT_BOLD);
        tvBishopEmail.setTextColor(COLOR_ACCENT_ORANGE);
        tvBishopEmail.setPadding(0, dp(context, 3), 0, dp(context, 4));
        tvBishopEmail.setOnClickListener(v -> ChurchIntentManager.sendEmail(
                context, "bishop@perazimchurch.org", "Giving Covenant / Pastoral Support", "Peace be with you Bishop Dr. David Mutweri,"));
        contactHelpBox.addView(tvBishopEmail);

        container.addView(contactHelpBox);

        dlg.show();
        return dlg;
    }

    private static int dp(@NonNull Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private static Drawable createPillBg(@NonNull Context context, int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(context, 16));
        g.setStroke(dp(context, 1), strokeColor);
        return g;
    }

    private static Drawable create3dButtonDrawable(@NonNull Context context, int faceColor, int shadowColor, int radiusDp, int shadowDepthDp) {
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(shadowColor);
        shadow.setCornerRadius(dp(context, radiusDp));

        GradientDrawable face = new GradientDrawable();
        face.setColor(faceColor);
        face.setCornerRadius(dp(context, radiusDp));

        LayerDrawable layers = new LayerDrawable(new Drawable[]{shadow, face});
        layers.setLayerInset(0, 0, dp(context, shadowDepthDp), 0, 0);
        layers.setLayerInset(1, 0, 0, 0, dp(context, shadowDepthDp));
        return layers;
    }
}
