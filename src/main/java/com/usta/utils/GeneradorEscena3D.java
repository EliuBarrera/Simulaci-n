package com.usta.utils;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Utilidad para generar una escena 3D nativa de JavaFX.
 * Incluye cámara, luces, ejes cartesianos y métodos para rotar (orbit) y hacer zoom.
 */
public class GeneradorEscena3D {

    private final Group root3D;
    private final Group world;
    private final Group elementosGraficos; // Partículas, líneas, flechas
    private final SubScene subScene;
    private final PerspectiveCamera camera;

    // Transformaciones para orbitar la cámara
    private final Group cameraXform = new Group();
    private final Group cameraXform2 = new Group();
    private final Group cameraXform3 = new Group();
    private final Rotate cameraRotX = new Rotate(-30, Rotate.X_AXIS);
    private final Rotate cameraRotY = new Rotate(45, Rotate.Y_AXIS);
    private final Translate cameraPan = new Translate(0, 0, 0);
    
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;

    // Escala para unidades métricas -> pixeles 3D
    private final double scale = 100.0;

    public GeneradorEscena3D(double width, double height) {
        root3D = new Group();
        world = new Group();
        elementosGraficos = new Group();
        
        root3D.getChildren().add(world);
        world.getChildren().add(elementosGraficos);

        // Construir Cámara
        camera = new PerspectiveCamera(true);
        cameraXform.getChildren().add(cameraXform2);
        cameraXform2.getChildren().add(cameraXform3);
        cameraXform3.getChildren().add(camera);
        
        cameraXform.getTransforms().addAll(cameraPan, cameraRotY);
        cameraXform2.getTransforms().add(cameraRotX);

        root3D.getChildren().add(cameraXform);

        camera.setNearClip(0.1);
        camera.setFarClip(20000.0);
        camera.setTranslateZ(-2000); // Mover la cámara hacia atrás
        
        // Nodos base (ejes y luces)
        construirEjes();
        construirLuces();

        subScene = new SubScene(root3D, width, height, true, javafx.scene.SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#f5f5f5"));
        subScene.setCamera(camera);

        manejarEventosRaton(subScene);
    }

    public void sincronizarGrafo(com.usta.models.Grafo grafo, com.usta.utils.UnidadDistancia unidad) {
        limpiarElementos();

        // Dibujar Aristas
        for (com.usta.models.Arista arista : grafo.getAristas()) {
            com.usta.models.Nodo o = arista.getOrigen();
            com.usta.models.Nodo d = arista.getDestino();

            // Usamos la misma escala (scale)
            double x1 = o.getX() * scale, y1 = -o.getY() * scale, z1 = o.getZ() * scale;
            double x2 = d.getX() * scale, y2 = -d.getY() * scale, z2 = d.getZ() * scale;

            javafx.geometry.Point3D p1 = new javafx.geometry.Point3D(x1, y1, z1);
            javafx.geometry.Point3D p2 = new javafx.geometry.Point3D(x2, y2, z2);

            Cylinder line = crearCilindroEntrePuntos(p1, p2, 2, Color.GRAY);
            elementosGraficos.getChildren().add(line);
        }

        // Dibujar Nodos
        for (com.usta.models.Nodo nodo : grafo.getNodos()) {
            Sphere s = new Sphere(15);
            s.setTranslateX(nodo.getX() * scale);
            s.setTranslateY(-nodo.getY() * scale);
            s.setTranslateZ(nodo.getZ() * scale);

            Color color = nodo.getTipoCarga().equals("+") ? Color.LIGHTCORAL : Color.LIGHTBLUE;
            s.setMaterial(new PhongMaterial(color));

            elementosGraficos.getChildren().add(s);
        }
    }

    public void dibujarFicha(double x, double y, double z, double fx, double fy, double fz, Color color) {
        double px = x * scale;
        double py = -y * scale; // Y invertido en JavaFX
        double pz = z * scale;

        //doble inversion del plano en el eje "Y", refactorizar

        // Normalizamos el vector de fuerza para darle un tamaño fijo visual
        double mag = Math.sqrt(fx*fx + fy*fy + fz*fz);
        if (mag == 0) return;

        double factorV = 100.0; // Largo de la flecha

        javafx.geometry.Point3D origin = new javafx.geometry.Point3D(px, py, pz);
        javafx.geometry.Point3D target = new javafx.geometry.Point3D(px + (fx/mag)*factorV, py - (fy/mag)*factorV, pz + (fz/mag)*factorV);

        Cylinder arrowBody = crearCilindroEntrePuntos(origin, target, 4, color);
        elementosGraficos.getChildren().add(arrowBody);
    }

    private Cylinder crearCilindroEntrePuntos(javafx.geometry.Point3D origin, javafx.geometry.Point3D target, double radius, Color color) {
        javafx.geometry.Point3D yAxis = new javafx.geometry.Point3D(0, 1, 0);
        javafx.geometry.Point3D diff = target.subtract(origin);
        double height = diff.magnitude();

        javafx.geometry.Point3D mid = target.midpoint(origin);
        Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

        javafx.geometry.Point3D axisOfRotation = diff.crossProduct(yAxis);
        double angle = Math.acos(diff.normalize().dotProduct(yAxis));
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

        Cylinder line = new Cylinder(radius, height);
        line.setMaterial(new PhongMaterial(color));
        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
        return line;
    }

    private void construirEjes() {
        double length = 1000.0; // Largo de los ejes

        // Eje X (Rojo)
        Box xAxis = new Box(length, 2, 2);
        xAxis.setMaterial(new PhongMaterial(Color.RED));
        xAxis.setTranslateX(length / 2);

        // Eje Y (Verde) -> En JavaFX Y crece hacia abajo, por eso lo hacemos visualmente compatible
        Box yAxis = new Box(2, length, 2);
        yAxis.setMaterial(new PhongMaterial(Color.GREEN));
        yAxis.setTranslateY(-length / 2);

        // Eje Z (Azul)
        Box zAxis = new Box(2, 2, length);
        zAxis.setMaterial(new PhongMaterial(Color.BLUE));
        zAxis.setTranslateZ(length / 2);

        // Centramos un pequeño origen
        Sphere origin = new Sphere(5);
        origin.setMaterial(new PhongMaterial(Color.BLACK));

        Group ejes = new Group(xAxis, yAxis, zAxis, origin);
        world.getChildren().add(ejes);
    }

    private void construirLuces() {
        AmbientLight ambient = new AmbientLight(Color.color(0.5, 0.5, 0.5));
        
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-500);
        pointLight.setTranslateY(-500);
        pointLight.setTranslateZ(-500);

        PointLight pointLight2 = new PointLight(Color.color(0.3, 0.3, 0.3));
        pointLight2.setTranslateX(500);
        pointLight2.setTranslateY(500);
        pointLight2.setTranslateZ(500);

        world.getChildren().addAll(ambient, pointLight, pointLight2);
    }

