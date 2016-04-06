package com.github.st1hy.gesturedetector;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;

import java.util.EnumMap;
import java.util.EnumSet;

import static com.github.st1hy.gesturedetector.Options.Constant.DOUBLE_CLICK_TIME_LIMIT;
import static com.github.st1hy.gesturedetector.Options.Constant.FLING_TRANSLATION_THRESHOLD;
import static com.github.st1hy.gesturedetector.Options.Constant.FLING_VELOCITY_THRESHOLD;
import static com.github.st1hy.gesturedetector.Options.Constant.LONG_PRESS_TIME_MS;
import static com.github.st1hy.gesturedetector.Options.Constant.MATRIX_MAX_POINTERS_COUNT;
import static com.github.st1hy.gesturedetector.Options.Constant.ROTATION_START_THRESHOLD;
import static com.github.st1hy.gesturedetector.Options.Constant.SCALE_START_THRESHOLD;
import static com.github.st1hy.gesturedetector.Options.Constant.TRANSLATION_START_THRESHOLD;
import static com.github.st1hy.gesturedetector.Options.Flag.MATRIX_OPEN_GL_COMPATIBILITY;
import static com.github.st1hy.gesturedetector.Options.Flag.TRANSLATION_STRICT_ONE_FINGER;

/**
 * Gesture detection options.
 *
 * By default all {@link Options.Event events} are enabled (with exception of {@link Event#DOUBLE_CLICK}) and all {@link Flag flags} except {@link Flag#TRANSLATION_STRICT_ONE_FINGER} and {@link Flag#MATRIX_OPEN_GL_COMPATIBILITY} are set.
 */
public class Options implements Cloneable {
    private EnumSet<Event> enabledEvents = EnumSet.noneOf(Event.class);
    private EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
    private EnumMap<Constant, Integer> constants = new EnumMap<>(Constant.class);

    public enum Event {
        SCALE, ROTATE, TRANSLATE, CLICK, DOUBLE_CLICK, LONG_PRESS, FLING, MATRIX_TRANSFORMATION
    }

    public enum Flag {
        /**
         * When detected other gestures don't trigger click event.
         */
        IGNORE_CLICK_EVENT_ON_GESTURES,
        /**
         * Restrict translation to using one finger only. When multitouch is detected end translations.
         */
        TRANSLATION_STRICT_ONE_FINGER,
        /**
         * Return rotation in degrees instead of radians
         */
        ROTATION_DEGREES,
        /**
         * When computing coordinates of events assume y-axis is oriented in opposite direction than whats received.
         * It computes new y position be subtracting current y value from view height.
         */
        MATRIX_OPEN_GL_COMPATIBILITY
    }

    public enum Constant {
        /**
         * How long user must press to detect a {@link Event#LONG_PRESS}, anything lower than this value will be considered a {@link Event#CLICK}
         * Hardcoded default: 500 ms
         */
        LONG_PRESS_TIME_MS(500),
        /**
         * Maximum break time between first press event and second release event to trigger {@link Event#DOUBLE_CLICK}
         * When {@link Event#DOUBLE_CLICK} is enabled this time will also delay {@link Event#CLICK}.
         * Hardcoded default: 400 ms
         */
        DOUBLE_CLICK_TIME_LIMIT(400),
        /**
         * Hardcoded default: 20 px
         */
        TRANSLATION_START_THRESHOLD(20),
        /**
         * Hardcoded default: 5 deg
         */
        ROTATION_START_THRESHOLD(5),
        /**
         * Hardcoded default: 20 px
         */
        SCALE_START_THRESHOLD(20),
        /**
         * Hardcoded default: 1000 px / s
         */
        FLING_VELOCITY_THRESHOLD(1000),
        /**
         * How much pointer have to move to consider this a fling.
         *
         * i.e value 50 means that pointer needs to move 50 % of the related view width or height depending on the fling.
         * Hardcoded default: 50 %
         */
        FLING_TRANSLATION_THRESHOLD(50),
        /**
         * Maximum number of pointers used to calculate transformation matrix.
         * Hardcoded default: 4
         */
        MATRIX_MAX_POINTERS_COUNT(4);
        private final int defaultValue;

