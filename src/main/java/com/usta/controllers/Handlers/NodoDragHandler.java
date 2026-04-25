package com.usta.controllers.Handlers;

import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.UnidadDistancia;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Hace arrastrables los círculos de partículas en el plano lógico.
 *
 * Reglas de arrastre:
 *  - Click izquierdo + arrastrar  → mueve en X/Y (Z constante).
 *  - Shift + arrastrar vertical   → mueve en Z (solo modo 3D).
 *
 * Requiere un {@link Supplier<CoordenadasTransformador>} para obtener
 * siempre el transformador con el estado actual de la vista.
 */
public class NodoDragHandler {

    private final Pane grafoPane;
    private final Map<Nodo, Circle> nodoCirculos;
    private final RutaHandler rutaHandler;

    /** Proporciona el transformador actualizado en cada evento. */
    private final Supplier<CoordenadasTransformador> transformadorSupplier;
    /** Indica si la vista está en modo 3D en el momento del evento. */
    private final BooleanSupplier modo3DSupplier;

    // -------------------------------------------------------------------------
    public NodoDragHandler(Pane grafoPane,
                           Map<Nodo, Circle> nodoCirculos,
                           RutaHandler rutaHandler,
                           Supplier<CoordenadasTransformador> transformadorSupplier,
                           BooleanSupplier modo3DSupplier) {
        this.grafoPane             = grafoPane;
        this.nodoCirculos          = nodoCirculos;
        this.rutaHandler           = rutaHandler;
        this.transformadorSupplier = transformadorSupplier;
        this.modo3DSupplier        = modo3DSupplier;
    }

    // -------------------------------------------------------------------------
    /**
     * Registra los manejadores de ratón en {@code circulo} para que el
     * {@code nodo} sea arrastrable.
     */
    public void hacerArrastrable(Circle circulo, Nodo nodo) {

        // [0]=offsetX, [1]=offsetY, [2]=startLogZ, [3]=startScreenY
        final double[] delta = new double[4];

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

            boolean modo3D = modo3DSupplier.getAsBoolean();
            CoordenadasTransformador t = transformadorSupplier.get();

            if (modo3D && me.isShiftDown()) {
                // ── Shift + arrastrar → mover en Z ──────────────────────────
                double dy  = me.getY() - delta[3];
                double newZ = delta[2] - dy / t.getPxPorUnidad();
                nodo.setZ(Math.max(0, Math.min(10, newZ)));
            } else {
                // ── Arrastrar normal → mover en X/Y ─────────────────────────
                double nx = me.getX() + delta[0];
                double ny = me.getY() + delta[1];
                double[] logCoords = t.screenToLogical(nx, ny, nodo.getZ(), modo3D);
                nodo.setX(logCoords[0]);
                nodo.setY(logCoords[1]);
            }

            // Actualizar posición del círculo en pantalla
            double[] screen = t.logicalToScreen(nodo.getX(), nodo.getY(), nodo.getZ(), modo3DSupplier.getAsBoolean());
            circulo.setCenterX(screen[0]);
            circulo.setCenterY(screen[1]);

            // Redibujar aristas (solo si el handler de rutas existe)
            if (rutaHandler != null) {
                rutaHandler.actualizarVisuales(modo3DSupplier.getAsBoolean(), t,
                        t.getUnidad() != null ? t.getUnidad() : com.usta.utils.UnidadDistancia.METROS);
            }

            // Actualizar etiqueta de texto
            actualizarEtiqueta(nodo, screen, modo3DSupplier.getAsBoolean());

            me.consume();
        });

        circulo.setOnMouseEntered(me -> circulo.setCursor(Cursor.HAND));
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void actualizarEtiqueta(Nodo nodo, double[] screen, boolean modo3D) {
        grafoPane.getChildren().stream()
                .filter(n -> n instanceof Text
                        && ((Text) n).getText().startsWith(nodo.getNombre() + " "))
                .findFirst()
                .ifPresent(n -> {
                    Text txt = (Text) n;
                    txt.setX(screen[0] - 4);
                    txt.setY(screen[1] + 4);
                    txt.setText(buildLabel(nodo, modo3D));
                });
    }

    static String buildLabel(Nodo nodo, boolean modo3D) {
        if (modo3D) {
            return String.format("%s (%s) (%s) z=%.1f",
                    nodo.getNombre(), nodo.getValorCarga(),
                    nodo.getTipoCarga(), nodo.getZ());
        }
        return nodo.getNombre() + " (" + nodo.getValorCarga() + ") (" + nodo.getTipoCarga() + ")";
    }
}