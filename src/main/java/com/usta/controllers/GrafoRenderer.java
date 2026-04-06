package com.usta.controllers;

import com.usta.models.Nodo;
import com.usta.models.ResultadoFuerza;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.UnidadDistancia;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Dibuja flechas de fuerza (total e individuales) y el cuadrante del canvas (2D e isométrico 3D).
 */
public class GrafoRenderer {

    private final Pane   grafoPane;
    private final Canvas canvasPlano;

    private static final Color[] COLORES_FLECHAS = {
        Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE,
        Color.CYAN, Color.MAGENTA, Color.BROWN, Color.PINK,
        Color.DARKGREEN, Color.DARKBLUE, Color.DARKORANGE, Color.DARKVIOLET
    };

    public GrafoRenderer(Pane grafoPane, Canvas canvasPlano) {
        this.grafoPane   = grafoPane;
        this.canvasPlano = canvasPlano;
    }

    // ── Canvas ──────────────────────────────────────────────────────────────

    /** Dibuja la cuadrícula y ejes cartesianos 2D. */
    public void dibujarCuadrante(UnidadDistancia unidadActual) {
        if (canvasPlano == null) return;
        GraphicsContext gc  = canvasPlano.getGraphicsContext2D();
        double width        = canvasPlano.getWidth();
        double height       = canvasPlano.getHeight();
        final double MARGIN = 40;
        final double STEP   = 100;

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);
        for (double x = MARGIN + STEP; x <= width - MARGIN; x += STEP)
            gc.strokeLine(x, MARGIN, x, height - MARGIN);
        for (double y = height - MARGIN; y >= MARGIN; y -= STEP)
            gc.strokeLine(MARGIN, y, width - MARGIN, y);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeLine(MARGIN, height - MARGIN, width - MARGIN, height - MARGIN);
        gc.strokeLine(MARGIN, MARGIN, MARGIN, height - MARGIN);

        gc.setFill(Color.BLACK);
        gc.setLineWidth(1);
        gc.setFont(Font.font(10));

        int xMax = (int)((width - 2 * MARGIN) / STEP);
        for (int i = 0; i <= xMax; i++) {
            double px = MARGIN + i * STEP;
            gc.strokeLine(px, height - MARGIN, px, height - MARGIN + 5);
            gc.fillText(String.valueOf(i), px - 4, height - MARGIN + 15);
        }
        int yMax = (int)((height - 2 * MARGIN) / STEP);
        for (int i = 0; i <= yMax; i++) {
            double py = height - MARGIN - i * STEP;
            gc.strokeLine(MARGIN - 5, py, MARGIN, py);
            gc.fillText(String.valueOf(i), MARGIN - 25, py + 4);
        }

