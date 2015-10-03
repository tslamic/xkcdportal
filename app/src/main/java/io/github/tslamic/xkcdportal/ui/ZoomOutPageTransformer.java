package io.github.tslamic.xkcdportal.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Does fancy ViewPager swipe animation. Shamelessly copied from http://developer.android.com/training/animation/screen-slide.html.
 */
public class ZoomOutPageTransformer implements ViewPager.PageTransformer {

    private static final float MIN_SCALE = 0.85f;
    private static final float MIN_ALPHA = 0.5f;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void transformPage(View page, float position) {
        final int pageWidth = page.getWidth();
        final int pageHeight = page.getHeight();

        if (position < -1) {
            page.setAlpha(0);
        } else if (position <= 1) {
            final float scale = Math.max(MIN_SCALE, 1 - Math.abs(position));
            final float diff = 1 - scale;
            float vm = pageHeight * diff / 2;
            float hm = pageWidth * diff / 2;
            if (position < 0) {
                page.setTranslationX(hm - vm / 2);
            } else {
                page.setTranslationX(-hm + vm / 2);
            }
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setAlpha(MIN_ALPHA + (scale - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
        } else {
            page.setAlpha(0);
        }
    }

}
