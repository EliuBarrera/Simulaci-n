package com.usta.controllers;

import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.utils.CoordenadasTransformador;
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
 *
 * Las coordenadas del Nodo son siempre LÓGICAS (unidades del plano).
 * La conversión a pantalla se hace mediante CoordenadasTransformador.
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
    public ToggleButton positivaToggle;
    public ToggleButton negativaToggle;
    public TextField coordXField;
    public TextField coordYField;
    public TextField coordZField;
    public ComboBox<String> particulaEliminarComboBox;

    // Editar partícula
    public TextField particulaEditarField;
    public ComboBox<String> particulaEditarComboBox;
    public TextField editCoordXField;
    public TextField editCoordYField;
    public TextField editCoordZField;

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
     * Las coordenadas del Nodo se almacenan en unidades lógicas del plano.
     *
     * @param modo3D        si el modo tridimensional está activo
     * @param t             transformador de coordenadas actual
     * @param onAgregada    callback tras agregar exitosamente
     */
    public void agregar(boolean modo3D, CoordenadasTransformador t, Runnable onAgregada) {
        String nombre        = nombreParticulaField.getText().trim();
        String valorCargaStr = valorCargaField.getText().trim();
        String tipoCarga     = positivaToggle.isSelected() ? "+" : "-";

        if (nombre.isEmpty())        { mostrarAlerta("Error", "Ingrese el nombre de la partícula."); return; }
        if (valorCargaStr.isEmpty()) { mostrarAlerta("Error", "Ingrese el valor de la carga.");      return; }

        double valorCarga;
        try {
            valorCarga = Double.parseDouble(valorCargaStr);
            if (valorCarga <= 0) { mostrarAlerta("Error", "La carga debe ser positiva."); return; }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La carga debe ser un número válido."); return;
        }

        /*if (!tipoCarga.equals("+") && !tipoCarga.equals("-")) {
            mostrarAlerta("Error", "El tipo de carga debe ser + o -"); return;
        }*/
        for (Nodo n : grafo.getNodos()) {
            if (n.getNombre().equalsIgnoreCase(nombre)) {
                mostrarAlerta("Error", "Ya existe una partícula con ese nombre."); return;
            }
        }

        // ── Coordenadas lógicas ─────────────────────────────────────────
        String cxStr = coordXField.getText().trim();
        String cyStr = coordYField.getText().trim();
        String czStr = (coordZField != null) ? coordZField.getText().trim() : "";
        boolean coordsManuales = !cxStr.isEmpty() && !cyStr.isEmpty();

        double logX, logY, logZ = 0;

        if (coordsManuales) {
            try {
                logX = Double.parseDouble(cxStr);
                logY = Double.parseDouble(cyStr);
                if (modo3D && !czStr.isEmpty()) logZ = Double.parseDouble(czStr);
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "Las coordenadas deben ser números válidos."); return;
            }
        } else {
            // Generar coordenadas lógicas aleatorias dentro del viewport visible
            Random random = new Random();
            if (modo3D) {
                logX = 0.5 + random.nextDouble() * 9;
                logY = 0.5 + random.nextDouble() * 9;
                logZ = 0;
            } else {
                // Calcular el rango visible en unidades lógicas
                double viewW = scrollPane.getViewportBounds().getWidth();
                double viewH = scrollPane.getViewportBounds().getHeight();
                double scrollX = scrollPane.getHvalue() * (canvasPlano.getWidth()  - viewW);
                double scrollY = scrollPane.getVvalue() * (canvasPlano.getHeight() - viewH);

                double minLogX = t.pxXToUnidad(scrollX + 50);
                double maxLogX = t.pxXToUnidad(scrollX + viewW - 50);
                double minLogY = t.pxYToUnidad(scrollY + viewH - 50);
                double maxLogY = t.pxYToUnidad(scrollY + 50);

                logX = logY = 0;
                int intentos = 0;
                boolean sup;
                do {
                    logX = minLogX + random.nextDouble() * (maxLogX - minLogX);
                    logY = minLogY + random.nextDouble() * (maxLogY - minLogY);
                    logX = Math.max(0.5, logX);
                    logY = Math.max(0.5, logY);
                    double[] scr = t.logicalToScreen(logX, logY, 0, false);
                    sup = verificarSuperposicionScreen(scr[0], scr[1]);
                    intentos++;
                } while (sup && intentos < 100);
                if (intentos >= 100) { mostrarAlerta("Error", "No se pudo ubicar la partícula."); return; }
            }
        }

        // ── Crear nodo con coordenadas lógicas ──────────────────────────
        Nodo nueva = new Nodo(nombre, logX, logY, logZ, valorCarga, tipoCarga);
        grafo.agregarNodo(nueva);
        nombresParticulas.add(nombre);

        // ── Crear representación visual ─────────────────────────────────
        double[] screen = t.logicalToScreen(logX, logY, logZ, modo3D);
        double visPx = screen[0], visPy = screen[1];

        Color color = tipoCarga.equals("+") ? Color.LIGHTCORAL : Color.LIGHTBLUE;
        Circle circulo = new Circle(visPx, visPy, 15, color);
        circulo.setStroke(Color.BLACK);

        String etiqueta = modo3D
            ? nombre + " (" + valorCarga + ") (" + tipoCarga + ") z=" + String.format("%.1f", logZ)
            : nombre + " (" + valorCarga + ") (" + tipoCarga + ")";
        Text texto = new Text(etiqueta);
        texto.setX(visPx - 4);
        texto.setY(visPy + 4);

        if (modo3D) {
            circulo.setVisible(false);
            texto.setVisible(false);
        }

        nodoCirculos.put(nueva, circulo);
        grafoPane.getChildren().addAll(circulo, texto);

        limpiarCampos();
        if (onAgregada != null) onAgregada.run();
    }

    /**
     * Elimina la partícula seleccionada en el combo de eliminar.
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
     * Edita la partícula seleccionada: renombra y/o reposiciona.
     *
     * @param modo3D  si el modo 3D está activo
     * @param t       transformador de coordenadas actual
     * @param onEditada callback tras editar exitosamente
     */
    public void editar(boolean modo3D, CoordenadasTransformador t, Runnable onEditada) {
        String nombreActual = particulaEditarComboBox.getValue();
        String nuevoNombre  = particulaEditarField.getText().trim();
        if (nombreActual == null)  { mostrarAlerta("Error", "Seleccione una partícula."); return; }

        Nodo nodo = buscarNodoPorNombre(nombreActual);
        if (nodo == null) { mostrarAlerta("Error", "La partícula no existe."); return; }

        // ── Renombrar ───────────────────────────────────────────────────
        String nombreFinal = nombreActual;
        if (!nuevoNombre.isEmpty() && !nuevoNombre.equals(nombreActual)) {
            for (Nodo n : grafo.getNodos()) {
                if (n.getNombre().equals(nuevoNombre)) {
                    mostrarAlerta("Error", "Ya existe una partícula con ese nombre."); return;
                }
            }
            nodo.setNombre(nuevoNombre);
            nombresParticulas.remove(nombreActual);
            nombresParticulas.add(nuevoNombre);
            nombreFinal = nuevoNombre;
        }

        // ── Reubicar ────────────────────────────────────────────────────
        boolean reubicado = false;
        String exStr = (editCoordXField != null) ? editCoordXField.getText().trim() : "";
        String eyStr = (editCoordYField != null) ? editCoordYField.getText().trim() : "";
        String ezStr = (editCoordZField != null) ? editCoordZField.getText().trim() : "";

        if (!exStr.isEmpty() || !eyStr.isEmpty() || !ezStr.isEmpty()) {
            try {
                if (!exStr.isEmpty()) nodo.setX(Double.parseDouble(exStr));
                if (!eyStr.isEmpty()) nodo.setY(Double.parseDouble(eyStr));
                if (!ezStr.isEmpty() && modo3D) nodo.setZ(Double.parseDouble(ezStr));
                reubicado = true;
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "Las coordenadas deben ser números válidos."); return;
            }
        }

        // ── Actualizar visuales ─────────────────────────────────────────
        Circle c = nodoCirculos.get(nodo);
        if (c != null && reubicado) {
            double[] screen = t.logicalToScreen(nodo.getX(), nodo.getY(), nodo.getZ(), modo3D);
            c.setCenterX(screen[0]);
            c.setCenterY(screen[1]);
        }

        // Actualizar etiqueta de texto
        final String labelStart = nombreActual + " ";
        final String newLabel = modo3D
            ? nombreFinal + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ") z=" + String.format("%.1f", nodo.getZ())
            : nombreFinal + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ")";

        grafoPane.getChildren().stream()
            .filter(n -> n instanceof Text && ((Text) n).getText().startsWith(labelStart))
            .findFirst()
            .ifPresent(n -> {
                Text txt = (Text) n;
                txt.setText(newLabel);
                if (c != null) {
                    txt.setX(c.getCenterX() - 4);
                    txt.setY(c.getCenterY() + 4);
                }
                if (modo3D) txt.setVisible(false);
            });

        particulaEditarComboBox.getSelectionModel().clearSelection();
        particulaEditarField.clear();
        if (editCoordXField != null) editCoordXField.clear();
        if (editCoordYField != null) editCoordYField.clear();
        if (editCoordZField != null) editCoordZField.clear();

        if (onEditada != null) onEditada.run();
    }

    // ── Utilidades ──────────────────────────────────────────────────────

    public Nodo buscarNodoPorNombre(String nombre) {
        return grafo.getNodos().stream()
            .filter(n -> n.getNombre().equals(nombre))
            .findFirst().orElse(null);
    }

    private boolean verificarSuperposicionScreen(double screenX, double screenY) {
        return nodoCirculos.values().stream()
            .anyMatch(c -> Math.hypot(c.getCenterX() - screenX, c.getCenterY() - screenY) < 40);
    }

    private void limpiarCampos() {
        nombreParticulaField.clear();
        valorCargaField.clear();
        positivaToggle.setSelected(true);
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