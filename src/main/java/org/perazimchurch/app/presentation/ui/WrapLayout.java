package org.perazimchurch.app.presentation.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Places children left to right (or right to left) and wraps them onto new lines when a row is full.
 * Used for the interlinear word columns, so a verse fits the card width without sideways scrolling.
 */
final class WrapLayout extends ViewGroup {

    private final int horizontalGap;
    private final int verticalGap;
    private boolean rightToLeft;
    private int[] xs = new int[0];
    private int[] ys = new int[0];

    WrapLayout(Context context, int horizontalGap, int verticalGap) {
        super(context);
        this.horizontalGap = horizontalGap;
        this.verticalGap = verticalGap;
    }

    void setRightToLeft(boolean value) {
        if (rightToLeft != value) {
            rightToLeft = value;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int available = MeasureSpec.getSize(widthMeasureSpec);
        int count = getChildCount();
        xs = new int[count];
        ys = new int[count];
        int x = 0;
        int y = 0;
        int lineHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChild(child, MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();
            if (x > 0 && x + w > available) {
                x = 0;
                y += lineHeight + verticalGap;
                lineHeight = 0;
            }
            xs[i] = x;
            ys[i] = y;
            x += w + horizontalGap;
            lineHeight = Math.max(lineHeight, h);
        }
        int height = y + lineHeight;
        setMeasuredDimension(available, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();
            int x = rightToLeft ? width - xs[i] - w : xs[i];
            child.layout(x, ys[i], x + w, ys[i] + h);
        }
    }
}
