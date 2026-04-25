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
import javafx.scene.input.MouseEvent;
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
    @FXML private Label resultadoPotencialLabel;
    @FXML private Label mousePosLabel;

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

        // Listener para medir potencial en tiempo real
        canvasPlano.setOnMouseMoved(this::handleMouseMoved);
        canvasPlano.setOnMouseExited(e -> {
            resultadoPotencialLabel.setText("-");
            mousePosLabel.setText("Mueva el mouse sobre el plano");
        });

        javafx.application.Platform.runLater(this::cargarSistemaPrueba);
    }

    private void handleMouseMoved(MouseEvent e) {
        if (modo3D) return; // En 3D el mouse orbita la cámara

        CoordenadasTransformador t = crearTransformador();
        double logX = t.pxXToUnidad(e.getX());
        double logY = t.pxYToUnidad(e.getY());

        double V = calcularPotencialEn(logX, logY, 0);
        
        mousePosLabel.setText(String.format("Posición: (%.2f, %.2f) %s", logX, logY, unidadActual.getSimbolo()));
        resultadoPotencialLabel.setText(String.format("%.4e V", V));
    }

    private double calcularPotencialEn(double x, double y, double z) {
        double k = 8.9875517923e9;
        double potencialTotal = 0;
        
        // Convertir coordenadas lógicas a metros usando la unidad actual
        for (Nodo n : grafo.getNodos()) {
            double dx = unidadActual.convertirAMetros(x - n.getX());
            double dy = unidadActual.convertirAMetros(y - n.getY());
            double dz = unidadActual.convertirAMetros(z - n.getZ());
            double r = Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (r > 1e-12) { // Evitar división por cero
                // Aplicar el signo de la carga (+ o -)
                double signo = n.getTipoCarga().equals("+") ? 1.0 : -1.0;
                double q = signo * n.getValorCarga() * 1e-6; // µC to C
                potencialTotal += k * q / r;
            }
        }
        return potencialTotal;
    }


    @FXML
    private void agregarParticula() {
        CoordenadasTransformador t = crearTransformador();
        particulaHandler.agregar(modo3D, t, () -> {
            Nodo ultimo = grafo.getNodos().get(grafo.getNodos().size() - 1);
            Circle c = nodoCirculos.get(ultimo);
            if (c != null) dragHandler.hacerArrastrable(c, ultimo);
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

    @FXML
    private void toggleModo3D() {
        modo3D = modo3DCheckBox.isSelected();
        modo3DHandler.toggle(modo3D, unidadActual);
    }

    @FXML
    private void Regresar() throws IOException { App.setRoot("Simuladores"); }

    private CoordenadasTransformador crearTransformador() {
        return etiquetaReposicionador.crearTransformador(unidadActual);
    }

    private void cambiarUnidad(UnidadDistancia nueva) {
        unidadActual = nueva;
        if (modo3D) renderer.dibujarCuadrante3D(unidadActual, crearTransformador());
        else        renderer.dibujarCuadrante(unidadActual);
    }

    private void limpiarPotencial() {
        resultadoPotencialLabel.setText("-");
    }

    private void cargarSistemaPrueba() {
        nombreParticulaField.setText("q1"); positivaToggle.setSelected(true);
        valorCargaField.setText("5"); coordXField.setText("5"); coordYField.setText("5");
        agregarParticula();
        nombreParticulaField.clear(); valorCargaField.clear(); coordXField.clear(); coordYField.clear();
        scrollPane.setHvalue(0); scrollPane.setVvalue(1.0);
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
