package org.perazimchurch.app.ui.theme;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Swaps the app's text onto the font the person chose in Profile &gt; Display.
 *
 * <p>The fonts are bundled under {@code assets/fonts} as small Latin subsets of open-source (SIL OFL) families, one
 * regular and one bold file each, so they work on every supported Android version. Index 0 keeps the phone's own font
 * and touches nothing. Called from the glass pass, so every screen and dialog picks it up after each layout.
 */
public final class AppFonts {

    private static final Map<String, Typeface> FACES = new HashMap<>();
    /** Bold faces we loaded, so a later pass can tell a bold label from a regular one. */
    private static final Set<Typeface> BOLD_FACES = Collections.newSetFromMap(new WeakHashMap<>());
    private static int cachedChoice = -1;

    private AppFonts() {
    }

    /** Clears the cached choice after the person picks a new font. */
    public static void forget() {
        cachedChoice = -1;
    }

    /** Restyles one text view to the chosen font, keeping its bold weight. Safe to call on every pass. */
    public static void apply(TextView tv) {
        Context context = tv.getContext();
        if (context == null) {
            return;
        }
        int choice = choice(context);
        if (choice == 0) {
            return;
        }
        Typeface current = tv.getTypeface();
        if (current != null && current.isItalic()) {
            return; // keep italics as they are
        }
        boolean bold = current != null && (current.isBold() || BOLD_FACES.contains(current));
        Typeface wanted = face(context, choice, bold);
        if (wanted != null && wanted != current) {
            tv.setTypeface(wanted);
        }
    }

    private static int choice(Context context) {
        if (cachedChoice < 0) {
            cachedChoice = DisplaySettings.fontChoice(context);
        }
        return cachedChoice;
    }

    private static Typeface face(Context context, int choice, boolean bold) {
        String stem = DisplaySettings.FONT_FILES[choice];
        if (stem == null) {
            return null;
        }
        String key = stem + (bold ? "-bold" : "-regular");
        Typeface cached = FACES.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Typeface loaded = Typeface.createFromAsset(context.getApplicationContext().getAssets(),
                    "fonts/" + key + ".ttf");
            FACES.put(key, loaded);
            if (bold) {
                BOLD_FACES.add(loaded);
            }
            return loaded;
        } catch (RuntimeException missing) {
            // A missing font file must never break a screen: keep the current face.
            return null;
        }
    }
}
