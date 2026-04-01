package com.usta.utils;

public enum UnidadDistancia {
    PICOMETROS("pm", 1e12),
    NANOMETROS("nm", 1e9),
    MICROMETROS("µm", 1e6),
    MILIMETROS("mm", 1e3),
    CENTIMETROS("cm", 100),
    METROS("m", 1),
    KILOMETROS("km", 0.001);

    private final String simbolo;
    private final double conversionAMetros; // Factor para convertir de metros a esta unidad

    UnidadDistancia(String simbolo, double conversionAMetros) {
        this.simbolo = simbolo;
        this.conversionAMetros = conversionAMetros;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public double getConversionAMetros() {
        return conversionAMetros;
    }

    // Convierte de metros a esta unidad
    public double convertirDesdeMetros(double metros) {
        return metros * conversionAMetros;
    }

    // Convierte de esta unidad a metros
    public double convertirAMetros(double valor) {
        return valor / conversionAMetros;
    }

    @Override
    public String toString() {
        return simbolo;
    }

    // Método auxiliar para obtener por el símbolo
    public static UnidadDistancia porSimbolo(String simbolo) {
        for (UnidadDistancia unidad : UnidadDistancia.values()) {
            if (unidad.simbolo.equals(simbolo)) {
                return unidad;
            }
        }
        return METROS; // valor por defecto
    }
}
