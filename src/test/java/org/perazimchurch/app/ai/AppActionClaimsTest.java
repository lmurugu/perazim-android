package org.perazimchurch.app.ai;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for app-action claims. Synthetic sentences only; no member data.
 * Pure string checks: no Android context required.
 */
public class AppActionClaimsTest {

    @Test
    public void completedActionClaimsAreDetectedInAllCommonForms() {
        // Original forms
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("Your prayer request has been submitted."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I have contacted the Bishop for you."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I sent your message to the leaders."));
        // Contractions and short forms that the earlier pattern missed
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I've submitted your prayer request."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I’ve sent this to the office."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("We recorded your membership details."));
        // Passive forms
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("Your request was sent to the pastor."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("Your membership has now been approved."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("The message is now recorded in the system."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("Your update was successfully saved."));
        // Forwarding and notifying
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I passed this on to your elders."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I have let the pastor know about your concern."));
    }

    @Test
    public void promisesOfAppActionAreDetected() {
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I'll submit this prayer for you now."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I will notify the bishop tomorrow."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I can now register your attendance."));
    }

    @Test
    public void humanAuthorityClaimsAreStillDetected() {
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I am your pastor, and I forgive you."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I now declare you a member."));
        assertTrue(AppActionClaims.claimsAppActionOrHumanAuthority("I’m the bishop of this church."));
    }

    @Test
    public void honestNegativeAndTheologicalRepliesPass() {
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority("Ephesians 2:8 teaches that grace is a gift of God."));
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority(
                "The church documents do not establish an official position on this question."));
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority("I cannot confirm that."));
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority("I cannot submit prayer requests from this guide."));
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority("I will not send it for you."));
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority(
                "This message has not been sent for pastoral follow-up."));
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority(
                "You can ask your pastor to pray with you when you are ready."));
        assertFalse(AppActionClaims.claimsAppActionOrHumanAuthority(null));
    }

    @Test
    public void builtInBoundaryRepliesNeverClaimAppActions() {
        String[] canned = {
                PerazimAiEngine.crisisResponse().getMessageText(),
                PerazimAiEngine.sensitivePastoralResponse().getMessageText(),
                PerazimAiEngine.humanDecisionResponse().getMessageText(),
                PerazimAiEngine.professionalBoundaryResponse().getMessageText(),
                PerazimAiEngine.outOfScopeResponse().getMessageText(),
        };
        for (String reply : canned) {
            assertFalse("Built-in reply must not claim an app action: " + reply,
                    AppActionClaims.claimsAppActionOrHumanAuthority(reply));
        }
    }

    @Test
    public void engineDelegatesToTheSameGuard() {
        assertTrue(PerazimAiEngine.claimsAppActionOrHumanAuthority("Your prayer request has been submitted."));
        assertFalse(PerazimAiEngine.claimsAppActionOrHumanAuthority("Ephesians 2:8 teaches grace."));
    }
}
