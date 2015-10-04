package io.github.tslamic.xkcdportal.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

/**
 * ViewPager only as high as its highest child.
 */
public class WrapViewPager extends ViewPager {

    private int mMaxHeight;

    public WrapViewPager(Context context) {
        super(context);
    }

    public WrapViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxChildHeight = 0;

        final int defaultMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        for (int i = 0, size = getChildCount(); i < size; i++) {
            final View child = getChildAt(i);
            child.measure(widthMeasureSpec, defaultMeasureSpec);
            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
        }

        // Adhere to the height of the views that might not be currently attached.
        mMaxHeight = Math.max(maxChildHeight, mMaxHeight);

        final int hms = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, hms);
    }

}
