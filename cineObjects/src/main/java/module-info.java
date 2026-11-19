/**
 * Por qué existe: declara de qué módulos depende el proyecto (como en el ejemplo MVCone).
 * java.sql trae JDBC; mysql.connector.j es el nombre de módulo del conector de MySQL;
 * javafx.controls y javafx.fxml son la interfaz gráfica.
 */
module ucsur.pe {
    requires java.sql;
    requires mysql.connector.j;
    requires javafx.controls;
    requires javafx.fxml;

    // JavaFX lee los @FXML de los controllers de pantalla, así que necesita acceso al paquete vista
    opens ucsur.pe.vista to javafx.fxml;

    exports ucsur.pe;
}
