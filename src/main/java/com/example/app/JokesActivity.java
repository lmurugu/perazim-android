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

public class JokesActivity extends Activity implements View.OnClickListener {

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
    private Button btnSmile;

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
        title.setText("CHRISTIAN JOY & HUMOR");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        title.setPadding(dp(16), 0, 0, 0);
        navBar.addView(title);

        content.addView(navBar);

        // Classical Art Banner
        Bitmap artBmp = loadAssetBitmap("art_christian.jpg", 800);
        if (artBmp != null) {
            Bitmap roundArt = getRoundedCornerBitmap(artBmp, dp(14));
            ImageView artView = new ImageView(this);
            artView.setImageBitmap(roundArt);
            artView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpArt = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(160)
            );
            lpArt.setMargins(0, 0, 0, dp(14));
            artView.setLayoutParams(lpArt);
            content.addView(artView);
        }

        // Header Quote Card
        LinearLayout quoteCard = createCard(this, COLOR_WHITE, dp(14), COLOR_PURPLE_BORDER, dp(1));
        quoteCard.setOrientation(LinearLayout.VERTICAL);
        TextView quote = new TextView(this);
        quote.setText("“A cheerful heart is good medicine, but a crushed spirit dries up the bones.” — Proverbs 17:22");
        quote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        quote.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        quote.setTextColor(COLOR_PURPLE);
        quote.setLineSpacing(dp(2), 1.15f);
        quoteCard.addView(quote);
        content.addView(quoteCard);

        // JOKE 1
        content.addView(createJokeCard(
                "FINANCIAL ACUMEN",
                "Who was the greatest financier in the Bible?",
                "Noah! He floated his entire stock successfully while the rest of the world was in total liquidation!",
                COLOR_PURPLE_TINT,
                COLOR_PURPLE,
                COLOR_PURPLE_BORDER
        ));

        // JOKE 2
        content.addView(createJokeCard(
                "SUNDAY SCHOOL CLASSIC",
                "Why did Moses wander in the desert for 40 years?",
                "Because even in antiquity, men steadfastly refused to stop and ask for directions!",
                COLOR_ORANGE_TINT,
                COLOR_ORANGE,
                COLOR_ORANGE_BORDER
        ));

        // JOKE 3
        content.addView(createJokeCard(
                "PROPHETIC DELIVERY",
                "Why did Elijah never need food delivery apps?",
                "Because the ravens delivered his meals by the Brook Cherith every single day — with zero fees and top ratings!",
                COLOR_PURPLE_TINT,
                COLOR_PURPLE,
                COLOR_PURPLE_BORDER
        ));

        // JOKE 4
        content.addView(createJokeCard(
                "MARINE BIOLOGY",
                "Why was Jonah the ultimate deep-sea explorer?",
                "He spent three full days studying marine anatomy from the inside out before concluding it was time to preach in Nineveh!",
                COLOR_ORANGE_TINT,
                COLOR_ORANGE,
                COLOR_ORANGE_BORDER
        ));

        // JOKE 5
        content.addView(createJokeCard(
                "SUNDAY SLEEPER",
                "The Pastor and the Usher",
                "Pastor: 'Usher, wake up that brother snoring in row four!'\nUsher: 'Pastor, you put him to sleep, you wake him up!'",
                COLOR_PURPLE_TINT,
                COLOR_PURPLE,
                COLOR_PURPLE_BORDER
        ));

        // Interactive Button
        btnSmile = new Button(this);
        btnSmile.setText("Smile & Praise (+10 XP)");
        btnSmile.setTextColor(COLOR_WHITE);
        btnSmile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnSmile.setTypeface(Typeface.DEFAULT_BOLD);
        btnSmile.setBackground(create3dButtonDrawable(COLOR_ORANGE, COLOR_ORANGE_SHADOW, 12, 4));
        btnSmile.setPadding(dp(14), dp(12), dp(14), dp(12));
        btnSmile.setOnClickListener(this);
        content.addView(btnSmile);

        scrollView.addView(content);
        setContentView(scrollView);
    }

    private LinearLayout createJokeCard(String badge, String title, String body, int badgeBg, int badgeText, int borderColor) {
        LinearLayout card = createCard(this, COLOR_WHITE, dp(14), borderColor, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);

        TextView pill = createPillInline(badge, badgeBg, badgeText);
        card.addView(pill);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_PURPLE_DARK);
        tvTitle.setPadding(0, dp(6), 0, dp(4));
        card.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvBody.setTextColor(COLOR_PURPLE_MUTED);
        tvBody.setLineSpacing(dp(2), 1.15f);
        card.addView(tvBody);

        return card;
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnSmile) {
            Toast.makeText(this, "😄 +10 XP earned! Joy of the Lord is our strength!", Toast.LENGTH_SHORT).show();
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
