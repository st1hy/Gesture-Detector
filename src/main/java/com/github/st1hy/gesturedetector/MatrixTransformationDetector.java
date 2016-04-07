package com.github.st1hy.gesturedetector;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Calculates transformation matrix based on user touch input.
 *
 * Internally it uses {@link Matrix#setPolyToPoly(float[], int, float[], int, int)} with start points matching pointers when event started and ending points of the current pointers from {@link MotionEvent}.
 */
public class MatrixTransformationDetector implements GestureDetector {
    protected final boolean enabled, openGLCompatibility;
    protected final int maxPointersCount;
    protected final Listener listener;
    protected boolean isEventValid = false;
    protected GestureEventState currentState = GestureEventState.ENDED;
    /**
     * Indexed by pointer id.
     */
    protected final SparseArray<PointF> startPoints = new SparseArray<>();
    protected final Matrix matrix = new Matrix();
    protected final float[] poly = new float[16];
    protected final int translationThreshold;
    protected int height;

    /**
     * Created transformation matrix detector with default values.
     *
     * @param listener Event listener that will receive computed matrix
     * @throws NullPointerException if listener is null.
     */
    public MatrixTransformationDetector(Listener listener) {
        if (listener == null) throw new NullPointerException("Listener cannot be null");
        this.listener = listener;
        this.maxPointersCount = 4;
        this.enabled = true;
        this.openGLCompatibility = false;
        this.translationThreshold = 0;
    }

    /**
     * Created transformation matrix detector.
     *
     * @param listener         Event listener that will receive computed matrix
     * @param maxPointersCount Maximum pointers used to calculate transformation matrix.
     * @throws NullPointerException     if listener is null.
     * @throws IllegalArgumentException if maxPointersCount is greater than 4
     */
    public MatrixTransformationDetector(Listener listener, int maxPointersCount) {
        if (listener == null) throw new NullPointerException("Listener cannot be null");
        if (maxPointersCount > 4)
            throw new IllegalArgumentException("Maximum pointers count cannot exceed 4");
        this.listener = listener;
        this.maxPointersCount = maxPointersCount;
        this.enabled = true;
        this.openGLCompatibility = false;
        this.translationThreshold = 0;
    }

    /**
     * Created transformation matrix detector.
     *
     * @param listener Listener to be called when events happen.
     * @param options  Options for controlling behavior of this detector.
     * @throws NullPointerException if listener of options are null.
     * @throws IllegalArgumentException if {@link Options.Constant#MATRIX_MAX_POINTERS_COUNT} has greater value than 4
     */
    public MatrixTransformationDetector(Listener listener, Options options) {
        if (listener == null) throw new NullPointerException("Listener cannot be null");
        if (options == null) throw new NullPointerException("Options cannot be null");
        this.listener = listener;
        this.maxPointersCount = options.get(Options.Constant.MATRIX_MAX_POINTERS_COUNT);
        if (maxPointersCount > 4)
            throw new IllegalArgumentException("Maximum pointers count cannot exceed 4");
        this.enabled = options.isEnabled(Options.Event.MATRIX_TRANSFORMATION);
        this.openGLCompatibility = options.getFlag(Options.Flag.MATRIX_OPEN_GL_COMPATIBILITY);
        this.translationThreshold = options.get(Options.Constant.TRANSLATION_START_THRESHOLD);
    }

    @Override
    public void invalidate() {
        isEventValid = false;
        if (!GestureEventState.ENDED.equals(currentState)) {
            notifyListener(GestureEventState.ENDED);
        }
    }

    public interface Listener {
        /**
         * Called when matrix transformation is detected.
         *
         * @param state                 Current state of event.
         * @param currentTransformation Current matrix transformation. Its computed from the beginning of the gesture up to this point. Absolute values.
         */
        void onMatrix(GestureEventState state, Matrix currentTransformation);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!enabled) return false;
        height = v.getHeight();
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
        setStartPoints(event);
        isEventValid = true;
        return true;
    }

    protected boolean onActionPointerDown(MotionEvent event) {
        if (!isEventValid) return false;
        if (currentState != GestureEventState.ENDED) notifyListener(GestureEventState.ENDED);
        setStartPoints(event);
        return true;
    }

    protected boolean onActionUp(MotionEvent event) {
        if (!isEventValid) return false;
        invalidate();
        return true;
    }

    protected boolean onActionMove(MotionEvent event) {
        if (!isEventValid) return false;
        computeMatrix(event);
        if (!isEventValid) return false;
        if (currentState == GestureEventState.ENDED) {
            notifyListener(GestureEventState.STARTED);
        } else {
            notifyListener(GestureEventState.IN_PROGRESS);
        }
        return true;
    }

    protected void notifyListener(GestureEventState state) {
        currentState = state;
        listener.onMatrix(currentState, matrix);
    }

    protected void computeMatrix(MotionEvent event) {
        int pointsCount = event.getPointerCount();
        if (pointsCount > maxPointersCount) {
            pointsCount = maxPointersCount;
        }
        int polySize = pointsCount * 2;
        for (int i = 0; i < pointsCount; i++) {
            int pointerId = event.getPointerId(i);
            PointF src = startPoints.get(pointerId);
            int xIndex = i * 2;
            int yIndex = xIndex + 1;
            poly[xIndex] = src.x;
            poly[yIndex] = openGLCompatibility ? height - src.y : src.y;
            poly[xIndex + polySize] = event.getX(i);
            poly[yIndex + polySize] = openGLCompatibility ? height - event.getY(i) : event.getY(i);
        }
        matrix.setPolyToPoly(poly, 0, poly, polySize, pointsCount);
    }

    protected void setStartPoints(MotionEvent event) {
        int pointsCount = event.getPointerCount();
        for (int i = 0; i < pointsCount; i++) {
            int pointerId = event.getPointerId(i);
            setStartPoint(pointerId, event.getX(i), event.getY(i));
        }
    }

    protected void setStartPoint(int pointerID, float x, float y) {
        PointF pointF = startPoints.get(pointerID);
        if (pointF == null) {
            pointF = new PointF(x, y);
            startPoints.put(pointerID, pointF);
        } else {
            pointF.set(x, y);
        }
    }

    protected boolean onActionPointerUp(MotionEvent event) {
        if (!isEventValid) return false;
        if (currentState != GestureEventState.ENDED) notifyListener(GestureEventState.ENDED);
        setStartPoints(event);
        return true;
    }
}
