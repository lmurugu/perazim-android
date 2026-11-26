package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Capability regression set (Levi, 2026-11-25): verse lookup, phrase and word search, verse-about search, the approved
 * meaning route, safety and greeting replies. Deterministic, no model. Must pass before any phone build.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CapabilityRegressionTest {

    @Test
    public void capabilityCasesPass() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        List<String> lines = Files.readAllLines(new File("tools/ai-eval/capability-regression/cases.jsonl").toPath(), StandardCharsets.UTF_8);
        StringBuilder failures = new StringBuilder();
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            JSONObject kase = new JSONObject(line);
            SoftSafety.reset();
            PerazimAiEngine engine = new PerazimAiEngine(c);
            GuideTurnRouter.Turn turn = GuideTurnRouter.route(c, engine, kase.getString("question"));
            String text = turn.response == null ? "" : turn.response.getMessageText();
            String status = turn.status == null ? "" : turn.status;
            boolean statusOk = status.equals(kase.getString("status"));
            boolean textOk = text.contains(kase.optString("contains", ""));
            if (!statusOk || !textOk) {
                failures.append(kase.getString("id")).append(": status=").append(status)
                        .append(" expected=").append(kase.getString("status"))
                        .append(" textOk=").append(textOk).append('\n');
            }
        }
        assertTrue("Capability regression failures:\n" + failures, failures.length() == 0);
    }
}
