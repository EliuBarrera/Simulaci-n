package com.usta.controllers;

import com.usta.App;
import com.usta.models.FiguraGauss;
import com.usta.models.ResultadoGauss;
import com.usta.models.TipoFigura;
import com.usta.utils.Gauss3DManager;
import com.usta.utils.GaussCalculator;
import com.usta.utils.GaussCanvasRenderer;
import com.usta.utils.GeneradorEscena3D;
import com.usta.utils.ProcedimientoBuilder;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Controlador del simulador de Ley de Gauss en 2D/3D.
 *
 * Flujo de uso:
 * 1. El usuario selecciona una figura del menú lateral.
 * 2. Arrastra el mouse en el canvas para dibujarla (modo 2D)
 * o ingresa dimensiones (modo 3D).
 * 3. Ingresa carga y signo, presiona "Confirmar figura".
 * 4. Selecciona la figura de la superficie gaussiana y la dibuja.
 * 5. Presiona "Calcular" para ver el resultado.
 * 6. Puede activar "Ver líneas de campo" sobre la superficie gaussiana.
 *
 * Este controlador solo mantiene estado y coordina; el renderizado 2D
 * lo delega en {@link GaussCanvasRenderer}, el 3D en {@link Gauss3DManager}
 * y el texto de procedimiento en {@link ProcedimientoBuilder}.
 */
public class LeyGaussController {

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final double PX_POR_UNIT = 80.0;
    private static final double CANVAS_W = 1400.0;
    private static final double CANVAS_H = 900.0;

    // ── FXML — canvas y contenedor ───────────────────────────────────────────
    @FXML
    private Canvas canvasGauss;
    @FXML
    private Pane gaussPane;
    @FXML
    private ScrollPane gaussScrollPane;

    // ── FXML — figura cargada ────────────────────────────────────────────────
    @FXML
    private ComboBox<TipoFigura> figuraCargadaCombo;
    @FXML
    private TextField cargaField;
    @FXML
    private ComboBox<String> signoCargaCombo;
    @FXML
    private Button btnConfirmarFigura;
    @FXML
    private Label lblEstadoFigura;

    // ── FXML — superficie gaussiana ──────────────────────────────────────────
    @FXML
    private ComboBox<TipoFigura> superficieCombo;
    @FXML
    private Button btnConfirmarSuperficie;
    @FXML
    private Label lblEstadoSuperficie;

    // ── FXML — acciones ──────────────────────────────────────────────────────
    @FXML
    private Button btnCalcular;
    @FXML
    private Button btnLimpiar;
    @FXML
    private CheckBox chkLineasCampo;
    @FXML
    private CheckBox chkModo3D;

    // ── FXML — modo 3D ───────────────────────────────────────────────────────
    @FXML
    private RadioButton optCargaExacta;
    @FXML
    private RadioButton optDensidad;
    @FXML
    private Label lblCargaInfo;
    @FXML
    private VBox panelDimensiones3DFigura;
    @FXML
    private TextField figCxField, figCyField, figCzField;
    @FXML
    private TextField figP1Field, figP2Field, figP3Field;
    @FXML
    private VBox panelDimensiones3DSuperficie;
    @FXML
    private TextField supCxField, supCyField, supCzField;
    @FXML
    private TextField supP1Field, supP2Field, supP3Field;

    // ── FXML — resultados ─────────────────────────────────────────────────────
    @FXML
    private Label lblQenc;
    @FXML
    private Label lblFlujo;
    @FXML
    private Label lblCampoE;
    @FXML
    private Label lblFraccion;
    @FXML
    private TextArea txtProcedimiento;

    // ── Estado de la UI ───────────────────────────────────────────────────────
    private enum Modo {
        NADA, DIBUJANDO_FIGURA, DIBUJANDO_SUPERFICIE, MOVIENDO
    }

    private Modo modoActual = Modo.NADA;
    private boolean esperandoDibujo = false;
    private boolean figuraConfirmada = false;
    private boolean superficieConfirmada = false;
    private boolean modo3D = false;

    // ── Modelos ───────────────────────────────────────────────────────────────
    private FiguraGauss figuraCargada = null;
    private FiguraGauss superficieGauss = null;
    private ResultadoGauss ultimoResultado = null;

    // ── Drag 2D ───────────────────────────────────────────────────────────────
    private double dragStartX, dragStartY, dragCurrentX, dragCurrentY;

    // ── Mover figura ──────────────────────────────────────────────────────────
    private FiguraGauss figuraMoviendo = null;
    private double offsetMoveX, offsetMoveY;

    // ── Contador de nombres ───────────────────────────────────────────────────
    private int contadorFiguras = 1;

