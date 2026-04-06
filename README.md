# Proyecto EyM - Grafos y Simuladores de Física

Este repositorio contiene una aplicación JavaFX que combina simuladores de física y estructuras de grafos. El proyecto está construido con Maven y Java 17.

## Descripción

La aplicación se inicia con una interfaz gráfica basada en FXML y controla simuladores de: 
- Ley de Coulomb
- Ley de Gauss
- Conceptos de física
- Flujos unificados y animaciones

También incluye generación de PDF usando iText y recursos visuales como CSS e imágenes.

## Estructura principal del proyecto

- `pom.xml` - configuración de Maven y dependencias
- `src/main/java/com/usta/App.java` - clase principal de JavaFX
- `src/main/java/com/usta/Launcher.java` - punto de arranque para el JAR empaquetado
- `src/main/java/com/usta/controllers/` - controladores de los FXML
- `src/main/java/com/usta/models/` - clases del dominio y estructuras de datos
- `src/main/java/com/usta/utils/` - utilidades de cálculo, PDF y ventanas
- `src/main/java/com/usta/views/` - archivos FXML, CSS e imágenes
- `src/main/resources/` - recursos adicionales del proyecto

## Mapa del proyecto

```
Proyecto EyM/
├─ pom.xml
├─ module-info.java
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ com/
│  │  │  │  ├─ usta/
│  │  │  │  │  ├─ App.java
│  │  │  │  │  ├─ Launcher.java
│  │  │  │  │  ├─ controllers/
│  │  │  │  │  │  ├─ AnimacionCoulombController.java
│  │  │  │  │  │  ├─ ConceptosController.java
│  │  │  │  │  │  ├─ LeyCoulombController.java
│  │  │  │  │  │  ├─ LeyGaussController.java
│  │  │  │  │  │  ├─ MenuController.java
│  │  │  │  │  │  ├─ SimuladoresController.java
│  │  │  │  │  │  ├─ UnifiedFlowController.java
│  │  │  │  │  ├─ models/
│  │  │  │  │  │  ├─ Arista.java
│  │  │  │  │  │  ├─ FiguraGauss.java
│  │  │  │  │  │  ├─ Grafo.java
│  │  │  │  │  │  ├─ Nodo.java
│  │  │  │  │  │  ├─ ResultadoCalculo.java
│  │  │  │  │  │  ├─ ResultadoFuerza.java
│  │  │  │  │  │  ├─ ResultadoGauss.java
│  │  │  │  │  │  ├─ TipoFigura.java
│  │  │  │  │  ├─ utils/
│  │  │  │  │  │  ├─ CoordenadasTransformador.java
│  │  │  │  │  │  ├─ CoulombCalculator.java
│  │  │  │  │  │  ├─ GaussCalculator.java
│  │  │  │  │  │  ├─ PdfGenerator.java
│  │  │  │  │  │  ├─ UnidadDistancia.java
│  │  │  │  │  │  ├─ Ventana.java
│  │  │  │  │  ├─ views/
│  │  │  │  │  │  ├─ conceptos.fxml
│  │  │  │  │  │  ├─ guardados.fxml
│  │  │  │  │  │  ├─ leyCoulomb.fxml
│  │  │  │  │  │  ├─ LeyGauss.fxml
│  │  │  │  │  │  ├─ login.fxml
│  │  │  │  │  │  ├─ Simuladores.fxml
│  │  │  │  │  │  ├─ unifiedFlow.fxml
│  │  │  │  │  │  ├─ css/
│  │  │  │  │  │  │  ├─ Menustyle.css
│  │  │  │  │  │  │  ├─ SimuladoresStyle.css
│  │  │  │  │  │  │  ├─ Style.css
│  │  │  │  │  │  ├─ img/
│  │  │  │  │  │  │  ├─ logoFinal.png
│  │  │  │  │  │  │  ├─ ...otras imágenes...
│  │  ├─ resources/
│  │  │  ├─ com/
│  │  │  │  ├─ usta/
│  │  │  │  │  ├─ views/
│  │  │  │  │  │  ├─ conceptos.fxml
│  │  │  │  │  │  ├─ guardados.fxml
│  │  │  │  │  │  ├─ leyCoulomb.fxml
│  │  │  │  │  │  ├─ LeyGauss.fxml
│  │  │  │  │  │  ├─ login.fxml
│  │  │  │  │  │  ├─ Simuladores.fxml
│  │  │  │  │  │  ├─ unifiedFlow.fxml
│  │  │  │  │  │  ├─ css/
│  │  │  │  │  │  │  ├─ Menustyle.css
│  │  │  │  │  │  │  ├─ SimuladoresStyle.css
│  │  │  │  │  │  │  ├─ Style.css
│  │  │  │  │  │  ├─ img/
│  │  │  │  │  │  │  ├─ logoFinal.png
│  │  │  │  │  │  │  ├─ ...otras imágenes...
│  │  │  ├─ ...otros recursos si existen...
│  ├─ test/
│  │  ├─ java/
│  │  ├─ resources/
├─ target/
│  ├─ classes/
│  │  ├─ com/
│  │  │  ├─ usta/
│  │  │  │  ├─ controllers/
│  │  │  │  ├─ models/
│  │  │  │  ├─ utils/
│  │  │  │  ├─ views/
│  │  │  │  │  ├─ css/
│  │  │  │  │  ├─ img/
│  ├─ generated-sources/
│  ├─ libs/
│  │  ├─ itext7-core-7.2.5.pom
```

