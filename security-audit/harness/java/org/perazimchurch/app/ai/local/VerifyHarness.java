package org.perazimchurch.app.ai.local;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

/** Runs the real Ed25519Verifier (unmodified source) against Node-generated synthetic vectors. */
public final class VerifyHarness {
    public static void main(String[] args) throws Exception {
        JSONObject root = new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])), "UTF-8"));
        JSONArray vectors = root.getJSONArray("vectors");
        int mismatches = 0;
        for (int i = 0; i < vectors.length(); i++) {
            JSONObject v = vectors.getJSONObject(i);
            byte[] pk = Base64.getDecoder().decode(v.getString("pk"));
            byte[] msg = Base64.getDecoder().decode(v.getString("msg"));
            byte[] sig = Base64.getDecoder().decode(v.getString("sig"));
            boolean actual;
            try { actual = Ed25519Verifier.verify(pk, msg, sig); }
            catch (Exception e) { actual = false; }
            boolean expected = v.getBoolean("expected");
            if (actual != expected) mismatches++;
            System.out.println((actual == expected ? "OK   " : "DIFF ") + v.getString("label")
                    + " expected=" + expected + " actual=" + actual);
        }
        System.out.println("mismatches=" + mismatches + " of " + vectors.length());
    }
}
