package com.usta.controllers.Handlers;

import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoPotencial;
import com.usta.utils.CoordenadasTransformador;
import com.usta.utils.PotencialCalculator;
import com.usta.utils.UnidadDistancia;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Gestiona el ciclo de vida del cálculo del Potencial Eléctrico y Energía Potencial.
 * Notifica al controlador principal mediante callbacks para que actualice la UI.
 */
public class CalculoPotencialHandler {

    private final Grafo grafo;
    private final Canvas canvasPlano;

    // Campos FXML — se asignan desde PotencialElectricoController
    public ComboBox<String> particulaOrigenComboBox;
    public ComboBox<UnidadDistancia> unidadDistanciaComboBox;
    public Button calcularButton;
    public Button cancelarButton;
    public Label resultadoPotencialLabel;
    public Label resultadoEnergiaLabel;

    // Callbacks al controlador principal
    private Consumer<ResultadoPotencial> onResultado;
    private Runnable onCancelar;

    // Estado del hilo
    private volatile boolean estaCalculando = false;
    private volatile int calculoVersion = 0;
    private Thread hiloCalculo = null;

    public CalculoPotencialHandler(Grafo grafo, Canvas canvasPlano) {
        this.grafo = grafo;
        this.canvasPlano = canvasPlano;
    }

    public void setOnResultado(Consumer<ResultadoPotencial> callback) {
        this.onResultado = callback;
    }

    public void setOnCancelar(Runnable callback) {
        this.onCancelar = callback;
    }

    public boolean estaCalculando() {
        return estaCalculando;
    }

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
            resultadoPotencialLabel.setText(" ");
            resultadoEnergiaLabel.setText(" ");
            calcularButton.setDisable(false);
            cancelarButton.setDisable(true);
        });
    }

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
        PotencialCalculator calculator = new PotencialCalculator(grafo, transformador, modo3D);

        ResultadoPotencial resultado = calculator.calcular(origen);
        if (version != calculoVersion) return;

        if (resultado == null) {
            resultadoPotencialLabel.setText("Sin conexiones");
            resultadoEnergiaLabel.setText(" ");
            if (onCancelar != null) onCancelar.run();
            return;
        }

        if (onResultado != null) onResultado.accept(resultado);

        resultadoPotencialLabel.setText(formatearNumero(resultado.getPotencialTotalV()) + " V");
        resultadoEnergiaLabel.setText(formatearNumero(resultado.getEnergiaTotalU()) + " J");
    }

    public int getCalculoVersion() {
        return calculoVersion;
    }

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
