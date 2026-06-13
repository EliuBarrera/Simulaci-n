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
    private double cx, cy, cz;   // centro espacial
    private double param1;       // radio / semiancho / semilado / semibase
    private double param2;       // semialto / altura (para rect y tri)
    private double param3;       // profundidad / altura 3D

    // Solo para figuras cargadas
    private double cargaTotal;   // en µC
    private String signo;        // "+" o "-"
    private String nombre;

    public FiguraGauss(TipoFigura tipo, double cx, double cy, double cz,
                        double param1, double param2, double param3,
                        double cargaTotal, String signo, String nombre) {
        this.tipo       = tipo;
        this.cx         = cx;
        this.cy         = cy;
        this.cz         = cz;
        this.param1     = param1;
        this.param2     = param2;
        this.param3     = param3;
        this.cargaTotal = cargaTotal;
        this.signo      = signo;
        this.nombre     = nombre;
    }

    // ── Geometría ────────────────────────────────────────────────────────────

    /**
     * Retorna el área o volumen de la figura en unidades reales.
     * Se usa para cálculo referencial y Densidad de Carga.
     */
    public double calcularMagnitudEspacial(double pxPorUnidad) {
        double p1u = param1 / pxPorUnidad;
        double p2u = param2 / pxPorUnidad;
        double p3u = param3 / pxPorUnidad;

        return switch (tipo) {
            case CIRCULO    -> Math.PI * Math.pow(p1u, 2);
            case RECTANGULO -> (2 * p1u) * (2 * p2u);
            case CUADRADO   -> Math.pow(2 * p1u, 2);
            case TRIANGULO  -> 0.5 * (2 * p1u) * p2u;
            case ESFERA     -> (4.0/3.0) * Math.PI * Math.pow(p1u, 3);
            case CILINDRO   -> Math.PI * Math.pow(p1u, 2) * p2u; // param2 = altura
            case CAJA       -> (2 * p1u) * (2 * p2u) * (2 * p3u);
        };
    }

    /**
     * Calcula el área superficial de la superficie gaussiana cerrada.
     * Para la Ley de Gauss: E = Φ / A_superficie
     *
     * Figuras 2D se tratan como su análogo 3D cerrado:
     *   CIRCULO    → esfera  (4πr²)
     *   CUADRADO   → cubo    (6·(2l)²)
     *   RECTANGULO → caja    (2·(ab + ac + bc)), con c = min(a,b)
     *   TRIANGULO  → prisma triangular aproximado
     *
     * Figuras 3D ya son superficies cerradas reales.
     */
    public double calcularAreaSuperficial(double pxPorUnidad) {
        double p1u = param1 / pxPorUnidad;
        double p2u = param2 / pxPorUnidad;
        double p3u = param3 / pxPorUnidad;

        return switch (tipo) {
            case CIRCULO    -> 4 * Math.PI * Math.pow(p1u, 2);   // esfera equivalente
            case CUADRADO   -> 6 * Math.pow(2 * p1u, 2);         // cubo equivalente
            case RECTANGULO -> {
                double a = 2 * p1u;
                double b = 2 * p2u;
                double c = Math.min(a, b);  // profundidad = dimensión menor
                yield 2 * (a * b + a * c + b * c);
            }
            case TRIANGULO  -> {
                double base = 2 * p1u;
                double h = p2u;
                double lado = Math.hypot(p1u, h);
                double areaTriangulo = 0.5 * base * h;
                double perimetroTri = base + 2 * lado;
                double profundidad = Math.min(base, h);
                yield 2 * areaTriangulo + perimetroTri * profundidad;
            }
            case ESFERA     -> 4 * Math.PI * Math.pow(p1u, 2);
            case CILINDRO   -> 2 * Math.PI * p1u * (p1u + p2u);
            case CAJA       -> 2 * ((2*p1u)*(2*p2u) + (2*p1u)*(2*p3u) + (2*p2u)*(2*p3u));
        };
    }

    /**
     * Determina si un punto (px, py, pz) está dentro de esta figura.
     */
    public boolean contienePunto(double px, double py, double pz) {
        return switch (tipo) {
            case CIRCULO   -> Math.hypot(px - cx, py - cy) <= param1 && Math.abs(pz - cz) <= 0.01; // Como es 2D asume grosor ~0
            case CUADRADO  -> Math.abs(px - cx) <= param1 && Math.abs(py - cy) <= param1 && Math.abs(pz - cz) <= 0.01;
            case RECTANGULO -> Math.abs(px - cx) <= param1 && Math.abs(py - cy) <= param2 && Math.abs(pz - cz) <= 0.01;
            case TRIANGULO -> {
                double topY    = cy - param2; 
                double relY    = py - topY;   
                double altura  = param2;
                if (relY < 0 || relY > altura) yield false;
                double semiancho = param1 * (relY / altura);
                yield Math.abs(px - cx) <= semiancho && Math.abs(pz - cz) <= 0.01;
            }
            case ESFERA -> {
                double dx = px - cx; double dy = py - cy; double dz = pz - cz;
                yield (dx*dx + dy*dy + dz*dz) <= (param1*param1);
            }
            case CILINDRO -> {
                double dx = px - cx; double dz = pz - cz;
                boolean inCircle = (dx*dx + dz*dz) <= (param1*param1); // cilindro orientado en Y
                boolean inHeight = Math.abs(py - cy) <= (param2 / 2.0); // cilindro centrado en Y
                yield inCircle && inHeight;
            }
            case CAJA -> {
                yield Math.abs(px - cx) <= param1 && Math.abs(py - cy) <= param2 && Math.abs(pz - cz) <= param3;
            }
        };
    }
    
    public boolean contienePunto(double px, double py) {
        return contienePunto(px, py, this.cz);
    }

    public TipoFigura getTipo()        { return tipo; }
    public double getCx()              { return cx; }
    public double getCy()              { return cy; }
    public double getCz()              { return cz; }
    public double getParam1()          { return param1; }
    public double getParam2()          { return param2; }
    public double getParam3()          { return param3; }
    public double getCargaTotal()      { return cargaTotal; }
    public String getSigno()           { return signo; }
    public String getNombre()          { return nombre; }

    public void setCx(double cx)       { this.cx = cx; }
    public void setCy(double cy)       { this.cy = cy; }
    public void setCz(double cz)       { this.cz = cz; }
    public void setParam1(double p)    { this.param1 = p; }
    public void setParam2(double p)    { this.param2 = p; }
    public void setParam3(double p)    { this.param3 = p; }
    public void setCargaTotal(double c){ this.cargaTotal = c; }
    public void setSigno(String s)     { this.signo = s; }
    public void setNombre(String n)    { this.nombre = n; }

    /** Carga con signo en Coulombs. */
    public double getCargaEnCoulombs() {
        double q = cargaTotal * 1e-6;
        return signo.equals("-") ? -q : q;
    }
}