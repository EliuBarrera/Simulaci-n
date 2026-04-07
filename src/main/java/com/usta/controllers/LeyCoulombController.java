package com.usta.controllers;

import com.usta.App;
import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoCalculo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.UnidadDistancia;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.usta.utils.GeneradorEscena3D;

/**
 * Controlador principal para la vista de Ley de Coulomb.
 *
 * Actúa como coordinador: mantiene el estado compartido
 * (Grafo, nodoCirculos, ultimoResultado, modo3D, unidadActual, rotación)
 * y delega la lógica a handlers especializados.
 *
 * Coordenadas del Nodo: siempre LÓGICAS (unidades del plano).
 * Conversión a pantalla: mediante CoordenadasTransformador.
 */
public class LeyCoulombController {

    // ── FXML: layout principal ────────────────────────────────────────────
    @FXML private Pane       grafoPane;
    @FXML private ScrollPane scrollPane;
    @FXML private Canvas     canvasPlano;
    @FXML private AnchorPane rootPane;

    // ── FXML: partículas ─────────────────────────────────────────────────
    @FXML private TextField  nombreParticulaField;
    @FXML private TextField  valorCargaField;
    @FXML private TextField  tipoCargaField;
    @FXML private ComboBox<String> particulaEliminarComboBox;
    @FXML private TextField  particulaEditarField;
    @FXML private ComboBox<String> particulaEditarComboBox;

    // ── FXML: coordenadas agregar ────────────────────────────────────────
    @FXML private TextField coordXField;
    @FXML private TextField coordYField;
    @FXML private TextField coordZField;
    @FXML private HBox      coordZBox;
    @FXML private CheckBox  modo3DCheckBox;
    @FXML private Label     modo3DInfoLabel;

    // ── FXML: coordenadas editar ─────────────────────────────────────────
    @FXML private TextField editCoordXField;
    @FXML private TextField editCoordYField;
    @FXML private TextField editCoordZField;
    @FXML private HBox      editCoordZBox;

    // ── FXML: rutas ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> origenRutaComboBox;
    @FXML private ComboBox<String> destinoRutaComboBox;
    @FXML private ComboBox<String> eliminarRutaComboBox;

    // ── FXML: cálculos ────────────────────────────────────────────────────
    @FXML private ComboBox<String>           particulaOrigenComboBox;
    @FXML private ComboBox<UnidadDistancia>  unidadDistanciaComboBox;
    @FXML private RadioButton  fuerzaTotalRadio;
    @FXML private RadioButton  fuerzasIndividualesRadio;
    @FXML private Button       calcularButton;
    @FXML private Button       cancelarButton;

    // ── FXML: resultados ─────────────────────────────────────────────────
    @FXML private Label    resultadoFuerzaLabel;
    @FXML private Label    resultadoCampoLabel;
    @FXML private TextArea calculosDetalladosTextArea;

    // ── FXML: animación ───────────────────────────────────────────────────
    @FXML private Tab    animacionTab;
    @FXML private Label  pasoIndicadorLabel;
    @FXML private Label  pasoDescripcionLabel;
    @FXML private Label  barraProgresoLabel;
    @FXML private Button btnAnteriorPaso;
    @FXML private Button btnSiguientePaso;
    @FXML private Button btnReiniciarAnimacion;
    @FXML private Button btnDetenerAnimacion;

    // ── Estado compartido ────────────────────────────────────────────────
    private Grafo                  grafo;
    private Map<Nodo, Circle>      nodoCirculos;
    private ObservableList<String> nombresParticulas;
    private boolean                modo3D       = false;
    private UnidadDistancia        unidadActual = UnidadDistancia.METROS;
    private ResultadoCalculo       ultimoResultado = null;
    private ToggleGroup            modoVisualizacionGroup;


    // ── Handlers ─────────────────────────────────────────────────────────
    private ParticulaHandler    particulaHandler;
    private RutaHandler         rutaHandler;
    private CalculoHandler      calculoHandler;
    private GrafoRenderer       renderer;
    private DetallesPdfHandler  detallesPdfHandler;
    private AnimacionTabHandler animacionHandler;
    private GeneradorEscena3D   generador3D;

    // =========================================================================
    // UTILIDADES DE TRANSFORMACIÓN
    // =========================================================================

