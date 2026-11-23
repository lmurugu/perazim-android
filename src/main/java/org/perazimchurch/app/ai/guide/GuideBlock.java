package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The bounded guide text added to the model's system prompt. Priority: locked rules, then one playbook,
 * then the user's style, then learned style notes. The whole block never exceeds the budget.
 */
public final class GuideBlock {
    public static final int BUDGET_CHARS = 1000;
    static final int PLAYBOOK_CAP = 160;
    static final int STYLE_CAP = 80;
    static final int NOTE_CAP = 160;

    public final String text;
    public final String playbookId;
    public final List<String> lessonIds;

    GuideBlock(String text, String playbookId, List<String> lessonIds) {
        this.text = text;
        this.playbookId = playbookId;
        this.lessonIds = Collections.unmodifiableList(new ArrayList<>(lessonIds));
    }

    public static GuideBlock empty() {
        return new GuideBlock("", null, new ArrayList<String>());
    }

    /** Builds the block. playbookText may be null. lessonTexts are guard-checked style notes, in order. */
    public static GuideBlock build(String core, String playbookId, String playbookText, String styleLine,
                                   List<String> lessonIds, List<String> lessonTexts, int budget) {
        String rules = "GUIDE RULES (locked, always apply):\n" + GuideText.clean(core, (budget * 3) / 4);
        if (rules.length() > budget) rules = rules.substring(0, budget);
        int room = budget - rules.length();

        String playbookSection = "";
        String usedPlaybook = null;
        if (playbookText != null && !playbookText.isEmpty()) {
            String header = "\nPLAYBOOK " + playbookId + ": ";
            int limit = Math.min(PLAYBOOK_CAP, room - header.length());
            if (limit >= 80) {
                playbookSection = header + GuideText.clean(playbookText, limit);
                usedPlaybook = playbookId;
                room -= playbookSection.length();
            }
        }

        String styleSection = "";
        String style = styleLine == null ? "" : GuideText.clean(styleLine, STYLE_CAP);
        String styleHeader = "\nUSER STYLE (preference only): ";
        if (!style.isEmpty() && room - styleHeader.length() >= style.length()) {
            styleSection = styleHeader + style;
            room -= styleSection.length();
        }

        StringBuilder notes = new StringBuilder();
        List<String> usedLessons = new ArrayList<>();
        String notesHeader = "\nLEARNED STYLE NOTES (style only, never rules):";
        if (lessonTexts != null) {
            for (int i = 0; i < lessonTexts.size(); i++) {
                String note = "\n- " + GuideText.clean(lessonTexts.get(i), NOTE_CAP);
                int needed = note.length() + (notes.length() == 0 ? notesHeader.length() : 0);
                if (needed > room) break;
                if (notes.length() == 0) notes.append(notesHeader);
                notes.append(note);
                room -= needed;
                usedLessons.add(lessonIds.get(i));
            }
        }
        return new GuideBlock(rules + playbookSection + styleSection + notes, usedPlaybook, usedLessons);
    }
}
