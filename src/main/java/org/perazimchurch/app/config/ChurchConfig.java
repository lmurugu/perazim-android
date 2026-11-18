package org.perazimchurch.app.config;

/**
 * The single source of church contact and giving details used by the app.
 * Change these values here only. Other code and the README refer to this class.
 */
public final class ChurchConfig {
    /** Pastoral line in international format, for calls, SMS and WhatsApp. */
    public static final String PASTORAL_PHONE_E164 = "+254710772227";
    /** Same number without the plus sign, used by WhatsApp links. */
    public static final String PASTORAL_PHONE_DIGITS = "254710772227";
    /** Same number as shown to members. */
    public static final String PASTORAL_PHONE_DISPLAY = "(+254) 0710 772 227";
    /** Same number in the short display form used on contact rows. */
    public static final String PASTORAL_PHONE_DISPLAY_INTL = "(+254) 710 772 227";
    /** Same number, spaced for printed text. */
    public static final String PASTORAL_PHONE_SPACED = "+254 710 772 227";
    /** M-Pesa Paybill for giving. */
    public static final String MPESA_PAYBILL = "4069983";

    private ChurchConfig() {}
}