    // ── Colaboradores ─────────────────────────────────────────────────────────
    private GaussCanvasRenderer renderer;
    private Gauss3DManager manager3D;
    private ToggleGroup toggleCargaGroup;

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================

    public void initialize() {
        configurarCombos();
        configurarCanvas();
        configurarToggleCarga();
        configurarEscena3D();
        configurarListeners();

        actualizarEstadoBotones();
        onToggleModo3D();
        redibujar();
    }

    private void configurarCombos() {
        figuraCargadaCombo.getItems().addAll(TipoFigura.values());
        figuraCargadaCombo.setValue(TipoFigura.CIRCULO);
        superficieCombo.getItems().addAll(TipoFigura.values());
        superficieCombo.setValue(TipoFigura.CIRCULO);
        signoCargaCombo.getItems().addAll("+", "-");
        signoCargaCombo.setValue("+");
    }

    private void configurarCanvas() {
        canvasGauss.setWidth(CANVAS_W);
        canvasGauss.setHeight(CANVAS_H);
        renderer = new GaussCanvasRenderer(canvasGauss);

        canvasGauss.setOnMousePressed(this::onMousePressed);
        canvasGauss.setOnMouseDragged(this::onMouseDragged);
        canvasGauss.setOnMouseReleased(this::onMouseReleased);
        canvasGauss.setOnMouseMoved(this::onMouseMoved);
    }

    private void configurarToggleCarga() {
        toggleCargaGroup = new ToggleGroup();
        if (optCargaExacta == null)
            return;
        optCargaExacta.setToggleGroup(toggleCargaGroup);
        optDensidad.setToggleGroup(toggleCargaGroup);
        toggleCargaGroup.selectedToggleProperty()
                .addListener((obs, o, n) -> lblCargaInfo.setText(optCargaExacta.isSelected()
                        ? "Carga Q (µC):"
                        : "Densidad (µC/m, m², m³):"));
    }

    private void configurarEscena3D() {
        if (gaussPane == null || gaussScrollPane == null)
            return;
        GeneradorEscena3D gen = new GeneradorEscena3D(1000, 800);
        gen.getSubScene().widthProperty().bind(gaussScrollPane.widthProperty().subtract(2));
        gen.getSubScene().heightProperty().bind(gaussScrollPane.heightProperty().subtract(2));
        gen.getSubScene().setVisible(false);
        gen.getSubScene().setManaged(false);
        gaussPane.getChildren().add(0, gen.getSubScene());
        manager3D = new Gauss3DManager(gen);
    }

    private void configurarListeners() {
        chkLineasCampo.selectedProperty().addListener((obs, o, n) -> redibujar());
    }

    // =========================================================================
    // ACCIONES DEL PANEL — modo 3D
    // =========================================================================

    @FXML
    private void onToggleModo3D() {
        if (chkModo3D == null)
            return;
        modo3D = chkModo3D.isSelected();

        panelDimensiones3DFigura.setVisible(modo3D);
        panelDimensiones3DFigura.setManaged(modo3D);
        panelDimensiones3DSuperficie.setVisible(modo3D);
        panelDimensiones3DSuperficie.setManaged(modo3D);

        recargarCombosSegunModo();
        alternarVistaPrincipal();
        onLimpiar();
    }

    private void recargarCombosSegunModo() {
        figuraCargadaCombo.getItems().clear();
        superficieCombo.getItems().clear();

        if (modo3D) {
            figuraCargadaCombo.getItems().addAll(TipoFigura.values());
            superficieCombo.getItems().addAll(TipoFigura.ESFERA, TipoFigura.CILINDRO, TipoFigura.CAJA);
            if (btnConfirmarFigura != null)
                btnConfirmarFigura.setText("Confirmar figura 3D");
            if (btnConfirmarSuperficie != null)
                btnConfirmarSuperficie.setText("Confirmar superficie 3D");
        } else {
            figuraCargadaCombo.getItems().addAll(
                    TipoFigura.CIRCULO, TipoFigura.RECTANGULO, TipoFigura.CUADRADO, TipoFigura.TRIANGULO);
            superficieCombo.getItems().addAll(
                    TipoFigura.CIRCULO, TipoFigura.RECTANGULO, TipoFigura.CUADRADO, TipoFigura.TRIANGULO);
            if (btnConfirmarFigura != null)
                btnConfirmarFigura.setText("Dibujar figura →");
            if (btnConfirmarSuperficie != null)
                btnConfirmarSuperficie.setText("Dibujar superficie →");
        }
        figuraCargadaCombo.getSelectionModel().selectFirst();
        superficieCombo.getSelectionModel().selectFirst();
    }

