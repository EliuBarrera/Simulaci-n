package com.usta.utils;

import com.usta.models.FiguraGauss;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;

/**
 * Gestiona la creación y actualización de la escena 3D en el simulador de Gauss.
 * Delega el renderizado en {@link GeneradorEscena3D}; aquí solo se traduce de
 * {@link FiguraGauss} a formas JavaFX 3D.
 */
public class Gauss3DManager {

    private static final double PX_POR_UNIT = 80.0;

    private final GeneradorEscena3D generador;

    public Gauss3DManager(GeneradorEscena3D generador) {
        this.generador = generador;
    }

    public GeneradorEscena3D getGenerador() {
        return generador;
    }

    /**
     * Limpia la escena y redibuja las figuras indicadas.
     *
     * @param figuraCargada   Figura con carga (null si no existe).
     * @param superficieGauss Superficie gaussiana (null si no existe).
     */
    public void actualizar(FiguraGauss figuraCargada, FiguraGauss superficieGauss) {
        generador.limpiarElementos();

        if (figuraCargada != null) {
            Color color = figuraCargada.getSigno().equals("+") ? Color.RED : Color.BLUE;
            agregarForma(figuraCargada, color, 1.0);
        }

        if (superficieGauss != null) {
            agregarForma(superficieGauss, Color.web("#00acc1", 0.3), 0.3);
        }
    }

    // =========================================================================
    // PRIVADOS
    // =========================================================================

    private void agregarForma(FiguraGauss f, Color color, double opacity) {
        Shape3D shape = crearForma(f);
        if (shape == null) return;

        shape.setMaterial(new PhongMaterial(color));
        shape.setOpacity(opacity);

        double esc = generador.getScale();
        shape.setTranslateX(toScene(f.getCx(), esc));
        shape.setTranslateY(-toScene(f.getCy(), esc));   // Y invertido en JavaFX 3D
        shape.setTranslateZ(toScene(f.getCz(), esc));

        generador.getElementosGraficos().getChildren().add(shape);
    }

    private Shape3D crearForma(FiguraGauss f) {
        double esc = generador.getScale();
        double p1  = toScene(f.getParam1(), esc);
        double p2  = toScene(f.getParam2(), esc);
        double p3  = toScene(f.getParam3(), esc);

        return switch (f.getTipo()) {
            case ESFERA     -> new Sphere(p1);
            case CILINDRO   -> new Cylinder(p1, p2);
            case CAJA       -> new Box(2 * p1, 2 * p2, 2 * p3);
            case CIRCULO    -> new Cylinder(p1, 2);
            case RECTANGULO -> new Box(2 * p1, 2, 2 * p2);
            case CUADRADO   -> new Box(2 * p1, 2, 2 * p1);
            case TRIANGULO  -> new Cylinder(p1, 2);
        };
    }

    /** Convierte unidades de modelo (px) a unidades de escena 3D. */
    private double toScene(double px, double escala) {
        return (px / PX_POR_UNIT) * escala;
    }
}