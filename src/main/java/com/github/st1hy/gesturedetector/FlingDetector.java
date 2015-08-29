package com.github.st1hy.gesturedetector;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;


/**
 * Detects fling events.
 * <p/>
 * Calls {@link Listener#onFling(PointF, float, Direction)} when appropriate.
 * <p/>
 * {@link Options.Event#FLING} enables or disables this detector.
 */
public class FlingDetector implements GestureDetector {
    protected static final float MS_IN_SEC = 1e3f;
    protected final boolean enabled;
    protected final float flingThresholdPercent, flingVelocityThreshold;
    protected final Listener listener;
    protected float lastX, lastY;
    protected boolean isValid = false;
    protected float xThreshold = Float.NaN, yThreshold = Float.NaN;
    protected PointF startPoint;

    /**
     * Constructs new {@link FlingDetector}.
     *
     * @param listener Listener to be called when events happen.
     * @param options  Options for controlling behavior of this detector.
     * @throws NullPointerException if listener of options are null.
     */
    public FlingDetector(Listener listener, Options options) {
        if (listener == null) throw new NullPointerException("Listener cannot be null");
        if (options == null) throw new NullPointerException("Options cannot be null");
        this.listener = listener;
        this.enabled = options.isEnabled(Options.Event.FLING);
        this.flingThresholdPercent = options.get(Options.Constant.FLING_TRANSLATION_THRESHOLD) / 100f;
        this.flingVelocityThreshold = options.get(Options.Constant.FLING_VELOCITY_THRESHOLD);
    }

    public interface Listener {
        /**
         * Called when fling is detected. Require {@link Options.Event#FLING} to be enabled in {@link Options}.
         *
         * @param startPoint Point from which fling first started.
         * @param velocity   Detected velocity of fling in px / s.
         * @param direction  Direction of the fling.
         */
        void onFling(PointF startPoint, float velocity, Direction direction);
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    @Override
    public void invalidate() {
        isValid = false;
        xThreshold = Float.NaN;
        yThreshold = Float.NaN;
        lastX = 0;
        lastY = 0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!enabled) return false;
        switch (event.getActionMasked()) {
            case ACTION_DOWN:
                return onActionDown(v, event);
            case ACTION_UP:
                return onActionUp(event);
            case ACTION_POINTER_DOWN:
                return onActionPointerDown(event);
            case ACTION_POINTER_UP:
                return onActionPointerUp(event);
        }
        return false;
    }

    protected boolean onActionDown(View v, MotionEvent event) {
        isValid = true;
        lastX = event.getX();
        lastY = event.getY();
        startPoint = new PointF(lastX, lastY);
        xThreshold = flingThresholdPercent * v.getWidth();
        yThreshold = flingThresholdPercent * v.getHeight();
        return true;
    }

    protected boolean onActionUp(MotionEvent event) {
        if (!isValid) return false;
        long dt = event.getEventTime() - event.getDownTime(); //[ms]
        float x = event.getX(); // [px]
        float y = event.getY();
        float dx = x - lastX;
        float dy = y - lastY;
        float vx = MS_IN_SEC * dx / dt; // [ms / s * px / ms] = [px / s]
        float vy = MS_IN_SEC * dy / dt;
        float absVx = Math.abs(vx);
        float absVy = Math.abs(vy);
        if (absVx > absVy) {
            if (absVx > flingVelocityThreshold) {
                notifyListener(absVx, vx > 0 ? Direction.RIGHT : Direction.LEFT);
                return true;
            }
        } else {
            if (absVy > flingVelocityThreshold) {
                notifyListener(absVy, vy > 0 ? Direction.DOWN : Direction.UP);
                return true;
            }
        }
        float absDx = Math.abs(dx);
        float absDy = Math.abs(dy);
        if (absDx > absDy) {
            if (absDx > xThreshold) {
                notifyListener(absVx, dx > 0 ? Direction.RIGHT : Direction.LEFT);
            }
        } else {
            if (absDy > yThreshold) {
                notifyListener(absVy, dy > 0 ? Direction.DOWN : Direction.UP);
            }
        }
        return true;
    }

    protected boolean onActionPointerDown(MotionEvent event) {
        if (!isValid) return false;
        invalidate();
        return true;
    }

    protected boolean onActionPointerUp(MotionEvent event) {
        if (!isValid) return false;
        invalidate();
        return true;
    }

    protected void notifyListener(float velocity, Direction direction) {
        listener.onFling(startPoint, velocity, direction);
        invalidate();
    }
}
