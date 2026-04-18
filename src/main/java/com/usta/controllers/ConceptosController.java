package com.usta.controllers;

import java.io.IOException;

import com.usta.App;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ConceptosController {

    @FXML
    private ImageView fc;

    @FXML
    public void initialize() {
        try {
            // Cargar imagen desde resources
            Image imagen = new Image(getClass().getResource("/com/usta/views/img/fc.png").toExternalForm());
            fc.setImage(imagen);
            System.out.println("Imagen cargada correctamente y vista de conceptos de Coulomb iniciada.");
        } catch (Exception e) {
            System.err.println("Error cargando la imagen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void volverAConceptos() throws IOException {
        App.setRoot("conceptosSelector");
    }

    @FXML
    private void volverAlMenu() throws IOException {
        App.setRoot("login");
    }
}
