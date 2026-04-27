package com.usta.controllers;

import com.usta.App;
import com.usta.controllers.Handlers.AnimacionPotencialHandler;
import com.usta.controllers.Handlers.CalculoPotencialHandler;
import com.usta.controllers.Handlers.DetallesPdfPotencialHandler;
import com.usta.controllers.Handlers.EtiquetaReposicionador;
import com.usta.controllers.Handlers.GrafoRenderer;
import com.usta.controllers.Handlers.Modo3DHandler;
import com.usta.controllers.Handlers.NodoDragHandler;
import com.usta.controllers.Handlers.ParticulaHandler;
import com.usta.controllers.Handlers.RutaHandler;
import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoPotencial;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.GeneradorEscena3D;
import com.usta.utils.UnidadDistancia;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador principal para la vista de Ley de Coulomb.
 *
 * Actúa exclusivamente como <em>coordinador</em>: mantiene el estado
 * compartido e instancia/enlaza los handlers especializados.
 *
 * Responsabilidades propias:
 * - Mantener {@code modo3D} y {@code unidadActual} como fuente de verdad.
 * - Inicializar el grafo, los handlers y los controles FXML.
 * - Recibir eventos @FXML y delegar sin lógica adicional.
 *
 * Lógica delegada:
 * - Partículas → {@link ParticulaHandler}
 * - Rutas → {@link RutaHandler}
 * - Cálculo → {@link CalculoHandler}
 * - Renderizado → {@link GrafoRenderer}
 * - PDF/Detalles → {@link DetallesPdfHandler}
 * - Animación → {@link AnimacionTabHandler}
 * - Arrastre nodos → {@link NodoDragHandler}
 * - Toggle 2D/3D → {@link Modo3DHandler}
 * - Etiquetas → {@link EtiquetaReposicionador}
 */
public class PotencialElectricoController {

    // ── FXML: layout principal ────────────────────────────────────────────────
    @FXML
    private Pane grafoPane;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Canvas canvasPlano;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private TabPane tabPanePrincipal;

    // ── FXML: partículas ──────────────────────────────────────────────────────
    @FXML
    private TextField nombreParticulaField;
    @FXML
    private TextField valorCargaField;
    @FXML
    private ToggleButton positivaToggle;
    @FXML
    private ToggleButton negativaToggle;
    @FXML
    private ComboBox<String> particulaEliminarComboBox;
    @FXML
    private TextField particulaEditarField;
    @FXML
    private ComboBox<String> particulaEditarComboBox;

    // ── FXML: coordenadas agregar ─────────────────────────────────────────────
    @FXML
    private TextField coordXField;
    @FXML
    private TextField coordYField;
    @FXML
    private TextField coordZField;
    @FXML
    private HBox coordZBox;
    @FXML
    private CheckBox modo3DCheckBox;
    @FXML
    private Label modo3DInfoLabel;

    // ── FXML: coordenadas editar ──────────────────────────────────────────────
    @FXML
    private TextField editCoordXField;
    @FXML
    private TextField editCoordYField;
    @FXML
    private TextField editCoordZField;
    @FXML
    private HBox editCoordZBox;

    // ── FXML: rutas ───────────────────────────────────────────────────────────
    @FXML
    private ComboBox<String> origenRutaComboBox;
    @FXML
    private ComboBox<String> destinoRutaComboBox;
    @FXML
    private ComboBox<String> eliminarRutaComboBox;

    // ── FXML: cálculos ────────────────────────────────────────────────────────
    @FXML
    private ComboBox<String> particulaOrigenComboBox;
    @FXML
    private ComboBox<UnidadDistancia> unidadDistanciaComboBox;
    @FXML
    private Button calcularButton;
    @FXML
    private Button cancelarButton;

    // ── FXML: resultados ──────────────────────────────────────────────────────
    @FXML
    private Label resultadoPotencialLabel;
    @FXML
    private Label resultadoEnergiaLabel;
    @FXML
    private TextArea calculosDetalladosTextArea;