    private void alternarVistaPrincipal() {
        boolean sub = manager3D != null;
        canvasGauss.setVisible(!modo3D);
        canvasGauss.setManaged(!modo3D);
        if (sub) {
            manager3D.getGenerador().getSubScene().setVisible(modo3D);
            manager3D.getGenerador().getSubScene().setManaged(modo3D);
            if (modo3D)
                manager3D.getGenerador().resetCamera();
        }
        if (gaussScrollPane != null)
            gaussScrollPane.setPannable(!modo3D);
    }

    // =========================================================================
    // ACCIONES DEL PANEL — figura y superficie
    // =========================================================================

    @FXML
    private void onIniciarDibujoFigura() {
        if (modo3D) {
            crearFigura3DDirecto();
            return;
        }

        if (!validarCampoNumerico(cargaField, "Ingrese el valor antes de dibujar."))
            return;

        modoActual = Modo.DIBUJANDO_FIGURA;
        esperandoDibujo = true;
        lblEstadoFigura.setText("Dibuja la figura en el plano →");
        canvasGauss.setCursor(Cursor.CROSSHAIR);
    }

    @FXML
    private void onIniciarDibujoSuperficie() {
        if (modo3D) {
            crearSuperficie3DDirecto();
            return;
        }
        if (figuraCargada == null) {
            mostrarAlerta("Sin figura", "Primero confirma la figura cargada.");
            return;
        }
        modoActual = Modo.DIBUJANDO_SUPERFICIE;
        esperandoDibujo = true;
        lblEstadoSuperficie.setText("Dibuja la superficie en el plano →");
        canvasGauss.setCursor(Cursor.CROSSHAIR);
    }

    private void crearFigura3DDirecto() {
        try {
            double q = Double.parseDouble(cargaField.getText().trim());
            if (q <= 0) {
                mostrarAlerta("Error", "El valor debe ser positivo.");
                return;
            }

            double cx = Double.parseDouble(figCxField.getText().trim());
            double cy = Double.parseDouble(figCyField.getText().trim());
            double cz = Double.parseDouble(figCzField.getText().trim());
            double p1 = Double.parseDouble(figP1Field.getText().trim());
            double p2 = parseOpcional(figP2Field);
            double p3 = parseOpcional(figP3Field);

            TipoFigura tipo = figuraCargadaCombo.getValue();
            String signo = signoCargaCombo.getValue();
            double cargaT = calcularCargaTotal(q, tipo, p1, p2, p3);

            figuraCargada = new FiguraGauss(tipo,
                    cx * PX_POR_UNIT, cy * PX_POR_UNIT, cz * PX_POR_UNIT,
                    p1 * PX_POR_UNIT, p2 * PX_POR_UNIT, p3 * PX_POR_UNIT,
                    cargaT, signo, "F" + contadorFiguras++);

            figuraConfirmada = true;
            lblEstadoFigura.setText("✔ Figura: " + tipo + " | Q=" + String.format("%.2f", cargaT) + " µC");
            actualizarEstadoBotones();
            manager3D.actualizar(figuraCargada, superficieGauss);

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Ingresa dimensiones numéricas válidas.");
        }
    }

    private void crearSuperficie3DDirecto() {
        if (figuraCargada == null) {
            mostrarAlerta("Error", "Primero confirma la figura cargada.");
            return;
        }
        try {
            double cx = Double.parseDouble(supCxField.getText().trim());
            double cy = Double.parseDouble(supCyField.getText().trim());
            double cz = Double.parseDouble(supCzField.getText().trim());
            double p1 = Double.parseDouble(supP1Field.getText().trim());
            double p2 = parseOpcional(supP2Field);
            double p3 = parseOpcional(supP3Field);

            TipoFigura tipo = superficieCombo.getValue();
            superficieGauss = new FiguraGauss(tipo,
                    cx * PX_POR_UNIT, cy * PX_POR_UNIT, cz * PX_POR_UNIT,
                    p1 * PX_POR_UNIT, p2 * PX_POR_UNIT, p3 * PX_POR_UNIT,
                    0, "+", "S");

            superficieConfirmada = true;
            lblEstadoSuperficie.setText("✔ Superficie: " + tipo);
            actualizarEstadoBotones();
            manager3D.actualizar(figuraCargada, superficieGauss);

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Ingresa dimensiones numéricas válidas.");
        }
    }

    // =========================================================================
    // ACCIONES GLOBALES
    // =========================================================================

