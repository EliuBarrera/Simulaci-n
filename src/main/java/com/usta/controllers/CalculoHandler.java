package com.usta.controllers;

import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoCalculo;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.CoulombCalculator;
import com.usta.utils.UnidadDistancia;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Gestiona el ciclo de vida del cálculo de Coulomb (iniciar, cancelar, ejecutar).
 * Notifica al controlador principal mediante callbacks para que actualice las flechas
 * y el último resultado.
 */
public class CalculoHandler {

    private final Grafo  grafo;
    private final Canvas canvasPlano;

    // Campos FXML — se asignan desde LeyCoulombController
    public ComboBox<String>       particulaOrigenComboBox;
    public ComboBox<UnidadDistancia> unidadDistanciaComboBox;
    public RadioButton            fuerzasIndividualesRadio;
    public Button                 calcularButton;
    public Button                 cancelarButton;
    public Label                  resultadoFuerzaLabel;
    public Label                  resultadoCampoLabel;

    // Callbacks al controlador principal
    private Consumer<ResultadoCalculo> onResultado;
    private Runnable                   onCancelar;

    // Estado del hilo
    private volatile boolean estaCalculando = false;
    private volatile int     calculoVersion = 0;
    private Thread           hiloCalculo    = null;

    public CalculoHandler(Grafo grafo, Canvas canvasPlano) {
        this.grafo       = grafo;
        this.canvasPlano = canvasPlano;
    }

    /**
     * Registra el callback que recibe el resultado calculado (para dibujar flechas, etc.).
     */
    public void setOnResultado(Consumer<ResultadoCalculo> callback) {
        this.onResultado = callback;
    }

    /**
     * Registra el callback que se invoca al cancelar (para limpiar flechas en la UI).
     */
    public void setOnCancelar(Runnable callback) {
        this.onCancelar = callback;
    }

    /** @return true si hay un cálculo activo en este momento. */
    public boolean estaCalculando() {
        return estaCalculando;
    }

    /** Inicia el hilo de cálculo continuo (una actualización cada ~16 ms). */
    public void iniciar(boolean modo3D, UnidadDistancia unidadActual) {
        String nombre = particulaOrigenComboBox.getValue();
        if (nombre == null) {
            mostrarAlertaError("Error", "Seleccione una partícula.");
            return;
        }
        if (buscarNodo(nombre) == null) {
            mostrarAlertaError("Error", "La partícula no existe.");
            return;
        }

        estaCalculando = true;
        calcularButton.setDisable(true);
        cancelarButton.setDisable(false);

        hiloCalculo = new Thread(() -> {
            int miVersion = calculoVersion;
            while (estaCalculando && calculoVersion == miVersion) {
                try {
                    final int v = miVersion;
                    Platform.runLater(() -> ejecutar(v, modo3D, unidadDistanciaComboBox.getValue()));
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        hiloCalculo.setDaemon(true);
        hiloCalculo.start();
    }

    /** Detiene el hilo y limpia el estado visual mediante el callback onCancelar. */
    public void cancelar() {
        estaCalculando = false;
        calculoVersion++;

        if (hiloCalculo != null) {
            hiloCalculo.interrupt();
            try { hiloCalculo.join(500); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            hiloCalculo = null;
        }

        Platform.runLater(() -> {
            if (onCancelar != null) onCancelar.run();
            resultadoFuerzaLabel.setText(" ");
            resultadoCampoLabel.setText(" ");
            calcularButton.setDisable(false);
            cancelarButton.setDisable(true);
        });
    }

    /**
     * Ejecuta un ciclo de cálculo y notifica el resultado.
     * Si la versión ya no coincide (fue cancelado), descarta silenciosamente.
     */
    public void ejecutar(int version, boolean modo3D, UnidadDistancia unidadActual) {
        if (version != calculoVersion) return;

        String nombre = particulaOrigenComboBox.getValue();
        if (nombre == null) return;

        Nodo origen = buscarNodo(nombre);
        if (origen == null) {
            cancelar();
            particulaOrigenComboBox.getSelectionModel().clearSelection();
            return;
        }

        CoordenadasTransformador transformador =
            new CoordenadasTransformador(canvasPlano.getHeight(), unidadActual);
        CoulombCalculator calculator = new CoulombCalculator(grafo, transformador, modo3D);

        ResultadoCalculo resultado = calculator.calcular(origen);
        if (version != calculoVersion) return;

        if (resultado == null) {
            resultadoFuerzaLabel.setText("Sin conexiones");
            resultadoCampoLabel.setText(" ");
            if (onCancelar != null) onCancelar.run(); // limpiar flechas
            return;
        }

        if (onResultado != null) onResultado.accept(resultado);

        resultadoFuerzaLabel.setText(formatearNumero(resultado.getFuerzaTotal()) + " N");
        resultadoCampoLabel.setText(formatearNumero(resultado.getCampoElectrico()) + " N/C");
    }

    /** @return la versión actual del cálculo (se incrementa al cancelar). */
    public int getCalculoVersion() {
        return calculoVersion;
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private Nodo buscarNodo(String nombre) {
        return grafo.getNodos().stream()
            .filter(n -> n.getNombre().equals(nombre))
            .findFirst().orElse(null);
    }

    private String formatearNumero(double valor) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.000000", sym).format(valor);
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        javafx.scene.control.Alert a =
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(" "); a.setContentText(mensaje);
        a.showAndWait();
    }
}