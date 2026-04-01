package com.usta.models;

import java.util.List;

/**
 * Encapsula el resultado completo del cálculo de fuerza eléctrica
 * y campo eléctrico sobre una partícula de análisis.
 * Soporta tanto cálculos 2D como 3D.
 */
public class ResultadoCalculo {

    private final Nodo particulaOrigen;

    // Coordenadas de la partícula origen en sistema matemático (unidades reales)
    private final double x0;
    private final double y0;
    private final double z0;

    // Fuerzas individuales ejercidas por cada partícula conectada
    private final List<ResultadoFuerza> fuerzasIndividuales;

    // Suma vectorial
    private final double fuerzaTotalX;
    private final double fuerzaTotalY;
    private final double fuerzaTotalZ;
    private final double fuerzaTotal;       // magnitud resultante en N
    private final double anguloResultante;  // grados [0, 360) en plano XY
    private final double anguloElevacionResultante; // grados elevación desde plano XY

    // Campo eléctrico en N/C
    private final double campoElectrico;

    // Flag 3D
    private final boolean es3D;

    /**
     * Constructor 2D — retrocompatible.
     */
    public ResultadoCalculo(Nodo particulaOrigen,
                            double x0, double y0,
                            List<ResultadoFuerza> fuerzasIndividuales,
                            double fuerzaTotalX, double fuerzaTotalY) {
        this(particulaOrigen, x0, y0, 0, fuerzasIndividuales,
             fuerzaTotalX, fuerzaTotalY, 0, false);
    }

    /**
     * Constructor 3D completo.
     */
    public ResultadoCalculo(Nodo particulaOrigen,
                            double x0, double y0, double z0,
                            List<ResultadoFuerza> fuerzasIndividuales,
                            double fuerzaTotalX, double fuerzaTotalY, double fuerzaTotalZ,
                            boolean es3D) {
        this.particulaOrigen     = particulaOrigen;
        this.x0                  = x0;
        this.y0                  = y0;
        this.z0                  = z0;
        this.fuerzasIndividuales = fuerzasIndividuales;
        this.fuerzaTotalX        = fuerzaTotalX;
        this.fuerzaTotalY        = fuerzaTotalY;
        this.fuerzaTotalZ        = fuerzaTotalZ;
        this.es3D                = es3D;

        if (es3D) {
            this.fuerzaTotal = Math.sqrt(fuerzaTotalX * fuerzaTotalX
                                       + fuerzaTotalY * fuerzaTotalY
                                       + fuerzaTotalZ * fuerzaTotalZ);
        } else {
            this.fuerzaTotal = Math.hypot(fuerzaTotalX, fuerzaTotalY);
        }

        double ang = Math.toDegrees(Math.atan2(fuerzaTotalY, fuerzaTotalX));
        if (ang < 0) ang += 360.0;
        this.anguloResultante = ang;

        if (es3D) {
            double distXY = Math.hypot(fuerzaTotalX, fuerzaTotalY);
            this.anguloElevacionResultante = Math.toDegrees(Math.atan2(fuerzaTotalZ, distXY));
        } else {
            this.anguloElevacionResultante = 0;
        }

        double q0 = Math.abs(particulaOrigen.getValorCarga() * 1e-6);
        this.campoElectrico = (q0 != 0) ? fuerzaTotal / q0 : 0;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Nodo getParticulaOrigen()              { return particulaOrigen; }
    public double getX0()                         { return x0; }
    public double getY0()                         { return y0; }
    public double getZ0()                         { return z0; }
    public List<ResultadoFuerza> getFuerzasIndividuales() { return fuerzasIndividuales; }
    public double getFuerzaTotalX()               { return fuerzaTotalX; }
    public double getFuerzaTotalY()               { return fuerzaTotalY; }
    public double getFuerzaTotalZ()               { return fuerzaTotalZ; }
    public double getFuerzaTotal()                { return fuerzaTotal; }
    public double getAnguloResultante()           { return anguloResultante; }
    public double getAnguloElevacionResultante()  { return anguloElevacionResultante; }
    public double getCampoElectrico()             { return campoElectrico; }
    public boolean isEs3D()                       { return es3D; }
}