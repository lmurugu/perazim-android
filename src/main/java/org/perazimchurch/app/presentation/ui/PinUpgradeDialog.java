package org.perazimchurch.app.presentation.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.remote.PinRemoteSource;
import org.perazimchurch.app.domain.PinPolicy;

/** Offers a member who signed in with a short PIN the chance to upgrade it. Declining is always allowed. */
final class PinUpgradeDialog {
    private PinUpgradeDialog() {}

    static void offerIfShort(@NonNull Context context, @NonNull String authToken, @NonNull String currentPin) {
        if (!PinPolicy.needsUpgrade(currentPin)) return;

        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad / 2, pad, 0);
        EditText newPin = new EditText(context);
        newPin.setHint("New PIN (6 to 12 digits)");
        newPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        form.addView(newPin);

        new AlertDialog.Builder(context)
                .setTitle("Upgrade your PIN")
                .setMessage("Your PIN is shorter than the current minimum. Choose a new PIN of 6 to 12 digits to keep your account safer. Other devices will need to sign in again.")
                .setView(form)
                .setPositiveButton("Save new PIN", (dialog, which) -> {
                    String candidate = newPin.getText().toString().trim();
                    String error = PinPolicy.validateNewPin(candidate);
                    if (error != null) {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                        return;
                    }
                    PinRemoteSource.changePin(context, authToken, currentPin, candidate, new PinRemoteSource.Callback() {
                        @Override public void onChanged(@NonNull String newToken) {
                            SessionManager.getInstance(context).setAuthToken(newToken);
                            Toast.makeText(context, "Your PIN has been updated.", Toast.LENGTH_LONG).show();
                        }
                        @Override public void onError(@NonNull String message) {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Later", null)
                .show();
    }
}
