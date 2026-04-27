package com.usta.controllers.Handlers;

import com.usta.models.ResultadoPotencial;
import com.usta.models.ResultadoPotencialIndividual;
import com.usta.utils.PdfPotencialGenerator;
import com.usta.utils.UnidadDistancia;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera el texto detallado paso a paso del último cálculo de Potencial Eléctrico
 * y lo exporta como PDF.
 */
public class DetallesPdfPotencialHandler {

    private final Pane grafoPane;

    public TextArea calculosDetalladosTextArea;

    public DetallesPdfPotencialHandler(Pane grafoPane) {
        this.grafoPane = grafoPane;
    }

    public void mostrarTextoDetallado(ResultadoPotencial res, UnidadDistancia unidadActual) {
        if (calculosDetalladosTextArea == null) return;
        String simb = unidadActual.getSimbolo();
        boolean is3D = res.isEs3D();
        StringBuilder sb = new StringBuilder();

        sb.append("================================================================\n");
        sb.append("   CALCULO DE POTENCIAL Y ENERGIA POTENCIAL ELECTRICA\n");
        sb.append("   Escalares  |  Sistema USTA");
        if (is3D) sb.append("  |  MODO 3D");
        sb.append("\n================================================================\n\n");

        sb.append("DATOS INICIALES\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  Particula de analisis : %s\n", res.getParticulaOrigen().getNombre()));
        if (is3D) {
            sb.append(String.format("  Posicion              : (%.4f %s, %.4f %s, %.4f %s)\n",
                    res.getX0(), simb, res.getY0(), simb, res.getZ0(), simb));
        } else {
            sb.append(String.format("  Posicion              : (%.4f %s, %.4f %s)\n",
                    res.getX0(), simb, res.getY0(), simb));
        }
        
        double q0_abs = Math.abs(res.getParticulaOrigen().getValorCarga());
        double q0 = "-".equals(res.getParticulaOrigen().getTipoCarga()) ? -q0_abs : q0_abs;
        
        sb.append(String.format("  Carga  q0             : %.2f uC = %.4e C\n", q0, q0 * 1e-6));
        sb.append("  Constante  k          : 8.99 x 10^9 N*m^2/C^2\n");
        sb.append(String.format("  Unidad de distancia   : %s (%s)\n", unidadActual.name(), simb));
        if (is3D) sb.append("  Modo de calculo       : TRIDIMENSIONAL (X, Y, Z)\n");
        sb.append("\n");

        sb.append("CALCULO DE POTENCIALES INDIVIDUALES (V)\n");
        sb.append("----------------------------------------------------------------\n");

        int n = 1;
        for (ResultadoPotencialIndividual rpi : res.getPotencialesIndividuales()) {
            sb.append(String.format("\n%d) Potencial en q0 debido a %s\n",
                    n++, rpi.getParticulaFuente().getNombre()));
            if (is3D) {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s, %.4f %s)\n",
                        rpi.getParticulaFuente().getNombre(), rpi.getX1(), simb, rpi.getY1(), simb, rpi.getZ1(), simb));
            } else {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s)\n",
                        rpi.getParticulaFuente().getNombre(), rpi.getX1(), simb, rpi.getY1(), simb));
            }
            
            double q1_abs = Math.abs(rpi.getParticulaFuente().getValorCarga());
            double q1 = "-".equals(rpi.getParticulaFuente().getTipoCarga()) ? -q1_abs : q1_abs;

            sb.append(String.format("   Carga  q1  : %.2f uC = %.4e C\n", q1, q1 * 1e-6));

            sb.append("\n   Calculo de distancia r:\n");
            double dx = rpi.getX1() - rpi.getX0();
            double dy = rpi.getY1() - rpi.getY0();
            sb.append(String.format("     Dx = %.4f - %.4f = %.4f %s\n", rpi.getX1(), rpi.getX0(), dx, simb));
            sb.append(String.format("     Dy = %.4f - %.4f = %.4f %s\n", rpi.getY1(), rpi.getY0(), dy, simb));
            if (is3D) {
                double dz = rpi.getZ1() - rpi.getZ0();
                sb.append(String.format("     Dz = %.4f - %.4f = %.4f %s\n", rpi.getZ1(), rpi.getZ0(), dz, simb));
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2 + (%.4f)^2)\n", dx, dy, dz));
            } else {
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2)\n", dx, dy));
            }
            sb.append(String.format("     r  = %.4f %s  =  %.4e m\n",
                    rpi.getDistanciaUnidades(), simb, rpi.getDistanciaMetros()));

            sb.append("\n   Potencial V = k * q / r:\n");
            sb.append(String.format("     V = (8.99e9) * (%.4e) / (%.4e)\n",
                    q1 * 1e-6, rpi.getDistanciaMetros()));
            sb.append(String.format("     V = %.6e Voltios (V)\n", rpi.getPotencialV()));
        }

        sb.append("\n================================================================\n");
        sb.append("SUMA ESCALAR DE POTENCIALES\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  V_total = %.6e V\n", res.getPotencialTotalV()));

        sb.append("\n================================================================\n");
        sb.append("ENERGIA POTENCIAL ELECTRICA (U)\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  U = q0 * V_total\n"));
        sb.append(String.format("  U = (%.4e C) * (%.6e V)\n", q0 * 1e-6, res.getPotencialTotalV()));
        sb.append(String.format("  U = %.6e Joules (J)\n", res.getEnergiaTotalU()));
        
        sb.append("\n================================================================\n");
        sb.append("                      FIN DEL CALCULO\n");
        sb.append("================================================================\n");

        calculosDetalladosTextArea.setText(sb.toString());
    }

    public void generarPDF(ResultadoPotencial ultimoResultado, UnidadDistancia unidadActual) {
        if (calculosDetalladosTextArea == null ||
                calculosDetalladosTextArea.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "Genere los cálculos detallados primero."); return;
        }
        if (ultimoResultado == null) {
            mostrarAlerta("Error", "Realice un cálculo antes de generar el PDF."); return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF");
        fc.setInitialFileName("Calculos_Potencial.pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        File dir = new File(System.getProperty("user.home") + File.separator + "Desktop");
        if (!dir.exists()) dir = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(dir);

        Stage stage = (Stage) grafoPane.getScene().getWindow();
        File destino = fc.showSaveDialog(stage);
        if (destino == null) return;

        try {
            String ruta = destino.getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";
            String fecha = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            new PdfPotencialGenerator().generar(ruta, ultimoResultado, unidadActual, fecha);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("PDF Generado"); ok.setHeaderText("Éxito");
            ok.setContentText("Guardado en:\n" + ruta);
            ok.showAndWait();

        } catch (Exception e) {
            mostrarAlerta("Error", "Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(" "); a.setContentText(mensaje);
        a.showAndWait();
    }
}
