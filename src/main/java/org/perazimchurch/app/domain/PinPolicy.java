package org.perazimchurch.app.domain;

import androidx.annotation.Nullable;

/**
 * PIN rules. New PINs must be 6 to 12 digits. Existing members may still sign in
 * with a shorter PIN (4 or 5 digits), and are then offered an upgrade.
 */
public final class PinPolicy {
    public static final int MIN_NEW_LENGTH = 6;
    public static final int MIN_SIGN_IN_LENGTH = 4;
    public static final int MAX_LENGTH = 12;

    private PinPolicy() {}

    /** Returns an error message for a PIN that may not be created, or null if valid. */
    @Nullable
    public static String validateNewPin(@Nullable String pin) {
        if (!isDigits(pin, MIN_NEW_LENGTH, MAX_LENGTH)) {
            return "PIN must be " + MIN_NEW_LENGTH + " to " + MAX_LENGTH + " digits";
        }
        return null;
    }

    /** Returns an error message for a PIN that cannot be used to sign in, or null if valid. */
    @Nullable
    public static String validateSignInPin(@Nullable String pin) {
        if (!isDigits(pin, MIN_SIGN_IN_LENGTH, MAX_LENGTH)) {
            return "PIN must be " + MIN_SIGN_IN_LENGTH + " to " + MAX_LENGTH + " digits";
        }
        return null;
    }

    /** True when a PIN that signed in is shorter than the current minimum. */
    public static boolean needsUpgrade(@Nullable String pin) {
        return pin != null && pin.length() < MIN_NEW_LENGTH && isDigits(pin, MIN_SIGN_IN_LENGTH, MAX_LENGTH);
    }

    private static boolean isDigits(@Nullable String pin, int min, int max) {
        if (pin == null || pin.length() < min || pin.length() > max) return false;
        for (int i = 0; i < pin.length(); i++) {
            if (!Character.isDigit(pin.charAt(i))) return false;
        }
        return true;
    }
}