    private void manejarEventosRaton(SubScene scene) {
        scene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getX();
            mousePosY = me.getY();
            mouseOldX = me.getX();
            mouseOldY = me.getY();
            me.consume(); // Prevenir que el evento se propague al panel de fondo
        });

        scene.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getX();
            mousePosY = me.getY();
            
            double mouseDeltaX = (mousePosX - mouseOldX);
            double mouseDeltaY = (mousePosY - mouseOldY);

            // Click primario = Orbit (rotar)
            if (me.isPrimaryButtonDown()) {
                double modifier = 0.2; // Velocidad de rotación
                cameraRotY.setAngle(cameraRotY.getAngle() + mouseDeltaX * modifier);
                
                double rx = cameraRotX.getAngle() - mouseDeltaY * modifier;
                // Limitar rotación vertical para no invertir la cámara
                rx = Math.max(-89, Math.min(89, rx));
                cameraRotX.setAngle(rx);
            }
            
            // Botón secundario = Paneo (trasladar)
            if (me.isSecondaryButtonDown()) {
                double modifier = 2.0;
                cameraPan.setX(cameraPan.getX() - mouseDeltaX * modifier);
                cameraPan.setY(cameraPan.getY() - mouseDeltaY * modifier);
            }
            
            me.consume(); // Evitar movimientos involuntarios del fondo
        });

        scene.addEventFilter(ScrollEvent.ANY, (ScrollEvent event) -> {
            double zoomMod = event.getDeltaY() * 5.0; // Velocidad de zoom controlada
            if (Math.abs(zoomMod) > 0.01) {
                double newZ = camera.getTranslateZ() + zoomMod;
                
                // Limitar el zoom para no atravesar los objetos ni alejarse infinitamente
                newZ = Math.min(-100, Math.max(-15000, newZ));
                
                camera.setTranslateZ(newZ);
            }
            event.consume(); // Interceptar scroll a nivel nativo y evitar su propagacion
        });
    }

    // --- Métodos de utilidad pública ---

    public SubScene getSubScene() {
        return subScene;
    }

    public Group getElementosGraficos() {
        return elementosGraficos;
    }

    public double getScale() {
        return scale;
    }

    /**
     * Limpia las esferas y líneas dibujadas
     */
    public void limpiarElementos() {
        elementosGraficos.getChildren().clear();
    }
    
    /*
     * Ajusta el tamaño del SubScene
     */
    public void setSize(double width, double height) {
        subScene.setWidth(width);
        subScene.setHeight(height);
    }
}
