package com.usta.models;

/**
 * Encapsula el resultado del cálculo de la Ley de Gauss para una
 * configuración de figura cargada + superficie gaussiana.
 *
 * Ley de Gauss en 2D (analogía): ∮ E·dl = Q_int / ε₀
 *
 * En 2D estricto la ley tiene una forma logarítmica, pero para propósitos
 * didácticos usamos la forma integral estándar:
 *   Φ = Q_enc / ε₀
 *
 * donde Q_enc es la carga encerrada por la superficie gaussiana.
 */
public class ResultadoGauss {

    private static final double EPSILON_0 = 8.854187817e-12; // C²/(N·m²)

    private final FiguraGauss figuraCargada;
    private final FiguraGauss superficieGaussiana;

    private final double cargaEncerradaCoulombs;   // Q_enc en C
    private final double fraccionEncerrada;        // 0..1 (qué % de la figura queda dentro)
    private final double flujoElectrico;           // Φ = Q_enc / ε₀  (N·m²/C en 3D, analógico)
    private final double campoPromedio;            // E = Φ / A_superficie

    private final double areaSuperficie;           // en unidades reales (m²)
    private final boolean figuraDentro;            // true si la figura está completamente dentro

    public ResultadoGauss(FiguraGauss figuraCargada,
                           FiguraGauss superficieGaussiana,
                           double cargaEncerradaCoulombs,
                           double fraccionEncerrada,
                           double areaSuperficie,
                           boolean figuraDentro) {
        this.figuraCargada          = figuraCargada;
        this.superficieGaussiana    = superficieGaussiana;
        this.cargaEncerradaCoulombs = cargaEncerradaCoulombs;
        this.fraccionEncerrada      = fraccionEncerrada;
        this.areaSuperficie         = areaSuperficie;
        this.figuraDentro           = figuraDentro;
        this.flujoElectrico         = cargaEncerradaCoulombs / EPSILON_0;
        this.campoPromedio          = areaSuperficie > 0
            ? Math.abs(flujoElectrico) / areaSuperficie
            : 0;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public FiguraGauss getFiguraCargada()           { return figuraCargada; }
    public FiguraGauss getSuperficieGaussiana()     { return superficieGaussiana; }
    public double getCargaEncerradaCoulombs()        { return cargaEncerradaCoulombs; }
    public double getFraccionEncerrada()             { return fraccionEncerrada; }
    public double getFlujoElectrico()                { return flujoElectrico; }
    public double getCampoPromedio()                 { return campoPromedio; }
    public double getAreaSuperficie()               { return areaSuperficie; }
    public boolean isFiguraDentro()                  { return figuraDentro; }
    public double getCargaTotalCoulombs()            { return figuraCargada.getCargaEnCoulombs(); }

    public static double getEpsilon0()               { return EPSILON_0; }
}