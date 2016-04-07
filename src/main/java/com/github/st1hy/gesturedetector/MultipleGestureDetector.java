package com.github.st1hy.gesturedetector;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

/**
 * Listens for gesture events.
 *
 * Events are delivered through {@link MultipleGestureListener}. {@link Options} control which events will be triggered and various thresholds and settings.
 */
public class MultipleGestureDetector implements MultipleGestureListener, GestureDetector {
    protected static final boolean inDebug = Config.DEBUG;
    protected static final String TAG = "GestureDetector";
    protected final MultipleGestureListener listener;
    protected final ClickDetector clickDetector;
    protected final List<GestureDetector> detectors;

    /**
     * Create new gesture detector that detects multiple optional types of gestures.
     *
     * @param listener will receive calls about gestures.
     * @param options  describes detector parameters.
     * @throws NullPointerException when listener or options are null.
     */
    public MultipleGestureDetector(MultipleGestureListener listener, Options options) {
        if (listener == null || options == null) throw new NullPointerException();
        this.listener = listener;
        this.detectors = new LinkedList<>();
        this.clickDetector = new ClickDetector(this, options);
        this.detectors.add(clickDetector);
        if (options.isEnabled(Options.Event.TRANSLATE))
            this.detectors.add(new TranslationDetector(this, options));
        if (options.isEnabled(Options.Event.SCALE))
            this.detectors.add(new ScaleDetector(this, options));
        if (options.isEnabled(Options.Event.ROTATE))
            this.detectors.add(new RotationDetector(this, options));
        if (options.isEnabled(Options.Event.FLING))
            this.detectors.add(new FlingDetector(this, options));
        if (options.isEnabled(Options.Event.MATRIX_TRANSFORMATION))
            this.detectors.add(new MatrixTransformationDetector(this, options));
    }

    @Override
    public void invalidate() {
        for (GestureDetector detector : detectors) {
            detector.invalidate();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean isConsumed = false;
        for (GestureDetector detector : detectors) {
            isConsumed |= detector.onTouch(v, event);
        }
        return isConsumed;
    }

    @Override
    public void onTranslate(GestureEventState state, PointF startPoint, float x, float y, float dx, float dy, double distance) {
        if (inDebug) {
            Log.v(TAG, String.format("Translation %s: startX = %.1f, startY = %.1f, x = %.1f, y = %.1f, dx = %.2f, dy = %.2f, distance = %.2f", state.toString(), startPoint.x, startPoint.y, x, y, dx, dy, distance));
        }
        clickDetector.onExternalGesture();
        listener.onTranslate(state, startPoint, x, y, dx, dy, distance);
    }

    @Override
    public void onRotate(GestureEventState state, PointF centerPoint, double rotation, double delta) {
        if (inDebug) {
            Log.v(TAG, String.format("Rotation %s: x = %.1f, y = %.1f, rotation = %.1f, delta = %.2f", state.toString(), centerPoint.x, centerPoint.y, rotation, delta));
        }
        clickDetector.onExternalGesture();
        listener.onRotate(state, centerPoint, rotation, delta);
    }

    @Override
    public void onScale(GestureEventState state, PointF centerPoint, float scale, float scaleRelative) {
        if (inDebug) {
            Log.v(TAG, String.format("Scale %s: x = %.1f, y = %.1f, scale = %.2f, diffScale = %.3f", state.toString(), centerPoint.x, centerPoint.y, scale, scaleRelative));
        }
        clickDetector.onExternalGesture();
        listener.onScale(state, centerPoint, scale, scaleRelative);
    }

    @Override
    public void onClick(PointF startPoint) {
        if (inDebug) {
            Log.v(TAG, String.format("Clicked: x = %.1f, y = %.1f", startPoint.x, startPoint.y));
        }
        listener.onClick(startPoint);
    }

    @Override
    public void onLongPressed(PointF startPoint) {
        if (inDebug) {
            Log.v(TAG, String.format("Long pressed: x = %.1f, y = %.1f", startPoint.x, startPoint.y));
        }
        listener.onLongPressed(startPoint);
    }

    @Override
    public void onDoubleClick(PointF startPoint) {
        if (inDebug) {
            Log.v(TAG, String.format("Double clicked: x = %.1f, y = %.1f", startPoint.x, startPoint.y));
        }
        listener.onDoubleClick(startPoint);
    }

    @Override
    public void onFling(PointF startPoint, float velocity, FlingDetector.Direction direction) {
        if (inDebug) {
            Log.v(TAG, String.format("Fling %s: x = %.1f, y = %.1f, velocity = %.2f", direction.toString(), startPoint.x, startPoint.y, velocity));
        }
        clickDetector.onExternalGesture();
        listener.onFling(startPoint, velocity, direction);
    }

    @Override
    public void onMatrix(GestureEventState state, Matrix currentTransformation) {
        if (inDebug) {
            Log.v(TAG, String.format("Matrix %s: %s", state.toString(), currentTransformation.toShortString()));
        }
        clickDetector.onExternalGesture();
        listener.onMatrix(state, currentTransformation);
    }
}
