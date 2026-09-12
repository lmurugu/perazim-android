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

public class RiddlesActivity extends Activity implements View.OnClickListener {

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
    private Button btnRiddle1;
    private Button btnRiddle2;
    private Button btnRiddle3;
    private Button btnRiddle4;

    private TextView ansRiddle1;
    private TextView ansRiddle2;
    private TextView ansRiddle3;
    private TextView ansRiddle4;

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
        title.setText("BIBLICAL RIDDLES");
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
                    dp(160)
            );
            lpArt.setMargins(0, 0, 0, dp(14));
            artView.setLayoutParams(lpArt);
            content.addView(artView);
        }

        // RIDDLE 1: ADAM
        LinearLayout card1 = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        card1.setOrientation(LinearLayout.VERTICAL);

        TextView tag1 = createPillInline("CREATION RIDDLE", COLOR_PURPLE_TINT, COLOR_PURPLE);
        card1.addView(tag1);

        TextView q1 = new TextView(this);
        q1.setText("I had no mother and no father. I had no childhood years or youth. I walked in the garden before sin ever entered the world. Who am I?");
        q1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        q1.setTextColor(COLOR_PURPLE_DARK);
        q1.setLineSpacing(dp(2), 1.15f);
        q1.setPadding(0, dp(8), 0, dp(10));
        card1.addView(q1);

        btnRiddle1 = new Button(this);
        btnRiddle1.setText("Reveal Answer (+10 XP)");
        btnRiddle1.setTextColor(COLOR_WHITE);
        btnRiddle1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnRiddle1.setTypeface(Typeface.DEFAULT_BOLD);
        btnRiddle1.setBackground(create3dButtonDrawable(COLOR_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnRiddle1.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnRiddle1.setOnClickListener(this);
        card1.addView(btnRiddle1);

        ansRiddle1 = createAnswerView("Answer: Adam!\nFormed directly from the dust of the ground by God (Genesis 2:7).");
        card1.addView(ansRiddle1);
        content.addView(card1);

        // RIDDLE 2: LAZARUS
        LinearLayout card2 = createCard(this, COLOR_WHITE, dp(16), COLOR_ORANGE_BORDER, dp(2));
        card2.setOrientation(LinearLayout.VERTICAL);

        TextView tag2 = createPillInline("MIRACLE RIDDLE", COLOR_ORANGE_TINT, COLOR_ORANGE);
        card2.addView(tag2);

        TextView q2 = new TextView(this);
        q2.setText("I was wrapped in graveclothes and laid in a stone tomb for four days until a single voice called me forth to walk the earth again. Who am I?");
        q2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        q2.setTextColor(COLOR_PURPLE_DARK);
        q2.setLineSpacing(dp(2), 1.15f);
        q2.setPadding(0, dp(8), 0, dp(10));
        card2.addView(q2);

        btnRiddle2 = new Button(this);
        btnRiddle2.setText("Reveal Answer (+10 XP)");
        btnRiddle2.setTextColor(COLOR_WHITE);
        btnRiddle2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnRiddle2.setTypeface(Typeface.DEFAULT_BOLD);
        btnRiddle2.setBackground(create3dButtonDrawable(COLOR_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
        btnRiddle2.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnRiddle2.setOnClickListener(this);
        card2.addView(btnRiddle2);

        ansRiddle2 = createAnswerView("Answer: Lazarus of Bethany!\nJesus cried with a loud voice: 'Lazarus, come out!' and he came forth alive (John 11:43–44).");
        card2.addView(ansRiddle2);
        content.addView(card2);

        // RIDDLE 3: BAAL PERAZIM
        LinearLayout card3 = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        card3.setOrientation(LinearLayout.VERTICAL);

        TextView tag3 = createPillInline("BREAKTHROUGH RIDDLE", COLOR_PURPLE_TINT, COLOR_PURPLE);
        card3.addView(tag3);

        TextView q3 = new TextView(this);
        q3.setText("When David fought the Philistines, God broke out before him like a rushing flood. What was this valley of victory named?");
        q3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        q3.setTextColor(COLOR_PURPLE_DARK);
        q3.setLineSpacing(dp(2), 1.15f);
        q3.setPadding(0, dp(8), 0, dp(10));
        card3.addView(q3);

        btnRiddle3 = new Button(this);
        btnRiddle3.setText("Reveal Answer (+10 XP)");
        btnRiddle3.setTextColor(COLOR_WHITE);
        btnRiddle3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnRiddle3.setTypeface(Typeface.DEFAULT_BOLD);
        btnRiddle3.setBackground(create3dButtonDrawable(COLOR_ORANGE, COLOR_ORANGE_SHADOW, 10, 3));
        btnRiddle3.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnRiddle3.setOnClickListener(this);
        card3.addView(btnRiddle3);

        ansRiddle3 = createAnswerView("Answer: Baal Perazim!\n'The LORD has broken out against my enemies before me like bursting water' (2 Samuel 5:20).");
        card3.addView(ansRiddle3);
        content.addView(card3);

        // RIDDLE 4: METHUSELAH
        LinearLayout card4 = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        card4.setOrientation(LinearLayout.VERTICAL);

        TextView tag4 = createPillInline("GENEALOGY RIDDLE", COLOR_ORANGE_TINT, COLOR_ORANGE);
        card4.addView(tag4);

        TextView q4 = new TextView(this);
        q4.setText("I lived longer than any other man recorded in Scripture (969 years), yet I died before my father did. How can this be?");
        q4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        q4.setTextColor(COLOR_PURPLE_DARK);
        q4.setLineSpacing(dp(2), 1.15f);
        q4.setPadding(0, dp(8), 0, dp(10));
        card4.addView(q4);

        btnRiddle4 = new Button(this);
        btnRiddle4.setText("Reveal Answer (+10 XP)");
        btnRiddle4.setTextColor(COLOR_WHITE);
        btnRiddle4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnRiddle4.setTypeface(Typeface.DEFAULT_BOLD);
        btnRiddle4.setBackground(create3dButtonDrawable(COLOR_PURPLE, COLOR_PURPLE_SHADOW, 10, 3));
        btnRiddle4.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnRiddle4.setOnClickListener(this);
        card4.addView(btnRiddle4);

        ansRiddle4 = createAnswerView("Answer: Methuselah!\nHis father was Enoch, who walked with God and never died because God took him (Genesis 5:24).");
        card4.addView(ansRiddle4);
        content.addView(card4);

        scrollView.addView(content);
        setContentView(scrollView);
    }

    private TextView createAnswerView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(COLOR_PURPLE_DARK);
        tv.setLineSpacing(dp(2), 1.15f);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setVisibility(View.GONE);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_PURPLE_TINT);
        gd.setCornerRadius(dp(8));
        gd.setStroke(dp(1), COLOR_PURPLE_BORDER);
        tv.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(10), 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnRiddle1) {
            revealAnswer(ansRiddle1, btnRiddle1);
        } else if (v == btnRiddle2) {
            revealAnswer(ansRiddle2, btnRiddle2);
        } else if (v == btnRiddle3) {
            revealAnswer(ansRiddle3, btnRiddle3);
        } else if (v == btnRiddle4) {
            revealAnswer(ansRiddle4, btnRiddle4);
        }
    }

    private void revealAnswer(TextView ansView, Button btn) {
        if (ansView.getVisibility() == View.GONE) {
            ansView.setVisibility(View.VISIBLE);
            btn.setText("Hide Answer");
            Toast.makeText(this, "🧩 +10 XP earned! Scriptural mystery solved!", Toast.LENGTH_SHORT).show();
        } else {
            ansView.setVisibility(View.GONE);
            btn.setText("Reveal Answer (+10 XP)");
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
