package com.github.st1hy.gesturedetector;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static com.github.st1hy.gesturedetector.Options.Constant.TRANSLATION_START_THRESHOLD;
import static com.github.st1hy.gesturedetector.Options.Flag.MATRIX_OPEN_GL_COMPATIBILITY;
import static com.github.st1hy.gesturedetector.Options.Flag.TRANSLATION_STRICT_ONE_FINGER;

/**
 * Detects translation events.
 *
 * Calls {@link Listener#onTranslate(GestureEventState, PointF, float, float, float, float, double)} when appropriate.
 *
 * {@link Options.Event#TRANSLATE} enables or disables this detector.
 */
public class TranslationDetector implements GestureDetector {
    protected final boolean enabled, strictOneFinger, openGLCompat;
    protected final int translationThreshold;
    protected final Listener listener;
    protected final PointF centerPoint = new PointF();
    protected float x, dx;
    protected float y, dy;
    protected double currentDistance;
    protected boolean isEventValid = false;
    protected boolean inProgress = false;
    protected GestureEventState currentState = GestureEventState.ENDED;
    protected int height;

    /**
     * Constructs new {@link TranslationDetector}.
     *
     * @param listener Listener to be called when events happen.
     * @param options  Options for controlling behavior of this detector.
     * @throws NullPointerException if listener of options are null.
     */
    public TranslationDetector(Listener listener, Options options) {
        if (listener == null) throw new NullPointerException("Listener cannot be null");
        if (options == null) throw new NullPointerException("Options cannot be null");
        this.listener = listener;
        this.enabled = options.isEnabled(Options.Event.TRANSLATE);
        this.strictOneFinger = options.getFlag(TRANSLATION_STRICT_ONE_FINGER);
        this.translationThreshold = options.get(TRANSLATION_START_THRESHOLD);
        this.openGLCompat = options.getFlag(MATRIX_OPEN_GL_COMPATIBILITY);
    }

    public interface Listener {
        /**
         * Is called when translation occurs. Require {@link Options.Event#TRANSLATE} to be set in {@link Options}.
         *
         * @param state      state of event. Can be either {@link GestureEventState#STARTED} when {@link Options.Constant#TRANSLATION_START_THRESHOLD} is first reached, {@link GestureEventState#ENDED} when translation ends or {@link GestureEventState#IN_PROGRESS}.
         * @param startPoint position on the finger when it was first pressed
         * @param x          initial translation on x axis when event was triggered
         * @param y          initial translation on y axis when event was triggered
         * @param dx         relative translations since last call on x axis.
         * @param dy         relative translation since last call on y axis
         * @param distance   distance between start point and current point that triggered translation. Must be above {@link Options.Constant#TRANSLATION_START_THRESHOLD}.
         */
        void onTranslate(GestureEventState state, PointF startPoint, float x, float y, float dx, float dy, double distance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidate() {
        isEventValid = false;
        inProgress = false;
        if (!GestureEventState.ENDED.equals(currentState)) {
            notifyListener(GestureEventState.ENDED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!enabled) return false;
        switch (event.getActionMasked()) {
            case ACTION_DOWN:
                height = v.getHeight();
                return onActionDown(event);
            case ACTION_UP:
                return onActionUp(event);
            case ACTION_MOVE:
                return onActionMove(event);
            case ACTION_POINTER_DOWN:
                return onActionPointerDown(event);
            case ACTION_POINTER_UP:
                return onActionPointerUp(event);
        }
        return false;
    }

    protected boolean onActionDown(MotionEvent event) {
        calculateCenter(event);
        isEventValid = true;
        return true;
    }

    protected boolean onActionPointerDown(MotionEvent event) {
        if (!isEventValid) return false;
        if (strictOneFinger) {
            invalidate();
        } else {
            if (currentState != GestureEventState.ENDED) notifyListener(GestureEventState.ENDED);
            calculateCenter(event);
        }
        return true;
    }

    protected boolean onActionUp(MotionEvent event) {
        if (!isEventValid) return false;
        invalidate();
        return true;
    }

    protected boolean onActionMove(MotionEvent event) {
        if (!isEventValid) return false;
        calculatePosition(event);
        if (!isEventValid) return false;
        if (currentState == GestureEventState.ENDED && (inProgress || currentDistance > translationThreshold)) {
            inProgress = true;
            notifyListener(GestureEventState.STARTED);
        } else if (currentState != GestureEventState.ENDED) {
            notifyListener(GestureEventState.IN_PROGRESS);
        }
        return true;
    }

    protected void notifyListener(GestureEventState state) {
        currentState = state;
        PointF point = new PointF();
        point.set(centerPoint);
        listener.onTranslate(state, point, x, y, dx, dy, currentDistance);
    }

    protected void calculatePosition(MotionEvent event) {
        float centerX = 0;
        float centerY = 0;
        int pointsCount = event.getPointerCount();
        for (int i = 0; i < pointsCount; i++) {
            centerX += event.getX(i);
            centerY += getValueOfY(event, i);
        }
        centerX /= pointsCount;
        centerY /= pointsCount;

        float x = centerX - centerPoint.x;
        float y = centerY - centerPoint.y;
        currentDistance = distance(x, y);
        dx = x - this.x;
        dy = y - this.y;
        this.x = x;
        this.y = y;
    }

    protected void calculateCenter(MotionEvent event) {
        calculateCenter(event, -1);
    }

    protected void calculateCenter(MotionEvent event, int discardPointerIndex) {
        float centerX = 0;
        float centerY = 0;
        int pointsCount = event.getPointerCount();
        for (int i = 0; i < pointsCount; i++) {
            if (i == discardPointerIndex) continue;
            centerX += event.getX(i);
            centerY += getValueOfY(event, i);
        }
        if (discardPointerIndex != -1) pointsCount -= 1;
        centerX /= pointsCount;
        centerY /= pointsCount;
        x = 0;
        y = 0;
        centerPoint.set(centerX, centerY);
    }

    protected static double distance(float a, float b) {
        return Math.sqrt(a * a + b * b);
    }

    protected boolean onActionPointerUp(MotionEvent event) {
        if (!isEventValid) return false;
        if (currentState != GestureEventState.ENDED) notifyListener(GestureEventState.ENDED);
        calculateCenter(event, event.getActionIndex());
        return true;
    }

    private float getValueOfY(MotionEvent event, int pointerIndex) {
        float y = event.getY(pointerIndex);
        return openGLCompat ? height - y : y;
    }
}