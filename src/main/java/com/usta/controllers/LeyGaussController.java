package com.usta.controllers;

import com.usta.App;
import com.usta.models.FiguraGauss;
import com.usta.models.ResultadoGauss;
import com.usta.models.TipoFigura;
import com.usta.utils.GaussCalculator;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del simulador de Ley de Gauss en 2D.
 *
 * Flujo de uso:
 *  1. El usuario selecciona una figura del menú lateral.
 *  2. Arrastra el mouse en el canvas para dibujarla.
 *  3. Ingresa carga y signo, presiona "Confirmar figura".
 *  4. Selecciona la figura de la superficie gaussiana y la dibuja.
 *  5. Presiona "Calcular" para ver el resultado.
 *  6. Puede activar "Ver líneas de campo" sobre la superficie gaussiana.
 *
 * Coordenadas: el canvas usa Y+ hacia abajo (pantalla).
 *              El plano matemático se muestra con Y+ hacia arriba (etiquetas).
 */
public class LeyGaussController {

    // ── Constantes de layout ──────────────────────────────────────────────────
    private static final double MARGIN      = 50.0;
    private static final double PX_POR_UNIT = 80.0;   // 80px = 1 metro (ajustable)
    private static final double CANVAS_W    = 1400.0;
    private static final double CANVAS_H    = 900.0;

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Canvas        canvasGauss;
    @FXML private Pane          gaussPane;

    // Panel izquierdo — figura cargada
    @FXML private ComboBox<TipoFigura> figuraCargadaCombo;
    @FXML private TextField            cargaField;
    @FXML private ComboBox<String>     signoCargaCombo;
    @FXML private Button               btnConfirmarFigura;
    @FXML private Label                lblEstadoFigura;

    // Panel izquierdo — superficie gaussiana
    @FXML private ComboBox<TipoFigura> superficieCombo;
    @FXML private Button               btnConfirmarSuperficie;
    @FXML private Label                lblEstadoSuperficie;

    // Panel izquierdo — acciones
    @FXML private Button    btnCalcular;
    @FXML private Button    btnLimpiar;
    @FXML private CheckBox  chkLineasCampo;

    // Panel resultados
    @FXML private Label     lblQenc;
    @FXML private Label     lblFlujo;
    @FXML private Label     lblCampoE;
    @FXML private Label     lblFraccion;
    @FXML private TextArea  txtProcedimiento;

    // ── Estado ────────────────────────────────────────────────────────────────
    private enum Modo { NADA, DIBUJANDO_FIGURA, DIBUJANDO_SUPERFICIE, MOVIENDO }

    private Modo            modoActual        = Modo.NADA;
    private boolean         esperandoDibujo   = false;   // true tras confirmar figura/superficie
    private boolean         figuraConfirmada  = false;
    private boolean         superficieConfirmada = false;

    private FiguraGauss     figuraCargada     = null;
    private FiguraGauss     superficieGauss   = null;
    private ResultadoGauss  ultimoResultado   = null;

    // Dibujo temporal (drag)
    private double dragStartX, dragStartY;
    private double dragCurrentX, dragCurrentY;

    // Para mover figuras
    private FiguraGauss figuraMoviendo   = null;
    private double      offsetMoveX, offsetMoveY;

    // Contadores para nombres
    private int contadorFiguras = 1;

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================
    public void initialize() {
        // ComboBoxes
        figuraCargadaCombo.getItems().addAll(TipoFigura.values());
        figuraCargadaCombo.setValue(TipoFigura.CIRCULO);

        superficieCombo.getItems().addAll(TipoFigura.values());
        superficieCombo.setValue(TipoFigura.CIRCULO);

        signoCargaCombo.getItems().addAll("+", "-");
        signoCargaCombo.setValue("+");

        // Canvas
        canvasGauss.setWidth(CANVAS_W);
        canvasGauss.setHeight(CANVAS_H);

        // Listeners del canvas
        canvasGauss.setOnMousePressed(this::onMousePressed);
        canvasGauss.setOnMouseDragged(this::onMouseDragged);
        canvasGauss.setOnMouseReleased(this::onMouseReleased);
        canvasGauss.setOnMouseMoved(this::onMouseMoved);

        // CheckBox líneas de campo
        chkLineasCampo.selectedProperty().addListener((obs, o, n) -> redibujar());

        // Estado inicial
        actualizarEstadoBotones();
        dibujar(null, null);
    }

