package com.usta.controllers;

import java.io.IOException;

import com.usta.App;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

public class ConceptosPotencialController {

    @FXML
    private ImageView fc;

    @FXML
    public void initialize() {

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
