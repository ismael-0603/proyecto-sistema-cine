package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Por qué existe: varias escrituras van juntas o no van (una sala y sus butacas, una película y sus géneros).
 * Ejecuta el bloque en una transacción: commit si todo sale bien y rollback si algo falla, y deja la conexión
 * como estaba. Así cada DAO no repite el mismo try/catch.
 */
final class Transaccion {

    /** Un bloque de código JDBC que puede lanzar SQLException. */
    interface Bloque {
        void ejecutar() throws SQLException;
    }

    private Transaccion() { }

    /** Ejecuta el bloque en una transacción; si falla, revierte (envuelve SQLException en RepositoryException y relanza RuntimeException). */
    static void ejecutar(Connection conexion, String mensajeError, Bloque bloque) {
        try {
            boolean autoCommitAntes = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            try {
                bloque.ejecutar();
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(autoCommitAntes);
            }
        } catch (SQLException e) {
            throw new RepositoryException(mensajeError, e);
        }
    }
}
