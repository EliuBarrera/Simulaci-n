package com.usta.utils;

import com.usta.models.FiguraGauss;
import com.usta.models.ResultadoGauss;

/**
 * Encapsula la lógica física del cálculo de la Ley de Gauss en 2D.
 *
 * Estrategia para Q_enc:
 *   Si la figura cargada está completamente dentro → Q_enc = Q_total
 *   Si está completamente fuera                   → Q_enc = 0
 *   Si está parcialmente dentro → se estima Q_enc por muestreo Monte Carlo
 *   sobre el área de la figura cargada (proporcional al área encerrada).
 */
public class GaussCalculator {

    private static final int MUESTRAS_MC = 4000; // muestras Monte Carlo

    private final double pxPorUnidad;

    public GaussCalculator(double pxPorUnidad) {
        this.pxPorUnidad = pxPorUnidad;
    }

    /**
     * Calcula el resultado de la Ley de Gauss para la configuración dada.
     *
     * @param figuraCargada       La figura que tiene carga distribuida.
     * @param superficieGaussiana La superficie cerrada que puede encerrar la carga.
     * @return ResultadoGauss con flujo, campo promedio y datos del procedimiento.
     */
    public ResultadoGauss calcular(FiguraGauss figuraCargada,
                                    FiguraGauss superficieGaussiana) {

        double fraccion = calcularFraccionEncerrada(figuraCargada, superficieGaussiana);
        double qEnc     = figuraCargada.getCargaEnCoulombs() * fraccion;
        double areaSup  = superficieGaussiana.calcularAreaSuperficial(pxPorUnidad);
        boolean dentroCompleto = fraccion >= 0.999;

        return new ResultadoGauss(
            figuraCargada, superficieGaussiana,
            qEnc, fraccion, areaSup, dentroCompleto);
    }

    /**
     * Estima la fracción del área de la figura cargada que queda dentro
     * de la superficie gaussiana, usando muestreo Monte Carlo.
     *
     * Retorna un valor entre 0.0 (nada dentro) y 1.0 (todo dentro).
     */
    private double calcularFraccionEncerrada(FiguraGauss figura,
                                              FiguraGauss superficie) {
        // Bounding box de la figura cargada (3D)
        double[] bb  = boundingBox(figura);
        double minX = bb[0], minY = bb[1], minZ = bb[2];
        double maxX = bb[3], maxY = bb[4], maxZ = bb[5];

        int dentroFigura     = 0;
        int dentroAmbos      = 0;

        java.util.Random rng = new java.util.Random(42); // semilla fija para reproducibilidad

        for (int i = 0; i < MUESTRAS_MC; i++) {
            double px = minX + rng.nextDouble() * (maxX - minX);
            double py = minY + rng.nextDouble() * (maxY - minY);
            double pz = minZ + rng.nextDouble() * (maxZ - minZ);

            if (figura.contienePunto(px, py, pz)) {
                dentroFigura++;
                if (superficie.contienePunto(px, py, pz)) {
                    dentroAmbos++;
                }
            }
        }

        if (dentroFigura == 0) return 0.0;
        return (double) dentroAmbos / dentroFigura;
    }

    /**
     * Retorna el bounding box (minX, minY, minZ, maxX, maxY, maxZ) en píxeles de una figura.
     */
    private double[] boundingBox(FiguraGauss f) {
        double cx = f.getCx(), cy = f.getCy(), cz = f.getCz();
        double p1 = f.getParam1(), p2 = f.getParam2(), p3 = f.getParam3();
        double d = 0.01; // Para figuras 2D
        return switch (f.getTipo()) {
            case CIRCULO    -> new double[]{ cx-p1, cy-p1, cz-d, cx+p1, cy+p1, cz+d };
            case CUADRADO   -> new double[]{ cx-p1, cy-p1, cz-d, cx+p1, cy+p1, cz+d };
            case RECTANGULO -> new double[]{ cx-p1, cy-p2, cz-d, cx+p1, cy+p2, cz+d };
            case TRIANGULO  -> new double[]{ cx-p1, cy-p2, cz-d, cx+p1, cy,    cz+d };
            case ESFERA     -> new double[]{ cx-p1, cy-p1, cz-p1, cx+p1, cy+p1, cz+p1 };
            case CILINDRO   -> new double[]{ cx-p1, cy-p2/2, cz-p1, cx+p1, cy+p2/2, cz+p1 };
            case CAJA       -> new double[]{ cx-p1, cy-p2, cz-p3, cx+p1, cy+p2, cz+p3 };
        };
    }
}