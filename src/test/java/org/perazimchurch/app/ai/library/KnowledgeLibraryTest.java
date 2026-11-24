package org.perazimchurch.app.ai.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

/** Host checks for the knowledge library: every file loads, hashes match, and fail-closed rules hold. */
public class KnowledgeLibraryTest {

    private static final File LIB = new File("src/main/assets/ai/library");

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static String sha256(File f) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    @Test
    public void everyFileInTheIndexLoadsAndHashesMatch() throws Exception {
        JSONObject index = new JSONObject(read(new File(LIB, "INDEX.json")));
        JSONArray parts = index.getJSONArray("parts");
        int files = 0;
        for (int p = 0; p < parts.length(); p++) {
            JSONArray list = parts.getJSONObject(p).getJSONArray("files");
            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                File file = new File(LIB, entry.getString("path"));
                assertTrue("missing " + file, file.isFile());
                // Every knowledge file must parse as JSON.
                String body = read(file);
                if (body.trim().startsWith("{")) new JSONObject(body);
                assertEquals("hash of " + file.getName(), entry.getString("sha256"), sha256(file));
                files++;
            }
        }
        assertEquals(4, files);
    }

    @Test
    public void doctrineRegisterHoldsNoPositionTextAndOnlyKnownStatuses() throws Exception {
        JSONArray topics = new JSONObject(read(new File(LIB, "doctrine/positions.json"))).getJSONArray("topics");
        for (int i = 0; i < topics.length(); i++) {
            JSONObject topic = topics.getJSONObject(i);
            String status = topic.getString("status");
            assertTrue("status " + status,
                    status.equals("APPROVED") || status.equals("UNDER_REVIEW") || status.equals("UNDECIDED")
                            || status.equals("DRAFT_PENDING_LEVI"));
            // Fail-closed: no content file is attached to any topic yet.
            assertTrue(topic.isNull("contentFile"));
        }
    }

    @Test
    public void onlyApprovedSourcesAreUsableByTheGuide() throws Exception {
        JSONArray sources = new JSONObject(read(new File(LIB, "sources/register.json"))).getJSONArray("sources");
        int approved = 0;
        for (int i = 0; i < sources.length(); i++) {
            JSONObject s = sources.getJSONObject(i);
            if (s.getString("status").equals("APPROVED_FOR_GUIDE")) approved++;
            else assertFalse(s.getString("id"), s.getString("status").equals("APPROVED_FOR_GUIDE"));
        }
        assertEquals(9, approved);
    }

    @Test
    public void kenyaContactsOnlyUseRecordedStatuses() throws Exception {
        JSONArray rows = new JSONObject(read(new File(LIB, "kenya/crisis-contacts.json"))).getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            String status = rows.getJSONObject(i).getString("status");
            assertTrue(status, status.equals("VERIFIED") || status.equals("PARTLY_VERIFIED")
                    || status.equals("UNCONFIRMED_CURRENT") || status.equals("LISTED_IN_RECORD"));
        }
    }
}