    // ── FXML: animación ───────────────────────────────────────────────────────
    @FXML
    private Tab animacionTab;
    @FXML
    private Label pasoIndicadorLabel;
    @FXML
    private Label pasoDescripcionLabel;
    @FXML
    private Label barraProgresoLabel;
    @FXML
    private Button btnAnteriorPaso;
    @FXML
    private Button btnSiguientePaso;
    @FXML
    private Button btnReiniciarAnimacion;
    @FXML
    private Button btnDetenerAnimacion;

    // ── Estado compartido ─────────────────────────────────────────────────────
    private Grafo grafo;
    private Map<Nodo, Circle> nodoCirculos;
    private ObservableList<String> nombresParticulas;
    private boolean modo3D = false;
    private UnidadDistancia unidadActual = UnidadDistancia.METROS;
    private ResultadoPotencial ultimoResultado;
    private ToggleGroup polaridadGroup;

    // ── Handlers ──────────────────────────────────────────────────────────────
    private ParticulaHandler particulaHandler;
    private RutaHandler rutaHandler;
    private CalculoPotencialHandler calculoHandler;
    private GrafoRenderer renderer;
    private DetallesPdfPotencialHandler detallesPdfHandler;
    private AnimacionPotencialHandler animacionHandler;
    private GeneradorEscena3D generador3D;
    private NodoDragHandler dragHandler;
    private Modo3DHandler modo3DHandler;
    private EtiquetaReposicionador etiquetaReposicionador;

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================

