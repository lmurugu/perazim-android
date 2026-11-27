package org.perazimchurch.app.data.local.preference;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import org.perazimchurch.app.domain.reading.ReadingCatalog;

/** Account-local progress, kept independently from church daily awards. */
public final class DailyReadingStore {
    public static final class State {
        private final JSONObject record;
        private final String owner, date, saved;
        public final String topic;
        public final String readingId;
        public final int length, day, completed, step;

        private State(JSONObject record, String owner, String date, String saved, String randomId) {
            this.record = record;
            this.owner = owner;
            this.date = date;
            this.saved = saved;
            topic = record.optString("topic");
            JSONArray entries = record.optJSONArray("readings");
            length = entries == null ? 0 : entries.length();
            completed = record.optInt("completed");
            boolean doneToday = date.compareTo(record.optString("completedDate")) <= 0;
            day = length == 0 ? 0 : Math.min(length, completed + (doneToday ? 0 : 1));
            readingId = length == 0 ? randomId : entries.optString(Math.max(0, day - 1));
            boolean sameEntry = length > 0 || date.equals(record.optString("stepDate"));
            step = doneToday || (length > 0 && completed >= length) ? 4
                    : sameEntry ? Math.max(1, Math.min(3, record.optInt("step"))) : 1;
        }

        public ReadingCatalog.Reading researchReading() {
            JSONArray sources = record.optJSONArray("research");
            if (sources == null) return null;
            JSONObject entry = sources.optJSONObject(Math.max(0, day - 1));
            if (entry == null) return null;
            try {
                return ReadingCatalog.Reading.restoreResearch(entry, topic);
            } catch (JSONException | IllegalArgumentException e) {
                return null;
            }
        }

        public boolean isPlan() { return length > 0; }
        public boolean planFinished() { return isPlan() && completed >= length; }
    }

    private final SharedPreferences prefs;

    public DailyReadingStore(Context context) {
        prefs = context.getSharedPreferences("perazim_daily_reading_v1", Context.MODE_PRIVATE);
    }

    public State load(String owner, String date, String randomId) {
        String saved = prefs.getString(planKey(owner), null);
        if (saved == null) saved = prefs.getString(dailyKey(owner), null);
        if (saved != null) {
            try { return new State(new JSONObject(saved), owner, date, saved, randomId); }
            catch (JSONException ignored) { }
        }
        return new State(new JSONObject(), owner, date, saved, randomId);
    }

    public boolean start(String owner, String topic, List<String> readingIds) {
        if (readingIds.isEmpty() || readingIds.size() > ReadingCatalog.MAX_DAYS) return false;
        try {
            JSONObject record = new JSONObject();
            record.put("topic", topic);
            record.put("readings", new JSONArray(readingIds));
            return save(planKey(owner), record);
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean startResearch(String owner, String topic, List<ReadingCatalog.Reading> readings) {
        if (readings.isEmpty() || readings.size() > ReadingCatalog.MAX_DAYS) return false;
        try {
            JSONArray ids = new JSONArray();
            JSONArray sources = new JSONArray();
            for (ReadingCatalog.Reading reading : readings) {
                ids.put(reading.id);
                sources.put(reading.toResearchJson());
            }
            JSONObject record = new JSONObject().put("topic", topic).put("readings", ids).put("research", sources);
            return save(planKey(owner), record);
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean useDaily(String owner) { return prefs.edit().remove(planKey(owner)).commit(); }

    public boolean advance(String owner, String date, State state) {
        if (state.step >= 4 || !date.equals(state.date) || !owner.equals(state.owner)) return false;
        String key = state.isPlan() ? planKey(owner) : dailyKey(owner);
        if (!state.isPlan() && prefs.contains(planKey(owner))) return false;
        if (!java.util.Objects.equals(state.saved, prefs.getString(key, null))) return false;
        try {
            JSONObject record = new JSONObject(state.record.toString());
            record.put("stepDate", date);
            if (state.step < 3) {
                record.put("step", state.step + 1);
            } else {
                if (state.isPlan()) record.put("completed", state.completed + 1);
                record.put("completedDate", date);
                record.put("step", 1);
            }
            return save(key, record);
        } catch (JSONException e) {
            return false;
        }
    }

    private static String planKey(String owner) { return "plan." + owner; }
    private static String dailyKey(String owner) { return "daily." + owner; }

    private boolean save(String key, JSONObject data) {
        return prefs.edit().putString(key, data.toString()).commit();
    }
}
