package com.github.st1hy.gesturedetector;

import android.graphics.PointF;

public class GeometryUtils {

    /**
     * Measures distance between 2 points.
     * @param point1 first point
     * @param point2 second point
     * @return distance between 2 points
     */
    public static double distance(PointF point1, PointF point2) {
        return hypotenuse(point1.x - point2.x, point1.y - point2.y);
    }

    /**
     * Measures distance between 2 points.
     * @param point1 fist point
     * @param x2 x coordinate of second point
     * @param y2 y coordinate of second point
     * @return distance between 2 points
     */
    public static double distance(PointF point1, float x2, float y2) {
        return hypotenuse(point1.x - x2, point1.y - y2);
    }

    /**
     * Measures distance between 2 points.
     * @param x1 x coordinate of first point
     * @param y1 y coordinate of first point
     * @param x2 x coordinate of second point
     * @param y2 y coordinate of second point
     * @return distance between 2 points
     */
    public static double distance(float x1, float y1, float x2, float y2) {
        return hypotenuse(x1 - x2, y1 - y2);
    }

    /**
     * Measures length of hypothenuse (longest side in triangle)
     * @param a length of the side next to the right angle in triangle
     * @param b length of the side next to the right angle in triangle, opposite to a
     * @return length of hypothenuse
     */
    public static double hypotenuse(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }
}