        gc.setFont(Font.font(11));
        gc.fillText("X (" + unidadActual.getSimbolo() + ")", width - MARGIN + 5, height - MARGIN + 4);
        gc.fillText("Y (" + unidadActual.getSimbolo() + ")", MARGIN - 8, MARGIN - 8);
    }

    /** Dibuja los ejes isométricos 3D con grillas en los tres planos. */
    public void dibujarCuadrante3D(UnidadDistancia unidadActual) {
        if (canvasPlano == null) return;
        GraphicsContext gc = canvasPlano.getGraphicsContext2D();
        double width  = canvasPlano.getWidth();
        double height = canvasPlano.getHeight();

        gc.setFill(Color.web("#f5f5f5"));
        gc.fillRect(0, 0, width, height);

        CoordenadasTransformador t = new CoordenadasTransformador(height, unidadActual);
        int MAX = 10;

        gc.setStroke(Color.web("#e0e0e0")); gc.setLineWidth(0.5);
        for (int i = 0; i <= MAX; i++) {
            gc.strokeLine(t.isoXToPx(i,0,0), t.isoYToPx(i,0,0), t.isoXToPx(i,MAX,0), t.isoYToPx(i,MAX,0));
            gc.strokeLine(t.isoXToPx(0,i,0), t.isoYToPx(0,i,0), t.isoXToPx(MAX,i,0), t.isoYToPx(MAX,i,0));
        }
        gc.setStroke(Color.web("#e8e8e8"));
        for (int i = 0; i <= MAX; i++) {
            gc.strokeLine(t.isoXToPx(i,0,0), t.isoYToPx(i,0,0), t.isoXToPx(i,0,MAX), t.isoYToPx(i,0,MAX));
            gc.strokeLine(t.isoXToPx(0,0,i), t.isoYToPx(0,0,i), t.isoXToPx(MAX,0,i), t.isoYToPx(MAX,0,i));
            gc.strokeLine(t.isoXToPx(0,i,0), t.isoYToPx(0,i,0), t.isoXToPx(0,i,MAX), t.isoYToPx(0,i,MAX));
            gc.strokeLine(t.isoXToPx(0,0,i), t.isoYToPx(0,0,i), t.isoXToPx(0,MAX,i), t.isoYToPx(0,MAX,i));
        }

        gc.setLineWidth(2.5);
        gc.setStroke(Color.web("#e53935"));
        gc.strokeLine(t.isoXToPx(0,0,0), t.isoYToPx(0,0,0), t.isoXToPx(MAX,0,0), t.isoYToPx(MAX,0,0));
        gc.setStroke(Color.web("#43a047"));
        gc.strokeLine(t.isoXToPx(0,0,0), t.isoYToPx(0,0,0), t.isoXToPx(0,MAX,0), t.isoYToPx(0,MAX,0));
        gc.setStroke(Color.web("#1e88e5"));
        gc.strokeLine(t.isoXToPx(0,0,0), t.isoYToPx(0,0,0), t.isoXToPx(0,0,MAX), t.isoYToPx(0,0,MAX));

        String simb = unidadActual.getSimbolo();
        gc.setFont(Font.font(12));
        gc.setFill(Color.web("#e53935"));
        gc.fillText("X (" + simb + ")", t.isoXToPx(MAX+0.3,0,0), t.isoYToPx(MAX+0.3,0,0));
        gc.setFill(Color.web("#43a047"));
        gc.fillText("Y (" + simb + ")", t.isoXToPx(0,MAX+0.3,0), t.isoYToPx(0,MAX+0.3,0));
        gc.setFill(Color.web("#1e88e5"));
        gc.fillText("Z (" + simb + ")", t.isoXToPx(0,0,MAX+0.3), t.isoYToPx(0,0,MAX+0.3));

        gc.setFont(Font.font(9)); gc.setFill(Color.web("#666"));
        for (int i = 1; i <= MAX; i++) {
            gc.fillText(String.valueOf(i), t.isoXToPx(i,0,0)-4,  t.isoYToPx(i,0,0)+14);
            gc.fillText(String.valueOf(i), t.isoXToPx(0,i,0)-18, t.isoYToPx(0,i,0)+4);
            gc.fillText(String.valueOf(i), t.isoXToPx(0,0,i)+6,  t.isoYToPx(0,0,i)+4);
        }

        gc.setFont(Font.font(14));
        gc.setFill(Color.web("#004d40"));
        gc.fillText("⬡ MODO 3D  |  10×10×10 " + simb, 20, 25);
    }

    // ── Flechas ──────────────────────────────────────────────────────────────

    /** Dibuja una única flecha de fuerza resultante. */
    public void dibujarFlechaFuerza(Nodo origen, double fuerzaX, double fuerzaY) {
        limpiarFlechas();
        double mag = Math.hypot(fuerzaX, fuerzaY);
        if (mag == 0) return;
        dibujarFlecha(origen.getX(), origen.getY(),
            fuerzaX / mag, -fuerzaY / mag,
            Color.RED, 3, 18, "flechaFuerza", null);
    }

    /** Dibuja una flecha por cada fuerza individual. */
    public void dibujarFlechasIndividuales(Nodo origen, List<ResultadoFuerza> fuerzas) {
        limpiarFlechas();
        int idx = 0;
        for (ResultadoFuerza rf : fuerzas) {
            double mag = rf.getMagnitud();
            if (mag == 0) continue;
            Color color = COLORES_FLECHAS[idx++ % COLORES_FLECHAS.length];
            String etiq = String.format("F_%s\nFe = %s N",
                rf.getParticulaCausante().getNombre(), formatearNumero(mag));
            dibujarFlecha(origen.getX(), origen.getY(),
                rf.getFx() / mag, -rf.getFy() / mag,
                color, 2.5, 15, "flechaIndividual", etiq);
        }
    }

    /** Elimina todas las flechas del grafoPane sin tocar aristas. */
    public void limpiarFlechas() {
        grafoPane.getChildren().removeIf(n -> n.getUserData() != null &&
            (n.getUserData().equals("flechaFuerza") ||
             n.getUserData().equals("flechaIndividual")));
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private void dibujarFlecha(double sx, double sy,
                                double dirX, double dirY,
                                Color color, double grosor, double head,
                                String tag, String etiqueta) {
        double ex = sx + dirX * 100;
        double ey = sy + dirY * 100;

        Line linea = new Line(sx, sy, ex, ey);
        linea.setStroke(color); linea.setStrokeWidth(grosor); linea.setUserData(tag);

        double ang = Math.atan2(ey - sy, ex - sx);
        Line h1 = new Line(ex, ey,
            ex - head * Math.cos(ang - Math.PI / 7),
            ey - head * Math.sin(ang - Math.PI / 7));
        Line h2 = new Line(ex, ey,
            ex - head * Math.cos(ang + Math.PI / 7),
            ey - head * Math.sin(ang + Math.PI / 7));
        h1.setStroke(color); h1.setStrokeWidth(grosor); h1.setUserData(tag);
        h2.setStroke(color); h2.setStrokeWidth(grosor); h2.setUserData(tag);

        grafoPane.getChildren().addAll(linea, h1, h2);

        if (etiqueta != null) {
            Text lbl = new Text(etiqueta);
            lbl.setFont(Font.font(12));
            lbl.setFill(color);
            lbl.setUserData(tag);
            lbl.setX(ex + (dirX < 0 ? -80 : 10));
            lbl.setY(ey + (dirY < 0 ? -15 : 15));
            grafoPane.getChildren().add(lbl);
        }
    }

    private String formatearNumero(double valor) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.000000", sym).format(valor);
    }
}