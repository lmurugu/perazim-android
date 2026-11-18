package org.perazimchurch.app.ui.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.perazimchurch.app.R;

/**
 * Building blocks that reproduce the frosted-glass mockups one to one
 * (Perazim Glass Style boards: Home, Sermons, Community, Give, Worship, Profile, Daily reading, Bible).
 *
 * <p>Every size below is the mockup's CSS pixel value used as dp, and every colour is the mockup's colour.
 * Views made here are tagged so {@link GlassPolish} leaves them exactly as built.
 */
public final class GlassKit {

    private GlassKit() {
    }

    // ------------------------------------------------------------------ palette (mockup values)

    public static boolean light() {
        return PerazimTheme.isLight();
    }

    /** Main text: #F5F0F7 on dark glass, #2A0F33 on light glass. */
    public static int text() {
        return light() ? Color.parseColor("#2A0F33") : Color.parseColor("#F5F0F7");
    }

    /** Labels and secondary lines: #CDBBD6 / #5A3A66. */
    public static int muted() {
        return light() ? Color.parseColor("#5A3A66") : Color.parseColor("#CDBBD6");
    }

    /** Body copy inside cards: #E6D8EE / #5A3A66. */
    public static int body() {
        return light() ? Color.parseColor("#5A3A66") : Color.parseColor("#E6D8EE");
    }

    /** Warm accent text such as "Step 2 of 3": #F8C894 / #8A4A12. */
    public static int accent() {
        return light() ? Color.parseColor("#8A4A12") : Color.parseColor("#F8C894");
    }

    public static final int ORANGE = Color.parseColor("#E17D2F");
    public static final int ORANGE_TOP = Color.parseColor("#E88636");
    public static final int ORANGE_BOTTOM = Color.parseColor("#C06119");
    public static final int PURPLE_TOP = Color.parseColor("#8A2BA3");
    public static final int PURPLE_BOTTOM = Color.parseColor("#681A7D");

    // ------------------------------------------------------------------ drawables

