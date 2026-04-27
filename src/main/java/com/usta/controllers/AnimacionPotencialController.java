package com.usta.controllers;

import com.usta.models.Nodo;
import com.usta.models.ResultadoPotencial;
import com.usta.models.ResultadoPotencialIndividual;
import com.usta.utils.UnidadDistancia;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Animación paso a paso del Potencial Eléctrico.
 */
public class AnimacionPotencialController {

    private static final Color[] PALETA = {
            Color.web("#ff9800"), Color.web("#f57c00"), Color.web("#e65100"),
            Color.web("#ffb74d"), Color.web("#ffa726"), Color.web("#ffcc80")
    };

    private static final Color COLOR_ORIGEN = Color.web("#e53935");
    private static final Color COLOR_RESULTADO = Color.web("#1565c0");

    private ResultadoPotencial resultado;
    private UnidadDistancia unidad;
    private Pane grafoPane;
    private Map<Nodo, Circle> nodoCirculos;
    private int pasoActual = 0;
    private int totalPasos = 0;
    private List<ResultadoPotencialIndividual> potenciales;

    private record EstadoCirculo(Color fill, Color stroke, double radio, double strokeWidth) {
    }

    private final Map<Circle, EstadoCirculo> estadosOriginales = new HashMap<>();

    private final List<javafx.scene.Node> nodosAnimacion = new ArrayList<>();
    private Circle haloOrigen;
    private Timeline timelineHalo;

    public void inicializar(ResultadoPotencial resultado,
            UnidadDistancia unidad,
            Pane grafoPane,
            Map<Nodo, Circle> nodoCirculos) {
        this.resultado = resultado;
        this.unidad = unidad;
        this.grafoPane = grafoPane;
        this.nodoCirculos = nodoCirculos;
        this.potenciales = resultado.getPotencialesIndividuales();
        this.pasoActual = 0;
        // Pasos: 0 (origen) + (n * 2 sub-pasos) + 1 (suma) + 1 (energía)
        this.totalPasos = 1 + potenciales.size() * 2 + 2;

        guardarEstadosOriginales();
        limpiarAnimacion();
        renderizarPaso();
    }

    public boolean siguiente() {
        if (pasoActual < totalPasos - 1) {
            pasoActual++;
            limpiarAnimacion();
            renderizarPaso();
            return true;
        }
        return false;
    }

    public boolean anterior() {
        if (pasoActual > 0) {
            pasoActual--;
            limpiarAnimacion();
            renderizarPaso();
            return true;
        }
        return false;
    }

    public void restaurar() {
        limpiarAnimacion();
        restaurarCirculos();
        estadosOriginales.clear();
        resultado = null;
    }

    public int getPasoActual() {
        return pasoActual;
    }

    public int getTotalPasos() {
        return totalPasos;
    }

    public boolean hayResultado() {
        return resultado != null;
    }

    private void renderizarPaso() {
        int n = potenciales.size();

        aplicarEstadoOrigen();
        restaurarCausantesANeutro();

        if (pasoActual == 0) {
            mostrarEtiquetaOrigen();

        } else if (pasoActual >= 1 && pasoActual <= n * 2) {
            int idx = (pasoActual - 1) / 2;
            int subPas = (pasoActual - 1) % 2;
            Color color = PALETA[idx % PALETA.length];
            ResultadoPotencialIndividual rpi = potenciales.get(idx);

            aplicarEstadoCausanteActivo(rpi, color);

            switch (subPas) {
                case 0 -> dibujarVectorR(rpi, color);
                case 1 -> {
                    dibujarVectorR(rpi, color);
                    mostrarFormulaV(rpi, color);
                }
            }

        } else if (pasoActual == n * 2 + 1) {
            dibujarTodasLineasR();
            mostrarPotencialTotal();

        } else if (pasoActual == n * 2 + 2) {
            dibujarTodasLineasR();
            mostrarEnergiaPotencial();
        }
    }

    private void guardarEstadosOriginales() {
        estadosOriginales.clear();
        Nodo origen = resultado.getParticulaOrigen();
        guardarCirculo(nodoCirculos.get(origen));
        for (ResultadoPotencialIndividual rpi : potenciales)
            guardarCirculo(nodoCirculos.get(rpi.getParticulaFuente()));
    }