    // =========================================================================
    // ACCIONES DEL PANEL LATERAL
    // =========================================================================

    /** El usuario quiere dibujar la figura cargada. */
    @FXML
    private void onIniciarDibujoFigura() {
        String cargaStr = cargaField.getText().trim();
        if (cargaStr.isEmpty()) {
            mostrarAlerta("Falta dato", "Ingrese el valor de la carga antes de dibujar.");
            return;
        }
        try {
            double q = Double.parseDouble(cargaStr);
            if (q <= 0) { mostrarAlerta("Error", "La carga debe ser un valor positivo."); return; }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La carga debe ser un número válido."); return;
        }

        modoActual      = Modo.DIBUJANDO_FIGURA;
        esperandoDibujo = true;
        lblEstadoFigura.setText("Dibuja la figura en el plano →");
        canvasGauss.setCursor(Cursor.CROSSHAIR);
    }

    /** El usuario quiere dibujar la superficie gaussiana. */
    @FXML
    private void onIniciarDibujoSuperficie() {
        if (figuraCargada == null) {
            mostrarAlerta("Sin figura", "Primero confirma la figura cargada.");
            return;
        }
        modoActual      = Modo.DIBUJANDO_SUPERFICIE;
        esperandoDibujo = true;
        lblEstadoSuperficie.setText("Dibuja la superficie en el plano →");
        canvasGauss.setCursor(Cursor.CROSSHAIR);
    }

    @FXML
    private void onCalcular() {
        if (figuraCargada == null || superficieGauss == null) {
            mostrarAlerta("Incompleto", "Necesitas una figura cargada y una superficie gaussiana.");
            return;
        }
        GaussCalculator calc = new GaussCalculator(PX_POR_UNIT);
        ultimoResultado = calc.calcular(figuraCargada, superficieGauss);
        mostrarResultados(ultimoResultado);
        redibujar();
    }

    @FXML
    private void onLimpiar() {
        figuraCargada        = null;
        superficieGauss      = null;
        ultimoResultado      = null;
        figuraConfirmada     = false;
        superficieConfirmada = false;
        modoActual           = Modo.NADA;
        esperandoDibujo      = false;
        contadorFiguras      = 1;

        lblEstadoFigura.setText("Sin figura");
        lblEstadoSuperficie.setText("Sin superficie");
        lblQenc.setText("—");
        lblFlujo.setText("—");
        lblCampoE.setText("—");
        lblFraccion.setText("—");
        if (txtProcedimiento != null) txtProcedimiento.clear();
        chkLineasCampo.setSelected(false);

        canvasGauss.setCursor(Cursor.DEFAULT);
        actualizarEstadoBotones();
        dibujar(null, null);
    }

    // =========================================================================
    // EVENTOS DEL CANVAS
    // =========================================================================

    private void onMousePressed(MouseEvent e) {
        double px = e.getX(), py = e.getY();

        if (esperandoDibujo) {
            dragStartX   = px;
            dragStartY   = py;
            dragCurrentX = px;
            dragCurrentY = py;
            return;
        }

        // Modo mover: detectar si se clickea sobre alguna figura
        if (figuraCargada != null && figuraCargada.contienePunto(px, py)) {
            figuraMoviendo = figuraCargada;
            offsetMoveX    = px - figuraCargada.getCx();
            offsetMoveY    = py - figuraCargada.getCy();
            modoActual     = Modo.MOVIENDO;
            canvasGauss.setCursor(Cursor.MOVE);
        } else if (superficieGauss != null && superficieGauss.contienePunto(px, py)) {
            figuraMoviendo = superficieGauss;
            offsetMoveX    = px - superficieGauss.getCx();
            offsetMoveY    = py - superficieGauss.getCy();
            modoActual     = Modo.MOVIENDO;
            canvasGauss.setCursor(Cursor.MOVE);
        }
    }