    /** rgba(255,255,255,0.08) glass with a 0.18 rim and a soft top sheen (inset highlight). */
    public static Drawable glass(Context c, float radiusDp) {
        GradientDrawable d;
        if (light()) {
            d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.parseColor("#C7FFFFFF"), Color.parseColor("#B3FFFFFF")});
            d.setStroke(px(c, 1), Color.parseColor("#F2FFFFFF"));
        } else {
            d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.parseColor("#24FFFFFF"), Color.parseColor("#14FFFFFF"), Color.parseColor("#12FFFFFF")});
            d.setStroke(px(c, 1), Color.parseColor("#2EFFFFFF"));
        }
        d.setCornerRadius(px(c, radiusDp));
        if (light()) {
            // Light glass floats on a soft plum shadow: box-shadow 0 8px 24px rgba(61,23,73,0.12).
            return new SoftShadow(d, Color.parseColor("#1F3D1749"), px(c, 8), px(c, 24), px(c, radiusDp));
        }
        return d;
    }

    /** Slightly brighter glass for small round buttons and pills: rgba(255,255,255,0.10), rim 0.20. */
    public static Drawable glassControl(Context c, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        if (light()) {
            d.setColor(Color.parseColor("#B3FFFFFF"));
            d.setStroke(px(c, 1), Color.parseColor("#E6FFFFFF"));
        } else {
            d.setColor(Color.parseColor("#1AFFFFFF"));
            d.setStroke(px(c, 1), Color.parseColor("#33FFFFFF"));
        }
        d.setCornerRadius(px(c, radiusDp));
        return d;
    }

    /** The orange call to action: linear-gradient(180deg, #E88636, #C06119). */
    public static Drawable orange(Context c, float radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ORANGE_TOP, ORANGE_BOTTOM});
        d.setCornerRadius(px(c, radiusDp));
        return d;
    }

    /** Brand purple pill: linear-gradient(180deg, #8A2BA3, #681A7D) with a 0.3 white rim. */
    public static Drawable purple(Context c, float radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{PURPLE_TOP, PURPLE_BOTTOM});
        d.setStroke(px(c, 1), Color.parseColor("#4DFFFFFF"));
        d.setCornerRadius(px(c, radiusDp));
        return d;
    }

    /** The 44dp icon square on Home tiles: 135deg rgba(139,50,170,0.9) to rgba(104,26,125,0.6). */
    public static Drawable iconSquare(Context c, float radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#E68B32AA"), Color.parseColor("#99681A7D")});
        d.setStroke(px(c, 1), Color.parseColor("#4DFFFFFF"));
        d.setCornerRadius(px(c, radiusDp));
        return d;
    }

    // ------------------------------------------------------------------ views

    public static TextView text(Context c, CharSequence s, float sp, boolean bold, int color) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        tv.setIncludeFontPadding(false);
        if (bold) {
            tv.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        } else {
            tv.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        return keep(tv);
    }

    /** Uppercase card label: 11sp, bold, 0.1em tracking, muted. */
    public static TextView label(Context c, CharSequence s) {
        TextView tv = text(c, s, 11, true, muted());
        tv.setLetterSpacing(0.1f);
        return tv;
    }

    /** A glass card: vertical, padded, rounded. */
    public static LinearLayout card(Context c, float radiusDp, int padDp) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(c, padDp), px(c, padDp), px(c, padDp), px(c, padDp));
        card.setBackground(glass(c, radiusDp));
        return keep(card);
    }

    /** Full-width orange pill button with the mockup's orange glow. */
    public static TextView orangeButton(Context c, CharSequence s, float textSp) {
        TextView b = text(c, s, textSp, true, Color.WHITE);
        b.setLetterSpacing(0.04f);
        b.setGravity(Gravity.CENTER);
        b.setBackground(orange(c, 999));
        b.setClickable(true);
        b.setFocusable(true);
        glow(b, ORANGE, 8);
        return b;
    }

    /** Glass pill button. */
    public static TextView glassButton(Context c, CharSequence s, float textSp) {
        TextView b = text(c, s, textSp, true, text());
        b.setLetterSpacing(0.04f);
        b.setGravity(Gravity.CENTER);
        b.setBackground(glassControl(c, 999));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    /** Filter chip: selected is the purple pill, others are glass. padding 9/16, 12sp. */
    public static TextView chip(Context c, CharSequence s, boolean selected) {
        TextView chip = text(c, s, 12, selected, selected ? Color.WHITE : text());
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(px(c, 16), px(c, 9), px(c, 16), px(c, 9));
        chip.setBackground(selected ? purple(c, 999) : glass(c, 999));
        chip.setClickable(true);
        return chip;
    }

    /** Three-part progress bar: 4dp tall, 6dp gaps, orange for done steps. */
    public static LinearLayout stepBar(Context c, int total, int done) {
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < total; i++) {
            View seg = new View(c);
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(px(c, 2));
            d.setColor(i < done ? ORANGE : (light() ? Color.parseColor("#263D1749") : Color.parseColor("#33FFFFFF")));
            seg.setBackground(d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(c, 4), 1f);
            if (i > 0) {
                lp.leftMargin = px(c, 6);
            }
            bar.addView(seg, lp);
        }
        return keep(bar);
    }

    /** Re-colours an existing step bar. */
    public static void setSteps(LinearLayout bar, int done) {
        for (int i = 0; i < bar.getChildCount(); i++) {
            Drawable d = bar.getChildAt(i).getBackground();
            if (d instanceof GradientDrawable) {
                ((GradientDrawable) d).setColor(i < done ? ORANGE
                        : (light() ? Color.parseColor("#263D1749") : Color.parseColor("#33FFFFFF")));
            }
        }
    }

    /** Round glass button holding a line icon (40dp in the mockup header). */
    public static FrameLayout circleIcon(Context c, int iconRes, int sizeDp, int iconDp, CharSequence description) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(glassControl(c, sizeDp / 2f));
        ImageView iv = new ImageView(c);
        iv.setImageResource(iconRes);
        iv.setImageTintList(ColorStateList.valueOf(text()));
        f.addView(iv, new FrameLayout.LayoutParams(px(c, iconDp), px(c, iconDp), Gravity.CENTER));
        f.setContentDescription(description);
        f.setClickable(true);
        f.setFocusable(true);
        f.setLayoutParams(new LinearLayout.LayoutParams(px(c, sizeDp), px(c, sizeDp)));
        return keep(f);
    }

    /** A line icon tinted white, sitting in a coloured square or circle. */
    public static FrameLayout iconBadge(Context c, int iconRes, Drawable bg, int sizeDp, int iconDp) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(bg);
        ImageView iv = new ImageView(c);
        iv.setImageResource(iconRes);
        iv.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        f.addView(iv, new FrameLayout.LayoutParams(px(c, iconDp), px(c, iconDp), Gravity.CENTER));
        return keep(f);
    }

    /**
     * The screen header from the mockups: logo, "Perazim", then a light/dark switch and the 40dp glass profile button.
     * Either listener may be null.
     */
    public static LinearLayout header(Context c, View.OnClickListener onTheme, View.OnClickListener onProfile) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(c, 20), px(c, 16), px(c, 20), px(c, 6));

        ImageView logo = new ImageView(c);
        logo.setImageResource(R.drawable.ic_perazim_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(px(c, 30), px(c, 30));
        lpLogo.rightMargin = px(c, 8);
        row.addView(logo, lpLogo);

        row.addView(text(c, "Perazim", 17, true, text()));

        View spacer = new View(c);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        if (onTheme != null) {
            FrameLayout theme = circleIcon(c, light() ? R.drawable.gl_moon : R.drawable.gl_sun, 40, 18,
                    light() ? "Switch to dark mode" : "Switch to light mode");
            theme.setOnClickListener(onTheme);
            ((LinearLayout.LayoutParams) theme.getLayoutParams()).rightMargin = px(c, 8);
            row.addView(theme);
        }
        FrameLayout profile = circleIcon(c, R.drawable.gl_person, 40, 18, "My profile");
        if (onProfile != null) {
            profile.setOnClickListener(onProfile);
        }
        row.addView(profile);
        return keep(row);
    }

    // ------------------------------------------------------------------ helpers

    public static LinearLayout.LayoutParams matchWrap(Context c, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(c, topDp);
        return lp;
    }

    public static LinearLayout.LayoutParams matchHeight(Context c, int heightDp, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(c, heightDp));
        lp.topMargin = px(c, topDp);
        return lp;
    }

    /** Coloured glow under opaque buttons (Android 9+). */
    /**
     * The mockups' coloured glow under orange and purple controls (box-shadow 0 Npx 2.5Npx colour/0.4),
     * drawn as a real blurred shadow rather than Android's faint grey elevation shadow.
     */
    public static void glow(View v, int color, int elevationDp) {
        float alpha = light() ? 0.40f : 0.45f;
        shadow(v, withAlpha(color, alpha), elevationDp, elevationDp * 2.6f);
    }

    /** box-shadow: 0 dyDp blurDp colour, behind the view's current background (call after setBackground). */
    public static void shadow(View v, int color, float dyDp, float blurDp) {
        Drawable bg = v.getBackground();
        if (bg instanceof SoftShadow) {
            bg = ((SoftShadow) bg).getFace();
        }
        float radius = 0f;
        if (bg instanceof GradientDrawable) {
            GradientDrawable g = (GradientDrawable) bg;
            radius = g.getShape() == GradientDrawable.OVAL ? 100000f : g.getCornerRadius();
        }
        v.setElevation(0f);
        v.setBackground(new SoftShadow(bg, color, px(v.getContext(), dyDp), px(v.getContext(), blurDp), radius));
    }

    public static int withAlpha(int color, float alpha) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | (color & 0x00FFFFFF);
    }
        v.setElevation(px(v.getContext(), elevationDp));
    }

    /** Tags a view so GlassPolish leaves it (and its children) exactly as built. */
    public static <T extends View> T keep(T v) {
        v.setTag(R.id.glass_keep, Boolean.TRUE);
        return v;
    }

    public static boolean isKept(View v) {
        return v.getTag(R.id.glass_keep) != null;
    }

    public static int px(Context c, float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }
}
