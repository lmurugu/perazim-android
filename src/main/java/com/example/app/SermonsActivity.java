package com.example.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
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

public class SermonsActivity extends Activity implements View.OnClickListener {

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
    private Button btnWatchSeminar;
    private Button btnChannel;

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
        title.setText("SERMONS & MEDIA");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_PURPLE_DARK);
        title.setPadding(dp(16), 0, 0, 0);
        navBar.addView(title);

        content.addView(navBar);

        // Featured Speaker Header
        LinearLayout speakerCard = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        speakerCard.setOrientation(LinearLayout.HORIZONTAL);
        speakerCard.setGravity(Gravity.CENTER_VERTICAL);

        Bitmap bishopBmp = loadAssetBitmap("bishop_portrait.jpg", 300);
        if (bishopBmp != null) {
            Bitmap roundBishop = getCircularBitmap(bishopBmp);
            ImageView bView = new ImageView(this);
            bView.setImageBitmap(roundBishop);
            LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(dp(64), dp(64));
            lpB.setMargins(0, 0, dp(12), 0);
            bView.setLayoutParams(lpB);
            speakerCard.addView(bView);
        }

        LinearLayout spInfo = new LinearLayout(this);
        spInfo.setOrientation(LinearLayout.VERTICAL);

        TextView spName = new TextView(this);
        spName.setText("Bishop Dr. David Mutweri");
        spName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        spName.setTypeface(Typeface.DEFAULT_BOLD);
        spName.setTextColor(COLOR_PURPLE_DARK);
        spInfo.addView(spName);

        TextView spTitle = new TextView(this);
        spTitle.setText("Founder & General Overseer · PMC");
        spTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        spTitle.setTypeface(Typeface.DEFAULT_BOLD);
        spTitle.setTextColor(COLOR_PURPLE);
        spInfo.addView(spTitle);

        TextView spContact = new TextView(this);
        spContact.setText("📞 (+254) 0710 772 227 · ✉️ bishop@perazimchurch.org");
        spContact.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        spContact.setTextColor(COLOR_PURPLE_MUTED);
        spContact.setPadding(0, dp(2), 0, 0);
        spInfo.addView(spContact);

        speakerCard.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Bishop Dr. David Mutweri")
                    .setMessage("Presiding Bishop · Perazim Mission Church\n\n"
                            + "• Direct Phone: (+254) 0710 772 227\n"
                            + "• Bishop Email: bishop@perazimchurch.org\n"
                            + "• Church Office: info@perazimchurch.org\n"
                            + "• Giving Paybill: 4069983")
                    .setPositiveButton("📞 Call Bishop", (d, w) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+254710772227")));
                        } catch (Exception ignored) {}
                    })
                    .setNegativeButton("✉️ Email Bishop", (d, w) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:bishop@perazimchurch.org")));
                        } catch (Exception ignored) {}
                    })
                    .setNeutralButton("Close", null)
                    .show();
        });

        speakerCard.addView(spInfo);
        content.addView(speakerCard);

        // Featured Seminar Card
        LinearLayout seminarCard = createCard(this, COLOR_WHITE, dp(16), COLOR_ORANGE_BORDER, dp(2));
        seminarCard.setOrientation(LinearLayout.VERTICAL);

        TextView semTag = createPillInline("FEATURED LEADERSHIP SEMINAR", COLOR_ORANGE_TINT, COLOR_ORANGE);
        seminarCard.addView(semTag);

        TextView semTitle = new TextView(this);
        semTitle.setText("Embu Pastors & Leaders Couples Seminar 2025");
        semTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        semTitle.setTypeface(Typeface.DEFAULT_BOLD);
        semTitle.setTextColor(COLOR_PURPLE_DARK);
        semTitle.setPadding(0, dp(8), 0, dp(4));
        seminarCard.addView(semTitle);

        TextView semMeta = new TextView(this);
        semMeta.setText("Recorded Live at Perazim Mission Church Headquarters, Embu");
        semMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        semMeta.setTextColor(COLOR_PURPLE_MUTED);
        semMeta.setPadding(0, 0, 0, dp(12));
        seminarCard.addView(semMeta);

        TextView semNotes = new TextView(this);
        semNotes.setText("• Kingdom Order: True ministerial fruitfulness begins at the family altar.\n• Spiritual Roots: The depth of your private prayer determines your public breakout.\n• Covenant Power: God's breakthrough surges where couples stand in righteous unity.");
        semNotes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        semNotes.setTextColor(COLOR_PURPLE_DARK);
        semNotes.setLineSpacing(dp(3), 1.15f);
        semNotes.setPadding(0, 0, 0, dp(14));
        seminarCard.addView(semNotes);

        btnWatchSeminar = new Button(this);
        btnWatchSeminar.setText("▶ Watch on YouTube (3O6meSzCl3I)");
        btnWatchSeminar.setTextColor(COLOR_WHITE);
        btnWatchSeminar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnWatchSeminar.setTypeface(Typeface.DEFAULT_BOLD);
        btnWatchSeminar.setBackground(create3dButtonDrawable(COLOR_ORANGE, COLOR_ORANGE_SHADOW, 12, 4));
        btnWatchSeminar.setPadding(dp(14), dp(12), dp(14), dp(12));
        btnWatchSeminar.setOnClickListener(this);
        seminarCard.addView(btnWatchSeminar);

        content.addView(seminarCard);

        // Official Channel Portal Card
        LinearLayout channelCard = createCard(this, COLOR_WHITE, dp(16), COLOR_PURPLE_BORDER, dp(1));
        channelCard.setOrientation(LinearLayout.VERTICAL);

        TextView chTitle = new TextView(this);
        chTitle.setText("Official YouTube Broadcasts");
        chTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        chTitle.setTypeface(Typeface.DEFAULT_BOLD);
        chTitle.setTextColor(COLOR_PURPLE_DARK);
        channelCard.addView(chTitle);

        TextView chDesc = new TextView(this);
        chDesc.setText("Subscribe to @perazimchurchembu for Sunday service live streams, Kesha revivals, and biblical doctrine series.");
        chDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chDesc.setTextColor(COLOR_PURPLE_MUTED);
        chDesc.setPadding(0, dp(4), 0, dp(12));
        channelCard.addView(chDesc);

        btnChannel = new Button(this);
        btnChannel.setText("Visit Channel (@perazimchurchembu)");
        btnChannel.setTextColor(COLOR_WHITE);
        btnChannel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnChannel.setTypeface(Typeface.DEFAULT_BOLD);
        btnChannel.setBackground(create3dButtonDrawable(COLOR_PURPLE, COLOR_PURPLE_SHADOW, 12, 4));
        btnChannel.setPadding(dp(14), dp(12), dp(14), dp(12));
        btnChannel.setOnClickListener(this);
        channelCard.addView(btnChannel);

        content.addView(channelCard);

        scrollView.addView(content);
        setContentView(scrollView);
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnWatchSeminar) {
            openUrl("https://www.youtube.com/watch?v=3O6meSzCl3I");
        } else if (v == btnChannel) {
            openUrl("https://youtube.com/@perazimchurchembu");
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Opening URL: " + url, Toast.LENGTH_SHORT).show();
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

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect((bitmap.getWidth() - size) / 2, (bitmap.getHeight() - size) / 2, (bitmap.getWidth() + size) / 2, (bitmap.getHeight() + size) / 2);
        final Rect dstRect = new Rect(0, 0, size, size);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, dstRect, paint);
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
