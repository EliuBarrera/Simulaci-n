module com.usta {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;

    opens com.usta to javafx.fxml;
    opens com.usta.controllers to javafx.fxml;
    exports com.usta;
}