    private void onMouseDragged(MouseEvent e) {
        double px = e.getX(), py = e.getY();

        if (esperandoDibujo) {
            dragCurrentX = px;
            dragCurrentY = py;
            dibujar(buildFiguraPreview(modoActual), null);
            return;
        }

        if (modoActual == Modo.MOVIENDO && figuraMoviendo != null) {
            figuraMoviendo.setCx(px - offsetMoveX);
            figuraMoviendo.setCy(py - offsetMoveY);
            ultimoResultado = null;   // invalidar resultado al mover
            lblQenc.setText("—"); lblFlujo.setText("—");
            lblCampoE.setText("—"); lblFraccion.setText("—");
            redibujar();
        }
    }

    private void onMouseReleased(MouseEvent e) {
        if (modoActual == Modo.MOVIENDO) {
            figuraMoviendo = null;
            modoActual     = Modo.NADA;
            canvasGauss.setCursor(Cursor.DEFAULT);
            return;
        }

        if (!esperandoDibujo) return;

        double px = e.getX(), py = e.getY();
        double cx  = (dragStartX + px) / 2;
        double cy  = (dragStartY + py) / 2;
        double dx  = Math.abs(px - dragStartX) / 2;
        double dy  = Math.abs(py - dragStartY) / 2;
        double dim = Math.max(dx, dy);

        if (dim < 10) {
            lblEstadoFigura.setText("Figura demasiado pequeña, intenta de nuevo.");
            esperandoDibujo = false;
            modoActual      = Modo.NADA;
            canvasGauss.setCursor(Cursor.DEFAULT);
            dibujar(null, null);
            return;
        }

        if (modoActual == Modo.DIBUJANDO_FIGURA) {
            TipoFigura tipo = figuraCargadaCombo.getValue();
            double p1 = (tipo == TipoFigura.RECTANGULO) ? dx : dim;
            double p2 = (tipo == TipoFigura.RECTANGULO || tipo == TipoFigura.TRIANGULO) ? dy : dim;
            double q  = Double.parseDouble(cargaField.getText().trim());
            String s  = signoCargaCombo.getValue();

            figuraCargada    = new FiguraGauss(tipo, cx, cy, p1, p2, q, s, "F" + contadorFiguras++);
            figuraConfirmada = true;
            lblEstadoFigura.setText("✔ Figura: " + tipo + " | " + q + " µC (" + s + ")");

        } else if (modoActual == Modo.DIBUJANDO_SUPERFICIE) {
            TipoFigura tipo = superficieCombo.getValue();
            double p1 = (tipo == TipoFigura.RECTANGULO) ? dx : dim;
            double p2 = (tipo == TipoFigura.RECTANGULO || tipo == TipoFigura.TRIANGULO) ? dy : dim;

            superficieGauss      = new FiguraGauss(tipo, cx, cy, p1, p2, 0, "+", "S");
            superficieConfirmada = true;
            lblEstadoSuperficie.setText("✔ Superficie: " + tipo);
        }

        esperandoDibujo = false;
        modoActual      = Modo.NADA;
        canvasGauss.setCursor(Cursor.DEFAULT);
        actualizarEstadoBotones();
        redibujar();
    }

    private void onMouseMoved(MouseEvent e) {
        // Cambiar cursor al pasar sobre figuras móviles
        if (!esperandoDibujo) {
            double px = e.getX(), py = e.getY();
            if ((figuraCargada != null && figuraCargada.contienePunto(px, py)) ||
                (superficieGauss != null && superficieGauss.contienePunto(px, py))) {
                canvasGauss.setCursor(Cursor.HAND);
            } else {
                canvasGauss.setCursor(Cursor.DEFAULT);
            }
        }
    }

