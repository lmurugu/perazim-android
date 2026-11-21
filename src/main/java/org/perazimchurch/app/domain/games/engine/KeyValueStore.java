package org.perazimchurch.app.domain.games.engine;

/**
 * Minimal storage for local progress. The Android layer backs it with SharedPreferences;
 * tests use an in-memory map. Nothing stored here leaves the phone.
 */
public interface KeyValueStore {
    int getInt(String key, int defaultValue);
    void putInt(String key, int value);
}
