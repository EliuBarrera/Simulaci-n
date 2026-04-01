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
        double perim    = superficieGaussiana.calcularPerimetro(pxPorUnidad);
        boolean dentroCompleto = fraccion >= 0.999;

        return new ResultadoGauss(
            figuraCargada, superficieGaussiana,
            qEnc, fraccion, perim, dentroCompleto);
    }

    /**
     * Estima la fracción del área de la figura cargada que queda dentro
     * de la superficie gaussiana, usando muestreo Monte Carlo.
     *
     * Retorna un valor entre 0.0 (nada dentro) y 1.0 (todo dentro).
     */
    private double calcularFraccionEncerrada(FiguraGauss figura,
                                              FiguraGauss superficie) {
        // Bounding box de la figura cargada
        double[] bb  = boundingBox(figura);
        double minX = bb[0], minY = bb[1], maxX = bb[2], maxY = bb[3];

        int dentroFigura     = 0;
        int dentroAmbos      = 0;

        java.util.Random rng = new java.util.Random(42); // semilla fija para reproducibilidad

        for (int i = 0; i < MUESTRAS_MC; i++) {
            double px = minX + rng.nextDouble() * (maxX - minX);
            double py = minY + rng.nextDouble() * (maxY - minY);

            if (figura.contienePunto(px, py)) {
                dentroFigura++;
                if (superficie.contienePunto(px, py)) {
                    dentroAmbos++;
                }
            }
        }

        if (dentroFigura == 0) return 0.0;
        return (double) dentroAmbos / dentroFigura;
    }

    /**
     * Retorna el bounding box (minX, minY, maxX, maxY) en píxeles de una figura.
     */
    private double[] boundingBox(FiguraGauss f) {
        double cx = f.getCx(), cy = f.getCy();
        double p1 = f.getParam1(), p2 = f.getParam2();
        return switch (f.getTipo()) {
            case CIRCULO    -> new double[]{ cx-p1, cy-p1, cx+p1, cy+p1 };
            case CUADRADO   -> new double[]{ cx-p1, cy-p1, cx+p1, cy+p1 };
            case RECTANGULO -> new double[]{ cx-p1, cy-p2, cx+p1, cy+p2 };
            case TRIANGULO  -> new double[]{ cx-p1, cy-p2, cx+p1, cy      };
        };
    }
}