    /** Crea un transformador con el estado actual (canvas, unidad, ángulos de rotación). */
    private CoordenadasTransformador crearTransformador() {
        double alphaDeg = 30;
        double betaDeg  = 30;
        return new CoordenadasTransformador(
            canvasPlano.getHeight(), canvasPlano.getWidth(),
            unidadActual, alphaDeg, betaDeg);
    }


    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================
    public void initialize() {
        grafo             = new Grafo();
        nodoCirculos      = new HashMap<>();
        nombresParticulas = FXCollections.observableArrayList();

        // Instanciar handlers
        particulaHandler   = new ParticulaHandler(grafo, nodoCirculos, nombresParticulas,
                                 canvasPlano, scrollPane, grafoPane);
        rutaHandler        = new RutaHandler(grafo, grafoPane);
        calculoHandler     = new CalculoHandler(grafo, canvasPlano);
        renderer           = new GrafoRenderer(grafoPane, canvasPlano);
        detallesPdfHandler = new DetallesPdfHandler(grafoPane);
        animacionHandler   = new AnimacionTabHandler();

        // Enlazar campos FXML a los handlers
        enlazarCamposParticula();
        enlazarCamposRuta();
        enlazarCamposCalculo();
        enlazarCamposDetalles();
        enlazarCamposAnimacion();

        // Configurar combos de partículas
        origenRutaComboBox.setItems(nombresParticulas);
        destinoRutaComboBox.setItems(nombresParticulas);
        particulaEliminarComboBox.setItems(nombresParticulas);
        particulaOrigenComboBox.setItems(nombresParticulas);
        if (particulaEditarComboBox != null)
            particulaEditarComboBox.setItems(nombresParticulas);
        eliminarRutaComboBox.setItems(FXCollections.observableArrayList());

        // Grupo de radio de visualización
        modoVisualizacionGroup = new ToggleGroup();
        fuerzaTotalRadio.setToggleGroup(modoVisualizacionGroup);
        fuerzasIndividualesRadio.setToggleGroup(modoVisualizacionGroup);
        fuerzaTotalRadio.setSelected(true);
        modoVisualizacionGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            renderer.limpiarFlechas();
            if (calculoHandler.estaCalculando())
                calculoHandler.ejecutar(calculoHandler.getCalculoVersion(), modo3D, unidadActual);
        });

        // Combo de unidades
        ObservableList<UnidadDistancia> unidades = FXCollections.observableArrayList(
            UnidadDistancia.KILOMETROS, UnidadDistancia.METROS,
            UnidadDistancia.CENTIMETROS, UnidadDistancia.MILIMETROS,
            UnidadDistancia.MICROMETROS, UnidadDistancia.NANOMETROS,
            UnidadDistancia.PICOMETROS
        );
        unidadDistanciaComboBox.setItems(unidades);
        unidadDistanciaComboBox.setValue(UnidadDistancia.METROS);
        unidadDistanciaComboBox.setOnAction(e -> cambiarUnidad(unidadDistanciaComboBox.getValue()));

        calculoHandler.setOnResultado(res -> {
            ultimoResultado = res;
            Nodo orig = res.getParticulaOrigen();
            Circle c = nodoCirculos.get(orig);
            if (c == null) return;

            if (modo3D) {
                generador3D.sincronizarGrafo(grafo, unidadActual);
                if (fuerzasIndividualesRadio.isSelected()) {
                    for (com.usta.models.ResultadoFuerza rf : res.getFuerzasIndividuales()) {
                        generador3D.dibujarFicha(orig.getX(), orig.getY(), orig.getZ(),
                            rf.getFx(), rf.getFy(), rf.getFz(), javafx.scene.paint.Color.ORANGE);
                    }
                } else {
                    generador3D.dibujarFicha(orig.getX(), orig.getY(), orig.getZ(),
                        res.getFuerzaTotalX(), res.getFuerzaTotalY(), res.getFuerzaTotalZ(), javafx.scene.paint.Color.RED);
                }
            } else {
                double sx = c.getCenterX(), sy = c.getCenterY();
                if (fuerzasIndividualesRadio.isSelected()) {
                    renderer.dibujarFlechasIndividuales(sx, sy, res.getFuerzasIndividuales());
                } else {
                    renderer.dibujarFlechaFuerza(sx, sy, res.getFuerzaTotalX(), res.getFuerzaTotalY());
                }
            }
        });
        calculoHandler.setOnCancelar(() -> {
            renderer.limpiarFlechas();
            if (modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
        });

        // Canvas inicial
        canvasPlano.widthProperty().addListener((obs, o, n) -> renderer.dibujarCuadrante(unidadActual));
        canvasPlano.heightProperty().addListener((obs, o, n) -> renderer.dibujarCuadrante(unidadActual));
        canvasPlano.setWidth(2100);
        canvasPlano.setHeight(1300);
        renderer.dibujarCuadrante(unidadActual);

        // TextArea de detalles
        if (calculosDetalladosTextArea != null) {
            calculosDetalladosTextArea.setEditable(false);
            calculosDetalladosTextArea.setWrapText(true);
            calculosDetalladosTextArea.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        }

        // Inicializar 3D nativo
        generador3D = new GeneradorEscena3D(1000, 800);
        javafx.beans.binding.DoubleBinding rootWidth = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> rootPane != null ? rootPane.getWidth() : grafoPane.getWidth(),
            grafoPane.widthProperty()
        );
        generador3D.getSubScene().widthProperty().bind(rootWidth);
        generador3D.getSubScene().heightProperty().bind(grafoPane.heightProperty());
        generador3D.getSubScene().setVisible(false);
        generador3D.getSubScene().setManaged(false);
        grafoPane.getChildren().add(0, generador3D.getSubScene());
    }

    // =========================================================================
    // ACCIONES FXML — delegan en los handlers
    // =========================================================================

    @FXML private void agregarParticula() {
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.agregar(modo3D, t, () -> {
            Nodo ultimo = grafo.getNodos().get(grafo.getNodos().size() - 1);
            Circle c = nodoCirculos.get(ultimo);
            if (c != null) hacerNodoArrastrable(c, ultimo);
            if (modo3D) {
                generador3D.sincronizarGrafo(grafo, unidadActual);
            } else {
                rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
            }
        });
    }

    @FXML private void eliminarParticula() {
        String nombre = particulaEliminarComboBox.getValue();
        if (calculoHandler.estaCalculando() &&
            nombre != null && nombre.equals(particulaOrigenComboBox.getValue())) {
            calculoHandler.cancelar();
            resultadoFuerzaLabel.setText(" ");
            resultadoCampoLabel.setText(" ");
            renderer.limpiarFlechas();
        }
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.eliminar(() -> {
            if(modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
            rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
        });
        if (nombre != null && nombre.equals(particulaOrigenComboBox.getValue()))
            particulaOrigenComboBox.getSelectionModel().clearSelection();
    }

    @FXML private void editarParticula() {
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.editar(modo3D, t, () -> {
            if(modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
            rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
        });
    }

    @FXML private void agregarRuta() {
        rutaHandler.agregar(modo3D);
        CoordenadasTransformador t = crearTransformador();
        if(modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
        rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
    }

    @FXML private void eliminarRuta() {
        rutaHandler.eliminar();
        CoordenadasTransformador t = crearTransformador();
        if(modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
        rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
    }

    @FXML private void iniciarCalculo()  { calculoHandler.iniciar(modo3D, unidadActual); }
    @FXML private void cancelarCalculo() { calculoHandler.cancelar(); }
    @FXML private void calcularCampo()   { calculoHandler.ejecutar(calculoHandler.getCalculoVersion(), modo3D, unidadActual); }
    @FXML private void calcularCampoUI() { calculoHandler.ejecutar(calculoHandler.getCalculoVersion(), modo3D, unidadActual); }

    @FXML private void generarCalculosDetallados() {
        if (ultimoResultado == null) {
            mostrarAlerta("Error", "Primero seleccione una partícula y presione 'Calcular'.");
            return;
        }
        detallesPdfHandler.mostrarTextoDetallado(ultimoResultado, unidadActual);
    }

    @FXML private void generarPDF() {
        detallesPdfHandler.generarPDF(ultimoResultado, unidadActual);
    }

    @FXML private void toggleModo3D() {
        modo3D = modo3DCheckBox.isSelected();
        coordZBox.setVisible(modo3D);
        coordZBox.setManaged(modo3D);
        modo3DInfoLabel.setVisible(modo3D);
        modo3DInfoLabel.setManaged(modo3D);

        // Mostrar/ocultar campo Z de edición
        if (editCoordZBox != null) {
            editCoordZBox.setVisible(modo3D);
            editCoordZBox.setManaged(modo3D);
        }

        if (calculoHandler.estaCalculando()) calculoHandler.cancelar();
        renderer.limpiarFlechas();
        resultadoFuerzaLabel.setText(" ");
        resultadoCampoLabel.setText(" ");
        ultimoResultado = null;

        if (modo3D) {
            canvasPlano.setVisible(false);
            generador3D.getSubScene().setVisible(true);
            generador3D.getSubScene().setManaged(true);
            
            for (javafx.scene.Node n : grafoPane.getChildren()) {
                if (n != canvasPlano && n != generador3D.getSubScene() && !(n instanceof javafx.scene.layout.GridPane)) {
                    n.setVisible(false);
                }
            }
            generador3D.sincronizarGrafo(grafo, unidadActual);
        } else {
            canvasPlano.setVisible(true);
            generador3D.getSubScene().setVisible(false);
            generador3D.getSubScene().setManaged(false);

            canvasPlano.setWidth(2100);
            canvasPlano.setHeight(1300);
            renderer.dibujarCuadrante(unidadActual);

            for (javafx.scene.Node n : grafoPane.getChildren()) {
                if (n != canvasPlano && n != generador3D.getSubScene()) {
                    n.setVisible(true);
                }
            }
            reposicionarParticulas();
            CoordenadasTransformador t = crearTransformador();
            rutaHandler.actualizarVisuales(false, t, unidadActual);
        }
    }

    @FXML private void onAnimacionTabSeleccionada(Event e) {
        animacionHandler.onTabSeleccionada(
            ultimoResultado, unidadActual, grafoPane, nodoCirculos,
            () -> {
                if (calculoHandler.estaCalculando()) calculoHandler.cancelar();
                renderer.limpiarFlechas();
                resultadoFuerzaLabel.setText(" ");
                resultadoCampoLabel.setText(" ");
            });
    }

    @FXML private void siguientePaso()   { animacionHandler.siguiente(ultimoResultado); }
    @FXML private void anteriorPaso()    { animacionHandler.anterior(ultimoResultado);  }
    @FXML private void detenerAnimacion(){ animacionHandler.detener(); }
    @FXML private void reiniciarAnimacion() {
        animacionHandler.reiniciar(ultimoResultado, unidadActual, grafoPane, nodoCirculos);
    }

    @FXML private void Regresar() throws IOException {
        App.setRoot("Simuladores");
    }

    // =========================================================================
    // ESTADO COMPARTIDO — helpers
    // =========================================================================

    private void cambiarUnidad(UnidadDistancia nueva) {
        unidadActual = nueva;
        CoordenadasTransformador t = crearTransformador();
        rutaHandler.actualizarVisuales(modo3D, t, unidadActual);
        if (modo3D) {
            renderer.dibujarCuadrante3D(unidadActual, t);
        } else {
            renderer.dibujarCuadrante(unidadActual);
        }
    }

    /**
     * Recalcula la posición de pantalla de todas las partículas
     * a partir de sus coordenadas lógicas.
     */
    private void reposicionarParticulas() {
        CoordenadasTransformador t = crearTransformador();
        for (Map.Entry<Nodo, Circle> entry : nodoCirculos.entrySet()) {
            Nodo nodo = entry.getKey();
            Circle c  = entry.getValue();
            double[] screen = t.logicalToScreen(nodo.getX(), nodo.getY(), nodo.getZ(), modo3D);
            c.setCenterX(screen[0]);
            c.setCenterY(screen[1]);
            // Actualizar etiqueta de texto
            grafoPane.getChildren().stream()
                .filter(n -> n instanceof Text && ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                .findFirst()
                .ifPresent(n -> {
                    ((Text) n).setX(screen[0] - 4);
                    ((Text) n).setY(screen[1] + 4);
                });
        }
    }

    /**
     * Hace arrastrable un nodo en el plano.
     *
     * - Click izquierdo + arrastrar → mueve en X/Y (Z constante).
     * - Shift + arrastrar vertical → mueve en Z (X/Y constantes, solo en modo 3D).
     */
    private void hacerNodoArrastrable(Circle circulo, Nodo nodo) {
        final double[] delta = new double[4]; // [0]=offsetX, [1]=offsetY, [2]=startLogZ, [3]=startScreenY

        circulo.setOnMousePressed((MouseEvent me) -> {
            if (me.getButton() != MouseButton.PRIMARY) return;
            delta[0] = circulo.getCenterX() - me.getX();
            delta[1] = circulo.getCenterY() - me.getY();
            delta[2] = nodo.getZ();
            delta[3] = me.getY();
            circulo.setCursor(Cursor.MOVE);
            me.consume();
        });

        circulo.setOnMouseReleased(me -> {
            circulo.setCursor(Cursor.HAND);
            me.consume();
        });

        circulo.setOnMouseDragged((MouseEvent me) -> {
            if (me.getButton() != MouseButton.PRIMARY) return;

            CoordenadasTransformador t = crearTransformador();

            if (modo3D && me.isShiftDown()) {
                // ── Shift + arrastrar → mover en Z ──────────────────────
                double dy = me.getY() - delta[3];
                double newZ = delta[2] - dy / t.getPxPorUnidad();
                newZ = Math.max(0, Math.min(10, newZ));
                nodo.setZ(newZ);
            } else {
                // ── Arrastrar normal → mover en X/Y ─────────────────────
                double nx = me.getX() + delta[0];
                double ny = me.getY() + delta[1];

                double[] logCoords = t.screenToLogical(nx, ny, nodo.getZ(), modo3D);
                nodo.setX(logCoords[0]);
                nodo.setY(logCoords[1]);
            }

            // Recalcular posición de pantalla y actualizar Circle
            double[] screen = t.logicalToScreen(nodo.getX(), nodo.getY(), nodo.getZ(), modo3D);
            circulo.setCenterX(screen[0]);
            circulo.setCenterY(screen[1]);

            // Actualizar aristas
            rutaHandler.actualizarVisuales(modo3D, t, unidadActual);

            // Actualizar etiqueta de texto
            grafoPane.getChildren().stream()
                .filter(n -> n instanceof Text &&
                    ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                .findFirst()
                .ifPresent(n -> {
                    ((Text) n).setX(screen[0] - 4);
                    ((Text) n).setY(screen[1] + 4);
                });

            me.consume();
        });

        circulo.setOnMouseEntered(me -> circulo.setCursor(Cursor.HAND));
    }

    // =========================================================================
    // ENLACE DE CAMPOS FXML A HANDLERS
    // =========================================================================
    private void enlazarCamposParticula() {
        particulaHandler.nombreParticulaField    = nombreParticulaField;
        particulaHandler.valorCargaField         = valorCargaField;
        particulaHandler.tipoCargaField          = tipoCargaField;
        particulaHandler.coordXField             = coordXField;
        particulaHandler.coordYField             = coordYField;
        particulaHandler.coordZField             = coordZField;
        particulaHandler.particulaEliminarComboBox = particulaEliminarComboBox;
        particulaHandler.particulaEditarField    = particulaEditarField;
        particulaHandler.particulaEditarComboBox = particulaEditarComboBox;
        particulaHandler.editCoordXField         = editCoordXField;
        particulaHandler.editCoordYField         = editCoordYField;
        particulaHandler.editCoordZField         = editCoordZField;
    }

    private void enlazarCamposRuta() {
        rutaHandler.origenRutaComboBox   = origenRutaComboBox;
        rutaHandler.destinoRutaComboBox  = destinoRutaComboBox;
        rutaHandler.eliminarRutaComboBox = eliminarRutaComboBox;
    }

    private void enlazarCamposCalculo() {
        calculoHandler.particulaOrigenComboBox    = particulaOrigenComboBox;
        calculoHandler.unidadDistanciaComboBox    = unidadDistanciaComboBox;
        calculoHandler.fuerzasIndividualesRadio   = fuerzasIndividualesRadio;
        calculoHandler.calcularButton             = calcularButton;
        calculoHandler.cancelarButton             = cancelarButton;
        calculoHandler.resultadoFuerzaLabel       = resultadoFuerzaLabel;
        calculoHandler.resultadoCampoLabel        = resultadoCampoLabel;
    }

    private void enlazarCamposDetalles() {
        detallesPdfHandler.calculosDetalladosTextArea = calculosDetalladosTextArea;
    }

    private void enlazarCamposAnimacion() {
        animacionHandler.animacionTab         = animacionTab;
        animacionHandler.pasoIndicadorLabel   = pasoIndicadorLabel;
        animacionHandler.pasoDescripcionLabel = pasoDescripcionLabel;
        animacionHandler.barraProgresoLabel   = barraProgresoLabel;
        animacionHandler.btnAnteriorPaso      = btnAnteriorPaso;
        animacionHandler.btnSiguientePaso     = btnSiguientePaso;
        animacionHandler.btnReiniciarAnimacion = btnReiniciarAnimacion;
        animacionHandler.btnDetenerAnimacion  = btnDetenerAnimacion;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(" "); a.setContentText(mensaje);
        a.showAndWait();
    }
}
