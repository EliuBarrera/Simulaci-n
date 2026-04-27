package com.usta.models;

import java.util.List;

/**
 * Encapsula el resultado completo del cálculo de Potencial Eléctrico
 * y Energía Potencial sobre una partícula de análisis.
 */
public class ResultadoPotencial {

    private final Nodo particulaOrigen;

    // Coordenadas de la partícula origen
    private final double x0;
    private final double y0;
    private final double z0;

    // Contribuciones individuales
    private final List<ResultadoPotencialIndividual> potencialesIndividuales;

    // Sumas escalares
    private final double potencialTotalV;
    private final double energiaTotalU;

    private final boolean es3D;

    public ResultadoPotencial(Nodo particulaOrigen,
                              double x0, double y0, double z0,
                              List<ResultadoPotencialIndividual> potencialesIndividuales,
                              double potencialTotalV, double energiaTotalU,
                              boolean es3D) {
        this.particulaOrigen = particulaOrigen;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.potencialesIndividuales = potencialesIndividuales;
        this.potencialTotalV = potencialTotalV;
        this.energiaTotalU = energiaTotalU;
        this.es3D = es3D;
    }

    public Nodo getParticulaOrigen() { return particulaOrigen; }
    public double getX0() { return x0; }
    public double getY0() { return y0; }
    public double getZ0() { return z0; }
    public List<ResultadoPotencialIndividual> getPotencialesIndividuales() { return potencialesIndividuales; }
    public double getPotencialTotalV() { return potencialTotalV; }
    public double getEnergiaTotalU() { return energiaTotalU; }
    public boolean isEs3D() { return es3D; }
}
