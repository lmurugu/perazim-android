package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Chooses the skills whose triggers appear in the question. Crisis always ranks first. */
public final class GuideRouter {
    private GuideRouter() {}

    public static List<GuideDocs.SkillEntry> route(String question, List<GuideDocs.SkillEntry> index, int max) {
        String text = GuideText.normalise(question);
        List<Scored> scored = new ArrayList<>();
        for (int i = 0; i < index.size(); i++) {
            GuideDocs.SkillEntry skill = index.get(i);
            int score = 0;
            for (String trigger : skill.triggers) {
                if (GuideText.hasPhrase(text, trigger)) score++;
            }
            if (score > 0) scored.add(new Scored(skill, score, i));
        }
        Collections.sort(scored, new Comparator<Scored>() {
            @Override public int compare(Scored a, Scored b) {
                boolean ac = "crisis".equals(a.skill.id), bc = "crisis".equals(b.skill.id);
                if (ac != bc) return ac ? -1 : 1;
                if (a.score != b.score) return b.score - a.score;
                return a.index - b.index;
            }
        });
        List<GuideDocs.SkillEntry> out = new ArrayList<>();
        for (Scored s : scored) {
            if (out.size() >= max) break;
            out.add(s.skill);
        }
        return out;
    }

    private static final class Scored {
        final GuideDocs.SkillEntry skill;
        final int score;
        final int index;
        Scored(GuideDocs.SkillEntry skill, int score, int index) {
            this.skill = skill;
            this.score = score;
            this.index = index;
        }
    }
}
