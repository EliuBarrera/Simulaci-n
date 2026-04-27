package com.usta.utils;

import com.usta.models.ResultadoPotencial;
import com.usta.models.ResultadoPotencialIndividual;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera un archivo PDF con el procedimiento detallado del cálculo
 * de Potencial Eléctrico y Energía Potencial.
 *
 * Implementa la especificación PDF 1.4 de forma manual (sin librerías externas).
 */
public class PdfPotencialGenerator {

    private static final int PAGE_W        = 612;
    private static final int PAGE_H        = 792;
    private static final int MARGIN        = 50;
    private static final int FS_HEAD       = 10;
    private static final int FS_BODY       = 8;
    private static final int LH            = 12;
    private static final int TEXT_W        = PAGE_W - MARGIN * 2;
    private static final int CHARS_PER_LINE = TEXT_W / 5;

    public void generar(String rutaSalida,
                        ResultadoPotencial resultado,
                        UnidadDistancia unidad,
                        String fecha) throws Exception {
        String contenido = construirTexto(resultado, unidad);
        generarPDFDesdeTexto(rutaSalida, contenido, fecha);
    }

    private String construirTexto(ResultadoPotencial res, UnidadDistancia unidad) {
        String simb = unidad.getSimbolo();
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
        sb.append(String.format("  Unidad de distancia   : %s (%s)\n", unidad.name(), simb));
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

        return sb.toString();
    }

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

        offsets.add(buf.size());
        escribir(buf, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(buf.size());
        StringBuilder kids = new StringBuilder("[");
        for (int p = 0; p < totalPages; p++) kids.append(3 + p * 2).append(" 0 R ");
        kids.append("]");
        escribir(buf, "2 0 obj\n<< /Type /Pages /Kids " + kids
                + " /Count " + totalPages + " >>\nendobj\n");

        int objFont = 3 + totalPages * 2;

        List<byte[]> pageStreams = new ArrayList<>();
        for (int pg = 0; pg < totalPages; pg++) {
            pageStreams.add(construirStream(pg, totalPages, lines,
                    linesP1, linesPn, Y_BODY_P1, Y_BODY_PN, fecha)
                    .getBytes("ISO-8859-1"));
        }

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

        offsets.add(buf.size());
        escribir(buf,
                objFont + " 0 obj\n" +
                        "<< /Type /Font /Subtype /Type1 /BaseFont /Courier " +
                        "/Encoding /WinAnsiEncoding >>\nendobj\n");

        int xrefOff  = buf.size();
        int totalObj = offsets.size() + 1;
        escribir(buf, "xref\n0 " + totalObj + "\n");
        escribir(buf, "0000000000 65535 f \n");
        for (int off : offsets) escribir(buf, String.format("%010d 00000 n \n", off));

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
            s.append("(CALCULO DE POTENCIAL Y ENERGIA POTENCIAL ELECTRICA) Tj\nET\n");

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

        s.append("BT\n/F1 7 Tf\n");
        s.append(MARGIN).append(" ").append(MARGIN - 10).append(" Td\n");
        s.append("(Pagina ").append(pg + 1).append(" de ").append(totalPages)
                .append(" - Sistema Potencial Electrico USTA) Tj\nET\n");

        return s.toString();
    }

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
