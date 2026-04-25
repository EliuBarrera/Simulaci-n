package com.usta.utils;

import com.usta.models.FiguraGauss;
import com.usta.models.ResultadoGauss;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula toda la lógica de renderizado 2D sobre el Canvas de Gauss.
 * No tiene estado propio más allá del canvas que recibe; el controlador
 * le pasa los datos que necesita en cada llamada a {@link #render}.
 */
public class GaussCanvasRenderer {

    private static final double MARGIN      = 50.0;
    private static final double PX_POR_UNIT = 80.0;

    private final Canvas canvas;

    public GaussCanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    // =========================================================================
    // PUNTO DE ENTRADA
    // =========================================================================

    /**
     * Redibuja el canvas completo.
     *
     * @param figuraCargada    Figura con carga (puede ser null).
     * @param superficieGauss  Superficie gaussiana (puede ser null).
     * @param resultado        Último resultado calculado (null si no hay).
     * @param mostrarLineas    Si se deben dibujar líneas de campo.
     * @param preview          Figura temporal durante el drag (null si no aplica).
     * @param previewEsFigura  true → preview es figura cargada; false → superficie.
     */
    public void render(FiguraGauss figuraCargada,
                       FiguraGauss superficieGauss,
                       ResultadoGauss resultado,
                       boolean mostrarLineas,
                       FiguraGauss preview,
                       boolean previewEsFigura) {

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);

        dibujarPlano(gc, w, h);

        if (mostrarLineas && resultado != null && superficieGauss != null) {
            dibujarLineasCampo(gc, figuraCargada, superficieGauss);
        }

        if (figuraCargada != null) {
            dibujarFigura(gc, figuraCargada,
                Color.web("#e53935"), Color.web("#ffcdd2"),
                true, resultado != null);
        }

        if (superficieGauss != null) {
            dibujarFigura(gc, superficieGauss,
                Color.web("#00838f"), Color.TRANSPARENT,
                false, false);
        }

        if (preview != null) {
            Color stroke = previewEsFigura ? Color.web("#e53935") : Color.web("#00838f");
            gc.setStroke(stroke.deriveColor(0, 1, 1, 0.7));
            gc.setLineWidth(2);
            gc.setLineDashes(8, 5);
            trazarForma(gc, preview);
            gc.setLineDashes();
        }
    }

    // =========================================================================
    // CUADRÍCULA Y EJES
    // =========================================================================

    private void dibujarPlano(GraphicsContext gc, double w, double h) {
        // Cuadrícula
        gc.setStroke(Color.web("#e0f7fa"));
        gc.setLineWidth(0.8);
        for (double x = MARGIN; x <= w - MARGIN; x += PX_POR_UNIT)
            gc.strokeLine(x, MARGIN, x, h - MARGIN);
        for (double y = h - MARGIN; y >= MARGIN; y -= PX_POR_UNIT)
            gc.strokeLine(MARGIN, y, w - MARGIN, y);

        // Ejes
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(1.5);
        gc.strokeLine(MARGIN, h - MARGIN, w - MARGIN, h - MARGIN);
        gc.strokeLine(MARGIN, MARGIN, MARGIN, h - MARGIN);

        // Ticks y etiquetas X
        gc.setFill(Color.web("#6B7280"));
        gc.setFont(Font.font("Courier New", 9));
        int xMax = (int) ((w - 2 * MARGIN) / PX_POR_UNIT);
        for (int i = 0; i <= xMax; i++) {
            double px = MARGIN + i * PX_POR_UNIT;
            gc.strokeLine(px, h - MARGIN, px, h - MARGIN + 4);
            gc.fillText(String.valueOf(i), px - 3, h - MARGIN + 14);
        }

        // Ticks y etiquetas Y
        int yMax = (int) ((h - 2 * MARGIN) / PX_POR_UNIT);
        for (int i = 0; i <= yMax; i++) {
            double py = h - MARGIN - i * PX_POR_UNIT;
            gc.strokeLine(MARGIN - 4, py, MARGIN, py);
            gc.fillText(String.valueOf(i), MARGIN - 22, py + 4);
        }

        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        gc.fillText("X (m)", w - MARGIN + 5, h - MARGIN + 4);
        gc.fillText("Y (m)", MARGIN - 10, MARGIN - 10);
    }

    // =========================================================================
    // FIGURA
    // =========================================================================

    private void dibujarFigura(GraphicsContext gc, FiguraGauss f,
                                Color stroke, Color fill,
                                boolean mostrarCarga, boolean destacar) {
        gc.save();

        // Relleno semitransparente
        gc.setFill(fill.equals(Color.TRANSPARENT)
            ? Color.TRANSPARENT
            : fill.deriveColor(0, 1, 1, 0.25));
        rellenarForma(gc, f);

        // Borde
        gc.setStroke(stroke);
        gc.setLineWidth(destacar ? 3 : 2);
        gc.setLineDashes();
        trazarForma(gc, f);

        // Etiqueta
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        if (mostrarCarga) {
            gc.setFill(stroke.darker());
            String[] lineas = (f.getNombre() + "\nQ = " + f.getCargaTotal()
                + " µC (" + f.getSigno() + ")").split("\n");
            for (int i = 0; i < lineas.length; i++)
                gc.fillText(lineas[i], f.getCx() + f.getParam1() + 6, f.getCy() - 8 + i * 15);
        } else {
            gc.setFill(stroke);
            gc.fillText("Superficie G.", f.getCx() - 40, f.getCy() - f.getParam1() - 8);
        }

        // Punto central
        gc.setFill(stroke);
        gc.fillOval(f.getCx() - 4, f.getCy() - 4, 8, 8);

        gc.restore();
    }

    // =========================================================================
    // FORMAS
    // =========================================================================

    public void trazarForma(GraphicsContext gc, FiguraGauss f) {
        double cx = f.getCx(), cy = f.getCy(), p1 = f.getParam1(), p2 = f.getParam2();
        switch (f.getTipo()) {
            case CIRCULO    -> gc.strokeOval(cx - p1, cy - p1, 2 * p1, 2 * p1);
            case CUADRADO   -> gc.strokeRect(cx - p1, cy - p1, 2 * p1, 2 * p1);
            case RECTANGULO -> gc.strokeRect(cx - p1, cy - p2, 2 * p1, 2 * p2);
            case TRIANGULO  -> {
                gc.beginPath();
                gc.moveTo(cx, cy - p2);
                gc.lineTo(cx - p1, cy);
                gc.lineTo(cx + p1, cy);
                gc.closePath();
                gc.stroke();
            }
            // Proyecciones 2D de figuras 3D
            case ESFERA     -> gc.strokeOval(cx - p1, cy - p1, 2 * p1, 2 * p1);
            case CILINDRO   -> gc.strokeRect(cx - p1, cy - p2, 2 * p1, 2 * p2);
            case CAJA       -> gc.strokeRect(cx - p1, cy - p2, 2 * p1, 2 * p2);
        }
    }

    private void rellenarForma(GraphicsContext gc, FiguraGauss f) {
        double cx = f.getCx(), cy = f.getCy(), p1 = f.getParam1(), p2 = f.getParam2();
        switch (f.getTipo()) {
            case CIRCULO    -> gc.fillOval(cx - p1, cy - p1, 2 * p1, 2 * p1);
            case CUADRADO   -> gc.fillRect(cx - p1, cy - p1, 2 * p1, 2 * p1);
            case RECTANGULO -> gc.fillRect(cx - p1, cy - p2, 2 * p1, 2 * p2);
            case TRIANGULO  -> {
                gc.beginPath();
                gc.moveTo(cx, cy - p2);
                gc.lineTo(cx - p1, cy);
                gc.lineTo(cx + p1, cy);
                gc.closePath();
                gc.fill();
            }
            // Proyecciones 2D de figuras 3D
            case ESFERA     -> gc.fillOval(cx - p1, cy - p1, 2 * p1, 2 * p1);
            case CILINDRO   -> gc.fillRect(cx - p1, cy - p2, 2 * p1, 2 * p2);
            case CAJA       -> gc.fillRect(cx - p1, cy - p2, 2 * p1, 2 * p2);
        }
    }

    // =========================================================================
    // LÍNEAS DE CAMPO
    // =========================================================================

    private void dibujarLineasCampo(GraphicsContext gc,
                                     FiguraGauss figura,
                                     FiguraGauss superficie) {
        if (figura == null || superficie == null) return;

        final int    NUM_LINEAS = 16;
        final double PASO       = 4.0;
        final int    MAX_PASOS  = 600;

        double  cx       = figura.getCx();
        double  cy       = figura.getCy();
        boolean positiva = figura.getSigno().equals("+");
        Color   color    = positiva ? Color.web("#e53935", 0.7) : Color.web("#1565c0", 0.7);

        gc.setStroke(color);
        gc.setLineWidth(1.2);
        gc.setLineDashes();

        for (int i = 0; i < NUM_LINEAS; i++) {
            double angulo = 2 * Math.PI * i / NUM_LINEAS;
            double dirX   = Math.cos(angulo);
            double dirY   = Math.sin(angulo);
            double dX     = positiva ? dirX : -dirX;
            double dY     = positiva ? dirY : -dirY;

            double currentX = cx + dirX * figura.getParam1();
            double currentY = cy + dirY * figura.getParam1();

            gc.beginPath();
            gc.moveTo(currentX, currentY);

            double prevX = currentX, prevY = currentY;

            for (int s = 0; s < MAX_PASOS; s++) {
                currentX += dX * PASO;
                currentY += dY * PASO;
                
                if (!superficie.contienePunto(currentX, currentY)) break;
                
                gc.lineTo(currentX, currentY);
                prevX = currentX; 
                prevY = currentY;
            }
            gc.stroke();
            
            // Dibujar punta de flecha al final si la línea tiene longitud
            if (currentX != cx + dirX * figura.getParam1()) {
                double angArrow = Math.atan2(currentY - (currentY - dY*PASO), currentX - (currentX - dX*PASO));
                dibujarPuntaRapida(gc, currentX, currentY, angArrow);
            }
        }
    }

    private void dibujarPuntaRapida(GraphicsContext gc, double x, double y, double ang) {
        double head = 8;
        gc.beginPath();
        gc.moveTo(x, y);
        gc.lineTo(x - head * Math.cos(ang - Math.PI / 7), y - head * Math.sin(ang - Math.PI / 7));
        gc.moveTo(x, y);
        gc.lineTo(x - head * Math.cos(ang + Math.PI / 7), y - head * Math.sin(ang + Math.PI / 7));
        gc.stroke();
    }

    private void dibujarPuntaFlecha(GraphicsContext gc, List<double[]> puntos) {
        if (puntos.size() <= 2) return;
        double[] last = puntos.get(puntos.size() - 1);
        double[] prev = puntos.get(puntos.size() - 2);
        double   ang  = Math.atan2(last[1] - prev[1], last[0] - prev[0]);
        double   head = 8;
        gc.beginPath();
        gc.moveTo(last[0], last[1]);
        gc.lineTo(last[0] - head * Math.cos(ang - Math.PI / 7),
                  last[1] - head * Math.sin(ang - Math.PI / 7));
        gc.moveTo(last[0], last[1]);
        gc.lineTo(last[0] - head * Math.cos(ang + Math.PI / 7),
                  last[1] - head * Math.sin(ang + Math.PI / 7));
        gc.stroke();
    }
}