package com.github.st1hy.gesturedetector;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static com.github.st1hy.gesturedetector.Options.Constant.SCALE_START_THRESHOLD;

/**
 * Detects scaling events.
 * <p/>
 * Calls {@link Listener#onScale(GestureEventState, PointF, float, float)} when appropriate.
 * <p/>
 * {@link Options.Event#SCALE} enables or disables this detector.
 */
public class ScaleDetector implements GestureDetector {
    protected final boolean enabled;
    protected final int scaleThreshold;
    protected final Listener listener;
    protected final PointF centerPoint = new PointF();
    protected float scale, scaleRelative;
    protected double distanceStart;
    protected double currentDistance;
    protected boolean isEventValid = false;
    protected boolean inProgress = false;
    protected GestureEventState currentState = GestureEventState.ENDED;

    /**
     * Constructs new {@link ScaleDetector}.
     *
     * @param listener Listener to be called when events happen.
     * @param options  Options for controlling behavior of this detector.
     * @throws NullPointerException if listener of options are null.
     */
    public ScaleDetector(Listener listener, Options options) {
        this.listener = listener;
        this.enabled = options.isEnabled(Options.Event.SCALE);
        this.scaleThreshold = options.get(SCALE_START_THRESHOLD);
    }

    public interface Listener {
        /**
         * Called when scaling is detected. Only called when {@link Options.Event#SCALE} is set in {@link Options}.
         *
         * @param state         state of event. Can be either {@link GestureEventState#STARTED} when {@link Options.Constant#SCALE_START_THRESHOLD} is first reached, {@link GestureEventState#ENDED} when scaling ends or {@link GestureEventState#IN_PROGRESS}.
         * @param centerPoint   center of the gesture from the moment of event start.
         * @param scale         how much distance between points have grown since the beginning of this gesture
         * @param scaleRelative relative scale change since last call
         */
        void onScale(GestureEventState state, PointF centerPoint, float scale, float scaleRelative);
    }

    @Override
    public void invalidate() {
        isEventValid = false;
        inProgress = false;
        if (!GestureEventState.ENDED.equals(currentState)) {
            notifyListener(GestureEventState.ENDED);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!enabled) return false;
        switch (event.getActionMasked()) {
            case ACTION_DOWN:
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
        if (currentState != GestureEventState.ENDED) notifyListener(GestureEventState.ENDED);
        calculateCenter(event);
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
        if (currentState == GestureEventState.ENDED && (inProgress || currentDistance > scaleThreshold)) {
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
        listener.onScale(currentState, point, scale, scaleRelative);
    }

    protected void calculatePosition(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            inProgress = false;
            currentDistance = 0;
            return;
        }
        float centerX = 0;
        float centerY = 0;
        int pointsCount = event.getPointerCount();
        for (int i = 0; i < pointsCount; i++) {
            centerX += event.getX(i);
            centerY += event.getY(i);
        }
        centerX /= pointsCount;
        centerY /= pointsCount;

        double distance = 0;
        for (int i = 0; i < pointsCount; i++) {
            float dx = event.getX(i) - centerX;
            float dy = event.getY(i) - centerY;
            distance += distance(dx, dy);
        }
        currentDistance = distance / pointsCount;
        float scale = (float) (currentDistance / distanceStart);
        scaleRelative = scale / this.scale;
        this.scale = scale;
    }

    protected void calculateCenter(MotionEvent event) {
        calculateCenter(event, -1);
    }

    protected void calculateCenter(MotionEvent event, int discardPointerIndex) {
        float centerX = 0;
        float centerY = 0;
        int pointsCount = event.getPointerCount();
        for (int i = 0; i < pointsCount; i++) {
            if (discardPointerIndex == i) continue;
            centerX += event.getX(i);
            centerY += event.getY(i);
        }
        if (discardPointerIndex != -1) pointsCount -= 1;
        centerX /= pointsCount;
        centerY /= pointsCount;
        centerPoint.set(centerX, centerY);

        double distance = 0;
        pointsCount = event.getPointerCount();
        for (int i = 0; i < pointsCount; i++) {
            if (discardPointerIndex == i) continue;
            float dx = event.getX(i) - centerX;
            float dy = event.getY(i) - centerY;
            distance += distance(dx, dy);
        }
        if (discardPointerIndex != -1) pointsCount -= 1;
        scale = 1f;
        distanceStart = distance / pointsCount;
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
}