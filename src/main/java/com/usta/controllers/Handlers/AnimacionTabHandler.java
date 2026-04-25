package com.usta.controllers.Handlers;

import com.usta.controllers.AnimacionCoulombController;
import com.usta.models.Nodo;
import com.usta.models.ResultadoCalculo;
import com.usta.utils.UnidadDistancia;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.Map;

/**
 * Maneja la pestaña de animación paso a paso del cálculo de Coulomb.
 */
public class AnimacionTabHandler {

    private final AnimacionCoulombController animController = new AnimacionCoulombController();

    // Campos FXML — se asignan desde LeyCoulombController
    public Tab    animacionTab;
    public Label  pasoIndicadorLabel;
    public Label  pasoDescripcionLabel;
    public Label  barraProgresoLabel;
    public Button btnAnteriorPaso;
    public Button btnSiguientePaso;
    public Button btnReiniciarAnimacion;
    public Button btnDetenerAnimacion;

    /**
     * Llamado cuando se selecciona o deselecciona la pestaña de animación.
     *
     * @param ultimoResultado  resultado activo (puede ser null si no hay cálculo)
     * @param unidadActual     unidad de distancia activa
     * @param grafoPane        panel principal donde se dibujan los nodos
     * @param nodoCirculos     mapa nodo → círculo visual
     * @param onEntrar         callback al entrar a la pestaña (para cancelar cálculo activo, limpiar flechas)
     */
    public void onTabSeleccionada(ResultadoCalculo ultimoResultado,
                                  UnidadDistancia unidadActual,
                                  Pane grafoPane,
                                  Map<Nodo, Circle> nodoCirculos,
                                  Runnable onEntrar) {
        boolean activa = animacionTab.isSelected();

        if (activa) {
            if (onEntrar != null) onEntrar.run();

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

            animController.inicializar(ultimoResultado, unidadActual, grafoPane, nodoCirculos);
            btnAnteriorPaso.setDisable(true);
            btnSiguientePaso.setDisable(animController.getTotalPasos() <= 1);
            btnReiniciarAnimacion.setDisable(false);
            btnDetenerAnimacion.setDisable(false);
            actualizarInfoPaso(ultimoResultado);

        } else {
            animController.restaurar();
        }
    }

    /** Avanza al siguiente paso y actualiza la UI. */
    public void siguiente(ResultadoCalculo ultimoResultado) {
        if (!animController.hayResultado()) return;
        animController.siguiente();
        actualizarBotones();
        actualizarInfoPaso(ultimoResultado);
    }

    /** Retrocede al paso anterior y actualiza la UI. */
    public void anterior(ResultadoCalculo ultimoResultado) {
        if (!animController.hayResultado()) return;
        animController.anterior();
        actualizarBotones();
        actualizarInfoPaso(ultimoResultado);
    }

    /**
     * Reinicia la animación desde el paso 0.
     *
     * @param ultimoResultado  resultado a reanimar
     * @param unidadActual     unidad activa
     * @param grafoPane        panel de dibujo
     * @param nodoCirculos     mapa nodo → círculo
     */
    public void reiniciar(ResultadoCalculo ultimoResultado,
                          UnidadDistancia unidadActual,
                          Pane grafoPane,
                          Map<Nodo, Circle> nodoCirculos) {
        if (ultimoResultado == null) return;
        animController.inicializar(ultimoResultado, unidadActual, grafoPane, nodoCirculos);
        btnAnteriorPaso.setDisable(true);
        btnSiguientePaso.setDisable(animController.getTotalPasos() <= 1);
        btnDetenerAnimacion.setDisable(false);
        actualizarInfoPaso(ultimoResultado);
    }

    /** Detiene la animación y restaura el estado visual de los nodos. */
    public void detener() {
        animController.restaurar();
        animController.restaurarCausantesANeutro();
        pasoIndicadorLabel.setText("Animación detenida.");
        pasoDescripcionLabel.setText("");
        barraProgresoLabel.setText("──────────");
        btnAnteriorPaso.setDisable(true);
        btnSiguientePaso.setDisable(true);
        btnReiniciarAnimacion.setDisable(true);
        btnDetenerAnimacion.setDisable(true);
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private void actualizarBotones() {
        btnAnteriorPaso.setDisable(animController.getPasoActual() == 0);
        btnSiguientePaso.setDisable(
            animController.getPasoActual() >= animController.getTotalPasos() - 1);
    }

    private void actualizarInfoPaso(ResultadoCalculo ultimoResultado) {
        int actual = animController.getPasoActual() + 1;
        int total  = animController.getTotalPasos();
        int n      = ultimoResultado.getFuerzasIndividuales().size();

        pasoIndicadorLabel.setText("Paso " + actual + " de " + total);
        pasoDescripcionLabel.setText(obtenerDescripcionPaso(animController.getPasoActual(), n, ultimoResultado));

        int llenos = (int) Math.round(((double) actual / total) * 10);
        StringBuilder barra = new StringBuilder();
        for (int i = 0; i < 10; i++) barra.append(i < llenos ? "▓" : "░");
        barraProgresoLabel.setText(barra.toString());
    }

    private String obtenerDescripcionPaso(int paso, int n, ResultadoCalculo ultimoResultado) {
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
        if (paso == n * 3 + 1) return "Suma vectorial:\nResultante de todas\nlas fuerzas (ΣF).";
        if (paso == n * 3 + 2) return "Campo eléctrico:\nE = F / |q₀|\nen la partícula origen.";
        return "";
    }
}