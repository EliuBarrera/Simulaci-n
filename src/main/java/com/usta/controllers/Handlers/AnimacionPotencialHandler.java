package com.usta.controllers.Handlers;

import com.usta.controllers.AnimacionPotencialController;
import com.usta.models.Nodo;
import com.usta.models.ResultadoPotencial;
import com.usta.utils.UnidadDistancia;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.Map;

/**
 * Maneja la pestaña de animación paso a paso del cálculo de Potencial Eléctrico.
 */
public class AnimacionPotencialHandler {

    private final AnimacionPotencialController animController = new AnimacionPotencialController();

    public Tab animacionTab;
    public Label pasoIndicadorLabel;
    public Label pasoDescripcionLabel;
    public Label barraProgresoLabel;
    public Button btnAnteriorPaso;
    public Button btnSiguientePaso;
    public Button btnReiniciarAnimacion;
    public Button btnDetenerAnimacion;

    public void onTabSeleccionada(ResultadoPotencial ultimoResultado,
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

    public void siguiente(ResultadoPotencial ultimoResultado) {
        if (!animController.hayResultado()) return;
        animController.siguiente();
        actualizarBotones();
        actualizarInfoPaso(ultimoResultado);
    }

    public void anterior(ResultadoPotencial ultimoResultado) {
        if (!animController.hayResultado()) return;
        animController.anterior();
        actualizarBotones();
        actualizarInfoPaso(ultimoResultado);
    }

    public void reiniciar(ResultadoPotencial ultimoResultado,
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

    private void actualizarBotones() {
        btnAnteriorPaso.setDisable(animController.getPasoActual() == 0);
        btnSiguientePaso.setDisable(
                animController.getPasoActual() >= animController.getTotalPasos() - 1);
    }

    private void actualizarInfoPaso(ResultadoPotencial ultimoResultado) {
        int actual = animController.getPasoActual() + 1;
        int total  = animController.getTotalPasos();
        int n      = ultimoResultado.getPotencialesIndividuales().size();

        pasoIndicadorLabel.setText("Paso " + actual + " de " + total);
        pasoDescripcionLabel.setText(obtenerDescripcionPaso(animController.getPasoActual(), n, ultimoResultado));

        int llenos = (int) Math.round(((double) actual / total) * 10);
        StringBuilder barra = new StringBuilder();
        for (int i = 0; i < 10; i++) barra.append(i < llenos ? "▓" : "░");
        barraProgresoLabel.setText(barra.toString());
    }

    private String obtenerDescripcionPaso(int paso, int n, ResultadoPotencial ultimoResultado) {
        if (paso == 0)
            return "Partícula de análisis\ndestacada.\nObserve su posición y carga.";

        if (paso >= 1 && paso <= n * 2) {
            int idx    = (paso - 1) / 2;
            int subPas = (paso - 1) % 2;
            String causante = ultimoResultado.getPotencialesIndividuales()
                    .get(idx).getParticulaFuente().getNombre();
            return switch (subPas) {
                case 0 -> "Vector r:\nDistancia entre el origen\ny " + causante + ".";
                case 1 -> "Fórmula V:\nCálculo del Potencial (V)\ndebido a " + causante + ".";
                default -> "";
            };
        }
        if (paso == n * 2 + 1) return "Superposición:\nSuma escalar de todos\nlos potenciales (V).";
        if (paso == n * 2 + 2) return "Energía Potencial:\nU = q₀ · V_total\nen la partícula origen.";
        return "";
    }
}
