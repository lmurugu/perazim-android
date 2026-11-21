package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.function.BooleanSupplier;

/**
 * Everything a registered game screen needs from the games hub: the activity, the back action,
 * the score store, the owner's key prefix (from GamesHubDialog.ownerKey), the owner check and the
 * seed reset used by Replay. Stars and progress are saved under the owner prefix, so they belong to
 * the account, not the device.
 */
public final class GameContext {

    private final Activity activity;
    private final Runnable hub;
    private final SharedPreferences scores;
    private final String scorePrefix;
    private final BooleanSupplier ownerCheck;
    private final Runnable seedReset;
    private GameUi ui;

    public GameContext(Activity activity, Runnable hub, SharedPreferences scores, String scorePrefix,
                       BooleanSupplier ownerCheck, Runnable seedReset) {
        this.activity = activity;
        this.hub = hub;
        this.scores = scores;
        this.scorePrefix = scorePrefix == null ? "" : scorePrefix;
        this.ownerCheck = ownerCheck;
        this.seedReset = seedReset;
    }

    public Activity activity() { return activity; }
    /** Returns to the games list. */
    public Runnable hub() { return hub; }
    public SharedPreferences scores() { return scores; }
    /** Key prefix for this account. Stars and progress for a game are stored under this prefix plus its id. */
    public String scorePrefix() { return scorePrefix; }
    public BooleanSupplier ownerCheck() { return ownerCheck; }
    public Runnable seedReset() { return seedReset; }

    /** A GameUi already configured with this context. The same instance is returned each time. */
    public GameUi ui() {
        if (ui == null) ui = new GameUi(activity, hub).configure(scores, scorePrefix, ownerCheck, seedReset);
        return ui;
    }
}