        Constant(int defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    /**
     * Creates default Options using provided {@link Resources}.
     *
     * @param resources Resources providing default values.
     * @throws NullPointerException if resources are null.
     * @throws NotFoundException    if resource id of one of the used resources could not be found.
     */
    public Options(Resources resources) {
        if (resources == null) throw new NullPointerException("Resources cannot be null");
        enabledEvents.addAll(EnumSet.allOf(Event.class));
        enabledEvents.remove(Event.DOUBLE_CLICK);
        flags.addAll(EnumSet.allOf(Flag.class));
        flags.remove(TRANSLATION_STRICT_ONE_FINGER);
        flags.remove(MATRIX_OPEN_GL_COMPATIBILITY);

        for (Constant constant : Constant.values()) {
            constants.put(constant, constant.defaultValue);
        }
        constants.put(LONG_PRESS_TIME_MS, resources.getInteger(R.integer.gesture_detector_long_press_time_ms));
        constants.put(DOUBLE_CLICK_TIME_LIMIT, resources.getInteger(R.integer.gesture_detector_double_click_time_limit_ms));
        constants.put(ROTATION_START_THRESHOLD, resources.getInteger(R.integer.gesture_detector_rotation_start_threshold_deg));
        constants.put(TRANSLATION_START_THRESHOLD, resources.getDimensionPixelSize(R.dimen.gesture_detector_translation_start_threshold_distance));
        constants.put(SCALE_START_THRESHOLD, resources.getDimensionPixelSize(R.dimen.gesture_detector_scale_start_threshold_distance));
        constants.put(FLING_VELOCITY_THRESHOLD, resources.getDimensionPixelSize(R.dimen.gesture_detector_fling_threshold_velocity));
        constants.put(FLING_TRANSLATION_THRESHOLD, resources.getInteger(R.integer.gesture_detector_fling_translation_threshold_percent));
        constants.put(MATRIX_MAX_POINTERS_COUNT, resources.getInteger(R.integer.gesture_detector_matrix_transformation_pointers_max_count));
    }

    /**
     * Creates empty options.
     *
     * None of the {@link Event events} or {@link Flag flags} are set. Constants will return 0.
     */
    public Options() {
    }

    /**
     * Check is every one of this events is enabled
     *
     * @param event  First event to check
     * @param events Optional var-args of events to check. Can be null.
     * @return true if every event on the list is enabled
     * @throws NullPointerException if first event is null
     */
    public boolean isEnabled(Event event, Event... events) {
        if (event == null) throw new NullPointerException("Cannot get state of null event!");
        boolean isEnabled = enabledEvents.contains(event);
        if (events == null || !isEnabled) return isEnabled;
        for (Event e : events) {
            if (!enabledEvents.contains(e)) return false;
        }
        return true;
    }

    /**
     * Enable or disable event detection.
     *
     * @param event     Which event to set.
     * @param isEnabled Requested state of event.
     * @throws NullPointerException if event is null
     */
    public void setEnabled(Event event, boolean isEnabled) {
        if (event == null) throw new NullPointerException("Cannot change state of null event!");
        if (isEnabled) {
            enabledEvents.add(event);
        } else {
            enabledEvents.remove(event);
        }
    }

    /**
     * Get state of requested flags.
     *
     * @param flag  First flag it check.
     * @param flags Optional varargs of flags to check. Can be null.
     * @return true if all of the selected flags are enabled.
     * @throws NullPointerException if first flag is null
     */
    public boolean getFlag(Flag flag, Flag... flags) {
        if (flag == null) throw new NullPointerException("Cannot get state of null flag!");
        boolean isSet = this.flags.contains(flag);
        if (flags == null || !isSet) return isSet;
        for (Flag f : flags) {
            if (!this.flags.contains(f)) return false;
        }
        return true;
    }

    /**
     * Set state of flag.
     *
     * @param flag      Which flag state will be changed.
     * @param isEnabled Requested state of this flag.
     * @throws NullPointerException if flag is null.
     */
    public void setFlag(Flag flag, boolean isEnabled) {
        if (flag == null) throw new NullPointerException("Cannot set state of null flag!");
        if (isEnabled) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    /**
     * Get value of a constant.
     *
     * @param constant Which constant to get.
     * @return Value of the constant.
     * @throws NullPointerException if constant is null.
     */
    public int get(Constant constant) {
        if (constant == null) throw new NullPointerException("Cannot get value of null constant!");
        Integer value = constants.get(constant);
        return value != null ? value : 0;
    }

    /**
     * Sets new value to a constant.
     *
     * @param constant Which constant to set.
     * @param value    New value for the constant.
     * @throws NullPointerException if constant is null.
     */
    public void set(Constant constant, int value) {
        if (constant == null) throw new NullPointerException("Cannot set value of null constant!");
        constants.put(constant, value);
    }

    /**
     * Performs a deep copy of the options.
     *
     * @return Clone of this
     */
    public Options clone() {
        try {
            Object object = super.clone();
            Options clone = getClass().cast(object);
            clone.enabledEvents = EnumSet.copyOf(enabledEvents);
            clone.flags = EnumSet.copyOf(flags);
            clone.constants = new EnumMap<>(constants);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