    // =========================================================================
    // DIBUJO
    // =========================================================================

    /** Redibuja el canvas con el estado actual. */
    private void redibujar() {
        dibujar(null, null);
    }

    /**
     * Dibuja el canvas completo.
     * @param preview  Figura temporal durante el drag (puede ser null).
     * @param tag      "figura" o "superficie" para identificar el preview.
     */
    private void dibujar(FiguraGauss preview, String tag) {
        GraphicsContext gc = canvasGauss.getGraphicsContext2D();
        double w = canvasGauss.getWidth();
        double h = canvasGauss.getHeight();

        // ── Fondo ────────────────────────────────────────────────────────────
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);

        // ── Cuadrícula y ejes ─────────────────────────────────────────────────
        dibujarPlano(gc, w, h);

        // ── Líneas de campo (si hay resultado y checkbox activo) ──────────────
        if (chkLineasCampo.isSelected() && ultimoResultado != null && superficieGauss != null) {
            dibujarLineasCampo(gc);
        }

        // ── Figura cargada ───────────────────────────────────────────────────
        if (figuraCargada != null) {
            dibujarFigura(gc, figuraCargada, Color.web("#e53935"), Color.web("#ffcdd2"),
                true, ultimoResultado != null);
        }

        // ── Superficie gaussiana ──────────────────────────────────────────────
        if (superficieGauss != null) {
            dibujarFigura(gc, superficieGauss, Color.web("#00838f"), Color.TRANSPARENT,
                false, false);
        }

