package com.github.st1hy.gesturedetector;

import android.graphics.Matrix;
import android.graphics.PointF;

public class SimpleGestureListener implements MultipleGestureListener {
    @Override
    public void onTranslate(GestureEventState state, PointF startPoint, float x, float y, float dx, float dy, double distance) {

    }

    @Override
    public void onRotate(GestureEventState state, PointF centerPoint, double rotation, double delta) {

    }

    @Override
    public void onScale(GestureEventState state, PointF centerPoint, float scale, float scaleRelative) {

    }

    @Override
    public void onClick(PointF startPoint) {

    }

    @Override
    public void onLongPressed(PointF startPoint) {

    }

    @Override
    public void onDoubleClick(PointF startPoint) {

    }

    @Override
    public void onFling(PointF startPoint, float velocity, FlingDetector.Direction direction) {

    }

    @Override
    public void onMatrix(GestureEventState state, Matrix currentTransformation) {

    }
}
