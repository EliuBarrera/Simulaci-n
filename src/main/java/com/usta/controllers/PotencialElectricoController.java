package com.usta.controllers;

import com.usta.App;
import com.usta.controllers.Handlers.EtiquetaReposicionador;
import com.usta.controllers.Handlers.GrafoRenderer;
import com.usta.controllers.Handlers.Modo3DHandler;
import com.usta.controllers.Handlers.NodoDragHandler;
import com.usta.controllers.Handlers.ParticulaHandler;
import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.GeneradorEscena3D;
import com.usta.utils.UnidadDistancia;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PotencialElectricoController {

    @FXML private Pane        grafoPane;
    @FXML private ScrollPane  scrollPane;
    @FXML private Canvas      canvasPlano;
    @FXML private AnchorPane  rootPane;
    @FXML private TabPane     tabPanePrincipal;

    @FXML private TextField          nombreParticulaField;
    @FXML private TextField          valorCargaField;
    @FXML private ToggleButton       positivaToggle;
    @FXML private ToggleButton       negativaToggle;
    @FXML private ComboBox<String>   particulaEliminarComboBox;
    @FXML private ComboBox<String>   particulaEditarComboBox;
    @FXML private TextField          particulaEditarField;

    @FXML private TextField coordXField;
    @FXML private TextField coordYField;
    @FXML private TextField coordZField;
    @FXML private HBox      coordZBox;
    @FXML private CheckBox  modo3DCheckBox;
    @FXML private Label     modo3DInfoLabel;

    @FXML private TextField editCoordXField;
    @FXML private TextField editCoordYField;
    @FXML private TextField editCoordZField;
    @FXML private HBox      editCoordZBox;

    @FXML private ComboBox<UnidadDistancia> unidadDistanciaComboBox;
    @FXML private ComboBox<String>        puntoPruebaComboBox;
    @FXML private Label                   resultadoPotencialLabel;
    @FXML private TextArea                calculosDetalladosTextArea;

    private Grafo                    grafo;
    private Map<Nodo, Circle>        nodoCirculos;
    private ObservableList<String>   nombresParticulas;
    private boolean                  modo3D        = false;
    private UnidadDistancia          unidadActual  = UnidadDistancia.METROS;
    private ToggleGroup              polaridadGroup;

    private ParticulaHandler         particulaHandler;
    private GrafoRenderer            renderer;
    private GeneradorEscena3D        generador3D;
    private NodoDragHandler          dragHandler;
    private Modo3DHandler            modo3DHandler;
    private EtiquetaReposicionador   etiquetaReposicionador;

    public void initialize() {
        grafo             = new Grafo();
        nodoCirculos      = new HashMap<>();
        nombresParticulas = FXCollections.observableArrayList();

        particulaHandler   = new ParticulaHandler(grafo, nodoCirculos, nombresParticulas,
                                                   canvasPlano, scrollPane, grafoPane);
        renderer           = new GrafoRenderer(grafoPane, canvasPlano);

        generador3D = new GeneradorEscena3D(1000, 800);
        generador3D.getSubScene().widthProperty().bind(scrollPane.widthProperty().subtract(2));
        generador3D.getSubScene().heightProperty().bind(scrollPane.heightProperty().subtract(2));
        generador3D.getSubScene().setVisible(false);
        generador3D.getSubScene().setManaged(false);
        grafoPane.getChildren().add(0, generador3D.getSubScene());

        etiquetaReposicionador = new EtiquetaReposicionador(grafoPane, canvasPlano, nodoCirculos, generador3D);
        dragHandler = new NodoDragHandler(grafoPane, nodoCirculos, null,
                () -> etiquetaReposicionador.crearTransformador(unidadActual), () -> modo3D);

        modo3DHandler = new Modo3DHandler(canvasPlano, scrollPane, grafoPane, tabPanePrincipal, null,
                coordZBox, editCoordZBox, modo3DInfoLabel, generador3D, renderer, null, grafo, nodoCirculos,
                etiquetaReposicionador, this::limpiarPotencial);

        enlazarCamposParticula();
        particulaEliminarComboBox.setItems(nombresParticulas);
        particulaEditarComboBox.setItems(nombresParticulas);
        puntoPruebaComboBox.setItems(nombresParticulas);

        polaridadGroup = new ToggleGroup();
        positivaToggle.setToggleGroup(polaridadGroup);
        negativaToggle.setToggleGroup(polaridadGroup);
        positivaToggle.setSelected(true);

        ObservableList<UnidadDistancia> unidades = FXCollections.observableArrayList(UnidadDistancia.values());
        unidadDistanciaComboBox.setItems(unidades);
        unidadDistanciaComboBox.setValue(UnidadDistancia.METROS);
        unidadDistanciaComboBox.setOnAction(e -> cambiarUnidad(unidadDistanciaComboBox.getValue()));

        canvasPlano.setWidth(2080);
        canvasPlano.setHeight(1580);
        renderer.dibujarCuadrante(unidadActual);

        javafx.application.Platform.runLater(this::cargarSistemaPrueba);
    }

    @FXML
    private void calcularPotencialBtn() {
        String nombreP = puntoPruebaComboBox.getValue();
        if (nombreP == null) {
            mostrarAlerta("Error", "Seleccione un punto de prueba P.");
            return;
        }
        Nodo nodoP = particulaHandler.buscarNodoPorNombre(nombreP);
        if (nodoP == null) return;

        double vTotal = calcularPotencialEn(nodoP.getX(), nodoP.getY(), nodoP.getZ());
        resultadoPotencialLabel.setText(String.format("%.4e V", vTotal));
        generarDetalles(nodoP, vTotal);
    }

    private double calcularPotencialEn(double x, double y, double z) {
        double k = 8.9875517923e9;
        double potencialTotal = 0;
        for (Nodo n : grafo.getNodos()) {
            if (n.getX() == x && n.getY() == y && n.getZ() == z) continue;

            double dx = unidadActual.convertirAMetros(x - n.getX());
            double dy = unidadActual.convertirAMetros(y - n.getY());
            double dz = unidadActual.convertirAMetros(z - n.getZ());
            double r = Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (r > 1e-12) {
                double signo = n.getTipoCarga().equals("+") ? 1.0 : -1.0;
                double q = signo * n.getValorCarga() * 1e-6;
                potencialTotal += k * q / r;
            }
        }
        return potencialTotal;
    }

    private void generarDetalles(Nodo p, double vTotal) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROCEDIMIENTO: POTENCIAL ELÉCTRICO\n");
        sb.append("====================================\n");
        sb.append(String.format("Punto P: %s en (%.2f, %.2f, %.2f) %s\n\n", 
                  p.getNombre(), p.getX(), p.getY(), p.getZ(), unidadActual.getSimbolo()));
        
        double k = 8.9875517923e9;
        for (Nodo n : grafo.getNodos()) {
            if (n == p) continue;
            double dx = unidadActual.convertirAMetros(p.getX() - n.getX());
            double dy = unidadActual.convertirAMetros(p.getY() - n.getY());
            double dz = unidadActual.convertirAMetros(p.getZ() - n.getZ());
            double r = Math.sqrt(dx*dx + dy*dy + dz*dz);
            double q = (n.getTipoCarga().equals("+") ? 1 : -1) * n.getValorCarga() * 1e-6;
            double vi = k * q / r;
            
            sb.append(String.format("Carga %s (%s%.2e C):\n", n.getNombre(), n.getTipoCarga(), Math.abs(q)));
            sb.append(String.format("  r = %.4e m\n", r));
            sb.append(String.format("  V = (k * q) / r = %.4e V\n\n", vi));
        }
        sb.append("------------------------------------\n");
        sb.append(String.format("POTENCIAL TOTAL V = %.4e V\n", vTotal));
        calculosDetalladosTextArea.setText(sb.toString());
    }

    @FXML private void generarPDF() { mostrarAlerta("Info", "PDF en desarrollo."); }

    @FXML
    private void agregarParticula() {
        particulaHandler.agregar(modo3D, crearTransformador(), () -> {
            Nodo u = grafo.getNodos().get(grafo.getNodos().size() - 1);
            Circle c = nodoCirculos.get(u);
            if (c != null) dragHandler.hacerArrastrable(c, u);
            if (modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
        });
    }

    @FXML
    private void eliminarParticula() {
        particulaHandler.eliminar(() -> {
            if (modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
        });
    }

    @FXML
    private void editarParticula() {
        particulaHandler.editar(modo3D, crearTransformador(), () -> {
            if (modo3D) generador3D.sincronizarGrafo(grafo, unidadActual);
        });
    }

    @FXML private void toggleModo3D() { modo3D = modo3DCheckBox.isSelected(); modo3DHandler.toggle(modo3D, unidadActual); }
    @FXML private void Regresar() throws IOException { App.setRoot("Simuladores"); }

    private CoordenadasTransformador crearTransformador() { return etiquetaReposicionador.crearTransformador(unidadActual); }
    private void cambiarUnidad(UnidadDistancia nueva) { unidadActual = nueva; if (modo3D) renderer.dibujarCuadrante3D(unidadActual, crearTransformador()); else renderer.dibujarCuadrante(unidadActual); }
    private void limpiarPotencial() { resultadoPotencialLabel.setText("-"); }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(" "); a.setContentText(mensaje);
        a.showAndWait();
    }

    private void cargarSistemaPrueba() {
        nombreParticulaField.setText("q1"); positivaToggle.setSelected(true); valorCargaField.setText("5"); coordXField.setText("5"); coordYField.setText("5");
        agregarParticula();
        nombreParticulaField.setText("P"); valorCargaField.setText("0"); coordXField.setText("10"); coordYField.setText("5");
        agregarParticula();
        puntoPruebaComboBox.setValue("P");
    }

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
}
