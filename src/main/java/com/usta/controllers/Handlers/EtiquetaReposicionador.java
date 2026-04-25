package com.usta.controllers.Handlers;

import com.usta.models.Arista;
import com.usta.models.Nodo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.GeneradorEscena3D;
import com.usta.utils.UnidadDistancia;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.Map;

/**
 * Calcula y aplica las posiciones de pantalla de las etiquetas de texto
 * (partículas y aristas) tanto en el plano 2D como en la escena 3D.
 *
 * <p>Centraliza la lógica que antes vivía en
 * {@code LeyCoulombController#reposicionarEtiquetas3D()} y
 * {@code LeyCoulombController#reposicionarParticulas()}.
 */
public class EtiquetaReposicionador {

    private final Pane              grafoPane;
    private final Canvas            canvasPlano;
    private final Map<Nodo, Circle> nodoCirculos;
    private final GeneradorEscena3D generador3D;

    // -------------------------------------------------------------------------
    public EtiquetaReposicionador(Pane grafoPane,
                                  Canvas canvasPlano,
                                  Map<Nodo, Circle> nodoCirculos,
                                  GeneradorEscena3D generador3D) {
        this.grafoPane    = grafoPane;
        this.canvasPlano  = canvasPlano;
        this.nodoCirculos = nodoCirculos;
        this.generador3D  = generador3D;
    }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Proyecta las posiciones 3D de las partículas y aristas a la pantalla
     * y actualiza los {@link Text} correspondientes en el {@code grafoPane}.
     *
     * <p>Debe llamarse cada vez que la cámara 3D cambia de ángulo, posición
     * o zoom.
     *
     * @param unidadActual unidad de distancia para el texto de las aristas.
     */
    public void reposicionarEtiquetas3D(UnidadDistancia unidadActual) {
        if (generador3D == null) return;

        double scale = generador3D.getScale();
        javafx.scene.Group world = generador3D.getWorld();

        // ── Etiquetas de partículas ──────────────────────────────────────────
        for (Map.Entry<Nodo, Circle> entry : nodoCirculos.entrySet()) {
            Nodo nodo = entry.getKey();

            Point3D p3d   = world.localToScene(nodo.getX() * scale,
                                               -nodo.getY() * scale,
                                               nodo.getZ() * scale);
            Point2D pLocal = grafoPane.sceneToLocal(p3d.getX(), p3d.getY());

            if (pLocal == null) continue;

            grafoPane.getChildren().stream()
                    .filter(n -> n instanceof Text
                            && ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                    .findFirst()
                    .ifPresent(n -> {
                        Text txt = (Text) n;
                        txt.setVisible(true);
                        txt.setX(pLocal.getX() + 15);
                        txt.setY(pLocal.getY() - 15);
                        txt.setText(etiqueta3DParticula(nodo));
                    });
        }

        // ── Etiquetas de aristas ─────────────────────────────────────────────
        for (javafx.scene.Node n : grafoPane.getChildren()) {
            if (!(n instanceof Text)) continue;
            if (!(n.getUserData() instanceof Object[])) continue;

            Object[] data = (Object[]) n.getUserData();
            if (!"arista".equals(data[0])) continue;

            Text txt   = (Text) n;
            Arista a   = (Arista) data[1];
            Nodo   o   = a.getOrigen();
            Nodo   dst = a.getDestino();

            double mx = (o.getX() + dst.getX()) / 2.0;
            double my = (o.getY() + dst.getY()) / 2.0;
            double mz = (o.getZ() + dst.getZ()) / 2.0;

            Point3D p3d   = world.localToScene(mx * scale, -my * scale, mz * scale);
            Point2D pLocal = grafoPane.sceneToLocal(p3d.getX(), p3d.getY());

            if (pLocal != null) {
                txt.setVisible(true);
                txt.setX(pLocal.getX());
                txt.setY(pLocal.getY());
                txt.setText(String.format("%.2f %s", a.getPeso(), unidadActual.getSimbolo()));
            }
        }
    }

    /**
     * Recalcula la posición de pantalla de todas las partículas a partir de
     * sus coordenadas lógicas (modo 2D).
     *
     * @param modo3D       si es {@code true} delega a
     *                     {@link #reposicionarEtiquetas3D(UnidadDistancia)}.
     * @param unidadActual unidad de distancia actual.
     */
    public void reposicionarParticulas(boolean modo3D, UnidadDistancia unidadActual) {
        if (modo3D) {
            reposicionarEtiquetas3D(unidadActual);
            return;
        }

        CoordenadasTransformador t = crearTransformador(unidadActual);

        for (Map.Entry<Nodo, Circle> entry : nodoCirculos.entrySet()) {
            Nodo   nodo = entry.getKey();
            Circle c    = entry.getValue();

            double[] screen = t.logicalToScreen(nodo.getX(), nodo.getY(), nodo.getZ(), false);
            c.setCenterX(screen[0]);
            c.setCenterY(screen[1]);

            grafoPane.getChildren().stream()
                    .filter(n -> n instanceof Text
                            && ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                    .findFirst()
                    .ifPresent(n -> {
                        Text txt = (Text) n;
                        txt.setX(screen[0] - 4);
                        txt.setY(screen[1] + 4);
                        txt.setText(etiqueta2DParticula(nodo));
                    });
        }
    }

    /**
     * Crea un {@link CoordenadasTransformador} con el estado actual del canvas.
     * Útil para que {@link Modo3DHandler} no tenga que duplicar esta lógica.
     */
    public CoordenadasTransformador crearTransformador(UnidadDistancia unidad) {
        double alphaDeg = 30;
        double betaDeg  = 30;
        return new CoordenadasTransformador(
                canvasPlano.getHeight(), canvasPlano.getWidth(),
                unidad, alphaDeg, betaDeg);
    }

    // =========================================================================
    // Helpers de formato de etiquetas
    // =========================================================================

    private static String etiqueta3DParticula(Nodo nodo) {
        return String.format("%s (%s %s)\nPos: (%.1f, %.1f, %.1f)",
                nodo.getNombre(), nodo.getValorCarga(), nodo.getTipoCarga(),
                nodo.getX(), nodo.getY(), nodo.getZ());
    }

    private static String etiqueta2DParticula(Nodo nodo) {
        return nodo.getNombre() + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ")";
    }
}