package org.perazimchurch.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinPolicyTest {
    @Test public void newPinNeedsSixDigits() {
        assertEquals("PIN must be 6 to 12 digits", PinPolicy.validateNewPin("12345"));
        assertNull(PinPolicy.validateNewPin("123456"));
        assertNull(PinPolicy.validateNewPin("123456789012"));
        assertEquals("PIN must be 6 to 12 digits", PinPolicy.validateNewPin("1234567890123"));
        assertEquals("PIN must be 6 to 12 digits", PinPolicy.validateNewPin("12a456"));
    }

    @Test public void signInAcceptsLegacyShortPins() {
        assertNull(PinPolicy.validateSignInPin("1234"));
        assertNull(PinPolicy.validateSignInPin("12345"));
        assertEquals("PIN must be 4 to 12 digits", PinPolicy.validateSignInPin("123"));
    }

    @Test public void shortPinsAreOfferedAnUpgrade() {
        assertTrue(PinPolicy.needsUpgrade("1234"));
        assertTrue(PinPolicy.needsUpgrade("12345"));
        assertFalse(PinPolicy.needsUpgrade("123456"));
        assertFalse(PinPolicy.needsUpgrade("12"));
    }
}