    @FXML
    private void onCalcular() {
        if (figuraCargada == null || superficieGauss == null) {
            mostrarAlerta("Incompleto", "Necesitas una figura cargada y una superficie gaussiana.");
            return;
        }
        ultimoResultado = new GaussCalculator(PX_POR_UNIT).calcular(figuraCargada, superficieGauss);
        mostrarResultados(ultimoResultado);
        redibujar();
    }

    @FXML
    private void onLimpiar() {
        figuraCargada = null;
        superficieGauss = null;
        ultimoResultado = null;
        figuraConfirmada = false;
        superficieConfirmada = false;
        modoActual = Modo.NADA;
        esperandoDibujo = false;
        contadorFiguras = 1;

        lblEstadoFigura.setText("Sin figura");
        lblEstadoSuperficie.setText("Sin superficie");
        lblQenc.setText("—");
        lblFlujo.setText("—");
        lblCampoE.setText("—");
        lblFraccion.setText("—");
        if (txtProcedimiento != null)
            txtProcedimiento.clear();
        chkLineasCampo.setSelected(false);
        if (manager3D != null)
            manager3D.getGenerador().limpiarElementos();
        canvasGauss.setCursor(Cursor.DEFAULT);
        actualizarEstadoBotones();
        redibujar();
    }

    @FXML
    private void Regresar() throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación de Salida");
        alert.setHeaderText("¿Está seguro de que desea salir?");
        alert.setContentText("Toda la configuración de la figura y superficie gaussiana se borrará.");