## Dependencias principales

- Java 17
- JavaFX 17.0.2
- iText 7.2.5

## Compilar y ejecutar

### Con Maven

Desde la carpeta raíz del proyecto:

```bash
mvn clean compile
mvn javafx:run
```

### Crear JAR ejecutable

```bash
mvn clean package
```

El JAR resultante se ubica en `target/` y las dependencias se copian en `target/libs/`.

### Ejecutar el JAR empaquetado

```bash
java -jar target/grafos-estructuras-1.0-SNAPSHOT.jar
```

> Si dependes de JavaFX fuera del JAR, asegúrate de usar el runtime de JavaFX apropiado o ejecuta con el plugin `javafx-maven-plugin`.

## Cómo modificar el programa

### 1. Cambiar vistas

- Abre o edita los archivos FXML en `src/main/java/com/usta/views/`.
- Ajusta los estilos en `src/main/java/com/usta/views/css/`.
- Agrega imágenes en `src/main/java/com/usta/views/img/`.

### 2. Cambiar lógica de controladores

- Edita las clases dentro de `src/main/java/com/usta/controllers/`.
- Los controladores están asociados a los FXML mediante el atributo `fx:controller`.

### 3. Cambiar lógica de modelos y cálculos

- Modifica o extiende las clases en `src/main/java/com/usta/models/`.
- Las utilidades de simulación están en `src/main/java/com/usta/utils/`, como `CoulombCalculator.java`, `GaussCalculator.java` y `PdfGenerator.java`.

### 4. Cambiar configuraciones globales

- Usa `module-info.java` para gestionar módulos Java y exportaciones.
- Ajusta el `pom.xml` para cambiar dependencias, plugins o versiones de Java.

## Puntos importantes para edición

- `App.java` controla la ventana principal y carga `login.fxml` al inicio.
- El título de la aplicación se define en `App.java` como `EduElectric- Simuladores de Física`.
- El ícono se carga desde `/com/usta/views/img/logoFinal.png`.
- Para ampliar la aplicación, agrega nuevos FXML y controladores, luego los enlazas desde los existentes.

## Recomendaciones

- Usa un IDE compatible con JavaFX (IntelliJ IDEA, Eclipse o VS Code con extensiones JavaFX).
- Ejecuta primero `mvn clean compile` para validar cambios de código.
- Si agregas nuevas dependencias, actualiza `pom.xml` y ejecuta `mvn package`.

## Actualización del README

Este archivo está diseñado para ser editable. Añade nuevas secciones sobre:
- nuevas funcionalidades
- flujos de uso
- instrucciones específicas de despliegue
- estructuras de datos y clases importantes

---

### Resumen rápido

- Iniciar: `mvn javafx:run`
- Compilar: `mvn clean compile`
- Empaquetar: `mvn clean package`
- Modificar vistas: `src/main/java/com/usta/views/`
- Modificar controladores: `src/main/java/com/usta/controllers/`
- Modificar lógica: `src/main/java/com/usta/models/` y `src/main/java/com/usta/utils/`
