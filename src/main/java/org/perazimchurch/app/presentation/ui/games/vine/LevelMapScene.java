package org.perazimchurch.app.presentation.ui.games.vine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.FrameLayout;

import org.perazimchurch.app.presentation.ui.games.common.GameArt;
import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * One level-map scene: the painted map as a background, with up to eight level nodes placed
 * along the path. Children are added in node order, first node at the bottom left.
 */
final class LevelMapScene extends FrameLayout {

    /** The map art is 1024 by 576. The scene keeps that shape at any width. */
    private static final float ASPECT_HEIGHT = 576f / 1024f;

    LevelMapScene(Context context, Bitmap map) {
        super(context);
        if (map != null) {
            setBackground(new BitmapDrawable(context.getResources(), map));
        } else {
            setBackground(PerazimTheme.createBackgroundGradient());
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = Math.round(width * ASPECT_HEIGHT);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        float[][] nodes = GameArt.levelNodesFor(width, height);
        int count = Math.min(getChildCount(), nodes.length);
        for (int i = 0; i < count; i++) {
            View node = getChildAt(i);
            int w = node.getMeasuredWidth();
            int h = node.getMeasuredHeight();
            int cx = Math.round(nodes[i][0]);
            int cy = Math.round(nodes[i][1]);
            node.layout(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
        }
    }
}
