package com.usta.models;

/**
 * Representa la contribución individual de una partícula fuente al potencial eléctrico
 * y a la energía potencial en la posición de una partícula de análisis.
 */
public class ResultadoPotencialIndividual {

    private final Nodo particulaFuente;

    // Coordenadas
    private final double x0, y0, z0; // Análisis
    private final double x1, y1, z1; // Fuente

    // Distancia
    private final double distanciaUnidades;
    private final double distanciaMetros;

    // Valores escalares
    private final double potencialV; // en Voltios (V)
    private final double energiaU;   // en Joules (J)

    public ResultadoPotencialIndividual(Nodo particulaFuente,
                                        double x0, double y0, double z0,
                                        double x1, double y1, double z1,
                                        double distanciaUnidades, double distanciaMetros,
                                        double potencialV, double energiaU) {
        this.particulaFuente = particulaFuente;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.distanciaUnidades = distanciaUnidades;
        this.distanciaMetros = distanciaMetros;
        this.potencialV = potencialV;
        this.energiaU = energiaU;
    }

    public Nodo getParticulaFuente() { return particulaFuente; }
    public double getX0() { return x0; }
    public double getY0() { return y0; }
    public double getZ0() { return z0; }
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getZ1() { return z1; }
    public double getDistanciaUnidades() { return distanciaUnidades; }
    public double getDistanciaMetros() { return distanciaMetros; }
    public double getPotencialV() { return potencialV; }
    public double getEnergiaU() { return energiaU; }
}
