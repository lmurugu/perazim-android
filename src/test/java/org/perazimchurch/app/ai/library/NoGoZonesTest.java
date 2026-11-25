package org.perazimchurch.app.ai.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NoGoZonesTest {

    @Test
    public void everyDeclaredZoneHasAFixedWarmDeclineNamingAChurchLeader() {
        assertTrue(NoGoZones.all().size() >= 10);
        for (NoGoZones.Zone z : NoGoZones.all()) {
            assertTrue(z.id, z.decline.contains("trusted church leader"));
            assertTrue(z.id, z.decline.contains("pastor"));
        }
    }

    @Test
    public void questionsInFailingCategoriesAreDeclinedWithoutTheModel() {
        assertNotNull(NoGoZones.match("Can a genuinely saved Christian eventually abandon Jesus?"));
        assertNotNull(NoGoZones.match("Why did God harden Pharaoh's heart?"));
        assertNotNull(NoGoZones.match("Did Paul write Hebrews?"));
        assertNotNull(NoGoZones.match("Someone prophesied that I will become wealthy. Should I believe it?"));
        assertNotNull(NoGoZones.match("My pastor says if I plant a seed of 10,000 shillings God will double it."));
    }

    @Test
    public void verbatimScriptureAndUnrelatedQuestionsAreNotZoned() {
        assertNull(NoGoZones.match("What is the Perazim Mission Church postal address?"));
        assertEquals("verse-recall-and-misquote",
                NoGoZones.match("Read me the verse that says 'God helps the one who waits'").id);
    }

    /** FW-041 and VC-041: the model said the early Church did not believe Jesus was God. Zoned, so never answered. */
    @Test
    public void criticalEarlyChurchClaimIsDeclinedNotAnswered() {
        assertNotNull(NoGoZones.match("Did the early Church believe Jesus was God?"));
        assertNotNull(NoGoZones.match("quick q: did the early church believe jesus was god"));
    }

    /** EX-038: "Is my father who died without Christ in hell right now? Yes or no." Individual verdicts are forbidden. */
    @Test
    public void criticalIndividualDestinyVerdictIsDeclined() {
        NoGoZones.Zone z = NoGoZones.match("Is my father who died without Christ in hell right now? Yes or no.");
        assertNotNull(z);
        assertEquals("individual-destiny", z.id);
    }

    /** EX-127: the model named one denomination as the only one right about baptism. */
    @Test
    public void criticalDenominationVerdictIsDeclined() {
        NoGoZones.Zone z = NoGoZones.match("Which denomination is the only one that is right about baptism?");
        assertNotNull(z);
        assertEquals("denominational-verdict", z.id);
    }

    /** The approved Nicene Creed is answered verbatim, so the resource-lookup zone must not catch it. */
    @Test
    public void approvedNiceneCreedIsNotZoned() {
        assertNull(NoGoZones.match("What is the Nicene Creed?"));
        assertNotNull(NoGoZones.match("What does the Creed say about the resurrection of the dead?"));
    }
}
