package com.usta.controllers;

import com.usta.App;
import com.usta.controllers.Handlers.CalculoPotencialHandler;
import com.usta.controllers.Handlers.DetallesPdfPotencialHandler;
import com.usta.controllers.Handlers.EtiquetaReposicionador;
import com.usta.controllers.Handlers.GrafoRenderer;
import com.usta.controllers.Handlers.Modo3DHandler;
import com.usta.controllers.Handlers.NodoDragHandler;
import com.usta.controllers.Handlers.ParticulaHandler;
import com.usta.controllers.Handlers.RutaHandler;
import com.usta.models.Arista;
import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoPotencial;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.GeneradorEscena3D;
import com.usta.utils.LineasDeCampoRenderer;
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
 * Controlador principal para la vista de Potencial Eléctrico.
 *
 * Actúa exclusivamente como <em>coordinador</em>: mantiene el estado
 * compartido e instancia/enlaza los handlers especializados.
 *
 * Responsabilidades propias:
 * - Mantener {@code modo3D} y {@code unidadActual} como fuente de verdad.
 * - Inicializar el grafo, los handlers y los controles FXML.
 * - Recibir eventos @FXML y delegar sin lógica adicional.
 * - Auto-conectar partículas (conexiones automáticas entre todas las partículas).
 * - Dibujar líneas de campo eléctrico en modo 2D.
 *
 * Lógica delegada:
 * - Partículas → {@link ParticulaHandler}
 * - Rutas → {@link RutaHandler}  (usadas internamente para auto-conexiones visuales)
 * - Cálculo → {@link CalculoPotencialHandler}
 * - Renderizado → {@link GrafoRenderer}
 * - PDF/Detalles → {@link DetallesPdfPotencialHandler}
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

    // ── FXML: cálculos ────────────────────────────────────────────────────────
    @FXML
    private ComboBox<String> particulaOrigenComboBox;
    @FXML
    private ComboBox<UnidadDistancia> unidadDistanciaComboBox;
    @FXML
    private Button calcularButton;
    @FXML
    private Button cancelarButton;

    // ── FXML: líneas de campo ─────────────────────────────────────────────────
    @FXML
    private CheckBox lineasCampoCheckBox;

    // ── FXML: resultados ──────────────────────────────────────────────────────
    @FXML
    private Label resultadoPotencialLabel;
    @FXML
    private Label resultadoEnergiaLabel;
    @FXML
    private TextArea calculosDetalladosTextArea;

    // ── Estado compartido ─────────────────────────────────────────────────────
    private Grafo grafo;
    private Map<Nodo, Circle> nodoCirculos;
    private ObservableList<String> nombresParticulas;
    private boolean modo3D = false;
    private boolean lineasCampoActivas = false;
    private UnidadDistancia unidadActual = UnidadDistancia.METROS;
    private ResultadoPotencial ultimoResultado;
    private ToggleGroup polaridadGroup;

    // ── Handlers ──────────────────────────────────────────────────────────────
    private ParticulaHandler particulaHandler;
    private RutaHandler rutaHandler;
    private CalculoPotencialHandler calculoHandler;
    private GrafoRenderer renderer;
    private DetallesPdfPotencialHandler detallesPdfHandler;
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
        dragHandler.setOnDragUpdate(this::redibujarCanvas);

        // Nota: animacionTab ya no existe, se pasa null al Modo3DHandler
        modo3DHandler = new Modo3DHandler(
                canvasPlano, scrollPane, grafoPane, tabPanePrincipal, null,
                coordZBox, editCoordZBox, modo3DInfoLabel,
                generador3D, renderer, rutaHandler, grafo, nodoCirculos,
                etiquetaReposicionador,
                this::limpiarEstadoCalculo);

        // Enlazar campos FXML a los handlers
        enlazarCamposParticula();
        enlazarCamposRutaInterno();
        enlazarCamposCalculo();
        enlazarCamposDetalles();

        // Combos de partículas
        particulaEliminarComboBox.setItems(nombresParticulas);
        particulaOrigenComboBox.setItems(nombresParticulas);
        if (particulaEditarComboBox != null)
            particulaEditarComboBox.setItems(nombresParticulas);

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
        canvasPlano.widthProperty().addListener((obs, o, n) -> redibujarCanvas());
        canvasPlano.heightProperty().addListener((obs, o, n) -> redibujarCanvas());
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
            // Auto-conectar la nueva partícula con todas las demás
            autoConectarParticulas();
            if (modo3D)
                generador3D.sincronizarGrafo(grafo, unidadActual);
            else {
                rutaHandler.actualizarVisuales(false, t, unidadActual);
                redibujarCanvas();
            }
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
            // Re-crear auto-conexiones tras la eliminación
            autoConectarParticulas();
            if (modo3D)
                generador3D.sincronizarGrafo(grafo, unidadActual);
            rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
            redibujarCanvas();
        });
        if (nombre != null && nombre.equals(particulaOrigenComboBox.getValue()))
            particulaOrigenComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void editarParticula() {
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.editar(modo3D, t, () -> {
            // Re-crear auto-conexiones tras la edición
            autoConectarParticulas();
            if (modo3D)
                generador3D.sincronizarGrafo(grafo, unidadActual);
            rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
            redibujarCanvas();
        });
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
        // Desactivar líneas de campo en 3D
        if (modo3D && lineasCampoActivas) {
            lineasCampoActivas = false;
            lineasCampoCheckBox.setSelected(false);
        }
        // Ocultar/mostrar el checkbox de líneas de campo según el modo
        lineasCampoCheckBox.setDisable(modo3D);
        modo3DHandler.toggle(modo3D, unidadActual);
    }

    @FXML
    private void toggleLineasCampo() {
        lineasCampoActivas = lineasCampoCheckBox.isSelected();
        redibujarCanvas();
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
            redibujarCanvas();
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

    /**
     * Redibuja el canvas: cuadrícula + líneas de campo (si están activas).
     */
    private void redibujarCanvas() {
        renderer.dibujarCuadrante(unidadActual);
        if (lineasCampoActivas && !modo3D) {
            LineasDeCampoRenderer.dibujar(canvasPlano, grafo.getNodos(), unidadActual);
        }
    }

    /**
     * Auto-conecta todas las partículas entre sí.
     * Limpia las aristas anteriores y crea una conexión completa (grafo completo)
     * entre todos los nodos presentes. Esto es exclusivo de Potencial Eléctrico:
     * en Ley de Coulomb las conexiones siguen siendo manuales.
     */
    private void autoConectarParticulas() {
        // Limpiar todas las aristas existentes
        grafo.getAristas().clear();

        // Crear conexiones entre todos los pares de partículas
        java.util.List<Nodo> nodos = grafo.getNodos();
        for (int i = 0; i < nodos.size(); i++) {
            for (int j = i + 1; j < nodos.size(); j++) {
                Nodo a = nodos.get(i);
                Nodo b = nodos.get(j);
                double dist;
                if (modo3D) {
                    dist = Math.sqrt(
                            Math.pow(a.getX() - b.getX(), 2) +
                            Math.pow(a.getY() - b.getY(), 2) +
                            Math.pow(a.getZ() - b.getZ(), 2));
                } else {
                    dist = Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
                }
                grafo.agregarArista(new Arista(a, b, dist));
            }
        }
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

        // Las conexiones son automáticas, no se necesitan rutas manuales

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

    /**
     * Enlaza los combos del RutaHandler internamente.
     * En Potencial Eléctrico los combos de conexiones ya no existen en la UI,
     * pero el RutaHandler aún se usa para dibujar las líneas de conexión visuales.
     * Se crean ComboBox internos que no se muestran al usuario.
     */
    private void enlazarCamposRutaInterno() {
        rutaHandler.origenRutaComboBox = new ComboBox<>();
        rutaHandler.destinoRutaComboBox = new ComboBox<>();
        rutaHandler.eliminarRutaComboBox = new ComboBox<>();
        // Estos combos son internos; el usuario no interactúa con ellos
        rutaHandler.origenRutaComboBox.setItems(nombresParticulas);
        rutaHandler.destinoRutaComboBox.setItems(nombresParticulas);
        rutaHandler.eliminarRutaComboBox.setItems(FXCollections.observableArrayList());
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
}