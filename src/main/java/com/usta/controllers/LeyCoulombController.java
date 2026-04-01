package com.usta.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import com.usta.App;
import com.usta.models.Arista;
import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoCalculo;
import com.usta.models.ResultadoFuerza;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.CoulombCalculator;
import com.usta.utils.PdfGenerator;
import com.usta.utils.UnidadDistancia;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LeyCoulombController {

    // ── FXML: layout principal ────────────────────────────────────────────────
    @FXML private Pane       grafoPane;
    @FXML private ScrollPane scrollPane;
    @FXML private Canvas     canvasPlano;
    @FXML private AnchorPane rootPane;

    // ── FXML: partículas ─────────────────────────────────────────────────────
    @FXML private TextField  nombreParticulaField;
    @FXML private TextField  valorCargaField;
    @FXML private TextField  tipoCargaField;
    @FXML private ComboBox<String> particulaEliminarComboBox;
    @FXML private TextField  particulaEditarField;
    @FXML private ComboBox<String> particulaEditarComboBox;

    // ── FXML: rutas ───────────────────────────────────────────────────────────
    @FXML private ComboBox<String> origenRutaComboBox;
    @FXML private ComboBox<String> destinoRutaComboBox;
    @FXML private ComboBox<String> eliminarRutaComboBox;

    // ── FXML: cálculos ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> particulaOrigenComboBox;
    @FXML private ComboBox<UnidadDistancia> unidadDistanciaComboBox;
    @FXML private RadioButton fuerzaTotalRadio;
    @FXML private RadioButton fuerzasIndividualesRadio;
    @FXML private Button calcularButton;
    @FXML private Button cancelarButton;

    // ── FXML: coordenadas manuales y 3D ───────────────────────────────────────
    @FXML private TextField  coordXField;
    @FXML private TextField  coordYField;
    @FXML private TextField  coordZField;
    @FXML private HBox       coordZBox;
    @FXML private CheckBox   modo3DCheckBox;
    @FXML private Label      modo3DInfoLabel;

    // ── FXML: resultados y detalles ───────────────────────────────────────────
    @FXML private Label    resultadoFuerzaLabel;
    @FXML private Label    resultadoCampoLabel;
    @FXML private TextArea calculosDetalladosTextArea;

    // ── FXML: animación ───────────────────────────────────────────────────────
    @FXML private Tab    animacionTab;
    @FXML private Label  pasoIndicadorLabel;
    @FXML private Label  pasoDescripcionLabel;
    @FXML private Label  barraProgresoLabel;
    @FXML private Button btnAnteriorPaso;
    @FXML private Button btnSiguientePaso;
    @FXML private Button btnReiniciarAnimacion;
    @FXML private Button btnDetenerAnimacion;

    // ── Estado interno ────────────────────────────────────────────────────────
    private Grafo           grafo;
    private UnidadDistancia unidadActual = UnidadDistancia.METROS;
    private boolean         modo3D       = false;

    private Map<Nodo, Circle>      nodoCirculos;
    private ObservableList<String> nombresParticulas;
    private ToggleGroup            modoVisualizacionGroup;

    private volatile boolean estaCalculando  = false;
    private volatile int     calculoVersion  = 0;   // se incrementa al cancelar
    private Thread           hiloCalculo     = null;

    /** Último resultado calculado — usado por Detalles, PDF y Animación. */
    private ResultadoCalculo ultimoResultado = null;

    /** Controlador de animación paso a paso. */
    private final AnimacionCoulombController animController = new AnimacionCoulombController();

    private static final Color[] COLORES_FLECHAS = {
        Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE,
        Color.CYAN, Color.MAGENTA, Color.BROWN, Color.PINK,
        Color.DARKGREEN, Color.DARKBLUE, Color.DARKORANGE, Color.DARKVIOLET
    };

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================
    public void initialize() {
        grafo             = new Grafo();
        nodoCirculos      = new HashMap<>();
        nombresParticulas = FXCollections.observableArrayList();

        origenRutaComboBox.setItems(nombresParticulas);
        destinoRutaComboBox.setItems(nombresParticulas);
        particulaEliminarComboBox.setItems(nombresParticulas);
        particulaOrigenComboBox.setItems(nombresParticulas);
        eliminarRutaComboBox.setItems(FXCollections.observableArrayList());

        modoVisualizacionGroup = new ToggleGroup();
        fuerzaTotalRadio.setToggleGroup(modoVisualizacionGroup);
        fuerzasIndividualesRadio.setToggleGroup(modoVisualizacionGroup);
        fuerzaTotalRadio.setSelected(true);
        modoVisualizacionGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            limpiarFlechas();
            if (estaCalculando) javafx.application.Platform.runLater(() -> ejecutarCalculo(calculoVersion));
        });

        ObservableList<UnidadDistancia> unidades = FXCollections.observableArrayList(
            UnidadDistancia.KILOMETROS, UnidadDistancia.METROS,
            UnidadDistancia.CENTIMETROS, UnidadDistancia.MILIMETROS,
            UnidadDistancia.MICROMETROS, UnidadDistancia.NANOMETROS,
            UnidadDistancia.PICOMETROS
        );
        unidadDistanciaComboBox.setItems(unidades);
        unidadDistanciaComboBox.setValue(UnidadDistancia.METROS);
        unidadDistanciaComboBox.setOnAction(e -> cambiarUnidad(unidadDistanciaComboBox.getValue()));

        actualizarComboBoxRutas();

        canvasPlano.widthProperty().addListener((obs, o, n) -> dibujarCuadrante());
        canvasPlano.heightProperty().addListener((obs, o, n) -> dibujarCuadrante());
        canvasPlano.setWidth(2100);
        canvasPlano.setHeight(1300);
        dibujarCuadrante();

        if (calculosDetalladosTextArea != null) {
            calculosDetalladosTextArea.setEditable(false);
            calculosDetalladosTextArea.setWrapText(true);
            calculosDetalladosTextArea.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        }
    }

    // =========================================================================
    // TOGGLE 3D
    // =========================================================================
    @FXML
    private void toggleModo3D() {
        modo3D = modo3DCheckBox.isSelected();
        coordZBox.setVisible(modo3D);
        coordZBox.setManaged(modo3D);
        modo3DInfoLabel.setVisible(modo3D);
        modo3DInfoLabel.setManaged(modo3D);

        if (estaCalculando) cancelarCalculo();
        limpiarFlechas();
        resultadoFuerzaLabel.setText(" ");
        resultadoCampoLabel.setText(" ");
        ultimoResultado = null;

        if (modo3D) {
            canvasPlano.setWidth(1500);
            canvasPlano.setHeight(1300);
            dibujarCuadrante3D();
        } else {
            canvasPlano.setWidth(2100);
            canvasPlano.setHeight(1300);
            dibujarCuadrante();
        }
        reposicionarParticulas();
    }

    private void reposicionarParticulas() {
        CoordenadasTransformador t = new CoordenadasTransformador(canvasPlano.getHeight(), unidadActual);
        for (Map.Entry<Nodo, Circle> entry : nodoCirculos.entrySet()) {
            Nodo nodo = entry.getKey();
            Circle c = entry.getValue();
            double px, py;
            if (modo3D) {
                px = t.isoXToPx(t.pxXToUnidad(nodo.getX()), t.pxYToUnidad(nodo.getY()), nodo.getZ());
                py = t.isoYToPx(t.pxXToUnidad(nodo.getX()), t.pxYToUnidad(nodo.getY()), nodo.getZ());
            } else {
                px = nodo.getX();
                py = nodo.getY();
            }
            c.setCenterX(px);
            c.setCenterY(py);
            grafoPane.getChildren().stream()
                .filter(n -> n instanceof Text && ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                .findFirst()
                .ifPresent(n -> { ((Text) n).setX(px - 4); ((Text) n).setY(py + 4); });
        }
        actualizarAristas();
    }

    // =========================================================================
    // PARTÍCULAS
    // =========================================================================
    @FXML
    private void agregarParticula() {
        String nombre        = nombreParticulaField.getText().trim();
        String valorCargaStr = valorCargaField.getText().trim();
        String tipoCarga     = tipoCargaField.getText().trim();

        if (nombre.isEmpty())        { mostrarAlerta("Error", "Ingrese el nombre de la partícula."); return; }
        if (valorCargaStr.isEmpty()) { mostrarAlerta("Error", "Ingrese el valor de la carga.");      return; }

        double valorCarga;
        try {
            valorCarga = Double.parseDouble(valorCargaStr);
            if (valorCarga <= 0) { mostrarAlerta("Error", "La carga debe ser positiva."); return; }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La carga debe ser un número válido."); return;
        }

        if (!tipoCarga.equals("+") && !tipoCarga.equals("-")) {
            mostrarAlerta("Error", "El tipo de carga debe ser + o -"); return;
        }
        for (Nodo n : grafo.getNodos()) {
            if (n.getNombre().equalsIgnoreCase(nombre)) {
                mostrarAlerta("Error", "Ya existe una partícula con ese nombre."); return;
            }
        }

        // ── Coordenadas: manuales o aleatorias ──
        String cxStr = coordXField.getText().trim();
        String cyStr = coordYField.getText().trim();
        String czStr = (coordZField != null) ? coordZField.getText().trim() : "";
        boolean coordsManuales = !cxStr.isEmpty() && !cyStr.isEmpty();

        double x, y;
        double zLogica = 0;

        if (coordsManuales) {
            try {
                double ux = Double.parseDouble(cxStr);
                double uy = Double.parseDouble(cyStr);
                // Convertir unidades del plano a px (100 px = 1 unidad)
                x = 40 + ux * 100;  // MARGIN + unidad * PX_POR_UNIT
                y = (canvasPlano.getHeight() - 40) - uy * 100;
                if (modo3D && !czStr.isEmpty()) {
                    zLogica = Double.parseDouble(czStr);
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "Las coordenadas deben ser números válidos."); return;
            }
        } else {
            Random random = new Random();
            double viewW   = scrollPane.getViewportBounds().getWidth();
            double viewH   = scrollPane.getViewportBounds().getHeight();
            double scrollX = scrollPane.getHvalue() * (canvasPlano.getWidth()  - viewW);
            double scrollY = scrollPane.getVvalue() * (canvasPlano.getHeight() - viewH);
            x = 0; y = 0;
            int intentos = 0;
            boolean sup;
            do {
                x = scrollX + 50 + random.nextDouble() * (viewW - 100);
                y = scrollY + 50 + random.nextDouble() * (viewH - 100);
                x = Math.max(50, Math.min(x, canvasPlano.getWidth()  - 50));
                y = Math.max(50, Math.min(y, canvasPlano.getHeight() - 50));
                sup = verificarSuperposicion(x, y);
                intentos++;
            } while (sup && intentos < 100);
            if (intentos >= 100) { mostrarAlerta("Error", "No se pudo ubicar la partícula."); return; }
        }

        Nodo nueva = new Nodo(nombre, x, y, zLogica, valorCarga, tipoCarga);
        grafo.agregarNodo(nueva);
        nombresParticulas.add(nombre);

        // En modo 3D, proyectar a isométrico para la visual
        double visPx = x, visPy = y;
        if (modo3D) {
            CoordenadasTransformador t = new CoordenadasTransformador(canvasPlano.getHeight(), unidadActual);
            double ux = t.pxXToUnidad(x);
            double uy = t.pxYToUnidad(y);
            visPx = t.isoXToPx(ux, uy, zLogica);
            visPy = t.isoYToPx(ux, uy, zLogica);
        }

        Color color = tipoCarga.equals("+") ? Color.LIGHTCORAL : Color.LIGHTBLUE;
        Circle circulo = new Circle(visPx, visPy, 15, color);
        circulo.setStroke(Color.BLACK);
        hacerNodoArrastrable(circulo, nueva);

        String etiqueta = modo3D
            ? nombre + " (" + valorCarga + ") (" + tipoCarga + ") z=" + String.format("%.1f", zLogica)
            : nombre + " (" + valorCarga + ") (" + tipoCarga + ")";
        Text texto = new Text(etiqueta);
        texto.setX(visPx - 4);
        texto.setY(visPy + 4);

        nodoCirculos.put(nueva, circulo);
        grafoPane.getChildren().addAll(circulo, texto);
        nombreParticulaField.clear();
        valorCargaField.clear();
        tipoCargaField.clear();
        coordXField.clear();
        coordYField.clear();
        if (coordZField != null) coordZField.clear();
        actualizarComboBoxRutas();
    }

    @FXML
    private void eliminarParticula() {
        String nombre = particulaEliminarComboBox.getValue();
        if (nombre == null) { mostrarAlerta("Error", "Seleccione una partícula."); return; }

        Nodo nodo = buscarNodoPorNombre(nombre);
        if (nodo == null) { mostrarAlerta("Error", "La partícula no existe."); return; }

        if (estaCalculando && nombre.equals(particulaOrigenComboBox.getValue())) {
            cancelarCalculo();
            resultadoFuerzaLabel.setText(" ");
            resultadoCampoLabel.setText(" ");
            limpiarFlechas();
        }

        grafo.eliminarAristasDeNodo(nodo);
        grafo.eliminarNodo(nodo);

        Circle c = nodoCirculos.remove(nodo);
        if (c != null) grafoPane.getChildren().remove(c);

        grafoPane.getChildren().removeIf(n ->
            n instanceof Text && ((Text) n).getText().startsWith(nodo.getNombre() + " "));

        actualizarAristas();
        nombresParticulas.remove(nombre);
        particulaEliminarComboBox.getSelectionModel().clearSelection();
        if (nombre.equals(particulaOrigenComboBox.getValue()))
            particulaOrigenComboBox.getSelectionModel().clearSelection();
        actualizarComboBoxRutas();
    }

    @FXML
    private void editarParticula() {
        String nombreActual = particulaEditarComboBox.getValue();
        String nuevoNombre  = particulaEditarField.getText().trim();
        if (nombreActual == null) { mostrarAlerta("Error", "Seleccione una partícula."); return; }
        if (nuevoNombre.isEmpty()) { mostrarAlerta("Error", "Ingrese el nuevo nombre."); return; }
        for (Nodo n : grafo.getNodos()) {
            if (n.getNombre().equals(nuevoNombre)) {
                mostrarAlerta("Error", "Ya existe una partícula con ese nombre."); return;
            }
        }
        Nodo nodo = buscarNodoPorNombre(nombreActual);
        if (nodo == null) { mostrarAlerta("Error", "La partícula no existe."); return; }

        nodo.setNombre(nuevoNombre);
        grafoPane.getChildren().stream()
            .filter(n -> n instanceof Text && ((Text) n).getText().startsWith(nombreActual + " "))
            .findFirst()
            .ifPresent(n -> ((Text) n).setText(nuevoNombre));

        nombresParticulas.remove(nombreActual);
        nombresParticulas.add(nuevoNombre);
        particulaEditarComboBox.getSelectionModel().clearSelection();
        particulaEditarField.clear();
        actualizarComboBoxRutas();
    }

    // =========================================================================
    // RUTAS
    // =========================================================================
    @FXML
    private void agregarRuta() {
        String oNombre = origenRutaComboBox.getValue();
        String dNombre = destinoRutaComboBox.getValue();
        if (oNombre == null || dNombre == null) { mostrarAlerta("Error", "Complete origen y destino."); return; }
        if (oNombre.equals(dNombre))            { mostrarAlerta("Error", "Origen y destino deben ser distintos."); return; }

        Nodo origen  = buscarNodoPorNombre(oNombre);
        Nodo destino = buscarNodoPorNombre(dNombre);
        if (origen == null || destino == null)  { mostrarAlerta("Error", "Las partículas deben existir."); return; }

        double peso = Math.hypot(origen.getX() - destino.getX(),
                                 origen.getY() - destino.getY()) / 100.0;

        Arista existente = null, inversa = null;
        for (Arista a : grafo.getAristas()) {
            if (a.getOrigen().equals(origen)  && a.getDestino().equals(destino)) existente = a;
            if (a.getOrigen().equals(destino) && a.getDestino().equals(origen))  inversa   = a;
        }
        if      (existente != null) existente.setPeso(peso);
        else if (inversa   != null) inversa.setPeso(peso);
        else grafo.agregarArista(new Arista(origen, destino, peso));

        actualizarAristas();
        origenRutaComboBox.getSelectionModel().clearSelection();
        destinoRutaComboBox.getSelectionModel().clearSelection();
        actualizarComboBoxRutas();
    }

    @FXML
    private void eliminarRuta() {
        String ruta = eliminarRutaComboBox.getValue();
        if (ruta == null) { mostrarAlerta("Error", "Seleccione una ruta."); return; }

        String[] partes = ruta.split(" - ");
        String oNombre  = partes[0];
        String dNombre  = partes[1].split(" \\(")[0];

        Nodo origen  = buscarNodoPorNombre(oNombre);
        Nodo destino = buscarNodoPorNombre(dNombre);
        if (origen == null || destino == null) { mostrarAlerta("Error", "Partículas no encontradas."); return; }

        List<Arista> aEliminar = new ArrayList<>();
        for (Arista a : grafo.getAristas()) {
            if (a.esIgual(new Arista(origen, destino, 0)) ||
                a.esIgual(new Arista(destino, origen, 0)))
                aEliminar.add(a);
        }
        if (aEliminar.isEmpty()) { mostrarAlerta("Error", "La ruta no existe."); return; }
        aEliminar.forEach(grafo::eliminarArista);
        actualizarAristas();
        actualizarComboBoxRutas();
    }

    // =========================================================================
    // CÁLCULO — delega en CoulombCalculator
    // =========================================================================
    @FXML
    private void iniciarCalculo() {
        String nombre = particulaOrigenComboBox.getValue();
        if (nombre == null)                     { mostrarAlerta("Error", "Seleccione una partícula."); return; }
        if (buscarNodoPorNombre(nombre) == null) { mostrarAlerta("Error", "La partícula no existe."); return; }

        estaCalculando = true;
        calcularButton.setDisable(true);
        cancelarButton.setDisable(false);

        hiloCalculo = new Thread(() -> {
            int miVersion = calculoVersion;
            while (estaCalculando && calculoVersion == miVersion) {
                try {
                    final int v = miVersion;
                    javafx.application.Platform.runLater(() -> ejecutarCalculo(v));
                    Thread.sleep(16); 
                } catch (InterruptedException e) { break; }
            }
        });
        hiloCalculo.setDaemon(true);
        hiloCalculo.start();
    }

    @FXML
    private void cancelarCalculo() {
        // 1. Señalizar parada y subir versión para invalidar runLater pendientes
        estaCalculando = false;
        calculoVersion++;

        // 2. Interrumpir y esperar que el hilo termine completamente
        if (hiloCalculo != null) {
            hiloCalculo.interrupt();
            try { hiloCalculo.join(500); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            hiloCalculo = null;
        }

        // 3. Ahora que no hay más tareas encoladas en camino, limpiar el mapa
        //    Se usa runLater para garantizar que se ejecuta DESPUÉS de cualquier
        //    runLater que ya estuviera en la cola con versión anterior
        javafx.application.Platform.runLater(() -> {
            limpiarFlechas();
            resultadoFuerzaLabel.setText(" ");
            resultadoCampoLabel.setText(" ");
            calcularButton.setDisable(false);
            cancelarButton.setDisable(true);
        });
    }

    private void ejecutarCalculo(int version) {
        // Si esta tarea fue encolada antes de un cancelar, descartarla
        if (version != calculoVersion) return;

        String nombre = particulaOrigenComboBox.getValue();
        if (nombre == null) return;

        Nodo origen = buscarNodoPorNombre(nombre);
        if (origen == null) {
            cancelarCalculo();
            resultadoFuerzaLabel.setText(" ");
            resultadoCampoLabel.setText(" ");
            particulaOrigenComboBox.getSelectionModel().clearSelection();
            return;
        }

        CoordenadasTransformador transformador =
            new CoordenadasTransformador(canvasPlano.getHeight(), unidadActual);
        CoulombCalculator calculator = new CoulombCalculator(grafo, transformador, modo3D);

        ultimoResultado = calculator.calcular(origen);

        // Verificar de nuevo por si cancelaron mientras calculábamos
        if (version != calculoVersion) return;

        if (ultimoResultado == null) {
            resultadoFuerzaLabel.setText("Sin conexiones");
            resultadoCampoLabel.setText(" ");
            limpiarFlechas();
            return;
        }

        if (fuerzasIndividualesRadio.isSelected()) {
            dibujarFlechasIndividuales(origen, ultimoResultado.getFuerzasIndividuales());
        } else {
            dibujarFlechaFuerza(origen,
                ultimoResultado.getFuerzaTotalX(),
                ultimoResultado.getFuerzaTotalY());
        }

        resultadoFuerzaLabel.setText(formatearNumero(ultimoResultado.getFuerzaTotal()) + " N");
        resultadoCampoLabel.setText(formatearNumero(ultimoResultado.getCampoElectrico()) + " N/C");
    }

    @FXML private void calcularCampo()   { ejecutarCalculo(calculoVersion); }
    @FXML private void calcularCampoUI() { ejecutarCalculo(calculoVersion); }

    // =========================================================================
    // DETALLES Y PDF
    // =========================================================================
    @FXML
    private void generarCalculosDetallados() {
        if (ultimoResultado == null) {
            mostrarAlerta("Error", "Primero seleccione una partícula y presione 'Calcular'.");
            return;
        }
        mostrarTextoDetallado(ultimoResultado);
    }

    private void mostrarTextoDetallado(ResultadoCalculo res) {
        if (calculosDetalladosTextArea == null) return;
        String simb = unidadActual.getSimbolo();
        boolean is3D = res.isEs3D();
        StringBuilder sb = new StringBuilder();

        sb.append("================================================================\n");
        sb.append("   CALCULO DE FUERZA ELECTRICA Y CAMPO ELECTRICO\n");
        sb.append("   Ley de Coulomb  |  Sistema USTA");
        if (is3D) sb.append("  |  MODO 3D");
        sb.append("\n================================================================\n\n");

        sb.append("DATOS INICIALES\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  Particula de analisis : %s\n", res.getParticulaOrigen().getNombre()));
        if (is3D) {
            sb.append(String.format("  Posicion              : (%.4f %s, %.4f %s, %.4f %s)\n",
                res.getX0(), simb, res.getY0(), simb, res.getZ0(), simb));
        } else {
            sb.append(String.format("  Posicion              : (%.4f %s, %.4f %s)\n",
                res.getX0(), simb, res.getY0(), simb));
        }
        sb.append(String.format("  Carga  q0             : %.2f uC = %.4e C  (%s)\n",
            res.getParticulaOrigen().getValorCarga(),
            res.getParticulaOrigen().getValorCarga() * 1e-6,
            res.getParticulaOrigen().getTipoCarga()));
        sb.append("  Constante  k          : 8.99 x 10^9 N*m^2/C^2\n");
        sb.append(String.format("  Unidad de distancia   : %s (%s)\n", unidadActual.name(), simb));
        if (is3D) sb.append("  Modo de calculo       : TRIDIMENSIONAL (X, Y, Z)\n");
        sb.append("\n");

        sb.append("CALCULO DE FUERZAS INDIVIDUALES\n");
        sb.append("----------------------------------------------------------------\n");

        int n = 1;
        for (ResultadoFuerza rf : res.getFuerzasIndividuales()) {
            sb.append(String.format("\n%d) Fuerza ejercida por %s sobre %s\n",
                n++, rf.getParticulaCausante().getNombre(), res.getParticulaOrigen().getNombre()));
            if (is3D) {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s, %.4f %s)\n",
                    rf.getParticulaCausante().getNombre(), rf.getX1(), simb, rf.getY1(), simb, rf.getZ1(), simb));
            } else {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s)\n",
                    rf.getParticulaCausante().getNombre(), rf.getX1(), simb, rf.getY1(), simb));
            }
            sb.append(String.format("   Carga  q1  : %.2f uC = %.4e C  (%s)\n",
                rf.getParticulaCausante().getValorCarga(),
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getTipoCarga()));
            sb.append(String.format("   Tipo       : %s\n", rf.getTipoInteraccion()));

            sb.append("\n   Calculo de distancia:\n");
            sb.append(String.format("     Dx = x1 - x0 = %.4f - %.4f = %.4f %s\n", rf.getX1(), rf.getX0(), rf.getDx(), simb));
            sb.append(String.format("     Dy = y1 - y0 = %.4f - %.4f = %.4f %s\n", rf.getY1(), rf.getY0(), rf.getDy(), simb));
            if (is3D) {
                sb.append(String.format("     Dz = z1 - z0 = %.4f - %.4f = %.4f %s\n", rf.getZ1(), rf.getZ0(), rf.getDz(), simb));
                sb.append("     r  = sqrt(Dx^2 + Dy^2 + Dz^2)\n");
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2 + (%.4f)^2)\n", rf.getDx(), rf.getDy(), rf.getDz()));
            } else {
                sb.append("     r  = sqrt(Dx^2 + Dy^2)\n");
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2)\n", rf.getDx(), rf.getDy()));
            }
            sb.append(String.format("     r  = %.4f %s\n", rf.getDistanciaEnUnidad(), simb));
            sb.append(String.format("     r  = %.4e m\n", rf.getDistanciaEnMetros()));

            sb.append("\n   Angulo (eje X+, sentido antihorario):\n");
            sb.append("     theta = arctan(Dy / Dx)\n");
            sb.append(String.format("     theta = arctan(%.4f / %.4f) = %.2f grados\n",
                rf.getDy(), rf.getDx(), rf.getAnguloDeg()));
            if (is3D) {
                sb.append(String.format("     phi (elevacion) = %.2f grados\n", rf.getAnguloElevacionDeg()));
            }

            sb.append("\n   Ley de Coulomb:\n");
            sb.append("     F = k * |q0 * q1| / r^2\n");
            sb.append(String.format("     F = (8.99x10^9) * |%.4e * %.4e| / (%.4e)^2\n",
                res.getParticulaOrigen().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getDistanciaEnMetros()));
            sb.append(String.format("     F = %.6e N\n", rf.getMagnitud()));

            sb.append("\n   Componentes (con signo segun interaccion):\n");
            sb.append(String.format("     Fx = %.6e N\n", rf.getFx()));
            sb.append(String.format("     Fy = %.6e N\n", rf.getFy()));
            if (is3D) sb.append(String.format("     Fz = %.6e N\n", rf.getFz()));
            sb.append(rf.isEsRepulsion()
                ? "     (Repulsion: direccion contraria al vector r)\n"
                : "     (Atraccion: direccion igual al vector r)\n");
        }

        sb.append("\n\n================================================================\n");
        sb.append("SUMA VECTORIAL DE FUERZAS\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  SFx = %.6e N\n", res.getFuerzaTotalX()));
        sb.append(String.format("  SFy = %.6e N\n", res.getFuerzaTotalY()));
        if (is3D) sb.append(String.format("  SFz = %.6e N\n", res.getFuerzaTotalZ()));
        sb.append("\n  Fuerza resultante:\n");
        if (is3D) {
            sb.append("    |F| = sqrt(SFx^2 + SFy^2 + SFz^2)\n");
            sb.append(String.format("    |F| = sqrt((%.6e)^2 + (%.6e)^2 + (%.6e)^2)\n",
                res.getFuerzaTotalX(), res.getFuerzaTotalY(), res.getFuerzaTotalZ()));
        } else {
            sb.append("    |F| = sqrt(SFx^2 + SFy^2)\n");
            sb.append(String.format("    |F| = sqrt((%.6e)^2 + (%.6e)^2)\n",
                res.getFuerzaTotalX(), res.getFuerzaTotalY()));
        }
        sb.append(String.format("    |F| = %.6e N\n", res.getFuerzaTotal()));
        sb.append(String.format("\n  Angulo resultante (theta) : %.2f grados\n", res.getAnguloResultante()));
        if (is3D) sb.append(String.format("  Angulo elevacion (phi)    : %.2f grados\n", res.getAnguloElevacionResultante()));

        sb.append("\n\n================================================================\n");
        sb.append("CAMPO ELECTRICO\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append("  E = F / |q0|\n");
        sb.append(String.format("  E = %.6e / %.4e\n",
            res.getFuerzaTotal(),
            Math.abs(res.getParticulaOrigen().getValorCarga() * 1e-6)));
        sb.append(String.format("  E = %.6e N/C\n", res.getCampoElectrico()));
        sb.append("\n================================================================\n");
        sb.append("                      FIN DEL CALCULO\n");
        sb.append("================================================================\n");

        calculosDetalladosTextArea.setText(sb.toString());
    }

    @FXML
    private void generarPDF() {
        if (calculosDetalladosTextArea == null ||
            calculosDetalladosTextArea.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "Genere los cálculos detallados primero."); return;
        }
        if (ultimoResultado == null) {
            mostrarAlerta("Error", "Realice un cálculo antes de generar el PDF."); return;
        }

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar PDF");
        fc.setInitialFileName("Calculos_Electricos.pdf");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        File dir = new File(System.getProperty("user.home") + File.separator + "Desktop");
        if (!dir.exists()) dir = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(dir);

        javafx.stage.Stage stage = (javafx.stage.Stage) grafoPane.getScene().getWindow();
        File destino = fc.showSaveDialog(stage);
        if (destino == null) return;

        try {
            String ruta = destino.getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";
            String fecha = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            new PdfGenerator().generar(ruta, ultimoResultado, unidadActual, fecha);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("PDF Generado");
            ok.setHeaderText("Éxito");
            ok.setContentText("Guardado en:\n" + ruta);
            ok.showAndWait();

        } catch (Exception e) {
            mostrarAlerta("Error", "Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // CANVAS – sistema matemático (Y+ hacia arriba)
    // =========================================================================
    private void dibujarCuadrante() {
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

    // =========================================================================
    // CANVAS 3D – proyección isométrica
    // =========================================================================
    private void dibujarCuadrante3D() {
        if (canvasPlano == null) return;
        GraphicsContext gc = canvasPlano.getGraphicsContext2D();
        double width  = canvasPlano.getWidth();
        double height = canvasPlano.getHeight();

        gc.setFill(Color.web("#f5f5f5"));
        gc.fillRect(0, 0, width, height);

        CoordenadasTransformador t = new CoordenadasTransformador(height, unidadActual);
        int MAX = 10;

        // Grid en el plano XY (z=0)
        gc.setStroke(Color.web("#e0e0e0"));
        gc.setLineWidth(0.5);
        for (int i = 0; i <= MAX; i++) {
            gc.strokeLine(t.isoXToPx(i,0,0), t.isoYToPx(i,0,0), t.isoXToPx(i,MAX,0), t.isoYToPx(i,MAX,0));
            gc.strokeLine(t.isoXToPx(0,i,0), t.isoYToPx(0,i,0), t.isoXToPx(MAX,i,0), t.isoYToPx(MAX,i,0));
        }

        // Grid en el plano XZ (y=0)
        gc.setStroke(Color.web("#e8e8e8"));
        for (int i = 0; i <= MAX; i++) {
            gc.strokeLine(t.isoXToPx(i,0,0), t.isoYToPx(i,0,0), t.isoXToPx(i,0,MAX), t.isoYToPx(i,0,MAX));
            gc.strokeLine(t.isoXToPx(0,0,i), t.isoYToPx(0,0,i), t.isoXToPx(MAX,0,i), t.isoYToPx(MAX,0,i));
        }

        // Grid en el plano YZ (x=0)
        gc.setStroke(Color.web("#e8e8e8"));
        for (int i = 0; i <= MAX; i++) {
            gc.strokeLine(t.isoXToPx(0,i,0), t.isoYToPx(0,i,0), t.isoXToPx(0,i,MAX), t.isoYToPx(0,i,MAX));
            gc.strokeLine(t.isoXToPx(0,0,i), t.isoYToPx(0,0,i), t.isoXToPx(0,MAX,i), t.isoYToPx(0,MAX,i));
        }

        // Ejes principales (más gruesos)
        gc.setLineWidth(2.5);
        // Eje X (rojo)
        gc.setStroke(Color.web("#e53935"));
        gc.strokeLine(t.isoXToPx(0,0,0), t.isoYToPx(0,0,0), t.isoXToPx(MAX,0,0), t.isoYToPx(MAX,0,0));
        // Eje Y (verde)
        gc.setStroke(Color.web("#43a047"));
        gc.strokeLine(t.isoXToPx(0,0,0), t.isoYToPx(0,0,0), t.isoXToPx(0,MAX,0), t.isoYToPx(0,MAX,0));
        // Eje Z (azul)
        gc.setStroke(Color.web("#1e88e5"));
        gc.strokeLine(t.isoXToPx(0,0,0), t.isoYToPx(0,0,0), t.isoXToPx(0,0,MAX), t.isoYToPx(0,0,MAX));

        // Etiquetas de ejes
        gc.setFont(Font.font(12));
        String simb = unidadActual.getSimbolo();
        gc.setFill(Color.web("#e53935"));
        gc.fillText("X (" + simb + ")", t.isoXToPx(MAX+0.3,0,0), t.isoYToPx(MAX+0.3,0,0));
        gc.setFill(Color.web("#43a047"));
        gc.fillText("Y (" + simb + ")", t.isoXToPx(0,MAX+0.3,0), t.isoYToPx(0,MAX+0.3,0));
        gc.setFill(Color.web("#1e88e5"));
        gc.fillText("Z (" + simb + ")", t.isoXToPx(0,0,MAX+0.3), t.isoYToPx(0,0,MAX+0.3));

        // Números en los ejes
        gc.setFont(Font.font(9));
        gc.setFill(Color.web("#666"));
        for (int i = 1; i <= MAX; i++) {
            gc.fillText(String.valueOf(i), t.isoXToPx(i,0,0)-4, t.isoYToPx(i,0,0)+14);
            gc.fillText(String.valueOf(i), t.isoXToPx(0,i,0)-18, t.isoYToPx(0,i,0)+4);
            gc.fillText(String.valueOf(i), t.isoXToPx(0,0,i)+6, t.isoYToPx(0,0,i)+4);
        }

        // Badge "3D" en esquina
        gc.setFont(Font.font(14));
        gc.setFill(Color.web("#004d40"));
        gc.fillText("⬡ MODO 3D  |  10×10×10 " + simb, 20, 25);
    }

    // =========================================================================
    // GRÁFICOS: flechas, aristas
    // =========================================================================
    private void dibujarFlechaFuerza(Nodo origen, double fuerzaX, double fuerzaY) {
        limpiarFlechas();
        double mag = Math.hypot(fuerzaX, fuerzaY);
        if (mag == 0) return;
        dibujarFlecha(origen.getX(), origen.getY(),
            fuerzaX / mag, -fuerzaY / mag,
            Color.RED, 3, 18, "flechaFuerza", null);
    }

    private void dibujarFlechasIndividuales(Nodo origen, List<ResultadoFuerza> fuerzas) {
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

    private void limpiarFlechas() {
        grafoPane.getChildren().removeIf(n -> n.getUserData() != null &&
            (n.getUserData().equals("flechaFuerza") ||
             n.getUserData().equals("flechaIndividual")));
    }

    private void actualizarAristas() {
        // Solo borra nodos etiquetados como "arista", nunca toca las flechas de fuerza
        grafoPane.getChildren().removeIf(n ->
            "arista".equals(n.getUserData()));

        for (Arista a : grafo.getAristas()) {
            Nodo o = a.getOrigen(), d = a.getDestino();
            double[] pi = puntoEnCirc(o.getX(), o.getY(), d.getX(), d.getY(), 15);
            double[] pf = puntoEnCirc(d.getX(), d.getY(), o.getX(), o.getY(), 15);

            Line linea = new Line(pi[0], pi[1], pf[0], pf[1]);
            linea.setStrokeWidth(2); linea.setStroke(Color.GRAY);
            linea.setUserData("arista"); // etiquetar para poder borrar selectivamente

            double distUnidad = Math.hypot(o.getX() - d.getX(), o.getY() - d.getY()) / 100.0;
            a.setPeso(distUnidad);

            Text peso = new Text(String.format("%.2f %s", distUnidad, unidadActual.getSimbolo()));
            peso.setX((pi[0] + pf[0]) / 2);
            peso.setY((pi[1] + pf[1]) / 2);
            peso.setUserData("arista");

            grafoPane.getChildren().addAll(linea, peso);
        }
    }

    private void actualizarComboBoxRutas() {
        ObservableList<String> rutas = FXCollections.observableArrayList();
        for (Arista a : grafo.getAristas()) {
            String r = a.getOrigen().getNombre() + " - " + a.getDestino().getNombre()
                + " (" + String.format("%.2f %s", a.getPeso(), unidadActual.getSimbolo()) + ")";
            if (!rutas.contains(r)) rutas.add(r);
        }
        eliminarRutaComboBox.setItems(rutas);
    }

    private void cambiarUnidad(UnidadDistancia nueva) {
        unidadActual = nueva;
        actualizarAristas();
        dibujarCuadrante();
    }

    // =========================================================================
    // ANIMACIÓN PASO A PASO
    // =========================================================================

    /**
     * Se dispara al seleccionar o deseleccionar la pestaña Animación.
     * Al entrar: inicializa la animación con los círculos reales del grafoPane.
     * Al salir:  restaura los círculos a su estado original.
     */
    @FXML
    private void onAnimacionTabSeleccionada(javafx.event.Event e) {
        boolean activa = animacionTab.isSelected();

        if (activa) {
            // Detener cálculo en curso y limpiar flechas al entrar a animación
            if (estaCalculando) {
                cancelarCalculo();
            }
            limpiarFlechas();
            resultadoFuerzaLabel.setText(" ");
            resultadoCampoLabel.setText(" ");

            if (ultimoResultado == null) {
                pasoIndicadorLabel.setText("Sin cálculo activo.\nRealice un cálculo primero.");
                pasoDescripcionLabel.setText("");
                barraProgresoLabel.setText("──────────");
                btnAnteriorPaso.setDisable(true);
                btnSiguientePaso.setDisable(true);
                btnReiniciarAnimacion.setDisable(true);
                btnDetenerAnimacion.setDisable(true);
                return;
            }

            animController.inicializar(ultimoResultado, unidadActual,
                grafoPane, nodoCirculos);

            btnAnteriorPaso.setDisable(true);
            btnSiguientePaso.setDisable(animController.getTotalPasos() <= 1);
            btnReiniciarAnimacion.setDisable(false);
            btnDetenerAnimacion.setDisable(false);
            actualizarInfoPaso();

        } else {
            // Al cambiar a otra pestaña, restaurar los círculos
            animController.restaurar();
        }
    }

    @FXML
    private void siguientePaso() {
        if (!animController.hayResultado()) return;
        animController.siguiente();
        actualizarBotonesAnimacion();
        actualizarInfoPaso();
    }

    @FXML
    private void anteriorPaso() {
        if (!animController.hayResultado()) return;
        animController.anterior();
        actualizarBotonesAnimacion();
        actualizarInfoPaso();
    }

    @FXML
    private void reiniciarAnimacion() {
        if (ultimoResultado == null) return;
        animController.inicializar(ultimoResultado, unidadActual,
            grafoPane, nodoCirculos);
        btnAnteriorPaso.setDisable(true);
        btnSiguientePaso.setDisable(animController.getTotalPasos() <= 1);
        btnDetenerAnimacion.setDisable(false);
        actualizarInfoPaso();
    }

    /**
     * Detiene la animación: restaura los círculos, limpia todos los nodos
     * de animación del mapa y resetea el estado del panel.
     */
    @FXML
    private void detenerAnimacion() {
        animController.restaurar();
        pasoIndicadorLabel.setText("Animación detenida.");
        pasoDescripcionLabel.setText("");
        barraProgresoLabel.setText("──────────");
        btnAnteriorPaso.setDisable(true);
        btnSiguientePaso.setDisable(true);
        btnReiniciarAnimacion.setDisable(true);
        btnDetenerAnimacion.setDisable(true);
        AnimacionCoulombController controller = new AnimacionCoulombController();
        controller.restaurar();
        controller.restaurarCausantesANeutro();
    }

    private void actualizarBotonesAnimacion() {
        btnAnteriorPaso.setDisable(animController.getPasoActual() == 0);
        btnSiguientePaso.setDisable(
            animController.getPasoActual() >= animController.getTotalPasos() - 1);
    }

    /** Actualiza el indicador de paso, descripción y barra de progreso. */
    private void actualizarInfoPaso() {
        int actual = animController.getPasoActual() + 1;
        int total  = animController.getTotalPasos();
        int n      = ultimoResultado.getFuerzasIndividuales().size();

        pasoIndicadorLabel.setText("Paso " + actual + " de " + total);
        pasoDescripcionLabel.setText(
            obtenerDescripcionPaso(animController.getPasoActual(), n));

        // Barra de progreso ASCII de 10 bloques
        int llenos = (int) Math.round(((double) actual / total) * 10);
        StringBuilder barra = new StringBuilder();
        for (int i = 0; i < 10; i++) barra.append(i < llenos ? "▓" : "░");
        barraProgresoLabel.setText(barra.toString());
    }

    private String obtenerDescripcionPaso(int paso, int n) {
        if (paso == 0)
            return "Partícula de análisis\ndestacada.\nObserve su posición y carga.";

        if (paso >= 1 && paso <= n * 3) {
            int idx    = (paso - 1) / 3;
            int subPas = (paso - 1) % 3;
            String causante = ultimoResultado.getFuerzasIndividuales()
                .get(idx).getParticulaCausante().getNombre();
            return switch (subPas) {
                case 0 -> "Vector r:\nDistancia entre el origen\ny " + causante + ".";
                case 1 -> "Fórmula F:\nCoulomb aplicado.\nFuerza de " + causante + ".";
                case 2 -> "Vector F:\nDirección y magnitud\nde la fuerza de " + causante + ".";
                default -> "";
            };
        }
        if (paso == n * 3 + 1)
            return "Suma vectorial:\nResultante de todas\nlas fuerzas (ΣF).";
        if (paso == n * 3 + 2)
            return "Campo eléctrico:\nE = F / |q₀|\nen la partícula origen.";
        return "";
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================
    private void hacerNodoArrastrable(Circle circulo, Nodo nodo) {
        final double[] delta = new double[2];
        circulo.setOnMousePressed((MouseEvent me) -> {
            delta[0] = circulo.getCenterX() - me.getX();
            delta[1] = circulo.getCenterY() - me.getY();
            circulo.setCursor(javafx.scene.Cursor.MOVE);
        });
        circulo.setOnMouseReleased(me -> circulo.setCursor(javafx.scene.Cursor.HAND));
        circulo.setOnMouseDragged((MouseEvent me) -> {
            double nx = me.getX() + delta[0];
            double ny = me.getY() + delta[1];
            circulo.setCenterX(nx); 
            circulo.setCenterY(ny);
            
            if (modo3D) {
                // Invertir proyección isométrica para hallar X e Y lógicos
                CoordenadasTransformador t = new CoordenadasTransformador(canvasPlano.getHeight(), unidadActual);
                double scale = t.getPxPorUnidad();
                double originX = t.getMargin() + 5 * scale * CoordenadasTransformador.getCosIso();
                double originY = t.getCanvasHeight() - t.getMargin() - 0.5 * scale;
                
                double uz = nodo.getZ();
                double ux = (nx - originX) / (scale * CoordenadasTransformador.getCosIso()) + uz;
                double uy = (originY - ny) / scale - (ux + uz) * CoordenadasTransformador.getSinIso();
                
                nodo.setX(t.unidadXToPx(ux));
                nodo.setY(t.unidadYToPx(uy));
            } else {
                nodo.setX(nx); 
                nodo.setY(ny);
            }
            
            actualizarAristas();
            grafoPane.getChildren().stream()
                .filter(n -> n instanceof Text &&
                    ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                .findFirst()
                .ifPresent(n -> { ((Text) n).setX(nx - 4); ((Text) n).setY(ny + 4); });
        });
        circulo.setOnMouseEntered(me -> circulo.setCursor(javafx.scene.Cursor.HAND));
    }

    private boolean verificarSuperposicion(double x, double y) {
        return nodoCirculos.values().stream()
            .anyMatch(c -> Math.hypot(c.getCenterX() - x, c.getCenterY() - y) < 40);
    }

    private Nodo buscarNodoPorNombre(String nombre) {
        return grafo.getNodos().stream()
            .filter(n -> n.getNombre().equals(nombre))
            .findFirst().orElse(null);
    }

    private double[] puntoEnCirc(double cx, double cy, double tx, double ty, double r) {
        double dx = tx - cx, dy = ty - cy, d = Math.hypot(dx, dy);
        if (d == 0) return new double[]{cx, cy};
        return new double[]{cx + dx / d * r, cy + dy / d * r};
    }

    private String formatearNumero(double valor) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.000000", sym).format(valor);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(" "); a.setContentText(mensaje);
        a.showAndWait();
    }
     @FXML
    private void Regresar() throws IOException {
        App.setRoot("Simuladores"); // Asegúrate de que este FXML existe
    }

}