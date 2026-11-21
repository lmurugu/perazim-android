package org.perazimchurch.app.presentation.ui.games.lampslide;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.perazimchurch.app.domain.games.lampslide.LampSlideScene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads the bundled scene list (assets/games/lamp_slide.json). Offline, read once. */
public final class LampSlideScenes {

    private static final String ASSET = "games/lamp_slide.json";
    private static volatile List<LampSlideScene> cached;
    private static Context appContext;

    private LampSlideScenes() {}

    /** Called once from the Application so the catalog can load scenes without a Context. */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    /** The bundled scenes. Empty when the file is missing or unreadable. */
    public static List<LampSlideScene> bundled() {
        List<LampSlideScene> local = cached;
        if (local != null) return local;
        List<LampSlideScene> loaded = Collections.emptyList();
        if (appContext != null) {
            try (InputStream in = appContext.getAssets().open(ASSET)) {
                loaded = parse(read(in));
            } catch (IOException | JSONException | IllegalArgumentException e) {
                loaded = Collections.emptyList();
            }
        }
        cached = loaded;
        return loaded;
    }

    /** Parses the scene JSON. Throws JSONException or IllegalArgumentException on bad data. */
    public static List<LampSlideScene> parse(String json) throws JSONException {
        JSONArray array = new JSONObject(json).getJSONArray("scenes");
        List<LampSlideScene> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            out.add(new LampSlideScene(
                    item.getString("id"),
                    item.getString("image"),
                    item.getString("reference"),
                    item.getString("verse"),
                    item.optString("prayer", "")));
        }
        return out;
    }

    /** Drawable id for a scene image name, or 0 when the art is not in the build yet. */
    public static int imageResource(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    private static String read(InputStream in) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) text.append(buffer, 0, read);
        }
        return text.toString();
    }
}
