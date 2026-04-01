package com.usta.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Ventana {
    private String titulo;
    private String header;
    private String body;
    private AlertType alertType;

    
    
    public Ventana(String titulo, String header, String body, AlertType alertType) {
        this.titulo = titulo;
        this.header = header;
        this.body = body;
        this.alertType = alertType;
        Alert alert = new Alert(alertType);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(body);
        alert.showAndWait();
    }
    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getHeader() {
        return header;
    }
    public void setHeader(String header) {
        this.header = header;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public AlertType getAlertType() {
        return alertType;
    }
    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    

}