        // ── Preview durante drag ──────────────────────────────────────────────
        if (preview != null) {
            boolean esFigura = (modoActual == Modo.DIBUJANDO_FIGURA);
            Color stroke = esFigura ? Color.web("#e53935") : Color.web("#00838f");
            gc.setStroke(stroke.deriveColor(0, 1, 1, 0.7));
            gc.setLineWidth(2);
            gc.setLineDashes(8, 5);
            trazarFormaFigura(gc, preview);
            gc.setLineDashes();
        }
    }

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
        gc.strokeLine(MARGIN, h - MARGIN, w - MARGIN, h - MARGIN); // X
        gc.strokeLine(MARGIN, MARGIN, MARGIN, h - MARGIN);           // Y

        // Ticks y etiquetas
        gc.setFill(Color.web("#6B7280"));
        gc.setFont(Font.font("Courier New", 9));
        int xMax = (int)((w - 2 * MARGIN) / PX_POR_UNIT);
        for (int i = 0; i <= xMax; i++) {
            double px = MARGIN + i * PX_POR_UNIT;
            gc.strokeLine(px, h - MARGIN, px, h - MARGIN + 4);
            gc.fillText(String.valueOf(i), px - 3, h - MARGIN + 14);
        }
        int yMax = (int)((h - 2 * MARGIN) / PX_POR_UNIT);
        for (int i = 0; i <= yMax; i++) {
            double py = h - MARGIN - i * PX_POR_UNIT;
            gc.strokeLine(MARGIN - 4, py, MARGIN, py);
            gc.fillText(String.valueOf(i), MARGIN - 22, py + 4);
        }

        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        gc.fillText("X (m)", w - MARGIN + 5, h - MARGIN + 4);
        gc.fillText("Y (m)", MARGIN - 10, MARGIN - 10);
    }

    /**
     * Dibuja una figura en el canvas con su relleno, borde y etiqueta.
     */
    private void dibujarFigura(GraphicsContext gc, FiguraGauss f,
                                Color stroke, Color fill,
                                boolean mostrarCarga, boolean destacar) {
        gc.save();

        // Relleno semitransparente
        gc.setFill(fill.equals(Color.TRANSPARENT)
            ? Color.TRANSPARENT
            : fill.deriveColor(0, 1, 1, 0.25));
        rellenarFormaFigura(gc, f);

        // Borde
        gc.setStroke(stroke);
        gc.setLineWidth(destacar ? 3 : 2);
        gc.setLineDashes();
        trazarFormaFigura(gc, f);

        // Etiqueta de carga sobre la figura
        if (mostrarCarga) {
            String lbl = f.getNombre() + "\nQ = " + f.getCargaTotal()
                + " µC (" + f.getSigno() + ")";
            gc.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
            gc.setFill(stroke.darker());
            String[] lineas = lbl.split("\n");
            for (int i = 0; i < lineas.length; i++) {
                gc.fillText(lineas[i], f.getCx() + f.getParam1() + 6,
                    f.getCy() - 8 + i * 15);
            }
        } else {
            // Etiqueta de superficie gaussiana
            gc.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
            gc.setFill(stroke);
            gc.fillText("Superficie G.", f.getCx() - 40,
                f.getCy() - f.getParam1() - 8);
        }

        // Punto central
        gc.setFill(stroke);
        gc.fillOval(f.getCx() - 4, f.getCy() - 4, 8, 8);

        gc.restore();
    }

    /** Traza (stroke) el contorno de una figura según su tipo. */
    private void trazarFormaFigura(GraphicsContext gc, FiguraGauss f) {
        double cx = f.getCx(), cy = f.getCy();
        double p1 = f.getParam1(), p2 = f.getParam2();
        switch (f.getTipo()) {
            case CIRCULO    -> gc.strokeOval(cx - p1, cy - p1, 2*p1, 2*p1);
            case CUADRADO   -> gc.strokeRect(cx - p1, cy - p1, 2*p1, 2*p1);
            case RECTANGULO -> gc.strokeRect(cx - p1, cy - p2, 2*p1, 2*p2);
            case TRIANGULO  -> {
                gc.beginPath();
                gc.moveTo(cx, cy - p2);
                gc.lineTo(cx - p1, cy);
                gc.lineTo(cx + p1, cy);
                gc.closePath();
                gc.stroke();
            }
        }
    }

    /** Rellena el interior de una figura. */
    private void rellenarFormaFigura(GraphicsContext gc, FiguraGauss f) {
        double cx = f.getCx(), cy = f.getCy();
        double p1 = f.getParam1(), p2 = f.getParam2();
        switch (f.getTipo()) {
            case CIRCULO    -> gc.fillOval(cx - p1, cy - p1, 2*p1, 2*p1);
            case CUADRADO   -> gc.fillRect(cx - p1, cy - p1, 2*p1, 2*p1);
            case RECTANGULO -> gc.fillRect(cx - p1, cy - p2, 2*p1, 2*p2);
            case TRIANGULO  -> {
                gc.beginPath();
                gc.moveTo(cx, cy - p2);
                gc.lineTo(cx - p1, cy);
                gc.lineTo(cx + p1, cy);
                gc.closePath();
                gc.fill();
            }
        }
    }

    /**
     * Dibuja líneas de campo eléctrico que salen (o entran) de la figura cargada
     * y atraviesan la superficie gaussiana.
     */
    private void dibujarLineasCampo(GraphicsContext gc) {
        if (figuraCargada == null || superficieGauss == null) return;

        int    numLineas = 16;
        double cx = figuraCargada.getCx(), cy = figuraCargada.getCy();
        boolean positiva = figuraCargada.getSigno().equals("+");

        // Color de líneas: rojo saliendo (positiva), azul entrando (negativa)
        Color colorLinea = positiva ? Color.web("#e53935", 0.7) : Color.web("#1565c0", 0.7);

        gc.setStroke(colorLinea);
        gc.setLineWidth(1.2);
        gc.setLineDashes();

        for (int i = 0; i < numLineas; i++) {
            double angulo = 2 * Math.PI * i / numLineas;
            double dirX   = Math.cos(angulo);
            double dirY   = Math.sin(angulo);

            // Punto de inicio: sobre el borde de la figura cargada
            double startX = cx + dirX * figuraCargada.getParam1();
            double startY = cy + dirY * figuraCargada.getParam1();

            // Trazar hasta el borde de la superficie gaussiana (o hasta salir del canvas)
            double endX = startX, endY = startY;
            double paso = 4.0;
            int   maxPasos = 600;

            // Para figuras negativas, las líneas van hacia adentro (invertir dirección visual)
            double dX = positiva ? dirX : -dirX;
            double dY = positiva ? dirY : -dirY;

            List<double[]> puntos = new ArrayList<>();
            puntos.add(new double[]{startX, startY});

            for (int s = 0; s < maxPasos; s++) {
                double nx = endX + dX * paso;
                double ny = endY + dY * paso;

                // Detener si sale de la superficie gaussiana
                if (!superficieGauss.contienePunto(nx, ny)) break;

                endX = nx; endY = ny;
                puntos.add(new double[]{endX, endY});
            }

            if (puntos.size() > 1) {
                gc.beginPath();
                gc.moveTo(puntos.get(0)[0], puntos.get(0)[1]);
                for (int k = 1; k < puntos.size(); k++)
                    gc.lineTo(puntos.get(k)[0], puntos.get(k)[1]);
                gc.stroke();

                // Punta de flecha al final
                if (puntos.size() > 2) {
                    double[] last  = puntos.get(puntos.size() - 1);
                    double[] prev  = puntos.get(puntos.size() - 2);
                    double   aAng  = Math.atan2(last[1] - prev[1], last[0] - prev[0]);
                    double   head  = 8;
                    gc.beginPath();
                    gc.moveTo(last[0], last[1]);
                    gc.lineTo(last[0] - head * Math.cos(aAng - Math.PI/7),
                              last[1] - head * Math.sin(aAng - Math.PI/7));
                    gc.moveTo(last[0], last[1]);
                    gc.lineTo(last[0] - head * Math.cos(aAng + Math.PI/7),
                              last[1] - head * Math.sin(aAng + Math.PI/7));
                    gc.stroke();
                }
            }
        }
    }

    // =========================================================================
    // FIGURA PREVIEW durante el drag
    // =========================================================================

    private FiguraGauss buildFiguraPreview(Modo modo) {
        double cx  = (dragStartX + dragCurrentX) / 2;
        double cy  = (dragStartY + dragCurrentY) / 2;
        double dx  = Math.abs(dragCurrentX - dragStartX) / 2;
        double dy  = Math.abs(dragCurrentY - dragStartY) / 2;
        double dim = Math.max(dx, dy);

        TipoFigura tipo = (modo == Modo.DIBUJANDO_FIGURA)
            ? figuraCargadaCombo.getValue()
            : superficieCombo.getValue();

        double p1 = (tipo == TipoFigura.RECTANGULO) ? dx : dim;
        double p2 = (tipo == TipoFigura.RECTANGULO || tipo == TipoFigura.TRIANGULO) ? dy : dim;

        return new FiguraGauss(tipo, cx, cy, p1, p2, 0, "+", "preview");
    }

    // =========================================================================
    // RESULTADOS
    // =========================================================================

    private void mostrarResultados(ResultadoGauss res) {
        lblQenc.setText(fmtCientifica(res.getCargaEncerradaCoulombs()) + " C");
        lblFlujo.setText(fmtCientifica(res.getFlujoElectrico()) + " N·m²/C");
        lblCampoE.setText(fmtCientifica(res.getCampoPromedio()) + " N/C");
        lblFraccion.setText(String.format("%.1f %%", res.getFraccionEncerrada() * 100));

        if (txtProcedimiento != null)
            txtProcedimiento.setText(construirProcedimiento(res));
    }

    private String construirProcedimiento(ResultadoGauss res) {
        StringBuilder sb = new StringBuilder();
        String simb = res.getSuperficieGaussiana().getTipo().toString();
        sb.append("══════════════════════════════════════════════════\n");
        sb.append("   LEY DE GAUSS — PROCEDIMIENTO DETALLADO\n");
        sb.append("══════════════════════════════════════════════════\n\n");

        sb.append("DATOS\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append(String.format("  Figura cargada      : %s (%s)\n",
            res.getFiguraCargada().getTipo(),
            res.getFiguraCargada().getNombre()));
        sb.append(String.format("  Carga total Q       : %.4f µC = %.4e C  (%s)\n",
            res.getFiguraCargada().getCargaTotal(),
            res.getCargaTotalCoulombs(),
            res.getFiguraCargada().getSigno()));
        sb.append(String.format("  Superficie Gaussiana: %s\n", simb));
        sb.append(String.format("  ε₀                  : %.4e C²/(N·m²)\n\n",
            ResultadoGauss.getEpsilon0()));

        sb.append("CÁLCULO DE Q_enc\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append(String.format("  Fracción encerrada  : %.2f %%\n",
            res.getFraccionEncerrada() * 100));
        sb.append(String.format("  Q_enc = Q_total × fracción\n"));
        sb.append(String.format("  Q_enc = %.4e × %.4f\n",
            res.getCargaTotalCoulombs(), res.getFraccionEncerrada()));
        sb.append(String.format("  Q_enc = %.6e C\n\n", res.getCargaEncerradaCoulombs()));

        sb.append("LEY DE GAUSS\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append("  ∮ E·dA = Q_enc / ε₀\n");
        sb.append(String.format("  Φ = %.6e / %.4e\n",
            res.getCargaEncerradaCoulombs(), ResultadoGauss.getEpsilon0()));
        sb.append(String.format("  Φ = %.6e N·m²/C\n\n", res.getFlujoElectrico()));

        sb.append("CAMPO ELÉCTRICO PROMEDIO\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append(String.format("  Perímetro superficie: %.4f m\n",
            res.getPerimetroSuperficie()));
        sb.append("  E = Φ / Perímetro\n");
        sb.append(String.format("  E = %.6e / %.4f\n",
            res.getFlujoElectrico(), res.getPerimetroSuperficie()));
        sb.append(String.format("  E = %.6e N/C\n\n", res.getCampoPromedio()));

        sb.append("INTERPRETACIÓN\n");
        sb.append("──────────────────────────────────────────────────\n");
        if (res.isFiguraDentro()) {
            sb.append("  La figura cargada está completamente dentro\n");
            sb.append("  de la superficie gaussiana.\n");
            sb.append("  → Q_enc = Q_total (toda la carga contribuye).\n");
        } else if (res.getFraccionEncerrada() < 0.001) {
            sb.append("  La figura cargada está completamente fuera\n");
            sb.append("  de la superficie gaussiana.\n");
            sb.append("  → Q_enc ≈ 0, por lo tanto Φ ≈ 0.\n");
        } else {
            sb.append("  La figura cargada está parcialmente dentro.\n");
            sb.append(String.format("  Solo el %.1f %% de la carga contribuye al flujo.\n",
                res.getFraccionEncerrada() * 100));
        }

        sb.append("\n══════════════════════════════════════════════════\n");
        sb.append("                   FIN DEL CÁLCULO\n");
        sb.append("══════════════════════════════════════════════════\n");
        return sb.toString();
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private void actualizarEstadoBotones() {
        btnCalcular.setDisable(figuraCargada == null || superficieGauss == null);
        chkLineasCampo.setDisable(ultimoResultado == null);
    }

    private String fmtCientifica(double valor) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setDecimalSeparator(',');
        return new DecimalFormat("0.000000E00", sym).format(valor);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(mensaje);
        a.showAndWait();
    }
     @FXML
    private void Regresar() throws IOException {
        App.setRoot("Simuladores"); // Asegúrate de que este FXML existe
    }
}