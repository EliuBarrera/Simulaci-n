package com.usta.models;

import java.util.Objects;

public class Nodo {
    private String nombre;
    private double x, y; // Coordenadas para la visualización
    private double z;     // Coordenada Z para modo 3D (default 0)
    private double valorCarga;
    private String tipoCarga;

    public Nodo(String nombre, double x, double y, double valorCarga, String tipoCarga) {
        this(nombre, x, y, 0, valorCarga, tipoCarga);
    }

    public Nodo(String nombre, double x, double y, double z, double valorCarga, String tipoCarga) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.z = z;
        this.valorCarga = valorCarga;
        this.tipoCarga = tipoCarga;
    }

    // Getters y setters
    public String getNombre() { return nombre; }
    public double getX() { return x; }
    public double getY() { return y;}
    public double getZ() { return z; }
    public double getValorCarga() {return valorCarga; }
    public String getTipoCarga() {return tipoCarga;}


    public void setNombre(String nombre) { 
        this.nombre = nombre; 
    }

    public void setX(double x) { 
        this.x = x; 
    }

    public void setY(double y) { 
        this.y = y; 
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setValorCarga(double valorCarga) {
        this.valorCarga = valorCarga;
    }
    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; 
        if (obj == null || getClass() != obj.getClass()) return false;
        Nodo nodo = (Nodo) obj; 
        return Objects.equals(nombre, nodo.nombre); 
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre); 
    }

   
   
}