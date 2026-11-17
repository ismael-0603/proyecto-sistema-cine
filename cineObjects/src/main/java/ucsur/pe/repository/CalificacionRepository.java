package ucsur.pe.repository;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;

/**
 * Contrato de persistencia de Calificacion: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple CalificacionMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface CalificacionRepository {
    /** Guarda la calificación; si aún no tiene id, le asigna uno (como el AUTO_INCREMENT). */
    void guardar(Calificacion calificacion);
    /** En la BD hay una sola calificación por compra (uq_calificacion_compra). */
    boolean existePorCompra(Compra compra);
}
