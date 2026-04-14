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
import javafx.scene.input.ScrollEvent;
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
    @FXML private TabPane    tabPanePrincipal;

    // ── FXML: partículas ─────────────────────────────────────────────────
    @FXML private TextField  nombreParticulaField;
    @FXML private TextField  valorCargaField;
    @FXML private ToggleButton positivaToggle;
    @FXML private ToggleButton negativaToggle;
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
    private ToggleGroup            polaridadGroup;


    // ── Handlers ─────────────────────────────────────────────────────────
    private ParticulaHandler    particulaHandler;
    private RutaHandler         rutaHandler;
    private CalculoHandler      calculoHandler;
    private GrafoRenderer       renderer;
    private DetallesPdfHandler  detallesPdfHandler;
    private AnimacionTabHandler animacionHandler;
    private GeneradorEscena3D   generador3D;

    private java.util.List<Text> etiquetasEjes = new java.util.ArrayList<>();


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

        // Grupo de polaridad (Dual Button)
        polaridadGroup = new ToggleGroup();
        positivaToggle.setToggleGroup(polaridadGroup);
        negativaToggle.setToggleGroup(polaridadGroup);
        positivaToggle.setSelected(true);

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
        // Bind SubScene al viewport del ScrollPane (area disponible sin el TabPane)
        generador3D.getSubScene().widthProperty().bind(scrollPane.widthProperty().subtract(2));
        generador3D.getSubScene().heightProperty().bind(scrollPane.heightProperty().subtract(2));
        generador3D.getSubScene().setVisible(false);
        generador3D.getSubScene().setManaged(false);
        grafoPane.getChildren().add(0, generador3D.getSubScene());

        // Auto-cargar sistema de prueba
        javafx.application.Platform.runLater(() -> {
            nombreParticulaField.setText("q1"); positivaToggle.setSelected(true); valorCargaField.setText("4");
            coordXField.setText("2"); coordYField.setText("5"); coordZField.setText("0"); agregarParticula();
            
            nombreParticulaField.setText("q2"); negativaToggle.setSelected(true); valorCargaField.setText("3");
            coordXField.setText("5"); coordYField.setText("2"); coordZField.setText("0"); agregarParticula();
            
            nombreParticulaField.setText("q3"); positivaToggle.setSelected(true); valorCargaField.setText("5");
            coordXField.setText("8"); coordYField.setText("5"); coordZField.setText("0"); agregarParticula();
            
            origenRutaComboBox.setValue("q1"); destinoRutaComboBox.setValue("q2"); agregarRuta();
            origenRutaComboBox.setValue("q2"); destinoRutaComboBox.setValue("q3"); agregarRuta();
            origenRutaComboBox.setValue("q3"); destinoRutaComboBox.setValue("q1"); agregarRuta();
            
            nombreParticulaField.clear(); positivaToggle.setSelected(true); valorCargaField.clear();
            coordXField.clear(); coordYField.clear(); coordZField.clear();
        });

        // ── Anti-zoom fantasma ───────────────────────────────────────────
        // 1. Interceptar scroll en la FASE DE CAPTURA del ScrollPane
        //    (antes de que el ScrollPane lo procese) cuando estamos en 3D
        scrollPane.addEventFilter(ScrollEvent.ANY, e -> {
            if (modo3D) {
                e.consume();
            }
        });

        // 2. Consumir todo scroll dentro del TabPane para que no afecte el viewport
        tabPanePrincipal.addEventFilter(ScrollEvent.ANY, Event::consume);

        // 3. Al cambiar de pestaña, guardar y restaurar la posición del scroll
        //    para evitar "saltos" causados por el re-layout del TabPane
        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            double h = scrollPane.getHvalue();
            double v = scrollPane.getVvalue();
            javafx.application.Platform.runLater(() -> {
                scrollPane.setHvalue(h);
                scrollPane.setVvalue(v);
            });
        });

        // Listeners para actualizar etiquetas en 3D al mover la cámara
        generador3D.getCameraRotX().angleProperty().addListener((obs,o,n) -> reposicionarEtiquetas3D());
        generador3D.getCameraRotY().angleProperty().addListener((obs,o,n) -> reposicionarEtiquetas3D());
        generador3D.getCameraPan().xProperty().addListener((obs,o,n) -> reposicionarEtiquetas3D());
        generador3D.getCameraPan().yProperty().addListener((obs,o,n) -> reposicionarEtiquetas3D());
        generador3D.getCamera().translateZProperty().addListener((obs,o,n) -> reposicionarEtiquetas3D());
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
            // Resetear la cámara 3D a posición predeterminada
            generador3D.resetCamera();
            canvasPlano.setVisible(false);
            canvasPlano.setWidth(0);
            canvasPlano.setHeight(0);
            generador3D.getSubScene().setVisible(true);
            generador3D.getSubScene().setManaged(true);
            
            canvasPlano.setVisible(false);
            canvasPlano.setManaged(false);

            // Crear etiquetas de ejes si no existen
            if (etiquetasEjes.isEmpty()) {
                for (int i = 1; i <= 10; i++) {
                    Text tx = new Text(String.valueOf(i)); tx.setFill(javafx.scene.paint.Color.RED); tx.setFont(javafx.scene.text.Font.font("System Bold", 10));
                    Text ty = new Text(String.valueOf(i)); ty.setFill(javafx.scene.paint.Color.GREEN); ty.setFont(javafx.scene.text.Font.font("System Bold", 10));
                    Text tz = new Text(String.valueOf(i)); tz.setFill(javafx.scene.paint.Color.BLUE); tz.setFont(javafx.scene.text.Font.font("System Bold", 10));
                    etiquetasEjes.addAll(java.util.Arrays.asList(tx, ty, tz));
                    grafoPane.getChildren().addAll(tx, ty, tz);
                }
            }
            etiquetasEjes.forEach(e -> e.setVisible(true));

            for (javafx.scene.Node n : grafoPane.getChildren()) {
                if (n != canvasPlano && n != generador3D.getSubScene() && !etiquetasEjes.contains(n)) {

                    n.setVisible(false);
                    n.setManaged(false);
                }
            }
            generador3D.sincronizarGrafo(grafo, unidadActual);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            // Bloqueo matematico del scroll y rubber banding interno
            scrollPane.setPannable(false);
            scrollPane.setHmax(0);
            scrollPane.setVmax(0);
            scrollPane.setHvalue(0);
            scrollPane.setVvalue(0);
        } else {
            canvasPlano.setVisible(true);
            canvasPlano.setManaged(true);
            canvasPlano.setWidth(2100);
            canvasPlano.setHeight(1300);
            renderer.dibujarCuadrante(unidadActual);
            generador3D.getSubScene().setVisible(false);
            generador3D.getSubScene().setManaged(false);

            etiquetasEjes.forEach(e -> e.setVisible(false));

            for (javafx.scene.Node n : grafoPane.getChildren()) {
                if (n != canvasPlano && n != generador3D.getSubScene() && !etiquetasEjes.contains(n)) {

                    n.setVisible(true);
                    n.setManaged(true);
                }
            }
            reposicionarParticulas();
            CoordenadasTransformador t = crearTransformador();
            rutaHandler.actualizarVisuales(false, t, unidadActual);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setPannable(true);
            scrollPane.setHmax(1.0);
            scrollPane.setVmax(1.0);
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
     * Proyecta las posiciones 3D a la pantalla 2D y actualiza todas las etiquetas
     * (partículas, ejes y distancias).
     */
    private void reposicionarEtiquetas3D() {
        if (!modo3D || generador3D == null) return;
        
        double scale = generador3D.getScale();
        javafx.scene.Group world = generador3D.getWorld();

        // 1. Etiquetas de Partículas (Nombre, Carga, Posición)
        for (Map.Entry<com.usta.models.Nodo, Circle> entry : nodoCirculos.entrySet()) {
            com.usta.models.Nodo nodo = entry.getKey();
            
            javafx.geometry.Point3D p3d = world.localToScene(nodo.getX() * scale, -nodo.getY() * scale, nodo.getZ() * scale);
            javafx.geometry.Point2D pLocal = grafoPane.sceneToLocal(p3d.getX(), p3d.getY());
            
            if (pLocal != null) {
                grafoPane.getChildren().stream()
                    .filter(n -> n instanceof Text && ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                    .findFirst()
                    .ifPresent(n -> {
                        Text txt = (Text) n;
                        txt.setVisible(true);
                        txt.setX(pLocal.getX() + 15);
                        txt.setY(pLocal.getY() - 15);
                        String nuevaEtiqueta = String.format("%s (%s %s)\nPos: (%.1f, %.1f, %.1f)", 
                            nodo.getNombre(), nodo.getValorCarga(), nodo.getTipoCarga(), 
                            nodo.getX(), nodo.getY(), nodo.getZ());
                        txt.setText(nuevaEtiqueta);
                    });
            }
        }

        // 2. Etiquetas de Ejes (Números 1-10)
        if (!etiquetasEjes.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                double val = i + 1;
                // X (rojo)
                updateLabel3D(etiquetasEjes.get(i*3), val * scale, 0, 0);
                // Y (verde)
                updateLabel3D(etiquetasEjes.get(i*3+1), 0, -val * scale, 0);
                // Z (azul)
                updateLabel3D(etiquetasEjes.get(i*3+2), 0, 0, val * scale);
            }
        }

        // 3. Etiquetas de Distancia (Aristas)
        for (javafx.scene.Node n : grafoPane.getChildren()) {
            if (n instanceof Text && n.getUserData() instanceof Object[]) {
                Object[] data = (Object[]) n.getUserData();
                if ("arista".equals(data[0])) {
                    Text txt = (Text) n;
                    com.usta.models.Arista a = (com.usta.models.Arista) data[1];
                    com.usta.models.Nodo o = a.getOrigen();
                    com.usta.models.Nodo d = a.getDestino();
                    
                    double mx = (o.getX() + d.getX()) / 2.0;
                    double my = (o.getY() + d.getY()) / 2.0;
                    double mz = (o.getZ() + d.getZ()) / 2.0;
                    
                    javafx.geometry.Point3D p3d = world.localToScene(mx * scale, -my * scale, mz * scale);
                    javafx.geometry.Point2D pLocal = grafoPane.sceneToLocal(p3d.getX(), p3d.getY());
                    
                    if (pLocal != null) {
                        txt.setVisible(true);
                        txt.setX(pLocal.getX());
                        txt.setY(pLocal.getY());
                        txt.setText(String.format("%.2f %s", a.getPeso(), unidadActual.getSimbolo()));
                    }
                }
            }
        }
    }


    private void updateLabel3D(Text txt, double x, double y, double z) {
        javafx.geometry.Point3D p3d = generador3D.getWorld().localToScene(x, y, z);
        javafx.geometry.Point2D pLocal = grafoPane.sceneToLocal(p3d.getX(), p3d.getY());
        if (pLocal != null) {
            txt.setX(pLocal.getX() + 5);
            txt.setY(pLocal.getY() - 5);
        }
    }

    /**
     * Recalcula la posición de pantalla de todas las partículas
     * a partir de sus coordenadas lógicas.
     */
    private void reposicionarParticulas() {
        if (modo3D) {
            reposicionarEtiquetas3D();
            return;
        }
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
                    Text txt = (Text) n;
                    txt.setX(screen[0] - 4);
                    txt.setY(screen[1] + 4);
                    
                    // Asegurar que el contenido refleje si estamos en 3D (mostrando Z) o 2D (ocultando Z)
                    String nuevaEtiqueta = modo3D
                        ? nodo.getNombre() + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ") z=" + String.format("%.1f", nodo.getZ())
                        : nodo.getNombre() + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ")";
                    txt.setText(nuevaEtiqueta);
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
                    Text txt = (Text) n;
                    txt.setX(screen[0] - 4);
                    txt.setY(screen[1] + 4);

                    // Actualizar el texto para que coincida con el modo actual (ocultar Z en 2D)
                    String nuevaEtiqueta = modo3D
                        ? nodo.getNombre() + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ") z=" + String.format("%.1f", nodo.getZ())
                        : nodo.getNombre() + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ")";
                    txt.setText(nuevaEtiqueta);
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
        particulaHandler.positivaToggle          = positivaToggle;
        particulaHandler.negativaToggle          = negativaToggle;
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
