package com.usta.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import com.usta.App;

public class MenuController {

    @FXML
    private void Simuladores() throws IOException {
        App.setRoot("Simuladores"); // Asegúrate de que este FXML existe
    }

    @FXML
    private void abrirVistaConceptos() throws IOException {
        App.setRoot("conceptosSelector"); // Abre el selector de conceptos
    }

    @FXML
    private void cerrarApp() {
        System.exit(0);
    }



}