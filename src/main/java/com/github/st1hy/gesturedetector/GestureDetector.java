package com.github.st1hy.gesturedetector;

import android.view.View;

public interface GestureDetector extends View.OnTouchListener {

    /**
     * Cancels any events that may be pending and resets detector.
     * <p/>
     * Use when you pause your activity to not receive any more events after pausing.
     */
    void invalidate();
}
