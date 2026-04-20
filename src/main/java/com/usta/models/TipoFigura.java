package com.usta.models;

/**
 * Tipos de figura disponibles para dibujar en el plano.
 */
public enum TipoFigura {
    CIRCULO("Círculo"),
    RECTANGULO("Rectángulo"),
    CUADRADO("Cuadrado"),
    TRIANGULO("Triángulo"),
    // 3D
    ESFERA("Esfera"),
    CILINDRO("Cilindro"),
    CAJA("Caja");

    private final String nombre;
    TipoFigura(String nombre) { this.nombre = nombre; }

    @Override public String toString() { return nombre; }

    public boolean is3D() {
        return this == ESFERA || this == CILINDRO || this == CAJA;
    }
}