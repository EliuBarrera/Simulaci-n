package com.usta.controllers;

import com.usta.models.ResultadoCalculo;
import com.usta.models.ResultadoFuerza;
import com.usta.utils.PdfGenerator;
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
 * Genera el texto detallado paso a paso del último cálculo y lo exporta como PDF.
 */
public class DetallesPdfHandler {

    private final Pane grafoPane;

    // Campo FXML — se asigna desde LeyCoulombController
    public TextArea calculosDetalladosTextArea;

    public DetallesPdfHandler(Pane grafoPane) {
        this.grafoPane = grafoPane;
    }

    /**
     * Rellena el TextArea con el desarrollo completo del cálculo.
     *
     * @param res           resultado obtenido por CoulombCalculator
     * @param unidadActual  unidad activa para mostrar el símbolo correcto
     */
    public void mostrarTextoDetallado(ResultadoCalculo res, UnidadDistancia unidadActual) {
        if (calculosDetalladosTextArea == null) return;
        String simb = unidadActual.getSimbolo();
        boolean is3D = res.isEs3D();
        StringBuilder sb = new StringBuilder();

        sb.append("================================================================\n");
        sb.append("   CALCULO DE FUERZA ELECTRICA Y CAMPO ELECTRICO\n");
        sb.append("   Ley de Coulomb  |  Sistema USTA");
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
        sb.append(String.format("  Carga  q0             : %.2f uC = %.4e C  (%s)\n",
            res.getParticulaOrigen().getValorCarga(),
            res.getParticulaOrigen().getValorCarga() * 1e-6,
            res.getParticulaOrigen().getTipoCarga()));
        sb.append("  Constante  k          : 8.99 x 10^9 N*m^2/C^2\n");
        sb.append(String.format("  Unidad de distancia   : %s (%s)\n", unidadActual.name(), simb));
        if (is3D) sb.append("  Modo de calculo       : TRIDIMENSIONAL (X, Y, Z)\n");
        sb.append("\n");

        sb.append("CALCULO DE FUERZAS INDIVIDUALES\n");
        sb.append("----------------------------------------------------------------\n");

        int n = 1;
        for (ResultadoFuerza rf : res.getFuerzasIndividuales()) {
            sb.append(String.format("\n%d) Fuerza ejercida por %s sobre %s\n",
                n++, rf.getParticulaCausante().getNombre(), res.getParticulaOrigen().getNombre()));
            if (is3D) {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s, %.4f %s)\n",
                    rf.getParticulaCausante().getNombre(), rf.getX1(), simb, rf.getY1(), simb, rf.getZ1(), simb));
            } else {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s)\n",
                    rf.getParticulaCausante().getNombre(), rf.getX1(), simb, rf.getY1(), simb));
            }
            sb.append(String.format("   Carga  q1  : %.2f uC = %.4e C  (%s)\n",
                rf.getParticulaCausante().getValorCarga(),
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getTipoCarga()));
            sb.append(String.format("   Tipo       : %s\n", rf.getTipoInteraccion()));

            sb.append("\n   Calculo de distancia:\n");
            sb.append(String.format("     Dx = %.4f - %.4f = %.4f %s\n", rf.getX1(), rf.getX0(), rf.getDx(), simb));
            sb.append(String.format("     Dy = %.4f - %.4f = %.4f %s\n", rf.getY1(), rf.getY0(), rf.getDy(), simb));
            if (is3D) {
                sb.append(String.format("     Dz = %.4f - %.4f = %.4f %s\n", rf.getZ1(), rf.getZ0(), rf.getDz(), simb));
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2 + (%.4f)^2)\n",
                    rf.getDx(), rf.getDy(), rf.getDz()));
            } else {
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2)\n", rf.getDx(), rf.getDy()));
            }
            sb.append(String.format("     r  = %.4f %s  =  %.4e m\n",
                rf.getDistanciaEnUnidad(), simb, rf.getDistanciaEnMetros()));

            sb.append("\n   Angulo:\n");
            sb.append(String.format("     theta = arctan(%.4f / %.4f) = %.2f°\n",
                rf.getDy(), rf.getDx(), rf.getAnguloDeg()));
            if (is3D)
                sb.append(String.format("     phi (elevacion) = %.2f°\n", rf.getAnguloElevacionDeg()));

            sb.append("\n   Ley de Coulomb:\n");
            sb.append(String.format("     F = (8.99e9) * |%.4e * %.4e| / (%.4e)^2\n",
                res.getParticulaOrigen().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getDistanciaEnMetros()));
            sb.append(String.format("     F = %.6e N\n", rf.getMagnitud()));

            sb.append("\n   Componentes:\n");
            sb.append(String.format("     Fx = %.6e N\n", rf.getFx()));
            sb.append(String.format("     Fy = %.6e N\n", rf.getFy()));
            if (is3D) sb.append(String.format("     Fz = %.6e N\n", rf.getFz()));
            sb.append(rf.isEsRepulsion()
                ? "     (Repulsion)\n" : "     (Atraccion)\n");
        }

        sb.append("\n================================================================\n");
        sb.append("SUMA VECTORIAL DE FUERZAS\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  SFx = %.6e N\n", res.getFuerzaTotalX()));
        sb.append(String.format("  SFy = %.6e N\n", res.getFuerzaTotalY()));
        if (is3D) sb.append(String.format("  SFz = %.6e N\n", res.getFuerzaTotalZ()));
        sb.append(String.format("  |F| = %.6e N\n", res.getFuerzaTotal()));
        sb.append(String.format("  theta = %.2f°\n", res.getAnguloResultante()));
        if (is3D) sb.append(String.format("  phi   = %.2f°\n", res.getAnguloElevacionResultante()));

        sb.append("\n================================================================\n");
        sb.append("CAMPO ELECTRICO\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  E = %.6e / %.4e = %.6e N/C\n",
            res.getFuerzaTotal(),
            Math.abs(res.getParticulaOrigen().getValorCarga() * 1e-6),
            res.getCampoElectrico()));
        sb.append("\n================================================================\n");
        sb.append("                      FIN DEL CALCULO\n");
        sb.append("================================================================\n");

        calculosDetalladosTextArea.setText(sb.toString());
    }

    /**
     * Abre un diálogo de guardado y genera el PDF con el último resultado.
     *
     * @param ultimoResultado  resultado a exportar
     * @param unidadActual     unidad activa
     */
    public void generarPDF(ResultadoCalculo ultimoResultado, UnidadDistancia unidadActual) {
        if (calculosDetalladosTextArea == null ||
            calculosDetalladosTextArea.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "Genere los cálculos detallados primero."); return;
        }
        if (ultimoResultado == null) {
            mostrarAlerta("Error", "Realice un cálculo antes de generar el PDF."); return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF");
        fc.setInitialFileName("Calculos_Electricos.pdf");
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

            new PdfGenerator().generar(ruta, ultimoResultado, unidadActual, fecha);

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