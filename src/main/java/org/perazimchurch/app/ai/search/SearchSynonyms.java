package org.perazimchurch.app.ai.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Words that mean the same thing in English Bible text, so a question about one of them also finds the others.
 * Each group is a general English word family, for example "devil" and "serpent" both name the adversary. The
 * groups are not tied to any question.
 */
public final class SearchSynonyms {

    private static final List<List<String>> GROUPS = Collections.unmodifiableList(Arrays.asList(
            Arrays.asList("satan", "devil", "adversary", "accuser", "serpent", "dragon", "lucifer", "tempter"),
            Arrays.asList("cast", "throw", "thrown", "threw", "hurl", "hurled"),
            Arrays.asList("down", "fall", "fallen", "fell", "overthrow", "overthrown")));

    private SearchSynonyms() {
    }

    /** The word itself first, then every other word of its group, all lower case, without duplicates. */
    public static List<String> forms(String word) {
        String lower = word == null ? "" : word.toLowerCase(Locale.ROOT).trim();
        List<String> forms = new ArrayList<>();
        if (lower.isEmpty()) {
            return forms;
        }
        forms.add(lower);
        for (List<String> group : GROUPS) {
            if (group.contains(lower)) {
                for (String other : group) {
                    if (!forms.contains(other)) {
                        forms.add(other);
                    }
                }
            }
        }
        return forms;
    }
}
