package com.usta.utils;

import com.usta.models.ResultadoGauss;

/**
 * Construye el texto detallado del procedimiento de la Ley de Gauss
 * a partir de un {@link ResultadoGauss}.
 *
 * Separado del controlador para mantener la lógica de presentación
 * independiente del estado de la UI.
 */
public class ProcedimientoBuilder {

    private ProcedimientoBuilder() { /* utilidad estática */ }

    public static String construir(ResultadoGauss res) {
        StringBuilder sb = new StringBuilder();

        encabezado(sb);
        seccionDatos(sb, res);
        seccionQenc(sb, res);
        seccionLeyGauss(sb, res);
        seccionCampo(sb, res);
        seccionInterpretacion(sb, res);
        pie(sb);

        return sb.toString();
    }

    // =========================================================================
    // SECCIONES
    // =========================================================================

    private static void encabezado(StringBuilder sb) {
        sb.append("══════════════════════════════════════════════════\n");
        sb.append("   LEY DE GAUSS — PROCEDIMIENTO DETALLADO\n");
        sb.append("══════════════════════════════════════════════════\n\n");
    }

    private static void seccionDatos(StringBuilder sb, ResultadoGauss res) {
        sb.append("DATOS\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append(String.format("  Figura cargada      : %s (%s)%n",
            res.getFiguraCargada().getTipo(),
            res.getFiguraCargada().getNombre()));
        sb.append(String.format("  Carga total Q       : %.4f µC = %.4e C  (%s)%n",
            res.getFiguraCargada().getCargaTotal(),
            res.getCargaTotalCoulombs(),
            res.getFiguraCargada().getSigno()));
        sb.append(String.format("  Superficie Gaussiana: %s%n",
            res.getSuperficieGaussiana().getTipo()));
        sb.append(String.format("  ε₀                  : %.4e C²/(N·m²)%n%n",
            ResultadoGauss.getEpsilon0()));
    }

    private static void seccionQenc(StringBuilder sb, ResultadoGauss res) {
        sb.append("CÁLCULO DE Q_enc\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append(String.format("  Fracción encerrada  : %.2f %%%n",
            res.getFraccionEncerrada() * 100));
        sb.append("  Q_enc = Q_total × fracción\n");
        sb.append(String.format("  Q_enc = %.4e × %.4f%n",
            res.getCargaTotalCoulombs(), res.getFraccionEncerrada()));
        sb.append(String.format("  Q_enc = %.6e C%n%n",
            res.getCargaEncerradaCoulombs()));
    }

    private static void seccionLeyGauss(StringBuilder sb, ResultadoGauss res) {
        sb.append("LEY DE GAUSS\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append("  ∮ E·dA = Q_enc / ε₀\n");
        sb.append(String.format("  Φ = %.6e / %.4e%n",
            res.getCargaEncerradaCoulombs(), ResultadoGauss.getEpsilon0()));
        sb.append(String.format("  Φ = %.6e N·m²/C%n%n",
            res.getFlujoElectrico()));
    }

    private static void seccionCampo(StringBuilder sb, ResultadoGauss res) {
        sb.append("CAMPO ELÉCTRICO PROMEDIO\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append(String.format("  Perímetro superficie: %.4f m%n",
            res.getPerimetroSuperficie()));
        sb.append("  E = Φ / Perímetro\n");
        sb.append(String.format("  E = %.6e / %.4f%n",
            res.getFlujoElectrico(), res.getPerimetroSuperficie()));
        sb.append(String.format("  E = %.6e N/C%n%n",
            res.getCampoPromedio()));
    }

    private static void seccionInterpretacion(StringBuilder sb, ResultadoGauss res) {
        sb.append("INTERPRETACIÓN\n");
        sb.append("──────────────────────────────────────────────────\n");

        if (res.isFiguraDentro()) {
            sb.append("  La figura cargada está completamente dentro\n");
            sb.append("  de la superficie gaussiana.\n");
            sb.append("  → Q_enc = Q_total (toda la carga contribuye).\n");
        } else if (res.getFraccionEncerrada() < 0.001) {
            sb.append("  La figura cargada está completamente fuera\n");
            sb.append("  de la superficie gaussiana.\n");
            sb.append("  → Q_enc ≈ 0, por lo tanto Φ ≈ 0.\n");
        } else {
            sb.append("  La figura cargada está parcialmente dentro.\n");
            sb.append(String.format("  Solo el %.1f %% de la carga contribuye al flujo.%n",
                res.getFraccionEncerrada() * 100));
        }
    }

    private static void pie(StringBuilder sb) {
        sb.append("\n══════════════════════════════════════════════════\n");
        sb.append("                   FIN DEL CÁLCULO\n");
        sb.append("══════════════════════════════════════════════════\n");
    }
}