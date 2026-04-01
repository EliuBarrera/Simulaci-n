package com.usta.controllers;

import com.usta.models.Nodo;
import com.usta.models.ResultadoCalculo;
import com.usta.models.ResultadoFuerza;
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
 * Animación paso a paso de la Ley de Coulomb.
 *
 * Opera directamente sobre los Circle del grafoPane ya existentes:
 * los modifica visualmente al entrar y los restaura al salir.
 *
 * Pasos:
 *   0        → Presentación: origen destacado, causantes en su color original
 *   1..N*3   → Por cada causante (3 sub-pasos):
 *                sub 0: vector r + distancia
 *                sub 1: fórmula F
 *                sub 2: vector fuerza individual
 *   N*3+1    → Suma vectorial + resultante
 *   N*3+2    → Campo eléctrico
 */
public class AnimacionCoulombController {

    // Paleta coherente con el CSS: variaciones de cyan/teal
    private static final Color[] PALETA = {
        Color.web("#00838f"),
        Color.web("#00acc1"),
        Color.web("#0097a7"),
        Color.web("#006064"),
        Color.web("#00bcd4"),
        Color.web("#26c6da"),
        Color.web("#4dd0e1"),
        Color.web("#80deea")
    };

    private static final Color COLOR_ORIGEN    = Color.web("#e53935");
    private static final Color COLOR_RESULTADO = Color.web("#004d40");

    // ── Estado ───────────────────────────────────────────────────────────────
    private ResultadoCalculo      resultado;
    private UnidadDistancia       unidad;
    private Pane                  grafoPane;
    private Map<Nodo, Circle>     nodoCirculos;
    private int                   pasoActual = 0;
    private int                   totalPasos = 0;
    private List<ResultadoFuerza> fuerzas;

    // Estado visual original de cada círculo para restaurar
    private record EstadoCirculo(Color fill, Color stroke, double radio, double strokeWidth) {}
    private final Map<Circle, EstadoCirculo> estadosOriginales = new HashMap<>();

    // Nodos de animación agregados al grafoPane
    private final List<javafx.scene.Node> nodosAnimacion = new ArrayList<>();

    // Halo pulsante del origen
    private Circle   haloOrigen;
    private Timeline timelineHalo;

    // =========================================================================
    // API pública
    // =========================================================================

