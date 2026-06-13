package com.usta.utils;

import com.usta.models.Nodo;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Dibuja líneas de campo eléctrico sobre el canvas 2D del simulador
 * de Potencial Eléctrico.
 *
 * Usa integración numérica (método de Euler) para trazar las líneas
 * de campo desde cada partícula cargada.
 */
public class LineasDeCampoRenderer {

    private static final double K = 8.99e9;
    private static final double MARGIN = 40;
    private static final double STEP_PX = 100; // Tamaño de celda del plano en píxeles

    /**
     * Dibuja las líneas de campo eléctrico sobre el canvas.
     *
     * @param canvas      el canvas 2D del plano
     * @param nodos       la lista de nodos (partículas) del grafo
     * @param unidadActual la unidad de distancia actual (para la cuadrícula)
     */
    public static void dibujar(Canvas canvas, List<Nodo> nodos, UnidadDistancia unidadActual) {
        if (canvas == null || nodos == null || nodos.size() < 1) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double canvasH = canvas.getHeight();

        int numLineas = 16; // Líneas por partícula
        double stepSize = 2.0; // Tamaño de paso en píxeles
        int maxSteps = 800; // Máximo de pasos por línea

        for (Nodo nodo : nodos) {
            // Coordenadas en píxeles del nodo
            double cx = MARGIN + nodo.getX() * STEP_PX;
            double cy = canvasH - MARGIN - nodo.getY() * STEP_PX;

            boolean esPositiva = "+".equals(nodo.getTipoCarga());
            // Dirección: las líneas salen de cargas positivas, entran en negativas
            double signoDir = esPositiva ? 1.0 : -1.0;

            // Color de las líneas según el tipo de carga
            Color colorLinea = esPositiva
                    ? Color.rgb(220, 60, 60, 0.45)   // Rojo semitransparente
                    : Color.rgb(60, 60, 220, 0.45);   // Azul semitransparente

            gc.setStroke(colorLinea);
            gc.setLineWidth(1.2);

            for (int i = 0; i < numLineas; i++) {
                double angulo = 2.0 * Math.PI * i / numLineas;
                double startRadius = 18.0; // Inicio fuera del círculo visual

                double px = cx + startRadius * Math.cos(angulo);
                double py = cy + startRadius * Math.sin(angulo);

                gc.beginPath();
                gc.moveTo(px, py);

                for (int s = 0; s < maxSteps; s++) {
                    // Calcular campo eléctrico total en (px, py) en coordenadas lógicas
                    double logX = (px - MARGIN) / STEP_PX;
                    double logY = (canvasH - MARGIN - py) / STEP_PX;

                    double ex = 0, ey = 0;
                    for (Nodo n : nodos) {
                        double dx = logX - n.getX();
                        double dy = logY - n.getY();
                        double r2 = dx * dx + dy * dy;
                        if (r2 < 0.01) { // Demasiado cerca de una carga → terminar
                            ex = 0;
                            ey = 0;
                            break;
                        }
                        double r = Math.sqrt(r2);
                        double valorCarga = Math.abs(n.getValorCarga());
                        if ("-".equals(n.getTipoCarga())) valorCarga = -valorCarga;
                        // E = K * q / r^2, dirección radial
                        double eMag = valorCarga / r2;
                        ex += eMag * (dx / r);
                        ey += eMag * (dy / r);
                    }

                    double eMag = Math.hypot(ex, ey);
                    if (eMag < 1e-12) break;

                    // Normalizar y avanzar
                    double nx = ex / eMag;
                    double ny = ey / eMag;

                    // Convertir dirección del campo (lógico) a píxeles
                    // En el canvas, Y está invertido respecto a lógico
                    px += signoDir * nx * stepSize;
                    py -= signoDir * ny * stepSize; // Invertir Y

                    // Verificar límites
                    if (px < MARGIN || px > canvas.getWidth() - MARGIN ||
                        py < MARGIN || py > canvasH - MARGIN) {
                        break;
                    }

                    // Verificar si estamos muy cerca de otra carga
                    boolean cercaDeOtraCarga = false;
                    for (Nodo n : nodos) {
                        if (n == nodo) continue;
                        double dxPx = px - (MARGIN + n.getX() * STEP_PX);
                        double dyPx = py - (canvasH - MARGIN - n.getY() * STEP_PX);
                        if (Math.hypot(dxPx, dyPx) < 12) {
                            cercaDeOtraCarga = true;
                            break;
                        }
                    }
                    if (cercaDeOtraCarga) {
                        gc.lineTo(px, py);
                        break;
                    }

                    gc.lineTo(px, py);
                }

                gc.stroke();
            }
        }
    }
}
