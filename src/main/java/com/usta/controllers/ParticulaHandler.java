package com.usta.controllers;

import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.UnidadDistancia;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.Map;
import java.util.Random;

/**
 * Maneja la lógica de agregar, editar y eliminar partículas (nodos) en el grafo.
 */
public class ParticulaHandler {

    private final Grafo grafo;
    private final Map<Nodo, Circle> nodoCirculos;
    private final ObservableList<String> nombresParticulas;
    private final Canvas canvasPlano;
    private final ScrollPane scrollPane;
    private final Pane grafoPane;

    // Campos FXML — se asignan desde LeyCoulombController
    public TextField nombreParticulaField;
    public TextField valorCargaField;
    public TextField tipoCargaField;
    public TextField coordXField;
    public TextField coordYField;
    public TextField coordZField;
    public ComboBox<String> particulaEliminarComboBox;
    public TextField particulaEditarField;
    public ComboBox<String> particulaEditarComboBox;

    public ParticulaHandler(Grafo grafo,
                            Map<Nodo, Circle> nodoCirculos,
                            ObservableList<String> nombresParticulas,
                            Canvas canvasPlano,
                            ScrollPane scrollPane,
                            Pane grafoPane) {
        this.grafo              = grafo;
        this.nodoCirculos       = nodoCirculos;
        this.nombresParticulas  = nombresParticulas;
        this.canvasPlano        = canvasPlano;
        this.scrollPane         = scrollPane;
        this.grafoPane          = grafoPane;
    }

    /**
     * Agrega una nueva partícula al grafo y la dibuja en el panel.
     *
     * @param modo3D        si el modo tridimensional está activo
     * @param unidadActual  unidad de distancia activa
     * @param onAgregada    callback que se ejecuta tras agregar exitosamente (para actualizar combos)
     */
    public void agregar(boolean modo3D, UnidadDistancia unidadActual, Runnable onAgregada) {
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

        // Coordenadas: manuales o aleatorias
        String cxStr = coordXField.getText().trim();
        String cyStr = coordYField.getText().trim();
        String czStr = (coordZField != null) ? coordZField.getText().trim() : "";
        boolean coordsManuales = !cxStr.isEmpty() && !cyStr.isEmpty();

        double x, y, zLogica = 0;

        if (coordsManuales) {
            try {
                double ux = Double.parseDouble(cxStr);
                double uy = Double.parseDouble(cyStr);
                x = 40 + ux * 100;
                y = (canvasPlano.getHeight() - 40) - uy * 100;
                if (modo3D && !czStr.isEmpty()) zLogica = Double.parseDouble(czStr);
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

        String etiqueta = modo3D
            ? nombre + " (" + valorCarga + ") (" + tipoCarga + ") z=" + String.format("%.1f", zLogica)
            : nombre + " (" + valorCarga + ") (" + tipoCarga + ")";
        Text texto = new Text(etiqueta);
        texto.setX(visPx - 4);
        texto.setY(visPy + 4);

        nodoCirculos.put(nueva, circulo);
        grafoPane.getChildren().addAll(circulo, texto);

        limpiarCampos();
        if (onAgregada != null) onAgregada.run();
    }

    /**
     * Elimina la partícula seleccionada en el combo de eliminar.
     *
     * @param onEliminada  callback tras eliminar (para limpiar flechas, actualizar combos, etc.)
     */
    public void eliminar(Runnable onEliminada) {
        String nombre = particulaEliminarComboBox.getValue();
        if (nombre == null) { mostrarAlerta("Error", "Seleccione una partícula."); return; }

        Nodo nodo = buscarNodoPorNombre(nombre);
        if (nodo == null) { mostrarAlerta("Error", "La partícula no existe."); return; }

        grafo.eliminarAristasDeNodo(nodo);
        grafo.eliminarNodo(nodo);

        Circle c = nodoCirculos.remove(nodo);
        if (c != null) grafoPane.getChildren().remove(c);
        grafoPane.getChildren().removeIf(n ->
            n instanceof Text && ((Text) n).getText().startsWith(nodo.getNombre() + " "));

        nombresParticulas.remove(nombre);
        particulaEliminarComboBox.getSelectionModel().clearSelection();

        if (onEliminada != null) onEliminada.run();
    }

    /**
     * Renombra la partícula seleccionada en el combo de editar.
     */
    public void editar() {
        String nombreActual = particulaEditarComboBox.getValue();
        String nuevoNombre  = particulaEditarField.getText().trim();
        if (nombreActual == null)  { mostrarAlerta("Error", "Seleccione una partícula."); return; }
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
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    public Nodo buscarNodoPorNombre(String nombre) {
        return grafo.getNodos().stream()
            .filter(n -> n.getNombre().equals(nombre))
            .findFirst().orElse(null);
    }

    private boolean verificarSuperposicion(double x, double y) {
        return nodoCirculos.values().stream()
            .anyMatch(c -> Math.hypot(c.getCenterX() - x, c.getCenterY() - y) < 40);
    }

    private void limpiarCampos() {
        nombreParticulaField.clear();
        valorCargaField.clear();
        tipoCargaField.clear();
        coordXField.clear();
        coordYField.clear();
        if (coordZField != null) coordZField.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(" "); a.setContentText(mensaje);
        a.showAndWait();
    }
}