package com.usta;

/**
 * Clase Launcher para evitar el error:
 * "JavaFX runtime components are missing"
 * 
 * Esta clase NO extiende de Application, por lo que puede ser
 * usada como Main-Class en el JAR sin problemas.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
