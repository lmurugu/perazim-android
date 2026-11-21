package org.perazimchurch.app.presentation.ui.games.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.io.InputStream;

/**
 * The game-feel art from src/main/assets/games/ui: a particle sprite sheet, the button and panel frames,
 * and the level-map scene. Each image is decoded once at half size and kept for the app's life, which
 * costs about 0.6 MB each. If an image is missing, every getter returns null and the callers draw
 * their plain fallbacks.
 *
 * <p>Coordinates below are in the original 1024 by 576 pixels of each image.
 */
public final class GameArt {

    static final int SOURCE_WIDTH = 1024;
    static final int SOURCE_HEIGHT = 576;

    /** Sprite rectangles in the particle sheet (approximate cells, centred on each sprite). */
    public static final int[] PARTICLE_STAR = {442, 2, 538, 98};
    public static final int[] PARTICLE_SPARKLE = {322, 97, 418, 193};
    public static final int[] PARTICLE_FLARE = {202, 287, 298, 383};

    /** Frames in the UI sheet. The pills and panels are nine-sliced, so only their corners stay fixed. */
    public static final int[] FRAME_PILL_ORANGE = {25, 30, 178, 88};
    public static final int[] FRAME_PILL_PURPLE = {190, 30, 342, 88};
    public static final int[] FRAME_PANEL_WIDE = {465, 262, 732, 408};
    public static final int[] FRAME_PANEL_TALL = {22, 255, 230, 528};
    public static final int FRAME_BORDER = 22;

    /**
     * Level nodes on the level-map scene as fractions of its width and height, from the first level
     * (bottom left) to the last (top right). Use these to place level buttons along the path.
     */
    public static final float[][] LEVEL_NODES = {
            {0.200f, 0.755f}, {0.380f, 0.670f}, {0.510f, 0.580f}, {0.655f, 0.465f},
            {0.650f, 0.480f}, {0.760f, 0.370f}, {0.790f, 0.270f}, {0.840f, 0.085f}
    };

    private static volatile Bitmap particles;
    private static volatile Bitmap frames;
    private static volatile Bitmap levelMap;

    private GameArt() {}

    /** The particle sprite sheet, or null when the asset is missing. */
    public static Bitmap particles(Context context) {
        if (particles == null) particles = load(context, "games/ui/game_feel_particles.webp");
        return particles;
    }

    /** The button and panel frame sheet, or null when the asset is missing. */
    public static Bitmap frames(Context context) {
        if (frames == null) frames = load(context, "games/ui/game_feel_ui_frames.webp");
        return frames;
    }

    /** The level-map scene, or null when the asset is missing. */
    public static Bitmap levelMap(Context context) {
        if (levelMap == null) levelMap = load(context, "games/ui/game_feel_level_map.webp");
        return levelMap;
    }

    /** Level node positions in pixels for a level picker of the given size, in the same order as LEVEL_NODES. */
    public static float[][] levelNodesFor(int width, int height) {
        float[][] out = new float[LEVEL_NODES.length][2];
        for (int i = 0; i < LEVEL_NODES.length; i++) {
            out[i][0] = LEVEL_NODES[i][0] * width;
            out[i][1] = LEVEL_NODES[i][1] * height;
        }
        return out;
    }

    /** A source rectangle in the decoded bitmap's pixels, scaled from the 1024 by 576 coordinates. */
    static Rect source(Bitmap bitmap, int[] r) {
        float sx = bitmap.getWidth() / (float) SOURCE_WIDTH;
        float sy = bitmap.getHeight() / (float) SOURCE_HEIGHT;
        return new Rect(Math.round(r[0] * sx), Math.round(r[1] * sy), Math.round(r[2] * sx), Math.round(r[3] * sy));
    }

    /** A nine-sliced frame from the UI sheet. Null when the sheet is missing. */
    public static Drawable frame(Context context, int[] sourceRect, int border, float cornerPx) {
        Bitmap sheet = frames(context);
        if (sheet == null) return null;
        return new NineSliceDrawable(sheet, source(sheet, sourceRect), Math.round(border * sheet.getWidth() / (float) SOURCE_WIDTH), cornerPx);
    }

    private static Bitmap load(Context context, String path) {
        try (InputStream in = context.getAssets().open(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            options.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(in, null, options);
        } catch (Exception e) {
            return null;
        }
    }

    /** Draws a bitmap rectangle as nine slices: fixed corners, stretched edges and centre. */
    static final class NineSliceDrawable extends Drawable {
        private final Bitmap bitmap;
        private final Rect src;
        private final int border;
        private final float corner;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        NineSliceDrawable(Bitmap bitmap, Rect src, int border, float corner) {
            this.bitmap = bitmap;
            this.src = src;
            this.border = border;
            this.corner = corner;
        }

        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            int cw = Math.round(Math.min(corner, b.width() / 2f));
            int ch = Math.round(Math.min(corner, b.height() / 2f));
            int[] xs = {src.left, src.left + border, src.right - border, src.right};
            int[] ys = {src.top, src.top + border, src.bottom - border, src.bottom};
            int[] dx = {b.left, b.left + cw, b.right - cw, b.right};
            int[] dy = {b.top, b.top + ch, b.bottom - ch, b.bottom};
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    Rect s = new Rect(xs[col], ys[row], xs[col + 1], ys[row + 1]);
                    Rect d = new Rect(dx[col], dy[row], dx[col + 1], dy[row + 1]);
                    if (s.width() > 0 && s.height() > 0 && d.width() > 0 && d.height() > 0) {
                        canvas.drawBitmap(bitmap, s, d, paint);
                    }
                }
            }
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
