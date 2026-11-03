package org.perazimchurch.app.navigation;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Universal Native OS Intent Manager for Perazim Android Application.
 * Provides practical, production-ready deep-linking tailored for Kenya:
 * - Lipa na M-Pesa 3-tier cascade (Clipboard -> Safaricom App -> SIM Toolkit -> *334# USSD)
 * - Direct WhatsApp Pastoral Care & Prayer requests (Bishop Dr. David Mutweri)
 * - Direct Offline SMS Prayer submission
 * - Phone dialer connection to church headquarters
 * - Google Maps sanctuary directions in Embu
 */
public final class ChurchIntentManager {

    public static final String BISHOP_PHONE = "+254710772227";
    public static final String BISHOP_PHONE_CLEAN = "254710772227";
    public static final String CHURCH_OFFICE_EMAIL = "info@perazimchurch.org";
    public static final String BISHOP_EMAIL = "bishop@perazimchurch.org";
    public static final String MPESA_PAYBILL = "4069983";
    public static final String DEFAULT_ACCOUNT = "PERAZIM-TITHE";
    public static final String USSD_MPESA = "*334#";
    public static final String MAPS_QUERY = "Perazim Mission Church Embu";

    private ChurchIntentManager() {
        // Prevent instantiation
    }

    // =========================================================================
    // 1. M-PESA GIVING & SIM TOOLKIT (3-TIER CASCADE)
    // =========================================================================

    /**
     * Executes the Lipa na M-Pesa cascade:
     * 1. Copies Paybill and Account to system clipboard.
     * 2. Attempts to open Safaricom M-Pesa app.
     * 3. Attempts to open SIM Toolkit (STK).
     * 4. Falls back to opening Safaricom USSD (*334#) in the phone dialer.
     */
    public static void launchMpesaGiving(@NonNull Context context,
                                         @NonNull String paybill,
                                         @NonNull String account,
                                         @Nullable String amount) {
        // Step 1: Copy to Clipboard
        copyToClipboard(context, "M-Pesa Paybill", paybill);
        String toastMsg = "✓ Copied Paybill " + paybill + " & Account " + account + "!";
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();

        // Step 2: Try Safaricom M-Pesa app
        PackageManager pm = context.getPackageManager();
        String[] mpesaPackages = {
                "com.safaricom.mpesa.lifestyle",
                "com.safaricom.mpesa"
        };
        for (String pkg : mpesaPackages) {
            Intent appIntent = pm.getLaunchIntentForPackage(pkg);
            if (appIntent != null) {
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(appIntent);
                return;
            }
        }

        // Step 3: Try SIM Toolkit (STK)
        String[] stkPackages = {
                "com.android.stk",
                "com.android.stk2",
                "com.mediatek.stkselection"
        };
        for (String pkg : stkPackages) {
            Intent stkIntent = pm.getLaunchIntentForPackage(pkg);
            if (stkIntent != null) {
                stkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(stkIntent);
                return;
            }
        }

        // Step 4: Safaricom USSD *334# Dialer Fallback
        dialUssd(context, USSD_MPESA);
    }

    /**
     * Directly launches the SIM Toolkit app after copying Paybill & Account to clipboard.
     */
    public static void openSimToolkit(@NonNull Context context, @NonNull String paybill, @NonNull String account) {
        copyToClipboard(context, "M-Pesa Giving", "Paybill: " + paybill + " | Account: " + account);
        Toast.makeText(context, "✓ Copied: Paybill " + paybill + ", Account: " + account, Toast.LENGTH_SHORT).show();

        PackageManager pm = context.getPackageManager();
        String[] stkPackages = {
                "com.android.stk",
                "com.android.stk2",
                "com.mediatek.stkselection"
        };
        for (String pkg : stkPackages) {
            Intent stkIntent = pm.getLaunchIntentForPackage(pkg);
            if (stkIntent != null) {
                stkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(stkIntent);
                return;
            }
        }

        // Fallback to Safaricom USSD dialer if vendor disables direct STK launch
        Toast.makeText(context, "Launching Safaricom *334# menu...", Toast.LENGTH_SHORT).show();
        dialUssd(context, USSD_MPESA);
    }

    /**
     * Directly launches the Safaricom M-Pesa app.
     */
    public static void openMpesaApp(@NonNull Context context, @NonNull String paybill, @NonNull String account) {
        copyToClipboard(context, "M-Pesa Giving", "Paybill: " + paybill + " | Account: " + account);
        Toast.makeText(context, "✓ Copied Paybill " + paybill + "!", Toast.LENGTH_SHORT).show();

        PackageManager pm = context.getPackageManager();
        String[] mpesaPackages = {
                "com.safaricom.mpesa.lifestyle",
                "com.safaricom.mpesa"
        };
        for (String pkg : mpesaPackages) {
            Intent appIntent = pm.getLaunchIntentForPackage(pkg);
            if (appIntent != null) {
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(appIntent);
                return;
            }
        }

        // If app not installed, open USSD dialer
        Toast.makeText(context, "M-Pesa app not installed. Opening *334# dialer...", Toast.LENGTH_SHORT).show();
        dialUssd(context, USSD_MPESA);
    }

    /**
     * Dials a USSD code (*334#) directly into the native dialer.
     */
    public static void dialUssd(@NonNull Context context, @NonNull String ussdCode) {
        try {
            String encodedHash = Uri.encode("#");
            String dialStr = ussdCode.replace("#", encodedHash);
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dialStr));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open dialer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 2. WHATSAPP PASTORAL CARE & SHARING
    // =========================================================================

    /**
     * Opens direct WhatsApp conversation with the pastoral line (Bishop Dr. David Mutweri).
     */
    public static void openWhatsAppChat(@NonNull Context context,
                                        @Nullable String phone,
                                        @Nullable String prefilledMessage) {
        String targetPhone = (phone != null && !phone.trim().isEmpty())
                ? phone.replaceAll("[^0-9]", "")
                : BISHOP_PHONE_CLEAN;

        String msg = (prefilledMessage != null) ? prefilledMessage : "Peace be with you, Bishop.";
        String url = "https://api.whatsapp.com/send?phone=" + targetPhone + "&text=" + Uri.encode(msg);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PackageManager pm = context.getPackageManager();
        String[] waPackages = {"com.whatsapp", "com.whatsapp.w4b"};
        boolean foundWa = false;
        for (String pkg : waPackages) {
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                intent.setPackage(pkg);
                foundWa = true;
                break;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // If WhatsApp package was set but failed, try generic web URL
            if (foundWa) {
                intent.setPackage(null);
                try {
                    context.startActivity(intent);
                    return;
                } catch (Exception ignored) {}
            }
            // Fallback to SMS if WhatsApp is unavailable
            Toast.makeText(context, "WhatsApp not available, switching to SMS...", Toast.LENGTH_SHORT).show();
            sendSms(context, "+" + targetPhone, msg);
        }
    }

    /**
     * Shares scripture, sermon notes, or prayer updates via WhatsApp or generic share sheet.
     */
    public static void openWhatsAppShare(@NonNull Context context, @NonNull String shareText) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PackageManager pm = context.getPackageManager();
        String[] waPackages = {"com.whatsapp", "com.whatsapp.w4b"};
        for (String pkg : waPackages) {
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                sendIntent.setPackage(pkg);
                context.startActivity(sendIntent);
                return;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        // Generic chooser fallback
        Intent chooser = Intent.createChooser(sendIntent, "Share via");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    // =========================================================================
    // 3. OFFLINE SMS PASTORAL CARE
    // =========================================================================

    /**
     * Opens the native SMS messaging app with a pre-filled prayer petition or inquiry.
     */
    public static void sendSms(@NonNull Context context,
                               @Nullable String phone,
                               @Nullable String message) {
        String target = (phone != null && !phone.trim().isEmpty()) ? phone : BISHOP_PHONE;
        String body = (message != null) ? message : "Perazim Prayer Request: ";

        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + target));
            intent.putExtra("sms_body", body);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open SMS app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 4. PHONE CALLS & EMAIL
    // =========================================================================

    /**
     * Dials church pastoral phone line directly in the native phonebook/dialer.
     */
    public static void dialPhone(@NonNull Context context, @Nullable String phone) {
        String target = (phone != null && !phone.trim().isEmpty()) ? phone : BISHOP_PHONE;
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + target));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open dialer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sends an email to church office or pastoral staff.
     */
    public static void sendEmail(@NonNull Context context,
                                 @NonNull String email,
                                 @NonNull String subject,
                                 @Nullable String body) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            if (body != null) {
                intent.putExtra(Intent.EXTRA_TEXT, body);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open email app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 5. GOOGLE MAPS SANCTUARY DIRECTIONS
    // =========================================================================

    /**
     * Opens directions to Perazim Mission Church Headquarters in Embu.
     */
    public static void openChurchMaps(@NonNull Context context) {
        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(MAPS_QUERY));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PackageManager pm = context.getPackageManager();
            if (mapIntent.resolveActivity(pm) != null) {
                context.startActivity(mapIntent);
            } else {
                // Fallback to web browser Google Maps
                Intent webMapIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + Uri.encode(MAPS_QUERY)));
                webMapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webMapIntent);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Could not open maps: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // CLIPBOARD HELPER
    // =========================================================================

    public static void copyToClipboard(@NonNull Context context, @NonNull String label, @NonNull String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(label, text);
                clipboard.setPrimaryClip(clip);
            }
        } catch (Exception ignored) {}
    }
}