    public void initialize() {
        grafo = new Grafo();
        nodoCirculos = new HashMap<>();
        nombresParticulas = FXCollections.observableArrayList();

        // Instanciar handlers que no dependen de otros handlers
        particulaHandler = new ParticulaHandler(grafo, nodoCirculos, nombresParticulas,
                canvasPlano, scrollPane, grafoPane);
        rutaHandler = new RutaHandler(grafo, grafoPane);
        calculoHandler = new CalculoPotencialHandler(grafo, canvasPlano);
        renderer = new GrafoRenderer(grafoPane, canvasPlano);
        detallesPdfHandler = new DetallesPdfPotencialHandler(grafoPane);
        animacionHandler = new AnimacionPotencialHandler();

        // 3D
        generador3D = new GeneradorEscena3D(1000, 800);
        generador3D.getSubScene().widthProperty().bind(scrollPane.widthProperty().subtract(2));
        generador3D.getSubScene().heightProperty().bind(scrollPane.heightProperty().subtract(2));
        generador3D.getSubScene().setVisible(false);
        generador3D.getSubScene().setManaged(false);
        grafoPane.getChildren().add(0, generador3D.getSubScene());

        // Handlers que dependen de generador3D
        etiquetaReposicionador = new EtiquetaReposicionador(
                grafoPane, canvasPlano, nodoCirculos, generador3D);

        dragHandler = new NodoDragHandler(
                grafoPane, nodoCirculos, rutaHandler,
                () -> etiquetaReposicionador.crearTransformador(unidadActual),
                () -> modo3D);

        modo3DHandler = new Modo3DHandler(
                canvasPlano, scrollPane, grafoPane, tabPanePrincipal, animacionTab,
                coordZBox, editCoordZBox, modo3DInfoLabel,
                generador3D, renderer, rutaHandler, grafo, nodoCirculos,
                etiquetaReposicionador,
                this::limpiarEstadoCalculo);

        // Enlazar campos FXML a los handlers
        enlazarCamposParticula();
        enlazarCamposRuta();
        enlazarCamposCalculo();
        enlazarCamposDetalles();
        enlazarCamposAnimacion();

        // Combos de partículas
        origenRutaComboBox.setItems(nombresParticulas);
        destinoRutaComboBox.setItems(nombresParticulas);
        particulaEliminarComboBox.setItems(nombresParticulas);
        particulaOrigenComboBox.setItems(nombresParticulas);
        if (particulaEditarComboBox != null)
            particulaEditarComboBox.setItems(nombresParticulas);
        eliminarRutaComboBox.setItems(FXCollections.observableArrayList());

        polaridadGroup = new ToggleGroup();
        positivaToggle.setToggleGroup(polaridadGroup);
        negativaToggle.setToggleGroup(polaridadGroup);
        positivaToggle.setSelected(true);

        // Combo de unidades
        ObservableList<UnidadDistancia> unidades = FXCollections.observableArrayList(
                UnidadDistancia.KILOMETROS, UnidadDistancia.METROS,
                UnidadDistancia.CENTIMETROS, UnidadDistancia.MILIMETROS,
                UnidadDistancia.MICROMETROS, UnidadDistancia.NANOMETROS,
                UnidadDistancia.PICOMETROS);
        unidadDistanciaComboBox.setItems(unidades);
        unidadDistanciaComboBox.setValue(UnidadDistancia.METROS);
        unidadDistanciaComboBox.setOnAction(e -> cambiarUnidad(unidadDistanciaComboBox.getValue()));

        calculoHandler.setOnResultado(res -> {
            ultimoResultado = res;
            Nodo orig = res.getParticulaOrigen();
            Circle c = nodoCirculos.get(orig);
            if (c == null)
                return;

            if (modo3D) {
                generador3D.sincronizarGrafo(grafo, unidadActual);
                // No se dibujan flechas para potencial eléctrico (escalar)
            } else {
                renderer.limpiarFlechas();
            }
        });

        calculoHandler.setOnCancelar(() -> {
            renderer.limpiarFlechas();
            if (modo3D)
                generador3D.sincronizarGrafo(grafo, unidadActual);
        });

        // Canvas
        // Canvas 20x15 unidades (STEP=100, MARGIN=40 -> 2080x1580 px)
        canvasPlano.setWidth(2080);
        canvasPlano.setHeight(1580);
        canvasPlano.widthProperty().addListener((obs, o, n) -> renderer.dibujarCuadrante(unidadActual));
        canvasPlano.heightProperty().addListener((obs, o, n) -> renderer.dibujarCuadrante(unidadActual));
        renderer.dibujarCuadrante(unidadActual);

        // TextArea de detalles
        if (calculosDetalladosTextArea != null) {
            calculosDetalladosTextArea.setEditable(false);
            calculosDetalladosTextArea.setWrapText(true);
            calculosDetalladosTextArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        }

        // Listeners de cámara → reposicionar etiquetas 3D
        generador3D.getCameraRotX().angleProperty()
                .addListener((obs, o, n) -> etiquetaReposicionador.reposicionarEtiquetas3D(unidadActual));
        generador3D.getCameraRotY().angleProperty()
                .addListener((obs, o, n) -> etiquetaReposicionador.reposicionarEtiquetas3D(unidadActual));
        generador3D.getCameraPan().xProperty()
                .addListener((obs, o, n) -> etiquetaReposicionador.reposicionarEtiquetas3D(unidadActual));
        generador3D.getCameraPan().yProperty()
                .addListener((obs, o, n) -> etiquetaReposicionador.reposicionarEtiquetas3D(unidadActual));
        generador3D.getCamera().translateZProperty()
                .addListener((obs, o, n) -> etiquetaReposicionador.reposicionarEtiquetas3D(unidadActual));

        // Anti-zoom fantasma en ScrollPane
        scrollPane.addEventFilter(ScrollEvent.ANY, e -> {
            if (modo3D && e.getTarget() != generador3D.getSubScene())
                e.consume();
        });
        tabPanePrincipal.addEventFilter(ScrollEvent.ANY, Event::consume);

        // Guardar/restaurar posición del scroll al cambiar de pestaña
        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            double h = scrollPane.getHvalue();
            double v = scrollPane.getVvalue();
            javafx.application.Platform.runLater(() -> {
                scrollPane.setHvalue(h);
                scrollPane.setVvalue(v);
            });
        });

        // Auto-cargar sistema de prueba
        javafx.application.Platform.runLater(this::cargarSistemaPrueba);
    }

    // =========================================================================
    // ACCIONES FXML — sin lógica, solo delegación
    // =========================================================================

    @FXML
    private void agregarParticula() {
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.agregar(modo3D, t, () -> {
            Nodo ultimo = grafo.getNodos().get(grafo.getNodos().size() - 1);
            Circle c = nodoCirculos.get(ultimo);
            if (c != null)
                dragHandler.hacerArrastrable(c, ultimo);
            if (modo3D)
                generador3D.sincronizarGrafo(grafo, unidadActual);
            else
                rutaHandler.actualizarVisuales(false, t, unidadActual);
        });
    }

    @FXML
    private void eliminarParticula() {
        String nombre = particulaEliminarComboBox.getValue();
        if (calculoHandler.estaCalculando()
                && nombre != null
                && nombre.equals(particulaOrigenComboBox.getValue())) {
            limpiarEstadoCalculo();
        }
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.eliminar(() -> {
            if (modo3D)
                generador3D.sincronizarGrafo(grafo, unidadActual);
            rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
        });
        if (nombre != null && nombre.equals(particulaOrigenComboBox.getValue()))
            particulaOrigenComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void editarParticula() {
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.editar(modo3D, t, () -> {
            if (modo3D)
                generador3D.sincronizarGrafo(grafo, unidadActual);
            rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
        });
    }

    @FXML
    private void agregarRuta() {
        rutaHandler.agregar(modo3D);
        CoordenadasTransformador t = crearTransformador();
        if (modo3D)
            generador3D.sincronizarGrafo(grafo, unidadActual);
        rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
    }

    @FXML
    private void eliminarRuta() {
        rutaHandler.eliminar();
        CoordenadasTransformador t = crearTransformador();
        if (modo3D)
            generador3D.sincronizarGrafo(grafo, unidadActual);
        rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
    }

    @FXML
    private void iniciarCalculo() {
        calculoHandler.iniciar(modo3D, unidadActual);
    }

    @FXML
    private void cancelarCalculo() {
        calculoHandler.cancelar();
    }

    @FXML
    private void calcularCampo() {
        calculoHandler.ejecutar(calculoHandler.getCalculoVersion(), modo3D, unidadActual);
    }

    @FXML
    private void calcularCampoUI() {
        calculoHandler.ejecutar(calculoHandler.getCalculoVersion(), modo3D, unidadActual);
    }

    @FXML
    private void generarCalculosDetallados() {
        if (ultimoResultado == null) {
            mostrarAlerta("Error", "Primero seleccione una partícula y presione 'Calcular'.");
            return;
        }
        detallesPdfHandler.mostrarTextoDetallado(ultimoResultado, unidadActual);
    }

    @FXML
    private void generarPDF() {
        detallesPdfHandler.generarPDF(ultimoResultado, unidadActual);
    }

    @FXML
    private void toggleModo3D() {
        modo3D = modo3DCheckBox.isSelected();
        modo3DHandler.toggle(modo3D, unidadActual);
    }

    @FXML
    private void onAnimacionTabSeleccionada(Event e) {
        animacionHandler.onTabSeleccionada(
                ultimoResultado, unidadActual, grafoPane, nodoCirculos,
                this::limpiarEstadoCalculo);
    }

    @FXML
    private void siguientePaso() {
        animacionHandler.siguiente(ultimoResultado);
    }

    @FXML
    private void anteriorPaso() {
        animacionHandler.anterior(ultimoResultado);
    }

    @FXML
    private void detenerAnimacion() {
        animacionHandler.detener();
    }

    @FXML
    private void reiniciarAnimacion() {
        animacionHandler.reiniciar(ultimoResultado, unidadActual, grafoPane, nodoCirculos);
    }

    @FXML
    private void Regresar() throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación de Salida");
        alert.setHeaderText("¿Está seguro de que desea salir?");
        alert.setContentText("Todo el trabajo actual y las partículas configuradas se borrarán.");

        ButtonType btnSi = new ButtonType("Sí, salir");
        ButtonType btnNo = new ButtonType("No, cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnSi, btnNo);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnSi) {
            App.setRoot("Simuladores");
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /** Crea el transformador con el estado actual de canvas y unidad. */
    private CoordenadasTransformador crearTransformador() {
        return etiquetaReposicionador.crearTransformador(unidadActual);
    }

    private void cambiarUnidad(UnidadDistancia nueva) {
        unidadActual = nueva;
        CoordenadasTransformador t = crearTransformador();
        rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
        if (modo3D)
            renderer.dibujarCuadrante3D(unidadActual, t);
        else
            renderer.dibujarCuadrante(unidadActual);
    }

    /** Limpia labels de resultado, flechas y cancelar cálculo activo. */
    private void limpiarEstadoCalculo() {
        if (calculoHandler.estaCalculando())
            calculoHandler.cancelar();
        renderer.limpiarFlechas();
        resultadoPotencialLabel.setText(" ");
        resultadoEnergiaLabel.setText(" ");
        ultimoResultado = null;
    }

    private void cargarSistemaPrueba() {
        nombreParticulaField.setText("q1");
        positivaToggle.setSelected(true);
        valorCargaField.setText("4");
        coordXField.setText("2");
        coordYField.setText("5");
        coordZField.setText("0");
        agregarParticula();

        nombreParticulaField.setText("q2");
        negativaToggle.setSelected(true);
        valorCargaField.setText("3");
        coordXField.setText("5");
        coordYField.setText("2");
        coordZField.setText("0");
        agregarParticula();

        nombreParticulaField.setText("q3");
        positivaToggle.setSelected(true);
        valorCargaField.setText("5");
        coordXField.setText("8");
        coordYField.setText("5");
        coordZField.setText("0");
        agregarParticula();

        origenRutaComboBox.setValue("q1");
        destinoRutaComboBox.setValue("q2");
        agregarRuta();
        origenRutaComboBox.setValue("q2");
        destinoRutaComboBox.setValue("q3");
        agregarRuta();
        origenRutaComboBox.setValue("q3");
        destinoRutaComboBox.setValue("q1");
        agregarRuta();

        nombreParticulaField.clear();
        positivaToggle.setSelected(true);
        valorCargaField.clear();
        coordXField.clear();
        coordYField.clear();
        coordZField.clear();

        scrollPane.setHvalue(0);
        scrollPane.setVvalue(1.0);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(" ");
        a.setContentText(mensaje);
        a.showAndWait();
    }

    // =========================================================================
    // ENLACE DE CAMPOS FXML A HANDLERS
    // =========================================================================

    private void enlazarCamposParticula() {
        particulaHandler.nombreParticulaField = nombreParticulaField;
        particulaHandler.valorCargaField = valorCargaField;
        particulaHandler.positivaToggle = positivaToggle;
        particulaHandler.negativaToggle = negativaToggle;
        particulaHandler.coordXField = coordXField;
        particulaHandler.coordYField = coordYField;
        particulaHandler.coordZField = coordZField;
        particulaHandler.particulaEliminarComboBox = particulaEliminarComboBox;
        particulaHandler.particulaEditarField = particulaEditarField;
        particulaHandler.particulaEditarComboBox = particulaEditarComboBox;
        particulaHandler.editCoordXField = editCoordXField;
        particulaHandler.editCoordYField = editCoordYField;
        particulaHandler.editCoordZField = editCoordZField;
    }

    private void enlazarCamposRuta() {
        rutaHandler.origenRutaComboBox = origenRutaComboBox;
        rutaHandler.destinoRutaComboBox = destinoRutaComboBox;
        rutaHandler.eliminarRutaComboBox = eliminarRutaComboBox;
    }

    private void enlazarCamposCalculo() {
        calculoHandler.particulaOrigenComboBox = particulaOrigenComboBox;
        calculoHandler.unidadDistanciaComboBox = unidadDistanciaComboBox;
        calculoHandler.calcularButton = calcularButton;
        calculoHandler.cancelarButton = cancelarButton;
        calculoHandler.resultadoPotencialLabel = resultadoPotencialLabel;
        calculoHandler.resultadoEnergiaLabel = resultadoEnergiaLabel;
    }

    private void enlazarCamposDetalles() {
        detallesPdfHandler.calculosDetalladosTextArea = calculosDetalladosTextArea;
    }

    private void enlazarCamposAnimacion() {
        animacionHandler.animacionTab = animacionTab;
        animacionHandler.pasoIndicadorLabel = pasoIndicadorLabel;
        animacionHandler.pasoDescripcionLabel = pasoDescripcionLabel;
        animacionHandler.barraProgresoLabel = barraProgresoLabel;
        animacionHandler.btnAnteriorPaso = btnAnteriorPaso;
        animacionHandler.btnSiguientePaso = btnSiguientePaso;
        animacionHandler.btnReiniciarAnimacion = btnReiniciarAnimacion;
        animacionHandler.btnDetenerAnimacion = btnDetenerAnimacion;
    }
}