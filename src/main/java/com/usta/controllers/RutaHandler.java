package com.usta.controllers;

import com.usta.models.Arista;
import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.UnidadDistancia;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Maneja la lógica de creación y eliminación de aristas (rutas) entre partículas.
 *
 * Las distancias se calculan en coordenadas lógicas del Nodo.
 * La representación visual usa CoordenadasTransformador para proyectar a pantalla.
 */
public class RutaHandler {

    private final Grafo grafo;
    private final Pane grafoPane;

    // Campos FXML — se asignan desde LeyCoulombController
    public ComboBox<String> origenRutaComboBox;
    public ComboBox<String> destinoRutaComboBox;
    public ComboBox<String> eliminarRutaComboBox;

    public RutaHandler(Grafo grafo, Pane grafoPane) {
        this.grafo     = grafo;
        this.grafoPane = grafoPane;
    }

    /**
     * Agrega o actualiza la arista entre las partículas seleccionadas.
     * La distancia se calcula en coordenadas lógicas (unidades del plano).
     */
    public void agregar(boolean modo3D) {
        String oNombre = origenRutaComboBox.getValue();
        String dNombre = destinoRutaComboBox.getValue();
        if (oNombre == null || dNombre == null) { mostrarAlerta("Error", "Complete origen y destino."); return; }
        if (oNombre.equals(dNombre))            { mostrarAlerta("Error", "Origen y destino deben ser distintos."); return; }

        Nodo origen  = buscarNodo(oNombre);
        Nodo destino = buscarNodo(dNombre);
        if (origen == null || destino == null)  { mostrarAlerta("Error", "Las partículas deben existir."); return; }

        // Distancia en coordenadas lógicas (unidades del plano)
        double peso;
        if (modo3D) {
            peso = Math.sqrt(
                Math.pow(origen.getX() - destino.getX(), 2) +
                Math.pow(origen.getY() - destino.getY(), 2) +
                Math.pow(origen.getZ() - destino.getZ(), 2));
        } else {
            peso = Math.hypot(origen.getX() - destino.getX(),
                              origen.getY() - destino.getY());
        }

        Arista existente = null, inversa = null;
        for (Arista a : grafo.getAristas()) {
            if (a.getOrigen().equals(origen)  && a.getDestino().equals(destino)) existente = a;
            if (a.getOrigen().equals(destino) && a.getDestino().equals(origen))  inversa   = a;
        }
        if      (existente != null) existente.setPeso(peso);
        else if (inversa   != null) inversa.setPeso(peso);
        else grafo.agregarArista(new Arista(origen, destino, peso));

        origenRutaComboBox.getSelectionModel().clearSelection();
        destinoRutaComboBox.getSelectionModel().clearSelection();
    }

    /**
     * Elimina la arista seleccionada en el combo de eliminar.
     */
    public void eliminar() {
        String ruta = eliminarRutaComboBox.getValue();
        if (ruta == null) { mostrarAlerta("Error", "Seleccione una ruta."); return; }

        String[] partes = ruta.split(" - ");
        String oNombre  = partes[0];
        String dNombre  = partes[1].split(" \\(")[0];

        Nodo origen  = buscarNodo(oNombre);
        Nodo destino = buscarNodo(dNombre);
        if (origen == null || destino == null) { mostrarAlerta("Error", "Partículas no encontradas."); return; }

        List<Arista> aEliminar = new ArrayList<>();
        for (Arista a : grafo.getAristas()) {
            if (a.esIgual(new Arista(origen, destino, 0)) ||
                a.esIgual(new Arista(destino, origen, 0)))
                aEliminar.add(a);
        }
        if (aEliminar.isEmpty()) { mostrarAlerta("Error", "La ruta no existe."); return; }
        aEliminar.forEach(grafo::eliminarArista);
    }

