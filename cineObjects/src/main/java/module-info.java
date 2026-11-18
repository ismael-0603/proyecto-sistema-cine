/**
 * Por qué existe: declara de qué módulos depende el proyecto (como en el ejemplo MVCone).
 * java.sql trae JDBC; mysql.connector.j es el nombre de módulo del conector de MySQL.
 */
module ucsur.pe {
    requires java.sql;
    requires mysql.connector.j;

    exports ucsur.pe;
}
