package com.example.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class HymnsActivity extends Activity implements View.OnClickListener {

    public static final int COLOR_PURPLE = Color.parseColor("#681A7D");
    public static final int COLOR_PURPLE_DARK = Color.parseColor("#2D0938");
    public static final int COLOR_PURPLE_SHADOW = Color.parseColor("#450E53");
    public static final int COLOR_PURPLE_BORDER = Color.parseColor("#E7D5EC");
    public static final int COLOR_PURPLE_TINT = Color.parseColor("#F5ECF7");
    public static final int COLOR_PURPLE_MUTED = Color.parseColor("#725B78");

    public static final int COLOR_ORANGE = Color.parseColor("#E17D2F");
    public static final int COLOR_ORANGE_DARK = Color.parseColor("#78350A");
    public static final int COLOR_ORANGE_SHADOW = Color.parseColor("#98450B");
    public static final int COLOR_ORANGE_BORDER = Color.parseColor("#F9DFCC");
    public static final int COLOR_ORANGE_TINT = Color.parseColor("#FDF5EF");

    public static final int COLOR_WHITE = Color.parseColor("#FFFFFF");
    public static final int COLOR_CANVAS = Color.parseColor("#FAF7FB");

    private Button btnBack;
    private Button btnSingHymn1;
    private Button btnSingHymn2;
    private Button btnSingHymn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_CANVAS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(36));

        // Top Navigation Bar
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER_VERTICAL);
        navBar.setPadding(0, 0, 0, dp(16));

        btnBack = new Button(this);
        btnBack.setText("← Back");
        btnBack.setTextColor(COLOR_WHITE);
        btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnBack.setBackground(create3dButtonDrawable(COLOR_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
        btnBack.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnBack.setOnClickListener(this);
        navBar.addView(btnBack);

        TextView title = new TextView(this);
        title.setText("HYMNS & WORSHIP");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        title.setPadding(dp(16), 0, 0, 0);
        navBar.addView(title);

        content.addView(navBar);

        // Classical Art Row
        LinearLayout artRow = new LinearLayout(this);
        artRow.setOrientation(LinearLayout.HORIZONTAL);
        artRow.setPadding(0, 0, 0, dp(16));

        Bitmap hymnBmp = loadAssetBitmap("art_hymn.jpg", 400);
        if (hymnBmp != null) {
            Bitmap roundHymn = getRoundedCornerBitmap(hymnBmp, dp(12));
            ImageView img1 = new ImageView(this);
            img1.setImageBitmap(roundHymn);
            img1.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(120), 1.0f);
            lp.setMargins(0, 0, dp(8), 0);
            img1.setLayoutParams(lp);
            artRow.addView(img1);
        }

        Bitmap worshipBmp = loadAssetBitmap("art_worship_classic.jpg", 400);
        if (worshipBmp != null) {
            Bitmap roundWorship = getRoundedCornerBitmap(worshipBmp, dp(12));
            ImageView img2 = new ImageView(this);
            img2.setImageBitmap(roundWorship);
            img2.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(120), 1.0f);
            lp2.setMargins(dp(8), 0, 0, 0);
            img2.setLayoutParams(lp2);
            artRow.addView(img2);
        }

        content.addView(artRow);

        // HYMN 1: GREAT IS THY FAITHFULNESS
        LinearLayout card1 = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        card1.setOrientation(LinearLayout.VERTICAL);

        TextView tag1 = createPillInline("FAITH & COVENANT", COLOR_PURPLE_TINT, COLOR_PURPLE);
        card1.addView(tag1);

        TextView h1Title = new TextView(this);
        h1Title.setText("Great Is Thy Faithfulness");
        h1Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        h1Title.setTypeface(Typeface.DEFAULT_BOLD);
        h1Title.setTextColor(COLOR_PURPLE_DARK);
        h1Title.setPadding(0, dp(6), 0, dp(2));
        card1.addView(h1Title);

        TextView h1Author = new TextView(this);
        h1Author.setText("Thomas O. Chisholm · Lamentations 3:22-23");
        h1Author.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        h1Author.setTextColor(COLOR_PURPLE_MUTED);
        h1Author.setPadding(0, 0, 0, dp(10));
        card1.addView(h1Author);

        TextView h1Lyrics = new TextView(this);
        h1Lyrics.setText("Great is Thy faithfulness, O God my Father,\nThere is no shadow of turning with Thee;\nThou changest not, Thy compassions, they fail not;\nAs Thou hast been Thou forever wilt be.\n\n[Chorus]\nGreat is Thy faithfulness! Great is Thy faithfulness!\nMorning by morning new mercies I see;\nAll I have needed Thy hand hath provided—\nGreat is Thy faithfulness, Lord, unto me!");
        h1Lyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        h1Lyrics.setTextColor(COLOR_PURPLE_DARK);
        h1Lyrics.setLineSpacing(dp(3), 1.15f);
        h1Lyrics.setPadding(0, 0, 0, dp(14));
        card1.addView(h1Lyrics);

        btnSingHymn1 = new Button(this);
        btnSingHymn1.setText("Sing & Praise (+10 XP)");
        btnSingHymn1.setTextColor(COLOR_WHITE);
        btnSingHymn1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSingHymn1.setTypeface(Typeface.DEFAULT_BOLD);
        btnSingHymn1.setBackground(create3dButtonDrawable(COLOR_ORANGE, COLOR_ORANGE_SHADOW, 12, 4));
        btnSingHymn1.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnSingHymn1.setOnClickListener(this);
        card1.addView(btnSingHymn1);

        content.addView(card1);

        // HYMN 2: AMAZING GRACE
        LinearLayout card2 = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        card2.setOrientation(LinearLayout.VERTICAL);

        TextView tag2 = createPillInline("REDEMPTION & HOPE", COLOR_ORANGE_TINT, COLOR_ORANGE);
        card2.addView(tag2);

        TextView h2Title = new TextView(this);
        h2Title.setText("Amazing Grace");
        h2Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        h2Title.setTypeface(Typeface.DEFAULT_BOLD);
        h2Title.setTextColor(COLOR_PURPLE_DARK);
        h2Title.setPadding(0, dp(6), 0, dp(2));
        card2.addView(h2Title);

        TextView h2Author = new TextView(this);
        h2Author.setText("John Newton · 1 Chronicles 17:16-17");
        h2Author.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        h2Author.setTextColor(COLOR_PURPLE_MUTED);
        h2Author.setPadding(0, 0, 0, dp(10));
        card2.addView(h2Author);

        TextView h2Lyrics = new TextView(this);
        h2Lyrics.setText("Amazing grace! how sweet the sound\nThat saved a wretch like me!\nI once was lost, but now am found,\nWas blind, but now I see.\n\n’Twas grace that taught my heart to fear,\nAnd grace my fears relieved;\nHow precious did that grace appear\nThe hour I first believed!");
        h2Lyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        h2Lyrics.setTextColor(COLOR_PURPLE_DARK);
        h2Lyrics.setLineSpacing(dp(3), 1.15f);
        h2Lyrics.setPadding(0, 0, 0, dp(14));
        card2.addView(h2Lyrics);

        btnSingHymn2 = new Button(this);
        btnSingHymn2.setText("Sing & Praise (+10 XP)");
        btnSingHymn2.setTextColor(COLOR_WHITE);
        btnSingHymn2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSingHymn2.setTypeface(Typeface.DEFAULT_BOLD);
        btnSingHymn2.setBackground(create3dButtonDrawable(COLOR_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnSingHymn2.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnSingHymn2.setOnClickListener(this);
        card2.addView(btnSingHymn2);

        content.addView(card2);

        // HYMN 3: BAAL PERAZIM ANTHEM
        LinearLayout card3 = createCard(this, COLOR_WHITE, dp(16), COLOR_ORANGE_BORDER, dp(2));
        card3.setOrientation(LinearLayout.VERTICAL);

        TextView tag3 = createPillInline("PERAZIM ANTHEM", COLOR_ORANGE_TINT, COLOR_ORANGE);
        card3.addView(tag3);

        TextView h3Title = new TextView(this);
        h3Title.setText("Baal Perazim — The Breakthrough Anthem");
        h3Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        h3Title.setTypeface(Typeface.DEFAULT_BOLD);
        h3Title.setTextColor(COLOR_PURPLE_DARK);
        h3Title.setPadding(0, dp(6), 0, dp(2));
        card3.addView(h3Title);

        TextView h3Lyrics = new TextView(this);
        h3Lyrics.setText("In the valley of the battle, when the enemy arose,\nDavid called upon Jehovah, conquering his greatest foes.\nLike a rushing mighty river breaking every valley wall,\nBaal Perazim went before him, causing every foe to fall!\n\n[Chorus]\nBreak out, O Lord of Glory, let Your flood of mercy surge!\nEvery barrier shatters down as Your holy saints emerge!\nBaal Perazim, our Master, on the rock our feet shall stand,\nFor the Lord of the breakthrough has claimed this holy land!");
        h3Lyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        h3Lyrics.setTextColor(COLOR_PURPLE_DARK);
        h3Lyrics.setLineSpacing(dp(3), 1.15f);
        h3Lyrics.setPadding(0, 0, 0, dp(14));
        card3.addView(h3Lyrics);

        btnSingHymn3 = new Button(this);
        btnSingHymn3.setText("Lift Anthem (+10 XP)");
        btnSingHymn3.setTextColor(COLOR_WHITE);
        btnSingHymn3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSingHymn3.setTypeface(Typeface.DEFAULT_BOLD);
        btnSingHymn3.setBackground(create3dButtonDrawable(COLOR_ORANGE, COLOR_ORANGE_SHADOW, 12, 4));
        btnSingHymn3.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnSingHymn3.setOnClickListener(this);
        card3.addView(btnSingHymn3);

        content.addView(card3);

        scrollView.addView(content);
        setContentView(scrollView);
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnSingHymn1 || v == btnSingHymn2 || v == btnSingHymn3) {
            Toast.makeText(this, "🎶 +10 XP earned! Praise lifted to the Throne of Grace!", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private Bitmap loadAssetBitmap(String name, int maxDim) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            java.io.InputStream is = getAssets().open(name);
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int sampleSize = 1;
            while ((opts.outWidth / sampleSize) > maxDim || (opts.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            is = getAssets().open(name);
            Bitmap bm = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bm;
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        if (bitmap == null) return null;
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawRoundRect(rectF, pixels, pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    private LinearLayout createCard(Context ctx, int bgColor, int paddingDp, int borderColor, int borderWidthDp) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setPadding(dp(paddingDp), dp(paddingDp), dp(paddingDp), dp(paddingDp));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(14));
        layout.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(14));
        if (borderWidthDp > 0) {
            bg.setStroke(dp(borderWidthDp), borderColor);
        }
        layout.setBackground(bg);
        return layout;
    }

    private TextView createPillInline(String text, int bgColor, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(textColor);
        tv.setPadding(dp(8), dp(4), dp(8), dp(4));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(12));
        tv.setBackground(gd);
        return tv;
    }

    private Drawable create3dButtonDrawable(int topColor, int bottomColor, int radiusDp, int shadowDepthDp) {
        GradientDrawable bottom = new GradientDrawable();
        bottom.setColor(bottomColor);
        bottom.setCornerRadius(dp(radiusDp));

        GradientDrawable top = new GradientDrawable();
        top.setColor(topColor);
        top.setCornerRadius(dp(radiusDp));

        LayerDrawable layer = new LayerDrawable(new Drawable[]{bottom, top});
        layer.setLayerInset(1, 0, 0, 0, dp(shadowDepthDp));
        return layer;
    }
}
