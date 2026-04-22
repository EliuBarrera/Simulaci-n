package com.usta.utils;

import com.usta.models.ResultadoCalculo;
import com.usta.models.ResultadoFuerza;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera un archivo PDF con el procedimiento detallado del cálculo
 * de fuerzas y campo eléctrico según la Ley de Coulomb.
 *
 * Implementa la especificación PDF 1.4 de forma manual (sin librerías externas).
 * Tamaño carta: 612 x 792 puntos.
 *
 * Soporta cálculos tanto 2D como 3D.
 */
public class PdfGenerator {

    // ── Constantes de layout ──────────────────────────────────────────────────
    private static final int PAGE_W        = 612;
    private static final int PAGE_H        = 792;
    private static final int MARGIN        = 50;
    private static final int FS_HEAD       = 10;   // font size título
    private static final int FS_BODY       = 8;    // font size cuerpo
    private static final int LH            = 12;   // line height en puntos
    private static final int TEXT_W        = PAGE_W - MARGIN * 2;   // 512 pts
    // Courier a FS=8: ~5 pts/char (conservador para evitar desbordamiento)
    private static final int CHARS_PER_LINE = TEXT_W / 5;

    // ── Punto de entrada principal ────────────────────────────────────────────

    /**
     * Genera el PDF a partir de un {@link ResultadoCalculo} y lo guarda en disco.
     *
     * @param rutaSalida  Ruta absoluta del archivo de salida (incluyendo .pdf).
     * @param resultado   Resultado del cálculo de Coulomb.
     * @param unidad      Unidad de distancia utilizada en el cálculo.
     * @param fecha       Fecha y hora de generación (cadena formateada).
     */
    public void generar(String rutaSalida,
                        ResultadoCalculo resultado,
                        UnidadDistancia unidad,
                        String fecha) throws Exception {
        String contenido = construirTexto(resultado, unidad);
        generarPDFDesdeTexto(rutaSalida, contenido, fecha);
    }

    // ── Construcción del texto del procedimiento ──────────────────────────────

    private String construirTexto(ResultadoCalculo res, UnidadDistancia unidad) {
        String simb = unidad.getSimbolo();
        boolean is3D = res.isEs3D();
        StringBuilder sb = new StringBuilder();

        sb.append("================================================================\n");
        sb.append("   CALCULO DE FUERZA ELECTRICA Y CAMPO ELECTRICO\n");
        sb.append("   Ley de Coulomb  |  Sistema USTA");
        if (is3D) sb.append("  |  MODO 3D");
        sb.append("\n");
        sb.append("================================================================\n\n");

        // ── Datos iniciales ──────────────────────────────────────────────────
        sb.append("DATOS INICIALES\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format("  Particula de analisis : %s\n",
            res.getParticulaOrigen().getNombre()));

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
        sb.append(String.format("  Unidad de distancia   : %s (%s)\n",
            unidad.name(), simb));
        if (is3D) sb.append("  Modo de calculo       : TRIDIMENSIONAL (X, Y, Z)\n");
        sb.append("\n");

        // ── Fuerzas individuales ─────────────────────────────────────────────
        sb.append("CALCULO DE FUERZAS INDIVIDUALES\n");
        sb.append("----------------------------------------------------------------\n");

