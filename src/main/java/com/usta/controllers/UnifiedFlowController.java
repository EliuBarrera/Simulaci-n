package com.usta.controllers;

import java.io.IOException;
import java.util.Locale;

import com.usta.App;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
public class UnifiedFlowController {

    // ── FXML nodes ────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> figuraComboBox;

    @FXML private TextField campoTextField;
    @FXML private TextField baseTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField radioTextField;
    @FXML private TextField angleTextField;

    @FXML private Label baseLabel;
    @FXML private Label heightLabel;
    @FXML private Label radioLabel;

    @FXML private Slider angleSlider;
    @FXML private Slider sizeSlider;
    @FXML private Label  angleSliderLabel;
    @FXML private Label  sizeSliderLabel;

    @FXML private TextField flowTextField;

    @FXML private Canvas    cartesianCanvas;
    @FXML private Pane      canvasPane;


    // ── State ─────────────────────────────────────────────────────────────────
    private boolean simulationActive = false;

    // Base values captured when simulation starts (at sizeSlider=1).
    // The size slider multiplies these to grow/shrink the physics values.
    private double baseValueAtStart   = 1.0;
    private double heightValueAtStart = 1.0;
    private double radioValueAtStart  = 1.0;

    // Guards to prevent A→B→A listener loops
    private boolean syncingAngle = false;
    private boolean syncingSize  = false;

    private static final double BASE_PX = 70.0;  // tamaño visual a escala ×1 (slider en mitad → figura media)

    // Últimos valores calculados — usados para dibujar el popup en el canvas
    private double lastCampo    = Double.NaN;
    private double lastArea     = Double.NaN;
    private double lastCosAngle = Double.NaN;
    private double lastAngle    = Double.NaN;
    private double lastFlujo    = Double.NaN;
    private boolean hasResult   = false;

    private static double parseDoubleLocal(String texto) throws NumberFormatException {
        if (texto == null || texto.trim().isEmpty()) {
            throw new NumberFormatException("Valor vacío");
        }
        // Reemplazamos coma por punto (y quitamos posibles separadores de miles si existieran)
        String limpio = texto.trim().replace(',', '.').replaceAll("[^0-9.-]", "");
        return Double.parseDouble(limpio);
    }

    // ══ Lifecycle ═════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {

        figuraComboBox.getItems().addAll("Rectángulo", "Triángulo", "Circunferencia");
        figuraComboBox.getSelectionModel().selectFirst();
        applyFiguraLayout("Rectángulo");

        // ── Canvas resizes with pane (fix: always fill pane, never shrink-lock) ──
        canvasPane.widthProperty().addListener((obs, o, n) -> {
            cartesianCanvas.setWidth(n.doubleValue());
            redraw();
        });
        canvasPane.heightProperty().addListener((obs, o, n) -> {
            cartesianCanvas.setHeight(n.doubleValue());
            redraw();
        });

