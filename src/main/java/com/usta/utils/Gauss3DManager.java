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

    // Caché de materiales para evitar recreación constante y reducir consumo de RAM
    private final PhongMaterial materialPositivo   = new PhongMaterial(Color.RED);
    private final PhongMaterial materialNegativo   = new PhongMaterial(Color.BLUE);
    private final PhongMaterial materialSuperficie = new PhongMaterial(Color.web("#00acc1", 0.3));

    public Gauss3DManager(GeneradorEscena3D generador) {
        this.generador = generador;
        // Ajustar especularidad para que no brillen demasiado
        materialPositivo.setSpecularColor(Color.WHITE);
        materialNegativo.setSpecularColor(Color.WHITE);
    }

    public GeneradorEscena3D getGenerador() {
        return generador;
    }

    /**
     * Limpia la escena y redibuja las figuras indicadas.
     */
    public void actualizar(FiguraGauss figuraCargada, FiguraGauss superficieGauss) {
        generador.limpiarElementos();

        if (figuraCargada != null) {
            PhongMaterial mat = figuraCargada.getSigno().equals("+") ? materialPositivo : materialNegativo;
            agregarForma(figuraCargada, mat, 1.0);
        }

        if (superficieGauss != null) {
            agregarForma(superficieGauss, materialSuperficie, 0.3);
        }
    }

    // =========================================================================
    // PRIVADOS
    // =========================================================================

    private void agregarForma(FiguraGauss f, PhongMaterial material, double opacity) {
        Shape3D shape = crearForma(f);
        if (shape == null) return;

        shape.setMaterial(material);
        // La opacidad en 3D se maneja mejor a través del color del material o del nodo
        // JavaFX 3D tiene soporte limitado para setOpacity en nodos complejos
        
        double esc = generador.getScale();
        shape.setTranslateX(toScene(f.getCx(), esc));
        shape.setTranslateY(-toScene(f.getCy(), esc));
        shape.setTranslateZ(toScene(f.getCz(), esc));

        generador.getElementosGraficos().getChildren().add(shape);
    }

    private Shape3D crearForma(FiguraGauss f) {
        double esc = generador.getScale();
        double p1  = toScene(f.getParam1(), esc);
        double p2  = toScene(f.getParam2(), esc);
        double p3  = toScene(f.getParam3(), esc);

        // Optimizamos las divisiones (segundo o tercer parámetro) para reducir polígonos
        return switch (f.getTipo()) {
            case ESFERA     -> new Sphere(p1, 32); // 32 divisiones es suficiente para buen detalle
            case CILINDRO   -> new Cylinder(p1, p2, 24); // 24 divisiones para cilindros
            case CAJA       -> new Box(2 * p1, 2 * p2, 2 * p3);
            case CIRCULO    -> new Cylinder(p1, 1, 24);
            case RECTANGULO -> new Box(2 * p1, 1, 2 * p2);
            case CUADRADO   -> new Box(2 * p1, 1, 2 * p1);
            case TRIANGULO  -> new Cylinder(p1, 1, 3); // Un triángulo 3D es un cilindro de 3 caras
        };
    }

    /** Convierte unidades de modelo (px) a unidades de escena 3D. */
    private double toScene(double px, double escala) {
        return (px / PX_POR_UNIT) * escala;
    }
}