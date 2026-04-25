package com.usta.controllers;

import java.io.IOException;

import com.usta.App;

import javafx.fxml.FXML;

public class SimuladoresController {
      @FXML
    private void SimuladorLeyCoulomb() throws IOException {
        App.setRoot("leyCoulomb"); // Asegúrate de que este FXML existe
    }
    @FXML
    private void SimuladorLeyGauss() throws IOException {
        App.setRoot("leyGauss"); // Asegúrate de que este FXML existe
    }

     @FXML
    private void volverLogin() throws IOException {
        App.setRoot("login"); // Asegúrate de que este FXML existe
    }
    @FXML
    private void flujoElectrico() throws IOException {
        App.setRoot("unifiedFlow"); // Asegúrate de que este FXML existe
    }

    @FXML
    private void SimuladorPotencialElectrico() throws IOException {
        App.setRoot("potencialElectrico");
    }

    @FXML
    private void cerrarApp() {
        System.exit(0);
    }


}