        // ── Angle slider → sync label + field + rotate + recalc ──────────────
        angleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingAngle) return;
            int angle = (int) Math.round(newVal.doubleValue());
            angleSliderLabel.setText(angle + "°");
            syncingAngle = true;
            angleTextField.setText(String.valueOf(angle));
            syncingAngle = false;
            redraw();
            if (simulationActive) tryAutoCalculate();
        });

        // ── Angle text field → sync slider + rotate + recalc ─────────────────
        angleTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingAngle) return;
            try {
                double v = parseDoubleLocal(newVal);
                if (v >= 0 && v <= 360) {
                    syncingAngle = true;
                    angleSlider.setValue(v);
                    angleSliderLabel.setText((int) v + "°");
                    syncingAngle = false;
                    redraw();
                }
            } catch (NumberFormatException ignored) {}
            if (simulationActive) tryAutoCalculate();
        });

        // ── Size slider → update fields + visual size + recalc ───────────────
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingSize) return;
            double scale = newVal.doubleValue();
            sizeSliderLabel.setText(String.format(Locale.US, "×%.1f", scale));
            
            syncingSize = true;
            applyScale(scale);   // writes fields + visual redraw
            syncingSize = false;
            if (simulationActive) tryAutoCalculate();
        });

        redraw();
    }

    // ══ "Calcular Flujo" button ════════════════════════════════════════════════
    @FXML
    private void startSimulation() {
        // Capturamos bases solo si no estaba activo antes (para no resetear al recalcular)
        if (!simulationActive) {
            double scale = sizeSlider.getValue();
            captureBaseValues(scale);
        }
        simulationActive = true;
        tryAutoCalculate();  // Siempre calcula al presionar
    }
    // ══ Size mechanics ════════════════════════════════════════════════════════

    private void captureBaseValues(double currentScale) {
        try { baseValueAtStart   = parseDoubleLocal(baseTextField.getText())   / currentScale; } catch (Exception e) { baseValueAtStart   = 1.0; }
        try { heightValueAtStart = parseDoubleLocal(heightTextField.getText())  / currentScale; } catch (Exception e) { heightValueAtStart = 1.0; }
        try { radioValueAtStart  = parseDoubleLocal(radioTextField.getText())   / currentScale; } catch (Exception e) { radioValueAtStart  = 1.0; }
    }

    /** Writes base×scale into dimension fields and resizes the figure image. */
    private void applyScale(double scale) {
        String figura = figuraComboBox.getValue();
        if (figura == null) return;
        switch (figura) {
            case "Circunferencia":
                radioTextField.setText(String.format(new Locale("es", "CO"), "%.4f", radioValueAtStart * scale));
                break;
            default:
                baseTextField  .setText(String.format(new Locale("es", "CO"), "%.4f", baseValueAtStart   * scale));
                heightTextField.setText(String.format(new Locale("es", "CO"), "%.4f", heightValueAtStart * scale));
                break;
        }
        redraw();
    }

    // ══ ComboBox ══════════════════════════════════════════════════════════════
    @FXML
    private void onFiguraChanged() {
        String selected = figuraComboBox.getValue();
        if (selected == null) return;
        applyFiguraLayout(selected);
        simulationActive = false;
        hasResult    = false;
        flowTextField.setText("--");
        
        baseValueAtStart = 1.0; heightValueAtStart = 1.0; radioValueAtStart = 1.0;
        syncingAngle = true; syncingSize = true;
        angleSlider.setValue(0);
        angleTextField.setText("0");
        angleSliderLabel.setText("0°");
        sizeSlider.setValue(5.5);
        syncingAngle = false; syncingSize = false;
        sizeSliderLabel.setText(String.format(Locale.US, "×%.1f", 5.5));
        redraw();
    }

    private void applyFiguraLayout(String figura) {
        boolean isCircle    = "Circunferencia".equals(figura);

        setVisible(baseLabel,       !isCircle);
        setVisible(baseTextField,   !isCircle);
        setVisible(heightLabel,     !isCircle);
        setVisible(heightTextField, !isCircle);
        setVisible(radioLabel,      isCircle);
        setVisible(radioTextField,  isCircle);
        
        redraw();
    }

    private void setVisible(javafx.scene.Node node, boolean show) {
        node.setVisible(show);
        node.setManaged(show);
    }

    // ══ Area ══════════════════════════════════════════════════════════════════
    private double area() {
        String figura = figuraComboBox.getValue();
        switch (figura) {
            case "Circunferencia": {
                double radio = parseDoubleLocal(radioTextField.getText());
                return Math.PI * Math.pow(radio, 2);
            }
            case "Triángulo": {
                double base   = parseDoubleLocal(baseTextField.getText());
                double height = parseDoubleLocal(heightTextField.getText());
                return (base * height) / 2.0;
            }
            default: {
                double base   = parseDoubleLocal(baseTextField.getText());
                double height = parseDoubleLocal(heightTextField.getText());
                return base * height;
            }
        }
    }

    // ══ Calculation ═══════════════════════════════════════════════════════════
    private void tryAutoCalculate() {
        if (!simulationActive) return;

        try {
            double campo = parseDoubleLocal(campoTextField.getText());
            double angle = parseDoubleLocal(angleTextField.getText());
            double angleRad = Math.toRadians(angle);

            int angleInt = (int) Math.round(angle) % 360;
            double cosAngle = (angleInt == 90 || angleInt == 270 || angleInt == 180)
                            ? 0.0
                            : Math.cos(angleRad);

            double areaVal = area();
            double flujo   = campo * areaVal * cosAngle;

            // Guardar para el popup en el canvas
            lastCampo    = campo;
            lastArea     = areaVal;
            lastCosAngle = cosAngle;
            lastAngle    = angle;
            lastFlujo    = flujo;
            hasResult    = true;

            flowTextField.setText(String.format(new Locale("es", "CO"), "%.4f", flujo));
            redraw(); // redibujar para mostrar/actualizar popup

        } catch (NumberFormatException | NullPointerException e) {
            hasResult = false;
            flowTextField.setText("--");
            redraw();
        }
    }

    // ══ Canvas drawing ════════════════════════════════════════════════════════
    private void redraw() {
        double W = cartesianCanvas.getWidth();
        double H = cartesianCanvas.getHeight();
        if (W <= 0 || H <= 0) return;

        double cx   = W / 2.0;
        double cy   = H / 2.0;
        double armX = cx * 0.90;
        double armY = cy * 0.90;
        double arrow = 9.0;
        double lOff  = 16.0;

        GraphicsContext gc = cartesianCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, W, H);

        // ── Fondo: patrón de Campo Eléctrico ─────────────────────────
        drawElectricFieldArrows(gc, W, H);

        // ── Grid cuadrada ─────────────────────────────────────────────────────
        // El paso de celda se basa en el eje más corto para que las celdas
        // sean siempre cuadradas independientemente del aspect-ratio del canvas.
        gc.setStroke(Color.rgb(0, 0, 0, 0.5));
        gc.setLineWidth(0.7);
        double cellSize = Math.min(armX, armY) / 5.0; // tamaño fijo de celda en px
        // Líneas verticales: de izquierda (-armX) a derecha (+armX) cada cellSize
        for (double dx = cellSize; dx <= armX + cellSize; dx += cellSize) {
            gc.strokeLine(cx + dx, cy - armY, cx + dx, cy + armY);
            gc.strokeLine(cx - dx, cy - armY, cx - dx, cy + armY);
        }
        // Líneas horizontales: de arriba (-armY) a abajo (+armY) cada cellSize
        for (double dy = cellSize; dy <= armY + cellSize; dy += cellSize) {
            gc.strokeLine(cx - armX, cy - dy, cx + armX, cy - dy);
            gc.strokeLine(cx - armX, cy + dy, cx + armX, cy + dy);
        }

        // ── Axes ─────────────────────────────────────────────────────────────
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2.2);
        gc.strokeLine(cx - armX, cy, cx + armX, cy);
        gc.strokeLine(cx, cy - armY, cx, cy + armY);

        // ── Arrowheads ───────────────────────────────────────────────────────
        gc.setFill(Color.BLACK);
        fillArrow(gc, cx + armX, cy,        0,            arrow);
        fillArrow(gc, cx - armX, cy,        Math.PI,      arrow);
        fillArrow(gc, cx, cy - armY,        -Math.PI / 2, arrow);
        fillArrow(gc, cx, cy + armY,         Math.PI / 2, arrow);

        // ── Labels ───────────────────────────────────────────────────────────
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText("0° / 360°", cx + armX + lOff - 8,  cy + 5);
        gc.fillText("180°",      cx - armX - lOff - 30,  cy + 5);
        gc.fillText("90°",       cx + 8, cy - armY - lOff + 14);
        gc.fillText("270°",      cx + 8, cy + armY + lOff + 2);

        // ── Origin dot ───────────────────────────────────────────────────────
        gc.setFill(Color.rgb(100, 118, 255));
        gc.fillOval(cx - 4, cy - 4, 8, 8);

        // ── Shape ────────────────────────────────────────────────────────────
        drawShape(gc, cx, cy);

        // ── Popup de cálculo ─────────────────────────────────────────────────
        if (hasResult) drawCalcPopup(gc, W, H);
    }

    private void drawElectricFieldArrows(GraphicsContext gc, double W, double H) {
        gc.setStroke(Color.rgb(0, 150, 255, 0.4)); // Celeste semi-transparente
        gc.setFill(Color.rgb(0, 150, 255, 0.4));
        gc.setLineWidth(2.0);
        
        double arrowLen = 12.0;
        double spacingX = 60.0;
        double spacingY = 60.0;
        
        for (double y = spacingY / 2; y < H; y += spacingY) {
            for (double x = spacingX / 2; x < W; x += spacingX) {
                gc.strokeLine(x - 15, y, x + 15, y);
                fillArrow(gc, x + 15, y, 0, arrowLen);
            }
        }
    }

    private void drawShape(GraphicsContext gc, double cx, double cy) {
        String figura = figuraComboBox.getValue();
        if (figura == null) return;
        
        double angle = 0.0;
        try {
            angle = parseDoubleLocal(angleTextField.getText());
        } catch (Exception ignored) {}

        double sizeFactor = sizeSlider.getValue();
        double wArea = 16.0 * sizeFactor;
        double hArea = 24.0 * sizeFactor;

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-angle);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(4.0);
        gc.setFill(Color.rgb(255, 255, 255, 0.4)); // Transparente claro en medio

        switch (figura) {
            case "Circunferencia":
                double rW = wArea * 0.4;
                double rH = hArea / 1.5;
                gc.fillOval(-rW, -rH, rW*2, rH*2);
                gc.strokeOval(-rW, -rH, rW*2, rH*2);
                break;
            case "Triángulo":
                double skewYTri = hArea * 0.15;
                double planeWTri = wArea * 0.4;
                gc.fillPolygon(
                    new double[] { -planeWTri, planeWTri, 0 }, 
                    new double[] { hArea/2 + skewYTri, hArea/2 - skewYTri, -hArea/2 }, 
                    3
                );
                gc.strokePolygon(
                    new double[] { -planeWTri, planeWTri, 0 }, 
                    new double[] { hArea/2 + skewYTri, hArea/2 - skewYTri, -hArea/2 }, 
                    3
                );
                break;
            default: // Rectángulo
                double skewY = hArea * 0.15;
                double planeW = wArea * 0.4;
                gc.fillPolygon(
                    new double[] { -planeW,   planeW,   planeW, -planeW }, 
                    new double[] { -hArea/2 + skewY, -hArea/2 - skewY, hArea/2 - skewY, hArea/2 + skewY }, 
                    4
                );
                gc.strokePolygon(
                    new double[] { -planeW,   planeW,   planeW, -planeW }, 
                    new double[] { -hArea/2 + skewY, -hArea/2 - skewY, hArea/2 - skewY, hArea/2 + skewY }, 
                    4
                );
                break;
        }

        // Vector de Área
        gc.setStroke(Color.rgb(255, 50, 50));
        gc.setFill(Color.rgb(255, 50, 50));
        gc.setLineWidth(4.0);
        
        double vectorLen = wArea * 0.8 + 20; 
        gc.strokeLine(0, 0, vectorLen, 0);
        fillArrow(gc, vectorLen, 0, 0, 15);
        
        gc.setFont(Font.font("System", FontWeight.BOLD, 18));
        gc.fillText("A", vectorLen - 10, -10);
        gc.setLineWidth(1.5);
        gc.strokeLine(vectorLen - 10, -25, vectorLen + 2, -25);
        fillArrow(gc, vectorLen + 2, -25, 0, 5);

        gc.restore();
    }

    /**
     * Dibuja una ventanita flotante cerca a la figura con los pasos del cálculo.
     * Se posiciona en la esquina superior derecha del canvas con margen.
     */
    private void drawCalcPopup(GraphicsContext gc, double W, double H) {
        Locale co = new Locale("es", "CO");

        String figura   = figuraComboBox.getValue();
        String areaFormula;
        switch (figura) {
            case "Circunferencia":
                areaFormula = String.format(co, "  A = π·r²  =  %.4f m²", lastArea);
                break;
            case "Triángulo":
                areaFormula = String.format(co, "  A = (b·h)/2  =  %.4f m²", lastArea);
                break;
            default:
                areaFormula = String.format(co, "  A = b·h  =  %.4f m²", lastArea);
                break;
        }

        String[] lines = {
            "  ── Pasos del cálculo ──",
            String.format(co, "  E  =  %.4f N/C", lastCampo),
            areaFormula,
            String.format(co, "  θ  =  %.1f°", lastAngle),
            String.format(co, "  cos(θ)  =  %.4f", lastCosAngle),
            "  ─────────────────────",
            "  Φ = E · A · cos(θ)",
            String.format(co, "  Φ = %.4f N·m²/C", lastFlujo),
        };

        // Dimensiones del popup
        Font popupFont  = Font.font("Monospaced", FontWeight.NORMAL, 12);
        Font titleFont  = Font.font("Monospaced", FontWeight.BOLD,   12);
        gc.setFont(popupFont);

        double lineH    = 18.0;
        double padX     = 10.0;
        double padY     = 8.0;
        double boxW     = 230.0;
        double boxH     = lines.length * lineH + padY * 2;

        // Posición: esquina superior derecha con margen
        double margin = 14.0;
        double boxX   = W - boxW - margin;
        double boxY   = margin;

        // Sombra suave
        gc.setFill(Color.rgb(0, 0, 0, 0.18));
        gc.fillRoundRect(boxX + 3, boxY + 3, boxW, boxH, 12, 12);

        // Fondo del popup
        gc.setFill(Color.rgb(255, 255, 255, 0.92));
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Borde
        gc.setStroke(Color.rgb(100, 118, 255, 0.85));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Encabezado con fondo azul suave
        gc.setFill(Color.rgb(100, 118, 255, 0.15));
        gc.fillRoundRect(boxX, boxY, boxW, lineH + padY * 2, 12, 12);
        // Cuadrar las esquinas inferiores del encabezado
        gc.fillRect(boxX, boxY + lineH, boxW, padY);

        // Texto línea por línea
        double textX = boxX + padX;
        double textY = boxY + padY + lineH * 0.8;
        for (int i = 0; i < lines.length; i++) {
            if (i == 0 || i == lines.length - 1) {
                gc.setFont(titleFont);
                gc.setFill(i == 0 ? Color.rgb(60, 80, 200) : Color.rgb(20, 120, 20));
            } else {
                gc.setFont(popupFont);
                gc.setFill(Color.rgb(40, 40, 40));
            }
            gc.fillText(lines[i], textX, textY + i * lineH);
        }
    }

    private void fillArrow(GraphicsContext gc, double x, double y, double anglRad, double size) {
        double sin = Math.sin(anglRad), cos = Math.cos(anglRad);
        double ax = x + cos * size,  ay = y + sin * size;
        double bx = x - cos * size * 0.5 - sin * size * 0.5;
        double by = y - sin * size * 0.5 + cos * size * 0.5;
        double cx2 = x - cos * size * 0.5 + sin * size * 0.5;
        double cy2 = y - sin * size * 0.5 - cos * size * 0.5;
        gc.fillPolygon(new double[]{ax, bx, cx2}, new double[]{ay, by, cy2}, 3);
    }


    // ══ Restart ═══════════════════════════════════════════════════════════════
    @FXML
    private void restart() {
        simulationActive = false;
        hasResult    = false;
        syncingAngle = false;
        syncingSize  = false;
        baseValueAtStart = 1.0; heightValueAtStart = 1.0; radioValueAtStart = 1.0;
        campoTextField .clear();
        baseTextField  .clear();
        heightTextField.clear();
        radioTextField .clear();
        angleTextField .clear();
        syncingAngle = true; syncingSize = true;
        angleTextField .setText("0");
        angleSlider.setValue(0);
        sizeSlider.setValue(5.5);
        syncingAngle = false; syncingSize = false;
        angleSliderLabel.setText("0°");
        sizeSliderLabel.setText(String.format(Locale.US, "×%.1f", 5.5));
        flowTextField.setText("--");
        redraw();
    }

    // ══ Navigation ════════════════════════════════════════════════════════════
    @FXML
    private void switchToMenu() throws IOException {
        App.setRoot("Simuladores"); // Asegúrate de que este FXML existe
    }
}
