package org.perazimchurch.app.ai.local;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ProbeRouteTest {
    @Test
    public void probe() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        List<String> qs = Files.readAllLines(new File("/home/user/scratch/probe_in.txt").toPath(), StandardCharsets.UTF_8);
        try (FileWriter w = new FileWriter("/home/user/scratch/probe_out.txt")) {
            for (String q : qs) {
                if (q.trim().isEmpty()) continue;
                SoftSafety.reset();
                PerazimAiEngine engine = new PerazimAiEngine(c);
                GuideTurnRouter.Turn t = GuideTurnRouter.route(c, engine, q);
                String text = t.response == null ? "<null>" : t.response.getMessageText();
                w.write(q + " | " + t.status + " | " + text.replace('\n', ' ').substring(0, Math.min(200, text.length())) + "\n");
            }
        }
    }
}
