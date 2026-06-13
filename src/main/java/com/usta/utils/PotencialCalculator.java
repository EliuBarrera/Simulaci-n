package com.usta.utils;

import com.usta.models.Grafo;
import com.usta.models.Nodo;
import com.usta.models.ResultadoPotencial;
import com.usta.models.ResultadoPotencialIndividual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsula toda la lógica física del cálculo del Potencial Eléctrico
 * y Energía Potencial.
 *
 * $V = \sum K \frac{q_i}{r_i}$
 * $U = q_0 \cdot V$
 */
public class PotencialCalculator {

    private static final double K = 8.99e9; // Constante de Coulomb (N·m²/C²)

    private final Grafo grafo;
    private final CoordenadasTransformador transformador;
    private final boolean es3D;

    public PotencialCalculator(Grafo grafo, CoordenadasTransformador transformador, boolean es3D) {
        this.grafo = grafo;
        this.transformador = transformador;
        this.es3D = es3D;
    }

    public ResultadoPotencial calcular(Nodo particulaOrigen) {
        // En Potencial Eléctrico las conexiones son automáticas:
        // se usan TODAS las demás partículas del grafo, no solo las conectadas con aristas.
        Set<Nodo> otrosNodos = obtenerOtrosNodos(particulaOrigen);
        if (otrosNodos.isEmpty()) return null;

        double x0 = particulaOrigen.getX();
        double y0 = particulaOrigen.getY();
        double z0 = es3D ? particulaOrigen.getZ() : 0;
        double q0 = particulaOrigen.getValorCarga() * 1e-6; // Carga de la partícula de análisis en Coulombs

        List<ResultadoPotencialIndividual> individuales = new ArrayList<>();
        double potencialTotalV = 0;
        double energiaTotalU = 0;

        for (Nodo nd : otrosNodos) {
            ResultadoPotencialIndividual rpi = calcularPotencialIndividual(particulaOrigen, x0, y0, z0, q0, nd);
            individuales.add(rpi);
            potencialTotalV += rpi.getPotencialV();
            energiaTotalU += rpi.getEnergiaU();
        }

        return new ResultadoPotencial(
                particulaOrigen, x0, y0, z0,
                individuales,
                potencialTotalV, energiaTotalU,
                es3D
        );
    }

    private ResultadoPotencialIndividual calcularPotencialIndividual(Nodo origen, double x0, double y0, double z0, double q0, Nodo nd) {
        double x1 = nd.getX();
        double y1 = nd.getY();
        double z1 = es3D ? nd.getZ() : 0;
        
        // Conservar el signo para el potencial eléctrico
        // Si el tipo de carga es negativo y el valor ingresado es positivo, lo hacemos negativo.
        // Asumimos que getValorCarga() retorna el valor absoluto y el tipo de carga define el signo.
        double valorCargaNd = Math.abs(nd.getValorCarga());
        if ("-".equals(nd.getTipoCarga())) {
            valorCargaNd = -valorCargaNd;
        }
        double q1 = valorCargaNd * 1e-6;

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

        // Potencial Eléctrico: V = K * q_fuente / r
        double potencialV = K * q1 / distMetros;

        // Carga de origen con signo
        double valorCargaOrigen = Math.abs(origen.getValorCarga());
        if ("-".equals(origen.getTipoCarga())) {
            valorCargaOrigen = -valorCargaOrigen;
        }
        double qOrigen = valorCargaOrigen * 1e-6;

        // Energía Potencial: U = q_analisis * V
        double energiaU = qOrigen * potencialV;

        return new ResultadoPotencialIndividual(
                nd, x0, y0, z0, x1, y1, z1,
                distUnidad, distMetros,
                potencialV, energiaU
        );
    }

    /**
     * Devuelve todos los nodos del grafo excepto el nodo dado.
     * En Potencial Eléctrico, cada partícula interactúa con todas las demás
     * automáticamente (no se requieren conexiones manuales).
     */
    private Set<Nodo> obtenerOtrosNodos(Nodo nodo) {
        Set<Nodo> otros = new HashSet<>();
        for (Nodo n : grafo.getNodos()) {
            if (!n.equals(nodo)) otros.add(n);
        }
        return otros;
    }

    public static double getConstanteK() {
        return K;
    }
}
