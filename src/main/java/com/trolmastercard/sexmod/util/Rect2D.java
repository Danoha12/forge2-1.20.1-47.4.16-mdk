package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades matemáticas

/**
 * Rect2D — Portado a 1.20.1.
 * * Rectángulo 2D alineado a los ejes (doble precisión).
 * * Utilizado probablemente para la detección de clics en tus interfaces gráficas (GUIs).
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

    public double width() {
        return this.x2 - this.x1;
    }

    public double height() {
        return this.y2 - this.y1;
    }

    public boolean contains(double x, double y) {
        return x >= this.x1 && x <= this.x2 && y >= this.y1 && y <= this.y2;
    }
}