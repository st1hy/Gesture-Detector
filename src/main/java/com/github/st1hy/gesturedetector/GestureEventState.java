package com.github.st1hy.gesturedetector;

/**
 * Specifies current state of event.
 */
public enum GestureEventState {
    /**
     * New gesture started.
     */
    STARTED,
    /**
     * Gesture state has changed.
     */
    IN_PROGRESS,
    /**
     * This gesture ended. No more events will be received until new gesture starts.
     */
    ENDED
}
