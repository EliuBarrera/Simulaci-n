package com.usta.controllers;

import java.io.IOException;

import com.usta.App;

import javafx.fxml.FXML;

public class ConceptosController {

    @FXML
    public void initialize() {
        System.out.println("Vista de conceptos de Coulomb iniciada.");
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
