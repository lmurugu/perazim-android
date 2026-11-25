package org.perazimchurch.app.ai.library;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * NO-GO ZONES: categories that still fail their gate after a full evaluation round. In a no-go zone the guide gives
 * one fixed, warm decline that names a trusted church leader, and the model never answers. A zone is lifted only by
 * a rerun that passes its gate. Safety and privacy are never listed here.
 *
 * The list is empty in this build: no category has been declared a no-go zone yet (see Status.md).
 */
public final class NoGoZones {

    /** One declared zone: a stable id, the question shapes it covers, and the fixed decline. */
    public static final class Zone {
        public final String id;
        public final Pattern pattern;
        public final String decline;

        Zone(String id, String pattern, String decline) {
            this.id = id;
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.decline = decline;
        }
    }

    private static final List<Zone> ZONES = Collections.emptyList();

    private NoGoZones() {}

    /** The zone that covers this question, or null when the question is not in a no-go zone. */
    public static Zone match(String question) {
        if (question == null) return null;
        String normalized = question.toLowerCase(Locale.ROOT);
        for (Zone z : ZONES) {
            if (z.pattern.matcher(normalized).find()) return z;
        }
        return null;
    }

    /** Every declared zone, for the report and for tests. */
    public static List<Zone> all() {
        return ZONES;
    }
}
