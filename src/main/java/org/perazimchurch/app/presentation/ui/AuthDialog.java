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
import android.widget.CheckBox;
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
import org.perazimchurch.app.data.remote.PrivacyNoticeRemoteSource;
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

        final LinearLayout privacyContainer = new LinearLayout(context);
        privacyContainer.setOrientation(LinearLayout.VERTICAL);
        privacyContainer.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        privacyContainer.setBackground(PerazimTheme.createCardGradient(dp(context, 8), dp(context, 1)));
        final TextView tvPrivacyStatus = new TextView(context);
        tvPrivacyStatus.setText("Load the current notice before creating an account.");
        tvPrivacyStatus.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvPrivacyStatus.setTextSize(13);
        privacyContainer.addView(tvPrivacyStatus);
        final Button btnReadNotice = new Button(context);
        btnReadNotice.setText("Read Perazim App Privacy Notice");
        btnReadNotice.setTextSize(13);
        btnReadNotice.setEnabled(false);
        privacyContainer.addView(btnReadNotice);
        final CheckBox chkPrivacyAcknowledgement = new CheckBox(context);
        chkPrivacyAcknowledgement.setText("I acknowledge that PMCI has provided me with the Perazim App Privacy Notice and that I can read the full PMCI Data Protection and Privacy Policy here. I understand how to contact PMCI about my personal information.");
        chkPrivacyAcknowledgement.setTextSize(13);
        chkPrivacyAcknowledgement.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        chkPrivacyAcknowledgement.setChecked(false);
        chkPrivacyAcknowledgement.setEnabled(false);
        privacyContainer.addView(chkPrivacyAcknowledgement);
        privacyContainer.setVisibility(View.GONE);
        formContainer.addView(privacyContainer);

        final String[] privacyVersion = {""};
        final String[] privacyHash = {""};
        final String[] privacyContent = {""};
        final long[] privacyPresentedAt = {0L};
        final boolean[] privacyLoadStarted = {false};

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

        // PIN field; pilot administrator setup permits PINs up to 12 digits.
        TextView lblPin = new TextView(context);
        lblPin.setText("Security PIN (4–12 digits)");
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
        etPin.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(12) });
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

        final Button btnReadPolicy = new Button(context);
        btnReadPolicy.setText("Privacy and data protection policy");
        btnReadPolicy.setTextSize(13);
        btnReadPolicy.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        btnReadPolicy.setBackgroundColor(Color.TRANSPARENT);
        formContainer.addView(btnReadPolicy);

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
                privacyContainer.setVisibility(View.VISIBLE);
                btnSubmit.setText("Create Account");
                if (!privacyLoadStarted[0]) {
                    privacyLoadStarted[0] = true;
                    tvPrivacyStatus.setText("Loading the current privacy notice…");
                    PrivacyNoticeRemoteSource.fetchCurrent(context, new PrivacyNoticeRemoteSource.Callback() {
                        @Override public void onLoaded(@NonNull org.json.JSONObject notice) {
                            privacyVersion[0] = notice.optString("version", "");
                            privacyHash[0] = notice.optString("sha256", "");
                            privacyContent[0] = notice.optString("content", "");
                            privacyPresentedAt[0] = notice.optLong("presented_at", 0L);
                            String status = notice.optString("publication_status", "DRAFT");
                            tvPrivacyStatus.setText("Current notice v" + privacyVersion[0] + ("DRAFT".equals(status) ? " · DRAFT FOR REVIEW" : ""));
                            btnReadNotice.setEnabled(!privacyContent[0].isEmpty());
                            chkPrivacyAcknowledgement.setEnabled(!privacyContent[0].isEmpty());
                        }
                        @Override public void onError(@NonNull String message) {
                            privacyLoadStarted[0] = false;
                            privacyVersion[0] = "";
                            privacyHash[0] = "";
                            privacyContent[0] = "";
                            privacyPresentedAt[0] = 0L;
                            tvPrivacyStatus.setText(message);
                            btnReadNotice.setEnabled(false);
                            chkPrivacyAcknowledgement.setEnabled(false);
                            chkPrivacyAcknowledgement.setChecked(false);
                        }
                    });
                }
            } else {
                btnTabLogin.setBackground(PerazimTheme.createSurface2Gradient(dp(context, 8), dp(context, 1)));
                btnTabLogin.setTextColor(Color.WHITE);
                btnTabRegister.setBackgroundColor(Color.TRANSPARENT);
                btnTabRegister.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
                layoutName.setVisibility(View.GONE);
                privacyContainer.setVisibility(View.GONE);
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

        btnReadNotice.setOnClickListener(v -> {
            if (!privacyContent[0].isEmpty()) {
                String title = "Perazim App Privacy Notice v" + privacyVersion[0];
                PrivacyDocumentDialog.show(context, title, privacyContent[0]);
            }
        });
        btnReadPolicy.setOnClickListener(v -> {
            try {
                PrivacyDocumentDialog.show(context, "PMCI Privacy Policy · Draft v0.1",
                        PrivacyDocumentDialog.readAsset(context, "privacy/pmci-public-privacy-policy.md"));
            } catch (Exception e) {
                Toast.makeText(context, "The privacy policy could not be opened.", Toast.LENGTH_LONG).show();
            }
        });

        btnGuest.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "Continuing offline in Guest Mode.", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> {
            if (isRegisterMode[0] && (privacyVersion[0].isEmpty() || privacyHash[0].isEmpty() || privacyContent[0].isEmpty() || privacyPresentedAt[0] <= 0L)) {
                Toast.makeText(context, "The current privacy notice must load before registration.", Toast.LENGTH_LONG).show();
                return;
            }
            if (isRegisterMode[0] && !chkPrivacyAcknowledgement.isChecked()) {
                Toast.makeText(context, "Please read and acknowledge the privacy notice to create an account.", Toast.LENGTH_LONG).show();
                return;
            }
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
                String campusId = "campus_embu";

                sm.registerWithPhoneAndPin(phone, pin, name, campusId, privacyVersion[0], privacyHash[0], privacyPresentedAt[0], new SessionManager.SessionAuthCallback() {
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
                        String authToken = sm.getAuthToken();
                        if (authToken == null || authToken.trim().isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            Toast.makeText(context, "Your secure session could not be restored. Please try again.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        PrivacyNoticeRemoteSource.fetchPresentation(context, authToken, new PrivacyNoticeRemoteSource.PresentationCallback() {
                            @Override public void onReady(@NonNull org.json.JSONObject presentation) {
                                if (presentation.optBoolean("acknowledged", false)) {
                                    finishLogin(context, user, listener, dialog, progressBar, btnSubmit);
                                    return;
                                }
                                String version = presentation.optString("version", "");
                                String content = presentation.optString("content", "");
                                String receipt = presentation.optString("receipt", "");
                                if (version.isEmpty() || content.isEmpty() || receipt.isEmpty()) {
                                    sm.logout();
                                    showPrivacyUnavailable(context, dialog, progressBar, btnSubmit);
                                    return;
                                }
                                new AlertDialog.Builder(context)
                                        .setTitle("Privacy Notice v" + version)
                                        .setMessage(content)
                                        .setPositiveButton("Acknowledge and continue", (noticeDialog, which) -> {
                                            progressBar.setVisibility(View.VISIBLE);
                                            PrivacyNoticeRemoteSource.acknowledge(context, authToken, receipt, new PrivacyNoticeRemoteSource.PresentationCallback() {
                                                @Override public void onReady(@NonNull org.json.JSONObject result) {
                                                    if (result.optBoolean("success", false)) finishLogin(context, user, listener, dialog, progressBar, btnSubmit);
                                                    else { sm.logout(); showPrivacyUnavailable(context, dialog, progressBar, btnSubmit); }
                                                }
                                                @Override public void onError(int statusCode, @NonNull String message) {
                                                    sm.logout();
                                                    showPrivacyUnavailable(context, dialog, progressBar, btnSubmit);
                                                }
                                            });
                                        })
                                        .setNegativeButton("Sign out", (noticeDialog, which) -> {
                                            sm.logout();
                                            progressBar.setVisibility(View.GONE);
                                            btnSubmit.setEnabled(true);
                                            Toast.makeText(context, "You have been signed out.", Toast.LENGTH_SHORT).show();
                                        })
                                        .setCancelable(false)
                                        .show();
                            }
                            @Override public void onError(int statusCode, @NonNull String message) {
                                sm.logout();
                                showPrivacyUnavailable(context, dialog, progressBar, btnSubmit);
                            }
                        });
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

    private static void finishLogin(Context context, UserEntity user, @Nullable AuthListener listener,
                                    AlertDialog dialog, ProgressBar progressBar, Button btnSubmit) {
        progressBar.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
        dialog.dismiss();
        Toast.makeText(context, "Welcome back, " + user.getName() + "!", Toast.LENGTH_LONG).show();
        if (listener != null) listener.onAuthenticated(user);
    }

    private static void showPrivacyUnavailable(Context context, AlertDialog dialog, ProgressBar progressBar, Button btnSubmit) {
        progressBar.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
        Toast.makeText(context, "The current privacy notice could not be acknowledged. You have been signed out; please try again later.", Toast.LENGTH_LONG).show();
    }
}