    private void guardarCirculo(Circle c) {
        if (c != null && !estadosOriginales.containsKey(c))
            estadosOriginales.put(c, new EstadoCirculo(
                    (Color) c.getFill(), (Color) c.getStroke(),
                    c.getRadius(), c.getStrokeWidth()));
    }

    private void aplicarEstadoOrigen() {
        Circle c = nodoCirculos.get(resultado.getParticulaOrigen());
        if (c == null)
            return;
        c.setFill(COLOR_ORIGEN);
        c.setStroke(Color.web("#b71c1c"));
        c.setStrokeWidth(3);
        c.setRadius(20);

        if (haloOrigen != null) {
            grafoPane.getChildren().remove(haloOrigen);
        }
        if (timelineHalo != null)
            timelineHalo.stop();

        haloOrigen = new Circle(c.getCenterX(), c.getCenterY(), 26);
        haloOrigen.setFill(Color.TRANSPARENT);
        haloOrigen.setStroke(COLOR_ORIGEN.deriveColor(0, 1, 1, 0.55));
        haloOrigen.setStrokeWidth(2.5);
        haloOrigen.setUserData("animacion");
        haloOrigen.setMouseTransparent(true);
        grafoPane.getChildren().add(0, haloOrigen);
        nodosAnimacion.add(haloOrigen);

        timelineHalo = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(haloOrigen.radiusProperty(), 22),
                        new KeyValue(haloOrigen.opacityProperty(), 0.7)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(haloOrigen.radiusProperty(), 34),
                        new KeyValue(haloOrigen.opacityProperty(), 0.0)));
        timelineHalo.setCycleCount(Timeline.INDEFINITE);
        timelineHalo.play();
    }

    public void restaurarCausantesANeutro() {
        for (ResultadoPotencialIndividual rpi : potenciales) {
            Circle c = nodoCirculos.get(rpi.getParticulaFuente());
            if (c == null)
                continue;
            EstadoCirculo est = estadosOriginales.get(c);
            if (est != null) {
                c.setFill(est.fill());
                c.setStroke(est.stroke());
                c.setRadius(est.radio());
                c.setStrokeWidth(est.strokeWidth());
            }
        }
    }

    private void aplicarEstadoCausanteActivo(ResultadoPotencialIndividual rpi, Color color) {
        Circle c = nodoCirculos.get(rpi.getParticulaFuente());
        if (c == null)
            return;
        c.setFill(color.deriveColor(0, 0.8, 1.2, 1));
        c.setStroke(color.darker());
        c.setStrokeWidth(3);
        c.setRadius(20);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), c);
        st.setFromX(0.8);
        st.setFromY(0.8);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    private void restaurarCirculos() {
        for (Map.Entry<Circle, EstadoCirculo> e : estadosOriginales.entrySet()) {
            Circle c = e.getKey();
            EstadoCirculo est = e.getValue();
            c.setFill(est.fill());
            c.setStroke(est.stroke());
            c.setRadius(est.radio());
            c.setStrokeWidth(est.strokeWidth());
        }
    }

    private void mostrarEtiquetaOrigen() {
        Nodo origen = resultado.getParticulaOrigen();
        Circle c = nodoCirculos.get(origen);
        if (c == null)
            return;

        double q0 = Math.abs(origen.getValorCarga());
        if ("-".equals(origen.getTipoCarga()))
            q0 = -q0;

        String texto = "Partícula de análisis\n"
                + origen.getNombre() + "  |  q₀ = "
                + q0 + " µC";
        mostrarEtiqueta(texto, c.getCenterX() + 28, c.getCenterY() - 10, COLOR_ORIGEN, true);
    }

    private void dibujarVectorR(ResultadoPotencialIndividual rpi, Color color) {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        Circle cC = nodoCirculos.get(rpi.getParticulaFuente());
        if (cO == null || cC == null)
            return;

        double ox = cO.getCenterX(), oy = cO.getCenterY();
        double cx = cC.getCenterX(), cy = cC.getCenterY();
        double mx = (ox + cx) / 2, my = (oy + cy) / 2;

        Line linea = new Line(ox, oy, cx, cy);
        linea.setStroke(color);
        linea.setStrokeWidth(2);
        linea.getStrokeDashArray().addAll(10.0, 6.0);
        linea.setMouseTransparent(true);
        agregarNodo(linea);
        fadeIn(linea, 0);

        String dist = String.format("r = %.4f %s\n  = %.4e m",
                rpi.getDistanciaUnidades(), unidad.getSimbolo(), rpi.getDistanciaMetros());
        mostrarEtiqueta(dist, mx + 10, my - 8, color, false);
    }

    private void mostrarFormulaV(ResultadoPotencialIndividual rpi, Color color) {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        Circle cC = nodoCirculos.get(rpi.getParticulaFuente());
        if (cO == null || cC == null)
            return;

        double ox = cO.getCenterX(), oy = cO.getCenterY();
        double cx = cC.getCenterX(), cy = cC.getCenterY();
        double mx = (ox + cx) / 2, my = (oy + cy) / 2;

        double fx = mx > grafoPane.getWidth() / 2 ? mx - 210 : mx + 15;
        double fy = my > grafoPane.getHeight() / 2 ? my - 105 : my + 10;

        double q1 = Math.abs(rpi.getParticulaFuente().getValorCarga());
        if ("-".equals(rpi.getParticulaFuente().getTipoCarga()))
            q1 = -q1;

        String formula = String.format(
                "V = k · q₁ / r\n" +
                        "  = 8.99×10⁹ · (%.2e)\n" +
                        "           / (%.4e)\n" +
                        "  = %.4e V",
                q1 * 1e-6,
                rpi.getDistanciaMetros(),
                rpi.getPotencialV());

        mostrarCajaFormula(formula, fx, fy, color, "Potencial");
    }

    private void dibujarTodasLineasR() {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        if (cO == null)
            return;
        for (ResultadoPotencialIndividual rpi : potenciales) {
            Circle cC = nodoCirculos.get(rpi.getParticulaFuente());
            if (cC == null)
                continue;
            Line l = new Line(cO.getCenterX(), cO.getCenterY(), cC.getCenterX(), cC.getCenterY());
            l.setStroke(Color.web("#9e9e9e"));
            l.setStrokeWidth(1);
            l.getStrokeDashArray().addAll(6.0, 4.0);
            l.setMouseTransparent(true);
            agregarNodo(l);
            fadeIn(l, 0);
        }
    }

    private void mostrarPotencialTotal() {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        if (cO == null)
            return;
        String txt = String.format("Suma Escalar (Potencial Total)\nV_total = %.6e V", resultado.getPotencialTotalV());
        mostrarEtiqueta(txt, cO.getCenterX() + 20, cO.getCenterY() - 60, COLOR_RESULTADO, true);
    }

    private void mostrarEnergiaPotencial() {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        if (cO == null)
            return;
        double ox = cO.getCenterX(), oy = cO.getCenterY();
        double posX = ox > grafoPane.getWidth() / 2 ? ox - 225 : ox + 25;
        double posY = oy > grafoPane.getHeight() / 2 ? oy - 120 : oy + 30;

        double q0 = Math.abs(resultado.getParticulaOrigen().getValorCarga());
        if ("-".equals(resultado.getParticulaOrigen().getTipoCarga()))
            q0 = -q0;

        String txt = String.format(
                "Energía Potencial (U)\n" +
                        "───────────────────\n" +
                        "U = q₀ · V_total\n" +
                        "  = (%.4e) · (%.4e)\n" +
                        "  = %.4e J",
                q0 * 1e-6,
                resultado.getPotencialTotalV(),
                resultado.getEnergiaTotalU());
        mostrarCajaFormula(txt, posX, posY, Color.web("#1565c0"), "Energía U");
    }

    private void mostrarEtiqueta(String texto, double x, double y, Color color, boolean oscuro) {
        String[] lineas = texto.split("\n");
        double lineH = 16, padH = 8, padV = 6;
        double maxW = 0;
        for (String l : lineas)
            maxW = Math.max(maxW, l.length() * 7.0);

        Rectangle fondo = new Rectangle(x - padH, y - padV - 12, maxW + padH * 2, lineas.length * lineH + padV * 2);
        fondo.setArcWidth(10);
        fondo.setArcHeight(10);
        fondo.setFill(oscuro ? color.deriveColor(0, 1, 0.3, 0.93) : Color.WHITE.deriveColor(0, 1, 1, 0.93));
        fondo.setStroke(color);
        fondo.setStrokeWidth(1.5);
        fondo.setMouseTransparent(true);
        agregarNodo(fondo);
        fadeIn(fondo, 0);

        for (int i = 0; i < lineas.length; i++) {
            Text t = new Text(x, y + i * lineH, lineas[i]);
            t.setFont(Font.font("Courier New", FontWeight.NORMAL, 11));
            t.setFill(oscuro ? Color.WHITE : color.darker());
            t.setMouseTransparent(true);
            agregarNodo(t);
            fadeIn(t, 60 + i * 40);
        }
    }

    private void mostrarCajaFormula(String texto, double x, double y, Color color, String badge) {
        String[] lineas = texto.split("\n");
        double lineH = 17, padH = 10, padV = 8;
        double maxW = 0;
        for (String l : lineas)
            maxW = Math.max(maxW, l.length() * 7.0);
        double totalH = lineas.length * lineH + padV * 2;

        Rectangle sombra = new Rectangle(x - padH + 3, y - padV - 13, maxW + padH * 2, totalH + 2);
        sombra.setArcWidth(12);
        sombra.setArcHeight(12);
        sombra.setFill(Color.web("#00000018"));
        sombra.setMouseTransparent(true);
        agregarNodo(sombra);
        fadeIn(sombra, 0);

        Rectangle fondo = new Rectangle(x - padH, y - padV - 13, maxW + padH * 2, totalH);
        fondo.setArcWidth(12);
        fondo.setArcHeight(12);
        fondo.setFill(Color.WHITE);
        fondo.setStroke(color);
        fondo.setStrokeWidth(2);
        fondo.setMouseTransparent(true);
        agregarNodo(fondo);
        fadeIn(fondo, 40);

        Rectangle barra = new Rectangle(x - padH, y - padV - 13, maxW + padH * 2, 5);
        barra.setArcWidth(12);
        barra.setArcHeight(12);
        barra.setFill(color);
        barra.setMouseTransparent(true);
        agregarNodo(barra);
        fadeIn(barra, 70);

        if (badge != null && !badge.isEmpty()) {
            double bw = badge.length() * 7.5 + 12;
            Rectangle br = new Rectangle(x - padH + 8, y - padV - 25, bw, 14);
            br.setArcWidth(7);
            br.setArcHeight(7);
            br.setFill(color);
            br.setMouseTransparent(true);
            agregarNodo(br);
            fadeIn(br, 90);

            Text bt = new Text(x - padH + 13, y - padV - 13, badge);
            bt.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
            bt.setFill(Color.WHITE);
            bt.setMouseTransparent(true);
            agregarNodo(bt);
            fadeIn(bt, 110);
        }

        for (int i = 0; i < lineas.length; i++) {
            boolean esHeader = lineas[i].startsWith("─") || lineas[i].startsWith("━")
                    || (lineas[i].length() > 3 && lineas[i].equals(lineas[i].toUpperCase()));
            Text t = new Text(x, y + i * lineH, lineas[i]);
            t.setFont(Font.font("Courier New", esHeader ? FontWeight.BOLD : FontWeight.NORMAL, esHeader ? 10 : 11));
            t.setFill(esHeader ? color.darker() : Color.web("#004d40"));
            t.setMouseTransparent(true);
            agregarNodo(t);
            fadeIn(t, 140 + i * 50);
        }
    }

    private void agregarNodo(javafx.scene.Node n) {
        grafoPane.getChildren().add(n);
        nodosAnimacion.add(n);
    }

    private void limpiarAnimacion() {
        if (timelineHalo != null) {
            timelineHalo.stop();
            timelineHalo = null;
        }
        if (haloOrigen != null) {
            grafoPane.getChildren().remove(haloOrigen);
            haloOrigen = null;
        }
        grafoPane.getChildren().removeAll(nodosAnimacion);
        nodosAnimacion.clear();
    }

    private void fadeIn(javafx.scene.Node node, int delayMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}
