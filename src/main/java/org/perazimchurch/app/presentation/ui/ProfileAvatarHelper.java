package org.perazimchurch.app.presentation.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/** Helpers for deterministic defaults, circular photo preparation and upload decoding. */
final class ProfileAvatarHelper {
    static final int COUNT = 20;
    static final int MAX_UPLOAD_BYTES = 1024 * 1024;

    private ProfileAvatarHelper() {}

    static int defaultNumber(@Nullable String userId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((userId == null ? "guest" : userId).getBytes("UTF-8"));
            long value = ((digest[0] & 0xffL) << 24) | ((digest[1] & 0xffL) << 16)
                    | ((digest[2] & 0xffL) << 8) | (digest[3] & 0xffL);
            return (int) (value % COUNT) + 1;
        } catch (Exception ignored) {
            return 1;
        }
    }

    static int numberFor(@Nullable String avatarRef, @Nullable String userId) {
        if (avatarRef != null && avatarRef.matches("default:(0?[1-9]|1[0-9]|20)")) {
            try { return Integer.parseInt(avatarRef.substring(8)); }
            catch (NumberFormatException ignored) { }
        }
        return defaultNumber(userId);
    }

    static int drawableId(@NonNull Context context, int number) {
        String name = String.format(java.util.Locale.US, "default_avatar_%02d", number);
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    @Nullable
    static byte[] makeCircularPng(@NonNull Context context, @NonNull Uri uri) {
        Bitmap source = null;
        try (InputStream boundsInput = context.getContentResolver().openInputStream(uri)) {
            if (boundsInput == null) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(boundsInput, null, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.min(bounds.outWidth, bounds.outHeight) / 1024);
            try (InputStream imageInput = context.getContentResolver().openInputStream(uri)) {
                if (imageInput == null) return null;
                source = BitmapFactory.decodeStream(imageInput, null, options);
            }
        } catch (Exception ignored) {
            return null;
        }
        if (source == null) return null;

        int side = Math.min(source.getWidth(), source.getHeight());
        int left = (source.getWidth() - side) / 2;
        int top = (source.getHeight() - side) / 2;
        Bitmap output = null;
        try {
            Bitmap square = Bitmap.createBitmap(source, left, top, side, side);
            if (square != source) source.recycle();
            source = null;
            int target = Math.min(512, side);
            Bitmap scaled = Bitmap.createScaledBitmap(square, target, target, true);
            if (scaled != square) square.recycle();
            output = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawCircle(target / 2f, target / 2f, target / 2f, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(scaled, new Rect(0, 0, target, target),
                    new RectF(0, 0, target, target), paint);
            paint.setXfermode(null);
            scaled.recycle();

            while (target >= 128) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                output.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                if (bytes.size() <= MAX_UPLOAD_BYTES) return bytes.toByteArray();
                bytes.close();
                int next = target / 2;
                Bitmap smaller = Bitmap.createScaledBitmap(output, next, next, true);
                output.recycle();
                output = smaller;
                target = next;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            output.compress(Bitmap.CompressFormat.PNG, 100, bytes);
            return bytes.size() <= MAX_UPLOAD_BYTES ? bytes.toByteArray() : null;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (source != null && !source.isRecycled()) source.recycle();
            if (output != null && !output.isRecycled()) output.recycle();
        }
    }
}
