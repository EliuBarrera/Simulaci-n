package com.usta.controllers;

import java.io.IOException;

import com.usta.App;

import javafx.fxml.FXML;

public class ConceptosSelectorController {

    @FXML
    private void conceptosCoulomb() throws IOException {
        App.setRoot("conceptosCoulomb");
    }

    @FXML
    private void conceptosGauss() throws IOException {
        App.setRoot("conceptosGauss");
    }

    @FXML
    private void conceptosFlujo() throws IOException {
        App.setRoot("conceptosFlujo");
    }

    @FXML
    private void conceptosPotencial() throws IOException {
        App.setRoot("conceptosPotencial");
    }

    @FXML
    private void volverLogin() throws IOException {
        App.setRoot("login");
    }

    @FXML
    private void cerrarApp() {
        System.exit(0);
    }
}
