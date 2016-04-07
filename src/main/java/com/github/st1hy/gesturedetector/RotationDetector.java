package com.github.st1hy.gesturedetector;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static com.github.st1hy.gesturedetector.Options.Constant.ROTATION_START_THRESHOLD;

/**
 * Provides rotation events to the {@link Listener}.
 *
 * Calls {@link Listener#onRotate(GestureEventState, PointF, double, double)} when appropriate.
 *
 * {@link Options.Event#TRANSLATE} enables or disables this detector.
 */
public class RotationDetector implements GestureDetector {
    protected final boolean enabled, degreesEnabled, openGLCompat;
    protected final double rotationThresholdRad;
    protected final Listener listener;
    protected final PointF centerPoint = new PointF();
    protected double rotation;
    protected double previousAngle;
    protected double deltaRotation;
    protected boolean isEventValid = false;
    protected boolean inProgress = false;
    protected int maxPointersCount = 2;
    protected GestureEventState currentState = GestureEventState.ENDED;

    protected int height;

    /**
     * Constructs new {@link RotationDetector}.
     *
     * @param listener Listener to be called when events happen.
     * @param options  Options for controlling behavior of this detector.
     * @throws NullPointerException if listener of options are null.
     */
    public RotationDetector(Listener listener, Options options) {
        if (listener == null) throw new NullPointerException("Listener cannot be null");
        if (options == null) throw new NullPointerException("Options cannot be null");
        this.listener = listener;
        this.enabled = options.isEnabled(Options.Event.ROTATE);
        this.rotationThresholdRad = Math.toRadians(options.get(ROTATION_START_THRESHOLD));
        this.degreesEnabled = options.getFlag(Options.Flag.ROTATION_DEGREES);
        this.openGLCompat = options.getFlag(Options.Flag.MATRIX_OPEN_GL_COMPATIBILITY);
    }

    public interface Listener {
        /**
         * Called when rotation is detected. Only received when {@link Options.Event#ROTATE} is set in {@link Options}.
         *
         * Rotation is returned in radians unless {@link Options.Flag#ROTATION_DEGREES} is enabled.
         *
         * @param state       state of event. Can be either {@link GestureEventState#STARTED} when {@link Options.Constant#ROTATION_START_THRESHOLD} is first reached, {@link GestureEventState#ENDED} when rotation ends or {@link GestureEventState#IN_PROGRESS}.
         * @param centerPoint point of reference for rotation
         * @param rotation    rotation in degrees since the beginning of the gesture.
         * @param delta       Relative rotation since the last call of onRotate.
         */
        void onRotate(GestureEventState state, PointF centerPoint, double rotation, double delta);
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
        calculateRotation(event);
        if (!isEventValid) return false;
        if (currentState == GestureEventState.ENDED && (inProgress || Math.abs(rotation) > rotationThresholdRad)) {
            inProgress = true;
            notifyListener(GestureEventState.STARTED);
        } else if (currentState != GestureEventState.ENDED) {
            notifyListener(GestureEventState.IN_PROGRESS);
        }
        return true;
    }

    protected void notifyListener(GestureEventState state) {
        if (degreesEnabled) {
            notifyListener(state, Math.toDegrees(rotation), Math.toDegrees(deltaRotation));
        } else {
            notifyListener(state, rotation, deltaRotation);
        }
    }

    protected void notifyListener(GestureEventState state, double rotation, double deltaRotation) {
        currentState = state;
        listener.onRotate(currentState, centerPoint, rotation, deltaRotation);
    }

    protected void calculateRotation(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            inProgress = false;
            rotation = 0;
            deltaRotation = 0;
            return;
        }
        float centerX = 0;
        float centerY = 0;
        int pointsCount = event.getPointerCount();
        if (pointsCount > maxPointersCount) pointsCount = maxPointersCount;
        for (int i = 0; i < pointsCount; i++) {
            centerX += event.getX(i);
            centerY += getValueOfY(event, i);
        }
        centerX /= pointsCount;
        centerY /= pointsCount;
        centerPoint.set(centerX, centerY);

        double angleSum = 0;
        for (int i = 0; i < pointsCount; i++) {
            float dx = event.getX(i) - centerX;
            float dy = getValueOfY(event, i) - centerY;
            float tan = dy / dx;
            double rad = Math.atan(tan);
            angleSum += rad;
        }
        angleSum /= pointsCount;
        deltaRotation = angleSum - this.previousAngle;
        this.previousAngle = angleSum;
        if (Math.PI - Math.abs(deltaRotation) < 0.5d) {
            //We are passing through the "danger zone"
            deltaRotation = Math.PI - deltaRotation;
        }
        rotation += deltaRotation;
    }

    protected void calculateCenter(MotionEvent event) {
        calculateCenter(event, -1);
    }

    protected void calculateCenter(MotionEvent event, int discardPointerIndex ) {
        float centerX = 0;
        float centerY = 0;
        final int pointsCount = event.getPointerCount();
        {
            int pointersAdded = 0;
            for (int i = 0; i < pointsCount; i++) {
                if (discardPointerIndex == i) continue;
                else if (pointersAdded == maxPointersCount) break;
                centerX += event.getX(i);
                centerY += getValueOfY(event, i);
                pointersAdded++;
            }
            centerX /= pointersAdded;
            centerY /= pointersAdded;
        }
        centerPoint.set(centerX, centerY);

        double angleSum = 0;
        int pointersAdded = 0;
        for (int i = 0; i < pointsCount; i++) {
            if (discardPointerIndex == i) continue;
            else if (pointersAdded == maxPointersCount) break;
            float dx = event.getX(i) - centerX;
            float dy = getValueOfY(event, i) - centerY;
            float tan = dy / dx;
            angleSum += Math.atan(tan);
            pointersAdded++;
        }
        this.previousAngle = angleSum / pointersAdded;
        rotation = 0;
        deltaRotation = 0;
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