        ButtonType btnSi = new ButtonType("Sí, salir");
        ButtonType btnNo = new ButtonType("No, cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnSi, btnNo);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnSi) {
            App.setRoot("Simuladores");
        }
    }

    // =========================================================================
    // EVENTOS DEL CANVAS
    // =========================================================================

    private void onMousePressed(MouseEvent e) {
        double px = e.getX(), py = e.getY();
        if (esperandoDibujo) {
            dragStartX = dragCurrentX = px;
            dragStartY = dragCurrentY = py;
            return;
        }
        iniciarMovimiento(px, py);
    }

    private void onMouseDragged(MouseEvent e) {
        double px = e.getX(), py = e.getY();
        if (esperandoDibujo) {
            dragCurrentX = px;
            dragCurrentY = py;
            renderConPreview();
            return;
        }
        if (modoActual == Modo.MOVIENDO && figuraMoviendo != null) {
            figuraMoviendo.setCx(px - offsetMoveX);
            figuraMoviendo.setCy(py - offsetMoveY);
            ultimoResultado = null;
            lblQenc.setText("—");
            lblFlujo.setText("—");
            lblCampoE.setText("—");
            lblFraccion.setText("—");
            redibujar();
        }
    }

    private void onMouseReleased(MouseEvent e) {
        if (modoActual == Modo.MOVIENDO) {
            figuraMoviendo = null;
            modoActual = Modo.NADA;
            canvasGauss.setCursor(Cursor.DEFAULT);
            return;
        }
        if (!esperandoDibujo)
            return;
        finalizarDibujo(e.getX(), e.getY());
    }

    private void onMouseMoved(MouseEvent e) {
        if (!esperandoDibujo) {
            double px = e.getX(), py = e.getY();
            boolean sobreFigura = (figuraCargada != null && figuraCargada.contienePunto(px, py))
                    || (superficieGauss != null && superficieGauss.contienePunto(px, py));
            canvasGauss.setCursor(sobreFigura ? Cursor.HAND : Cursor.DEFAULT);
        }
    }

    // ── Helpers de mouse ──────────────────────────────────────────────────────

    private void iniciarMovimiento(double px, double py) {
        if (figuraCargada != null && figuraCargada.contienePunto(px, py)) {
            figuraMoviendo = figuraCargada;
        } else if (superficieGauss != null && superficieGauss.contienePunto(px, py)) {
            figuraMoviendo = superficieGauss;
        } else
            return;

        offsetMoveX = px - figuraMoviendo.getCx();
        offsetMoveY = py - figuraMoviendo.getCy();
        modoActual = Modo.MOVIENDO;
        canvasGauss.setCursor(Cursor.MOVE);
    }

    private void finalizarDibujo(double px, double py) {
        double cx = (dragStartX + px) / 2;
        double cy = (dragStartY + py) / 2;
        double dx = Math.abs(px - dragStartX) / 2;
        double dy = Math.abs(py - dragStartY) / 2;
        double dim = Math.max(dx, dy);

        if (dim < 10) {
            lblEstadoFigura.setText("Figura demasiado pequeña, intenta de nuevo.");
            resetearModoDibujo();
            return;
        }

        if (modoActual == Modo.DIBUJANDO_FIGURA) {
            TipoFigura tipo = figuraCargadaCombo.getValue();
            double p1 = (tipo == TipoFigura.RECTANGULO) ? dx : dim;
            double p2 = (tipo == TipoFigura.RECTANGULO || tipo == TipoFigura.TRIANGULO) ? dy : dim;
            double q = Double.parseDouble(cargaField.getText().trim());
            String s = signoCargaCombo.getValue();

            figuraCargada = new FiguraGauss(tipo, cx, cy, 0, p1, p2, 0, q, s, "F" + contadorFiguras++);
            figuraConfirmada = true;
            lblEstadoFigura.setText("✔ Figura: " + tipo + " | " + q + " µC (" + s + ")");

        } else if (modoActual == Modo.DIBUJANDO_SUPERFICIE) {
            TipoFigura tipo = superficieCombo.getValue();
            double p1 = (tipo == TipoFigura.RECTANGULO) ? dx : dim;
            double p2 = (tipo == TipoFigura.RECTANGULO || tipo == TipoFigura.TRIANGULO) ? dy : dim;

            superficieGauss = new FiguraGauss(tipo, cx, cy, 0, p1, p2, 0, 0, "+", "S");
            superficieConfirmada = true;
            lblEstadoSuperficie.setText("✔ Superficie: " + tipo);
        }

        resetearModoDibujo();
        actualizarEstadoBotones();
        redibujar();
    }

    private void resetearModoDibujo() {
        esperandoDibujo = false;
        modoActual = Modo.NADA;
        canvasGauss.setCursor(Cursor.DEFAULT);
    }

    // =========================================================================
    // RENDERIZADO
    // =========================================================================

    private void redibujar() {
        if (modo3D) {
            if (manager3D != null)
                manager3D.actualizar(figuraCargada, superficieGauss);
        } else {
            renderer.render(figuraCargada, superficieGauss, ultimoResultado,
                    chkLineasCampo.isSelected(), null, false);
        }
    }

    private void renderConPreview() {
        FiguraGauss preview = buildFiguraPreview();
        boolean esFigura = (modoActual == Modo.DIBUJANDO_FIGURA);
        renderer.render(figuraCargada, superficieGauss, ultimoResultado,
                chkLineasCampo.isSelected(), preview, esFigura);
    }

    private FiguraGauss buildFiguraPreview() {
        double cx = (dragStartX + dragCurrentX) / 2;
        double cy = (dragStartY + dragCurrentY) / 2;
        double dx = Math.abs(dragCurrentX - dragStartX) / 2;
        double dy = Math.abs(dragCurrentY - dragStartY) / 2;
        double dim = Math.max(dx, dy);

        TipoFigura tipo = (modoActual == Modo.DIBUJANDO_FIGURA)
                ? figuraCargadaCombo.getValue()
                : superficieCombo.getValue();

        double p1 = (tipo == TipoFigura.RECTANGULO) ? dx : dim;
        double p2 = (tipo == TipoFigura.RECTANGULO || tipo == TipoFigura.TRIANGULO) ? dy : dim;

        return new FiguraGauss(tipo, cx, cy, 0, p1, p2, 0, 0, "+", "preview");
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
            txtProcedimiento.setText(ProcedimientoBuilder.construir(res));
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private void actualizarEstadoBotones() {
        btnCalcular.setDisable(figuraCargada == null || superficieGauss == null);
        chkLineasCampo.setDisable(ultimoResultado == null);
    }

    /**
     * Calcula la carga total según si el usuario eligió carga exacta o densidad.
     */
    private double calcularCargaTotal(double valor, TipoFigura tipo,
            double p1, double p2, double p3) {
        if (optCargaExacta != null && optCargaExacta.isSelected())
            return valor;
        FiguraGauss tmp = new FiguraGauss(tipo, 0, 0, 0,
                p1 * PX_POR_UNIT, p2 * PX_POR_UNIT, p3 * PX_POR_UNIT, 0, "+", "");
        return valor * tmp.calcularMagnitudEspacial(PX_POR_UNIT);
    }

    private double parseOpcional(TextField field) {
        String txt = field.getText().trim();
        return txt.isEmpty() ? 0 : Double.parseDouble(txt);
    }

    private boolean validarCampoNumerico(TextField field, String mensaje) {
        try {
            double v = Double.parseDouble(field.getText().trim());
            if (v <= 0) {
                mostrarAlerta("Error", "El valor debe ser positivo.");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            mostrarAlerta("Falta dato", mensaje);
            return false;
        }
    }

    private String fmtCientifica(double valor) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setDecimalSeparator(',');
        return new DecimalFormat("0.000000E00", sym).format(valor);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}