    /**
     * Reconstruye las líneas de aristas en el grafoPane y actualiza el combo de eliminar.
     * Usa el transformador para proyectar las coordenadas lógicas del Nodo a pantalla.
     *
     * @param modo3D        si el modo 3D está activo
     * @param t             transformador de coordenadas actual
     * @param unidadActual  unidad de distancia activa
     */
    public void actualizarVisuales(boolean modo3D, CoordenadasTransformador t,
                                    UnidadDistancia unidadActual) {
        grafoPane.getChildren().removeIf(n -> {
            Object ud = n.getUserData();
            if (ud instanceof String) return "arista".equals(ud);
            if (ud instanceof Object[]) return "arista".equals(((Object[])ud)[0]);
            return false;
        });

        for (Arista a : grafo.getAristas()) {
            Nodo o = a.getOrigen(), d = a.getDestino();

            // Proyectar coordenadas lógicas a pantalla
            double[] screenO = t.logicalToScreen(o.getX(), o.getY(), o.getZ(), modo3D);
            double[] screenD = t.logicalToScreen(d.getX(), d.getY(), d.getZ(), modo3D);

            double ox = screenO[0], oy = screenO[1];
            double dx = screenD[0], dy = screenD[1];

            double[] pi = puntoEnCirc(ox, oy, dx, dy, 15);
            double[] pf = puntoEnCirc(dx, dy, ox, oy, 15);

            Line linea = new Line(pi[0], pi[1], pf[0], pf[1]);
            linea.setStrokeWidth(2);
            linea.setStroke(Color.GRAY);
            linea.setUserData("arista");

            // Distancia en coordenadas lógicas
            double distUnidad;
            if (modo3D) {
                distUnidad = Math.sqrt(
                    Math.pow(o.getX() - d.getX(), 2) +
                    Math.pow(o.getY() - d.getY(), 2) +
                    Math.pow(o.getZ() - d.getZ(), 2));
            } else {
                distUnidad = Math.hypot(o.getX() - d.getX(), o.getY() - d.getY());
            }
            a.setPeso(distUnidad);

            Text peso = new Text(String.format("%.2f %s", distUnidad, unidadActual.getSimbolo()));
            peso.setX((pi[0] + pf[0]) / 2);
            peso.setY((pi[1] + pf[1]) / 2);
            // Guardamos tanto el tag como el objeto Arista para facilitar la proyección 3D
            peso.setUserData(new Object[]{"arista", a});

            if (modo3D) {
                linea.setVisible(false);
                // El peso se mantiene visible, pero será reposicionado por el controlador en 3D
                peso.setVisible(true);
            }

            grafoPane.getChildren().addAll(linea, peso);

        }

        actualizarComboEliminar(unidadActual);
    }

    /** Recarga el combo de rutas eliminables. */
    public void actualizarComboEliminar(UnidadDistancia unidadActual) {
        ObservableList<String> rutas = FXCollections.observableArrayList();
        java.util.Set<String> idSet = new java.util.HashSet<>();

        for (Arista a : grafo.getAristas()) {
            String id = a.getIdentificador();
            if (idSet.contains(id)) continue;
            idSet.add(id);

            String r = a.getOrigen().getNombre() + " - " + a.getDestino().getNombre()
                + " (" + String.format("%.2f %s", a.getPeso(), unidadActual.getSimbolo()) + ")";
            rutas.add(r);
        }
        eliminarRutaComboBox.setItems(rutas);
    }

    // ── Utilidades ──────────────────────────────────────────────────────

    private Nodo buscarNodo(String nombre) {
        return grafo.getNodos().stream()
            .filter(n -> n.getNombre().equals(nombre))
            .findFirst().orElse(null);
    }

    private double[] puntoEnCirc(double cx, double cy, double tx, double ty, double r) {
        double ddx = tx - cx, ddy = ty - cy, d = Math.hypot(ddx, ddy);
        if (d == 0) return new double[]{cx, cy};
        return new double[]{cx + ddx / d * r, cy + ddy / d * r};
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(" "); a.setContentText(mensaje);
        a.showAndWait();
    }
}