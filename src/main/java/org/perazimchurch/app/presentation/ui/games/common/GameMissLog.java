package org.perazimchurch.app.presentation.ui.games.common;

import android.content.Context;

import org.perazimchurch.app.data.local.preference.QuestionHistoryStore;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Writes one answered question to the question history so the on-phone guide can see which
 * questions a member misses. Data only: nothing is shown on screen and nothing leaves the phone.
 * Only a right or wrong answer is recorded. Ignored and moved taps are not questions answered.
 */
public final class GameMissLog {
    private GameMissLog() {}

    /** Records a session outcome. Returns quietly when the question has no text to record. */
    public static void answered(Context context, String gameId, String prompt, GameSession.Outcome outcome) {
        if (outcome == GameSession.Outcome.CORRECT) {
            answered(context, gameId, prompt, true);
        } else if (outcome == GameSession.Outcome.WRONG) {
            answered(context, gameId, prompt, false);
        }
    }

    /** Records a right or wrong answer for a legacy game that reports a boolean. */
    public static void answered(Context context, String gameId, String prompt, boolean correct) {
        if (context == null || gameId == null || prompt == null) return;
        String question = prompt.trim();
        if (question.isEmpty()) return;
        QuestionHistoryStore.record(context.getApplicationContext(), gameId, question, correct);
    }
}
