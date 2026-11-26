package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Story Timeline: tap a chapter's events in the order the text tells them. Twelve levels in three
 * tiers: Easy (1 to 4) and Medium (5 to 8) use four events and offer a hint; Hard (9 to 12) uses
 * five events and offers no hint. Chapters come from the shared StoryChapterBank.
 */
public final class TimelineDefinition implements GameDefinition {

    public static final int LEVELS = 12;

    /** Difficulty tier. Easy and Medium offer a hint; Hard does not. */
    public enum Tier { EASY, MEDIUM, HARD }

    private final int iconResource;

    public TimelineDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "story_timeline"; }
    @Override public String title() { return "Story Timeline"; }
    @Override public String description() { return "Tap the Bible events in the order they happen in the story."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return LEVELS; }

    @Override
    public String levelName(int level) {
        int n = clamp(level);
        return tierLabel(tier(n)) + " " + ((n - 1) % 4 + 1);
    }

    /** Tier for a 1-based level, clamped to the valid range. */
    public static Tier tier(int level) {
        int n = clamp(level);
        if (n <= 4) return Tier.EASY;
        if (n <= 8) return Tier.MEDIUM;
        return Tier.HARD;
    }

    /** Events in a run for the tier: four on Easy and Medium, five on Hard. */
    public static int eventsFor(Tier tier) {
        return tier == Tier.HARD ? 5 : 4;
    }

    @Override
    public GameSession createSession(int level, long seed) {
        int n = clamp(level);
        Tier tier = tier(n);
        int needed = eventsFor(tier);
        List<StoryChapterBank.Chapter> usable = new ArrayList<>();
        for (StoryChapterBank.Chapter c : StoryChapterBank.CHAPTERS) {
            if (c.events.size() >= needed) usable.add(c);
        }
        Collections.shuffle(usable, new Random(seed));
        StoryChapterBank.Chapter chapter = usable.get((n - 1) % usable.size());
        return new StoryTimelineSession(chapter.events.subList(0, needed), tier, seed);
    }

    static int clamp(int level) {
        return Math.max(1, Math.min(LEVELS, level));
    }

    private static String tierLabel(Tier tier) {
        switch (tier) {
            case EASY: return "Easy";
            case MEDIUM: return "Medium";
            default: return "Hard";
        }
    }
}
