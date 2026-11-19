package org.perazimchurch.app.ui.theme;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Turns every screen's old flat cards, pills and "3D" buttons into frosted glass, without rewriting each screen.
 *
 * <p>It watches a window's view tree and, after each layout, swaps solid-colour backgrounds for {@link Glass}
 * drawables, lets the glowing page background show through, and fixes text that would be hard to read on glass.
 * Views that are already glass are skipped, so repeated passes are cheap and settle after one round.
 */
public final class GlassPolish {

    private static final Map<Object, Boolean> INSTALLED = Collections.synchronizedMap(new WeakHashMap<>());

    private GlassPolish() {
    }

    public static void install(Activity activity) {
        if (activity == null || INSTALLED.containsKey(activity)) {
            return;
        }
        INSTALLED.put(activity, Boolean.TRUE);
        watch(activity.getWindow());
    }

    public static void install(Dialog dialog) {
        if (dialog == null || INSTALLED.containsKey(dialog)) {
            return;
        }
        INSTALLED.put(dialog, Boolean.TRUE);
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 31) {
            // Real frosted glass: blur whatever is behind the dialog window.
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.getAttributes().setBlurBehindRadius(Math.round(Glass.dp(24)));
            window.setAttributes(window.getAttributes());
        }
        watch(window);
    }

    private static void watch(Window window) {
        if (window == null) {
            return;
        }
        final View decor = window.getDecorView();
        decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                apply(decor);
            }
        });
        decor.post(() -> apply(decor));
    }

    /** One pass over a view tree. Safe to call repeatedly. */
    public static void apply(View root) {
        if (root == null) {
            return;
        }
        try {
            walk(root, hasGlow(root));
            freeShadows(root);
            fontTree(root);
        } catch (RuntimeException ignored) {
            // Styling must never crash a screen.
        }
    }

    private static void walk(View view, boolean glowAbove) {
        if (view.getVisibility() == View.GONE || GlassKit.isKept(view)) {
            return;
        }
        int id = view.getId();
        if (id == android.R.id.statusBarBackground || id == android.R.id.navigationBarBackground) {
            return; // the system bar scrims are not cards: glassing them hid the clock and battery icons
        }
        boolean glowHere = glowAbove || view.getBackground() instanceof PerazimTheme.GlowBackground;
        restyleBackground(view, glowHere);
        glowHere = glowHere || view.getBackground() instanceof PerazimTheme.GlowBackground;
        if (view instanceof TextView) {
            fixText((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                walk(group.getChildAt(i), glowHere);
            }
        }
    }

    /**
     * Puts the person's chosen font on every text view, including the ones inside views that are kept as they are.
     * Runs over the whole tree because the normal pass skips kept views and their children.
     */
    private static void fontTree(View view) {
        if (view.getVisibility() == View.GONE) {
            return;
        }
        if (view instanceof TextView) {
            AppFonts.apply((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                fontTree(group.getChildAt(i));
            }
        }
    }

    /**
     * Soft shadows and glows draw outside their view, so their parents must not clip them. Done here,
     * after each layout and before drawing, so even the first frame shows the full shadow.
     */
    private static void freeShadows(View view) {
        if (view.getVisibility() == View.GONE) {
            return;
        }
        if (view.getBackground() instanceof SoftShadow) {
            SoftShadow.unclip(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                freeShadows(group.getChildAt(i));
            }
        }
    }

    private static boolean hasGlow(View view) {
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            if (v.getBackground() instanceof PerazimTheme.GlowBackground) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- backgrounds

    private static void restyleBackground(View view, boolean glowAbove) {
        Drawable bg = view.getBackground();
        if (bg == null || bg instanceof Glass.GlassLayer || bg instanceof PerazimTheme.GlowBackground) {
            return;
        }
        if (view instanceof ImageView && !(view.isClickable())) {
            return;
        }
        Integer fill = solidFill(bg);
        if (fill == null) {
            return;
        }
        if (Color.alpha(fill) == 0) {
            return;
        }
        if (isPageColor(fill) && isLarge(view)) {
            // Let the glowing page background show through instead of a flat sheet of colour.
            view.setBackground(glowAbove ? null : PerazimTheme.createBackgroundGradient());
            return;
        }
        int w = view.getWidth();
        int h = view.getHeight();
        if (w > 0 && h > 0 && (w < Glass.dp(18) || h < Glass.dp(14))) {
            return; // dots, bars, progress tracks
        }
        if (bg instanceof GradientDrawable && ((GradientDrawable) bg).getShape() == GradientDrawable.OVAL) {
            return; // avatars and status dots keep their shape and colour
        }
        boolean buttonLike = view instanceof Button
                || (view instanceof TextView && !(view instanceof EditText) && view.isClickable()
                    && (h == 0 || h < Glass.dp(64)));
        Glass.GlassLayer glass;
        if (buttonLike) {
            glass = Glass.button(fill);
        } else {
            float radius = cornerRadius(bg);
            float min = h > Glass.dp(72) ? Glass.dp(22) : Glass.dp(14);
            glass = Glass.card(fill, Math.max(radius, min));
        }
        view.setBackground(glass);
        if (buttonLike && glass.kind != Glass.Kind.FROSTED && Build.VERSION.SDK_INT >= 28) {
            int glowColor = glass.kind == Glass.Kind.ORANGE ? Color.parseColor("#E17D2F") : glass.tint;
            view.setOutlineSpotShadowColor(glowColor);
            view.setOutlineAmbientShadowColor(glowColor);
            if (view.getElevation() < Glass.dp(4)) {
                view.setElevation(Glass.dp(6));
            }
        }
    }

    /** The solid colour behind a plain colour, a flat rounded rectangle, or an old two-layer "3D" button. */
    private static Integer solidFill(Drawable bg) {
        if (bg instanceof ColorDrawable) {
            return ((ColorDrawable) bg).getColor();
        }
        if (bg instanceof GradientDrawable) {
            ColorStateList csl = ((GradientDrawable) bg).getColor();
            return csl == null ? null : csl.getDefaultColor();
        }
        if (bg instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) bg;
            if (ld.getNumberOfLayers() == 2 && ld.getDrawable(1) instanceof GradientDrawable) {
                ColorStateList face = ((GradientDrawable) ld.getDrawable(1)).getColor();
                return face == null ? null : face.getDefaultColor();
            }
        }
        return null;
    }

    private static float cornerRadius(Drawable bg) {
        if (bg instanceof GradientDrawable) {
            return ((GradientDrawable) bg).getCornerRadius();
        }
        if (bg instanceof LayerDrawable && ((LayerDrawable) bg).getDrawable(1) instanceof GradientDrawable) {
            return ((GradientDrawable) ((LayerDrawable) bg).getDrawable(1)).getCornerRadius();
        }
        return 0f;
    }

    private static boolean isLarge(View v) {
        int w = v.getWidth();
        int h = v.getHeight();
        int screenW = v.getResources().getDisplayMetrics().widthPixels;
        return w >= screenW * 0.9f && (h == 0 || h > Glass.dp(200));
    }

    /** Old page canvases: the theme background, the legacy cream/lavender canvases and the deep purples. */
    private static boolean isPageColor(int c) {
        if (c == PerazimTheme.COLOR_BG_DEEP || c == PerazimTheme.COLOR_BG_NEUTRAL) {
            return true;
        }
        int rgb = c & 0x00FFFFFF;
        return rgb == 0xFAF7FB || rgb == 0xFAF6EE || rgb == 0x2A0F33 || rgb == 0x2E1038
                || rgb == 0x190720 || rgb == 0x140319 || rgb == 0xF6EEFA || rgb == 0xFFFFFF;
    }

    // ---------------------------------------------------------------- text

    private static void fixText(TextView tv) {
        if (sitsOnPhoto(tv)) {
            return;
        }
        Glass.Kind kind = kindBehind(tv);
        int current = tv.getCurrentTextColor();
        int readable = Glass.readableText(current, kind);
        if (readable != current) {
            tv.setTextColor(readable);
        }
        int hint = tv.getCurrentHintTextColor();
        if (tv instanceof EditText) {
            int readableHint = Glass.readableText(hint, kind);
            if (readableHint != hint) {
                tv.setHintTextColor((readableHint & 0x00FFFFFF) | 0xA0000000);
            }
        }
    }

    private static Glass.Kind kindBehind(View view) {
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            Drawable bg = v.getBackground();
            if (bg == null) {
                continue;
            }
            if (bg instanceof Glass.GlassLayer) {
                return ((Glass.GlassLayer) bg).kind;
            }
            if (bg instanceof PerazimTheme.GlowBackground) {
                return Glass.Kind.FROSTED;
            }
            Integer fill = solidFill(bg);
            if (fill != null && Color.alpha(fill) > 0x80) {
                return Glass.kindOf(fill) == Glass.Kind.FROSTED
                        ? (Color.luminance(fill) > 0.5f ? Glass.Kind.FROSTED : Glass.Kind.FROSTED)
                        : Glass.kindOf(fill);
            }
            return Glass.Kind.FROSTED;
        }
        return Glass.Kind.FROSTED;
    }

    /** White captions laid over photos keep their colour. */
    private static boolean sitsOnPhoto(View view) {
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            if (v.getParent() instanceof FrameLayout) {
                ViewGroup parent = (ViewGroup) v.getParent();
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View sibling = parent.getChildAt(i);
                    if (sibling != v && sibling instanceof ImageView
                            && sibling.getWidth() > Glass.dp(80) && sibling.getHeight() > Glass.dp(60)) {
                        return true;
                    }
                }
            }
            if (v.getBackground() != null) {
                return false;
            }
        }
        return false;
    }
}
