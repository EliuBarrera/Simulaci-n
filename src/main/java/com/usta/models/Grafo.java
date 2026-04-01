package com.usta.models;

import java.util.ArrayList;
import java.util.List;
public class Grafo {
    private List<Nodo> nodos;
    private List<Arista> aristas;

    public Grafo() {
        this.nodos = new ArrayList<>();
        this.aristas = new ArrayList<>();
    }

    public void agregarNodo(Nodo nodo) {
        nodos.add(nodo);
    }

    public void agregarArista(Arista arista) {
        // Verificar si ya existe una arista entre los nodos
        for (Arista a : aristas) {
            if (a.esIgual(arista)) {
                // Si ya existe, simplemente actualiza el peso
                a.setPeso(arista.getPeso());
                return;
            }
        }
        // Si no existe, agregar la nueva arista
        aristas.add(arista);

        // También agregar la arista en la dirección opuesta para la bidireccionalidad
        Arista aristaInversa = new Arista(arista.getDestino(), arista.getOrigen(), arista.getPeso());
        aristas.add(aristaInversa);
    }

    public List<Nodo> getNodos() {
        return nodos;
    }

    public List<Arista> getAristas() {
        return aristas;
    }

    public void eliminarNodo(Nodo nodo) {
        nodos.remove(nodo);
        eliminarAristasDeNodo(nodo);
    }

    public void eliminarAristasDeNodo(Nodo nodo) {
        aristas.removeIf(arista -> arista.getOrigen().equals(nodo) || arista.getDestino().equals(nodo));
    }

    public void eliminarArista(Arista arista) {
        aristas.remove(arista);
        // También eliminar la arista inversa
        Arista aristaInversa = new Arista(arista.getDestino(), arista.getOrigen(), arista.getPeso());
        aristas.remove(aristaInversa);
    }

    public List<Nodo> obtenerAdyacentes(Nodo nodo) {
        List<Nodo> adyacentes = new ArrayList<>();

        for (Arista arista : aristas) {
            if (arista.getOrigen().equals(nodo)) {
                adyacentes.add(arista.getDestino());
            } else if (arista.getDestino().equals(nodo)) {
                adyacentes.add(arista.getOrigen());
            }
        }

        return adyacentes;
    }

    public List<Arista> getAristasDesde(Nodo nodo) {
        List<Arista> aristasDesdeNodo = new ArrayList<>();

        for (Arista arista : aristas) {
            if (arista.getOrigen().equals(nodo)) {
                aristasDesdeNodo.add(arista);
            }
        }
        return aristasDesdeNodo;
    }
}