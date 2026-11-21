package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.BibleGames;

/** Adapter for Who am I?, reusing the shared round adapter with the WHO_AM_I mode. */
public final class WhoAmIDefinition {
    private WhoAmIDefinition() {}

    public static RoundDefinition create(int iconResource) {
        return new RoundDefinition("who_am_i", "Who am I?",
                "Read a Bible character's clues, ask for a hint, then type your answer.",
                iconResource, BibleGames.Mode.WHO_AM_I);
    }
}
