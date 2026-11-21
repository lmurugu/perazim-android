package org.perazimchurch.app.ai.local;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Runs the real TrustAnchors + SourceApprovals + Ed25519Verifier against Node-signed synthetic approvals. */
public final class ApprovalHarness {
    public static void main(String[] args) throws Exception {
        JSONObject root = new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])), "UTF-8"));
        JSONObject trustJson = root.getJSONObject("trust");
        Map<String, String> trust = new HashMap<>();
        for (String k : trustJson.keySet()) trust.put(k, trustJson.getString(k));
        JSONArray cases = root.getJSONArray("cases");
        int mismatches = 0;
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            boolean actual;
            String reason = "";
            try { SourceApprovals.verifyEntry(c.getJSONObject("entry"), trust); actual = true; }
            catch (Exception e) { actual = false; reason = e.getMessage(); }
            boolean expected = c.getBoolean("expected");
            if (actual != expected) mismatches++;
            System.out.println((actual == expected ? "OK   " : "DIFF ") + c.getString("label")
                    + " expected=" + expected + " actual=" + actual + (reason == null || reason.isEmpty() ? "" : " (" + reason + ")"));
        }
        System.out.println("mismatches=" + mismatches + " of " + cases.length());
    }
}
