package com.usta.models;

/**
 * Representa una figura geométrica en el plano del simulador de Gauss.
 * Puede ser una figura cargada o una superficie gaussiana.
 *
 * Las coordenadas (cx, cy) son el centro de la figura en píxeles del canvas.
 * Los parámetros (param1, param2) dependen del tipo:
 *   CIRCULO     → param1 = radio en px
 *   RECTANGULO  → param1 = ancho/2, param2 = alto/2
 *   CUADRADO    → param1 = lado/2
 *   TRIANGULO   → param1 = base/2, param2 = altura
 */
public class FiguraGauss {

    private TipoFigura tipo;
    private double cx, cy;       // centro en píxeles
    private double param1;       // radio / semiancho / semilado / semibase
    private double param2;       // semialto / altura (para rect y tri)

    // Solo para figuras cargadas
    private double cargaTotal;   // en µC
    private String signo;        // "+" o "-"
    private String nombre;

    public FiguraGauss(TipoFigura tipo, double cx, double cy,
                        double param1, double param2,
                        double cargaTotal, String signo, String nombre) {
        this.tipo       = tipo;
        this.cx         = cx;
        this.cy         = cy;
        this.param1     = param1;
        this.param2     = param2;
        this.cargaTotal = cargaTotal;
        this.signo      = signo;
        this.nombre     = nombre;
    }

    // ── Geometría ────────────────────────────────────────────────────────────

    /**
     * Retorna el área de la figura en unidades reales (unidad²).
     * Se usa solo para referencia; el flujo se calcula con Q_int / ε₀.
     */
    public double calcularArea(double pxPorUnidad) {
        return switch (tipo) {
            case CIRCULO    -> Math.PI * Math.pow(param1 / pxPorUnidad, 2);
            case RECTANGULO -> (2 * param1 / pxPorUnidad) * (2 * param2 / pxPorUnidad);
            case CUADRADO   -> Math.pow(2 * param1 / pxPorUnidad, 2);
            case TRIANGULO  -> 0.5 * (2 * param1 / pxPorUnidad) * (param2 / pxPorUnidad);
        };
    }

    /**
     * Calcula el perímetro / longitud de la superficie en 2D (unidad lineal).
     * En 2D, la "superficie gaussiana" es una curva cerrada; el "área" es el perímetro.
     */
    public double calcularPerimetro(double pxPorUnidad) {
        return switch (tipo) {
            case CIRCULO    -> 2 * Math.PI * (param1 / pxPorUnidad);
            case RECTANGULO -> 2 * (2 * param1 / pxPorUnidad + 2 * param2 / pxPorUnidad);
            case CUADRADO   -> 4 * (2 * param1 / pxPorUnidad);
            case TRIANGULO  -> {
                double b = 2 * param1 / pxPorUnidad;
                double h = param2 / pxPorUnidad;
                double lado = Math.hypot(param1 / pxPorUnidad, h);
                yield b + 2 * lado;
            }
        };
    }

    /**
     * Determina si un punto (px, py) en píxeles está dentro de esta figura.
     * Se usa para calcular qué fracción de la figura cargada queda dentro
     * de la superficie gaussiana.
     */
    public boolean contienePunto(double px, double py) {
        return switch (tipo) {
            case CIRCULO   -> Math.hypot(px - cx, py - cy) <= param1;
            case CUADRADO  -> Math.abs(px - cx) <= param1 && Math.abs(py - cy) <= param1;
            case RECTANGULO -> Math.abs(px - cx) <= param1 && Math.abs(py - cy) <= param2;
            case TRIANGULO -> {
                // Triángulo isósceles: base centrada en cx, vértice arriba
                double topY    = cy - param2;       // vértice superior (en px, Y+ abajo)
                double relY    = py - topY;         // distancia desde el vértice
                double altura  = param2;
                if (relY < 0 || relY > altura) yield false;
                double semiancho = param1 * (relY / altura);
                yield Math.abs(px - cx) <= semiancho;
            }
        };
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public TipoFigura getTipo()        { return tipo; }
    public double getCx()              { return cx; }
    public double getCy()              { return cy; }
    public double getParam1()          { return param1; }
    public double getParam2()          { return param2; }
    public double getCargaTotal()      { return cargaTotal; }
    public String getSigno()           { return signo; }
    public String getNombre()          { return nombre; }

    public void setCx(double cx)       { this.cx = cx; }
    public void setCy(double cy)       { this.cy = cy; }
    public void setParam1(double p)    { this.param1 = p; }
    public void setParam2(double p)    { this.param2 = p; }
    public void setCargaTotal(double c){ this.cargaTotal = c; }
    public void setSigno(String s)     { this.signo = s; }
    public void setNombre(String n)    { this.nombre = n; }

    /** Carga con signo en Coulombs. */
    public double getCargaEnCoulombs() {
        double q = cargaTotal * 1e-6;
        return signo.equals("-") ? -q : q;
    }
}