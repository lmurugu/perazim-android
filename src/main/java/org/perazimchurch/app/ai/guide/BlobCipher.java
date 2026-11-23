package org.perazimchurch.app.ai.guide;

import java.security.GeneralSecurityException;

/** Seals and opens the guide's personal files. The Android implementation uses the Keystore. */
public interface BlobCipher {
    byte[] seal(byte[] plain) throws GeneralSecurityException;

    /** Returns null when the sealed data is damaged or was made with another key. */
    byte[] open(byte[] sealed);
}
