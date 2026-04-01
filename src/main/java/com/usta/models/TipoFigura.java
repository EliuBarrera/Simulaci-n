package com.usta.models;

/**
 * Tipos de figura disponibles para dibujar en el plano.
 */
public enum TipoFigura {
    CIRCULO("Círculo"),
    RECTANGULO("Rectángulo"),
    CUADRADO("Cuadrado"),
    TRIANGULO("Triángulo");

    private final String nombre;
    TipoFigura(String nombre) { this.nombre = nombre; }

    @Override public String toString() { return nombre; }
}