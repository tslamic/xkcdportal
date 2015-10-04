package io.github.tslamic.xkcdportal.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.Util;

/**
 * Zoomable and draggable ImageView.
 */
public class ZoomImageView extends ImageView {

    public interface InteractionListener {

        void onLongPress();

    }

    private static final float MIN_SCALE_FACTOR = 1.0f;
    private static final float MAX_SCALE_FACTOR = 5.0f;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;

    private InteractionListener mInteractionListener;
    private Interpolator mInterpolator;

    private float mScaleFactor = MIN_SCALE_FACTOR;
    private float mTranslateX;
    private float mTranslateY;
    private float mFocusX;
    private float mFocusY;

    private boolean mIsZoomed;

    public ZoomImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        mScaleGestureDetector = new ScaleGestureDetector(context, new PinchListener());
        mGestureDetector = new GestureDetectorCompat(context, new DragListener());
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        getParent().requestDisallowInterceptTouchEvent(mIsZoomed);
        if (!mScaleGestureDetector.isInProgress()) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mIsZoomed) {
            canvas.save();
            canvas.scale(mScaleFactor, mScaleFactor, mFocusX, mFocusY);
            canvas.translate(mTranslateX, mTranslateY);
            super.onDraw(canvas);
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }

    public void setInteractionListener(@Nullable InteractionListener listener) {
        mInteractionListener = listener;
    }

    private class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            final float scale = mScaleFactor * detector.getScaleFactor();
            mFocusX = detector.getFocusX();
            mFocusY = detector.getFocusY();
            setScale(scale);
            return true;
        }

    }

    private class DragListener extends GestureDetector.SimpleOnGestureListener {

        private static final int ZOOM_ANIM_DURATION = 200;

        private static final float MIN_SCALE_DOUBLE_TAP = MIN_SCALE_FACTOR;
        private static final float MAX_SCALE_DOUBLE_TAP = MAX_SCALE_FACTOR / 2;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mTranslateX -= (distanceX / mScaleFactor);
            mTranslateY -= (distanceY / mScaleFactor);
            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (null != mInteractionListener) {
                mInteractionListener.onLongPress();
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (MIN_SCALE_FACTOR == mScaleFactor) {
                mFocusX = e.getX();
                mFocusY = e.getY();
            }

            final boolean zoomOut;
            final float from;
            final float to;

            if (MIN_SCALE_FACTOR == mScaleFactor) {
                zoomOut = false;
                from = MIN_SCALE_DOUBLE_TAP;
                to = MAX_SCALE_DOUBLE_TAP;
            } else {
                zoomOut = true;
                from = MAX_SCALE_DOUBLE_TAP;
                to = MIN_SCALE_DOUBLE_TAP;
            }

            if (Util.isHoneycombOrAbove()) {
                performDoubleTapOnHoneycombOrAbove(zoomOut, ZOOM_ANIM_DURATION, from, to);
            } else {
                if (zoomOut) {
                    mTranslateX = 0;
                    mTranslateY = 0;
                }
                setScale(to);
            }

            Analytics.trackEvent(Analytics.Category.COMIC,
                    Analytics.Action.COMIC_DOUBLE_TAP);
            return true;
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void performDoubleTapOnHoneycombOrAbove(boolean zoomOut, int duration, float from, float to) {
        final Animator animator;
        final Object target = ZoomImageView.this;

        final PropertyValuesHolder s = PropertyValuesHolder.ofFloat("scale", from, to);
        if (zoomOut) {
            final PropertyValuesHolder x = PropertyValuesHolder.ofFloat("translateX", mTranslateX, 0);
            final PropertyValuesHolder y = PropertyValuesHolder.ofFloat("translateY", mTranslateY, 0);
            animator = ObjectAnimator.ofPropertyValuesHolder(target, x, y, s);
        } else {
            animator = ObjectAnimator.ofPropertyValuesHolder(target, s);
        }
        if (null == mInterpolator) {
            mInterpolator = new DecelerateInterpolator();
        }
        animator.setInterpolator(mInterpolator);
        animator.setDuration(duration);

        animator.start();
    }

    private void setScale(float scale) {
        mScaleFactor = bound(scale, MIN_SCALE_FACTOR, MAX_SCALE_FACTOR);
        mIsZoomed = Float.compare(mScaleFactor, MIN_SCALE_FACTOR) != 0;
        invalidate();
    }

    // Used as ObjectAnimator helper.
    @SuppressWarnings("unused")
    private void setTranslateX(float x) {
        mTranslateX = x;
    }

    // Used as ObjectAnimator helper.
    @SuppressWarnings("unused")
    private void setTranslateY(float y) {
        mTranslateY = y;
    }

    private static float bound(float value, float min, float max) {
        final float bounded;
        if (value < min) {
            bounded = min;
        } else if (value > max) {
            bounded = max;
        } else {
            bounded = value;
        }
        return bounded;
    }

}
