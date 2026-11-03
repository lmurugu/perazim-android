package org.perazimchurch.app;

import android.app.Activity;
import android.app.AlertDialog;
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

public class ReflectionsActivity extends Activity implements View.OnClickListener {

    public static final int COLOR_PURPLE = Color.parseColor("#681A7D");
    public static final int COLOR_PURPLE_DARK = Color.parseColor("#2D0938");
    public static final int COLOR_PURPLE_DEEPEST = Color.parseColor("#140319");
    public static final int COLOR_PURPLE_SHADOW = Color.parseColor("#450E53");
    public static final int COLOR_PURPLE_SOFT = Color.parseColor("#C896D8");
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
    private Button btnMeditate;
    private Button btnPray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_CANVAS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(36));

        // Top Navigation Bar with Back Button
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
        title.setText("REFLECTIONS");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        title.setPadding(dp(16), 0, 0, 0);
        navBar.addView(title);

        content.addView(navBar);

        // Classical Art Banner
        Bitmap artBmp = loadAssetBitmap("art_lazarus.jpg", 800);
        if (artBmp != null) {
            Bitmap roundArt = getRoundedCornerBitmap(artBmp, dp(14));
            ImageView artView = new ImageView(this);
            artView.setImageBitmap(roundArt);
            artView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lpArt = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(180)
            );
            lpArt.setMargins(0, 0, 0, dp(14));
            artView.setLayoutParams(lpArt);
            content.addView(artView);
        }

        // Covenant Devotional Card
        LinearLayout card = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        card.setOrientation(LinearLayout.VERTICAL);

        TextView pill = createPillInline("2 SAMUEL 5:20 · BAAL PERAZIM", COLOR_ORANGE_TINT, COLOR_ORANGE);
        card.addView(pill);

        TextView heading = new TextView(this);
        heading.setText("The God of the Overflowing Breakthrough");
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextColor(COLOR_PURPLE_DARK);
        heading.setPadding(0, dp(8), 0, dp(8));
        card.addView(heading);

        TextView scripture = new TextView(this);
        scripture.setText("“As waters break out, the LORD has broken out against my enemies before me — therefore he named that place Baal Perazim.”");
        scripture.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        scripture.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        scripture.setTextColor(COLOR_PURPLE);
        scripture.setPadding(0, 0, 0, dp(12));
        scripture.setLineSpacing(dp(3), 1.15f);
        card.addView(scripture);

        TextView body = new TextView(this);
        body.setText("When King David confronted the Philistines in the Valley of Rephaim, he sought counsel from the Lord. God responded with divine assurance: He would break through like a gushing stream. In Hebrew, Baal Perazim signifies the Master of Breakthroughs — God shattering obstacles like a swollen river.\n\nJust as Christ commanded Lazarus to step forth from four days in the tomb, God speaks life into situations deemed impossible. Stand firm in covenant faith today: delayed promises and stubborn barriers are giving way to the surge of the Holy Spirit.");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        body.setTextColor(COLOR_PURPLE_MUTED);
        body.setLineSpacing(dp(3), 1.15f);
        body.setPadding(0, 0, 0, dp(14));
        card.addView(body);

        btnMeditate = new Button(this);
        btnMeditate.setText("Meditate & Reflect (+10 XP)");
        btnMeditate.setTextColor(COLOR_WHITE);
        btnMeditate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnMeditate.setTypeface(Typeface.DEFAULT_BOLD);
        btnMeditate.setBackground(create3dButtonDrawable(COLOR_ORANGE, COLOR_ORANGE_SHADOW, 12, 4));
        btnMeditate.setPadding(dp(14), dp(12), dp(14), dp(12));
        btnMeditate.setOnClickListener(this);
        card.addView(btnMeditate);

        content.addView(card);

        // Prayer Card
        LinearLayout prayerCard = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        prayerCard.setOrientation(LinearLayout.VERTICAL);

        TextView prTitle = new TextView(this);
        prTitle.setText("Breakthrough Prayer");
        prTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        prTitle.setTypeface(Typeface.DEFAULT_BOLD);
        prTitle.setTextColor(COLOR_PURPLE_DARK);
        prayerCard.addView(prTitle);

        TextView prBody = new TextView(this);
        prBody.setText("“Heavenly Father, You are the Lord of Baal Perazim. Burst open doors that no man can shut. Break down fear, sickness, and stagnation in my household. In Jesus’ name, Amen.”");
        prBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        prBody.setTextColor(COLOR_PURPLE_MUTED);
        prBody.setPadding(0, dp(6), 0, dp(12));
        prayerCard.addView(prBody);

        btnPray = new Button(this);
        btnPray.setText("Pray with Faith");
        btnPray.setTextColor(COLOR_WHITE);
        btnPray.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnPray.setTypeface(Typeface.DEFAULT_BOLD);
        btnPray.setBackground(create3dButtonDrawable(COLOR_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnPray.setPadding(dp(14), dp(12), dp(14), dp(12));
        btnPray.setOnClickListener(this);
        prayerCard.addView(btnPray);

        content.addView(prayerCard);

        scrollView.addView(content);
        setContentView(scrollView);
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnMeditate) {
            Toast.makeText(this, "⭐ +10 XP earned! Covenant reflection recorded.", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(this)
                    .setTitle("Covenant Reflection")
                    .setMessage("Take 2 quiet minutes to meditate on God's breakout power in your finances, family, and health.")
                    .setPositiveButton("Amen", null)
                    .show();
        } else if (v == btnPray) {
            Toast.makeText(this, "🙏 Prayer lifted in faith before Baal Perazim!", Toast.LENGTH_SHORT).show();
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
