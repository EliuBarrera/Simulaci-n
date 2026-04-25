package com.usta.controllers;

import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.GeneradorEscena3D;
import com.usta.utils.UnidadDistancia;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.Map;

/**
 * Gestiona la alternancia entre el modo de vista 2D (plano + ScrollPane)
 * y el modo 3D (SubScene JavaFX 3D).
 *
 * Responsabilidades:
 *  - Mostrar/ocultar Canvas, SubScene y nodos del grafoPane.
 *  - Ajustar políticas del ScrollPane.
 *  - Sincronizar el grafo 3D cuando se activa ese modo.
 *  - Delegar la limpieza de resultados al controlador mediante un {@link Runnable}.
 */
public class Modo3DHandler {

    // ── Referencias a la vista ────────────────────────────────────────────────
    private final Canvas          canvasPlano;
    private final ScrollPane      scrollPane;
    private final Pane            grafoPane;
    private final TabPane         tabPanePrincipal;
    private final Tab             animacionTab;
    private final HBox            coordZBox;
    private final HBox            editCoordZBox;   // puede ser null
    private final Label           modo3DInfoLabel;

    // ── Referencias a colaboradores ───────────────────────────────────────────
    private final GeneradorEscena3D  generador3D;
    private final GrafoRenderer      renderer;
    private final RutaHandler        rutaHandler;
    private final Grafo              grafo;
    private final Map<Nodo, Circle>  nodoCirculos;
    private final EtiquetaReposicionador etiquetaReposicionador;

    /**
     * Callback que el controlador usa para limpiar el estado de cálculo
     * (labels de resultado, flechas, ultimoResultado, etc.) al cambiar de modo.
     */
    private final Runnable limpiarEstadoCalculo;

    // -------------------------------------------------------------------------
    public Modo3DHandler(Canvas canvasPlano,
                         ScrollPane scrollPane,
                         Pane grafoPane,
                         TabPane tabPanePrincipal,
                         Tab animacionTab,
                         HBox coordZBox,
                         HBox editCoordZBox,
                         Label modo3DInfoLabel,
                         GeneradorEscena3D generador3D,
                         GrafoRenderer renderer,
                         RutaHandler rutaHandler,
                         Grafo grafo,
                         Map<Nodo, Circle> nodoCirculos,
                         EtiquetaReposicionador etiquetaReposicionador,
                         Runnable limpiarEstadoCalculo) {
        this.canvasPlano              = canvasPlano;
        this.scrollPane               = scrollPane;
        this.grafoPane                = grafoPane;
        this.tabPanePrincipal         = tabPanePrincipal;
        this.animacionTab             = animacionTab;
        this.coordZBox                = coordZBox;
        this.editCoordZBox            = editCoordZBox;
        this.modo3DInfoLabel          = modo3DInfoLabel;
        this.generador3D              = generador3D;
        this.renderer                 = renderer;
        this.rutaHandler              = rutaHandler;
        this.grafo                    = grafo;
        this.nodoCirculos             = nodoCirculos;
        this.etiquetaReposicionador   = etiquetaReposicionador;
        this.limpiarEstadoCalculo     = limpiarEstadoCalculo;
    }

    // -------------------------------------------------------------------------
    /**
     * Alterna entre los modos 2D y 3D.
     *
     * @param activar     {@code true} para activar 3D, {@code false} para volver a 2D.
     * @param unidadActual unidad de distancia en uso.
     */
    public void toggle(boolean activar, UnidadDistancia unidadActual) {
        // Controles de formulario que dependen del modo
        coordZBox.setVisible(activar);
        coordZBox.setManaged(activar);
        modo3DInfoLabel.setVisible(activar);
        modo3DInfoLabel.setManaged(activar);
        if (editCoordZBox != null) {
            editCoordZBox.setVisible(activar);
            editCoordZBox.setManaged(activar);
        }

        // Pestaña de animación solo disponible en 2D
        animacionTab.setDisable(activar);
        if (activar && tabPanePrincipal.getSelectionModel().getSelectedItem() == animacionTab) {
            tabPanePrincipal.getSelectionModel().select(0);
        }

        // Limpiar cálculo previo
        limpiarEstadoCalculo.run();

        if (activar) {
            activar3D(unidadActual);
        } else {
            activar2D(unidadActual);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void activar3D(UnidadDistancia unidadActual) {
        generador3D.resetCamera();

        // Ocultar canvas 2D
        canvasPlano.setWidth(0);
        canvasPlano.setHeight(0);
        canvasPlano.setVisible(false);
        canvasPlano.setManaged(false);

        // Mostrar SubScene 3D
        generador3D.getSubScene().setVisible(true);
        generador3D.getSubScene().setManaged(true);

        // Ocultar todos los nodos del grafoPane excepto el SubScene
        for (javafx.scene.Node n : grafoPane.getChildren()) {
            if (n != canvasPlano && n != generador3D.getSubScene()) {
                n.setVisible(false);
                n.setManaged(false);
            }
        }

        generador3D.sincronizarGrafo(grafo, unidadActual);

        // Bloquear scroll (el SubScene maneja su propio zoom)
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(false);
        scrollPane.setHmax(0);
        scrollPane.setVmax(0);
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
    }

    private void activar2D(UnidadDistancia unidadActual) {
        // Restaurar canvas 2D
        canvasPlano.setVisible(true);
        canvasPlano.setManaged(true);
        canvasPlano.setWidth(5000);
        canvasPlano.setHeight(5000);
        renderer.dibujarCuadrante(unidadActual);

        // Ocultar SubScene 3D
        generador3D.getSubScene().setVisible(false);
        generador3D.getSubScene().setManaged(false);

        // Restaurar todos los nodos del grafoPane
        for (javafx.scene.Node n : grafoPane.getChildren()) {
            if (n != canvasPlano && n != generador3D.getSubScene()) {
                n.setVisible(true);
                n.setManaged(true);
            }
        }

        // Reposicionar partículas en el plano 2D
        etiquetaReposicionador.reposicionarParticulas(false, unidadActual);

        CoordenadasTransformador t = etiquetaReposicionador.crearTransformador(unidadActual);
        rutaHandler.actualizarVisuales(false, t, unidadActual);

        // Restaurar scroll
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setHmax(1.0);
        scrollPane.setVmax(1.0);
    }
}