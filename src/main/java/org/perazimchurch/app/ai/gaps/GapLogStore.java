package org.perazimchurch.app.ai.gaps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.perazimchurch.app.ai.guide.GuideVault;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The on-device list of unanswered questions. Stored in the guide's encrypted vault; never uploaded by the app.
 * Repeats of the same question and reason are counted in one entry. The list keeps the newest {@link #CAP} entries.
 */
public final class GapLogStore {
    public static final String FILE = "gaps.bin";
    public static final int CAP = 500;
    private static final int ROUTE_CHARS = 80;

    private final GuideVault vault;
    private List<GapEntry> entries;

    private GapLogStore(GuideVault vault, List<GapEntry> entries) {
        this.vault = vault;
        this.entries = entries;
    }

    public static GapLogStore open(GuideVault vault) {
        List<GapEntry> loaded = new ArrayList<>();
        String text = vault.read(FILE);
        if (text != null) {
            try {
                JSONArray array = new JSONArray(text);
                for (int i = 0; i < array.length(); i++) loaded.add(GapEntry.fromJson(array.getJSONObject(i)));
            } catch (JSONException ignored) {
                loaded = new ArrayList<>(); // damaged data is treated as an empty list
            }
        }
        return new GapLogStore(vault, loaded);
    }

    /**
     * Adds one redacted question, or counts it again. Returns false when the list could not be saved.
     * The caller must have checked {@link GapPolicy#mayLog} and redacted the text already.
     */
    public synchronized boolean record(String redactedQuestion, String reason, String route, String appVersion, long now) {
        String key = GapRedactor.normalizedKey(redactedQuestion) + "|" + reason;
        List<GapEntry> next = new ArrayList<>(entries);
        int index = -1;
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).key.equals(key)) { index = i; break; }
        }
        String cleanRoute = route == null ? "" : route.length() > ROUTE_CHARS ? route.substring(0, ROUTE_CHARS) : route;
        if (index >= 0) {
            GapEntry old = next.remove(index);
            next.add(new GapEntry(key, old.question, reason, cleanRoute, appVersion, old.firstSeen, now, old.count + 1));
        } else {
            next.add(new GapEntry(key, redactedQuestion, reason, cleanRoute, appVersion, now, now, 1));
        }
        Collections.sort(next, Comparator.comparingLong((GapEntry e) -> e.lastSeen));
        while (next.size() > CAP) next.remove(0);
        if (!save(next)) return false;
        entries = next;
        return true;
    }

    /** Newest first. */
    public synchronized List<GapEntry> all() {
        List<GapEntry> copy = new ArrayList<>(entries);
        Collections.sort(copy, Comparator.comparingLong((GapEntry e) -> e.lastSeen).reversed());
        return copy;
    }

    public synchronized boolean clear() {
        if (!save(new ArrayList<>())) return false;
        entries = new ArrayList<>();
        return true;
    }

    private boolean save(List<GapEntry> list) {
        JSONArray array = new JSONArray();
        for (GapEntry e : list) array.put(e.toJson());
        try {
            vault.write(FILE, array.toString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** One JSON object per line, for the member to read and to share. Only the fields in {@link GapEntry#toJson}. */
    public static String toJsonLines(List<GapEntry> list) {
        StringBuilder out = new StringBuilder();
        for (GapEntry e : list) out.append(e.toJson().toString()).append('\n');
        return out.toString();
    }
}