    public void inicializar(ResultadoCalculo resultado,
                             UnidadDistancia unidad,
                             Pane grafoPane,
                             Map<Nodo, Circle> nodoCirculos) {
        this.resultado    = resultado;
        this.unidad       = unidad;
        this.grafoPane    = grafoPane;
        this.nodoCirculos = nodoCirculos;
        this.fuerzas      = resultado.getFuerzasIndividuales();
        this.pasoActual   = 0;
        this.totalPasos   = 1 + fuerzas.size() * 3 + 2;

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

    /** Restaura todos los círculos y limpia nodos de animación. Llamar al salir. */
    public void restaurar() {
        limpiarAnimacion();
        restaurarCirculos();
        estadosOriginales.clear();
        resultado = null;
    }

    public int     getPasoActual()  { return pasoActual; }
    public int     getTotalPasos()  { return totalPasos; }
    public boolean hayResultado()   { return resultado != null; }

    // =========================================================================
    // Renderizado por paso
    // =========================================================================

    private void renderizarPaso() {
        int n = fuerzas.size();

        aplicarEstadoOrigen();
        restaurarCausantesANeutro();

        if (pasoActual == 0) {
            mostrarEtiquetaOrigen();

        } else if (pasoActual >= 1 && pasoActual <= n * 3) {
            int idx    = (pasoActual - 1) / 3;
            int subPas = (pasoActual - 1) % 3;
            Color color = PALETA[idx % PALETA.length];
            ResultadoFuerza rf = fuerzas.get(idx);

            aplicarEstadoCausanteActivo(rf, color);

            switch (subPas) {
                case 0 -> dibujarVectorR(rf, color);
                case 1 -> { dibujarVectorR(rf, color); mostrarFormulaF(rf, color); }
                case 2 -> { dibujarVectorR(rf, color); mostrarFormulaF(rf, color);
                            dibujarVectorFuerza(rf, color); }
            }

        } else if (pasoActual == n * 3 + 1) {
            dibujarTodasLineasR();
            dibujarVectorResultante();

        } else if (pasoActual == n * 3 + 2) {
            dibujarTodasLineasR();
            dibujarVectorResultante();
            mostrarCampoElectrico();
        }
    }

    // =========================================================================
    // Modificación visual de círculos existentes
    // =========================================================================

    private void guardarEstadosOriginales() {
        estadosOriginales.clear();
        Nodo origen = resultado.getParticulaOrigen();
        guardarCirculo(nodoCirculos.get(origen));
        for (ResultadoFuerza rf : fuerzas)
            guardarCirculo(nodoCirculos.get(rf.getParticulaCausante()));
    }

    private void guardarCirculo(Circle c) {
        if (c != null && !estadosOriginales.containsKey(c))
            estadosOriginales.put(c, new EstadoCirculo(
                (Color) c.getFill(), (Color) c.getStroke(),
                c.getRadius(), c.getStrokeWidth()));
    }

    private void aplicarEstadoOrigen() {
        Circle c = nodoCirculos.get(resultado.getParticulaOrigen());
        if (c == null) return;
        c.setFill(COLOR_ORIGEN);
        c.setStroke(Color.web("#b71c1c"));
        c.setStrokeWidth(3);
        c.setRadius(20);

        // Halo pulsante
        if (haloOrigen != null) { grafoPane.getChildren().remove(haloOrigen); }
        if (timelineHalo != null) timelineHalo.stop();

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
                new KeyValue(haloOrigen.opacityProperty(), 0.0))
        );
        timelineHalo.setCycleCount(Timeline.INDEFINITE);
        timelineHalo.play();
    }

    public void restaurarCausantesANeutro() {
        for (ResultadoFuerza rf : fuerzas) {
            Circle c = nodoCirculos.get(rf.getParticulaCausante());
            if (c == null) continue;
            EstadoCirculo est = estadosOriginales.get(c);
            if (est != null) {
                c.setFill(est.fill());
                c.setStroke(est.stroke());
                c.setRadius(est.radio());
                c.setStrokeWidth(est.strokeWidth());
            }
        }
    }

    private void aplicarEstadoCausanteActivo(ResultadoFuerza rf, Color color) {
        Circle c = nodoCirculos.get(rf.getParticulaCausante());
        if (c == null) return;
        c.setFill(color.deriveColor(0, 0.8, 1.2, 1));
        c.setStroke(color.darker());
        c.setStrokeWidth(3);
        c.setRadius(20);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), c);
        st.setFromX(0.8); st.setFromY(0.8);
        st.setToX(1.0);   st.setToY(1.0);
        st.play();
    }

    private void restaurarCirculos() {
        for (Map.Entry<Circle, EstadoCirculo> e : estadosOriginales.entrySet()) {
            Circle c = e.getKey(); EstadoCirculo est = e.getValue();
            c.setFill(est.fill()); c.setStroke(est.stroke());
            c.setRadius(est.radio()); c.setStrokeWidth(est.strokeWidth());
        }
    }

    // =========================================================================
    // Paso 0: etiqueta de presentación
    // =========================================================================

    private void mostrarEtiquetaOrigen() {
        Nodo origen = resultado.getParticulaOrigen();
        Circle c = nodoCirculos.get(origen);
        if (c == null) return;

        String texto = "Partícula de análisis\n"
            + origen.getNombre() + "  |  q₀ = "
            + origen.getValorCarga() + " µC (" + origen.getTipoCarga() + ")";
        mostrarEtiqueta(texto, c.getCenterX() + 28, c.getCenterY() - 10,
            COLOR_ORIGEN, true);
    }

    // =========================================================================
    // Sub-pasos de fuerzas individuales
    // =========================================================================

    private void dibujarVectorR(ResultadoFuerza rf, Color color) {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        Circle cC = nodoCirculos.get(rf.getParticulaCausante());
        if (cO == null || cC == null) return;

        double ox = cO.getCenterX(), oy = cO.getCenterY();
        double cx = cC.getCenterX(), cy = cC.getCenterY();
        double mx = (ox + cx) / 2,   my = (oy + cy) / 2;

        Line linea = new Line(ox, oy, cx, cy);
        linea.setStroke(color); linea.setStrokeWidth(2);
        linea.getStrokeDashArray().addAll(10.0, 6.0);
        linea.setMouseTransparent(true);
        agregarNodo(linea); fadeIn(linea, 0);

        String dist = String.format("r = %.4f %s\n  = %.4e m",
            rf.getDistanciaEnUnidad(), unidad.getSimbolo(), rf.getDistanciaEnMetros());
        mostrarEtiqueta(dist, mx + 10, my - 8, color, false);
    }

    private void mostrarFormulaF(ResultadoFuerza rf, Color color) {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        Circle cC = nodoCirculos.get(rf.getParticulaCausante());
        if (cO == null || cC == null) return;

        double ox = cO.getCenterX(), oy = cO.getCenterY();
        double cx = cC.getCenterX(), cy = cC.getCenterY();
        double mx = (ox + cx) / 2,   my = (oy + cy) / 2;

        double fx = mx > grafoPane.getWidth()  / 2 ? mx - 210 : mx + 15;
        double fy = my > grafoPane.getHeight() / 2 ? my - 105 : my + 10;

        String formula;
        if (resultado.isEs3D()) {
            formula = String.format(
                "F = k · |q₀ · q₁| / r²\n" +
                "  = 8.99×10⁹·|%.2e·%.2e|\n" +
                "           / (%.4e)²\n" +
                "  = %.4e N\n" +
                "θ  = %.2f°  φ = %.2f°\n" +
                "Fx = %.3e N\n" +
                "Fy = %.3e N\n" +
                "Fz = %.3e N",
                resultado.getParticulaOrigen().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getDistanciaEnMetros(),
                rf.getMagnitud(), rf.getAnguloDeg(), rf.getAnguloElevacionDeg(),
                rf.getFx(), rf.getFy(), rf.getFz());
        } else {
            formula = String.format(
                "F = k · |q₀ · q₁| / r²\n" +
                "  = 8.99×10⁹·|%.2e·%.2e|\n" +
                "           / (%.4e)²\n" +
                "  = %.4e N\n" +
                "θ  = %.2f°\n" +
                "Fx = %.3e N\n" +
                "Fy = %.3e N",
                resultado.getParticulaOrigen().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getDistanciaEnMetros(),
                rf.getMagnitud(), rf.getAnguloDeg(),
                rf.getFx(), rf.getFy());
        }

        mostrarCajaFormula(formula, fx, fy, color,
            rf.isEsRepulsion() ? "Repulsión" : "Atracción");
    }

    private void dibujarVectorFuerza(ResultadoFuerza rf, Color color) {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        if (cO == null) return;
        double mag = rf.getMagnitud();
        if (mag == 0) return;

        dibujarFlecha(cO.getCenterX(), cO.getCenterY(),
            rf.getFx() / mag, -rf.getFy() / mag,
            90, color, 2.5, 14,
            String.format("|F| = %.3e N", mag));
    }

    // =========================================================================
    // Pasos finales
    // =========================================================================

    private void dibujarTodasLineasR() {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        if (cO == null) return;
        for (ResultadoFuerza rf : fuerzas) {
            Circle cC = nodoCirculos.get(rf.getParticulaCausante());
            if (cC == null) continue;
            Line l = new Line(cO.getCenterX(), cO.getCenterY(),
                              cC.getCenterX(), cC.getCenterY());
            l.setStroke(Color.web("#9e9e9e")); l.setStrokeWidth(1);
            l.getStrokeDashArray().addAll(6.0, 4.0);
            l.setMouseTransparent(true);
            agregarNodo(l); fadeIn(l, 0);
        }
    }

    private void dibujarVectorResultante() {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        if (cO == null) return;
        double ftx = resultado.getFuerzaTotalX(), fty = resultado.getFuerzaTotalY();
        double mag  = resultado.getFuerzaTotal();
        if (mag == 0) return;

        dibujarFlecha(cO.getCenterX(), cO.getCenterY(),
            ftx / mag, -fty / mag, 110, COLOR_RESULTADO, 4, 18, null);

        String txt;
        if (resultado.isEs3D()) {
            txt = String.format(
                "Suma vectorial\nΣFx = %.4e N\nΣFy = %.4e N\nΣFz = %.4e N\n|F|  = %.4e N\nθ=%.2f° φ=%.2f°",
                ftx, fty, resultado.getFuerzaTotalZ(), mag, resultado.getAnguloResultante(), resultado.getAnguloElevacionResultante());
        } else {
            txt = String.format(
                "Suma vectorial\nΣFx = %.4e N\nΣFy = %.4e N\n|F|  = %.4e N\nθ    = %.2f°",
                ftx, fty, mag, resultado.getAnguloResultante());
        }
        mostrarEtiqueta(txt, cO.getCenterX() + 20, cO.getCenterY() - 115,
            COLOR_RESULTADO, true);
    }

    private void mostrarCampoElectrico() {
        Circle cO = nodoCirculos.get(resultado.getParticulaOrigen());
        if (cO == null) return;
        double ox = cO.getCenterX(), oy = cO.getCenterY();
        double posX = ox > grafoPane.getWidth()  / 2 ? ox - 225 : ox + 25;
        double posY = oy > grafoPane.getHeight() / 2 ? oy - 120 : oy + 30;

        String txt = String.format(
            "Campo Eléctrico\n" +
            "───────────────────\n" +
            "E = F / |q₀|\n" +
            "  = %.4e / %.4e\n" +
            "  = %.4e N/C",
            resultado.getFuerzaTotal(),
            Math.abs(resultado.getParticulaOrigen().getValorCarga() * 1e-6),
            resultado.getCampoElectrico());
        mostrarCajaFormula(txt, posX, posY, Color.web("#00695c"), "Campo E");
    }

    // =========================================================================
    // Primitivas de dibujo
    // =========================================================================

    private void dibujarFlecha(double sx, double sy,
                                double dirX, double dirY,
                                double len, Color color,
                                double grosor, double head, String etiqueta) {
        double ex = sx + dirX * len, ey = sy + dirY * len;
        double ang = Math.atan2(ey - sy, ex - sx);

        Line cuerpo = new Line(sx, sy, ex, ey);
        cuerpo.setStroke(color); cuerpo.setStrokeWidth(grosor);
        cuerpo.setMouseTransparent(true);
        agregarNodo(cuerpo); fadeIn(cuerpo, 0);

        Line h1 = new Line(ex, ey,
            ex - head * Math.cos(ang - Math.PI / 7),
            ey - head * Math.sin(ang - Math.PI / 7));
        Line h2 = new Line(ex, ey,
            ex - head * Math.cos(ang + Math.PI / 7),
            ey - head * Math.sin(ang + Math.PI / 7));
        for (Line h : new Line[]{h1, h2}) {
            h.setStroke(color); h.setStrokeWidth(grosor);
            h.setMouseTransparent(true);
            agregarNodo(h); fadeIn(h, 80);
        }

        if (etiqueta != null) {
            Text lbl = new Text(etiqueta);
            lbl.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
            lbl.setFill(color.darker());
            lbl.setX(ex + (dirX >= 0 ? 8 : -130));
            lbl.setY(ey + (dirY >= 0 ? 16 : -8));
            lbl.setMouseTransparent(true);
            agregarNodo(lbl); fadeIn(lbl, 160);
        }
    }

    private void mostrarEtiqueta(String texto, double x, double y,
                                  Color color, boolean oscuro) {
        String[] lineas = texto.split("\n");
        double lineH = 16, padH = 8, padV = 6;
        double maxW = 0;
        for (String l : lineas) maxW = Math.max(maxW, l.length() * 7.0);

        Rectangle fondo = new Rectangle(x - padH, y - padV - 12,
            maxW + padH * 2, lineas.length * lineH + padV * 2);
        fondo.setArcWidth(10); fondo.setArcHeight(10);
        fondo.setFill(oscuro
            ? color.deriveColor(0, 1, 0.3, 0.93)
            : Color.WHITE.deriveColor(0, 1, 1, 0.93));
        fondo.setStroke(color); fondo.setStrokeWidth(1.5);
        fondo.setMouseTransparent(true);
        agregarNodo(fondo); fadeIn(fondo, 0);

        for (int i = 0; i < lineas.length; i++) {
            Text t = new Text(x, y + i * lineH, lineas[i]);
            t.setFont(Font.font("Courier New", FontWeight.NORMAL, 11));
            t.setFill(oscuro ? Color.WHITE : color.darker());
            t.setMouseTransparent(true);
            agregarNodo(t); fadeIn(t, 60 + i * 40);
        }
    }

    private void mostrarCajaFormula(String texto, double x, double y,
                                     Color color, String badge) {
        String[] lineas = texto.split("\n");
        double lineH = 17, padH = 10, padV = 8;
        double maxW = 0;
        for (String l : lineas) maxW = Math.max(maxW, l.length() * 7.0);
        double totalH = lineas.length * lineH + padV * 2;

        // Sombra
        Rectangle sombra = new Rectangle(x - padH + 3, y - padV - 13,
            maxW + padH * 2, totalH + 2);
        sombra.setArcWidth(12); sombra.setArcHeight(12);
        sombra.setFill(Color.web("#00000018"));
        sombra.setMouseTransparent(true);
        agregarNodo(sombra); fadeIn(sombra, 0);

        // Fondo blanco (estilo .vbox del CSS)
        Rectangle fondo = new Rectangle(x - padH, y - padV - 13,
            maxW + padH * 2, totalH);
        fondo.setArcWidth(12); fondo.setArcHeight(12);
        fondo.setFill(Color.WHITE);
        fondo.setStroke(color); fondo.setStrokeWidth(2);
        fondo.setMouseTransparent(true);
        agregarNodo(fondo); fadeIn(fondo, 40);

        // Barra superior
        Rectangle barra = new Rectangle(x - padH, y - padV - 13,
            maxW + padH * 2, 5);
        barra.setArcWidth(12); barra.setArcHeight(12);
        barra.setFill(color);
        barra.setMouseTransparent(true);
        agregarNodo(barra); fadeIn(barra, 70);

        // Badge
        if (badge != null && !badge.isEmpty()) {
            double bw = badge.length() * 7.5 + 12;
            Rectangle br = new Rectangle(x - padH + 8, y - padV - 25, bw, 14);
            br.setArcWidth(7); br.setArcHeight(7);
            br.setFill(color); br.setMouseTransparent(true);
            agregarNodo(br); fadeIn(br, 90);

            Text bt = new Text(x - padH + 13, y - padV - 13, badge);
            bt.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
            bt.setFill(Color.WHITE); bt.setMouseTransparent(true);
            agregarNodo(bt); fadeIn(bt, 110);
        }

        // Texto
        for (int i = 0; i < lineas.length; i++) {
            boolean esHeader = lineas[i].startsWith("─") || lineas[i].startsWith("━")
                || (lineas[i].length() > 3 && lineas[i].equals(lineas[i].toUpperCase()));
            Text t = new Text(x, y + i * lineH, lineas[i]);
            t.setFont(Font.font("Courier New",
                esHeader ? FontWeight.BOLD : FontWeight.NORMAL,
                esHeader ? 10 : 11));
            t.setFill(esHeader ? color.darker() : Color.web("#004d40"));
            t.setMouseTransparent(true);
            agregarNodo(t); fadeIn(t, 140 + i * 50);
        }
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private void agregarNodo(javafx.scene.Node n) {
        grafoPane.getChildren().add(n);
        nodosAnimacion.add(n);
    }

    private void limpiarAnimacion() {
        if (timelineHalo != null) { timelineHalo.stop(); timelineHalo = null; }
        if (haloOrigen   != null) { grafoPane.getChildren().remove(haloOrigen); haloOrigen = null; }
        grafoPane.getChildren().removeAll(nodosAnimacion);
        nodosAnimacion.clear();
    }

    private void fadeIn(javafx.scene.Node node, int delayMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }
}