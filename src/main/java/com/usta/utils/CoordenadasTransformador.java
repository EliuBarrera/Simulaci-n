package com.usta.utils;

/**
 * Transforma coordenadas entre el sistema lógico (unidades del plano)
 * y el sistema de pantalla (px).
 *
 * ── Modelo de Nodo ──
 *   Nodo.x, Nodo.y, Nodo.z siempre almacenan coordenadas LÓGICAS
 *   (unidades del plano), NO píxeles.
 *
 * ── 2D ──
 *   screenX = MARGIN + logX × PX_POR_UNIT
 *   screenY = (canvasHeight − MARGIN) − logY × PX_POR_UNIT
 *
 * ── 3D isométrico ──
 *   Parametrizado por dos ángulos (alpha, beta) que permiten
 *   rotar la vista. Por defecto ambos son 30°.
 *   X axis → derecha-abajo  (ángulo alpha)
 *   Z axis → izquierda-abajo (ángulo beta)
 *   Y axis → vertical arriba
 */
public class CoordenadasTransformador {

    private static final double MARGIN      = 40.0;
    private static final double PX_POR_UNIT = 100.0;

    private final double canvasHeight;
    private final double canvasWidth;
    private final UnidadDistancia unidad;

    // Ángulos de proyección 3D (en radianes)
    private final double alphaRad;   // ángulo del eje X desde la horizontal
    private final double betaRad;    // ángulo del eje Z desde la horizontal
    private final double cosAlpha, sinAlpha, cosBeta, sinBeta;

    // ── Constructores ────────────────────────────────────────────────────

    /** Constructor 2D (retrocompatible). */
    public CoordenadasTransformador(double canvasHeight, UnidadDistancia unidad) {
        this(canvasHeight, 1500, unidad, 30.0, 30.0);
    }

    /** Constructor 3D con ángulos de rotación. */
    public CoordenadasTransformador(double canvasHeight, double canvasWidth,
                                     UnidadDistancia unidad,
                                     double alphaDeg, double betaDeg) {
        this.canvasHeight = canvasHeight;
        this.canvasWidth  = canvasWidth;
        this.unidad       = unidad;
        this.alphaRad     = Math.toRadians(alphaDeg);
        this.betaRad      = Math.toRadians(betaDeg);
        this.cosAlpha     = Math.cos(alphaRad);
        this.sinAlpha     = Math.sin(alphaRad);
        this.cosBeta      = Math.cos(betaRad);
        this.sinBeta      = Math.sin(betaRad);
    }

    // ── 2D: lógico → pantalla ────────────────────────────────────────────

    public double unidadXToPx(double ux) {
        return MARGIN + ux * PX_POR_UNIT;
    }

    public double unidadYToPx(double uy) {
        return (canvasHeight - MARGIN) - uy * PX_POR_UNIT;
    }

    // ── 2D: pantalla → lógico ────────────────────────────────────────────

    public double pxXToUnidad(double pxX) {
        return (pxX - MARGIN) / PX_POR_UNIT;
    }

    public double pxYToUnidad(double pxY) {
        return ((canvasHeight - MARGIN) - pxY) / PX_POR_UNIT;
    }

    // ── 3D isométrico: lógico → pantalla ─────────────────────────────────

    /** Origen X de la proyección 3D. */
    public double getOriginX3D() {
        return MARGIN + 5 * PX_POR_UNIT * cosBeta;
    }

    /** Origen Y de la proyección 3D. */
    public double getOriginY3D() {
        return canvasHeight - MARGIN - 0.5 * PX_POR_UNIT;
    }

    public double isoXToPx(double ux, double uy, double uz) {
        return getOriginX3D() + (ux * cosAlpha - uz * cosBeta) * PX_POR_UNIT;
    }

    public double isoYToPx(double ux, double uy, double uz) {
        return getOriginY3D() - uy * PX_POR_UNIT
             - (ux * sinAlpha + uz * sinBeta) * PX_POR_UNIT;
    }

    // ── 3D: pantalla → lógico (dada Z fija) ──────────────────────────────

    /**
     * Dado un punto de pantalla y una Z fija, calcula las coordenadas
     * lógicas X, Y correspondientes.
     *
     * @return double[]{logX, logY}
     */
    public double[] screenToLogical3D(double screenX, double screenY, double fixedZ) {
        double originX = getOriginX3D();
        double originY = getOriginY3D();

        // De isoXToPx: screenX = originX + (logX * cosAlpha - fixedZ * cosBeta) * S
        double logX = ((screenX - originX) / PX_POR_UNIT + fixedZ * cosBeta) / cosAlpha;

        // De isoYToPx: screenY = originY - logY * S - (logX * sinAlpha + fixedZ * sinBeta) * S
        double logY = (originY - screenY) / PX_POR_UNIT
                    - logX * sinAlpha - fixedZ * sinBeta;

        return new double[]{ logX, logY };
    }

    // ── Conveniencia: lógico → pantalla (ambos modos) ────────────────────

    /**
     * Convierte coordenadas lógicas a pantalla según el modo.
     *
     * @return double[]{screenX, screenY}
     */
    public double[] logicalToScreen(double logX, double logY, double logZ, boolean modo3D) {
        if (modo3D) {
            return new double[]{ isoXToPx(logX, logY, logZ),
                                 isoYToPx(logX, logY, logZ) };
        } else {
            return new double[]{ unidadXToPx(logX), unidadYToPx(logY) };
        }
    }

    /**
     * Convierte un punto de pantalla a coordenadas lógicas según el modo.
     * En 3D se necesita la Z fija del nodo.
     *
     * @return double[]{logX, logY}
     */
    public double[] screenToLogical(double screenX, double screenY,
                                     double fixedZ, boolean modo3D) {
        if (modo3D) {
            return screenToLogical3D(screenX, screenY, fixedZ);
        } else {
            return new double[]{ pxXToUnidad(screenX), pxYToUnidad(screenY) };
        }
    }

    // ── Conversión de unidades a metros ──────────────────────────────────

    public double toMetros(double valorEnUnidad) {
        return unidad.convertirAMetros(valorEnUnidad);
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public UnidadDistancia getUnidad()   { return unidad; }
    public double getCanvasHeight()      { return canvasHeight; }
    public double getCanvasWidth()       { return canvasWidth; }
    public double getMargin()            { return MARGIN; }
    public double getPxPorUnidad()       { return PX_POR_UNIT; }
    public double getAlphaDeg()          { return Math.toDegrees(alphaRad); }
    public double getBetaDeg()           { return Math.toDegrees(betaRad); }
    public double getCosAlpha()          { return cosAlpha; }
    public double getSinAlpha()          { return sinAlpha; }
    public double getCosBeta()           { return cosBeta; }
    public double getSinBeta()           { return sinBeta; }

    // Mantener compatibilidad con getCosIso / getSinIso (usan alpha)
    public static double getCosIso()     { return Math.cos(Math.toRadians(30)); }
    public static double getSinIso()     { return Math.sin(Math.toRadians(30)); }
}