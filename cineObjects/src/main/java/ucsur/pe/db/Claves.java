package ucsur.pe.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Por qué existe: las tablas de entidades de DB_Cine generan su id con AUTO_INCREMENT. Cada DAO inserta la fila
 * sin id (con Statement.RETURN_GENERATED_KEYS) y lee aquí el id que generó la base, para asignárselo
 * al objeto del modelo con su método asignarId. Así ningún DAO repite este bloque.
 */
final class Claves {

    private Claves() { } // solo tiene un método estático

    /** Id generado por el último executeUpdate del PreparedStatement (creado con RETURN_GENERATED_KEYS). */
    static int generada(PreparedStatement ps) throws SQLException {
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new SQLException("La base no devolvió el id generado");
            }
            return claves.getInt(1);
        }
    }
}
