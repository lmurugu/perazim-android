package org.perazimchurch.app.presentation.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Clean, contemplative authentication dialog for Phone Number + 4-digit PIN sign in and registration.
 * Allows members to connect multiple devices while maintaining offline-first guest resilience.
 */
public final class AuthDialog {

    public interface AuthListener {
        void onAuthenticated(@NonNull UserEntity user);
    }

    private AuthDialog() {}

    public static void show(@NonNull Context context, @Nullable AuthListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20));

        // Header Title
        TextView tvTitle = new TextView(context);
        tvTitle.setText("Perazim Member Account");
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvSubtitle = new TextView(context);
        tvSubtitle.setText("Connect your prayer walk, streak, and fellowship across your phones.");
        tvSubtitle.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvSubtitle.setTextSize(13);
        tvSubtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(context, 4);
        subLp.bottomMargin = dp(context, 16);
        root.addView(tvSubtitle, subLp);

        // Tab Selector Row (Sign In vs Create Account)
        final boolean[] isRegisterMode = { false };
        LinearLayout tabRow = new LinearLayout(context);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setBackground(PerazimTheme.createCardGradient(dp(context, 10), dp(context, 1)));
        tabRow.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        Button btnTabLogin = new Button(context);
        btnTabLogin.setText("Sign In");
        btnTabLogin.setTextSize(14);
        btnTabLogin.setTypeface(Typeface.DEFAULT_BOLD);
        btnTabLogin.setLayoutParams(new LinearLayout.LayoutParams(0, dp(context, 38), 1f));

        Button btnTabRegister = new Button(context);
        btnTabRegister.setText("Create Account");
        btnTabRegister.setTextSize(14);
        btnTabRegister.setTypeface(Typeface.DEFAULT_BOLD);
        btnTabRegister.setLayoutParams(new LinearLayout.LayoutParams(0, dp(context, 38), 1f));

        tabRow.addView(btnTabLogin);
        tabRow.addView(btnTabRegister);
        root.addView(tabRow);

        // Form Fields Container
        LinearLayout formContainer = new LinearLayout(context);
        formContainer.setOrientation(LinearLayout.VERTICAL);
        formContainer.setPadding(0, dp(context, 16), 0, 0);

        // Name field (for registration only)
        final LinearLayout layoutName = new LinearLayout(context);
        layoutName.setOrientation(LinearLayout.VERTICAL);
        TextView lblName = new TextView(context);
        lblName.setText("Full Name");
        lblName.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        lblName.setTextSize(12);
        layoutName.addView(lblName);

        final EditText etName = new EditText(context);
        etName.setHint("e.g. John Mwangi");
        etName.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etName.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etName.setTextSize(15);
        etName.setBackground(PerazimTheme.createCardGradient(dp(context, 8), dp(context, 1)));
        etName.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        layoutName.addView(etName);
        layoutName.setVisibility(View.GONE);
        formContainer.addView(layoutName);

        // Campus selector (for registration only)
        final LinearLayout layoutCampus = new LinearLayout(context);
        layoutCampus.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams campusLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        campusLp.topMargin = dp(context, 10);
        layoutCampus.setLayoutParams(campusLp);

        TextView lblCampus = new TextView(context);
        lblCampus.setText("Home Campus");
        lblCampus.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        lblCampus.setTextSize(12);
        layoutCampus.addView(lblCampus);

        final Spinner spCampus = new Spinner(context);
        final List<String> campusDisplay = Arrays.asList(
                "Embu Headquarters (Central)",
                "Nairobi Fellowship",
                "Meru Outreach",
                "Online Global Campus"
        );
        final List<String> campusIds = Arrays.asList(
                "campus_embu",
                "campus_nairobi",
                "campus_meru",
                "campus_online"
        );
        ArrayAdapter<String> campusAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, campusDisplay);
        spCampus.setAdapter(campusAdapter);
        spCampus.setBackground(PerazimTheme.createCardGradient(dp(context, 8), dp(context, 1)));
        layoutCampus.addView(spCampus);
        layoutCampus.setVisibility(View.GONE);
        formContainer.addView(layoutCampus);

        // Phone Number field
        TextView lblPhone = new TextView(context);
        lblPhone.setText("Phone Number (Safaricom / Airtel)");
        lblPhone.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        lblPhone.setTextSize(12);
        LinearLayout.LayoutParams phoneLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        phoneLp.topMargin = dp(context, 10);
        formContainer.addView(lblPhone, phoneLp);

        final EditText etPhone = new EditText(context);
        etPhone.setHint("e.g. 0712345678");
        etPhone.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etPhone.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etPhone.setTextSize(15);
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setBackground(PerazimTheme.createCardGradient(dp(context, 8), dp(context, 1)));
        etPhone.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        formContainer.addView(etPhone);

        // 4-Digit PIN field
        TextView lblPin = new TextView(context);
        lblPin.setText("4-Digit Security PIN");
        lblPin.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        lblPin.setTextSize(12);
        LinearLayout.LayoutParams pinLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pinLp.topMargin = dp(context, 10);
        formContainer.addView(lblPin, pinLp);

        final EditText etPin = new EditText(context);
        etPin.setHint("••••");
        etPin.setHintTextColor(PerazimTheme.COLOR_TEXT_DISABLED);
        etPin.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        etPin.setTextSize(16);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(6) });
        etPin.setBackground(PerazimTheme.createCardGradient(dp(context, 8), dp(context, 1)));
        etPin.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        formContainer.addView(etPin);

        // Loading Spinner
        final ProgressBar progressBar = new ProgressBar(context);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progLp.gravity = Gravity.CENTER_HORIZONTAL;
        progLp.topMargin = dp(context, 12);
        formContainer.addView(progressBar, progLp);

        // Primary Action Button (Exactly ONE solid orange CTA per screen/dialog)
        final Button btnSubmit = new Button(context);
        btnSubmit.setText("Sign In");
        btnSubmit.setTextSize(15);
        btnSubmit.setTextColor(Color.WHITE);
        btnSubmit.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmit.setBackground(PerazimTheme.createPrimaryOrangeGradient(dp(context, 10)));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 46));
        btnLp.topMargin = dp(context, 16);
        formContainer.addView(btnSubmit, btnLp);

        // Continue as Guest Option
        final Button btnGuest = new Button(context);
        btnGuest.setText("Continue Offline as Guest");
        btnGuest.setTextSize(13);
        btnGuest.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        btnGuest.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams guestLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40));
        guestLp.topMargin = dp(context, 6);
        formContainer.addView(btnGuest, guestLp);

        root.addView(formContainer);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(root);
        builder.setView(scrollView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(PerazimTheme.createDialogGradient(dp(context, 18), dp(context, 1)));
        }

        // Tab Switching Logic
        final Runnable updateTabs = () -> {
            if (isRegisterMode[0]) {
                btnTabRegister.setBackground(PerazimTheme.createSurface2Gradient(dp(context, 8), dp(context, 1)));
                btnTabRegister.setTextColor(Color.WHITE);
                btnTabLogin.setBackgroundColor(Color.TRANSPARENT);
                btnTabLogin.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
                layoutName.setVisibility(View.VISIBLE);
                layoutCampus.setVisibility(View.VISIBLE);
                btnSubmit.setText("Create Account");
            } else {
                btnTabLogin.setBackground(PerazimTheme.createSurface2Gradient(dp(context, 8), dp(context, 1)));
                btnTabLogin.setTextColor(Color.WHITE);
                btnTabRegister.setBackgroundColor(Color.TRANSPARENT);
                btnTabRegister.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
                layoutName.setVisibility(View.GONE);
                layoutCampus.setVisibility(View.GONE);
                btnSubmit.setText("Sign In");
            }
        };
        updateTabs.run();

        btnTabLogin.setOnClickListener(v -> {
            isRegisterMode[0] = false;
            updateTabs.run();
        });

        btnTabRegister.setOnClickListener(v -> {
            isRegisterMode[0] = true;
            updateTabs.run();
        });

        btnGuest.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "Continuing offline in Guest Mode.", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String pin = etPin.getText().toString().trim();

            if (phone.isEmpty()) {
                Toast.makeText(context, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pin.length() < 4) {
                Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);

            SessionManager sm = SessionManager.getInstance(context);

            if (isRegisterMode[0]) {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show();
                    return;
                }
                int campusIdx = spCampus.getSelectedItemPosition();
                String campusId = (campusIdx >= 0 && campusIdx < campusIds.size()) ? campusIds.get(campusIdx) : "campus_embu";

                sm.registerWithPhoneAndPin(phone, pin, name, campusId, new SessionManager.SessionAuthCallback() {
                    @Override
                    public void onSuccess(@NonNull UserEntity user) {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        dialog.dismiss();
                        Toast.makeText(context, "Welcome, " + user.getName() + "!", Toast.LENGTH_LONG).show();
                        if (listener != null) listener.onAuthenticated(user);
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                sm.loginWithPhoneAndPin(phone, pin, new SessionManager.SessionAuthCallback() {
                    @Override
                    public void onSuccess(@NonNull UserEntity user) {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        dialog.dismiss();
                        Toast.makeText(context, "Welcome back, " + user.getName() + "!", Toast.LENGTH_LONG).show();
                        if (listener != null) listener.onAuthenticated(user);
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
