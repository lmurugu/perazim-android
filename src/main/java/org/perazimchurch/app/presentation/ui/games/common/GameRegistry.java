package org.perazimchurch.app.presentation.ui.games.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps a game id to the code that opens its own play screen or dialog. The Games hub asks this
 * registry before its built-in list, passing a {@link GameContext} with the owner's scores, so a game
 * can be added without editing the hub. Each game registers its launcher once, from
 * {@link GameLaunchers#registerAll()}. See docs/games/ENGINE_USAGE.md.
 */
public final class GameRegistry {

    /** Opens a game's own screen with the hub's context. */
    public interface Launcher {
        void launch(GameContext ctx);
    }

    private static final Map<String, Launcher> LAUNCHERS = new ConcurrentHashMap<>();

    private GameRegistry() {}

    /** Registers a launcher for a game id. A later registration for the same id replaces the first. */
    public static void register(String gameId, Launcher launcher) {
        if (gameId == null || launcher == null) throw new IllegalArgumentException("gameId and launcher are required");
        LAUNCHERS.put(gameId, launcher);
    }

    /** True when a launcher is registered for this id. */
    public static boolean isRegistered(String gameId) {
        return LAUNCHERS.containsKey(gameId);
    }

    /** Opens the registered screen for a game. Returns false when none is registered. */
    public static boolean launch(String gameId, GameContext ctx) {
        Launcher launcher = LAUNCHERS.get(gameId);
        if (launcher == null) return false;
        launcher.launch(ctx);
        return true;
    }
}
