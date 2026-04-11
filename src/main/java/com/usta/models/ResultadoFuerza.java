package com.usta.models;

/**
 * Encapsula el resultado del cálculo de fuerza eléctrica
 * entre dos partículas según la Ley de Coulomb.
 *
 * Las componentes Fx, Fy (y Fz en 3D) están expresadas en el sistema
 * matemático estándar (Y crece hacia arriba, Z hacia el observador).
 */
public class ResultadoFuerza {

    private final Nodo particulaCausante;

    // Coordenadas de ambas partículas en unidades reales (sistema matemático)
    private final double x0; // X de la partícula de análisis (unidades reales)
    private final double y0; // Y de la partícula de análisis (unidades reales)
    private final double z0; // Z de la partícula de análisis (unidades reales, 3D)
    private final double x1; // X de la partícula causante (unidades reales)
    private final double y1; // Y de la partícula causante (unidades reales)
    private final double z1; // Z de la partícula causante (unidades reales, 3D)

    // Vector diferencia en unidades reales
    private final double dx;
    private final double dy;
    private final double dz;

    // Distancia
    private final double distanciaEnUnidad; // en la unidad seleccionada
    private final double distanciaEnMetros; // en metros

    // Ángulo en grados [0, 360) respecto al eje X+, sentido antihorario (plano XY)
    private final double anguloDeg;
    // Ángulo de elevación en grados [-90, 90] respecto al plano XY (solo 3D)
    private final double anguloElevacionDeg;

    // Magnitud de la fuerza escalar (siempre positiva)
    private final double magnitud;

    // Componentes con signo (atracción/repulsión ya aplicado)
    private final double fx;
    private final double fy;
    private final double fz;

    // Tipo de interacción
    private final boolean esRepulsion;

    /**
     * Constructor 2D — retrocompatible (z0=0, z1=0, dz=0, fz=0).
     */
    public ResultadoFuerza(Nodo particulaCausante,
            double x0, double y0,
            double x1, double y1,
            double dx, double dy,
            double distanciaEnUnidad, double distanciaEnMetros,
            double magnitud, double fx, double fy,
            boolean esRepulsion) {
        this(particulaCausante,
                x0, y0, 0,
                x1, y1, 0,
                dx, dy, 0,
                distanciaEnUnidad, distanciaEnMetros,
                magnitud, fx, fy, 0,
                esRepulsion);
    }

    /**
     * Constructor 3D completo.
     */
    public ResultadoFuerza(Nodo particulaCausante,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double dx, double dy, double dz,
            double distanciaEnUnidad, double distanciaEnMetros,
            double magnitud, double fx, double fy, double fz,
            boolean esRepulsion) {
        this.particulaCausante = particulaCausante;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.distanciaEnUnidad = distanciaEnUnidad;
        this.distanciaEnMetros = distanciaEnMetros;
        this.magnitud = magnitud;
        this.fx = fx;
        this.fy = fy;
        this.fz = fz;
        this.esRepulsion = esRepulsion;

        // Ángulo azimutal (plano XY), normalizado a [0, 360)
        double ang = Math.toDegrees(Math.atan2(dy, dx));
        if (ang < 0)
            ang += 360.0;
        this.anguloDeg = ang;

        // Ángulo de elevación (desde plano XY hacia Z)
        double distXY = Math.hypot(dx, dy);
        this.anguloElevacionDeg = Math.toDegrees(Math.atan2(dz, distXY));
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Nodo getParticulaCausante() {
        return particulaCausante;
    }

    public double getX0() {
        return x0;
    }

    public double getY0() {
        return y0;
    }

    public double getZ0() {
        return z0;
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getZ1() {
        return z1;
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    public double getDz() {
        return dz;
    }

    public double getDistanciaEnUnidad() {
        return distanciaEnUnidad;
    }

    public double getDistanciaEnMetros() {
        return distanciaEnMetros;
    }

    public double getAnguloDeg() {
        return anguloDeg;
    }

    public double getAnguloElevacionDeg() {
        return anguloElevacionDeg;
    }

    public double getMagnitud() {
        return magnitud;
    }

    public double getFx() {
        return fx;
    }

    public double getFy() {
        return fy;
    }

    public double getFz() {
        return fz;
    }

    public boolean isEsRepulsion() {
        return esRepulsion;
    }

    public String getTipoInteraccion() {
        return esRepulsion ? "REPULSION (mismo signo)" : "ATRACCION (signos opuestos)";
    }
}