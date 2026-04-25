package com.usta.models;

public class Arista {
    private Nodo origen;
    private Nodo destino;
    private double peso; // Puede representar distancia, tiempo o costo

    public Arista(Nodo origen, Nodo destino, double peso) {
        this.origen = origen;
        this.destino = destino;
        this.peso = peso;
    }

    // Getters
    public Nodo getOrigen() { 
        return origen; 
    }
    public Nodo getDestino() { 
        return destino; 
    }
    public double getPeso() { 
        return peso; 
    }

    public void setPeso(double nuevoPeso) {
       this.peso = nuevoPeso;
    }
    public String getIdentificador() {
        return (origen.getNombre().compareTo(destino.getNombre()) < 0) ?
            origen.getNombre() + "-" + destino.getNombre() :
            destino.getNombre() + "-" + origen.getNombre();
    }
    public boolean esIgual(Arista otra) {
        if (otra == null) return false;
        return (this.origen.equals(otra.origen) && this.destino.equals(otra.destino) ||
                this.origen.equals(otra.destino) && this.destino.equals(otra.origen));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Arista otra = (Arista) obj;
        return esIgual(otra);
    }

    @Override
    public int hashCode() {
        // Usamos una suma simétrica para que el orden origen/destino no importe
        return origen.hashCode() + destino.hashCode();
    }
}