        int n = 1;
        for (ResultadoFuerza rf : res.getFuerzasIndividuales()) {
            sb.append(String.format("\n%d) Fuerza ejercida por %s sobre %s\n",
                n++,
                rf.getParticulaCausante().getNombre(),
                res.getParticulaOrigen().getNombre()));

            if (is3D) {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s, %.4f %s)\n",
                    rf.getParticulaCausante().getNombre(),
                    rf.getX1(), simb, rf.getY1(), simb, rf.getZ1(), simb));
            } else {
                sb.append(String.format("   Posicion %s : (%.4f %s, %.4f %s)\n",
                    rf.getParticulaCausante().getNombre(),
                    rf.getX1(), simb, rf.getY1(), simb));
            }

            sb.append(String.format("   Carga  q1  : %.2f uC = %.4e C  (%s)\n",
                rf.getParticulaCausante().getValorCarga(),
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getTipoCarga()));
            sb.append(String.format("   Tipo       : %s\n", rf.getTipoInteraccion()));

            sb.append("\n   Calculo de distancia:\n");
            sb.append(String.format("     Dx = x1 - x0 = %.4f - %.4f = %.4f %s\n",
                rf.getX1(), rf.getX0(), rf.getDx(), simb));
            sb.append(String.format("     Dy = y1 - y0 = %.4f - %.4f = %.4f %s\n",
                rf.getY1(), rf.getY0(), rf.getDy(), simb));

            if (is3D) {
                sb.append(String.format("     Dz = z1 - z0 = %.4f - %.4f = %.4f %s\n",
                    rf.getZ1(), rf.getZ0(), rf.getDz(), simb));
                sb.append("     r  = sqrt(Dx^2 + Dy^2 + Dz^2)\n");
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2 + (%.4f)^2)\n",
                    rf.getDx(), rf.getDy(), rf.getDz()));
            } else {
                sb.append("     r  = sqrt(Dx^2 + Dy^2)\n");
                sb.append(String.format("     r  = sqrt((%.4f)^2 + (%.4f)^2)\n",
                    rf.getDx(), rf.getDy()));
            }
            sb.append(String.format("     r  = %.4f %s\n", rf.getDistanciaEnUnidad(), simb));
            sb.append(String.format("     r  = %.4e m\n", rf.getDistanciaEnMetros()));

            sb.append("\n   Angulo del vector r (eje X+, sentido antihorario):\n");
            sb.append("     theta_r = arctan(Dy / Dx)\n");
            sb.append(String.format("     theta_r = arctan(%.4f / %.4f) = %.2f grados\n",
                rf.getDy(), rf.getDx(), rf.getAnguloDeg()));

            if (is3D) {
                sb.append("\n   Angulo de elevacion del vector r (desde plano XY):\n");
                sb.append("     phi_r = arctan(Dz / sqrt(Dx^2 + Dy^2))\n");
                sb.append(String.format("     phi_r = %.2f grados\n", rf.getAnguloElevacionDeg()));
            }

            sb.append("\n   Ley de Coulomb:\n");
            sb.append("     F = k * |q0 * q1| / r^2\n");
            sb.append(String.format(
                "     F = (8.99x10^9) * |%.4e * %.4e| / (%.4e)^2\n",
                res.getParticulaOrigen().getValorCarga() * 1e-6,
                rf.getParticulaCausante().getValorCarga() * 1e-6,
                rf.getDistanciaEnMetros()));
            sb.append(String.format("     F = %.6e N\n", rf.getMagnitud()));

            // Componentes usando vector unitario con signo de interacción
            String signoStr = rf.isEsRepulsion() ? "-" : "+";
            String signoDesc = rf.isEsRepulsion()
                ? "(signo - : repulsion, direccion opuesta a r)"
                : "(signo + : atraccion, misma direccion que r)";
            sb.append("\n   Componentes (descomposicion por vector unitario):\n");
            sb.append(String.format("     %s\n", signoDesc));
            sb.append(String.format(
                "     Fx = %sF * (Dx / r) = %s%.6e * (%.4f / %.4f) = %.6e N\n",
                signoStr, signoStr, rf.getMagnitud(),
                rf.getDx(), rf.getDistanciaEnUnidad(), rf.getFx()));
            sb.append(String.format(
                "     Fy = %sF * (Dy / r) = %s%.6e * (%.4f / %.4f) = %.6e N\n",
                signoStr, signoStr, rf.getMagnitud(),
                rf.getDy(), rf.getDistanciaEnUnidad(), rf.getFy()));

            if (is3D) {
                sb.append(String.format(
                    "     Fz = %sF * (Dz / r) = %s%.6e * (%.4f / %.4f) = %.6e N\n",
                    signoStr, signoStr, rf.getMagnitud(),
                    rf.getDz(), rf.getDistanciaEnUnidad(), rf.getFz()));
            }

            sb.append(rf.isEsRepulsion()
                ? "     (Repulsion: direccion contraria al vector r)\n"
                : "     (Atraccion: direccion igual al vector r)\n");
        }

        // ── Suma vectorial ───────────────────────────────────────────────────
        sb.append("\n\n================================================================\n");
        sb.append("SUMA VECTORIAL DE FUERZAS\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append("  Suma de componentes X:\n");
        sb.append(String.format("    SFx = %.6e N\n", res.getFuerzaTotalX()));
        sb.append("\n  Suma de componentes Y:\n");
        sb.append(String.format("    SFy = %.6e N\n", res.getFuerzaTotalY()));

        if (is3D) {
            sb.append("\n  Suma de componentes Z:\n");
            sb.append(String.format("    SFz = %.6e N\n", res.getFuerzaTotalZ()));
        }

        sb.append("\n  Fuerza resultante:\n");
        if (is3D) {
            sb.append("    |F| = sqrt(SFx^2 + SFy^2 + SFz^2)\n");
            sb.append(String.format("    |F| = sqrt((%.6e)^2 + (%.6e)^2 + (%.6e)^2)\n",
                res.getFuerzaTotalX(), res.getFuerzaTotalY(), res.getFuerzaTotalZ()));
        } else {
            sb.append("    |F| = sqrt(SFx^2 + SFy^2)\n");
            sb.append(String.format("    |F| = sqrt((%.6e)^2 + (%.6e)^2)\n",
                res.getFuerzaTotalX(), res.getFuerzaTotalY()));
        }
        sb.append(String.format("    |F| = %.6e N\n", res.getFuerzaTotal()));

        sb.append("\n  Angulo de la fuerza resultante:\n");
        sb.append(String.format("    theta_R = arctan(SFy / SFx) = %.2f grados\n",
            res.getAnguloResultante()));

        if (is3D) {
            sb.append(String.format("    phi_R   = arctan(SFz / sqrt(SFx^2+SFy^2)) = %.2f grados\n",
                res.getAnguloElevacionResultante()));
        }

        // ── Campo eléctrico ──────────────────────────────────────────────────
        sb.append("\n\n================================================================\n");
        sb.append("CAMPO ELECTRICO\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append("  Definicion: E = F / |q0|\n");
        sb.append(String.format("  E = %.6e / %.4e\n",
            res.getFuerzaTotal(),
            Math.abs(res.getParticulaOrigen().getValorCarga() * 1e-6)));
        sb.append(String.format("  E = %.6e N/C\n", res.getCampoElectrico()));

        sb.append("\n================================================================\n");
        sb.append("                      FIN DEL CALCULO\n");
        sb.append("================================================================\n");

        return sb.toString();
    }

    // ── Generación del archivo PDF ────────────────────────────────────────────

    private void generarPDFDesdeTexto(String rutaSalida,
                                       String contenido,
                                       String fecha) throws Exception {
        List<String> lines = partirEnLineas(contenido);

        final int HEADER_H  = 3 * LH + 10;
        final int FOOTER_H  = LH + 8;
        final int Y_BODY_P1 = PAGE_H - MARGIN - HEADER_H;
        final int Y_BODY_PN = PAGE_H - MARGIN;
        final int Y_BOTTOM  = MARGIN + FOOTER_H;

        int linesP1    = Math.max(1, (Y_BODY_P1 - Y_BOTTOM) / LH);
        int linesPn    = Math.max(1, (Y_BODY_PN - Y_BOTTOM) / LH);
        int totalPages = (lines.size() <= linesP1)
            ? 1
            : 1 + (int) Math.ceil((double)(lines.size() - linesP1) / linesPn);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        escribir(buf, "%PDF-1.4\n");
        escribir(buf, "%\u00e2\u00e3\u00cf\u00d3\n");

        // Obj 1: Catalog
        offsets.add(buf.size());
        escribir(buf, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // Obj 2: Pages
        offsets.add(buf.size());
        StringBuilder kids = new StringBuilder("[");
        for (int p = 0; p < totalPages; p++) kids.append(3 + p * 2).append(" 0 R ");
        kids.append("]");
        escribir(buf, "2 0 obj\n<< /Type /Pages /Kids " + kids
            + " /Count " + totalPages + " >>\nendobj\n");

        int objFont = 3 + totalPages * 2;

        // Streams por página
        List<byte[]> pageStreams = new ArrayList<>();
        for (int pg = 0; pg < totalPages; pg++) {
            pageStreams.add(construirStream(pg, totalPages, lines,
                linesP1, linesPn, Y_BODY_P1, Y_BODY_PN, fecha)
                .getBytes("ISO-8859-1"));
        }

        // Objetos Page + Stream
        for (int pg = 0; pg < totalPages; pg++) {
            int objPage    = 3 + pg * 2;
            int objContent = 4 + pg * 2;
            byte[] stream  = pageStreams.get(pg);

            offsets.add(buf.size());
            escribir(buf,
                objPage + " 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R " +
                "/MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] " +
                "/Contents " + objContent + " 0 R " +
                "/Resources << /Font << /F1 " + objFont + " 0 R >> >> >>\n" +
                "endobj\n");

            offsets.add(buf.size());
            escribir(buf, objContent + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
            buf.write(stream);
            escribir(buf, "\nendstream\nendobj\n");
        }

        // Fuente Courier
        offsets.add(buf.size());
        escribir(buf,
            objFont + " 0 obj\n" +
            "<< /Type /Font /Subtype /Type1 /BaseFont /Courier " +
            "/Encoding /WinAnsiEncoding >>\nendobj\n");

        // xref
        int xrefOff  = buf.size();
        int totalObj = offsets.size() + 1;
        escribir(buf, "xref\n0 " + totalObj + "\n");
        escribir(buf, "0000000000 65535 f \n");
        for (int off : offsets) escribir(buf, String.format("%010d 00000 n \n", off));

        // trailer
        escribir(buf,
            "trailer\n<< /Size " + totalObj + " /Root 1 0 R >>\n" +
            "startxref\n" + xrefOff + "\n%%EOF\n");

        try (FileOutputStream fos = new FileOutputStream(rutaSalida)) {
            buf.writeTo(fos);
        }
    }

    private String construirStream(int pg, int totalPages,
                                    List<String> lines,
                                    int linesP1, int linesPn,
                                    int yBodyP1, int yBodyPn,
                                    String fecha) {
        StringBuilder s = new StringBuilder();

        if (pg == 0) {
            int yTitle = PAGE_H - MARGIN - FS_HEAD;
            s.append("BT\n/F1 ").append(FS_HEAD).append(" Tf\n");
            s.append(MARGIN).append(" ").append(yTitle).append(" Td\n");
            s.append("(CALCULOS DE FUERZA ELECTRICA Y CAMPO ELECTRICO) Tj\nET\n");

            int yFecha = yTitle - LH;
            s.append("BT\n/F1 ").append(FS_BODY).append(" Tf\n");
            s.append(MARGIN).append(" ").append(yFecha).append(" Td\n");
            s.append("(Generado el: ").append(escapar(fecha)).append(") Tj\nET\n");

            int ySep = yFecha - LH / 2 - 2;
            s.append(MARGIN).append(" ").append(ySep).append(" m ")
             .append(PAGE_W - MARGIN).append(" ").append(ySep).append(" l S\n");
        }

        int lineStart = (pg == 0) ? 0 : linesP1 + (pg - 1) * linesPn;
        int lineEnd   = (pg == 0)
            ? Math.min(linesP1, lines.size())
            : Math.min(lineStart + linesPn, lines.size());
        int yStart    = (pg == 0) ? (yBodyP1 - LH) : (yBodyPn - LH);

        if (lineStart < lines.size()) {
            s.append("BT\n/F1 ").append(FS_BODY).append(" Tf\n");
            s.append(MARGIN).append(" ").append(yStart).append(" Td\n");
            for (int i = lineStart; i < lineEnd; i++) {
                s.append("(").append(escapar(lines.get(i))).append(") Tj\n");
                if (i < lineEnd - 1) s.append("0 -").append(LH).append(" Td\n");
            }
            s.append("ET\n");
        }

        // Pie de página
        s.append("BT\n/F1 7 Tf\n");
        s.append(MARGIN).append(" ").append(MARGIN - 10).append(" Td\n");
        s.append("(Pagina ").append(pg + 1).append(" de ").append(totalPages)
         .append(" - Sistema Ley de Coulomb USTA) Tj\nET\n");

        return s.toString();
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Parte el texto en líneas físicas respetando el ancho máximo de la página. */
    private List<String> partirEnLineas(String contenido) {
        String[] rawLines = contenido.split("\n", -1);
        List<String> lines = new ArrayList<>();
        for (String raw : rawLines) {
            if (raw.length() <= CHARS_PER_LINE) {
                lines.add(raw);
            } else {
                int indent = 0;
                for (int i = 0; i < raw.length() && raw.charAt(i) == ' '; i++) indent++;
                String pad = "      ".substring(0, Math.min(indent, 6));
                int pos = 0;
                while (pos < raw.length()) {
                    int end = Math.min(pos + CHARS_PER_LINE, raw.length());
                    lines.add(pos == 0 ? raw.substring(0, end) : pad + raw.substring(pos, end));
                    pos = end;
                }
            }
        }
        return lines;
    }

    private void escribir(ByteArrayOutputStream out, String texto) throws Exception {
        out.write(texto.getBytes("ISO-8859-1"));
    }

    private String escapar(String texto) {
        if (texto == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if      (c == '(' ) sb.append("\\(");
            else if (c == ')' ) sb.append("\\)");
            else if (c == '\\') sb.append("\\\\");
            else if (c < 32 || c > 126) sb.append('?');
            else sb.append(c);
        }
        return sb.toString();
    }
}