package com.trolmastercard.sexmod.util;

/**
 * Axis-aligned 2-D rectangle (double precision).
 * Ported from f2.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original field order: d=x1, a=y1, c=x2, b=y2.
 */
public class Rect2D {

    public double x1;
    public double y1;
    public double x2;
    public double y2;

    public Rect2D(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double width()  { return x2 - x1; }
    public double height() { return y2 - y1; }

    public boolean contains(double x, double y) {
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }
}
