package org.perazimchurch.app.presentation.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.perazimchurch.app.navigation.ChurchIntentManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Lipa na M-Pesa Giving & Tithes Dialog.
 * Quiet dark contemplative design matching Perazim design guidelines:
 * - Dialog surface: darkened gradient with hairline stroke
 * - Title: "Give to Perazim Mission Church" (#F5F0F7, 18sp bold) with official logo
 * - Paybill 4069983 on surface 2 gradient
 * - Primary action button: ONE muted/darkened orange gradient "Open M-Pesa"
 * - Secondary quiet actions: SIM Toolkit, Dial *334#
 * - Mandatory trust note: "Giving is processed directly by Safaricom M-Pesa. Perazim app never asks for or stores your M-Pesa PIN."
 * - Zero gamification rewards or points.
 */
public final class GivingDialog {

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
        sv.setBackground(PerazimTheme.createDialogGradient(dp(context, 18), dp(context, 1)));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, 20), dp(context, 22), dp(context, 20), dp(context, 22));
        container.setBackground(PerazimTheme.createDialogGradient(dp(context, 18), dp(context, 1)));

        // 1. Header with Church Logo & Title
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(context);
        logo.setImageResource(org.perazimchurch.app.R.drawable.ic_perazim_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(context, 34), dp(context, 34));
        lpLogo.setMargins(0, 0, dp(context, 10), 0);
        logo.setLayoutParams(lpLogo);
        titleRow.addView(logo);

        TextView title = new TextView(context);
        title.setText("Give to Perazim Mission Church");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        titleRow.addView(title);
        container.addView(titleRow);

        TextView subtitle = new TextView(context);
        subtitle.setText("“Honor the LORD with your wealth, with the firstfruits of all your crops.” — Proverbs 3:9");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        subtitle.setPadding(0, dp(context, 6), 0, dp(context, 16));
        container.addView(subtitle);

        // 2. Paybill Details Box on Surface 2 Gradient
        LinearLayout paybillBox = new LinearLayout(context);
        paybillBox.setOrientation(LinearLayout.VERTICAL);
        paybillBox.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        paybillBox.setBackground(PerazimTheme.createSurface2Gradient(dp(context, 12), dp(context, 1)));

        TextView tvPaybillLabel = new TextView(context);
        tvPaybillLabel.setText("SAFARICOM M-PESA PAYBILL");
        tvPaybillLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvPaybillLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillLabel.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        paybillBox.addView(tvPaybillLabel);

        LinearLayout pbRow = new LinearLayout(context);
        pbRow.setOrientation(LinearLayout.HORIZONTAL);
        pbRow.setGravity(Gravity.CENTER_VERTICAL);
        pbRow.setPadding(0, dp(context, 6), 0, dp(context, 6));

        TextView tvPaybillNum = new TextView(context);
        tvPaybillNum.setText("4069983");
        tvPaybillNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvPaybillNum.setTypeface(Typeface.DEFAULT_BOLD);
        tvPaybillNum.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        LinearLayout.LayoutParams lpPbNum = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvPaybillNum.setLayoutParams(lpPbNum);
        pbRow.addView(tvPaybillNum);

        Button btnCopyPaybill = new Button(context);
        btnCopyPaybill.setText("Copy Paybill");
        btnCopyPaybill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnCopyPaybill.setTypeface(Typeface.DEFAULT_BOLD);
        btnCopyPaybill.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnCopyPaybill.setBackground(PerazimTheme.createActivePillGradient(dp(context, 8)));
        btnCopyPaybill.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));
        pbRow.addView(btnCopyPaybill);
        paybillBox.addView(pbRow);

        final TextView tvAccount = new TextView(context);
        tvAccount.setText("Account: PERAZIM-TITHE");
        tvAccount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvAccount.setTypeface(Typeface.DEFAULT_BOLD);
        tvAccount.setTextColor(PerazimTheme.COLOR_ORANGE_SOFT);
        tvAccount.setPadding(0, dp(context, 2), 0, dp(context, 8));
        paybillBox.addView(tvAccount);

        Button btnCopyAll = new Button(context);
        btnCopyAll.setText("Copy Paybill & Account");
        btnCopyAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnCopyAll.setTypeface(Typeface.DEFAULT_BOLD);
        btnCopyAll.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnCopyAll.setBackground(PerazimTheme.createMutedPillGradient(dp(context, 8), PerazimTheme.COLOR_STROKE, dp(context, 1)));
        btnCopyAll.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        paybillBox.addView(btnCopyAll);

        container.addView(paybillBox);

        // Clipboard Listeners
        btnCopyPaybill.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Paybill", "4069983"));
                Toast.makeText(context, "✓ Copied Paybill 4069983", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyAll.setOnClickListener(v -> {
            String acc = (tvAccount.getText() != null)
                    ? tvAccount.getText().toString().replace("Account: ", "").trim() : "PERAZIM-TITHE";
            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("M-Pesa Giving", "Paybill: 4069983 | Account: " + acc));
                Toast.makeText(context, "✓ Copied: Paybill 4069983, Account " + acc, Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Fund Selector Spinner
        TextView tvFundLabel = new TextView(context);
        tvFundLabel.setText("SELECT GIVING FUND:");
        tvFundLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFundLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvFundLabel.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
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
        tvAmountLabel.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvAmountLabel.setPadding(0, dp(context, 14), 0, dp(context, 6));
        container.addView(tvAmountLabel);

        final EditText etAmount = new EditText(context);
        etAmount.setHint("Enter amount in KES (e.g. 1000)");
        etAmount.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etAmount.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        etAmount.setText("1000");
        etAmount.setBackground(createCardBackground(context, PerazimTheme.COLOR_SURFACE_2, 10, PerazimTheme.COLOR_STROKE, 1));
        etAmount.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));

        LinearLayout chipsRow = new LinearLayout(context);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, 0, 0, dp(context, 8));

        String[] chipAmounts = {"200", "500", "1,000", "2,500"};
        final String[] chipValues = {"200", "500", "1000", "2500"};
        for (int i = 0; i < chipAmounts.length; i++) {
            final String val = chipValues[i];
            Button chip = new Button(context);
            chip.setText(chipAmounts[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            chip.setBackground(createCardBackground(context, PerazimTheme.COLOR_SURFACE_2, 8, PerazimTheme.COLOR_STROKE, 1));
            LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(0, dp(context, 36), 1.0f);
            lpChip.setMargins(dp(context, 2), 0, dp(context, 2), 0);
            chip.setLayoutParams(lpChip);
            chip.setOnClickListener(v -> etAmount.setText(val));
            chipsRow.addView(chip);
        }
        container.addView(chipsRow);
        container.addView(etAmount);

        // 5. ONE Primary Orange CTA: "Open M-Pesa" (Muted/Darkened Orange Gradient)
        Button btnConfirmGive = new Button(context);
        btnConfirmGive.setText("Open M-Pesa");
        btnConfirmGive.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnConfirmGive.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnConfirmGive.setTypeface(Typeface.DEFAULT_BOLD);
        btnConfirmGive.setBackground(PerazimTheme.createPrimaryOrangeGradient(dp(context, 12)));
        LinearLayout.LayoutParams lpGive = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 48));
        lpGive.setMargins(0, dp(context, 16), 0, 0);
        btnConfirmGive.setLayoutParams(lpGive);
        container.addView(btnConfirmGive);

        // 6. Direct Secondary Safaricom Launch Options (Quiet row)
        TextView tvDirectTitle = new TextView(context);
        tvDirectTitle.setText("OTHER SAFARICOM LAUNCH OPTIONS:");
        tvDirectTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvDirectTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvDirectTitle.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvDirectTitle.setPadding(0, dp(context, 14), 0, dp(context, 6));
        container.addView(tvDirectTitle);

        LinearLayout rowDirectActions = new LinearLayout(context);
        rowDirectActions.setOrientation(LinearLayout.HORIZONTAL);
        rowDirectActions.setPadding(0, 0, 0, dp(context, 8));

        Button btnStk = new Button(context);
        btnStk.setText("SIM Toolkit");
        btnStk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnStk.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnStk.setBackground(PerazimTheme.createMutedPillGradient(dp(context, 8), PerazimTheme.COLOR_STROKE, dp(context, 1)));
        LinearLayout.LayoutParams lpStk = new LinearLayout.LayoutParams(0, dp(context, 38), 1.0f);
        lpStk.setMargins(0, 0, dp(context, 4), 0);
        btnStk.setLayoutParams(lpStk);
        rowDirectActions.addView(btnStk);

        Button btnUssd = new Button(context);
        btnUssd.setText("Dial *334#");
        btnUssd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnUssd.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnUssd.setBackground(PerazimTheme.createMutedPillGradient(dp(context, 8), PerazimTheme.COLOR_STROKE, dp(context, 1)));
        LinearLayout.LayoutParams lpUssd = new LinearLayout.LayoutParams(0, dp(context, 38), 1.0f);
        lpUssd.setMargins(dp(context, 4), 0, 0, 0);
        btnUssd.setLayoutParams(lpUssd);
        rowDirectActions.addView(btnUssd);

        container.addView(rowDirectActions);

        // 7. Mandatory Trust Note
        TextView tvTrust = new TextView(context);
        tvTrust.setText("Giving is processed directly by Safaricom M-Pesa. Perazim app never asks for or stores your M-Pesa PIN.");
        tvTrust.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTrust.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvTrust.setGravity(Gravity.CENTER);
        tvTrust.setPadding(0, dp(context, 10), 0, dp(context, 4));
        container.addView(tvTrust);

        sv.addView(container);
        builder.setView(sv);
        final AlertDialog dlg = builder.create();

        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirmGive.setOnClickListener(v -> {
            String amt = etAmount.getText().toString().trim();
            if (amt.isEmpty()) amt = "1000";
            String account = "PERAZIM-TITHE";
            if (tvAccount.getText() != null) {
                account = tvAccount.getText().toString().replace("Account: ", "").trim();
            }
            ChurchIntentManager.launchMpesaGiving(context, "4069983", account, amt);
            dlg.dismiss();
        });

        btnStk.setOnClickListener(v -> {
            String acc = (tvAccount.getText() != null)
                    ? tvAccount.getText().toString().replace("Account: ", "").trim() : "PERAZIM-TITHE";
            ChurchIntentManager.openSimToolkit(context, "4069983", acc);
            dlg.dismiss();
        });

        btnUssd.setOnClickListener(v -> {
            ChurchIntentManager.dialUssd(context, "*334#");
            dlg.dismiss();
        });

        dlg.show();
        return dlg;
    }

    private static int dp(@NonNull Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private static Drawable createCardBackground(@NonNull Context context, int bgColor, int cornerRadiusDp, int strokeColor, int strokeWidthDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(context, cornerRadiusDp));
        g.setStroke(dp(context, strokeWidthDp), strokeColor);
        return g;
    }

    private static Drawable createSolidPill(@NonNull Context context, int bgColor, int cornerRadiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(context, cornerRadiusDp));
        return g;
    }
}
