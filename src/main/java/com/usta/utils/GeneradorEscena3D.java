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

    // Materiales reutilizables para optimizar RAM
    private final PhongMaterial matEjeX = new PhongMaterial(Color.RED);
    private final PhongMaterial matEjeY = new PhongMaterial(Color.GREEN);
    private final PhongMaterial matEjeZ = new PhongMaterial(Color.BLUE);
    private final PhongMaterial matOrigin = new PhongMaterial(Color.BLACK);

    public GeneradorEscena3D(double width, double height) {
        root3D = new Group();
        world = new Group();
        elementosGraficos = new Group();
        
        root3D.getChildren().add(world);
        world.getChildren().add(elementosGraficos);

        // Centrar el cubo 10×10×10 en el origen del mundo para que la cámara
        // orbite alrededor del centro del cubo, no de una esquina.
        double halfCube = 5 * scale; // 500
        world.setTranslateX(-halfCube);
        world.setTranslateY(halfCube);    // Y invertido en JavaFX
        world.setTranslateZ(-halfCube);

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
        camera.setTranslateZ(-3500); // Mover la cámara más atrás para vista general
        
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
            Sphere s = new Sphere(15, 24); // Reducir divisiones
            s.setTranslateX(nodo.getX() * scale);
            s.setTranslateY(-nodo.getY() * scale);
            s.setTranslateZ(nodo.getZ() * scale);

            PhongMaterial mat = nodo.getTipoCarga().equals("+") ? matEjeX : matEjeZ; // Reusar materiales de ejes (Rojo/Azul)
            s.setMaterial(mat);

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

        Group arrow = crearFlechaEntrePuntos(origin, target, 4, color);
        elementosGraficos.getChildren().add(arrow);
    }

    // Malla estática para la cabeza de las flechas (Punta de pirámide)
    private static final javafx.scene.shape.TriangleMesh PUNTA_FLECHA_MESH = crearMallaPunta();

    private static javafx.scene.shape.TriangleMesh crearMallaPunta() {
        javafx.scene.shape.TriangleMesh mesh = new javafx.scene.shape.TriangleMesh();
        mesh.getTexCoords().addAll(0,0);
        float headHeight = 10, headRadius = 5;
        mesh.getPoints().addAll(
             0, headHeight/2, 0,
            -headRadius, -headHeight/2, -headRadius,
             headRadius, -headHeight/2, -headRadius,
             headRadius, -headHeight/2,  headRadius,
            -headRadius, -headHeight/2,  headRadius
        );
        mesh.getFaces().addAll(0,0, 1,0, 2,0,  0,0, 2,0, 3,0,  0,0, 3,0, 4,0,  0,0, 4,0, 1,0,  1,0, 3,0, 2,0,  1,0, 4,0, 3,0);
        return mesh;
    }

    private Group crearFlechaEntrePuntos(javafx.geometry.Point3D origin, javafx.geometry.Point3D target, double radius, Color color) {
        javafx.geometry.Point3D yAxis = new javafx.geometry.Point3D(0, 1, 0);
        javafx.geometry.Point3D diff = target.subtract(origin);
        double height = diff.magnitude();

        javafx.geometry.Point3D mid = target.midpoint(origin);
        Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

        javafx.geometry.Point3D axisOfRotation = diff.crossProduct(yAxis);
        double angle = diff.magnitude() == 0 ? 0 : Math.acos(diff.normalize().dotProduct(yAxis));
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

        Cylinder shaft = new Cylinder(radius, height, 8); // Baja resolución (8 caras)
        shaft.setMaterial(new PhongMaterial(color));
        
        javafx.scene.shape.MeshView head = new javafx.scene.shape.MeshView(PUNTA_FLECHA_MESH);
        head.setMaterial(new PhongMaterial(color));
        
        // Escalar la punta según el radio solicitado (por defecto la malla mide 10x10)
        double headScale = radius / 2.0; 
        head.setScaleX(headScale); head.setScaleY(headScale); head.setScaleZ(headScale);
        head.setTranslateY(height / 2 + (5 * headScale)); 
        
        Group flecha = new Group(shaft, head);
        flecha.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
        flecha.getTransforms().add(new Translate(0, 15, 0));
        return flecha;
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
        double length = 1000.0;

        // Eje X (Rojo)
        Box xAxis = new Box(length, 2, 2);
        xAxis.setMaterial(matEjeX);
        xAxis.setTranslateX(length / 2);

        // Eje Y (Verde)
        Box yAxis = new Box(2, length, 2);
        yAxis.setMaterial(matEjeY);
        yAxis.setTranslateY(-length / 2);

        // Eje Z (Azul)
        Box zAxis = new Box(2, 2, length);
        zAxis.setMaterial(matEjeZ);
        zAxis.setTranslateZ(length / 2);

        // Origen
        Sphere origin = new Sphere(5, 16); // Reducir divisiones
        origin.setMaterial(matOrigin);

        Group ejes = new Group(xAxis, yAxis, zAxis, origin);
        
        // Agregar Tics (Marcadores de unidad)
        for (int i = 1; i <= 10; i++) {
            double p = i * scale;
            
            // Tics en X (Reducir divisiones a 8 para ahorrar memoria)
            Cylinder tickX = new Cylinder(1, 14, 8);
            tickX.setMaterial(matEjeX);
            tickX.setTranslateX(p);
            
            // Tics en Y
            Cylinder tickY = new Cylinder(1, 14, 8);
            tickY.setMaterial(matEjeY);
            tickY.setTranslateY(-p);
            tickY.setRotationAxis(Rotate.Z_AXIS);
            tickY.setRotate(90);
            
            // Tics en Z
            Cylinder tickZ = new Cylinder(1, 14, 8);
            tickZ.setMaterial(matEjeZ);
            tickZ.setTranslateZ(p);
            
            ejes.getChildren().addAll(tickX, tickY, tickZ);
        }

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
            
            // Botón secundario = Paneo (trasladar) — con límites
            if (me.isSecondaryButtonDown()) {
                double modifier = 2.0;
                double newPanX = cameraPan.getX() - mouseDeltaX * modifier;
                double newPanY = cameraPan.getY() - mouseDeltaY * modifier;
                // Limitar el paneo para no perder de vista el sistema
                newPanX = Math.max(-500, Math.min(500, newPanX));
                newPanY = Math.max(-500, Math.min(500, newPanY));
                cameraPan.setX(newPanX);
                cameraPan.setY(newPanY);
            }
            
            me.consume(); // Evitar movimientos involuntarios del fondo
        });

        scene.addEventFilter(ScrollEvent.ANY, (ScrollEvent event) -> {
            double zoomMod = event.getDeltaY() * 5.0; // Velocidad de zoom controlada
            if (Math.abs(zoomMod) > 0.01) {
                double newZ = camera.getTranslateZ() + zoomMod;
                
                // Limitar el zoom para no atravesar los objetos ni alejarse demasiado
                newZ = Math.min(-300, Math.max(-5000, newZ));
                
                camera.setTranslateZ(newZ);
            }
            event.consume(); // Interceptar scroll a nivel nativo y evitar su propagacion
        });
    }

    // --- Métodos de utilidad pública ---

    public Rotate getCameraRotX() { return cameraRotX; }
    public Rotate getCameraRotY() { return cameraRotY; }
    public Translate getCameraPan() { return cameraPan; }
    public PerspectiveCamera getCamera() { return camera; }

    public Group getWorld() {

        return world;
    }

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

    /**
     * Restablece la cámara a la posición predeterminada que muestra
     * el cubo 3D completo centrado en la vista.
     */
    public void resetCamera() {
        cameraRotX.setAngle(-30);
        cameraRotY.setAngle(45);
        cameraPan.setX(0);
        cameraPan.setY(0);
        camera.setTranslateZ(-3500);
    }
    
    /*
     * Ajusta el tamaño del SubScene
     */
    public void setSize(double width, double height) {
        subScene.setWidth(width);
        subScene.setHeight(height);
    }
}
