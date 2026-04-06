package com.usta.utils;

import com.usta.models.Arista;
import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoCalculo;
import com.usta.models.ResultadoFuerza;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsula toda la lógica física del cálculo de fuerzas y campo
 * eléctrico según la Ley de Coulomb.
 *
 * No tiene dependencias con JavaFX ni con la interfaz gráfica.
 *
 * Las coordenadas del Nodo ya están en unidades lógicas del plano.
 * Solo se necesita el transformador para la conversión a metros.
 *
 * Soporta cálculos tanto en 2D como en 3D.
 */
public class CoulombCalculator {

    private static final double K = 8.99e9; // Constante de Coulomb (N·m²/C²)

    private final Grafo grafo;
    private final CoordenadasTransformador transformador;
    private final boolean es3D;

    /** Constructor 2D (retrocompatible). */
    public CoulombCalculator(Grafo grafo, CoordenadasTransformador transformador) {
        this(grafo, transformador, false);
    }

    /** Constructor con soporte 3D. */
    public CoulombCalculator(Grafo grafo, CoordenadasTransformador transformador, boolean es3D) {
        this.grafo         = grafo;
        this.transformador = transformador;
        this.es3D          = es3D;
    }

    /**
     * Calcula la fuerza eléctrica total y el campo sobre la partícula dada.
     *
     * Las coordenadas del Nodo ya son lógicas (unidades del plano),
     * se usan directamente sin conversión px→unidad.
     */
    public ResultadoCalculo calcular(Nodo particulaOrigen) {
        Set<Nodo> conectados = obtenerConectados(particulaOrigen);
        if (conectados.isEmpty()) return null;

        // Coordenadas lógicas directamente del Nodo
        double x0 = particulaOrigen.getX();
        double y0 = particulaOrigen.getY();
        double z0 = es3D ? particulaOrigen.getZ() : 0;
        double q0 = particulaOrigen.getValorCarga() * 1e-6;

        List<ResultadoFuerza> fuerzasIndividuales = new ArrayList<>();
        double fuerzaTotalX = 0;
        double fuerzaTotalY = 0;
        double fuerzaTotalZ = 0;

        for (Nodo nd : conectados) {
            ResultadoFuerza rf = calcularFuerzaIndividual(particulaOrigen, x0, y0, z0, q0, nd);
            fuerzasIndividuales.add(rf);
            fuerzaTotalX += rf.getFx();
            fuerzaTotalY += rf.getFy();
            fuerzaTotalZ += rf.getFz();
        }

        if (es3D) {
            return new ResultadoCalculo(
                particulaOrigen, x0, y0, z0,
                fuerzasIndividuales,
                fuerzaTotalX, fuerzaTotalY, fuerzaTotalZ,
                true
            );
        } else {
            return new ResultadoCalculo(
                particulaOrigen, x0, y0,
                fuerzasIndividuales,
                fuerzaTotalX, fuerzaTotalY
            );
        }
    }

    /**
     * Calcula la fuerza que ejerce {@code nd} sobre la partícula de análisis.
     */
    private ResultadoFuerza calcularFuerzaIndividual(Nodo origen, double x0, double y0, double z0,
                                                      double q0, Nodo nd) {
        // Coordenadas lógicas directamente del Nodo
        double x1 = nd.getX();
        double y1 = nd.getY();
        double z1 = es3D ? nd.getZ() : 0;
        double q1 = nd.getValorCarga() * 1e-6;

        // Vector de origen hacia nd, en unidades lógicas
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        double distUnidad;
        if (es3D) {
            distUnidad = Math.sqrt(dx * dx + dy * dy + dz * dz);
        } else {
            distUnidad = Math.hypot(dx, dy);
        }
        double distMetros = transformador.toMetros(distUnidad);

        // Magnitud escalar de la fuerza (siempre positiva)
        double F = K * Math.abs(q0) * Math.abs(q1) / (distMetros * distMetros);

        // Dirección:
        //   Mismos signos → repulsión → fuerza apunta DESDE nd HACIA origen → dirección (-dx, -dy, -dz)
        //   Signos distintos → atracción → fuerza apunta DESDE origen HACIA nd → dirección (+dx, +dy, +dz)
        boolean esRepulsion = origen.getTipoCarga().equals(nd.getTipoCarga());
        double signo = esRepulsion ? -1.0 : 1.0;

        double fx = signo * F * dx / distUnidad;
        double fy = signo * F * dy / distUnidad;
        double fz = es3D ? signo * F * dz / distUnidad : 0;

        if (es3D) {
            return new ResultadoFuerza(
                nd,
                x0, y0, z0,
                x1, y1, z1,
                dx, dy, dz,
                distUnidad, distMetros,
                F, fx, fy, fz,
                esRepulsion
            );
        } else {
            return new ResultadoFuerza(
                nd,
                x0, y0, x1, y1,
                dx, dy,
                distUnidad, distMetros,
                F, fx, fy,
                esRepulsion
            );
        }
    }

    /**
     * Devuelve el conjunto de nodos conectados a la partícula dada mediante aristas.
     */
    private Set<Nodo> obtenerConectados(Nodo nodo) {
        Set<Nodo> conectados = new HashSet<>();
        for (Arista arista : grafo.getAristas()) {
            if (arista.getOrigen().equals(nodo))       conectados.add(arista.getDestino());
            else if (arista.getDestino().equals(nodo)) conectados.add(arista.getOrigen());
        }
        return conectados;
    }

    public static double getConstanteK() {
        return K;
    }
}